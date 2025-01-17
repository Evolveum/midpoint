/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.schema.statistics.Operation;

import java.util.Locale;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityEventLoggingOptionType;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPerformanceInformation;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Logs reasonable information on item and bucket completion, with a level of detail driven by logging option set.
 */
public class StatisticsLogger {

    private static final Trace LOGGER = TraceManager.getTrace(StatisticsLogger.class);

    @NotNull private final IterativeActivityRun<?, ?, ?, ?> activityRun;

    public StatisticsLogger(@NotNull IterativeActivityRun<?, ?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }

    public void logItemCompletion(Operation operation, OperationResultStatus resultStatus) {

        ActivityEventLoggingOptionType logging = activityRun.getReportingDefinition().getItemCompletionLogging();
        if (shouldSkipLogging(logging)) {
            return; // No need to waste time computing all the stats
        }

        long end = operation.getEndTimeMillis();
        TransientActivityRunStatistics current = activityRun.getTransientRunStatistics();
        ActivityPerformanceInformation overall = getOverallStatistics();

        String mainMessage = String.format(Locale.US, "%s of %s%s done with status %s in %,1f ms.",
                activityRun.getShortName(), operation.getIterationItemInformation(),
                activityRun.getContextDescriptionSpaced(), resultStatus, operation.getDurationRounded());

        String counts = String.format(Locale.US,
                " Items processed in current run: %,d (%,d overall), errors: %,d (%,d overall).",
                current.getItemsProcessed(), overall.getItemsProcessed(),
                current.getErrors(), overall.getErrors());

        String throughput;
        Double overallThroughput = overall.getThroughput();
        if (overallThroughput != null) {
            throughput = String.format(Locale.US, " Overall throughput: %,.1f items per minute.", overallThroughput);
        } else {
            throughput = "";
        }

        String mainMessageAddition = counts + throughput;
        String fullStats = getFullStatMessage(overall, end);

        log(logging, mainMessage, mainMessageAddition, fullStats);
    }

    void logBucketCompletion(boolean complete) {

        ActivityEventLoggingOptionType logging = activityRun.getReportingDefinition().getBucketCompletionLogging();
        if (shouldSkipLogging(logging)) {
            return; // No need to waste time computing all the stats
        }

        TransientActivityRunStatistics current = activityRun.getTransientRunStatistics();
        long end = System.currentTimeMillis();
        ActivityPerformanceInformation overall = getOverallStatistics();

        RunningTask task = activityRun.getRunningTask();
        String mainMessage =
                String.format("%s bucket #%d for %s (%s in %s).%s",
                        complete ? "Completed" : "Partially processed",
                        activityRun.getBucket().getSequentialNumber(),
                        activityRun.getShortNameUncapitalized(), activityRun.getActivityPath().toDebugName(), task,
                        complete ? "" : " Bucket processing was interrupted.");


        String currentBrief = String.format(Locale.US, "Current run: processed %,d objects in %.1f seconds, got %,d errors.",
                current.getItemsProcessed(), current.getWallClockTime(end) / 1000.0, current.getErrors());
        if (current.getItemsProcessed() > 0) {
            currentBrief += String.format(Locale.US, " Average processing time for one object: %,.1f milliseconds. "
                            + "Wall clock average: %,.1f milliseconds, throughput: %,.1f items per minute.",
                    current.getAverageTime(), current.getAverageWallClockTime(end), current.getThroughput(end));
        }

        Long wallClockTime = overall.getWallClockTime();

        // Wall-clock time information is not available e.g. for activities with persistent state (like LiveSync)
        boolean hasWallClockTime = wallClockTime != null && wallClockTime > 0;

        String wallClockTimeString;
        if (hasWallClockTime) {
            wallClockTimeString = String.format(Locale.US, " in %.1f seconds", ActivityItemProcessingStatisticsUtil.toSeconds(wallClockTime));
        } else {
            wallClockTimeString = "";
        }
        String overallBrief = String.format(Locale.US,
                "Overall: processed %,d objects%s, got %,d errors. Real progress: %,d.",
                overall.getItemsProcessed(), wallClockTimeString, overall.getErrors(), overall.getProgress());
        if (overall.getItemsProcessed() > 0) {
            overallBrief += String.format(Locale.US,
                    " Average processing time for one object: %,.1f milliseconds.", overall.getAverageTime());
            if (hasWallClockTime && overall.getAverageWallClockTime() != null) {
                overallBrief += String.format(Locale.US,
                        " Wall clock average: %,.1f milliseconds, throughput: %,.1f items per minute.",
                        overall.getAverageWallClockTime(), overall.getThroughput());
            }
        }

        String mainMessageAddition = "\n" + currentBrief + "\n" + overallBrief;
        String fullStats = getFullStatMessage(overall, end);

        log(logging, mainMessage, mainMessageAddition, fullStats);
    }

