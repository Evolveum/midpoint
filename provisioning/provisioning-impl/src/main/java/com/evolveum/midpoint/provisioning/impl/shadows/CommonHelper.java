/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.RunAsCapabilityType;

/**
 * Helper code needed by the whole "shadow cache" package.
 *
 * TODO better name
 */
@Experimental
@Component
class CommonHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CommonHelper.class);

    @Autowired protected ShadowManager shadowManager;
    @Autowired private EventDispatcher eventDispatcher;
    @Autowired private ProvisioningContextFactory ctxFactory;

    ConnectorOperationOptions createConnectorOperationOptions(
            ProvisioningContext ctx, ProvisioningOperationOptions options, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
        if (options == null) {
            return null;
        }
        String runAsAccountOid = options.getRunAsAccountOid();
        if (runAsAccountOid == null) {
            return null;
        }
        RunAsCapabilityType capRunAs = ctx.getCapability(RunAsCapabilityType.class); // TODO check it's enabled!
        if (capRunAs == null) {
            LOGGER.trace("Operation runAs requested, but resource does not have the capability. Ignoring runAs");
            return null;
        }
        PrismObject<ShadowType> runAsShadow;
        try {
            runAsShadow = shadowManager.getShadow(runAsAccountOid, result);
        } catch (ObjectNotFoundException e) {
            throw new ConfigurationException("Requested non-existing 'runAs' shadow", e);
        }
        ProvisioningContext runAsCtx = ctxFactory.createForShadow(runAsShadow.asObjectable(), ctx.getResource(), ctx.getTask());
        runAsCtx.applyAttributesDefinition(runAsShadow);
        ResourceObjectIdentification runAsIdentification =
                ResourceObjectIdentification.createFromShadow(
                        runAsCtx.getObjectDefinitionRequired(), runAsShadow.asObjectable());
        ConnectorOperationOptions connOptions = new ConnectorOperationOptions();
        LOGGER.trace("RunAs identification: {}", runAsIdentification);
        connOptions.setRunAsIdentification(runAsIdentification);
        return connOptions;
    }

    void handleErrorHandlerException(
            ProvisioningContext ctx,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            ObjectDelta<ShadowType> delta,
            String message,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ObjectAlreadyExistsException, ExpressionEvaluationException {
        // Error handler had re-thrown the exception. We will throw the exception later. But first we need to record changes in opState.
        shadowManager.recordOperationException(ctx, opState, delta, result);

        ShadowType shadow;
        if (delta.isAdd()) {
            // This is more precise. Besides, there is no repo shadow in some cases (e.g. adding protected shadow).
            shadow = delta.getObjectToAdd().asObjectable();
        } else {
            shadow = opState.getRepoShadow();
        }
        ResourceOperationDescription operationDescription =
                ProvisioningUtil.createResourceFailureDescription(shadow, ctx.getResource(), delta, message);
        eventDispatcher.notifyFailure(operationDescription, ctx.getTask(), result);
    }
}
