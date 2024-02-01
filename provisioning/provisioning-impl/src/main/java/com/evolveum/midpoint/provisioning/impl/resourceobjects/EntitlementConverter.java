/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.EntitlementUtils.createEntitlementQuery;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectDiscriminator;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectOperations;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowAssociationsCollection;
import com.evolveum.midpoint.schema.util.ShadowAssociationsCollection.IterableAssociationValue;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

/**
 * Transforms subject provisioning operations (add, modify, delete) into entitlement operations, either on the subject,
 * or on entitlement objects.
 *
 * Deals with both subject-to-object and object-to-subject entitlement direction.
 *
 * Note the naming convention of
 *
 * - `transformToSubjectOpsOnXXX` (add, modify)
 * - `transformToObjectOpsOnXXX` (add, modify, delete)
 *
 * Intentionally package-private.
 *
 * @author Radovan Semancik
 */
class EntitlementConverter {

    private static final Trace LOGGER = TraceManager.getTrace(EntitlementConverter.class);

    @NotNull private final ProvisioningContext subjectCtx;

    private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    EntitlementConverter(@NotNull ProvisioningContext subjectCtx) {
        this.subjectCtx = subjectCtx;
    }

    //region Add

    /**
     * Transforms values in the `associations` container into subject attributes (where applicable).
     *
     * Used for:
     *
     * - subject ADD operations,
     * - {@link ResourceObjectAssociationDirectionType#SUBJECT_TO_OBJECT} entitlement direction.
     *
     * No deltas here. All that is done is the transformation of the object-to-be-added.
     *
     * An example: when a user is added, with `ri:privs` association to be created, the values of the association
     * are converted into user's `ri:privileges` attribute values.
     */
    void transformToSubjectOpsOnAdd(ResourceObject subject) throws SchemaException {
        List<IterableAssociationValue> associationValues =
                ShadowAssociationsCollection.ofShadow(subject.getBean()).getAllValues();
        SubjectOperations subjectOperations = transformToSubjectOps(associationValues);
        for (PropertyModificationOperation<?> operation : subjectOperations.getOperations()) {
            subject.applyDelta(operation.getPropertyDelta());
        }
    }

    /**
     * Transforms values in `associations` container into object operations (where applicable).
     *
     * Used for:
     *
     * - subject ADD operations,
     * - {@link ResourceObjectAssociationDirectionType#OBJECT_TO_SUBJECT} entitlement direction.
     *
     * An example: when a user is added, with `ri:groups` association to be created, the values of the association
     * are converted into ADD VALUE operations on `ri:member` attribute of the respective group objects.
     */
    @NotNull EntitlementObjectsOperations transformToObjectOpsOnAdd(
            ResourceObject subject, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        EntitlementObjectsOperations entitlementObjectsOperations = new EntitlementObjectsOperations();
        collectObjectOps(
                entitlementObjectsOperations,
                ShadowAssociationsCollection.ofShadow(subject.getBean()).getAllValues(),
                null,
                subject.getBean(),
                result);
        return entitlementObjectsOperations;
    }
    //endregion

    //region Modify
    /**
     * Transforms modifications against the shadow's `associations` container into subject attributes operations
     * (where applicable).
     *
     * Used for:
     *
     *  - on subject MODIFY operation,
     *  - {@link ResourceObjectAssociationDirectionType#SUBJECT_TO_OBJECT} entitlement direction.
     *
     * An example: when a user `ri:privs` association values are added, these values are converted into
     * ADD VALUE operations on `ri:privileges` attribute of the same user.
     */
    @NotNull SubjectOperations transformToSubjectOpsOnModify(
            @NotNull ShadowAssociationsCollection associationsCollection) throws SchemaException {
        checkNoReplace(associationsCollection);
        return transformToSubjectOps(associationsCollection.getAllValues());
    }

    private void checkNoReplace(@NotNull ShadowAssociationsCollection associationRelatedDelta) throws SchemaException {
        if (associationRelatedDelta.hasReplace()) {
            LOGGER.error("Replace delta not supported for\nassociation modifications:\n{}\nin provisioning context:\n{}",
                    associationRelatedDelta.debugDump(1), subjectCtx.debugDump(1));
            throw new SchemaException("Cannot perform replace delta for association");
        }
    }