    // Keep in sync with #shouldSkipLogging method below.
    private void log(ActivityEventLoggingOptionType logging, String mainMessage, String mainMessageAddition, String fullStats) {
        if (logging == ActivityEventLoggingOptionType.FULL) {
            LOGGER.info("{}\n\n{}", mainMessage, fullStats);
        } else if (logging == ActivityEventLoggingOptionType.BRIEF) {
            LOGGER.info("{}{}", mainMessage, mainMessageAddition);
            LOGGER.trace("{}", fullStats);
        } else {
            LOGGER.trace("{}\n\n{}", mainMessage, fullStats);
        }
    }

    // Keep in sync with #log method above.
    private boolean shouldSkipLogging(ActivityEventLoggingOptionType logging) {
        if (logging == ActivityEventLoggingOptionType.FULL || logging == ActivityEventLoggingOptionType.BRIEF) {
            return !LOGGER.isInfoEnabled();
        } else {
            return !LOGGER.isTraceEnabled();
        }
    }

    @NotNull
    private ActivityPerformanceInformation getOverallStatistics() {
        return ActivityPerformanceInformation.forRegularActivity(
                activityRun.getActivityPath(),
                activityRun.getActivityState().getLiveItemProcessingStatistics().getValueCopy(),
                activityRun.getActivityState().getLiveProgress().getValueCopy());
    }

    private String getFullStatMessage(ActivityPerformanceInformation overall, long end) {
        TransientActivityRunStatistics current = activityRun.getTransientRunStatistics();
        return String.format(Locale.US,
                "Items processed: %,d in current run and %,d overall.\n"
                        + "Errors: %,d in current run and %,d in overall.\n"
                        + "Real progress is %,d.\n\n"
                        + "Average duration is %,.1f ms (in current run) and %,.1f ms (overall).\n"
                        + "Wall clock average is %,.1f ms (in current run) and %,.1f ms (overall).\n"
                        + "Average throughput is %,.1f items per minute (in current run) and %,.1f items per minute (overall).\n\n"
                        + "Processing time is %,.1f ms (for current run) and %,.1f ms (overall)\n"
                        + "Wall-clock time is %,d ms (for current run) and %,d ms (overall)\n"
                        + "Start time was:\n"
                        + " - for current run: %s\n"
                        + " - overall:         %s\n",

                current.getItemsProcessed(), overall.getItemsProcessed(),
                current.getErrors(), overall.getErrors(),
                overall.getProgress(),
                current.getAverageTime(), overall.getAverageTime(),
                current.getAverageWallClockTime(end), overall.getAverageWallClockTime(),
                current.getThroughput(end), overall.getThroughput(),
                current.getProcessingTime(), overall.getProcessingTime(),
                current.getWallClockTime(end), overall.getWallClockTime(),
                XmlTypeConverter.createXMLGregorianCalendar(current.getStartTimeMillis()),
                overall.getEarliestStartTime());
    }
}
