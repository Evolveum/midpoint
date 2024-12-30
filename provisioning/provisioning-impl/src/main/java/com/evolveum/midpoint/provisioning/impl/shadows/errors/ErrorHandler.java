/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.*;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceConsistencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

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

    @Autowired protected EventDispatcher eventDispatcher;
    @Autowired private ResourceManager resourceManager;
    @Autowired protected PrismContext prismContext;

    /**
     * The method may return null if the repository shadow is found to be no longer existing.
     *
     * @param failedOperationResult The operation result carrying the failed operation. Should be closed.
     */
    public abstract RepoShadow handleGetError(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repositoryShadow,
            @NotNull Exception cause,
            @NotNull OperationResult failedOperationResult,
            @NotNull OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    public abstract OperationResultStatus handleAddError(
            @NotNull ShadowAddOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            OperationResult result)
                throws SchemaException, GenericFrameworkException, CommunicationException,
                ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
                SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    public abstract OperationResultStatus handleModifyError(
            @NotNull ShadowModifyOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult result)
                throws SchemaException, GenericFrameworkException, CommunicationException,
                ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
                SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    public abstract OperationResultStatus handleDeleteError(
            @NotNull ShadowDeleteOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult result)
                throws SchemaException, GenericFrameworkException, CommunicationException,
                ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
                SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    /**
     * Throw exception of appropriate type.
     * If exception is thrown then this is definitive end of the operation.
     * No more retries, no more attempts.
     */
    protected abstract void throwException(
            @Nullable ShadowProvisioningOperation operation, Exception cause, OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException;

    /**
     * Record error that completes the operation. If such error is recorded then this is definitive end of the operation.
     * No more retries, no more attempts.
     */
    protected void recordCompletionError(
            @Nullable ShadowProvisioningOperation operation,
            Exception cause,
            OperationResult result) {
        result.recordExceptionNotFinish(cause);
        if (operation != null) {
            operation.getOpState().setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
        }
    }

    void markResourceDown(
            ProvisioningContext ctx,
            String changeReason,
            OperationResult result) throws ObjectNotFoundException {
        resourceManager.modifyResourceAvailabilityStatus(
                ctx.getResourceOid(), AvailabilityStatusType.DOWN, changeReason, ctx.getTask(), result, false);
    }

    boolean isOperationRetryEnabled(ResourceType resource) {
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

    boolean isCompletePostponedOperations(ProvisioningOperationOptions options) {
        return ProvisioningOperationOptions.isCompletePostponed(options);
    }

}
