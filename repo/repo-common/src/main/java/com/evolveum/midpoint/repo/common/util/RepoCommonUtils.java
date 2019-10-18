/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.util;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType;

/**
 * @author katka
 *
 */
public class RepoCommonUtils {

    private static final transient Trace LOGGER = TraceManager.getTrace(RepoCommonUtils.class);

    public static void processErrorCriticality(Object object, CriticalityType criticality, Throwable e, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException,
    ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException {
    switch (criticality) {
        case FATAL:
            LOGGER.debug("Exception {} criticality set as FATAL in {}, stopping evaluation; exception message: {}", e.getClass().getSimpleName(), object, e.getMessage());
            LOGGER.error("Fatal error while processing projection on {}: {}", object, e.getMessage(), e);
            throwException(e, result);
            break; // not reached
        case PARTIAL:
            LOGGER.debug("Exception {} criticality set as PARTIAL in {}, continuing evaluation; exception message: {}", e.getClass().getSimpleName(), object, e.getMessage());
            if (result != null) {
                result.recordPartialError(e);
            }
            LOGGER.warn("Partial error while processing projection on {}: {}", object, e.getMessage(), e);
            LOGGER.warn("Operation result:\n{}", result != null ? result.debugDump() : "(null)");
            break;
        case IGNORE:
            LOGGER.debug("Exception {} criticality set as IGNORE in {}, continuing evaluation; exception message: {}", e.getClass().getSimpleName(), object, e.getMessage());
            if (result != null) {
                result.recordHandledError(e);
            }
            LOGGER.debug("Ignored error while processing projection on {}: {}", object, e.getMessage(), e);
            break;
    }
}

    public static void throwException(Throwable e, OperationResult result)
        throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException,
            PreconditionViolationException {
    if (result != null) {
        result.recordFatalError(e);
    }
    if (e instanceof RuntimeException) {
        throw (RuntimeException)e;
    } else if (e instanceof Error) {
        throw (Error)e;
    } else if (e instanceof ObjectNotFoundException) {
        throw (ObjectNotFoundException)e;
    } else if (e instanceof CommunicationException) {
        throw (CommunicationException)e;
    } else if (e instanceof SchemaException) {
        throw (SchemaException)e;
    } else if (e instanceof ConfigurationException) {
        throw (ConfigurationException)e;
    } else if (e instanceof SecurityViolationException) {
        throw (SecurityViolationException)e;
    } else if (e instanceof PolicyViolationException) {
        throw (PolicyViolationException)e;
    } else if (e instanceof ExpressionEvaluationException) {
        throw (ExpressionEvaluationException)e;
    } else if (e instanceof ObjectAlreadyExistsException) {
        throw (ObjectAlreadyExistsException)e;
    } else if (e instanceof PreconditionViolationException) {
        throw (PreconditionViolationException)e;
    } else {
        throw new SystemException(e.getMessage(), e);
    }
}
    //TODO implement better
    public static Throwable getResultException(OperationResult result) {
        if (result.getCause() != null) {
            return result.getCause();
        } else if (result.getLastSubresult().getCause() != null) {
            return result.getLastSubresult().getCause();
        } else {
            return new SystemException(result.getMessage());
        }
    }

}
