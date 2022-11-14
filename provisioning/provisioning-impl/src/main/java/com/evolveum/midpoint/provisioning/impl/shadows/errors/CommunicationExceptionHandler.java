/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.DeleteOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.ModifyOperationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Component
class CommunicationExceptionHandler extends ErrorHandler {

    private static final String OPERATION_HANDLE_ADD_ERROR = CommunicationExceptionHandler.class.getName() + ".handleAddError";
    private static final String OPERATION_HANDLE_MODIFY_ERROR = CommunicationExceptionHandler.class.getName() + ".handleModifyError";
    private static final String OPERATION_HANDLE_DELETE_ERROR = CommunicationExceptionHandler.class.getName() + ".handleDeleteError";

    private static final Trace LOGGER = TraceManager.getTrace(CommunicationExceptionHandler.class);

    @Override
    public ShadowType handleGetError(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repositoryShadow,
            @NotNull Exception cause,
            @NotNull OperationResult failedOperationResult,
            @NotNull OperationResult parentResult) throws ObjectNotFoundException {

        // TODO should we mark the resource also when in preview mode?
        markResourceDown(ctx, reasonMessage("getting", repositoryShadow, cause), parentResult);

        // We have very little to do here. Just change the result status to the partial error, provide more information
        // to the message, and return the repository shadow. Even the fetchResult will be set by the provisioning service itself.

        failedOperationResult.setStatus(OperationResultStatus.PARTIAL_ERROR);
        failedOperationResult.setMessage(
                String.format(
                        "Could not get %s from %s, because the resource is unreachable. Returning shadow from the repository: %s",
                        repositoryShadow, ctx.getResource(), failedOperationResult.getMessage()));

        return repositoryShadow;
    }

    @Override
    public OperationResultStatus handleAddError(
            ProvisioningContext ctx,
            ShadowType shadowToAdd,
            ProvisioningOperationOptions options,
            AddOperationState opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException {

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_ADD_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            markResourceDown(ctx, reasonMessage("adding", shadowToAdd, cause), result);
            return postponeOrThrow(ctx, opState, options, cause, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
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
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_MODIFY_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            markResourceDown(ctx, reasonMessage("modifying", repoShadow, cause), result);
            return postponeOrThrow(ctx, opState, options, cause, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public OperationResultStatus handleDeleteError(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            ProvisioningOperationOptions options,
            DeleteOperationState opState,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_DELETE_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            markResourceDown(ctx, reasonMessage("deleting", repoShadow, cause), result);
            return postponeOrThrow(ctx, opState, options, cause, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private OperationResultStatus postponeOrThrow(
            ProvisioningContext ctx,
            ProvisioningOperationState<?> opState,
            ProvisioningOperationOptions options,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult result) throws CommunicationException {
        if (shouldThrowImmediately(ctx, opState, options)) {
            throwException(cause, opState, result);
            throw new AssertionError("not here");
        } else {
            result.setInProgress();
            return opState.markAsPostponed(failedOperationResult);
        }
    }

    private boolean shouldThrowImmediately(
            ProvisioningContext ctx, ProvisioningOperationState<?> opState, ProvisioningOperationOptions options) {

        if (!isOperationRetryEnabled(ctx.getResource())) {
            LOGGER.trace("Operation retry turned off for the resource");
            return true;
        }

        if (!isCompletePostponedOperations(options)) {
            LOGGER.trace("Completion of postponed operations is not requested");
            return true;
        }

        int maxRetryAttempts = ProvisioningUtil.getMaxRetryAttempts(ctx);
        int attemptNumber = opState.getRealAttemptNumber();
        if (attemptNumber >= maxRetryAttempts) {
            LOGGER.debug("Maximum number of retry attempts ({}) reached for operation on {}", attemptNumber, ctx.getResource());
            return true;
        }
        LOGGER.trace("Will postpone the operation");
        return false;
    }

    @Override
    protected void throwException(
            Exception cause, ProvisioningOperationState<?> opState, OperationResult result)
            throws CommunicationException {
        recordCompletionError(cause, opState, result);
        if (cause instanceof CommunicationException) {
            throw (CommunicationException) cause;
        } else {
            throw new CommunicationException(cause.getMessage(), cause);
        }
    }

    private static String reasonMessage(String op, @NotNull ShadowType repositoryShadow, @NotNull Exception cause) {
        return op + " " + repositoryShadow + " ended with communication problem, " + cause.getMessage();
    }
}
