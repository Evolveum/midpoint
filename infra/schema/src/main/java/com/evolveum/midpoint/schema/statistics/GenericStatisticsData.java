/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.statistics;

public class GenericStatisticsData {

    private int count;
    private long totalDuration;
    private Long minDuration = null;
    private Long maxDuration = null;

    public int getCount() {
        return count;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public long getMinDuration() {
        return minDuration != null ? minDuration : 0;
    }

    public long getMaxDuration() {
        return maxDuration != null ? maxDuration : 0;
    }

    public void recordOperation(long duration, int count) {
        this.count += count;
        totalDuration += duration;
        if (minDuration == null || minDuration > duration) {
            minDuration = duration;
        }
        if (maxDuration == null || maxDuration < duration) {
            maxDuration = duration;
        }
    }
}
