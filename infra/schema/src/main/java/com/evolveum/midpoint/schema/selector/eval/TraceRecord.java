/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** TODO name */
public class TraceRecord {

    @NotNull private final String firstLine;
    @Nullable private final String nextLines;

    public TraceRecord(@NotNull String firstLine) {
        this(firstLine, null);
    }

    public TraceRecord(@NotNull String firstLine, @Nullable String nextLines) {
        this.firstLine = firstLine;
        this.nextLines = nextLines;
    }

    public @NotNull String getFirstLine() {
        return firstLine;
    }

    public @Nullable String getNextLines() {
        return nextLines;
    }
}
