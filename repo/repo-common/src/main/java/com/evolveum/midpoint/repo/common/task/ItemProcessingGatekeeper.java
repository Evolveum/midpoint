/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.activity.state.ActivityItemProcessingStatistics.Operation;
import com.evolveum.midpoint.repo.common.activity.state.ActivityStatistics;
import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultBuilder;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.util.task.ActivityPerformanceInformation;
import com.evolveum.midpoint.task.api.ExecutionContext;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import static java.util.Objects.requireNonNull;

/**
 * Responsible for the necessary general procedures before and after processing of a single item.
 *
 * In particular that means:
 *
 * 1. Progress reporting: recording an "iterative operation" within the task
 * 2. Operation result management (completion, cleanup)
 * 3. Error handling: determination of the course of action when an error occurs<
 * 4. Acknowledgments: acknowledges sync event being processed (or not) *TODO ok?*
 * 5. Error reporting: creating appropriate operation execution record
 * 6. Tracing: writing a trace file covering the item processing
 * 7. Dynamic profiling: logging profiling info just during the operation (probably will be deprecated)
 * 8. Recording performance and other statistics *TODO which ones?*
 * 9. Cache entry/exit *TODO really?*
 * 10. Diagnostic logging
 *
 * The task-specific processing is invoked by calling {@link ItemProcessor#process(ItemProcessingRequest, RunningTask, OperationResult)}
 * method.
 */
