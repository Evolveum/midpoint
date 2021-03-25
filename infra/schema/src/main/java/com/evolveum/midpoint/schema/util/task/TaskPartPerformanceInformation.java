/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.io.Serializable;
import java.util.Locale;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskPartItemsProcessingInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StructuredTaskProgressType;

/**
 * Extract of the most relevant performance information about a task part.
 */
@Experimental
public class TaskPartPerformanceInformation implements DebugDumpable, Serializable {

    /**
     * FIXME Sometimes we put handler URI here. That is not correct.
     */
    private final String partUri;

    /**
     * Items processed. Good indicator for avg wall clock time and throughput.
     */
    private final int itemsProcessed;

    /**
     * Number of errors. Included only because we traditionally displayed it in log messages.
     */
    private final int errors;

    /**
     * Real progress. This is less than items processed. Does not include duplicate processing.
     */
    private final int progress;

    /**
     * The sum of time spent processing individual items. For multithreaded execution each thread counts separately.
     * Does not include pre-processing. So this is actually not very helpful indicator of the performance.
     * But we include it here for completeness.
     */
    private final double processingTime;

    /**
     * Wall-clock time spent in processing this task part. Relevant for performance determination and ETA computation.
     * Includes time from all known executions of this tasks.
     *
     * TODO make this null (instead of 0) for tasks where we do not keep the wall-clock time information
     */
    private final Long wallClockTime;

    /**
     * Earliest known time when this part execution started. Just for testing/debugging reasons.
     */
    @VisibleForTesting
    private final XMLGregorianCalendar earliestStartTime;

    private TaskPartPerformanceInformation(String partUri, int itemsProcessed, int errors, int progress, double processingTime,
            Long wallClockTime, XMLGregorianCalendar earliestStartTime) {
        this.partUri = partUri;
        this.itemsProcessed = itemsProcessed;
        this.errors = errors;
        this.progress = progress;
        this.processingTime = processingTime;
        this.wallClockTime = wallClockTime;
        this.earliestStartTime = earliestStartTime;
    }

    @NotNull
    public static TaskPartPerformanceInformation forPart(@NotNull IterativeTaskPartItemsProcessingInformationType info,
            StructuredTaskProgressType structuredProgress) {

        String partUri = info.getPartUri();

        int itemsProcessed = TaskOperationStatsUtil.getItemsProcessed(info);
        int errors = TaskOperationStatsUtil.getErrors(info);
        int progress = TaskProgressUtil.getTotalProgressForPart(structuredProgress, partUri);
        double processingTime = TaskOperationStatsUtil.getProcessingTime(info);

        WallClockTimeComputer wallClockTimeComputer = new WallClockTimeComputer(info.getExecution());

        long wallClockTime = wallClockTimeComputer.getSummaryTime();
        XMLGregorianCalendar earliestStartTime = wallClockTimeComputer.getEarliestStartTime();

        return new TaskPartPerformanceInformation(partUri, itemsProcessed, errors, progress, processingTime,
                wallClockTime, earliestStartTime);
    }

    @NotNull
    public static TaskPartPerformanceInformation forCurrentPart(OperationStatsType operationStats,
            StructuredTaskProgressType structuredProgress) {
        IterativeTaskPartItemsProcessingInformationType info = TaskOperationStatsUtil
                .getIterativeInfoForCurrentPart(operationStats, structuredProgress);
        return forPart(info, structuredProgress);
    }

    public String getPartUri() {
        return partUri;
    }

    public int getItemsProcessed() {
        return itemsProcessed;
    }

    public int getProgress() {
        return progress;
    }

    public Long getWallClockTime() {
        return wallClockTime;
    }

    public int getErrors() {
        return errors;
    }

    public double getProcessingTime() {
        return processingTime;
    }

    public Double getAverageTime() {
        if (itemsProcessed > 0) {
            return processingTime / itemsProcessed;
        } else {
            return null;
        }
    }

    public Double getAverageWallClockTime() {
        if (wallClockTime != null && wallClockTime > 0 && itemsProcessed > 0) {
            return (double) wallClockTime / itemsProcessed;
        } else {
            return null;
        }
    }

    public Double getThroughput() {
        Double averageWallClockTime = getAverageWallClockTime();
        if (averageWallClockTime != null && averageWallClockTime > 0) {
            return 60000 / averageWallClockTime;
        } else {
            return null;
        }
    }

    public XMLGregorianCalendar getEarliestStartTime() {
        return earliestStartTime;
    }

    @Override
    public String toString() {
        return "TaskPartPerformanceInformation{" +
                "URI=" + partUri +
                ", itemsProcessed=" + itemsProcessed +
                ", errors=" + errors +
                ", progress=" + progress +
                ", processingTime=" + processingTime +
                ", wallClockTime=" + wallClockTime +
                ", earliestStartTime=" + earliestStartTime +
                '}';
    }

    public String toHumanReadableString() {
        Double throughput = getThroughput();
        if (throughput != null) {
            return String.format(Locale.US, "Throughput: %,.1f per minute (%,d items, %,.1f ms per item)",
                    throughput, getItemsProcessed(), getAverageWallClockTime());
        } else {
            return "No information";
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "URI", partUri, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Items processed", itemsProcessed, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Errors", errors, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Progress", progress, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Processing time", String.valueOf(processingTime), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Wall clock time", wallClockTime, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Earliest start time", String.valueOf(earliestStartTime), indent);
        DebugUtil.debugDumpWithLabel(sb, "Summary", toHumanReadableString(), indent);
        return sb.toString();
    }
}
