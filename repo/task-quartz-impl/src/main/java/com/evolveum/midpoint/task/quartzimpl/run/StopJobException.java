/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;

/**
 * Signalling that we need to immediately stop the job execution.
 * Carries the cause and/or the message (if any).
 *
 * Created to avoid nested checking for "can continue" flag.
 */
class StopJobException extends Exception {

    @NotNull private final Severity severity;

    StopJobException(@NotNull Severity severity, @NotNull String template, Throwable cause, Object... args) {
        super(formatMessage(template, args), cause);
        this.severity = severity;
    }

    StopJobException() {
        super();
        this.severity = Severity.NONE;
    }

    /**
     * Logs a message appropriate to the {@link #severity}.
     */
    public void log(Trace logger) {
        if (severity == Severity.NONE) {
            return;
        }
        if (getCause() != null) {
            switch (severity) {
                case WARNING:
                    LoggingUtils.logExceptionAsWarning(logger, getMessage(), getCause());
                    break;
                case ERROR:
                    LoggingUtils.logException(logger, getMessage(), getCause());
                    break;
                case UNEXPECTED_ERROR:
                default:
                    LoggingUtils.logUnexpectedException(logger, getMessage(), getCause());
                    break;
            }
        } else {
            switch (severity) {
                case WARNING:
                    logger.warn(getMessage());
                    break;
                case ERROR:
                case UNEXPECTED_ERROR:
                default:
                    logger.error(getMessage());
                    break;
            }
        }
    }

    @NotNull
    private static String formatMessage(@NotNull String template, Object[] args) {
        return Strings.lenientFormat(template, args);
    }

    enum Severity {

        /**
         * Benign situation OR the problem was already reported.
         * We shouldn't even care with reporting anything more.
         * We just exit the execution.
         */
        NONE,

        /**
         * Something suspicious occurred. Should log a warning.
         */
        WARNING,

        /**
         * An error, but not quite unexpected.
         */
        ERROR,

        /**
         * Something very strange that deserves e.g. full stack trace (if applicable).
         */
        UNEXPECTED_ERROR
    }
}
