/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
