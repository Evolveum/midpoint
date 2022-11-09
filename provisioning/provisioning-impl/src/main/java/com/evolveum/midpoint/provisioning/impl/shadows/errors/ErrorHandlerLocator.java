/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.MaintenanceException;

@Component
public class ErrorHandlerLocator {

    @Autowired CommunicationExceptionHandler communicationExceptionHandler;
    @Autowired SchemaExceptionHandler schemaExceptionHandler;
    @Autowired ObjectNotFoundHandler objectNotFoundHandler;
    @Autowired ObjectAlreadyExistHandler objectAlreadyExistsHandler;
    @Autowired GenericErrorHandler genericErrorHandler;
    @Autowired ConfigurationExceptionHandler configurationExceptionHandler;
    @Autowired SecurityViolationHandler securityViolationHandler;
    @Autowired PolicyViolationHandler policyViolationHandler;
    @Autowired MaintenanceExceptionHandler maintenanceExceptionHandler;

    public @NotNull ErrorHandler locateErrorHandlerRequired(Throwable ex) {
        return MiscUtil.requireNonNull(
                locateErrorHandler(ex),
                () -> new SystemException("Error without a handler: " + MiscUtil.formatExceptionMessage(ex), ex));

    }

    private ErrorHandler locateErrorHandler(Throwable ex) {
        if (ex instanceof MaintenanceException) {
            return maintenanceExceptionHandler;
        }
        if (ex instanceof CommunicationException){
            return communicationExceptionHandler;
        }
        if (ex instanceof GenericConnectorException) {
            return genericErrorHandler;
        }
        if (ex instanceof ObjectAlreadyExistsException) {
            return objectAlreadyExistsHandler;
        }
        if (ex instanceof ObjectNotFoundException) {
            return objectNotFoundHandler;
        }
        if (ex instanceof SchemaException) {
            return schemaExceptionHandler;
        }
        if (ex instanceof ConfigurationException) {
            return configurationExceptionHandler;
        }
        if (ex instanceof SecurityViolationException) {
            return securityViolationHandler;
        }
        if (ex instanceof PolicyViolationException) {
            return policyViolationHandler;
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException)ex;
        }
        if (ex instanceof Error) {
            throw (Error)ex;
        }

        throw new SystemException(ex != null ? ex.getClass().getName() +": "+ ex.getMessage() : "Unexpected error:", ex);
    }



}
