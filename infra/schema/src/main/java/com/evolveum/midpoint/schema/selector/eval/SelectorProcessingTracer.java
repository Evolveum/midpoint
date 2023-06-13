/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.logging.Trace;

/**
 * Facilitates troubleshooting of selectors and their clauses.
 * Crucial for authorizations: https://docs.evolveum.com/midpoint/reference/diag/troubleshooting/authorizations/.
 */
public interface SelectorProcessingTracer {

    boolean isEnabled();

    /** Called only if {@link #isEnabled()} is `true`. */
    void trace(@NotNull TraceEvent event);

    /**
     * Provides the default logger-based tracer.
     *
     * @param logger Logger to use. Necessary to e.g. group all authorization-related logging messages under common logger.
     * @param logPrefix Text to prepend to each first line of a log record.
     */
    static SelectorProcessingTracer loggerBased(
            @NotNull Trace logger, @NotNull String logPrefix) {
        return new LoggerBased(logger, logPrefix);
    }

    static SelectorProcessingTracer loggerBased(@NotNull Trace logger) {
        return loggerBased(logger, "");
    }

    class LoggerBased implements SelectorProcessingTracer {

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
        public void trace(@NotNull TraceEvent event) {
            var record = event.defaultTraceRecord();
            var nextLines = record.getNextLines();
            if (nextLines == null) {
                logger.trace("{}", record.getFirstLine());
            } else {
                logger.trace("{}\n{}", record.getFirstLine(), nextLines);
            }
        }
    }
}
