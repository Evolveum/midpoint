/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.prism.Referencable.getOid;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowAssociationsCollection;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

/**
 * Contains entitlements-related methods. (Or should that methods be distributed more closely to their clients?)
 */
@Component
@Experimental
class EntitlementsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(EntitlementsHelper.class);

    @Autowired ShadowFinder shadowFinder;

    /**
     * Makes sure that all the entitlements have identifiers in them so this is
     * usable by the {@link ResourceObjectConverter}.
     */
    void provideEntitlementsIdentifiers(
            ProvisioningContext ctx, ResourceObject resourceObjectToAdd, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        provideEntitlementIdentifiers(
                ctx,
                ShadowAssociationsCollection.ofShadow(resourceObjectToAdd.getBean()),
                resourceObjectToAdd.toString(),
                result);
    }

    /**
     * Makes sure that all the entitlements have identifiers in them so this is
     * usable by the ResourceObjectConverter.
     */
    void provideEntitlementsIdentifiers(
            ProvisioningContext ctx,
            Collection<? extends ItemDelta<?, ?>> modifications,
            String desc,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        for (ItemDelta<?, ?> modification : modifications) {
            provideEntitlementIdentifiers(
                    ctx,
                    ShadowAssociationsCollection.ofDelta(modification),
                    desc,
                    result);
        }
    }

    private void provideEntitlementIdentifiers(
            ProvisioningContext ctx,
            ShadowAssociationsCollection associationsCollection,
            String desc,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException {

        for (var iterableAssociationValue : associationsCollection.getAllValues()) {
            var associationPcv = iterableAssociationValue.associationPcv();

            var identifiersContainer = associationPcv.findContainer(ShadowAssociationValueType.F_IDENTIFIERS);
            if (identifiersContainer == null) {
                // we'll create and provide it below
            } else if (identifiersContainer.isEmpty()) {
                associationPcv.removeContainer(ShadowAssociationValueType.F_IDENTIFIERS);
                // we'll create and provide it below
            } else {
                continue; // there are identifiers already; no need to do anything here
            }

            LOGGER.trace("Going to provide identifiers to shadow association: {}", iterableAssociationValue);
            String entitlementOid =
                    MiscUtil.requireNonNull(
                            getOid(associationPcv.asContainerable().getShadowRef()),
                            () -> "No identifiers and no OID specified in entitlements association: " + iterableAssociationValue);

            RepoShadow entitlementShadow;
            try {
                entitlementShadow = shadowFinder.getRepoShadow(ctx, entitlementOid, result);
            } catch (ObjectNotFoundException e) {
                throw e.wrap("Couldn't resolve entitlement association OID %s in association %s in %s".formatted(
                        entitlementOid, iterableAssociationValue.name(), desc));
            }

            associationPcv.add(entitlementShadow.getIdentifiersAsContainer());
        }
    }
}
