/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectDiscriminator;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectOperations;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.EntitlementUtils.createEntitlementQuery;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;

/**
 * Transforms subject provisioning operations (add, modify, delete) into entitlement operations, either on the subject,
 * or on entitlement objects. Note the naming convention of
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

    /** Transforms values in `association` container into subject attributes (where applicable). */
    void transformToSubjectOpsOnAdd(ResourceObject subject) throws SchemaException {
        PrismObject<ShadowType> subjectPrismObject = subject.getPrismObject();
        PrismContainer<ShadowAssociationType> associationContainer = subjectPrismObject.findContainer(ShadowType.F_ASSOCIATION);
        if (associationContainer == null || associationContainer.isEmpty()) {
            return;
        }

        SubjectOperations subjectOperations = new SubjectOperations();
        transformToSubjectOps(subjectOperations, associationContainer.getValues(), ModificationType.ADD);
        for (PropertyModificationOperation<?> operation : subjectOperations.getOperations()) {
            operation.getPropertyDelta().applyTo(subjectPrismObject);
        }
    }

    /** Transforms values in `association` container into object operations (where applicable). */
    @NotNull EntitlementObjectsOperations transformToObjectOpsOnAdd(
            ResourceObject subject, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        EntitlementObjectsOperations entitlementObjectsOperations = new EntitlementObjectsOperations();
        PrismContainer<ShadowAssociationType> associationContainer = subject.getAssociationsContainer();
        if (associationContainer == null || associationContainer.isEmpty()) {
            return entitlementObjectsOperations;
        }
        collectObjectOps(
                entitlementObjectsOperations,
                associationContainer.getValues(),
                null,
                subject.getBean(),
                ModificationType.ADD,
                result);
        return entitlementObjectsOperations;
    }
    //endregion

    //region Modify
    /**
     * Transforms modifications against the shadow's `association` container into attribute operations
     * (where applicable, i.e. for {@link ResourceObjectAssociationDirectionType#SUBJECT_TO_OBJECT} entitlement direction).
     */
    @NotNull SubjectOperations transformToSubjectOpsOnModify(
            ContainerDelta<ShadowAssociationType> associationDelta)
            throws SchemaException {

        checkNoReplace(associationDelta);

        SubjectOperations subjectOperations = new SubjectOperations();
        transformToSubjectOps(subjectOperations, associationDelta.getValuesToAdd(), ModificationType.ADD);
        transformToSubjectOps(subjectOperations, associationDelta.getValuesToDelete(), ModificationType.DELETE);
        return subjectOperations;
    }

    private void checkNoReplace(ContainerDelta<ShadowAssociationType> associationDelta) throws SchemaException {
        var valuesToReplace = associationDelta.getValuesToReplace();
        if (valuesToReplace != null) {
            LOGGER.error("Replace delta not supported for association, modifications {},\n provisioning context: {}",
                    associationDelta, subjectCtx);
            throw new SchemaException("Cannot perform replace delta for association, replace values: " + valuesToReplace);
        }
    }

    /**
     * Transforms modifications against the shadow's `association` container into operations on entitlement objects
     * (where applicable, i.e., for {@link ResourceObjectAssociationDirectionType#OBJECT_TO_SUBJECT} entitlement direction).
     */
    ShadowType transformToObjectOpsOnModify(
            EntitlementObjectsOperations objectsOperations,
            ContainerDelta<ShadowAssociationType> associationDelta,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        checkNoReplace(associationDelta);

        subjectShadowAfter = collectObjectOps(
                objectsOperations, associationDelta.getValuesToAdd(),
                subjectShadowBefore, subjectShadowAfter, ModificationType.ADD, result);
        subjectShadowAfter = collectObjectOps(
                objectsOperations, associationDelta.getValuesToDelete(),
                subjectShadowBefore, subjectShadowAfter, ModificationType.DELETE, result);
        return subjectShadowAfter;
    }
    //endregion

    //region Delete
    /**
     * This is somehow different that all the other methods. We are not following the content of a shadow or delta.
     * We are following the definitions. This is to avoid the need to read the object that is going to be deleted.
     * In fact, the object should not be there any more, but we still want to clean up entitlement membership
     * based on the information from the shadow.
     *
     * @param <T> Type of the association (referencing) attribute
     */
    <T> @NotNull EntitlementObjectsOperations transformToObjectOpsOnDelete(
            ShadowType subjectShadow, OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        EntitlementObjectsOperations objectsOperations = new EntitlementObjectsOperations();

        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();

        Collection<ResourceAssociationDefinition> associationDefs =
                subjectDef.getAssociationDefinitions();
        if (associationDefs.isEmpty()) {
            LOGGER.trace("No associations in deleted shadow, nothing to do");
            return objectsOperations;
        }
        ResourceAttributeContainer subjectAttributesContainer = ShadowUtil.getAttributesContainer(subjectShadow);
        for (ResourceAssociationDefinition associationDef : associationDefs) {
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

            QName associationName = associationDef.getName();
            for (String entitlementIntent : associationDef.getIntents()) {
                ResolvedAssociationDefinition def = resolveDefinition(associationDef, entitlementIntent);

                //noinspection unchecked
                ResourceAttributeDefinition<T> assocAttrDef =
                        (ResourceAttributeDefinition<T>)
                                def.entitlementObjDef.findAttributeDefinitionRequired(
                                        def.assocAttrName,
                                        () -> " in entitlement association '%s' in %s [association attribute]".formatted(
                                                associationName, def.entitlementCtx));

                final ResourceAttribute<T> valueAttr = subjectAttributesContainer.findAttribute(def.valueAttrName);
                if (valueAttr == null || valueAttr.isEmpty()) {
                    // We really want to throw the exception here. We cannot ignore this. If we ignore it then there may be
                    // entitlement membership value left undeleted and this situation will go undetected.
                    // Although we cannot really remedy the situation now, we at least throw an error so the problem is detected.
                    throw new SchemaException(
                            "Value attribute %s has no value; attribute defined in entitlement association '%s' in %s".formatted(
                                    def.valueAttrName, associationName, subjectCtx));
                }
                if (valueAttr.size() > 1) {
                    throw new SchemaException(
                            "Value attribute %s has no more than one value; attribute defined in entitlement association '%s' in %s"
                                    .formatted(def.valueAttrName, associationName, subjectCtx));
                }

                PrismPropertyValue<T> valueAttrValue = valueAttr.getValue();
                ObjectQuery query = createEntitlementQuery(valueAttrValue, assocAttrDef);

                QueryWithConstraints queryWithConstraints =
                        b.delineationProcessor.determineQueryWithConstraints(def.entitlementCtx, query, result);

                UcfObjectHandler handler = (ucfObject, lResult) -> {
                    ShadowType entitlementShadow = ucfObject.getBean();

                    ResourceObjectIdentification.WithPrimary entitlementIdentification =
                            ResourceObjectIdentification.fromCompleteShadow(def.entitlementObjDef, entitlementShadow);
                    ResourceObjectOperations singleObjectOperations =
                            objectsOperations.findOrCreate(
                                    ResourceObjectDiscriminator.of(entitlementIdentification),
                                    def.entitlementCtx);

                    PropertyDelta<T> attributeDelta = null;
                    for (Operation ucfOperation: singleObjectOperations.getUcfOperations()) {
                        if (ucfOperation instanceof PropertyModificationOperation<?> propOp) {
                            if (propOp.getPropertyDelta().getElementName().equals(def.assocAttrName)) {
                                //noinspection unchecked
                                attributeDelta = (PropertyDelta<T>) propOp.getPropertyDelta();
                            }
                        }
                    }
                    if (attributeDelta == null) {
                        attributeDelta = assocAttrDef.createEmptyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, def.assocAttrName));
                        var attributeModification = new PropertyModificationOperation<>(attributeDelta);
                        attributeModification.setMatchingRuleQName(associationDef.getMatchingRule());
                        singleObjectOperations.add(attributeModification);
                    }

                    attributeDelta.addValuesToDelete(valueAttr.getClonedValues());
                    LOGGER.trace("Association in deleted shadow delta:\n{}", attributeDelta.debugDumpLazily());

                    return true;
                };
                try {
                    LOGGER.trace("Searching for associations in deleted shadow, query: {}", queryWithConstraints.query);
                    ConnectorInstance connector = subjectCtx.getConnector(ReadCapabilityType.class, result);
                    connector.search(
                            def.entitlementObjDef,
                            queryWithConstraints.query,
                            handler,
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

    private @NotNull ResolvedAssociationDefinition resolveDefinition(
            ResourceAssociationDefinition associationDef, String entitlementIntent)
            throws SchemaException, ConfigurationException {
        ProvisioningContext entitlementCtx = subjectCtx.spawnForKindIntent(associationDef.getKind(), entitlementIntent);
        ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

        QName assocAttrName = associationDef.getDefinitionBean().getAssociationAttribute();
        QName valueAttrName = associationDef.getDefinitionBean().getValueAttribute();

        QName associationName = associationDef.getName();
        schemaCheck(assocAttrName != null,
                "No association attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);
        schemaCheck(valueAttrName != null,
                "No value attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);
        return new ResolvedAssociationDefinition(entitlementCtx, entitlementDef, assocAttrName, valueAttrName);
    }

    //endregion

    //region Common
    /**
     * Transforms operations on associations to operations on attributes.
     */
    private void transformToSubjectOps(
            SubjectOperations subjectOperations,
            Collection<PrismContainerValue<ShadowAssociationType>> associationValues,
            ModificationType modificationType)
            throws SchemaException {
        for (PrismContainerValue<ShadowAssociationType> associationValue : emptyIfNull(associationValues)) {
            transformToSubjectOps(subjectOperations, associationValue, modificationType);
        }
    }

    /**
     *  Collects entitlement changes from the shadow to entitlement section into attribute operations.
     *  Collects a single value.
     *  NOTE: only collects  SUBJECT_TO_ENTITLEMENT entitlement direction.
     */
    private <T> void transformToSubjectOps(
            SubjectOperations subjectOperations,
            PrismContainerValue<ShadowAssociationType> associationValue,
            ModificationType modificationType) throws SchemaException {
        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();

        ShadowAssociationType associationBean = associationValue.asContainerable();
        QName associationName = associationBean.getName();
        schemaCheck(associationName != null, "No name in entitlement association %s", associationValue);

        ResourceAssociationDefinition associationDef =
                subjectDef.findAssociationDefinitionRequired(associationName, () -> " in " + subjectCtx);

        if (!associationDef.isSubjectToObject()) {
            // Process just this one direction. The other direction is treated in `collectObjectOps`.
            return;
        }

        QName assocAttrName = associationDef.getDefinitionBean().getAssociationAttribute();
        QName valueAttrName = associationDef.getDefinitionBean().getValueAttribute();

        schemaCheck(assocAttrName != null,
                "No association attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);
        schemaCheck(valueAttrName != null,
                "No value attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);

        ResourceAttributeDefinition<?> assocAttrDef = subjectDef.findAttributeDefinition(assocAttrName);
        if (assocAttrDef == null) {
            throw new SchemaException(
                    "Association attribute '%s' defined in entitlement association '%s' was not found in schema for %s".formatted(
                            assocAttrName, associationName, subjectCtx));
        }

        //noinspection unchecked
        PropertyModificationOperation<T> attributeOperation = (PropertyModificationOperation<T>) subjectOperations.get(assocAttrName);
        if (attributeOperation == null) {
            //noinspection unchecked
            PropertyDelta<T> emptyDelta = (PropertyDelta<T>)
                    assocAttrDef.createEmptyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, assocAttrName));
            attributeOperation = new PropertyModificationOperation<>(emptyDelta);
            attributeOperation.setMatchingRuleQName(associationDef.getMatchingRule());
            subjectOperations.put(assocAttrName, attributeOperation);
        }

        //MID-7144: Identifier container may not be resource attribute container, if its origin is serialized pending delta
        PrismContainer<?> identifiersContainer = associationValue
                .findContainer(ShadowAssociationType.F_IDENTIFIERS);
        PrismProperty<T> valueAttr = identifiersContainer.findProperty(ItemName.fromQName(valueAttrName));
        if (valueAttr == null) {
            throw new SchemaException("No value attribute " + valueAttrName + " present in entitlement association '"
                    + associationName + "' in shadow for " + subjectCtx);
        }

        if (modificationType == ModificationType.ADD) {
            attributeOperation.getPropertyDelta()
                    .addValuesToAdd(valueAttr.getClonedValues());
        } else if (modificationType == ModificationType.DELETE) {
            attributeOperation.getPropertyDelta()
                    .addValuesToDelete(valueAttr.getClonedValues());
        } else if (modificationType == ModificationType.REPLACE) {
            // TODO: check if already exists
            attributeOperation.getPropertyDelta()
                    .setValuesToReplace(valueAttr.getClonedValues());
        }
    }

    private ShadowType collectObjectOps(
            EntitlementObjectsOperations objectsOperations,
            Collection<PrismContainerValue<ShadowAssociationType>> associationValuesInDeltaSet,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            ModificationType modificationType,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        for (PrismContainerValue<ShadowAssociationType> associationValue : emptyIfNull(associationValuesInDeltaSet)) {
            subjectShadowAfter = collectObjectOps(
                    objectsOperations, associationValue, subjectShadowBefore, subjectShadowAfter, modificationType, result);
        }
        return subjectShadowAfter;
    }

    private <TV,TA> ShadowType collectObjectOps(
            EntitlementObjectsOperations objectsOperations,
            PrismContainerValue<ShadowAssociationType> associationValue,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            ModificationType modificationType,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ResourceType resource = subjectCtx.getResource();
        ShadowAssociationType associationBean = associationValue.asContainerable();
        if (subjectCtx.getOperationContext() != null) {
            // todo this shouldn't be subjectCtx but the other one [viliam]
            subjectCtx.setAssociationShadowRef(associationBean.getShadowRef());
        }

        QName associationName = associationBean.getName();
        if (associationName == null) {
            throw new SchemaException("No name in entitlement association " + associationValue);
        }
        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();
        ResourceAssociationDefinition associationDef =
                subjectDef.findAssociationDefinitionRequired(associationName, () -> " in " + subjectCtx);

        if (!associationDef.isObjectToSubject()) {
            // Process just this one direction. The other direction is processed elsewhere (see transformToSubjectOps)
            return subjectShadowAfter;
        }

        Collection<String> entitlementIntents = associationDef.getIntents();
        if (entitlementIntents.isEmpty()) {
            throw new SchemaException("No entitlement intent specified in association " + associationValue + " in " + resource);
        }
        // TODO reconsider the effectiveness of executing this loop repeatedly for multiple intents
        for (String entitlementIntent : entitlementIntents) {
            ResolvedAssociationDefinition def = resolveDefinition(associationDef, entitlementIntent);

            //noinspection unchecked
            ResourceAttributeDefinition<TA> assocAttrDef =
                    (ResourceAttributeDefinition<TA>) def.entitlementObjDef.findAttributeDefinition(def.assocAttrName);
            if (assocAttrDef == null) {
                throw new SchemaException(
                        ("Association attribute '%s' defined in entitlement association was not found in entitlement intent(s) "
                                + "'%s' in schema for %s").formatted(def.assocAttrName, entitlementIntents, resource));
            }

            ResourceObjectIdentification<?> rawEntitlementIdentification = // may be secondary-only
                    ResourceObjectIdentification.fromAssociationValue(def.entitlementObjDef, associationValue);
            var primaryEntitlementIdentification =
                    b.resourceObjectReferenceResolver.resolvePrimaryIdentifier(
                            def.entitlementCtx, rawEntitlementIdentification, result);

            ResourceObjectOperations objectOperations =
                    objectsOperations.findOrCreate(
                            ResourceObjectDiscriminator.of(primaryEntitlementIdentification),
                            def.entitlementCtx);

            // Which shadow would we use - shadowBefore or shadowAfter?
            //
            // If the operation is ADD or REPLACE, we use current version of the shadow (shadowAfter), because we want
            // to ensure that we add most-recent data to the subject.
            //
            // If the operation is DELETE, we have two possibilities:
            //  - if the resource provides referential integrity, the subject has already
            //    new data (because the object operation was already carried out), so we use shadowAfter
            //  - if the resource does not provide referential integrity, the subject has OLD data
            //    so we use shadowBefore
            ShadowType subjectShadow;
            if (modificationType != ModificationType.DELETE) {
                subjectShadow = subjectShadowAfter;
            } else {
                if (associationDef.requiresExplicitReferentialIntegrity()) {
                    // we must ensure the referential integrity
                    subjectShadow = subjectShadowBefore;
                } else {
                    // i.e. resource has ref integrity assured by itself
                    subjectShadow = subjectShadowAfter;
                }
            }

            ResourceAttribute<TV> valueAttr = ShadowUtil.getAttribute(subjectShadow, def.valueAttrName);
            if (valueAttr == null) {
                if (!ShadowUtil.isFullShadow(subjectShadow)) {
                    ResourceObjectIdentification.WithPrimary subjectIdentification =
                            subjectCtx.getIdentificationFromShadow(subjectShadow);
                    LOGGER.trace("Fetching {} ({})", subjectShadow, subjectIdentification);
                    if (!subjectCtx.isReadingCachingOnly()) {
                        subjectShadow = ResourceObject.getBean(
                                ResourceObjectFetchOperation.executeRaw( // TODO what if there is no read capability at all?
                                        subjectCtx, subjectIdentification, result));
                    }
                    subjectShadowAfter = subjectShadow;
                    valueAttr = ShadowUtil.getAttribute(subjectShadow, def.valueAttrName);
                }
                if (valueAttr == null) {
                    LOGGER.error("No value attribute {} in shadow\n{}", def.valueAttrName, subjectShadow.debugDump());
                    // TODO: check schema and try to fetch full shadow if necessary
                    throw new SchemaException("No value attribute " + def.valueAttrName + " in " + subjectShadow);
                }
            }

            PropertyDelta<TA> attributeDelta = null;
            for (Operation ucfOperation : objectOperations.getUcfOperations()) {
                if (ucfOperation instanceof PropertyModificationOperation<?> propOp) {
                    if (propOp.getPropertyDelta().getElementName().equals(def.assocAttrName)) {
                        //noinspection unchecked
                        attributeDelta = (PropertyDelta<TA>) propOp.getPropertyDelta();
                    }
                }
            }
            if (attributeDelta == null) {
                attributeDelta = assocAttrDef.createEmptyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, def.assocAttrName));
            }

            PrismProperty<TA> changedAssocAttr = PrismUtil.convertProperty(valueAttr, assocAttrDef);

            if (modificationType == ModificationType.ADD) {
                attributeDelta.addValuesToAdd(changedAssocAttr.getClonedValues());
            } else if (modificationType == ModificationType.DELETE) {
                attributeDelta.addValuesToDelete(changedAssocAttr.getClonedValues());
            } else {
                throw new IllegalArgumentException("Unsupported modification type " + modificationType);
            }

            if (ResourceTypeUtil.isAvoidDuplicateValues(resource)) {
                ExistingResourceObject currentObject = objectOperations.getCurrentResourceObject();
                if (currentObject == null) {
                    LOGGER.trace("Fetching entitlement shadow {} to avoid value duplication (intent={})",
                            primaryEntitlementIdentification, entitlementIntent);
                    currentObject = ResourceObjectFetchOperation.executeRaw(
                            def.entitlementCtx, primaryEntitlementIdentification, result);
                    objectOperations.setCurrentResourceObject(currentObject);
                }
                // TODO It seems that duplicate values are checked twice: once here and the second time
                //  in ResourceObjectConverter.executeModify. Check that and fix if necessary.
                PropertyDelta<TA> attributeDeltaAfterNarrow = ProvisioningUtil.narrowPropertyDelta(
                        attributeDelta, currentObject, associationDef.getMatchingRule(), b.matchingRuleRegistry);
                if (attributeDeltaAfterNarrow == null || attributeDeltaAfterNarrow.isEmpty()) {
                    LOGGER.trace("Not collecting entitlement object operations ({}) association {}: "
                                    + "attribute delta is empty after narrow, orig delta: {}",
                            modificationType, associationName.getLocalPart(), attributeDelta);
                }
                attributeDelta = attributeDeltaAfterNarrow;
            }

            if (attributeDelta != null && !attributeDelta.isEmpty()) {
                PropertyModificationOperation<?> attributeModification = new PropertyModificationOperation<>(attributeDelta);
                attributeModification.setMatchingRuleQName(associationDef.getMatchingRule());
                LOGGER.trace("Collecting entitlement object operations ({}) association {}: {}",
                        modificationType, associationName.getLocalPart(), attributeModification);
                objectOperations.add(attributeModification);
            }
        }
        return subjectShadowAfter;
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
     * See {@link #resolveDefinition(ResourceAssociationDefinition, String)}.
     */
    private record ResolvedAssociationDefinition(
            @NotNull ProvisioningContext entitlementCtx,
            @NotNull ResourceObjectDefinition entitlementObjDef,
            @NotNull QName assocAttrName,
            @NotNull QName valueAttrName) {
    }
}
