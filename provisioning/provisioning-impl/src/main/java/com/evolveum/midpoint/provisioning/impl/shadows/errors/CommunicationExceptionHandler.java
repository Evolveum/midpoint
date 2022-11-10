/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Component
class CommunicationExceptionHandler extends ErrorHandler {

    private static final String OPERATION_HANDLE_GET_ERROR = CommunicationExceptionHandler.class.getName() + ".handleGetError";
    private static final String OPERATION_HANDLE_ADD_ERROR = CommunicationExceptionHandler.class.getName() + ".handleAddError";
    private static final String OPERATION_HANDLE_MODIFY_ERROR = CommunicationExceptionHandler.class.getName() + ".handleModifyError";
    private static final String OPERATION_HANDLE_DELETE_ERROR = CommunicationExceptionHandler.class.getName() + ".handleDeleteError";

    private static final Trace LOGGER = TraceManager.getTrace(CommunicationExceptionHandler.class);

    @Override
    public ShadowType handleGetError(ProvisioningContext ctx,
            ShadowType repositoryShadow, GetOperationOptions rootOptions, Exception cause,
            Task task, OperationResult parentResult) throws ObjectNotFoundException {

        ResourceType resource = ctx.getResource();

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_GET_ERROR);
        result.addParam("exception", cause.getMessage());

        String stateChangeReason = "getting " + repositoryShadow + " ended with communication problem, " + cause.getMessage();
        markResourceDown(resource.getOid(), stateChangeReason, result, task);

        // nothing to do, just return the shadow from the repo and set fetch result..
        for (OperationResult subRes : parentResult.getSubresults()) {
            subRes.muteError();
        }
        result.recordPartialError("Could not get "+repositoryShadow+" from the resource "
                + resource + ", because resource is unreachable. Returning shadow from the repository");
        repositoryShadow.setFetchResult(result.createBeanReduced());
        return repositoryShadow;
    }

    @Override
    protected void throwException(Exception cause, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result) throws CommunicationException {
        recordCompletionError(cause, opState, result);
        if (cause instanceof CommunicationException) {
            throw (CommunicationException)cause;
        } else {
            throw new CommunicationException(cause.getMessage(), cause);
        }
    }

    @Override
    public OperationResultStatus handleAddError(ProvisioningContext ctx,
            ShadowType shadowToAdd,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<ShadowType>> opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException {

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_ADD_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            String stateChangeReason = "adding " + shadowToAdd + " ended with communication problem, " + cause.getMessage();
            markResourceDown(ctx.getResourceOid(), stateChangeReason, result, task);
            handleRetriesAndAttempts(ctx, opState, options, cause, result);
            return postponeAdd(shadowToAdd, opState, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public OperationResultStatus handleModifyError(ProvisioningContext ctx, ShadowType repoShadow,
            Collection<? extends ItemDelta> modifications, ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Exception cause, OperationResult failedOperationResult, OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException {

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_MODIFY_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            String stateChangeReason = "modifying " + repoShadow + " ended with communication problem, " + cause.getMessage();
            markResourceDown(ctx.getResourceOid(), stateChangeReason, result, ctx.getTask());
            handleRetriesAndAttempts(ctx, opState, options, cause, result);
            return postponeModify(ctx, repoShadow, modifications, opState, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public OperationResultStatus handleDeleteError(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_DELETE_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            String stateChangeReason = "deleting " + repoShadow + " ended with communication problem, " + cause.getMessage();
            markResourceDown(ctx.getResourceOid(), stateChangeReason, result, ctx.getTask());
            handleRetriesAndAttempts(ctx, opState, options, cause, result);
            return postponeDelete(ctx, repoShadow, opState, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void handleRetriesAndAttempts(
            ProvisioningContext ctx, ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            ProvisioningOperationOptions options, Exception cause, OperationResult result) throws CommunicationException {
        ResourceType resource = ctx.getResource();
        if (!isOperationRetryEnabled(resource) || !isCompletePostponedOperations(options)) {
            LOGGER.trace("Operation retry turned off for {}", resource);
            throwException(cause, opState, result);
        }

        int maxRetryAttempts = ProvisioningUtil.getMaxRetryAttempts(ctx);
        Integer attemptNumber = defaultIfNull(opState.getAttemptNumber(), 1);
        if (attemptNumber >= maxRetryAttempts) {
            LOGGER.debug("Maximum number of retry attempts ({}) reached for operation on {}", attemptNumber, ctx.getResource());
            throwException(cause, opState, result);
        }
    }

}
