/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.errorhandling;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.impl.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Handler for "hard" errors - error that cannot be handled in any smart way.
 * @author semancik
 *
 */
public abstract class HardErrorHandler extends ErrorHandler {

    private static final String OPERATION_HANDLE_GET_ERROR = HardErrorHandler.class.getName() + ".handleGetError";
    private static final String OPERATION_HANDLE_ADD_ERROR = HardErrorHandler.class.getName() + ".handleAddError";
    private static final String OPERATION_HANDLE_MODIFY_ERROR = HardErrorHandler.class.getName() + ".handleModifyError";
    private static final String OPERATION_HANDLE_DELETE_ERROR = HardErrorHandler.class.getName() + ".handleDeleteError";

    private static final Trace LOGGER = TraceManager.getTrace(HardErrorHandler.class);

    @Override
    public PrismObject<ShadowType> handleGetError(ProvisioningContext ctx,
            PrismObject<ShadowType> repositoryShadow, GetOperationOptions rootOptions, Exception cause,
            Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException,
            CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(cause, null, parentResult);
        return null; // not reached
    }

    @Override
    public OperationResultStatus handleAddError(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(cause, opState, parentResult);
        return OperationResultStatus.FATAL_ERROR; // not reached
    }

    @Override
    public OperationResultStatus handleModifyError(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            Collection<? extends ItemDelta> modifications, ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(cause, opState, parentResult);
        return OperationResultStatus.FATAL_ERROR; // not reached
    }

    @Override
    public OperationResultStatus handleDeleteError(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationResult> opState, Exception cause,
            OperationResult failedOperationResult, Task task, OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(cause, opState, parentResult);
        return OperationResultStatus.FATAL_ERROR; // not reached
    }

}
