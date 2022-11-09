/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
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

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private PrismContext prismContext;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;

    /**
     * Makes sure that all the entitlements have identifiers in them so this is
     * usable by the ResourceObjectConverter.
     */
    void preprocessEntitlements(final ProvisioningContext ctx, final PrismObject<ShadowType> resourceObjectToAdd,
            final OperationResult result) throws SchemaException, ObjectNotFoundException,
            ConfigurationException, CommunicationException, ExpressionEvaluationException {
        try {
            resourceObjectToAdd.accept(
                    (visitable) -> {
                        try {
                            //noinspection unchecked
                            preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>) visitable,
                                    resourceObjectToAdd.toString(), result);
                        } catch (SchemaException | ObjectNotFoundException | ConfigurationException
                                | CommunicationException | ExpressionEvaluationException e) {
                            throw new TunnelException(e);
                        }
                    },
                    ItemPath.create(ShadowType.F_ASSOCIATION, null), false);
        } catch (TunnelException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SchemaException) {
                throw (SchemaException) cause;
            } else if (cause instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) cause;
            } else if (cause instanceof ConfigurationException) {
                throw (ConfigurationException) cause;
            } else if (cause instanceof CommunicationException) {
                throw (CommunicationException) cause;
            } else if (cause instanceof ExpressionEvaluationException) {
                throw (ExpressionEvaluationException) cause;
            } else {
                throw new SystemException("Unexpected exception " + cause, cause);
            }
        }
    }

    /**
     * Makes sure that all the entitlements have identifiers in them so this is
     * usable by the ResourceObjectConverter.
     */
    void preprocessEntitlements(final ProvisioningContext ctx,
            Collection<? extends ItemDelta> modifications, final String desc, final OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException,
            CommunicationException, ExpressionEvaluationException {
        try {
            ItemDeltaCollectionsUtil.accept(modifications,
                    (visitable) -> {
                        try {
                            preprocessEntitlement(ctx, (PrismContainerValue<ShadowAssociationType>) visitable, desc,
                                    result);
                        } catch (SchemaException | ObjectNotFoundException | ConfigurationException
                                | CommunicationException | ExpressionEvaluationException e) {
                            throw new TunnelException(e);
                        }
                    },
                    ItemPath.create(ShadowType.F_ASSOCIATION, null), false);
        } catch (TunnelException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SchemaException) {
                throw (SchemaException) cause;
            } else if (cause instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) cause;
            } else if (cause instanceof ConfigurationException) {
                throw (ConfigurationException) cause;
            } else if (cause instanceof CommunicationException) {
                throw (CommunicationException) cause;
            } else if (cause instanceof ExpressionEvaluationException) {
                throw (ExpressionEvaluationException) cause;
            } else {
                throw new SystemException("Unexpected exception " + cause, cause);
            }
        }
    }

    private void preprocessEntitlement(ProvisioningContext ctx,
            PrismContainerValue<ShadowAssociationType> association, String desc, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException,
            CommunicationException, ExpressionEvaluationException {
        PrismContainer<Containerable> identifiersContainer = association
                .findContainer(ShadowAssociationType.F_IDENTIFIERS);
        if (identifiersContainer != null && !identifiersContainer.isEmpty()) {
            // We already have identifiers here
            return;
        }
        ShadowAssociationType associationType = association.asContainerable();
        LOGGER.debug("###Shadow association: {}, class: {}", associationType.getName(), associationType.getName().getClass());
        if (associationType.getShadowRef() == null
                || StringUtils.isEmpty(associationType.getShadowRef().getOid())) {
            throw new SchemaException(
                    "No identifiers and no OID specified in entitlements association " + association);
        }
        PrismObject<ShadowType> repoShadow;
        try {
            repoShadow = repositoryService.getObject(ShadowType.class,
                    associationType.getShadowRef().getOid(), null, result);
        } catch (ObjectNotFoundException e) {
            throw e.wrap("Couldn't resolve entitlement association OID in " + association + " in " + desc);
        }
        ctx.applyAttributesDefinition(repoShadow);
        transplantIdentifiers(association, repoShadow);
    }

    private void transplantIdentifiers(PrismContainerValue<ShadowAssociationType> association,
            PrismObject<ShadowType> repoShadow) throws SchemaException {
        PrismContainer<ShadowAttributesType> identifiersContainer = association
                .findContainer(ShadowAssociationType.F_IDENTIFIERS);
        if (identifiersContainer == null) {
            ResourceAttributeContainer origContainer = ShadowUtil.getAttributesContainer(repoShadow);
            identifiersContainer = ObjectFactory.createResourceAttributeContainer(ShadowAssociationType.F_IDENTIFIERS,
                    origContainer.getDefinition());
            association.add(identifiersContainer);
        }
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(repoShadow);
        for (ResourceAttribute<?> identifier : identifiers) {
            identifiersContainer.add(identifier.clone());
        }
        Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil
                .getSecondaryIdentifiers(repoShadow);
        for (ResourceAttribute<?> identifier : secondaryIdentifiers) {
            identifiersContainer.add(identifier.clone());
        }
    }

}
