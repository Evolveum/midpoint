/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.RepoShadow;

import com.evolveum.midpoint.schema.util.ShadowReferenceAttributesCollection;

import com.evolveum.midpoint.schema.util.ShadowReferenceAttributesCollection.IterableReferenceAttributeValue;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectOperations;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.EntitlementUtils.*;

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

    /** Information to be used in error messages. */
    @NotNull private final Object errorCtx;

    private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    EntitlementConverter(@NotNull ProvisioningContext subjectCtx) {
        this.subjectCtx = subjectCtx;
        this.errorCtx = subjectCtx.getExceptionDescriptionLazy();
    }

    //region Subject-to-object
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
    void transformToSubjectOpsOnAdd(ResourceObjectShadow subject) throws SchemaException {
        List<IterableReferenceAttributeValue> refAttrValues =
                ShadowReferenceAttributesCollection.ofShadow(subject.getBean()).getAllIterableValues();
        subject.applyOperations(
                transformToSubjectOps(refAttrValues));
    }

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
    @NotNull Collection<Operation> transformToSubjectOpsOnModify(
            @NotNull ShadowReferenceAttributesCollection attributesCollection) throws SchemaException {
        checkNoReplace(attributesCollection);
        return transformToSubjectOps(attributesCollection.getAllIterableValues())
                .getOperations();
    }

    /**
     * Transforms operations on [subject] associations to operations on the same subject attributes.
     *
     * For {@link ResourceObjectAssociationDirectionType#SUBJECT_TO_OBJECT} entitlement direction, like `ri:priv`.
     */
    private @NotNull SubjectOperations transformToSubjectOps(
            @NotNull Collection<IterableReferenceAttributeValue> iterableRefAttrValues)
            throws SchemaException {

        LOGGER.trace("Transforming {} iterable reference attribute values to subject operations", iterableRefAttrValues.size());

        var subjectOperations = new SubjectOperations();
        for (var iterableRefAttrValue : iterableRefAttrValues) {
            var refAttrDef = getReferenceAttributeDefinition(iterableRefAttrValue.name());
            if (!isVisible(refAttrDef, subjectCtx)) {
                continue;
            }

            var simulationDefinition = refAttrDef.getSimulationDefinition();
            if (simulationDefinition == null) {
                // Native association: just use the value as it is
                subjectOperations
                        .findOrCreateOperation(refAttrDef)
                        .swallowValue(
                                iterableRefAttrValue.value().clone(),
                                iterableRefAttrValue.isAddNotDelete());
                continue;
            }

            if (!isSimulatedSubjectToObject(refAttrDef)
                    || !doesMatchSubjectDelineation(refAttrDef, subjectCtx)) {
                continue;
            }

            // Just take the binding attribute value (like privilege name, e.g. "read") ...
            PrismPropertyValue<?> bindingAttributeValue =
                    iterableRefAttrValue.value().getSingleIdentifierValueRequired(
                            simulationDefinition.getPrimaryObjectBindingAttributeName(), // e.g. icfs:name on privilege
                            errorCtx);

            // ... and add/delete it to/from the binding attribute of the subject (like ri:privileges in account).
            subjectOperations
                    .findOrCreateOperation(
                            simulationDefinition.getSubjectSidePrimaryBindingAttributeDef(), // e.g. ri:privileges on account
                            simulationDefinition.getPrimaryBindingMatchingRuleLegacy())
                    .swallowValue(
                            bindingAttributeValue.clone(), // e.g. "read" (a privilege name)
                            iterableRefAttrValue.isAddNotDelete());
        }

        LOGGER.trace("Transformed iterable association values to:\n{}", subjectOperations.debugDumpLazily(1));
        return subjectOperations;
    }
    //endregion

    //region Object-to-subject
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
    @NotNull EntitlementObjectsOperations transformToObjectOpsOnAdd(ResourceObjectShadow subject, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {

        EntitlementObjectsOperations entitlementObjectsOperations = new EntitlementObjectsOperations();
        collectObjectOps(
                entitlementObjectsOperations,
                ShadowReferenceAttributesCollection.ofShadow(subject.getBean()).getAllIterableValues(),
                null,
                subject.getBean(),
                result);
        return entitlementObjectsOperations;
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
    void transformToObjectOpsOnModify(
            @NotNull EntitlementObjectsOperations objectsOperations,
            @NotNull ShadowReferenceAttributesCollection attributesCollection,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        checkNoReplace(attributesCollection);
        collectObjectOps(
                objectsOperations, attributesCollection.getAllIterableValues(),
                subjectShadowBefore, subjectShadowAfter, result);
    }

    /**
     * Transforms operations on [subject] associations to operations on target objects' attributes.
     *
     * For {@link ResourceObjectAssociationDirectionType#OBJECT_TO_SUBJECT} entitlement direction, like `ri:groups`.
     */
    private void collectObjectOps(
            @NotNull EntitlementObjectsOperations objectsOperations,
            @NotNull Collection<IterableReferenceAttributeValue> iterableRefAttrValues,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {

        for (var iterableRefAttrValue : iterableRefAttrValues) {

            var refAttrName = iterableRefAttrValue.name();
            var isAddNotDelete = iterableRefAttrValue.isAddNotDelete();

            if (subjectCtx.getOperationContext() != null) {
                // todo this shouldn't be subjectCtx but the other one [viliam]
                subjectCtx.setAssociationShadowRef(iterableRefAttrValue.value().asObjectReferenceType());
            }

            ShadowReferenceAttributeDefinition refAttrDef = getReferenceAttributeDefinition(refAttrName);
            if (!isSimulatedObjectToSubject(refAttrDef) // The other direction is processed elsewhere (see transformToSubjectOps).
                    || !isVisible(refAttrDef, subjectCtx)
                    || !doesMatchSubjectDelineation(refAttrDef, subjectCtx)) {
                continue;
            }

            var simulationDefinition = refAttrDef.getSimulationDefinitionRequired();
            var binding = simulationDefinition.getPrimaryAttributeBinding(); // e.g. account "ri:dn" <-> group "ri:member"

            for (var participant : simulationDefinition.getObjects()) {

                // TODO clear the relevant parts of the context
                var entitlementCtx = subjectCtx.spawnForDefinition(participant.getObjectDefinition());

                // Should we take the name from "before" or "after" state of the shadow? It depends on the operation
                // and whether referential integrity is provided by midPoint or the resource.
                var sourceSubjectShadow = selectSourceShadow(
                        subjectShadowBefore, subjectShadowAfter, isAddNotDelete,
                        simulationDefinition.requiresExplicitReferentialIntegrity());

                // Take e.g. account's DN (e.g. "uid=joe,ou=people,dc=example,dc=com") ...
                PrismPropertyValue<?> bindingAttributeValue =
                        EntitlementUtils.getSingleValueRequired(
                                sourceSubjectShadow, binding.subjectSide(), refAttrName, errorCtx);

                // ... convert it into the definition of the object's binding attribute (e.g. "ri:member" on group) ...
                var objectBindingAttrDef = participant.getObjectAttributeDefinition(binding);
                var objectBindingAttrValue = objectBindingAttrDef.convertPrismValue(bindingAttributeValue);

                // ... and add/remove it to/from that binding attribute on a specific group!
                objectsOperations
                        .findOrCreate( // operations on specific group (e.g. "cn=wheel,ou=groups,dc=example,dc=com")
                                getEntitlementDiscriminator(iterableRefAttrValue, entitlementCtx, result), // e.g. group uuid
                                entitlementCtx)
                        .findOrCreateAttributeOperation( // operations on specific attribute (e.g. ri:member on that group)
                                objectBindingAttrDef,
                                simulationDefinition.getPrimaryBindingMatchingRuleLegacy())
                        .swallowValue(objectBindingAttrValue.clone(), isAddNotDelete);
            }
        }
    }

    /**
     * We are going to issue some operations on the entitlement object, based on the (usually naming) attribute
     * of the subject.
     *
     * But which subject shadow should we use - "before" or "after"?
     *
     * If the operation is ADD or REPLACE, we use "after" version of the shadow, because we want
     * to ensure that we add most-recent data to the entitlement object.
     *
     * If the operation is DELETE, we have two possibilities:
     *
     * - if the resource provides referential integrity, the entitlement object has already
     * new data (because the subject operation was already carried out), so we use "after";
     * - if the resource does not provide referential integrity, the entitlement object has OLD data
     * so we use "before" state.
     */
    private static ShadowType selectSourceShadow(
            ShadowType subjectShadowBefore, ShadowType subjectShadowAfter, boolean isAddNotDelete, boolean explicitIntegrity) {
        if (isAddNotDelete) {
            return subjectShadowAfter;
        } else {
            if (explicitIntegrity) {
                return subjectShadowBefore; // we must ensure the referential integrity
            } else {
                return subjectShadowAfter; // the resource has referential integrity assured by itself
            }
        }
    }

    private ResourceObjectDiscriminator getEntitlementDiscriminator(
            IterableReferenceAttributeValue iterableRefAttrValue,
            ProvisioningContext entitlementCtx,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        // this identification may be secondary-only
        var providedEntitlementIdentification = iterableRefAttrValue.value().getIdentification();
        var primaryIdentification =
                b.resourceObjectReferenceResolver.resolvePrimaryIdentifier(entitlementCtx, providedEntitlementIdentification, result);
        return ResourceObjectDiscriminator.of(primaryIdentification);
    }

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
     * 2. For the other direction, this is somehow different from {@link #collectObjectOps(EntitlementObjectsOperations,
     * Collection, ShadowType, ShadowType, OperationResult)}. We are not following the content of a shadow or delta, that is,
     * the associations that are going to be deleted. We are following the definitions, and looking for all existing entitlements.
     * This is to avoid the need to read the object that is going to be deleted. In fact, the object should not be there any more,
     * but we still want to clean up entitlement membership based on the information from the shadow - typically, the account DN.
     *
     * @see #collectObjectOps(EntitlementObjectsOperations, Collection, ShadowType, ShadowType, OperationResult)
     */
    @NotNull EntitlementObjectsOperations transformToObjectOpsOnSubjectDelete(
            @NotNull RepoShadow subjectRepoShadow, @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        EntitlementObjectsOperations objectsOperations = new EntitlementObjectsOperations();

        for (var refAttrDef : subjectCtx.getReferenceAttributeDefinitions()) {

            if (!isSimulatedObjectToSubject(refAttrDef) // subject-to-object dies with the subject shadow that is being deleted
                    || !isVisible(refAttrDef, subjectCtx)
                    || !requiresExplicitReferentialIntegrity(refAttrDef) // it is taken care by resource itself
                    || !doesMatchSubjectDelineation(refAttrDef, subjectCtx)) {
                continue;
            }

            var simulationDefinition = refAttrDef.getSimulationDefinitionRequired();
            var binding = simulationDefinition.getPrimaryAttributeBinding();

            // This will find all entitlement objects (e.g. groups) that are associated with the subject (e.g. account).
            var searchOp = new EntitlementObjectSearch<>(subjectCtx, simulationDefinition, binding, subjectRepoShadow.getBean());

            // Now, take the value of the subject's binding attribute (like "uid=joe,ou=people,dc=example,dc=com") ...
            PrismPropertyValue<?> subjectAttrValue = searchOp.getSubjectAttrValue();
            if (subjectAttrValue == null) {
                // This is very unfortunate situation. We wanted to delete all mentions of the subject in entitlement objects
                // (like groups), but the subject shadow has no identifier! Most probably the value is not cached.
                LOGGER.error("""
                                No value of binding attribute '{}' in the subject repo shadow:
                                {}
                                Simulation definition:
                                {}
                                Subject context:
                                {}

                                Most probably, the attribute is not cached. The recommended way is to mark it as a secondary identifier.""",
                        searchOp.getSubjectAttrName(), subjectRepoShadow.debugDump(1),
                        simulationDefinition.debugDump(1), subjectCtx.debugDump(1));
                throw new SchemaException(String.format(
                        "Cannot delete references for the subject, as the value of binding attribute '%s' "
                                + "is unknown or missing in the subject shadow.", searchOp.getSubjectAttrName()));
            }

            // .. and remove it from each of the entitlement objects (e.g. group) found for that account.
            searchOp.execute(
                    (entitlementObjectFound, lResult) -> {
                        // The group from which we want to remove the account being deleted. We are OK with the raw UCF-like object.
                        var entitlementObject = entitlementObjectFound.getInitialUcfResourceObject();
                        objectsOperations
                                .findOrCreate( // operations on specific group (e.g. "cn=wheel,ou=groups,dc=example,dc=com")
                                        ResourceObjectDiscriminator.of(entitlementObject.getBean()),
                                        subjectCtx.spawnForDefinition(entitlementObject.getObjectDefinition()))
                                .findOrCreateAttributeOperation( // operations on specific attribute (e.g. ri:member) on the group
                                        entitlementObject.getObjectDefinition().findSimpleAttributeDefinitionRequired(binding.objectSide()), // like ri:member
                                        simulationDefinition.getPrimaryBindingMatchingRuleLegacy())
                                .swallowValueToDelete(subjectAttrValue.clone()); // like "uid=joe,ou=people,dc=example,dc=com"

                        return true;
                    }, result);
        }

        LOGGER.trace("Entitlement objects operations on subject delete:\n{}", objectsOperations.debugDumpLazily(1));
        return objectsOperations;
    }
    //endregion

    //region Common
    private @NotNull ShadowReferenceAttributeDefinition getReferenceAttributeDefinition(@NotNull ItemName refAttrName)
            throws SchemaException {
        return subjectCtx.findReferenceAttributeDefinitionRequired(refAttrName);
    }

    private void checkNoReplace(@NotNull ShadowReferenceAttributesCollection attributesCollection) throws SchemaException {
        if (attributesCollection.hasReplace()) {
            LOGGER.error("Replace delta not supported for\nreference attribute modifications:\n{}\nin provisioning context:\n{}",
                    attributesCollection.debugDump(1), subjectCtx.debugDump(1));
            throw new SchemaException("Cannot perform replace delta for reference attribute");
        }
    }
    //endregion

    /** Operation to be executed on the _subject_. Keeps modification operations indexed by attribute names. */
    static class SubjectOperations implements DebugDumpable {

        private final Map<QName, Operation> operationMap = new HashMap<>();

        public Collection<Operation> getOperations() {
            return operationMap.values();
        }

        Operation get(QName attributeName) {
            return operationMap.get(attributeName);
        }

        void put(QName attributeName, Operation operation) {
            operationMap.put(attributeName, operation);
        }

        @NotNull <T> PropertyModificationOperation<T> findOrCreateOperation(
                ShadowSimpleAttributeDefinition<T> assocAttrDef, QName matchingRuleName) {
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

        @NotNull ReferenceModificationOperation findOrCreateOperation(ShadowReferenceAttributeDefinition refAttrDef) {
            ItemName attrName = refAttrDef.getItemName();
            ReferenceModificationOperation attributeOperation = (ReferenceModificationOperation) get(attrName);
            if (attributeOperation != null) {
                return attributeOperation;
            }
            var newOperation = new ReferenceModificationOperation(refAttrDef.createEmptyDelta());
            put(attrName, newOperation);
            return newOperation;
        }

        @Override
        public String debugDump(int indent) {
            return DebugUtil.debugDump(operationMap, indent);
        }
    }

    /** Operations to be executed on entitlement _objects_. */
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
}
