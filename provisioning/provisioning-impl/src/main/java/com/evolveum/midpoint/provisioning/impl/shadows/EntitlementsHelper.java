/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.schema.util.ShadowUtil.getAllIdentifiers;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
     * usable by the ResourceObjectConverter.
     */
    void provideEntitlementsIdentifiers(
            ProvisioningContext ctx, ShadowType resourceObjectToAdd, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        try {
            // Why using visitor? (maybe because of the similarity to deltas where the visitor is appropriate)
            String desc = resourceObjectToAdd.toString();
            //noinspection unchecked
            resourceObjectToAdd.asPrismObject().accept(
                    (visitable) ->
                            provideEntitlementIdentifiers(
                                    ctx, (PrismContainerValue<ShadowAssociationType>) visitable, desc, result),
                    ItemPath.create(ShadowType.F_ASSOCIATION, null),
                    false);
        } catch (LocalTunnelException e) {
            e.unwrapAndRethrow();
            throw new AssertionError();
        }
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
        try {
            //noinspection unchecked
            ItemDeltaCollectionsUtil.accept(modifications,
                    (visitable) ->
                            provideEntitlementIdentifiers(
                                    ctx, (PrismContainerValue<ShadowAssociationType>) visitable, desc, result),
                    ItemPath.create(ShadowType.F_ASSOCIATION, null),
                    false);
        } catch (LocalTunnelException e) {
            e.unwrapAndRethrow();
            throw new AssertionError();
        }
    }

    private void provideEntitlementIdentifiers(
            ProvisioningContext ctx,
            PrismContainerValue<ShadowAssociationType> association,
            String desc,
            OperationResult result) throws LocalTunnelException {
        try {
            PrismContainer<Containerable> identifiersContainer = association.findContainer(ShadowAssociationType.F_IDENTIFIERS);
            if (identifiersContainer != null && !identifiersContainer.isEmpty()) {
                // We already have identifiers here
                return;
            }
            ShadowAssociationType associationBean = association.asContainerable();
            LOGGER.trace("Shadow association: {}, class: {}", associationBean.getName(), associationBean.getName().getClass());
            String entitlementOid =
                    MiscUtil.requireNonNull(
                            getOid(associationBean.getShadowRef()),
                            () -> "No identifiers and no OID specified in entitlements association " + association);
            PrismObject<ShadowType> entitlementShadow;
            try {
                entitlementShadow = shadowFinder.getShadow(entitlementOid, result);
            } catch (ObjectNotFoundException e) {
                throw e.wrap("Couldn't resolve entitlement association OID in " + association + " in " + desc);
            }
            ctx.applyAttributesDefinition(entitlementShadow);
            transplantIdentifiers(association, entitlementShadow);
        } catch (SchemaException | ObjectNotFoundException | ConfigurationException e) {
            throw new LocalTunnelException(e);
        }
    }

    private void transplantIdentifiers(
            PrismContainerValue<ShadowAssociationType> association,
            PrismObject<ShadowType> repoShadow) throws SchemaException {
        PrismContainer<ShadowAttributesType> identifiersContainer =
                association.findContainer(ShadowAssociationType.F_IDENTIFIERS);
        if (identifiersContainer == null) {
            ResourceAttributeContainer origContainer = ShadowUtil.getAttributesContainer(repoShadow);
            identifiersContainer =
                    ObjectFactory.createResourceAttributeContainer(
                            ShadowAssociationType.F_IDENTIFIERS, origContainer.getDefinition());
            association.add(identifiersContainer);
        }
        for (ResourceAttribute<?> identifier : getAllIdentifiers(repoShadow)) {
            identifiersContainer.add(identifier.clone());
        }
    }

    private static class LocalTunnelException extends RuntimeException {
        private LocalTunnelException(Throwable cause) {
            super(cause);
        }

        private void unwrapAndRethrow()
                throws SchemaException, ObjectNotFoundException, ConfigurationException {
            Throwable cause = getCause();
            if (cause instanceof SchemaException) {
                throw (SchemaException) cause;
            } else if (cause instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) cause;
            } else if (cause instanceof ConfigurationException) {
                throw (ConfigurationException) cause;
            } else {
                throw SystemException.unexpected(cause);
            }
        }
    }
}
