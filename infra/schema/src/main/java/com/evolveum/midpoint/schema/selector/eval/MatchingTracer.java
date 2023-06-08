/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.logging.Trace;

public interface MatchingTracer {

    boolean isEnabled();

    /** Called only if {@link #isEnabled()} is `true`. */
    void trace(@NotNull TraceEvent event);

    class LoggerBased implements MatchingTracer {

        @NotNull final Trace logger;
        final String logPrefix;
        private final boolean enabled;

        public LoggerBased(@NotNull Trace logger, String logPrefix) {
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
