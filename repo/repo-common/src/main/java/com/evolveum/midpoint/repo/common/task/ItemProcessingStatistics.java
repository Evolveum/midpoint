/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintains selected statistical information related to processing items in during task part execution.
 *
 * Must be thread safe.
 */
public class ItemProcessingStatistics {

    /** Number of items processed during the task part execution. (So not counting items in other buckets, for example.) */
    private final AtomicInteger itemsProcessed = new AtomicInteger();

    /** Number of items experiencing errors during the task part execution. */
    private final AtomicInteger errors = new AtomicInteger();

    /**
     * Time (in millis) spend while processing the items during the task part execution. Does NOT include time spend
     * in pre-processing nor while waiting in the queue.
     */
    private final AtomicDouble totalTimeProcessing = new AtomicDouble();

    /**
     * Initial progress of the task - i.e. when this bucket in the current part started.
     * TODO what to do with multi part tasks like the reconciliation?
     */
    private final long initialProgress;

    /** The time when the task part execution started. */
    protected final long startTimeMillis;

    ItemProcessingStatistics(long initialProgress) {
        this.initialProgress = initialProgress;
        this.startTimeMillis = System.currentTimeMillis();
    }

    public double addDuration(double delta) {
        return totalTimeProcessing.addAndGet(delta);
    }

    long incrementProgress() {
        return initialProgress + itemsProcessed.incrementAndGet();
    }

    int incrementErrors() {
        return errors.incrementAndGet();
    }

    /**
     * @return Total progress to be recorded into the task. Computed as initial progress plus number of items processed.
     */
    public long getTotalProgress() {
        return initialProgress + itemsProcessed.get();
    }

    final Double getAverageTime() {
        long count = getTotalProgress();
        if (count > 0) {
            double total = totalTimeProcessing.get();
            return total / count;
        } else {
            return null;
        }
    }

    final Double getWallAverageTime() {
        long count = getTotalProgress();
        if (count > 0) {
            return (double) getWallTime() / count;
        } else {
            return null;
        }
    }

    final long getWallTime() {
        return System.currentTimeMillis() - startTimeMillis;
    }

    public int getErrors() {
        return errors.get();
    }

    public int getItemsProcessed() {
        return itemsProcessed.get();
    }
}
