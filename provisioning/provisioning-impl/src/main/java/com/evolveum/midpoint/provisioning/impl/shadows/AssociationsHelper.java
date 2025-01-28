/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowAssociationsCollection;
import com.evolveum.midpoint.schema.util.ShadowAssociationsCollection.IterableAssociationValue;
import com.evolveum.midpoint.schema.util.ShadowReferenceAttributesCollection;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static com.evolveum.midpoint.provisioning.impl.shadows.RepoShadowWithState.ShadowState.EXISTING;

/**
 * Contains associations-related methods at the *shadows* level.
 */
@Component
@Experimental
class AssociationsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationsHelper.class);

    private static final String OP_CONVERT_REFERENCE_ATTRIBUTES_TO_ASSOCIATIONS =
            AssociationsHelper.class.getName() + ".convertReferenceAttributesToAssociations";

    @Autowired ShadowFinder shadowFinder;

    /**
     * Makes sure that all object references (in associations and in reference attributes) have identifiers in them.
     * This is necessary for the actual resource-level operations.
     */
    void provideObjectsIdentifiersToSubject(
            ProvisioningContext ctx, ResourceObjectShadow objectToAdd, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {
        provideObjectsIdentifiersToAssociations(
                ctx,
                ShadowAssociationsCollection.ofShadow(objectToAdd.getBean()),
                objectToAdd.toString(),
                result);
        provideObjectsIdentifiersToReferenceAttributes(
                ctx,
                ShadowReferenceAttributesCollection.ofShadow(objectToAdd.getBean()),
                objectToAdd.toString(),
                result);
    }

    /**
     * Makes sure that all object references (in associations and in reference attributes) have identifiers in them.
     * This is necessary for the actual resource-level operations.
     */
    void provideObjectsIdentifiersToDelta(
            ProvisioningContext ctx,
            Collection<? extends ItemDelta<?, ?>> modifications,
            String desc,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {
        for (ItemDelta<?, ?> modification : modifications) {
            provideObjectsIdentifiersToAssociations(
                    ctx,
                    ShadowAssociationsCollection.ofDelta(modification),
                    desc,
                    result);
            provideObjectsIdentifiersToReferenceAttributes(
                    ctx,
                    ShadowReferenceAttributesCollection.ofDelta(modification),
                    desc,
                    result);
        }
    }

    private void provideObjectsIdentifiersToAssociations(
            ProvisioningContext ctx,
            ShadowAssociationsCollection associationsCollection,
            String desc,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {

        for (var iterableAssocValue : associationsCollection.getAllIterableValues()) {
            var assocValue = iterableAssocValue.associationValue();
            var shadowRefAttrsCollection = ShadowReferenceAttributesCollection.ofAssociationValue(assocValue);
            provideObjectsIdentifiersToReferenceAttributes(ctx, shadowRefAttrsCollection, desc, result);
        }
    }

    private void provideObjectsIdentifiersToReferenceAttributes(
            ProvisioningContext ctx,
            ShadowReferenceAttributesCollection referenceAttributesCollection,
            String desc,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {

        for (var iterableRefAttrValue : referenceAttributesCollection.getAllIterableValues()) {
            var refAttrValue = iterableRefAttrValue.value();
            var embeddedShadow = refAttrValue.getShadowIfPresent();
            if (embeddedShadow != null && embeddedShadow.getAttributesContainer().size() > 0) {
                continue;
            }

            LOGGER.trace("Going to provide identifiers to association reference value: {}", iterableRefAttrValue);
            String objectOid =
                    MiscUtil.requireNonNull(
                            refAttrValue.getOid(),
                            () -> "No identifiers and no OID specified in association reference attribute: %s in %s".formatted(
                                    iterableRefAttrValue, desc));

            try {
                var repoShadow = shadowFinder.getRepoShadow(ctx, objectOid, result);
                try {
                    ctx.spawnForDefinition(repoShadow.getObjectDefinition())
                            .computeAndUpdateEffectiveMarksAndPolicies(repoShadow, EXISTING, result);
                } catch (CommunicationException | SecurityViolationException e) {
                    throw new SystemException(
                            "Couldn't compute effective marks and policies for %s: %s".formatted(repoShadow, e.getMessage()),
                            e);
                }
                refAttrValue.setShadow(repoShadow);
            } catch (ObjectNotFoundException e) {
                throw e.wrap("Couldn't resolve object reference OID %s in %s".formatted(objectOid, desc));
            }
        }
    }

    /**
     * Converts associations (high-level concept) into reference attributes (low-level concept) that implement them.
     *
     * Note that this is something that must be undone before storing the shadow in a pending operation, if applicable.
     */
    void convertAssociationsToReferenceAttributes(ResourceObjectShadow objectToAdd) throws SchemaException {
        var iterator = ShadowAssociationsCollection.ofShadow(objectToAdd.getBean()).iterator();
        var attrsContainer = objectToAdd.getAttributesContainer();
        while (iterator.hasNext()) {
            var iterableAssocValue = iterator.next();
            var assocValue = iterableAssocValue.associationValue();
            var assocDef = assocValue.getDefinitionRequired();
            var refAttrName = assocDef.getReferenceAttributeDefinition().getItemName();
            attrsContainer
                    .findOrCreateReferenceAttribute(refAttrName)
                    .add(assocValue.toReferenceAttributeValue());
        }
    }

    void convertAssociationDeltasToReferenceAttributeDeltas(Collection<? extends ItemDelta<?, ?>> modifications)
            throws SchemaException {
        for (var modification : List.copyOf(modifications)) {
            var iterator = ShadowAssociationsCollection.ofDelta(modification).iterator();
            while (iterator.hasNext()) {
                //noinspection unchecked
                ((Collection<ItemDelta<?, ?>>) modifications).add(
                        createRefAttrDelta(iterator.next()));
            }
        }
    }

    private static ReferenceDelta createRefAttrDelta(IterableAssociationValue iterableAssocValue)
            throws SchemaException {
        var assocValue = iterableAssocValue.associationValue();
        var assocDef = assocValue.getDefinitionRequired();
        var refAttrDelta = assocDef.getReferenceAttributeDefinition().createEmptyDelta();
        var refAttrValue = assocValue.toReferenceAttributeValue();
        if (iterableAssocValue.isAddNotDelete()) {
            refAttrDelta.addValueToAdd(refAttrValue);
        } else {
            refAttrDelta.addValueToDelete(refAttrValue);
        }
        return refAttrDelta;
    }

    /**
     * Converts reference attributes to associations.
     *
     * Removes the values of reference attributes that were converted into association values.
     * Removes all such attributes that are reduced to zero values.
     *
     * Assumptions: This method will not touch the resource. Hence, all the necessary information must be provided by the caller
     * or be present in the repository:
     *
     * . Each reference attribute value has either the full shadow (including kind and intent), or at least the shadow OID.
     * . For rich associations, either the full object must be present (with the above conditions being held for that object),
     * or the repository must contain the cached shadow for the associated object.
     */
    void convertReferenceAttributesToAssociations(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType shadow,
            @NotNull ResourceObjectDefinition definition,
            @NotNull OperationResult parentResult)
            throws SchemaException {

        var result = parentResult.subresult(OP_CONVERT_REFERENCE_ATTRIBUTES_TO_ASSOCIATIONS)
                .setMinor()
                .build();
        try {
            var attributesContainer = ShadowUtil.getAttributesContainerRequired(shadow);
            var referenceAttributes = attributesContainer.getReferenceAttributes();

            for (var refAttr : referenceAttributes) {
                var refAttrName = refAttr.getDefinition().getItemName();

                // We do the loading even if there are no associations defined to preserve the post-conditions
                // that all reference attributes are resolved, i.e., they contain resolved shadows (if at all possible).
                // We may later avoid this if we want to optimize the performance. See also ReturnedShadowValidityChecker.
                loadShadowsForReferenceAttributeValuesIfNeeded(ctx, refAttr, result);

                var assocDefs = definition.getAssociationDefinitionsFor(refAttrName);
                if (assocDefs.isEmpty()) {
                    LOGGER.trace("Not converting reference attribute '{}' as it has no associations attached", refAttrName);
                    continue;
                }
                LOGGER.trace("Converting reference attribute '{}' to {} association(s)", refAttrName, assocDefs.size());
                var refAttrValuesIterator = refAttr.getValues().iterator();
                var reduced = false;
                values: while (refAttrValuesIterator.hasNext()) {
                    var refAttrValue = (ShadowReferenceAttributeValue) refAttrValuesIterator.next();
                    for (var assocDef : assocDefs) {
                        if (convertReferenceAttributeValueToAssociationValueIfPossible(
                                ctx, shadow, refAttrValue, assocDef, result)) {
                            refAttrValuesIterator.remove();
                            reduced = true;
                            continue values;
                        }
                    }
                }
                if (reduced && refAttr.hasNoValues()) {
                    attributesContainer.removeReference(refAttrName);
                }
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }

        // We need to store the result in order for ReturnedShadowValidityChecker know whether the shadow is valid or not.
        assert !shadow.isImmutable();
        ProvisioningUtil.storeFetchResultIfApplicable(shadow, result);
    }

    private void loadShadowsForReferenceAttributeValuesIfNeeded(
            @NotNull ProvisioningContext ctx, @NotNull ShadowReferenceAttribute refAttr, @NotNull OperationResult result) {
        for (var refAttrValue : refAttr.getAttributeValues()) {
            loadShadowForReferenceAttributeValueIfNeeded(ctx, refAttr.getElementName(), refAttrValue, result);
        }
    }

    private void loadShadowForReferenceAttributeValueIfNeeded(
            @NotNull ProvisioningContext ctx,
            @NotNull QName refAttrName,
            @NotNull ShadowReferenceAttributeValue refAttrValue,
            @NotNull OperationResult result) {
        if (refAttrValue.getObject() == null) {
            var oid = refAttrValue.getOidRequired();
            try {
                // Operation execution is stored in a separate table. We don't need them for referenced shadows.
                var options = GetOperationOptionsBuilder.create()
                        .item(ShadowType.F_OPERATION_EXECUTION)
                        .retrieve(RetrieveOption.EXCLUDE)
                        .build();
                var rawRepoShadow = shadowFinder.getRepoShadow(oid, options, result);
                var repoShadow = ctx.adoptRawRepoShadow(rawRepoShadow);
                ctx.computeAndUpdateEffectiveMarksAndPolicies(repoShadow, EXISTING, result);
                refAttrValue.setShadow(repoShadow);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(
                        LOGGER, "Couldn't retrieve repo shadow for the value {} of reference attribute {}", e,
                        refAttrValue, refAttrName);
                // We assume the exception is already recorded in the operation result, so it will be visible
                // in the fetchResult in the shadow.
            }
        }
    }

    /**
     * Tries to convert reference attribute value to association value.
     */
    private boolean convertReferenceAttributeValueToAssociationValueIfPossible(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType shadow,
            @NotNull ShadowReferenceAttributeValue refAttrValue,
            @NotNull ShadowAssociationDefinition assocDef,
            @NotNull OperationResult result)
            throws SchemaException {

        LOGGER.trace("Considering conversion of reference attribute value {} into {}", refAttrValue, assocDef);

        var refAttrShadow = refAttrValue.getShadowIfPresent();
        if (refAttrShadow == null) {
            LOGGER.warn("Ignoring unresolvable reference attribute value {} in {}", refAttrValue, shadow);
            return false;
        }

        var participantsMap = assocDef.getObjectParticipants();
        for (QName objectName : participantsMap.keySet()) {
            var expectedObjectTypes = participantsMap.get(objectName);
            LOGGER.trace("Checking participating object {}; expecting: {}", objectName, expectedObjectTypes);
            AbstractShadow objectShadow;
            if (assocDef.isComplex()) {
                var objectRefAttrValue = refAttrShadow.getReferenceAttributeSingleValue(objectName);
                if (objectRefAttrValue == null) {
                    LOGGER.trace("Reference attribute {} not found, skipping the check", objectName);
                    continue;
                }
                loadShadowForReferenceAttributeValueIfNeeded(ctx, objectName, objectRefAttrValue, result);
                objectShadow = objectRefAttrValue.getShadowIfPresent();
                if (objectShadow == null) {
                    LOGGER.trace("Object for the reference attribute {} value {} could not be found, ignoring the value",
                            objectName, objectRefAttrValue);
                    continue;
                }
            } else {
                objectShadow = refAttrShadow;
            }
            if (expectedObjectTypes.stream().anyMatch(
                    type -> type.matches(objectShadow.getBean()))) {
                LOGGER.trace("Shadow {} accepted for participating object {}", objectShadow, objectName);
            } else {
                LOGGER.trace("Shadow {} NOT accepted for participating object {}, rejecting the whole value",
                        objectShadow, objectName);
                return false;
            }
        }

        ShadowUtil
                .getOrCreateAssociationsContainer(shadow)
                .findOrCreateAssociation(assocDef.getItemName())
                .createNewValue()
                .fillFromReferenceAttributeValue(refAttrValue);

        return true;
    }
}
