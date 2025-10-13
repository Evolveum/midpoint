/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces.details;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default or customized text form of an {@link AbstractTraceEvent}.
 *
 * @see AbstractTraceEvent#defaultTraceRecord()
 */
public record TraceRecord(@NotNull String firstLine, @Nullable String nextLines) {

    public static TraceRecord of(@NotNull String firstLine) {
        return new TraceRecord(firstLine, null);
    }

    public static TraceRecord of(@NotNull String firstLine, @Nullable String nextLines) {
        return new TraceRecord(firstLine, nextLines);
    }
}
