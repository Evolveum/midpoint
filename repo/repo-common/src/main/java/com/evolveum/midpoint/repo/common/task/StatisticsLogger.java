/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static com.evolveum.midpoint.repo.common.activity.state.ActivityItemProcessingStatistics.Operation;

import java.util.Locale;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPerformanceInformation;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskLoggingOptionType;

/**
 * Logs reasonable information on item and bucket completion, with a level of detail driven by logging option set.
 */
class StatisticsLogger {

    private static final Trace LOGGER = TraceManager.getTrace(StatisticsLogger.class);

    @NotNull private final IterativeActivityExecution<?, ?, ?, ?, ?, ?> activityExecution;

    StatisticsLogger(@NotNull IterativeActivityExecution<?, ?, ?, ?, ?, ?> activityExecution) {
        this.activityExecution = activityExecution;
    }

    void logItemCompletion(Operation operation, OperationResultStatus resultStatus) {

        long end = operation.getEndTimeMillis();
        TransientActivityExecutionStatistics current = activityExecution.getTransientExecutionStatistics();
        ActivityPerformanceInformation overall = getOverallStatistics();

        String mainMessage = String.format(Locale.US, "%s of %s%s done with status %s in %,1f ms.",
                activityExecution.getShortName(), operation.getIterationItemInformation(),
                activityExecution.getContextDescriptionSpaced(), resultStatus, operation.getDurationRounded());

        String counts = String.format(Locale.US,
                " Items processed in current execution: %,d (%,d overall), errors: %,d (%,d overall).",
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

        log(ActivityReportingOptions::getItemCompletionLogging, mainMessage, mainMessageAddition, fullStats);
    }

    void logBucketCompletion() {

        TransientActivityExecutionStatistics current = activityExecution.getTransientExecutionStatistics();
        long end = System.currentTimeMillis();
        ActivityPerformanceInformation overall = getOverallStatistics();

        RunningTask task = activityExecution.getRunningTask();
        String mainMessage =
                String.format("Finished a bucket for %s (%s in %s).%s",
                        activityExecution.getShortNameUncapitalized(), activityExecution.getActivityPath().toDebugName(), task,
                        task.canRun() ? "" : " Task was interrupted during processing.");


        String currentBrief = String.format(Locale.US, "Current execution: processed %,d objects in %.1f seconds, got %,d errors.",
                current.getItemsProcessed(), current.getWallClockTime(end) / 1000.0, current.getErrors());
        if (current.getItemsProcessed() > 0) {
            currentBrief += String.format(Locale.US, " Average processing time for one object: %,.1f milliseconds. "
                            + "Wall clock average: %,.1f milliseconds, throughput: %,.1f items per minute.",
                    current.getAverageTime(), current.getAverageWallClockTime(end), current.getThroughput(end));
        }

        String overallBrief = String.format(Locale.US, "Overall: processed %,d objects in %.1f seconds, got %,d errors. Real progress: %,d.",
                overall.getItemsProcessed(), ActivityItemProcessingStatisticsUtil.toSeconds(overall.getWallClockTime()),
                overall.getErrors(), overall.getProgress());
        if (overall.getItemsProcessed() > 0) {
            overallBrief += String.format(Locale.US, " Average processing time for one object: %,.1f milliseconds. "
                            + "Wall clock average: %,.1f milliseconds, throughput: %,.1f items per minute.",
                    overall.getAverageTime(), overall.getAverageWallClockTime(),
                    overall.getThroughput());
        }

        String mainMessageAddition = "\n" + currentBrief + "\n" + overallBrief;
        String fullStats = getFullStatMessage(overall, end);

        log(ActivityReportingOptions::getItemCompletionLogging, mainMessage, mainMessageAddition, fullStats);
    }

    private void log(Function<ActivityReportingOptions, TaskLoggingOptionType> loggingExtractor, String mainMessage,
            String mainMessageAddition, String fullStats) {
        TaskLoggingOptionType logging = loggingExtractor.apply(activityExecution.getReportingOptions());
        if (logging == TaskLoggingOptionType.FULL) {
            LOGGER.info("{}\n\n{}", mainMessage, fullStats);
        } else if (logging == TaskLoggingOptionType.BRIEF) {
            LOGGER.info("{}{}", mainMessage, mainMessageAddition);
            LOGGER.debug("{}", fullStats);
        } else {
            LOGGER.debug("{}\n\n{}", mainMessage, fullStats);
        }
    }

    @NotNull
    private ActivityPerformanceInformation getOverallStatistics() {
        return ActivityPerformanceInformation.forRegularActivity(
                activityExecution.getActivityPath(),
                activityExecution.getActivityState().getLiveItemProcessingStatistics().getValueCopy(),
                activityExecution.getActivityState().getLiveProgress().getValueCopy());
    }

    private String getFullStatMessage(ActivityPerformanceInformation overall, long end) {
        TransientActivityExecutionStatistics current = activityExecution.getTransientExecutionStatistics();
        return String.format(Locale.US,
                "Items processed: %,d in current execution and %,d overall.\n"
                        + "Errors: %,d in current execution and %,d in overall.\n"
                        + "Real progress is %,d.\n\n"
                        + "Average duration is %,.1f ms (in current execution) and %,.1f ms (overall).\n"
                        + "Wall clock average is %,.1f ms (in current execution) and %,.1f ms (overall).\n"
                        + "Average throughput is %,.1f items per minute (in current execution) and %,.1f items per minute (overall).\n\n"
                        + "Processing time is %,.1f ms (for current execution) and %,.1f ms (overall)\n"
                        + "Wall-clock time is %,d ms (for current execution) and %,d ms (overall)\n"
                        + "Start time was:\n"
                        + " - for current execution: %s\n"
                        + " - overall:               %s\n",

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
