/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ErrorSelectorType;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author Radovan Semancik
 *
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
        if (exception instanceof CommunicationException) {
            return defaultIfNull(selector.getNetwork(), defaultValue);
        }
        if (exception instanceof SecurityViolationException) {
            return defaultIfNull(selector.getSecurity(), defaultValue);
        }
        if (exception instanceof PolicyViolationException) {
            return defaultIfNull(selector.getPolicy(), defaultValue);
        }
        if (exception instanceof SchemaException) {
            return defaultIfNull(selector.getSchema(), defaultValue);
        }
        if (exception instanceof ConfigurationException || exception instanceof ExpressionEvaluationException) {
            return defaultIfNull(selector.getConfiguration(), defaultValue);
        }
        if (exception instanceof UnsupportedOperationException) {
            return defaultIfNull(selector.getUnsupported(), defaultValue);
        }
        return defaultIfNull(selector.getGeneric(), defaultValue);
    }

    public static boolean isFatalCriticality(CriticalityType value, CriticalityType defaultValue) {
        return defaultIfNull(value, defaultValue) == CriticalityType.FATAL;
    }

    public static LocalizableMessage getUserFriendlyMessage(Throwable cause) {
        while (cause != null) {
            if (cause instanceof CommonException) {
                LocalizableMessage userFriendlyMessage = ((CommonException)cause).getUserFriendlyMessage();
                if (userFriendlyMessage != null) {
                    return userFriendlyMessage;
                }
            }
            cause = cause.getCause();
        }
        return null;
    }

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

    public static <T extends Throwable> T findException(Throwable throwable, Class<T> clazz) {
        while (throwable != null) {
            if (clazz.isAssignableFrom(throwable.getClass())) {
                //noinspection unchecked
                return (T) throwable;
            }
            throwable = throwable.getCause();
        }
        return null;
    }

    public static String printStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
