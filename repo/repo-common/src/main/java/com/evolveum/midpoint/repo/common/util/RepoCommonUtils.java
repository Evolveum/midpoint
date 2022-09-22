/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.util;

import java.util.List;

import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 * @author katka
 *
 */
public class RepoCommonUtils {

    private static final Trace LOGGER = TraceManager.getTrace(RepoCommonUtils.class);

    public static void processErrorCriticality(Object object, CriticalityType criticality, Throwable e, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {
        switch (criticality) {
            case FATAL:
                LOGGER.debug("Exception {} criticality set as FATAL in {}, stopping evaluation; exception message: {}",
                        e.getClass().getSimpleName(), object, e.getMessage());
                LoggingUtils.logExceptionAsWarning( // Intentionally not displaying the full exception (MID-6695)
                        LOGGER, "An error (potentially recoverable) while processing projection on {}: {}",
                        e, object, e.getMessage());
                throwException(e, result);
                throw new AssertionError("not reached");
            case PARTIAL:
                LOGGER.debug("Exception {} criticality set as PARTIAL in {}, continuing evaluation; exception message: {}",
                        e.getClass().getSimpleName(), object, e.getMessage());
                if (result != null) {
                    result.recordPartialError(e);
                }
                LOGGER.warn("Partial error while processing projection on {}: {}", object, e.getMessage(), e);
                LOGGER.warn("Operation result:\n{}", result != null ? result.debugDump() : "(null)");
                break;
            case IGNORE:
                LOGGER.debug("Exception {} criticality set as IGNORE in {}, continuing evaluation; exception message: {}",
                        e.getClass().getSimpleName(), object, e.getMessage());
                if (result != null) {
                    result.recordHandledError(e);
                }
                LOGGER.debug("Ignored error while processing projection on {}: {}", object, e.getMessage(), e);
                break;
        }
    }

    public static void throwException(Throwable e, OperationResult result)
        throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {
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
        } else {
            throw new SystemException(e.getMessage(), e);
        }
    }

    // TODO preserve full stack traces somehow (maybe using user data)
    @NotNull
    public static Throwable getResultException(@NotNull OperationResultType result) {
        return getResultException(OperationResult.createOperationResult(result));
    }

    @NotNull
    public static Throwable getResultException(@NotNull OperationResult result) {
        Throwable t = getResultExceptionIfExists(result);
        if (t != null) {
            return t;
        } else {
            LOGGER.debug("No exception found in operation result - but there should be some. Using an artificial one:\n{}",
                    result.debugDumpLazily(1));
            return new SystemException(result.getMessage());
        }
    }

    /**
     * This method tries to determine an exception from an operation result. It does a depth-first search, but treating
     * subresults from last one to the first one. Hopefully this will lead to correct exception.
     *
     * TODO think about handled errors here: e.g. should we skip them when looking for exceptions?
     */
    public static Throwable getResultExceptionIfExists(OperationResult result) {
        if (result.getCause() != null) {
            return result.getCause();
        }
        List<OperationResult> subresults = result.getSubresults();
        for (int i = subresults.size() - 1; i >= 0; i--) {
            Throwable subresultException = getResultExceptionIfExists(subresults.get(i));
            if (subresultException != null) {
                return subresultException;
            }
        }
        return null;
    }
}
