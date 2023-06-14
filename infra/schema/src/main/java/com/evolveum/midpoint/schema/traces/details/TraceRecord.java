/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