    /**
     * Transforms modifications against the shadow's `associations` container into operations on entitlement objects
     * (where applicable).
     *
     * Used for:
     *
     * - subject MODIFY operation,
     * - {@link ResourceObjectAssociationDirectionType#OBJECT_TO_SUBJECT} entitlement direction.
     *
     * An example: when a user `ri:groups` association values are added, these values are converted into
     * ADD VALUE operations on `ri:member` attribute of the respective group objects.
     *
     * Special case: this method is also called when subject name is changed. There are special "fake" deltas
     * that add/delete the respective association value. See the respective caller code.
     */
    ShadowType transformToObjectOpsOnModify(
            @NotNull EntitlementObjectsOperations objectsOperations,
            @NotNull ShadowAssociationsCollection associationsCollection,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        checkNoReplace(associationsCollection);
        return collectObjectOps(
                objectsOperations, associationsCollection.getAllValues(),
                subjectShadowBefore, subjectShadowAfter, result);
    }
    //endregion

    //region Delete
    /**
     * Transforms subject's deletion into operations on entitlement objects (where applicable).
     *
     * Used for:
     *
     * - subject DELETE operation,
     * - {@link ResourceObjectAssociationDirectionType#OBJECT_TO_SUBJECT} entitlement direction.
     *
     * An example: when a user is deleted, all the respective values of `ri:member` attribute
     * of the group objects where he was a member are removed.
     *
     * Note:
     *
     * 1. There no corresponding method for {@link ResourceObjectAssociationDirectionType#SUBJECT_TO_OBJECT} direction.
     * Obviously, when the subject is deleted, all the subject-attached entitlement values are deleted with it.
     *
     * 2. For the other direction, this is somehow different that all the other methods. We are not following
     * the content of a shadow or delta. We are following the definitions. This is to avoid the need to read
     * the object that is going to be deleted. In fact, the object should not be there any more, but we still
     * want to clean up entitlement membership based on the information from the shadow.
     *
     * @param <T> Type of the association (referencing) attribute
     */
    <T> @NotNull EntitlementObjectsOperations transformToObjectOpsOnDelete(
            @NotNull ShadowType subjectShadow, @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        EntitlementObjectsOperations objectsOperations = new EntitlementObjectsOperations();

        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();

        for (ShadowAssociationDefinition associationDef : subjectDef.getAssociationDefinitions()) {
            if (!associationDef.isObjectToSubject()) {
                // We can ignore these. They will die together with the object. No need to explicitly delete them.
                LOGGER.trace("Ignoring subject-to-object association in deleted shadow");
                continue;
            }
            if (!associationDef.requiresExplicitReferentialIntegrity()) {
                LOGGER.trace("Ignoring association in deleted shadow because it does not require explicit"
                        + " referential integrity assurance");
                continue;
            }
            QName auxiliaryObjectClass = associationDef.getAuxiliaryObjectClass();
            if (auxiliaryObjectClass != null
                    && !subjectDef.hasAuxiliaryObjectClass(auxiliaryObjectClass)) {
                LOGGER.trace("Ignoring association in deleted shadow because subject does not have {} auxiliary object class",
                        auxiliaryObjectClass);
                continue;
            }

            // TODO We iterate through all the intents.
            //  This may be a bit inefficient, as it may find the same object multiple times. See e.g. MID-5687.
            for (String entitlementIntent : associationDef.getIntents()) {
                ResolvedAssociationDefinition def = resolveAssociationDefinition(associationDef, entitlementIntent);

                ResourceAttributeDefinition<T> assocAttrDef = def.getObjectAssociationAttributeDef();

                ResourceAttribute<T> valueAttr = ShadowUtil.getAttribute(subjectShadow, def.valueAttrName);
                // We really must get exactly one value here. We cannot ignore this. If we ignore it then there may be
                // entitlement membership value left undeleted and this situation will go undetected.
                // Although we cannot really remedy the situation now, we at least throw an error so the problem is detected.
                PrismPropertyValue<T> valueAttrValue = getSingleValue(valueAttr, def);
                ObjectQuery query = createEntitlementQuery(valueAttrValue, assocAttrDef);

                QueryWithConstraints queryWithConstraints =
                        b.delineationProcessor.determineQueryWithConstraints(def.entitlementCtx, query, result);

                UcfObjectHandler referencingAttributeValueDeletionHandler = (entitlementUcfObject, lResult) -> {
                    // Here we delete the referencing value by creating the respective modification operation on the entitlement.
                    ShadowType entitlementShadow = entitlementUcfObject.getBean();

                    ResourceObjectIdentification.WithPrimary entitlementIdentification =
                            ResourceObjectIdentification.fromCompleteShadow(def.entitlementObjDef, entitlementShadow);
                    ResourceObjectOperations singleObjectOperations =
                            objectsOperations.findOrCreate(
                                    ResourceObjectDiscriminator.of(entitlementIdentification),
                                    def.entitlementCtx);

                    PropertyModificationOperation<T> attributeOp =
                            singleObjectOperations.findOrCreateAttributeOperation(
                                    def.getObjectAssociationAttributeDef(),
                                    associationDef.getMatchingRule());

                    attributeOp.addValueToDelete(valueAttrValue.clone());
                    LOGGER.trace("Association in deleted shadow delta:\n{}", attributeOp.debugDumpLazily());

                    return true;
                };
                try {
                    LOGGER.trace("Searching for associations in deleted shadow, query: {}", queryWithConstraints.query);
                    ConnectorInstance connector = subjectCtx.getConnector(ReadCapabilityType.class, result);
                    connector.search(
                            def.entitlementObjDef,
                            queryWithConstraints.query,
                            referencingAttributeValueDeletionHandler,
                            def.entitlementCtx.createAttributesToReturn(),
                            null,
                            queryWithConstraints.constraints,
                            UcfFetchErrorReportingMethod.EXCEPTION,
                            subjectCtx.getUcfExecutionContext(),
                            result);
                } catch (TunnelException e) {
                    throw (SchemaException)e.getCause();
                } catch (GenericFrameworkException e) {
                    throw new GenericConnectorException(e.getMessage(), e);
                }
            }
        }
        return objectsOperations;
    }