class ItemProcessingGatekeeper<I> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemProcessingGatekeeper.class);

    private static final String OP_HANDLE = ItemProcessingGatekeeper.class.getName() + ".handle";

    /** Request to be processed */
    @NotNull private final ItemProcessingRequest<I> request;

    /** Task part execution that requested processing of this item. */
    @NotNull private final AbstractIterativeActivityExecution<I, ?, ?, ?> activityExecution;

    /** Local coordinator task that drives fetching items for processing. */
    @NotNull private final RunningTask coordinatorTask;

    /** Assigned worker task that executes the processing. May be the same as the coordinator task. */
    @NotNull private final RunningTask workerTask;

    /** Current {@link Operation}. Contains e.g. the timing information. */
    private Operation operation;

    /**
     * Tuple "name, display name, type, OID" that is to be written to the iterative task information.
     */
    @NotNull private final IterationItemInformation iterationItemInformation;

    /**
     * Did we have requested tracing for this item? We need to know it to decide if we should write the trace.
     * Relying on {@link OperationResult#isTraced()} is not enough.
     */
    private boolean tracingRequested;

    /** What was the final processing result? */
    private ProcessingResult processingResult;

    /**
     * True if the flow of new events can continue. It can be switched to false either if the item processor
     * or error-handling routine tells so. Generally, it must be very harsh situation that results in immediate task stop.
     * An example is when a threshold is reached.
     */
    private boolean canContinue = true;

    ItemProcessingGatekeeper(@NotNull ItemProcessingRequest<I> request,
            @NotNull AbstractIterativeActivityExecution<I, ?, ?, ?> activityExecution,
            @NotNull RunningTask workerTask) {
        this.request = request;
        this.activityExecution = activityExecution;
        this.coordinatorTask = activityExecution.getRunningTask();
        this.workerTask = workerTask;
        this.iterationItemInformation = request.getIterationItemInformation();
    }

    boolean process(OperationResult parentResult) {

        ExecutionContext originalExecutionContext = workerTask.getExecutionContext();
        try {
            workerTask.setExecutionContext(activityExecution);

            startTracingAndDynamicProfiling();
            logOperationStart();
            operation = updateStatisticsOnStart();

            OperationResult result = doProcessItem(parentResult);
            try {

                stopTracingAndDynamicProfiling(result, parentResult);
                writeOperationExecutionRecord(result);

                if (isError()) {
                    canContinue = handleError(result) && canContinue;
                }

                acknowledgeItemProcessed(result);

                updateStatisticsOnEnd(result);
                logOperationEnd(result);

                cleanupAndSummarizeResults(result, parentResult);

                return canContinue;

            } catch (RuntimeException e) {

                // This is just to record the exception to the appropriate result.
                // The exception will be also recorded to the task result when handling
                // the errorState.permanentErrorException later.
                result.recordFatalError(e);
                throw e;
            }

        } catch (RuntimeException e) {

            // This is unexpected exception. We should perhaps stop the whole processing.
            // Just throwing the exception would simply kill one worker thread. This is something
            // that would easily be lost in the logs.

            activityExecution.errorState.setStoppingException(e);

            LoggingUtils.logUnexpectedException(LOGGER, "Fatal error while doing administration over "
                            + "processing item {} in {}:{}. Stopping the whole processing.",
                    e, request.getItem(), coordinatorTask, workerTask);

            acknowledgeItemProcessedAsEmergency();

            return false;

        } finally {
            workerTask.setExecutionContext(originalExecutionContext);
        }
    }

    /**
     * Fills-in resultException and processingResult.
     */
    private OperationResult doProcessItem(OperationResult parentResult) {

        OperationResult result = new OperationResult("dummy");

        enterLocalCaches();
        try {
            result = initializeOperationResult(parentResult);

            canContinue = activityExecution.itemProcessor.process(request, workerTask, result);

            computeStatusIfNeeded(result);

            processingResult = ProcessingResult.fromOperationResult(result, getPrismContext());

        } catch (Throwable t) {

            result.recordFatalError(t);

            // This is an error with top-level exception.
            // Note that we intentionally do not rethrow the exception.
            processingResult = ProcessingResult.fromException(t, getPrismContext());

        } finally {
            RepositoryCache.exitLocalCaches();
        }

        return result;
    }

    /** This is just to ensure we will not wait indefinitely for the item to complete. */
    private void acknowledgeItemProcessedAsEmergency() {
        OperationResult dummyResult = new OperationResult("dummy");
        request.acknowledge(false, dummyResult);
    }

    private void acknowledgeItemProcessed(OperationResult result) {
        request.acknowledge(shouldReleaseItem(), result);
    }

    private boolean shouldReleaseItem() {
        return canContinue; // TODO some error handling here
    }

    public boolean isError() {
        return processingResult.isError();
    }

    public boolean isSkipped() {
        return processingResult.isSkip();
    }

    public boolean isSuccess() {
        return processingResult.isSuccess();
    }

    private void cleanupAndSummarizeResults(OperationResult result, OperationResult parentResult) {
        if (result.isSuccess() && !tracingRequested && !result.isTraced()) {
            // FIXME: hack. Hardcoded ugly summarization of successes. something like
            //   AbstractSummarizingResultHandler [lazyman]
            result.getSubresults().clear();
        }

        // parentResult is worker-thread-specific result (because of concurrency issues)
        // or parentResult as obtained in handle(..) method in single-thread scenario
        parentResult.summarize();
    }

    // FIXME
    private ActivityExecutionStatistics getCurrentBucketStatistics() {
        return activityExecution.executionStatistics;
    }

    private String getActivityShortNameCapitalized() {
        return activityExecution.getActivityShortNameCapitalized();
    }

    /** Must come after item and task statistics are updated. */
    private void logOperationEnd(OperationResult result) {

        logResultAndExecutionStatistics(result);

        if (isError() && getReportingOptions().isLogErrors()) {
            LOGGER.error("{} of object {} {} failed: {}", getActivityShortNameCapitalized(), iterationItemInformation,
                    getContextDesc(), processingResult.getMessage(), processingResult.exception);
        }
    }

    // TODO deduplicate with statistics output in AbstractIterativeTaskPartExecution
    // TODO decide on the final form of these messages
    private void logResultAndExecutionStatistics(OperationResult result) {
        ActivityExecutionStatistics bucketStatistics = getCurrentBucketStatistics();

        long now = operation.getEndTimeMillis();

        ActivityPerformanceInformation activityStatistics =
                ActivityPerformanceInformation.forRegularActivity(
                        activityExecution.getActivityPath(),
                        activityExecution.getActivityState().getLiveItemProcessingStatistics().getValueCopy(),
                        activityExecution.getActivityState().getLiveProgress().getValueCopy());

        String mainMessage = String.format(Locale.US, "%s of %s %s done with status %s.",
                getActivityShortNameCapitalized(), iterationItemInformation, getContextDesc(), result.getStatus());

        String briefStats = String.format(Locale.US, "Items processed: %,d (%,d in part), errors: %,d (%,d in part).",
                bucketStatistics.getItemsProcessed(), activityStatistics.getItemsProcessed(),
                bucketStatistics.getErrors(), activityStatistics.getErrors());

        Double partThroughput = activityStatistics.getThroughput();
        if (partThroughput != null) {
            briefStats += String.format(Locale.US, " Overall throughput: %,.1f items per minute.", partThroughput);
        }

        String fullStats = String.format(Locale.US,
                        "Items processed: %,d in current bucket and %,d in current part.\n"
                        + "Errors: %,d in current bucket and %,d in current part.\n"
                        + "Real progress is %,d.\n\n"
                        + "Duration for this item was %,.1f ms. Average duration is %,.1f ms (in current bucket) and %,.1f ms (in current part).\n"
                        + "Wall clock average is %,.1f ms (in current bucket) and %,.1f ms (in current part).\n"
                        + "Average throughput is %,.1f items per minute (in current bucket) and %,.1f items per minute (in current part).\n\n"
                        + "Processing time is %,.1f ms (for current bucket) and %,.1f ms (for current part)\n"
                        + "Wall-clock time is %,d ms (for current bucket) and %,d ms (for current part)\n"
                        + "Start time was:\n"
                        + " - for current bucket: %s\n"
                        + " - for current part:   %s\n",

                bucketStatistics.getItemsProcessed(), activityStatistics.getItemsProcessed(),
                bucketStatistics.getErrors(), activityStatistics.getErrors(),
                activityStatistics.getProgress(),
                operation.getDurationRounded(), bucketStatistics.getAverageTime(), activityStatistics.getAverageTime(),
                bucketStatistics.getAverageWallClockTime(now), activityStatistics.getAverageWallClockTime(),
                bucketStatistics.getThroughput(now), partThroughput,
                bucketStatistics.getProcessingTime(), activityStatistics.getProcessingTime(),
                bucketStatistics.getWallClockTime(now), activityStatistics.getWallClockTime(),
                XmlTypeConverter.createXMLGregorianCalendar(bucketStatistics.getStartTimeMillis()),
                activityStatistics.getEarliestStartTime());

        TaskLoggingOptionType logging = getReportingOptions().getItemCompletionLogging();
        if (logging == TaskLoggingOptionType.FULL) {
            LOGGER.info("{}\n\n{}", mainMessage, fullStats);
        } else if (logging == TaskLoggingOptionType.BRIEF) {
            LOGGER.info("{} {}", mainMessage, briefStats);
            LOGGER.debug("{}", fullStats);
        } else {
            LOGGER.debug("{}\n\n{}", mainMessage, fullStats);
        }
    }

    /**
     * Determines whether to continue, stop, or suspend.
     * TODO implement better
     */
    private boolean handleError(OperationResult result) {
        OperationResultStatus status = result.getStatus();
        Throwable exception = processingResult.getExceptionRequired();
        LOGGER.debug("Starting handling error with status={}, exception={}", status, exception);

        ErrorHandlingStrategyExecutor.FollowUpAction followUpAction =
                activityExecution.handleError(status, exception, request, result);

        switch (followUpAction) {
            case CONTINUE:
                return true;
            case STOP:
                activityExecution.errorState.setStoppingException(exception);
                return false;
            default:
                throw new AssertionError(followUpAction);
        }
    }

    private Operation recordIterativeOperationStart() {
        return activityExecution.getActivityState().getLiveItemProcessingStatistics()
                .recordOperationStart(new IterativeOperationStartInfo(iterationItemInformation));
    }

    private void recordIterativeOperationEnd(Operation operation) {
        // Does NOT increase structured progress. (Currently.)
        operation.done(processingResult.outcome, processingResult.exception);
    }

    private void computeStatusIfNeeded(OperationResult result) {
        // We do not want to override the result set by handler. This is just a fallback case
        if (result.isUnknown() || result.isInProgress()) {
            result.computeStatus();
        }
    }

    private OperationResult initializeOperationResult(OperationResult parentResult) throws SchemaException {
        OperationResultBuilder builder = parentResult.subresult(OP_HANDLE)
                .addParam("object", iterationItemInformation.toString());
        if (workerTask.getTracingRequestedFor().contains(TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING)) {
            tracingRequested = true;
            builder.tracingProfile(getTracer().compileProfile(workerTask.getTracingProfile(), parentResult));
        }
        return builder.build();
    }

    private void logOperationStart() {
        LOGGER.trace("{} starting for {} {}", getActivityShortNameCapitalized(), iterationItemInformation, getContextDesc());
    }

    private String getContextDesc() {
        return activityExecution.getContextDescription();
    }

    private void startTracingAndDynamicProfiling() {
        if (activityExecution.providesTracingAndDynamicProfiling()) {
            int objectsSeen = coordinatorTask.getAndIncrementObjectsSeen();
            workerTask.startDynamicProfilingIfNeeded(coordinatorTask, objectsSeen);
            workerTask.requestTracingIfNeeded(coordinatorTask, objectsSeen, TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING);
        }
    }

    private void stopTracingAndDynamicProfiling(OperationResult result, OperationResult parentResult) {
        workerTask.stopDynamicProfiling();
        workerTask.stopTracing();
        if (tracingRequested) {
            getTracer().storeTrace(workerTask, result, parentResult);
            TracingAppender.terminateCollecting(); // todo reconsider
            LevelOverrideTurboFilter.cancelLoggingOverride(); // todo reconsider
        }
    }

    private Tracer getTracer() {
        return getBeans().tracer;
    }

    private void enterLocalCaches() {
        RepositoryCache.enterLocalCaches(getCacheConfigurationManager());
    }

    private void writeOperationExecutionRecord(OperationResult result) {
        if (processingResult.isSkip()) {
            LOGGER.trace("Skipping writing operation execution record because the item was skipped: {}", processingResult);
            return;
        }

        if (getReportingOptions().isSkipWritingOperationExecutionRecords()) {
            LOGGER.trace("Skipping writing operation execution record because of the reporting options.");
            return;
        }

        OperationExecutionRecorderForTasks.Target target = request.getOperationExecutionRecordingTarget();
        if (target != null) {
            getOperationExecutionRecorder().recordOperationExecution(target, coordinatorTask,
                    activityExecution.activityIdentifier, result);
        } else {
            LOGGER.trace("No target to write operation execution record to.");
        }
    }

    private OperationExecutionRecorderForTasks getOperationExecutionRecorder() {
        return getBeans().operationExecutionRecorder;
    }

    private @NotNull ActivityReportingOptions getReportingOptions() {
        return activityExecution.getReportingOptions();
    }

    private CacheConfigurationManager getCacheConfigurationManager() {
        return getBeans().cacheConfigurationManager;
    }

    private @NotNull Operation updateStatisticsOnStart() {
        ActivityStatistics liveStats = activityExecution.getActivityState().getLiveStatistics();
        if (getReportingOptions().isEnableSynchronizationStatistics()) {
            liveStats.startCollectingSynchronizationStatistics(workerTask,
                    request.getIdentifier(), request.getSynchronizationSituationOnProcessingStart());
        }
        if (getReportingOptions().isEnableActionsExecutedStatistics()) {
            liveStats.startCollectingActivityExecutions(workerTask);
        }
        return recordIterativeOperationStart();
    }

    private void updateStatisticsOnEnd(OperationResult result) {
        recordIterativeOperationEnd(operation);
        ActivityStatistics liveStats = activityExecution.getActivityState().getLiveStatistics();
        if (getReportingOptions().isEnableSynchronizationStatistics()) {
            liveStats.stopCollectingSynchronizationStatistics(workerTask, processingResult.outcome);
        }
        if (getReportingOptions().isEnableActionsExecutedStatistics()) {
            liveStats.stopCollectingActivityExecutions(workerTask);
        }

        updateStatisticsInPartExecutionObject();
        updateStatisticsInTasks(result);
    }

    private void updateStatisticsInPartExecutionObject() {
        ActivityExecutionStatistics partStatistics = getCurrentBucketStatistics();
        partStatistics.incrementProgress();
        if (isError()) {
            partStatistics.incrementErrors();
        }
        partStatistics.addDuration(operation.getDurationRounded());
    }

    /**
     * Increments the progress and gives a task a chance to update its statistics.
     */
    private void updateStatisticsInTasks(OperationResult result) {
        // The structured progress is maintained only in the coordinator task
        activityExecution.incrementProgress(processingResult.outcome);
        //coordinatorTask.incrementStructuredProgress(activityExecution.activityIdentifier, processingResult.outcome);

        if (activityExecution.isMultithreaded()) {
            assert workerTask.isTransient();

            // In lightweight subtasks we store progress and operational statistics.
            // We DO NOT store structured progress there.
            workerTask.incrementProgressTransient();
            workerTask.updateStatisticsInTaskPrism(true);

            // In coordinator we have to update the statistics in prism:
            // operation stats, structured progress, and progress itself
            coordinatorTask.updateStatisticsInTaskPrism(false);

        } else {

            // Structured progress is incremented. Now we simply update all the stats in the coordinator task.
            coordinatorTask.updateStatisticsInTaskPrism(true);

        }

        // If needed, let us write current statistics into the repository.
        // There is no need to do this for worker task, because it is either the same as the coordinator, or it's a LAT.
        coordinatorTask.storeStatisticsIntoRepositoryIfTimePassed(result);
    }

    private PrismContext getPrismContext() {
        return getBeans().prismContext;
    }

    @NotNull
    private CommonTaskBeans getBeans() {
        return activityExecution.getBeans();
    }

    @Experimental
    private static class ProcessingResult {

        /** Outcome of the processing */
        @NotNull private final QualifiedItemProcessingOutcomeType outcome;

        /**
         * Exception (either native or constructed) related to the error in processing.
         * Always non-null if there is an error and null if there is no error! We can rely on this.
         */
        @Nullable private final Throwable exception;

        private ProcessingResult(@NotNull ItemProcessingOutcomeType outcome, @Nullable Throwable exception,
                PrismContext prismContext) {
            this.outcome = new QualifiedItemProcessingOutcomeType(prismContext)
                    .outcome(outcome);
            this.exception = exception;
            argCheck(outcome != ItemProcessingOutcomeType.FAILURE || exception != null,
                    "Error without exception");
        }

        static ProcessingResult fromOperationResult(OperationResult result, PrismContext prismContext) {
            if (result.isError()) {
                // This is an error without visible top-level exception, so we have to find one.
                Throwable exception = RepoCommonUtils.getResultException(result);
                return new ProcessingResult(ItemProcessingOutcomeType.FAILURE, exception, prismContext);
            } else if (result.isNotApplicable()) {
                return new ProcessingResult(ItemProcessingOutcomeType.SKIP, null, prismContext);
            } else {
                return new ProcessingResult(ItemProcessingOutcomeType.SUCCESS, null, prismContext);
            }
        }

        public static ProcessingResult fromException(Throwable e, PrismContext prismContext) {
            return new ProcessingResult(ItemProcessingOutcomeType.FAILURE, e, prismContext);
        }

        public boolean isError() {
            return outcome.getOutcome() == ItemProcessingOutcomeType.FAILURE;
        }

        public boolean isSuccess() {
            return outcome.getOutcome() == ItemProcessingOutcomeType.SUCCESS;
        }

        public boolean isSkip() {
            return outcome.getOutcome() == ItemProcessingOutcomeType.SKIP;
        }

        public String getMessage() {
            return exception != null ? exception.getMessage() : null;
        }

        public Throwable getExceptionRequired() {
            return requireNonNull(exception, "Error without exception");
        }

        @Override
        public String toString() {
            return "ProcessingResult{" +
                    "outcome=" + outcome +
                    ", exception=" + exception +
                    '}';
        }
    }
}
