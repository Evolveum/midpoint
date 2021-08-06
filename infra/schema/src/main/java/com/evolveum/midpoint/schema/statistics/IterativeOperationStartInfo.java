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
     * If present, we use this object to increment the progress on operation completion.
     * This is useful because there is a lot of shared information, e.g. qualified outcome.
     *
     * Currently not implemented.
     *
     * TODO implement or throw away
     */
    private ProgressCollector progressCollector;

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

    public ProgressCollector getProgressCollector() {
        return progressCollector;
    }

    public void setProgressCollector(ProgressCollector progressCollector) {
        this.progressCollector = progressCollector;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "item=" + item +
                ", startTimeMillis=" + startTimeMillis +
                ", progressCollector=" + progressCollector +
                '}';
    }
}
