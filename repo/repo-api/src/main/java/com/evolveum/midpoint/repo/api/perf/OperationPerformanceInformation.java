/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api.perf;

import java.util.Locale;

import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryOperationPerformanceInformationType;

/**
 * Experimental.
 */
public class OperationPerformanceInformation implements ShortDumpable, Cloneable {

    private int invocationCount;
    private int executionCount; // counts each attempt, including retries
    private long totalTime; // ms
    private Long minTime;
    private Long maxTime;
    private long totalWastedTime;
    private Long minWastedTime;
    private Long maxWastedTime;

    public int getInvocationCount() {
        return invocationCount;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public Long getMinTime() {
        return minTime;
    }

    public Long getMaxTime() {
        return maxTime;
    }

    public long getTotalWastedTime() {
        return totalWastedTime;
    }

    public Long getMinWastedTime() {
        return minWastedTime;
    }

    public Long getMaxWastedTime() {
        return maxWastedTime;
    }

    public synchronized void register(OperationRecord operation) {
        invocationCount++;
        executionCount += operation.getAttempts();
        addTotalTime(operation.getTotalTime());
        addWastedTime(operation.getWastedTime());
    }

    private void addTotalTime(long time) {
        totalTime += time;
        if (minTime == null || time < minTime) {
            minTime = time;
        }
        if (maxTime == null || time > maxTime) {
            maxTime = time;
        }
    }

    private void addWastedTime(long time) {
        totalWastedTime += time;
        if (minWastedTime == null || time < minWastedTime) {
            minWastedTime = time;
        }
        if (maxWastedTime == null || time > maxWastedTime) {
            maxWastedTime = time;
        }
    }

    public synchronized RepositoryOperationPerformanceInformationType toRepositoryOperationPerformanceInformationType(String kind) {
        RepositoryOperationPerformanceInformationType rv = new RepositoryOperationPerformanceInformationType();
        rv.setName(kind);
        rv.setInvocationCount(invocationCount);
        rv.setExecutionCount(executionCount);
        rv.setTotalTime(totalTime);
        rv.setMinTime(minTime);
        rv.setMaxTime(maxTime);
        rv.setTotalWastedTime(totalWastedTime);
        rv.setMinWastedTime(minWastedTime);
        rv.setMaxWastedTime(maxWastedTime);
        return rv;
    }

    @Override
    public synchronized void shortDump(StringBuilder sb) {
        sb.append(invocationCount);
        sb.append(", total time: ").append(timeInfo(totalTime, minTime, maxTime, invocationCount));
        if (totalTime > 0 && executionCount > invocationCount) {
            sb.append(String.format(Locale.US, ", wasted time for %d retry/retries: %s (%s)", executionCount - invocationCount,
                    timeInfo(totalWastedTime, minWastedTime, maxWastedTime, invocationCount), percent(totalWastedTime, totalTime)));
        }
    }

    private String timeInfo(long total, Long min, Long max, int count) {
        return total + " ms" +
                (count > 0 ? String.format(Locale.US, " (min/max/avg: %d/%d/%.1f)", min, max, (float) total / count) : "");
    }

    private String percent(long value, long base) {
        if (base != 0) {
            return String.format(Locale.US, "%.2f%%", 100.0 * value / base);
        } else if (value == 0) {
            return String.format(Locale.US, "%.2f%%", 0.0);
        } else {
            return "NaN%";
        }
    }

    public OperationPerformanceInformation clone() {
        try {
            return (OperationPerformanceInformation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new SystemException(e);
        }
    }

    /** Shortened string info mentioned mostly for debugger usage. */
    @Override
    public String toString() {
        return "OpPerfInfo{" +
                "I=" + invocationCount +
                ", E=" + executionCount +
                ", totalMs=" + totalTime +
                ", minMs=" + minTime +
                ", maxMs=" + maxTime +
                ", wastedMs=" + totalWastedTime +
                ", minWMs=" + minWastedTime +
                ", maxWMs=" + maxWastedTime +
                '}';
    }
}
