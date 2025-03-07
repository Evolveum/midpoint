/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingConditionEvaluator.AdditionalVariableProvider;
import com.evolveum.midpoint.repo.common.activity.run.reports.ActivityReportUtil;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStatistics;
import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.reporting.ConnIdOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultBuilder;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.task.api.ConnIdOperationsListener;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Responsible for the necessary general procedures before and after processing of a single item,
 * offloading {@link IterativeActivityRun} (or {@link ItemProcessingRequest}) from these duties.
 *
 * This class is instantiated for each {@link #request} being processed.
 *
 * The processing consists of - see {@link #process(OperationResult)}:
 *
 * 1. Progress reporting: recording an "iterative operation" within the activity
 * 2. Operation result management (completion, cleanup)
 * 3. Error handling: determination of the course of action when an error occurs.
 * 4. Acknowledgments: acknowledges sync event being processed (or not) *TODO ok?*
 * 5. Error reporting: creating appropriate operation execution record
 * 6. Tracing: writing a trace file covering the item processing (see {@link ItemProcessingMonitor})
 * 7. Dynamic profiling: logging profiling info just during the operation (probably will be deprecated) (see {@link ItemProcessingMonitor})
 * 8. Recording performance and other statistics *TODO which ones?*
 * 9. Cache entry/exit *TODO do we really should do that here?*
 * 10. Diagnostic logging
 *
 * The activity-specific processing is invoked by calling {@link ItemProcessor#processItem(ItemProcessingRequest, RunningTask, OperationResult)}
 * method.
 */
class ItemProcessingGatekeeper<I> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemProcessingGatekeeper.class);

    private static final String OP_PROCESS = ItemProcessingGatekeeper.class.getName() + ".process";
    private static final String OP_HANDLE = ItemProcessingGatekeeper.class.getName() + ".handle";

    /** Request to be processed */
    @NotNull private final ItemProcessingRequest<I> request;

    /** Activity run that requested processing of this item. */
    @NotNull private final IterativeActivityRun<I, ?, ?, ?> activityRun;

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
     * Did we have requested tracing for this item? (Either because of tracing configuration in the task,
     * or because of global tracing override.)
     *
     * We need to know it to decide if we should write the trace. Relying on {@link OperationResult#isTraced()} is not enough.
     */
    private boolean tracingRequested;

    /** Whether internal operations report was requested. */
    private boolean internalOperationReportRequested;

    /** Helper for evaluation conditions related to item processing. */
    @NotNull final ItemProcessingConditionEvaluator conditionEvaluator;

    /**
     * Helper for tracing and dynamic profiling functionality.
     */
    @NotNull private final ItemProcessingMonitor<I> itemProcessingMonitor;

    /** What was the final processing result? */
    private ProcessingResult processingResult;

    /**
     * True if the flow of new events can continue. It can be switched to false either if the item processor
     * or error-handling routine tells so. Generally, it must be very harsh situation that results in immediate
     * activity stop. An example is when a threshold is reached; or when live sync / async update activity wants
     * to stop because of unprocessable event.
     */
    private boolean canContinue = true;

    /**
     * Item-specific ConnID operations listener. Null if no listening takes place (either not requested,
     * or blocked by before-item condition).
     */
    @Nullable private ConnIdOperationsListener connIdOperationsListener;

    ItemProcessingGatekeeper(
            @NotNull ItemProcessingRequest<I> request,
            @NotNull IterativeActivityRun<I, ?, ?, ?> activityRun,
            @NotNull RunningTask workerTask) {
        this.request = request;
        this.activityRun = activityRun;
        this.coordinatorTask = activityRun.getRunningTask();
        this.workerTask = workerTask;
        this.iterationItemInformation = request.getIterationItemInformation();
        this.conditionEvaluator = new ItemProcessingConditionEvaluator(this);
        this.itemProcessingMonitor = new ItemProcessingMonitor<>(this);
    }

    boolean process(OperationResult parentResult) {

        OperationResult result = parentResult.subresult(OP_PROCESS)
                .build();

        try {
            workerTask.setExecutionSupport(activityRun);

            logOperationStart();
            operation = updateStatisticsOnStart();

            itemProcessingMonitor.startProfilingAndTracingIfNeeded(result);

            var oldSimulationTransaction = workerTask.setSimulationTransaction(activityRun.getSimulationTransaction());
            try {
                startLocalConnIdListeningIfNeeded(result);

                // After this call, "processingResult.operationResult" will be a closed child of "result".
                processingResult = doProcessItem(result);
            } finally {
                workerTask.setSimulationTransaction(oldSimulationTransaction);
                stopLocalConnIdOperationListening();
            }

            updateStatisticsOnEnd(result);

            storeTraceIfRequested(result);
            updateReports(result);

            itemProcessingMonitor.stopProfilingAndTracing();
            writeOperationExecutionRecord(result);

            if (isError()) {
                canContinue = handleError(result) && canContinue;
            }

            acknowledgeItemProcessed(result);

            logOperationEnd();

            return canContinue;

        } catch (RuntimeException | CommonException e) {

            result.recordFatalError(e);

            // This is unexpected exception. We should perhaps stop the whole processing.
            // Just throwing the exception would simply kill one worker thread. This is something
            // that would easily be lost in the logs.

            activityRun.getErrorState().setStoppingException(e);

            LoggingUtils.logUnexpectedException(LOGGER, "Fatal error while doing administration over "
                            + "processing item {} in {}:{}. Stopping the whole processing.",
                    e, request.getItem(), coordinatorTask, workerTask);

            acknowledgeItemProcessedAsEmergency();

            return false;

        } finally {

            result.close();
            cleanupAndSummarizeResults(parentResult);

            workerTask.setExecutionSupport(null);
        }
    }

    private void startLocalConnIdListeningIfNeeded(OperationResult result) {
        activityRun.disableGlobalConnIdOperationsListener();

        if (!activityRun.shouldReportConnIdOperations() ||
                !beforeConditionForConnIdReportPasses(result)) {
            return;
        }

        connIdOperationsListener = new ItemRelatedConnIdOperationListener();
        workerTask.registerConnIdOperationsListener(connIdOperationsListener);
    }

    private void stopLocalConnIdOperationListening() {
        activityRun.enableGlobalConnIdOperationsListener();

        if (connIdOperationsListener != null) {
            workerTask.unregisterConnIdOperationsListener(connIdOperationsListener);
        }
    }

    private void updateReports(OperationResult result) {
        if (internalOperationReportRequested &&
                afterConditionForInternalOpReportPasses(result)) {
            activityRun.getActivityState().getInternalOperationsReport()
                    .add(request, activityRun.getBucket(), processingResult.operationResult, workerTask, result);
        }
        activityRun.getConnIdOperationsReport().flush(workerTask, result);
        reportItemProcessed(result);
    }

    private boolean beforeConditionForConnIdReportPasses(OperationResult result) {
        ConnIdOperationsReportDefinitionType def =
                activityRun.getActivity().getReportingDefinition().getConnIdOperationsReportDefinition();
        assert def != null;
        return conditionEvaluator.anyItemReportingConditionApplies(def.getBeforeItemCondition(), result);
    }

    private boolean beforeConditionForInternalOpReportPasses(OperationResult result) {
        InternalOperationsReportDefinitionType def =
                activityRun.getActivity().getReportingDefinition().getInternalOperationsReportDefinition();
        assert def != null;
        return conditionEvaluator.anyItemReportingConditionApplies(def.getBeforeItemCondition(), result);
    }

    private boolean afterConditionForInternalOpReportPasses(OperationResult result) {
        InternalOperationsReportDefinitionType def =
                activityRun.getActivity().getReportingDefinition().getInternalOperationsReportDefinition();
        assert def != null;
        return conditionEvaluator.anyItemReportingConditionApplies(def.getAfterItemCondition(),
                afterItemProcessingVariableProvider(), result);
    }

    private void reportItemProcessed(OperationResult result) {
        if (activityRun.shouldReportItems()) {
            IterativeOperationStartInfo startInfo = operation.getStartInfo();
            ItemProcessingRecordType record = new ItemProcessingRecordType()
                    .sequentialNumber(request.getSequentialNumber())
                    .name(startInfo.getItem().getObjectName())
                    .displayName(startInfo.getItem().getObjectDisplayName())
                    .type(startInfo.getItem().getObjectType())
                    .oid(startInfo.getItem().getObjectOid())
                    .bucketSequentialNumber(activityRun.getBucket().getSequentialNumber())
                    .outcome(processingResult.outcome)
                    .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(startInfo.getStartTimeMillis()))
                    .endTimestamp(XmlTypeConverter.createXMLGregorianCalendar(operation.getEndTimeMillis()))
                    .duration(operation.getDurationRounded())
                    .errorMessage(processingResult.getMessage());
            activityRun.getItemsReport().recordItemProcessed(record, workerTask, result);
        }
    }

    /**
     * Fills-in resultException and processingResult.
     */
    private @NotNull ProcessingResult doProcessItem(OperationResult parentResult) {

        OperationResult itemOpResult = new OperationResult("dummy");

        enterLocalCaches(); // FIXME may be dangerous e.g. for multi-propagation task!!
        try {
            itemOpResult = initializeOperationResultIncludingTracingOrReporting(parentResult);

            if (activityRun.isNoExecution()) {
                itemOpResult.recordNotApplicable("'No processing' execution mode is selected");
                canContinue = true;
            } else if (!conditionEvaluator.evaluateConditionDefaultTrue(getItemProcessingCondition(), null, itemOpResult)) {
                itemOpResult.recordNotApplicable("Processing skipped because the item processing condition is false");
                canContinue = true;
            } else {
                canContinue = activityRun.processItem(request, workerTask, itemOpResult);
            }

            computeStatusIfNeeded(itemOpResult);

            return ProcessingResult.fromOperationResult(itemOpResult);

        } catch (Throwable t) {

            itemOpResult.recordFatalError(t);

            // This is an error with top-level exception.
            // Note that we intentionally do not rethrow the exception.
            return ProcessingResult.fromException(itemOpResult, t);

        } finally {
            RepositoryCache.exitLocalCaches();
            itemOpResult.close();
        }
    }

    private @Nullable ExpressionType getItemProcessingCondition() {
        return getActivityDefinition().getControlFlowDefinition().getItemProcessingCondition();
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

    private void cleanupAndSummarizeResults(OperationResult parentResult) {
        assert processingResult.operationResult.isClosed();
        processingResult.operationResult.deleteSubresultsIfPossible();

        // parentResult is worker-thread-specific result (because of concurrency issues)
        // or parentResult as obtained in handle(..) method in single-thread scenario
        parentResult.summarize();
    }

    /** Must come after item and task statistics are updated. */
    private void logOperationEnd() {

        new StatisticsLogger(activityRun)
                .logItemCompletion(operation, processingResult.operationResult.getStatus());

        if (isError() && getReportingCharacteristics().isLogErrors()) {
            LOGGER.error("{} of object {}{} failed: {}", activityRun.getShortName(), iterationItemInformation,
                    activityRun.getContextDescriptionSpaced(), processingResult.getMessage(), processingResult.exception);
        }
    }

    /**
     * Determines whether to continue, stop, or suspend.
     * TODO implement better
     */
    private boolean handleError(OperationResult result) {
        OperationResultStatus status = processingResult.operationResult.getStatus();
        Throwable exception = processingResult.getExceptionRequired();
        LOGGER.debug("Starting handling error with status={}, exception={}", status, exception.getMessage(), exception);

        ErrorHandlingStrategyExecutor.FollowUpAction followUpAction =
                activityRun.handleError(status, exception, request, result);

        LOGGER.debug("Follow-up action: {}", followUpAction);

        return switch (followUpAction) {
            case CONTINUE -> true;
            case STOP -> {
                activityRun.getErrorState().setStoppingException(exception);
                yield false;
            }
        };
    }

    private Operation recordIterativeOperationStart() {
        return activityRun.getActivityState().getLiveItemProcessingStatistics()
                .recordOperationStart(new IterativeOperationStartInfo(iterationItemInformation));
    }

    private void recordIterativeOperationEnd(Operation operation) {
        // Does NOT increase progress. (Currently.)
        operation.done(processingResult.outcome, processingResult.exception);
    }

    private void computeStatusIfNeeded(OperationResult result) {
        // We do not want to override the result set by handler. This is just a fallback case
        if (result.isUnknown() || result.isInProgress()) {
            result.computeStatus();
        }
    }

    private OperationResult initializeOperationResultIncludingTracingOrReporting(OperationResult parentResult) throws SchemaException {
        OperationResultBuilder builder = parentResult.subresult(OP_HANDLE)
                .addParam("object", iterationItemInformation.toString());
        if (workerTask.isTracingRequestedFor(TracingRootType.ACTIVITY_ITEM_PROCESSING)) {
            tracingRequested = true;
            builder.tracingProfile(getTracer().compileProfile(workerTask.getTracingProfile(), parentResult));
        } else if (activityRun.shouldReportInternalOperations() &&
                beforeConditionForInternalOpReportPasses(parentResult)) {
            internalOperationReportRequested = true;
            builder.preserve();
        }
        return builder.build();
    }

    private void storeTraceIfRequested(OperationResult parentResult) {
        if (tracingRequested) {
            itemProcessingMonitor.storeTrace(getTracer(), afterItemProcessingVariableProvider(), workerTask,
                    processingResult.operationResult, parentResult);
            TracingAppender.removeSink(); // todo reconsider
            LevelOverrideTurboFilter.cancelLoggingOverride(); // todo reconsider
        }
    }

    private AdditionalVariableProvider afterItemProcessingVariableProvider() {
        return variables -> {
            variables.put(ExpressionConstants.VAR_OPERATION, operation, Operation.class);
            variables.put(ExpressionConstants.VAR_OPERATION_RESULT, processingResult.operationResult, OperationResult.class);
        };
    }

    private void logOperationStart() {
        LOGGER.trace("{} starting for {}{}", activityRun.getShortName(), iterationItemInformation,
                activityRun.getContextDescriptionSpaced());
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

        if (getReportingCharacteristics().isSkipWritingOperationExecutionRecords()) {
            LOGGER.trace("Skipping writing operation execution record because of the reporting options.");
            return;
        }

        OperationExecutionRecorderForTasks.Target target = request.getOperationExecutionRecordingTarget();
        if (target != null) {
            getOperationExecutionRecorder().recordOperationExecution(target, coordinatorTask,
                    activityRun.getActivityPath(), processingResult.operationResult, result);
        } else {
            LOGGER.trace("No target to write operation execution record to.");
        }
    }

    private OperationExecutionRecorderForTasks getOperationExecutionRecorder() {
        return getBeans().operationExecutionRecorder;
    }

    private CacheConfigurationManager getCacheConfigurationManager() {
        return getBeans().cacheConfigurationManager;
    }

    private @NotNull Operation updateStatisticsOnStart() {
        ActivityStatistics liveStats = activityRun.getActivityState().getLiveStatistics();
        if (getReportingCharacteristics().areSynchronizationStatisticsSupported()) {
            liveStats.startCollectingSynchronizationStatistics(workerTask,
                    request.getIdentifier(), request.getSynchronizationSituationOnProcessingStart());
        }
        if (getReportingCharacteristics().areActionsExecutedStatisticsSupported()) {
            liveStats.startCollectingActionsExecuted(workerTask);
        }
        return recordIterativeOperationStart();
    }

    private void updateStatisticsOnEnd(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        recordIterativeOperationEnd(operation);
        ActivityStatistics liveStats = activityRun.getActivityState().getLiveStatistics();
        if (getReportingCharacteristics().areSynchronizationStatisticsSupported()) {
            liveStats.stopCollectingSynchronizationStatistics(workerTask, processingResult.outcome);
        }
        if (getReportingCharacteristics().areActionsExecutedStatisticsSupported()) {
            liveStats.stopCollectingActionsExecuted(workerTask);
        }

        activityRun.getTransientRunStatistics().update(
                isError(),
                operation.getDurationRounded(),
                processingResult.getMessage());
        updateStatisticsInTasks(result);
    }

    /**
     * Increments the progress and updates various statistics in the tasks (LAT, coordinator, tree).
     */
    private void updateStatisticsInTasks(OperationResult result) throws SchemaException, ObjectNotFoundException {
        activityRun.incrementProgress(processingResult.outcome);

        boolean updateThreadLocalStatisticsInCoordinator;
        if (activityRun.isMultithreaded()) {
            assert workerTask.isTransient();

            // Lightweight subtasks: we store legacy progress and operational statistics in them.
            // Obviously, we DO NOT store activity progress nor activity-level statistics here.
            workerTask.incrementLegacyProgressTransient();
            workerTask.updateOperationStatsInTaskPrism(true);

            // We must not update thread local statistics in coordinator task, because we execute in a different thread.
            updateThreadLocalStatisticsInCoordinator = false;
        } else {
            // We can update TL statistics in coordinator, because we are the coordinator.
            updateThreadLocalStatisticsInCoordinator = true;
        }

        activityRun.updateStatistics(updateThreadLocalStatisticsInCoordinator, result);
        activityRun.updateItemProgressInTreeOverviewIfTimePassed(result);
    }

    @NotNull
    private CommonTaskBeans getBeans() {
        return activityRun.getBeans();
    }

    public @NotNull IterativeActivityRun<I, ?, ?, ?> getActivityRun() {
        return activityRun;
    }

    public @NotNull ActivityDefinition<?> getActivityDefinition() {
        return getActivityRun().getActivity().getDefinition();
    }

    private @NotNull ActivityReportingCharacteristics getReportingCharacteristics() {
        return getActivityRun().getReportingCharacteristics();
    }

    public @NotNull RunningTask getWorkerTask() {
        return workerTask;
    }

    @NotNull ItemProcessingRequest<I> getRequest() {
        return request;
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

        /**
         * The operation result for item processing. Should be closed.
         * Must not be used for further recording. (Only for analysis.)
         */
        @NotNull private final OperationResult operationResult;

        private ProcessingResult(
                @NotNull ItemProcessingOutcomeType outcome,
                @Nullable Throwable exception,
                @NotNull OperationResult operationResult) {
            this.operationResult = operationResult;
            this.outcome = new QualifiedItemProcessingOutcomeType()
                    .outcome(outcome);
            this.exception = exception;
            argCheck(outcome != ItemProcessingOutcomeType.FAILURE || exception != null,
                    "Error without exception");
        }

        static ProcessingResult fromOperationResult(OperationResult result) {
            if (result.isError()) {
                // This is an error without visible top-level exception, so we have to find one.
                Throwable exception = RepoCommonUtils.getResultException(result);
                return new ProcessingResult(ItemProcessingOutcomeType.FAILURE, exception, result);
            } else if (result.isNotApplicable()) {
                return new ProcessingResult(ItemProcessingOutcomeType.SKIP, null, result);
            } else {
                return new ProcessingResult(ItemProcessingOutcomeType.SUCCESS, null, result);
            }
        }

        static ProcessingResult fromException(OperationResult result, Throwable e) {
            return new ProcessingResult(ItemProcessingOutcomeType.FAILURE, e, result);
        }

        boolean isError() {
            return outcome.getOutcome() == ItemProcessingOutcomeType.FAILURE;
        }

        boolean isSuccess() {
            return outcome.getOutcome() == ItemProcessingOutcomeType.SUCCESS;
        }

        boolean isSkip() {
            return outcome.getOutcome() == ItemProcessingOutcomeType.SKIP;
        }

        String getMessage() {
            return exception != null ? exception.getMessage() : null;
        }

        Throwable getExceptionRequired() {
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

    /**
     * This listener forwards ConnId operations to the report,
     * enriched with the information about current item being processed.
     */
    private class ItemRelatedConnIdOperationListener implements ConnIdOperationsListener {

        @Override
        public void onConnIdOperationEnd(@NotNull ConnIdOperation operation) {
            ConnIdOperationRecordType record = operation.toOperationRecordBean();
            ActivityReportUtil.addItemInformation(record, request, activityRun.getBucket());

            activityRun.getConnIdOperationsReport().addRecord(record);
        }
    }
}
