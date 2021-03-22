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
import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultBuilder;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation.Operation;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.task.TaskPartPerformanceInformation;
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
 * The task-specific processing is invoked by calling {@link AbstractIterativeItemProcessor#process(ItemProcessingRequest, RunningTask, OperationResult)}
 * method.
 */
class ItemProcessingGatekeeper<I> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemProcessingGatekeeper.class);

    /** Request to be processed */
    @NotNull private final ItemProcessingRequest<I> request;

    /** The processor responsible for the actual processing of the item. */
    @NotNull private final AbstractIterativeItemProcessor<I, ?, ?, ?, ?> itemProcessor;

    /** Task part execution that requested processing of this item. */
    @NotNull private final AbstractIterativeTaskPartExecution<I, ?, ?, ?, ?> partExecution;

    /** The whole task execution. */
    @NotNull private final AbstractTaskExecution<?, ?> taskExecution;

    /** Local coordinator task that drives fetching items for processing. */
    @NotNull private final RunningTask coordinatorTask;

    /** Assigned worker task that executes the processing. May be the same as the coordinator task. */
    @NotNull private final RunningTask workerTask;

    /** Task handler related logger. It is here to enable turning on logging for specific task types. */
    @NotNull private final Trace logger;

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
            @NotNull AbstractIterativeItemProcessor<I, ?, ?, ?, ?> itemProcessor,
            @NotNull RunningTask workerTask) {
        this.request = request;
        this.itemProcessor = itemProcessor;
        this.partExecution = itemProcessor.partExecution;
        this.taskExecution = partExecution.taskExecution;
        this.coordinatorTask = partExecution.localCoordinatorTask;
        this.workerTask = workerTask;
        this.logger = partExecution.getLogger();
        this.iterationItemInformation = request.getIterationItemInformation();
    }

    boolean process(OperationResult parentResult) {

        try {

            startTracingAndDynamicProfiling();
            logOperationStart();
            operation = updateStatisticsOnStart();

            OperationResult result = doProcessItem(parentResult);

            stopTracingAndDynamicProfiling(result, parentResult);
            writeOperationExecutionRecord(result);

            canContinue = checkIfCanContinue(result) && canContinue;

            acknowledgeItemProcessed(result);

            updateStatisticsOnEnd(result);
            logOperationEnd(result);

            cleanupAndSummarizeResults(result, parentResult);

            return canContinue;

        } catch (RuntimeException e) {

            // This is unexpected exception. We should perhaps stop the whole processing.
            // Just throwing the exception would simply kill one worker thread. This is something
            // that would easily be lost in the logs.

            LoggingUtils.logUnexpectedException(LOGGER, "Fatal error while doing administration over "
                            + "processing item {} in {}:{}. Stopping the whole processing.",
                    e, request.getItem(), coordinatorTask, workerTask);

            acknowledgeItemProcessedAsEmergency();

            return false;
        }
    }

    /**
     * Fills-in resultException and processingStatus.
     */
    private OperationResult doProcessItem(OperationResult parentResult) {

        OperationResult result = new OperationResult("dummy");

        enterLocalCaches();
        try {
            result = initializeOperationResult(parentResult);

            canContinue = itemProcessor.process(request, workerTask, result);

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

    private CurrentBucketStatistics getCurrentBucketStatistics() {
        return partExecution.bucketStatistics;
    }

    private String getProcessShortNameCapitalized() {
        return partExecution.getProcessShortNameCapitalized();
    }

    /** Must come after item and task statistics are updated. */
    private void logOperationEnd(OperationResult result) {

        logResultAndExecutionStatistics(result);

        if (isError() && getReportingOptions().isLogErrors()) {
            logger.error("{} of object {} {} failed: {}", getProcessShortNameCapitalized(), iterationItemInformation,
                    getContextDesc(), processingResult.getMessage(), processingResult.exception);
        }
    }

    // TODO deduplicate with statistics output in AbstractIterativeTaskPartExecution
    // TODO decide on the final form of these messages
    // TODO turn on/off via reporting options
    private void logResultAndExecutionStatistics(OperationResult result) {
        if (!logger.isDebugEnabled()) {
            return;
        }

        CurrentBucketStatistics bucketStatistics = getCurrentBucketStatistics();

        long now = operation.getEndTimeMillis();

        OperationStatsType operationStats = coordinatorTask.getStoredOperationStatsOrClone();
        StructuredTaskProgressType structuredProgress = coordinatorTask.getStructuredProgressOrClone();
        TaskPartPerformanceInformation partStatistics =
                TaskPartPerformanceInformation.forCurrentPart(operationStats, structuredProgress);

        String message = String.format(Locale.US,
                "%s of %s %s done with status %s.\n\n"
                        + "Items processed: %,d in current bucket and %,d in current part.\n"
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
                getProcessShortNameCapitalized(), iterationItemInformation, getContextDesc(), result.getStatus(),
                bucketStatistics.getItemsProcessed(), partStatistics.getItemsProcessed(),
                bucketStatistics.getErrors(), partStatistics.getErrors(),
                partStatistics.getProgress(),
                operation.getDurationRounded(), bucketStatistics.getAverageTime(), partStatistics.getAverageTime(),
                bucketStatistics.getAverageWallClockTime(now), partStatistics.getAverageWallClockTime(),
                bucketStatistics.getThroughput(now), partStatistics.getThroughput(),
                bucketStatistics.getProcessingTime(), partStatistics.getProcessingTime(),
                bucketStatistics.getWallClockTime(now), partStatistics.getWallClockTime(),
                XmlTypeConverter.createXMLGregorianCalendar(bucketStatistics.getStartTimeMillis()),
                partStatistics.getEarliestStartTime());

        logger.debug("{}", message);
    }

    /**
     * Determines whether to continue, stop, or suspend.
     * TODO implement better
     */
    private boolean checkIfCanContinue(OperationResult result) {

        if (!isError()) {
            return true;
        }

        Throwable exception = processingResult.getExceptionRequired();

        TaskPartitionDefinitionType partDef = taskExecution.partDefinition;
        if (partDef == null) {
            return getContinueOnError(result.getStatus(), exception, request, result);
        }

        CriticalityType criticality = ExceptionUtil.getCriticality(partDef.getErrorCriticality(), exception, CriticalityType.PARTIAL);
        try {
            RepoCommonUtils.processErrorCriticality(iterationItemInformation.getObjectName(), criticality, exception, result);
            return true; // If we are here, the error is not fatal and we can continue.
        } catch (Throwable e) {
            // Exception means fatal error.
            taskExecution.setPermanentErrorEncountered(e);
            return false;
        }
    }

    private boolean getContinueOnError(@NotNull OperationResultStatus status, @NotNull Throwable exception,
            ItemProcessingRequest<?> request, OperationResult result) {
        ErrorHandlingStrategyExecutor.Action action = partExecution.determineErrorAction(status, exception, request, result);
        switch (action) {
            case CONTINUE:
                return true;
            case SUSPEND:
                taskExecution.setPermanentErrorEncountered(exception);
            case STOP:
            default:
                return false;
        }
    }

    private Operation recordIterativeOperationStart() {
        return workerTask.recordIterativeOperationStart(
                new IterativeOperationStartInfo(
                        iterationItemInformation, partExecution.getPartUri(), partExecution.getPartStartTimestamp()));
    }

    private void recordIterativeOperationEnd(Operation operation) {
        // Does NOT increase structured progress. (Currently.)
        operation.done(processingResult.outcome, processingResult.exception);
    }

    private void onSyncItemProcessingStart() {
        if (getReportingOptions().isEnableSynchronizationStatistics()) {
            workerTask.onSyncItemProcessingStart(request.getIdentifier(), request.getSynchronizationSituationOnProcessingStart());
        }
    }

    private void onSyncItemProcessingEnd() {
        if (getReportingOptions().isEnableSynchronizationStatistics()) {
            workerTask.onSyncItemProcessingEnd(request.getIdentifier(), processingResult.outcome);
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
        return getTaskHandler().getTracer();
    }

    private AbstractTaskHandler<?, ?> getTaskHandler() {
        return taskExecution.taskHandler;
    }

    private void computeStatusIfNeeded(OperationResult result) {
        // We do not want to override the result set by handler. This is just a fallback case
        if (result.isUnknown() || result.isInProgress()) {
            result.computeStatus();
        }
    }

    private OperationResult initializeOperationResult(OperationResult parentResult) throws SchemaException {
        OperationResultBuilder builder = parentResult.subresult(getTaskOperationPrefix() + ".handle")
                .addParam("object", iterationItemInformation.toString());
        if (workerTask.getTracingRequestedFor().contains(TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING)) {
            tracingRequested = true;
            builder.tracingProfile(getTracer().compileProfile(workerTask.getTracingProfile(), parentResult));
        }
        return builder.build();
    }

    private void logOperationStart() {
        logger.trace("{} starting for {} {}", getProcessShortNameCapitalized(), iterationItemInformation, getContextDesc());
    }

    private String getContextDesc() {
        return partExecution.getContextDescription();
    }

    private void startTracingAndDynamicProfiling() {
        if (partExecution.providesTracingAndDynamicProfiling()) {
            int objectsSeen = coordinatorTask.getAndIncrementObjectsSeen();
            workerTask.startDynamicProfilingIfNeeded(coordinatorTask, objectsSeen);
            workerTask.requestTracingIfNeeded(coordinatorTask, objectsSeen, TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING);
        }
    }

    private void enterLocalCaches() {
        RepositoryCache.enterLocalCaches(getCacheConfigurationManager());
    }

    private void writeOperationExecutionRecord(OperationResult result) {
        if (getReportingOptions().isSkipWritingOperationExecutionRecords()) {
            return;
        }

        OperationExecutionRecorderForTasks.Target target = request.getOperationExecutionRecordingTarget();
        RunningTask task = taskExecution.localCoordinatorTask;

        getOperationExecutionRecorder().recordOperationExecution(target, task, partExecution.partUri, result);
    }

    private OperationExecutionRecorderForTasks getOperationExecutionRecorder() {
        return getTaskHandler().getOperationExecutionRecorder();
    }

    private @NotNull TaskReportingOptions getReportingOptions() {
        return partExecution.getReportingOptions();
    }

    private CacheConfigurationManager getCacheConfigurationManager() {
        return getTaskHandler().getCacheConfigurationManager();
    }

    private String getTaskOperationPrefix() {
        return getTaskHandler().taskOperationPrefix;
    }

    private @NotNull Operation updateStatisticsOnStart() {
        onSyncItemProcessingStart();
        return recordIterativeOperationStart();
    }

    private void updateStatisticsOnEnd(OperationResult result) {
        recordIterativeOperationEnd(operation);
        onSyncItemProcessingEnd();

        updateStatisticsInPartExecutionObject();
        updateStatisticsInTasks(result);
    }

    private void updateStatisticsInPartExecutionObject() {
        CurrentBucketStatistics partStatistics = getCurrentBucketStatistics();
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
        coordinatorTask.incrementStructuredProgress(partExecution.partUri, processingResult.outcome);

        if (partExecution.isMultithreaded()) {
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
        return taskExecution.getPrismContext();
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
    }
}
