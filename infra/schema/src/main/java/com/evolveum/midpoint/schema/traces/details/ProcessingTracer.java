/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.details;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;

import org.jetbrains.annotations.NotNull;

/**
 * Traces processing of low-level operations like selectors or authorizations processing.
 *
 * Useful when unstructured logging is not enough.
 *
 * Highly experimental. We'll see if it's of any use.
 */
@Experimental
public interface ProcessingTracer<E extends AbstractTraceEvent> {

    boolean isEnabled();

    /** Called only if {@link #isEnabled()} is `true`. */
    void trace(@NotNull E event);

    /**
     * Provides the default logger-based tracer.
     *
     * @param logger Logger to use. Necessary to e.g. group all authorization-related logging messages under common logger.
     * @param logPrefix Text to prepend to each first line of a log record.
     */
    static <E extends AbstractTraceEvent> ProcessingTracer<E> loggerBased(
            @NotNull Trace logger, @NotNull String logPrefix) {
        return new LoggerBased<>(logger, logPrefix);
    }

    static <E extends AbstractTraceEvent> ProcessingTracer<E> loggerBased(@NotNull Trace logger) {
        return loggerBased(logger, "");
    }

    class LoggerBased<E extends AbstractTraceEvent> implements ProcessingTracer<E> {

        @NotNull final Trace logger;
        final String logPrefix;
        private final boolean enabled;

        LoggerBased(@NotNull Trace logger, String logPrefix) {
            this.logger = logger;
            this.logPrefix = logPrefix;
            this.enabled = logger.isTraceEnabled();
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void trace(@NotNull E event) {
            var record = event.defaultTraceRecord();
            var nextLines = record.nextLines();
            if (nextLines == null) {
                logger.trace("{}", record.firstLine());
            } else {
                logger.trace("{}\n{}", record.firstLine(), nextLines);
            }
        }
    }
}
