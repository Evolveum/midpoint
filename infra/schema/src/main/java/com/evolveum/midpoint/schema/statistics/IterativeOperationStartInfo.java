/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Represents data about iterative operation that starts.
 *
 * TODO better name
 */
@Experimental
public class IterativeOperationStartInfo {

    @NotNull private final IterationItemInformation item;

    private final long startTimeMillis;
    private final long startTimeNanos;

    /**
     * Brutal hack. Indicates that the caller is a simple one, and wants us to do everything related.
     * So we'll increase the progress, and commit stats as well.
     */
    private boolean simpleCaller;

    public IterativeOperationStartInfo(@NotNull IterationItemInformation item) {
        this.item = item;

        this.startTimeMillis = System.currentTimeMillis();
        this.startTimeNanos = System.nanoTime();
    }

    public @NotNull IterationItemInformation getItem() {
        return item;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public boolean isSimpleCaller() {
        return simpleCaller;
    }

    public void setSimpleCaller(boolean simpleCaller) {
        this.simpleCaller = simpleCaller;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "item=" + item +
                ", startTimeMillis=" + startTimeMillis +
                ", simpleCaller=" + simpleCaller +
                '}';
    }
}
