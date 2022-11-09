/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.util.exception.*;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceConsistencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Handler for provisioning errors. The handler can invoke additional functionality to
 * handle the error, transform the error, turn critical errors to non-critical, and so on.
 *
 * The handler may "swallow" and re-throw the exception. If the exception is "swallowed" then
 * the operation continues. In that case the relevant information is in opState and result.
 * This will usually indicate "in progress" operation (e.g. operation prepared to be retried).
 *
 * If exception is thrown from the handler then this means the end of the operation.
 * No more retries, no more attempts.
 *
 * @author Katka Valalikova
 * @author Radovan Semancik
 *
 */
public abstract class ErrorHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ErrorHandler.class);

    @Autowired protected EventDispatcher eventDispatcher;
    @Autowired private ResourceManager resourceManager;
    @Autowired protected PrismContext prismContext;

    public abstract ShadowType handleGetError(
            ProvisioningContext ctx,
            ShadowType repositoryShadow,
            GetOperationOptions rootOptions,
            Exception cause,
            Task task,
            OperationResult parentResult)
                    throws SchemaException, GenericFrameworkException, CommunicationException,
                    ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
                    SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;


    public abstract OperationResultStatus handleAddError(
            ProvisioningContext ctx,
            ShadowType shadowToAdd,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<ShadowType>> opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult)
                throws SchemaException, GenericFrameworkException, CommunicationException,
                ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
                SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    OperationResultStatus postponeAdd(
            ShadowType shadowToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<ShadowType>> opState,
            OperationResult failedOperationResult,
            OperationResult result) {
        LOGGER.trace("Postponing ADD operation for {}", shadowToAdd);
        opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTING);
        AsynchronousOperationReturnValue<ShadowType> asyncResult = new AsynchronousOperationReturnValue<>();
        asyncResult.setOperationResult(failedOperationResult);
        asyncResult.setOperationType(PendingOperationTypeType.RETRY);
        opState.setAsyncResult(asyncResult);
        if (opState.getAttemptNumber() == null) {
            opState.setAttemptNumber(1);
        }
        result.setInProgress();
        return OperationResultStatus.IN_PROGRESS;
    }

    public abstract OperationResultStatus handleModifyError(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            Collection<? extends ItemDelta> modifications,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult parentResult)
                throws SchemaException, GenericFrameworkException, CommunicationException,
                ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
                SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    OperationResultStatus postponeModify(ProvisioningContext ctx,
            ShadowType repoShadow,
            Collection<? extends ItemDelta> modifications,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            OperationResult failedOperationResult,
            OperationResult result) {
        return ProvisioningUtil.postponeModify(ctx, repoShadow, modifications, opState, failedOperationResult, result);
    }

    OperationResultStatus postponeDelete(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            OperationResult failedOperationResult,
            OperationResult result) {
        LOGGER.trace("Postponing DELETE operation for {}", repoShadow);
        opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTING);
        AsynchronousOperationResult asyncResult = new AsynchronousOperationResult();
        asyncResult.setOperationResult(failedOperationResult);
        asyncResult.setOperationType(PendingOperationTypeType.RETRY);
        opState.setAsyncResult(asyncResult);
        if (opState.getAttemptNumber() == null) {
            opState.setAttemptNumber(1);
        }
        result.setInProgress();
        return OperationResultStatus.IN_PROGRESS;
    }

    public abstract OperationResultStatus handleDeleteError(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult result)
                throws SchemaException, GenericFrameworkException, CommunicationException,
                ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
                SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    /**
     * Throw exception of appropriate type.
     * If exception is thrown then this is definitive end of the operation.
     * No more retries, no more attempts.
     */
    protected abstract void throwException(
            Exception cause, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    /**
     * Record error that completes the operation. If such error is recorded then this is definitive end of the operation.
     * No more retries, no more attempts.
     */
    protected void recordCompletionError(Exception cause,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result) {
        result.recordFatalError(cause);
        if (opState != null) {
            opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
        }
    }

    void markResourceDown(String resourceOid, String changeReason, OperationResult parentResult, Task task) throws ObjectNotFoundException {
        resourceManager.modifyResourceAvailabilityStatus(resourceOid, AvailabilityStatusType.DOWN, changeReason, task, parentResult, false);
    }

    protected boolean isOperationRetryEnabled(ResourceType resource) {
        ResourceConsistencyType consistency = resource.getConsistency();
        if (consistency == null) {
            return true;
        }
        Integer operationRetryMaxAttempts = consistency.getOperationRetryMaxAttempts();
        if (operationRetryMaxAttempts == null) {
            return true;
        }
        return operationRetryMaxAttempts != 0;
    }

    protected boolean isCompletePostponedOperations(ProvisioningOperationOptions options) {
        return ProvisioningOperationOptions.isCompletePostponed(options);
    }

}
