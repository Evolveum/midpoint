/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.errorhandling;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager;
import com.evolveum.midpoint.provisioning.api.ShadowState;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Component
public class ObjectNotFoundHandler extends HardErrorHandler {

    private static final String OP_DISCOVERY = ObjectNotFoundHandler.class + ".discovery";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectNotFoundHandler.class);

    @Autowired private ShadowManager shadowManager;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private Clock clock;

    @Override
    public PrismObject<ShadowType> handleGetError(ProvisioningContext ctx,
            PrismObject<ShadowType> repositoryShadow, GetOperationOptions rootOptions, Exception cause,
            Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException,
            CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), rootOptions)) {
            discoverDeletedShadow(ctx, repositoryShadow, cause, task, parentResult);
        }

        if (repositoryShadow != null) {

            if (ShadowUtil.isExists(repositoryShadow.asObjectable())) {
                // This is some kind of reality mismatch. We obviously have shadow that is supposed
                // to be alive (exists=true). But it does not exist on resource.
                // This is NOT gestation quantum state, as that is handled directly in ShadowCache.
                // This may be "lost shadow" - shadow which exists but the resource object has disappeared without trace.
                // Or this may be a corpse - quantum state that has just collapsed to to tombstone.
                // Either way, it should be safe to set exists=false.
                LOGGER.trace("Setting {} as tombstone. This may be a quntum state collapse. Or maybe a lost shadow.", repositoryShadow);
                repositoryShadow = shadowManager.markShadowTombstone(repositoryShadow, parentResult);
            } else {
                // We always want to return repository shadow it such shadow is available.
                // The shadow may be dead, or it may be marked as "not exists", but we want
                // to return something if shadow exists in the repo. Otherwise model may
                // unlink the shadow or otherwise "forget" about it.
                LOGGER.debug("Shadow {} not found on the resource. However still have it in the repository. Therefore returning repository version.", repositoryShadow);
            }
            parentResult.setStatus(OperationResultStatus.HANDLED_ERROR);
            return repositoryShadow;
        }
        return super.handleGetError(ctx, repositoryShadow, rootOptions, cause, task, parentResult);
    }

    @Override
    public OperationResultStatus handleModifyError(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            Collection<? extends ItemDelta> modifications, ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), options)) {
            discoverDeletedShadow(ctx, repoShadow, cause, task, parentResult);
        }

        return super.handleModifyError(ctx, repoShadow, modifications, options, opState, cause, failedOperationResult, task, parentResult);
    }

    @Override
    public OperationResultStatus handleDeleteError(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow, ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationResult> opState, Exception cause,
            OperationResult failedOperationResult, Task task, OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), options)) {
            discoverDeletedShadow(ctx, repoShadow, cause, task, parentResult);
        }

        // Error deleting shadow because the shadow is already deleted. This means someone has done our job already.
        failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
        opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
        return OperationResultStatus.HANDLED_ERROR;
    }

    private void discoverDeletedShadow(ProvisioningContext ctx, PrismObject<ShadowType> repositoryShadow,
            Exception cause, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ShadowState shadowState = shadowCaretaker.determineShadowState(ctx, repositoryShadow, clock.currentTimeXMLGregorianCalendar());
        if (shadowState != ShadowState.LIFE) {
            // Do NOT do discovery of shadow that can legally not exist. This is no discovery.
            // We already know that the object are supposed not to exist yet or to dead already.
            LOGGER.trace("Skipping discovery of shadow {} becasue it is {}, we expect that it might not exist", repositoryShadow, shadowState);
            return;
        }

        // TODO: this probably should NOT be a subresult of parentResult. We probably want new result (and maybe also task) here.
        OperationResult result = parentResult.createSubresult(OP_DISCOVERY);

        LOGGER.debug("DISCOVERY: discovered deleted shadow {}", repositoryShadow);

        repositoryShadow = shadowManager.markShadowTombstone(repositoryShadow, result);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setResource(ctx.getResource().asPrismObject());
        change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));
        change.setObjectDelta(repositoryShadow.createDeleteDelta());
        change.setOldShadow(repositoryShadow);
        // Current shadow is a tombstone. This means that the object was deleted. But we need current shadow here.
        // Otherwise the synchronization situation won't be updated because SynchronizationService could think that
        // there is not shadow at all.
        change.setCurrentShadow(repositoryShadow);
        changeNotificationDispatcher.notifyChange(change, task, result);

        result.computeStatus();
    }

    @Override
    protected void throwException(Exception cause, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result)
            throws ObjectNotFoundException {
        recordCompletionError(cause, opState, result);
        if (cause instanceof ObjectNotFoundException) {
            throw (ObjectNotFoundException)cause;
        } else {
            throw new ObjectNotFoundException(cause.getMessage(), cause);
        }
    }

}
