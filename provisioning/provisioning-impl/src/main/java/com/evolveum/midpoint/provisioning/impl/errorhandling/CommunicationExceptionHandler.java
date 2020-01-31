/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.errorhandling;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.provisioning.impl.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ResourceManager;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@Component
public class CommunicationExceptionHandler extends ErrorHandler {

    private static final String OPERATION_HANDLE_GET_ERROR = CommunicationExceptionHandler.class.getName() + ".handleGetError";
    private static final String OPERATION_HANDLE_ADD_ERROR = CommunicationExceptionHandler.class.getName() + ".handleAddError";
    private static final String OPERATION_HANDLE_MODIFY_ERROR = CommunicationExceptionHandler.class.getName() + ".handleModifyError";
    private static final String OPERATION_HANDLE_DELETE_ERROR = CommunicationExceptionHandler.class.getName() + ".handleDeleteError";

    private static final Trace LOGGER = TraceManager.getTrace(CommunicationExceptionHandler.class);

    @Override
    public PrismObject<ShadowType> handleGetError(ProvisioningContext ctx,
            PrismObject<ShadowType> repositoryShadow, GetOperationOptions rootOptions, Exception cause,
            Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException,
            CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        ResourceType resource = ctx.getResource();
        if (!ProvisioningUtil.isDoDiscovery(resource, rootOptions)) {
            throwException(cause, null, parentResult);
        }

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_GET_ERROR);
        result.addParam("exception", cause.getMessage());

        String operationCtx = "getting " + repositoryShadow + " ended with communication problem, " + cause.getMessage();
        markResourceDown(resource, operationCtx, result);

        // nothing to do, just return the shadow from the repo and set fetch
        // result..
        for (OperationResult subRes : parentResult.getSubresults()) {
            subRes.muteError();
        }
        result.recordPartialError("Could not get "+repositoryShadow+" from the resource "
                + resource + ", because resource is unreachable. Returning shadow from the repository");
        repositoryShadow.asObjectable().setFetchResult(result.createOperationResultType());
//                    operationResult.recordSuccess();
//                    operationResult.computeStatus();
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
            PrismObject<ShadowType> shadowToAdd,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult)
                throws SchemaException, GenericFrameworkException, CommunicationException,
                ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
                SecurityViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_ADD_ERROR);
        result.addParam("exception", cause.getMessage());
        ResourceType resource = ctx.getResource();
        String operationCtx = "adding " + shadowToAdd + " ended with communication problem, " + cause.getMessage();
        markResourceDown(resource, operationCtx, result);
        handleRetriesAndAttempts(ctx, opState, options, cause, result);
        return postponeAdd(ctx, shadowToAdd, opState, failedOperationResult, result);
    }

    @Override
    public OperationResultStatus handleModifyError(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            Collection<? extends ItemDelta> modifications, ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_MODIFY_ERROR);
        result.addParam("exception", cause.getMessage());
        ResourceType resource = ctx.getResource();
        String operationCtx = "modifying " + repoShadow + " ended with communication problem, " + cause.getMessage();
        markResourceDown(resource, operationCtx, result);
        handleRetriesAndAttempts(ctx, opState, options, cause, result);
        return postponeModify(ctx, repoShadow, modifications, opState, failedOperationResult, result);
    }

    @Override
    public OperationResultStatus handleDeleteError(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationResult> opState, Exception cause,
            OperationResult failedOperationResult, Task task, OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_DELETE_ERROR);
        result.addParam("exception", cause.getMessage());
        ResourceType resource = ctx.getResource();
        String operationCtx = "deleting " + repoShadow + " ended with communication problem, " + cause.getMessage();
        markResourceDown(resource, operationCtx, result);
        handleRetriesAndAttempts(ctx, opState, options, cause, result);
        return postponeDelete(ctx, repoShadow, opState, failedOperationResult, result);
    }

    private void handleRetriesAndAttempts(ProvisioningContext ctx, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, ProvisioningOperationOptions options, Exception cause, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException {
        ResourceType resource = ctx.getResource();
        if (!isOperationRetryEnabled(resource) || !isCompletePostponedOperations(options)) {
            LOGGER.trace("Operation retry turned off for {}", resource);
            throwException(cause, opState, result);
        }

        int maxRetryAttempts = ProvisioningUtil.getMaxRetryAttempts(ctx);
        Integer attemptNumber = opState.getAttemptNumber();
        if (attemptNumber == null) {
            attemptNumber = 1;
        }
        if (attemptNumber >= maxRetryAttempts) {
            LOGGER.debug("Maximum nuber of retry attempts ({}) reached for operation on {}", attemptNumber, ctx.getResource() );
            throwException(cause, opState, result);
        }
    }

}
