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
 * Maintains selected statistical information related to processing items during task part execution.
 * Currently this means it is bound to a single bucket.
 *
 * Must be thread safe.
 */
public class CurrentBucketStatistics {

    /** Number of items processed during the task part execution (in current bucket). */
    private final AtomicInteger itemsProcessed = new AtomicInteger();

    /** Number of items experiencing errors during the task part execution (in current bucket). */
    private final AtomicInteger errors = new AtomicInteger();

    /**
     * Time (in millis) spend while processing the items during the task part execution. Does NOT include time spend
     * in pre-processing nor while waiting in the queue.
     */
    private final AtomicDouble totalTimeProcessing = new AtomicDouble();

    /** The wall clock time when the bucket execution started. */
    protected volatile long startTimeMillis;

    /** When the bucket execution ended. */
    protected volatile Long endTimeMillis;

    void recordStart() {
        this.startTimeMillis = System.currentTimeMillis();
    }

    void recordEnd() {
        this.endTimeMillis = System.currentTimeMillis();
    }

    public double addDuration(double delta) {
        return totalTimeProcessing.addAndGet(delta);
    }

    void incrementProgress() {
        itemsProcessed.incrementAndGet();
    }

    void incrementErrors() {
        errors.incrementAndGet();
    }

    final Double getAverageTime() {
        int count = getItemsProcessed();
        if (count > 0) {
            double total = totalTimeProcessing.get();
            return total / count;
        } else {
            return null;
        }
    }

    final double getProcessingTime() {
        return totalTimeProcessing.get();
    }

    Double getAverageWallClockTime(long now) {
        int count = getItemsProcessed();
        if (count > 0) {
            return (double) getWallClockTime(now) / count;
        } else {
            return null;
        }
    }

    Double getThroughput(long now) {
        Double wallAverageTime = getAverageWallClockTime(now);
        if (wallAverageTime != null) {
            return 60000.0 / wallAverageTime;
        } else {
            return null;
        }
    }

    public final long getWallClockTime(long now) {
        return now - startTimeMillis;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public Long getEndTimeMillis() {
        return endTimeMillis;
    }

    public int getErrors() {
        return errors.get();
    }

    public int getItemsProcessed() {
        return itemsProcessed.get();
    }
}
