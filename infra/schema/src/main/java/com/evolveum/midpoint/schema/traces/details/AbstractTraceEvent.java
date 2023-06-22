/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.details;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Something of interest during tracing of some low-level operation i.e. under of what is covered by {@link OperationResult}.
 *
 * It will eventually get converted into a logfile entry, or CSV line, trace file item, and so on.
 *
 * Not intended to be serializable, at least not for now.
 *
 * @see ProcessingTracer
 */
@Experimental
public abstract class AbstractTraceEvent {

    @Nullable final protected String message;
    @Nullable final protected Object[] arguments;

    public AbstractTraceEvent(@Nullable String message, @Nullable Object[] arguments) {
        this.message = message;
        this.arguments = arguments;
    }

    public abstract @NotNull TraceRecord defaultTraceRecord();

    protected String getFormattedMessage(@NotNull String prefix, @NotNull String suffix) {
        if (message != null) {
            return prefix + String.format(message, arguments) + suffix;
        } else {
            return "";
        }
    }
}
