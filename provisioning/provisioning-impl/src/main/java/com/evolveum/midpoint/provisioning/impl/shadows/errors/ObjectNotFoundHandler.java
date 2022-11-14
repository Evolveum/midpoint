/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.DeleteOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.ModifyOperationState;
import com.evolveum.midpoint.task.api.TaskUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.LIVE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.REAPING;

@Component
class ObjectNotFoundHandler extends HardErrorHandler {

    private static final String OP_DISCOVERY = ObjectNotFoundHandler.class + ".discovery";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectNotFoundHandler.class);

    @Autowired private ShadowManager shadowManager;
    @Autowired private ShadowCaretaker shadowCaretaker;

    @Override
    public ShadowType handleGetError(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repositoryShadow,
            @NotNull Exception cause,
            @NotNull OperationResult failedOperationResult,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        // We do this before marking shadow as tombstone.
        ShadowLifecycleStateType stateBefore = shadowCaretaker.determineShadowState(ctx, repositoryShadow);

        ShadowType shadowToReturn = markShadowTombstoneIfApplicable(ctx, repositoryShadow, result);

        if (ctx.getErrorReportingMethod() == FetchErrorReportingMethodType.FORCED_EXCEPTION) {
            LOGGER.debug("Got {} but 'forced exception' mode is selected. Will rethrow it.", cause.getClass().getSimpleName());
            throwException(cause, null, result);
            throw new AssertionError("not reached");
        }

        notifyAboutMissingObjectIfApplicable(ctx, repositoryShadow, stateBefore, result);
        failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
        return shadowToReturn;
    }

    private ShadowType markShadowTombstoneIfApplicable(
            ProvisioningContext ctx, ShadowType repositoryShadow, OperationResult result) throws SchemaException {

        if (!ShadowUtil.isExists(repositoryShadow)) {
            LOGGER.debug("Shadow {} not found on the resource. However still have it in the repository. "
                    + "Therefore returning repository version.", repositoryShadow);
            return repositoryShadow;
        }

        // This is some kind of reality mismatch. We obviously have shadow that is supposed to be alive (exists=true). But it does
        // not exist on resource. This is NOT gestation quantum state, as that is handled directly elsewhere in the shadow facade.
        // This may be "lost shadow" - shadow which exists but the resource object has disappeared without trace.
        // Or this may be a corpse - quantum state that has just collapsed to the tombstone. Either way, it should be
        // safe to set exists=false.
        //
        // UNLESS we are in simulation mode (when doing thresholds evaluation). In that case we have to preserve the current status.
        // Otherwise, the real execution will not work correctly, see 0a89ab6ace86eecfc27ea7afc026de0f4314311e. (Maybe this needs
        // a different solution, though. We will see.)
        //
        // For the dry run, we want to mark the shadow as dead. See MID-7724.

        // FIXME FIXME FIXME This is not quite correct. We should decide if simulation really should not touch these dead
        //  accounts, or (most probably) find a way how to ensure that they will be re-processed even if marked as tombstone
        //  in the meantime.

        Task task = ctx.getTask();
        if (TaskUtil.isExecute(task) || TaskUtil.isDryRun(task)) {
            LOGGER.trace("Setting {} as tombstone. This may be a quantum state collapse. Or maybe a lost shadow.",
                    repositoryShadow);
            return shadowManager.markShadowTombstone(repositoryShadow, task, result);
        } else {
            LOGGER.trace("Not in execute or dry-run mode ({}). Keeping shadow marked as 'exists'.",
                    TaskUtil.getExecutionMode(task));
            return repositoryShadow;
        }
    }

    @Override
    public OperationResultStatus handleModifyError(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repoShadow,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable ProvisioningOperationOptions options,
            @NotNull ModifyOperationState opState,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        // We do this before marking shadow as tombstone.
        ShadowLifecycleStateType stateBefore = shadowCaretaker.determineShadowState(ctx, repoShadow);
        markShadowTombstoneIfApplicable(ctx, repoShadow, result);

        if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), options)) { // Put options to ctx
            notifyAboutMissingObjectIfApplicable(ctx, repoShadow, stateBefore, result);
        }

        throwException(cause, opState, result);
        throw new AssertionError("not here");
    }

    @Override
    public OperationResultStatus handleDeleteError(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            ProvisioningOperationOptions options,
            DeleteOperationState opState,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult result) throws SchemaException {

        // We do this before marking shadow as tombstone.
        ShadowLifecycleStateType stareBefore = shadowCaretaker.determineShadowState(ctx, repoShadow);
        markShadowTombstoneIfApplicable(ctx, repoShadow, result);

        if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), options)) { // Put options to ctx
            notifyAboutMissingObjectIfApplicable(ctx, repoShadow, stareBefore, result);
        }

        // Error deleting shadow because the shadow is already deleted. This means someone has done our job already.
        failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
        opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
        return OperationResultStatus.HANDLED_ERROR;
    }

    private void notifyAboutMissingObjectIfApplicable(
            ProvisioningContext ctx, ShadowType repoShadow, ShadowLifecycleStateType stateBefore, OperationResult parentResult) {
        if (!ctx.shouldDoDiscovery()) {
            LOGGER.trace("Skipping discovery of shadow {} because the discovery is disabled", repoShadow);
            return;
        }

        if (stateBefore != LIVE && stateBefore != REAPING) {
            // Do NOT do discovery of shadow that can legally not exist. This is no discovery.
            // We already know that the object are supposed not to exist yet or to dead already.
            // Note: The shadow may be in REAPING state e.g. if "record all pending operations" is in effect.
            // (That is a technicality. But maybe even without that we should consider shadows being reaped
            // as legitimate candidates for discovery notifications.)
            LOGGER.trace("Skipping sending notification of missing {} because it is {}, we expect that it might not exist",
                    repoShadow, stateBefore);
            return;
        }

        OperationResult result = parentResult.createSubresult(OP_DISCOVERY);
        try {
            LOGGER.debug("DISCOVERY: the resource object seems to be missing: {}", repoShadow);
            ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
            change.setResource(ctx.getResource().asPrismObject());
            change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_DISCOVERY));
            change.setObjectDelta(repoShadow.asPrismObject().createDeleteDelta());
            // Current shadow is a tombstone. This means that the object was deleted. But we need current shadow here.
            // Otherwise the synchronization situation won't be updated because SynchronizationService could think that
            // there is not shadow at all.
            change.setShadowedResourceObject(repoShadow.asPrismObject());
            change.setSimulate(TaskUtil.isPreview(ctx.getTask()));
            eventDispatcher.notifyChange(change, ctx.getTask(), result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    protected void throwException(
            Exception cause, ProvisioningOperationState<?> opState, OperationResult result)
            throws ObjectNotFoundException {
        recordCompletionError(cause, opState, result);
        if (cause instanceof ObjectNotFoundException) {
            throw (ObjectNotFoundException)cause;
        } else {
            // Actually, this should never occur. ObjectNotFoundHandler is called only for ObjectNotFoundException causes.
            throw new ObjectNotFoundException(cause.getMessage(), cause);
        }
    }
}
