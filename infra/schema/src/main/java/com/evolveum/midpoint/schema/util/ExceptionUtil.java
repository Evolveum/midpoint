/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ErrorCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ErrorSelectorType;

/**
 * @author Radovan Semancik
 */
public class ExceptionUtil {

    public static Throwable lookForTunneledException(Throwable ex) {
        if (ex instanceof TunnelException) {
            return ex.getCause();
        }
        if (ex.getCause() != null) {
            return lookForTunneledException(ex.getCause());
        }
        return null;
    }

    public static String lookForMessage(Throwable e) {
        if (e.getMessage() != null) {
            return e.getMessage();
        }
        if (e.getCause() != null) {
            return lookForMessage(e.getCause());
        }
        return null;
    }

    public static CriticalityType getCriticality(ErrorSelectorType selector, Throwable exception, CriticalityType defaultValue) {
        if (selector == null) {
            return defaultValue;
        }
        ErrorCategoryType category = getErrorCategory(exception);
        switch (category) {
            case NETWORK:
                return defaultIfNull(selector.getNetwork(), defaultValue);
            case SECURITY:
                return defaultIfNull(selector.getSecurity(), defaultValue);
            case POLICY_THRESHOLD:
                return defaultIfNull(selector.getPolicyThreshold(), defaultValue);
            case POLICY:
                return defaultIfNull(selector.getPolicy(), defaultValue);
            case SCHEMA:
                return defaultIfNull(selector.getSchema(), defaultValue);
            case CONFIGURATION:
                return defaultIfNull(selector.getConfiguration(), defaultValue);
            case UNSUPPORTED:
                return defaultIfNull(selector.getUnsupported(), defaultValue);
            case GENERIC:
                return defaultIfNull(selector.getGeneric(), defaultValue);
            default:
                throw new AssertionError(category);
        }
    }

    // TODO improve the categorization code (e.g. not all expression evaluation exceptions are configuration-related)
    @NotNull
    public static ErrorCategoryType getErrorCategory(Throwable exception) {
        if (exception instanceof CommunicationException) {
            return ErrorCategoryType.NETWORK;
        } else if (exception instanceof SecurityViolationException) {
            return ErrorCategoryType.SECURITY;
        } else if (exception instanceof ThresholdPolicyViolationException) {
            return ErrorCategoryType.POLICY_THRESHOLD;
        } else if (exception instanceof PolicyViolationException) {
            return ErrorCategoryType.POLICY;
        } else if (exception instanceof SchemaException) {
            return ErrorCategoryType.SCHEMA;
        } else if (exception instanceof ConfigurationException || exception instanceof ExpressionEvaluationException) {
            return ErrorCategoryType.CONFIGURATION;
        } else if (exception instanceof UnsupportedOperationException) {
            return ErrorCategoryType.UNSUPPORTED;
        } else {
            return ErrorCategoryType.GENERIC;
        }
    }

    public static boolean isFatalCriticality(CriticalityType value, CriticalityType defaultValue) {
        return defaultIfNull(value, defaultValue) == CriticalityType.FATAL;
    }

    public static LocalizableMessage getUserFriendlyMessage(Throwable cause) {
        while (cause != null) {
            if (cause instanceof CommonException) {
                LocalizableMessage userFriendlyMessage = ((CommonException) cause).getUserFriendlyMessage();
                if (userFriendlyMessage != null) {
                    return userFriendlyMessage;
                }
            }
            cause = cause.getCause();
        }
        return null;
    }

    /**
     * Returns cause of specified type (can be `throwable` parameter itself) or `null`.
     */
    public static <T extends Throwable> T findCause(Throwable throwable, Class<T> causeClass) {
        while (throwable != null) {
            if (causeClass.isAssignableFrom(throwable.getClass())) {
                //noinspection unchecked
                return (T) throwable;
            }
            throwable = throwable.getCause();
        }
        return null;
    }

    public static Throwable findRootCause(Throwable throwable) {
        while (throwable != null && throwable.getCause() != null && throwable != throwable.getCause()) {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    public static List<Throwable> getCauses(@NotNull Throwable throwable) {
        List<Throwable> causes = new ArrayList<>();
        for (;;) {
            causes.add(throwable);
            var cause = throwable.getCause();
            if (cause == null || cause == throwable) {
                return causes;
            }
            throwable = cause;
        }
    }

    public static List<Throwable> getCausesFromBottomUp(@NotNull Throwable throwable) {
        var causes = getCauses(throwable);
        Collections.reverse(causes);
        return causes;
    }

    public static String printStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
