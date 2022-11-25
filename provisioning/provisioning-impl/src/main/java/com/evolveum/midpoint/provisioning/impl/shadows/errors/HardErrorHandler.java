/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;

import com.evolveum.midpoint.provisioning.impl.shadows.ShadowAddOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowDeleteOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowModifyOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
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
            @NotNull OperationResult result) throws SchemaException, GenericFrameworkException,
            CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(null, cause, result);
        throw new AssertionError("not reached");
    }

    @Override
    public OperationResultStatus handleAddError(
            @NotNull ShadowAddOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(operation, cause, result);
        return OperationResultStatus.FATAL_ERROR; // not reached
    }

    @Override
    public OperationResultStatus handleModifyError(
            @NotNull ShadowModifyOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(operation, cause, result);
        return OperationResultStatus.FATAL_ERROR; // not reached
    }

    @Override
    public OperationResultStatus handleDeleteError(
            @NotNull ShadowDeleteOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        throwException(operation, cause, parentResult);
        return OperationResultStatus.FATAL_ERROR; // not reached
    }
}
