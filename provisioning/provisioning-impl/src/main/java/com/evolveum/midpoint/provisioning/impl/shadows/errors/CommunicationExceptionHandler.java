/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
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
    public RepoShadow handleGetError(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repositoryShadow,
            @NotNull Exception cause,
            @NotNull OperationResult failedOperationResult,
            @NotNull OperationResult result) throws ObjectNotFoundException {

        // TODO should we mark the resource also when in preview mode?
        markResourceDown(ctx, reasonMessage("getting", repositoryShadow, cause), result);

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
            @NotNull ShadowAddOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException {

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_ADD_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            markResourceDown(operation.getCtx(), reasonMessage(operation, cause), result);
            return throwOrPostpone(operation, cause, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public OperationResultStatus handleModifyError(
            @NotNull ShadowModifyOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_MODIFY_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            markResourceDown(operation.getCtx(), reasonMessage(operation, cause), result);
            return throwOrPostpone(operation, cause, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public OperationResultStatus handleDeleteError(
            @NotNull ShadowDeleteOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_DELETE_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            markResourceDown(operation.getCtx(), reasonMessage(operation, cause), result);
            return throwOrPostpone(operation, cause, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private OperationResultStatus throwOrPostpone(
            ShadowProvisioningOperation operation,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult result) throws CommunicationException {
        if (shouldThrowImmediately(operation)) {
            throwException(operation, cause, result);
            throw new AssertionError("not here");
        } else {
            result.setInProgress();
            operation.getOpState().markAsPostponed(failedOperationResult.getStatus());
            return OperationResultStatus.IN_PROGRESS;
        }
    }

    private boolean shouldThrowImmediately(ShadowProvisioningOperation operation) {

        var ctx = operation.getCtx();
        if (!isOperationRetryEnabled(ctx.getResource())) {
            LOGGER.trace("Operation retry turned off for the resource");
            return true;
        }

        if (!isCompletePostponedOperations(operation.getOptions())) {
            LOGGER.trace("Completion of postponed operations is not requested");
            return true;
        }

        int maxRetryAttempts = ProvisioningUtil.getMaxRetryAttempts(ctx);
        int attemptNumber = operation.getOpState().getRealAttemptNumber();
        if (attemptNumber >= maxRetryAttempts) {
            LOGGER.debug("Maximum number of retry attempts ({}) reached for operation on {}", attemptNumber, ctx.getResource());
            return true;
        }
        LOGGER.trace("Will postpone the operation");
        return false;
    }

    @Override
    protected void throwException(
            @Nullable ShadowProvisioningOperation operation, Exception cause, OperationResult result)
            throws CommunicationException {
        recordCompletionError(operation, cause, result);
        if (cause instanceof CommunicationException communicationException) {
            throw communicationException;
        } else {
            throw new CommunicationException(cause.getMessage(), cause);
        }
    }

    private static String reasonMessage(@NotNull ShadowProvisioningOperation operation, @NotNull Exception cause) {
        return reasonMessage(operation.getGerund(), operation.getOpState().getRepoShadow(), cause);
    }

    private static String reasonMessage(String opName, RepoShadow repoShadow, @NotNull Exception cause) {
        return String.format("%s %s ended with communication problem, %s", opName, repoShadow, cause.getMessage());
    }
}
