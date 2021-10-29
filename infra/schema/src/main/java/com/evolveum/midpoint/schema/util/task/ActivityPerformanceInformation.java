/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityRunRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityItemProcessingStatisticsType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityProgressType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil.getItemProcessingStatisticsCollection;

/**
 * Extract of the most relevant performance information about an activity.
 *
 * Note: unlike {@link ActivityProgressInformation} this structure describes only a single activity.
 * It is expected to be wrapped inside {@link TreeNode} structure.
 */
@Experimental
public class ActivityPerformanceInformation implements DebugDumpable, Serializable {

    /**
     * Full path of the activity.
     */
    private final ActivityPath activityPath;

    /**
     * False if the performance information is not applicable e.g. because the activity is a composite one.
     */
    private final boolean applicable;

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

    private ActivityPerformanceInformation(ActivityPath activityPath,
            int itemsProcessed, int errors, int progress, double processingTime,
            WallClockInfo info) {
        this.activityPath = activityPath;
        this.applicable = true;
        this.itemsProcessed = itemsProcessed;
        this.errors = errors;
        this.progress = progress;
        this.processingTime = processingTime;
        this.wallClockTime = info.wallClockTime;
        this.earliestStartTime = info.earliestStartTime;
    }

    private ActivityPerformanceInformation(ActivityPath activityPath) {
        this.activityPath = activityPath;
        this.applicable = false;
        this.itemsProcessed = 0;
        this.errors = 0;
        this.progress = 0;
        this.processingTime = 0;
        this.wallClockTime = null;
        this.earliestStartTime = null;
    }

    public static @NotNull ActivityPerformanceInformation forRegularActivity(@NotNull ActivityPath path,
            @Nullable ActivityStateType state) {
        ActivityItemProcessingStatisticsType itemStats = state != null && state.getStatistics() != null ?
                state.getStatistics().getItemProcessing() : null;
        ActivityProgressType progress = state != null ? state.getProgress() : null;
        return forRegularActivity(path, itemStats, progress);
    }

    @NotNull
    public static ActivityPerformanceInformation forRegularActivity(@NotNull ActivityPath path,
            @Nullable ActivityItemProcessingStatisticsType itemStats, @Nullable ActivityProgressType activityProgress) {

        int itemsProcessed = ActivityItemProcessingStatisticsUtil.getItemsProcessedShallow(itemStats);
        int errors = ActivityItemProcessingStatisticsUtil.getErrorsShallow(itemStats);
        int progress = ActivityProgressUtil.getCurrentProgress(activityProgress);
        double processingTime = ActivityItemProcessingStatisticsUtil.getProcessingTime(itemStats);

        WallClockInfo wallClockInfo = WallClockInfo.determine(itemStats != null ? List.of(itemStats) : List.of());

        return new ActivityPerformanceInformation(path, itemsProcessed, errors, progress, processingTime, wallClockInfo);
    }

    @NotNull
    public static ActivityPerformanceInformation forCoordinator(@NotNull ActivityPath path,
            @NotNull Collection<ActivityStateType> workerStates) {

        int itemsProcessed = ActivityItemProcessingStatisticsUtil.getItemsProcessed(workerStates);
        int errors = ActivityItemProcessingStatisticsUtil.getErrors(workerStates);
        int progress = ActivityProgressUtil.getCurrentProgress(workerStates);
        double processingTime = ActivityItemProcessingStatisticsUtil.getProcessingTime(workerStates);

        WallClockInfo wallClockInfo = WallClockInfo.determine(getItemProcessingStatisticsCollection(workerStates));

        return new ActivityPerformanceInformation(path, itemsProcessed, errors, progress, processingTime, wallClockInfo);
    }

    public static ActivityPerformanceInformation notApplicable(@NotNull ActivityPath path) {
        return new ActivityPerformanceInformation(path);
    }

    public ActivityPath getActivityPath() {
        return activityPath;
    }

    public boolean isApplicable() {
        return applicable;
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
                "activityPath=" + activityPath +
                ", applicable=" + applicable +
                ", itemsProcessed=" + itemsProcessed +
                ", errors=" + errors +
                ", progress=" + progress +
                ", processingTime=" + processingTime +
                ", wallClockTime=" + wallClockTime +
                ", earliestStartTime=" + earliestStartTime +
                '}';
    }

    public String toHumanReadableString() {
        if (!applicable) {
            return "Not applicable";
        }
        Double throughput = getThroughput();
        if (throughput == null) {
            return "No information";
        }

        return String.format(Locale.US, "Throughput: %,.1f per minute (%,d items, %,.1f ms per item)",
                throughput, getItemsProcessed(), getAverageWallClockTime());
    }

    @Override
    public String debugDump(int indent) {
        String title = String.format("%s for %s", getClass().getSimpleName(), activityPath.toDebugName());
        StringBuilder sb = DebugUtil.createTitleStringBuilder(title, indent);
        if (applicable) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabelLn(sb, "Items processed", itemsProcessed, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "Errors", errors, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "Progress", progress, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "Processing time", String.valueOf(processingTime), indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "Wall clock time", wallClockTime, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "Earliest start time", String.valueOf(earliestStartTime), indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "Summary", toHumanReadableString(), indent + 1);
        } else {
            sb.append(": Not applicable");
        }
        return sb.toString();
    }

    /**
     * Information about the real (wall clock) time spent by an activity.
     */
    private static class WallClockInfo {

        /**
         * Aggregated wall-clock time when at least one activity execution took place.
         */
        private final Long wallClockTime;

        /**
         * The time of the earliest start of an execution of the activity.
         * It is not needed to evaluate the performance. It is here for debugging/testing purposes.
         */
        private final XMLGregorianCalendar earliestStartTime;

        private WallClockInfo(Long wallClockTime, XMLGregorianCalendar earliestStartTime) {
            this.wallClockTime = wallClockTime;
            this.earliestStartTime = earliestStartTime;
        }

        public static WallClockInfo determine(Collection<ActivityItemProcessingStatisticsType> statistics) {
            List<ActivityRunRecordType> allExecutions = statistics.stream()
                    .flatMap(s -> s.getRun().stream())
                    .collect(Collectors.toList());
            WallClockTimeComputer wallClockTimeComputer = new WallClockTimeComputer(allExecutions);
            return new WallClockInfo(wallClockTimeComputer.getSummaryTime(), wallClockTimeComputer.getEarliestStartTime());
        }
    }
}