    private <T> @NotNull PrismPropertyValue<T> getSingleValue(ResourceAttribute<T> valueAttr, ResolvedAssociationDefinition def)
            throws SchemaException {
        if (valueAttr == null || valueAttr.isEmpty()) {
            throw new SchemaException(
                    "Value attribute %s has no value; attribute defined in entitlement association '%s' in %s".formatted(
                            def.valueAttrName, def.associationName(), subjectCtx));
        }
        if (valueAttr.size() > 1) {
            throw new SchemaException(
                    "Value attribute %s has no more than one value; attribute defined in entitlement association '%s' in %s"
                            .formatted(def.valueAttrName, def.associationName(), subjectCtx));
        }

        return valueAttr.getValue();
    }

    /**
     * We need to collect (and check) quite a lot of information about the entitlement association,
     * for the particular intent.
     *
     * TODO Can we move this entirely to {@link ShadowAssociationDefinition}?
     */
    private @NotNull ResolvedAssociationDefinition resolveAssociationDefinition(
            ShadowAssociationDefinition associationDef, String entitlementIntent)
            throws SchemaException, ConfigurationException {

        ProvisioningContext entitlementCtx = subjectCtx.spawnForKindIntent(associationDef.getKind(), entitlementIntent);
        ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

        return new ResolvedAssociationDefinition(
                associationDef,
                entitlementCtx,
                entitlementDef,
                associationDef.getAssociationAttributeName(),
                associationDef.getValueAttributeName());
    }

    //endregion

