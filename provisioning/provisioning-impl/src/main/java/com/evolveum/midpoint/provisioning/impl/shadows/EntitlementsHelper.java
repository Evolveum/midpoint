/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowAssociationsCollection;
import com.evolveum.midpoint.schema.util.ShadowAssociationsCollection.IterableAssociationValue;
import com.evolveum.midpoint.schema.util.ShadowReferenceAttributesCollection;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Contains associations-related methods at the *shadows* level.
 *
 * NOTE: We use the historical name of "entitlements" here. This may change sometime in the future.
 */
@Component
@Experimental
class EntitlementsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(EntitlementsHelper.class);

    @Autowired ShadowFinder shadowFinder;

    /**
     * Makes sure that all object references (in associations and in reference attributes) have identifiers in them.
     * This is necessary for the actual resource-level operations.
     */
    void provideEntitlementsIdentifiersToObject(
            ProvisioningContext ctx, ResourceObjectShadow objectToAdd, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        provideEntitlementIdentifiersToAssociations(
                ctx,
                ShadowAssociationsCollection.ofShadow(objectToAdd.getBean()),
                objectToAdd.toString(),
                result);
        provideEntitlementIdentifiersToReferenceAttributes(
                ctx,
                ShadowReferenceAttributesCollection.ofShadow(objectToAdd.getBean()),
                objectToAdd.toString(),
                result);
    }

    /**
     * Makes sure that all object references (in associations and in reference attributes) have identifiers in them.
     * This is necessary for the actual resource-level operations.
     */
    void provideEntitlementsIdentifiersToDelta(
            ProvisioningContext ctx,
            Collection<? extends ItemDelta<?, ?>> modifications,
            String desc,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        for (ItemDelta<?, ?> modification : modifications) {
            provideEntitlementIdentifiersToAssociations(
                    ctx,
                    ShadowAssociationsCollection.ofDelta(modification),
                    desc,
                    result);
            provideEntitlementIdentifiersToReferenceAttributes(
                    ctx,
                    ShadowReferenceAttributesCollection.ofDelta(modification),
                    desc,
                    result);
        }
    }

    private void provideEntitlementIdentifiersToAssociations(
            ProvisioningContext ctx,
            ShadowAssociationsCollection associationsCollection,
            String desc,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException {

        for (var iterableAssocValue : associationsCollection.getAllIterableValues()) {
            var assocValue = iterableAssocValue.associationValue();
            var shadowRefAttrsCollection = ShadowReferenceAttributesCollection.ofAssociationValue(assocValue);
            provideEntitlementIdentifiersToReferenceAttributes(ctx, shadowRefAttrsCollection, desc, result);
        }
    }

    private void provideEntitlementIdentifiersToReferenceAttributes(
            ProvisioningContext ctx,
            ShadowReferenceAttributesCollection referenceAttributesCollection,
            String desc,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException {

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
                refAttrValue.setShadow(
                        shadowFinder.getRepoShadow(ctx, objectOid, result));
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
}
