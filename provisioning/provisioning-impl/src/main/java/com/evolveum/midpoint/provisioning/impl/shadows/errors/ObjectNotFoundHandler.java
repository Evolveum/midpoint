/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType.FORCED_EXCEPTION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.*;

import com.evolveum.midpoint.provisioning.impl.shadows.RepoShadowWithState;

import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowDeleteOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowModifyOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowProvisioningOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowUpdater;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;

@Component
class ObjectNotFoundHandler extends HardErrorHandler {

    private static final String OP_DISCOVERY = ObjectNotFoundHandler.class + ".discovery";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectNotFoundHandler.class);

    @Autowired private ShadowUpdater shadowUpdater;

    @Override
    public RepoShadow handleGetError(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull Exception cause,
            @NotNull OperationResult failedOperationResult,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        // We do this before marking shadow as tombstone.
        ShadowLifecycleStateType stateBefore = repoShadow.getShadowLifecycleState();

        RepoShadow shadowToReturn = markShadowTombstoneIfApplicable(ctx, repoShadow, result);

        if (ctx.getErrorReportingMethod() == FORCED_EXCEPTION) {
            LOGGER.debug("Got {} but 'forced exception' mode is selected. Will rethrow it.", cause.getClass().getSimpleName());
            throwException(null, cause, result);
            throw new AssertionError("not reached");
        }

        if (ctx.shouldDoDiscoveryOnGet()) {
            notifyAboutDisappearedObject(ctx, repoShadow, stateBefore, result);
        }

        failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
        return shadowToReturn;
    }

    private RepoShadow markShadowTombstoneIfApplicable(
            ProvisioningContext ctx, RepoShadow repoShadow, OperationResult result) throws SchemaException {

        if (!repoShadow.doesExist()) {
            LOGGER.debug("Shadow {} is already marked as not existing. No point in marking it as dead here.", repoShadow);
            return repoShadow;
        }

        // This is some kind of reality mismatch. We obviously have shadow that is supposed to be alive (exists=true). But it does
        // not exist on resource. This is NOT gestation quantum state, as that is handled directly elsewhere in the shadow facade.
        // This may be "lost shadow" - shadow which exists but the resource object has disappeared without trace.
        // Or this may be a corpse - quantum state that has just collapsed to the tombstone. Either way, it should be
        // safe to set exists=false.
        //
        // Even for the dry run, we want to mark the shadow as dead. See MID-7724.
        LOGGER.trace("Setting {} as tombstone. This may be a quantum state collapse. Or maybe a lost shadow.", repoShadow);
        return shadowUpdater.markShadowTombstone(repoShadow, ctx.getTask(), result);
    }

    @Override
    public OperationResultStatus handleModifyError(
            @NotNull ShadowModifyOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        markShadowAndNotify(operation, result);

        // There is nothing reasonable we can do here, only to throw the exception.
        throwException(operation, cause, result);
        throw new AssertionError("not here");
    }

    @Override
    public OperationResultStatus handleDeleteError(
            @NotNull ShadowDeleteOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult result) throws SchemaException {

        markShadowAndNotify(operation, result);

        // "Error deleting shadow because the shadow is already deleted."
        // This means someone has done our job already.
        failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
        operation.getOpState().setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
        return OperationResultStatus.HANDLED_ERROR;
    }

    private void markShadowAndNotify(
            @NotNull ShadowProvisioningOperation operation, @NotNull OperationResult result) throws SchemaException {

        ProvisioningContext ctx = operation.getCtx();
        RepoShadow repoShadow = operation.getOpState().getRepoShadowRequired();

        // We do this before marking shadow as tombstone.
        ShadowLifecycleStateType stateBefore = repoShadow.getShadowLifecycleState();
        markShadowTombstoneIfApplicable(ctx, repoShadow, result);

        if (ProvisioningUtil.isDiscoveryAllowed(ctx.getResource(), operation.getOptions())) { // Put options to ctx
            notifyAboutDisappearedObject(ctx, repoShadow, stateBefore, result);
        }
    }

    private void notifyAboutDisappearedObject(
            ProvisioningContext ctx, RepoShadow repoShadow, ShadowLifecycleStateType stateBefore, OperationResult parentResult) {
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
            change.setObjectDelta(repoShadow.getPrismObject().createDeleteDelta());
            // Current shadow is a tombstone. This means that the object was deleted. But we need current shadow here.
            // Otherwise the synchronization situation won't be updated because SynchronizationService could think that
            // there is not shadow at all.
            ctx.computeAndUpdateEffectiveMarksAndPolicies(repoShadow, RepoShadowWithState.ShadowState.EXISTING, result);
            change.setShadowedResourceObject(repoShadow.getPrismObject());
            eventDispatcher.notifyChange(change, ctx.getTask(), result);
        } catch (CommonException e) {
            result.recordException(e);
            throw SystemException.unexpected(e); // TODO reconsider
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    protected void throwException(
            @Nullable ShadowProvisioningOperation operation, Exception cause, OperationResult result)
            throws ObjectNotFoundException {
        recordCompletionError(operation, cause, result);
        if (cause instanceof ObjectNotFoundException objectNotFoundException) {
            throw objectNotFoundException;
        } else {
            // Actually, this should never occur. ObjectNotFoundHandler is called only for ObjectNotFoundException causes.
            throw new ObjectNotFoundException(cause.getMessage(), cause);
        }
    }
}