    //region Common
    /**
     * Transforms operations on [subject] associations to operations on the same subject attributes.
     *
     * For {@link ResourceObjectAssociationDirectionType#SUBJECT_TO_OBJECT} entitlement direction, like `ri:priv`.
     */
    private @NotNull SubjectOperations transformToSubjectOps(
            @NotNull Collection<IterableAssociationValue> iterableAssociationValues)
            throws SchemaException {
        var subjectOperations = new SubjectOperations();

        for (var iterableAssocValue : iterableAssociationValues) {
            var associationDef = getAssociationDefinition(iterableAssocValue.name());
            if (!associationDef.isSubjectToObject()) {
                continue; // Process just this one direction. The other direction is treated in `collectObjectOps`.
            }

            // The identifier value we look for is e.g. the privilege name. We want to put it into the respective subject's
            // attribute (e.g., privileges). The association value identifiers may be a "raw" PrismContainer, e.g.,
            // if its origin is serialized pending delta (MID-7144).
            PrismPropertyValue<?> identifierValue =
                    iterableAssocValue.getSingleIdentifierValueRequired(associationDef.getValueAttributeName(), subjectCtx);

            // Now we are going to modify the "target" subject attribute (association attribute, e.g., privileges).
            ResourceAttributeDefinition<?> assocAttrDef = getSubjectSideAssociationAttributeDef(associationDef);
            PropertyModificationOperation<?> assocAttrOperation =
                    subjectOperations.findOrCreateOperation(assocAttrDef, associationDef.getMatchingRule());

            if (isAddNotDelete(iterableAssocValue)) {
                assocAttrOperation.addValueToAdd(identifierValue.clone());
            } else {
                assocAttrOperation.addValueToDelete(identifierValue.clone());
            }
        }
        return subjectOperations;
    }

    private @NotNull ShadowAssociationDefinition getAssociationDefinition(@NotNull ItemName associationValue) throws SchemaException {
        return subjectCtx.getObjectDefinitionRequired()
                .findAssociationDefinitionRequired(associationValue, () -> " in " + subjectCtx);
    }

    private @NotNull ResourceAttributeDefinition<?> getSubjectSideAssociationAttributeDef(
            ShadowAssociationDefinition associationDef) throws SchemaException {
        assert associationDef.isSubjectToObject();
        return subjectCtx.getObjectDefinitionRequired()
                .findAttributeDefinitionRequired(
                        associationDef.getAssociationAttributeName(),
                        () -> " in entitlement association '%s' in %s [association attribute]".formatted(
                                associationDef.getName(), subjectCtx));
    }

    /**
     * Transforms operations on [subject] associations to operations on target objects' attributes.
     *
     * For {@link ResourceObjectAssociationDirectionType#OBJECT_TO_SUBJECT} entitlement direction, like `ri:groups`.
     */
    private ShadowType collectObjectOps(
            @NotNull EntitlementObjectsOperations objectsOperations,
            @NotNull Collection<IterableAssociationValue> associationValues,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        for (var associationValue : associationValues) {
            subjectShadowAfter =
                    collectObjectOps(objectsOperations, associationValue, subjectShadowBefore, subjectShadowAfter, result);
        }
        return subjectShadowAfter;
    }

