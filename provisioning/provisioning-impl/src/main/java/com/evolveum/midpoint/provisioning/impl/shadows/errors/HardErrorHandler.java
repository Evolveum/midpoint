/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.DeleteOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.ModifyOperationState;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handler for "hard" errors - error that cannot be handled in any smart way.
 * @author semancik
 *
 */
abstract class HardErrorHandler extends ErrorHandler {

    @Override
    public ShadowType handleGetError(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repositoryShadow,
            @NotNull Exception cause,
            @NotNull OperationResult failedOperationResult,
            @NotNull OperationResult parentResult) throws SchemaException, GenericFrameworkException,
            CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(cause, null, parentResult);
        throw new AssertionError("not reached");
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
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(cause, opState, parentResult);
        return OperationResultStatus.FATAL_ERROR; // not reached
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
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(cause, opState, parentResult);
        return OperationResultStatus.FATAL_ERROR; // not reached
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
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(cause, opState, parentResult);
        return OperationResultStatus.FATAL_ERROR; // not reached
    }

}