    private <TV,TA> ShadowType collectObjectOps(
            @NotNull EntitlementObjectsOperations entitlementObjectsOperations,
            @NotNull IterableAssociationValue associationValue,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        var associationBean = associationValue.value();
        if (subjectCtx.getOperationContext() != null) {
            // todo this shouldn't be subjectCtx but the other one [viliam]
            subjectCtx.setAssociationShadowRef(associationBean.getShadowRef());
        }

        var associationName = associationValue.name();
        ShadowAssociationDefinition associationDef = getAssociationDefinition(associationName);
        if (!associationDef.isObjectToSubject()) {
            return subjectShadowAfter; // The other direction is processed elsewhere (see transformToSubjectOps).
        }

        boolean isAddNotDelete = isAddNotDelete(associationValue);

        Collection<String> entitlementIntents = associationDef.getIntents();
        // TODO reconsider the effectiveness of executing this loop repeatedly for multiple intents
        for (String entitlementIntent : entitlementIntents) {
            ResolvedAssociationDefinition def = resolveAssociationDefinition(associationDef, entitlementIntent);

            var primaryEntitlementIdentification = getPrimaryEntitlementIdentification(associationValue, def, result);
            ResourceObjectOperations entitlementObjectOperations =
                    entitlementObjectsOperations.findOrCreate(
                            ResourceObjectDiscriminator.of(primaryEntitlementIdentification),
                            def.entitlementCtx);

            // We are going to issue some operations on the entitlement object, based on the (usually naming) attribute
            // of the subject.
            //
            // But which subject shadow should we use - "before" or "after"?
            //
            // If the operation is ADD or REPLACE, we use "after" version of the shadow, because we want
            // to ensure that we add most-recent data to the entitlement object.
            //
            // If the operation is DELETE, we have two possibilities:
            //  - if the resource provides referential integrity, the entitlement object has already
            //    new data (because the subject operation was already carried out), so we use "after";
            //  - if the resource does not provide referential integrity, the entitlement object has OLD data
            //    so we use "before" state.
            ShadowType subjectShadow;
            if (isAddNotDelete) {
                subjectShadow = subjectShadowAfter;
            } else {
                if (associationDef.requiresExplicitReferentialIntegrity()) {
                    subjectShadow = subjectShadowBefore; // we must ensure the referential integrity
                } else {
                    subjectShadow = subjectShadowAfter; // the resource has referential integrity assured by itself
                }
            }

            // Association attribute is e.g. the group "members" attribute. We are going to add/delete subject name to/from it.
            ResourceAttributeDefinition<TA> assocAttrDef = def.getObjectAssociationAttributeDef();

            // This is the specific operation that will modify the association attribute (e.g. group members list).
            PropertyModificationOperation<TA> assocAttrOp =
                    entitlementObjectOperations.findOrCreateAttributeOperation(assocAttrDef, associationDef.getMatchingRule());

            // What value should we add/delete from that attribute? (Like user DN.) It's called the value attribute.
            ResourceAttribute<TV> valueAttr = ShadowUtil.getAttribute(subjectShadow, def.valueAttrName);
            if (valueAttr == null) {
                if (!ShadowUtil.isFullShadow(subjectShadow)) {
                    // Normally, this should not be necessary, as the value attribute is often the identifier.
                    var subjectPrimaryIdentification = subjectCtx.getIdentificationFromShadow(subjectShadow);
                    LOGGER.trace("Fetching {} ({})", subjectShadow, subjectPrimaryIdentification);
                    if (!subjectCtx.isReadingCachingOnly()) {
                        subjectShadow = ResourceObject.getBean(
                                ResourceObjectFetchOperation.executeRaw( // TODO what if there is no read capability at all?
                                        subjectCtx, subjectPrimaryIdentification, result));
                    }
                    subjectShadowAfter = subjectShadow;
                    valueAttr = ShadowUtil.getAttribute(subjectShadow, def.valueAttrName);
                }
            }
            PrismPropertyValue<TV> valueAttrValue = getSingleValue(valueAttr, def);

            // Let us convert that into the schema of the entitlement object, if necessary.
            PrismPropertyValue<TA> assocAttrValueToAddOrDelete = assocAttrDef.convertPrismValue(valueAttrValue);

            if (isAddNotDelete) {
                assocAttrOp.addValueToAdd(assocAttrValueToAddOrDelete.clone());
            } else {
                assocAttrOp.addValueToDelete(assocAttrValueToAddOrDelete.clone());
            }

//            if (ResourceTypeUtil.isAvoidDuplicateValues(resource)) {
//                ExistingResourceObject currentObject = entitlementObjectOperations.getCurrentResourceObject();
//                if (currentObject == null) {
//                    LOGGER.trace("Fetching entitlement shadow {} to avoid value duplication (intent={})",
//                            primaryEntitlementIdentification, entitlementIntent);
//                    currentObject = ResourceObjectFetchOperation.executeRaw(
//                            def.entitlementCtx, primaryEntitlementIdentification, result);
//                    entitlementObjectOperations.setCurrentResourceObject(currentObject);
//                }
//                // TODO It seems that duplicate values are checked twice: once here and the second time
//                //  in ResourceObjectConverter.executeModify. Check that and fix if necessary.
//                PropertyDelta<TA> attributeDeltaAfterNarrow = ProvisioningUtil.narrowPropertyDelta(
//                        attributeDelta, currentObject, associationDef.getMatchingRule(), b.matchingRuleRegistry);
//                if (attributeDeltaAfterNarrow == null || attributeDeltaAfterNarrow.isEmpty()) {
//                    LOGGER.trace("Not collecting entitlement object operations ({}) association {}: "
//                                    + "attribute delta is empty after narrow, orig delta: {}",
//                            modificationType, associationName.getLocalPart(), attributeDelta);
//                }
//                attributeDelta = attributeDeltaAfterNarrow;
//            }
//
//            if (attributeDelta != null && !attributeDelta.isEmpty()) {
//                PropertyModificationOperation<?> attributeModification = new PropertyModificationOperation<>(attributeDelta);
//                attributeModification.setMatchingRuleQName(associationDef.getMatchingRule());
//                LOGGER.trace("Collecting entitlement object operations ({}) association {}: {}",
//                        modificationType, associationName.getLocalPart(), attributeModification);
//                entitlementObjectOperations.add(attributeModification);
//            }
        }
        return subjectShadowAfter;
    }

    private boolean isAddNotDelete(IterableAssociationValue associationValue) {
        if (associationValue.isAdd()) {
            return true;
        } else if (associationValue.isDelete()) {
            return false;
        } else {
            throw new AssertionError("We already checked that no REPLACE delta can be here");
        }
    }

    private @NotNull ResourceObjectIdentification.WithPrimary getPrimaryEntitlementIdentification(
            IterableAssociationValue associationValue, ResolvedAssociationDefinition def, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        // this identification may be secondary-only
        var rawEntitlementIdentification =
                ResourceObjectIdentification.fromAssociationValue(def.entitlementObjDef, associationValue.associationPcv());
        return b.resourceObjectReferenceResolver.resolvePrimaryIdentifier(
                def.entitlementCtx, rawEntitlementIdentification, result);
    }
    //endregion

    /**
     * Keeps modification operations indexed by attribute names.
     */
    static class SubjectOperations {
        private final Map<QName, PropertyModificationOperation<?>> operationMap = new HashMap<>();

        public Collection<PropertyModificationOperation<?>> getOperations() {
            return operationMap.values();
        }

        PropertyModificationOperation<?> get(QName attributeName) {
            return operationMap.get(attributeName);
        }

        void put(QName attributeName, PropertyModificationOperation<?> operation) {
            operationMap.put(attributeName, operation);
        }

        <T> @NotNull PropertyModificationOperation<T> findOrCreateOperation(
                ResourceAttributeDefinition<T> assocAttrDef, QName matchingRuleName) {
            ItemName assocAttrName = assocAttrDef.getItemName();
            //noinspection unchecked
            PropertyModificationOperation<T> attributeOperation = (PropertyModificationOperation<T>) get(assocAttrName);
            if (attributeOperation != null) {
                return attributeOperation;
            }
            PropertyDelta<T> emptyDelta = assocAttrDef.createEmptyDelta();
            var newOperation = new PropertyModificationOperation<>(emptyDelta);
            newOperation.setMatchingRuleQName(matchingRuleName);
            put(assocAttrName, newOperation);
            return newOperation;
        }
    }

    static class EntitlementObjectsOperations implements DebugDumpable {
        final Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap = new HashMap<>();

        @NotNull ResourceObjectOperations findOrCreate(
                @NotNull ResourceObjectDiscriminator disc, @NotNull ProvisioningContext entitlementCtx) {
            ResourceObjectOperations existing = roMap.get(disc);
            if (existing != null) {
                return existing;
            }
            var operations = new ResourceObjectOperations(entitlementCtx);
            roMap.put(disc, operations);
            return operations;
        }

        @Override
        public String debugDump(int indent) {
            return DebugUtil.debugDump(roMap, indent);
        }
    }

    /**
     * Association definition resolved into directly usable details.
     * See {@link #resolveAssociationDefinition(ShadowAssociationDefinition, String)}.
     */
    private record ResolvedAssociationDefinition(
            @NotNull ShadowAssociationDefinition associationDef,
            @NotNull ProvisioningContext entitlementCtx,
            @NotNull ResourceObjectDefinition entitlementObjDef,
            @NotNull QName assocAttrName,
            @NotNull QName valueAttrName) {

        QName associationName() {
            return associationDef.getName();
        }

        <T> @NotNull ResourceAttributeDefinition<T> getObjectAssociationAttributeDef() {
            assert associationDef.isObjectToSubject();
            try {
                return entitlementObjDef.findAttributeDefinitionRequired(
                        assocAttrName,
                        () -> " in entitlement association '%s' in %s [association attribute]".formatted(
                                associationName(), entitlementCtx));
            } catch (SchemaException e) {
                throw SystemException.unexpected(e); // FIXME resolve somehow
            }
        }
    }
}
