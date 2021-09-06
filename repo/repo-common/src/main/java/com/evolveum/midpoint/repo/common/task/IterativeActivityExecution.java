/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.PARTIAL_ERROR;
import static com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil.*;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.toLong;

import java.util.Objects;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.state.OtherActivityState;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.task.reports.ConnIdOperationsReport;
import com.evolveum.midpoint.repo.common.task.reports.ItemsReport;

import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.reporting.ConnIdOperation;
import com.evolveum.midpoint.task.api.ConnIdOperationsListener;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.execution.LocalActivityExecution;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.activity.state.ActivityItemProcessingStatistics;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.repo.common.task.work.GetBucketOperationOptions;
import com.evolveum.midpoint.repo.common.task.work.GetBucketOperationOptions.GetBucketOperationOptionsBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Represents an execution of an iterative activity: either plain iterative one or search-based one.
 *
 * Responsibilities at this level of abstraction:
 *
 * 1. Orchestrates the basic execution cycle - see {@link #executeLocal(OperationResult)}:
 *
 * a. calls before/after execution "hook" methods + the main execution routine,
 * b. generates the execution result based on kind(s) of error(s) encountered.
 *
 * 2. Orchestrates the basic bucket execution cycle - see {@link #executeOrAnalyzeOrSkipSingleBucket(OperationResult)}:
 *
 * a. item source preparation,
 * b. before/after bucket execution "hook" methods, along with main {@link #iterateOverItemsInBucket(OperationResult)} method,
 * c. sets up and winds down the coordinator,
 *
 */
public abstract class IterativeActivityExecution<
        I,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType,
        AE extends IterativeActivityExecution<I, WD, AH, ?, ?, ?>,
        AES extends IterativeActivityExecutionSpecifics>
        extends LocalActivityExecution<WD, AH, WS> implements ExecutionSupport {

    private static final Trace LOGGER = TraceManager.getTrace(IterativeActivityExecution.class);

    private static final long FREE_BUCKET_WAIT_TIME = -1; // indefinitely

    /**
     * Things like "Import", "Reconciliation (on resource)", and so on. The first letter should be a capital.
     *
     * Used e.g. in log messages like:
     *
     * "Import of UserType:jack (Jack Sparrow, c0c010c0-d34d-b33f-f00d-111111111111) from Crew Management has been started"
     */
    @NotNull final String shortName;

    /**
     * Information that augments the process short name. Used e.g. in log messages.
     *
     * An example: "from [resource]".
     */
    @NotNull private String contextDescription;

    /**
     * Current bucket that is being processed.
     *
     * It is used to narrow the search query for search-based activities.
     */
    protected WorkBucketType bucket;

    /**
     * Information needed to manage buckets.
     *
     * Determined on the execution start.
     */
    private BucketingSituation bucketingSituation;

    /**
     * Schedules individual items for processing by worker threads (if running in multiple threads).
     * Re-created for each individual bucket.
     */
    protected ProcessingCoordinator<I> coordinator;

    /**
     * Determines and executes error handling strategy for this activity.
     */
    @NotNull private final ErrorHandlingStrategyExecutor errorHandlingStrategyExecutor;

    /**
     * Error state. In particular, should we stop immediately because of a fatal exception?
     *
     * TODO rethink this
     */
    @NotNull protected final ErrorState errorState = new ErrorState();

    /**
     * Reporting options specific for this activity execution. They are obtained by merging
     * {@link IterativeActivityExecutionSpecifics#getDefaultReportingOptions()} with the options
     * configured for the specific activity ({@link ActivityDefinition#specificReportingOptions}).
     */
    @NotNull protected final ActivityReportingOptions reportingOptions;

    /**
     * Maintains selected statistical information related to processing items in the current execution.
     * It is like a simplified version of {@link ActivityItemProcessingStatistics} that cover all the executions
     * (and sometimes all the realizations) of an activity.
     */
    @NotNull final TransientActivityExecutionStatistics transientExecutionStatistics;

    /** Useful Spring beans. */
    @NotNull protected final CommonTaskBeans beans;

    /** Custom execution logic and state. */
    @NotNull protected final AES executionSpecifics;

    /**
     * Listener for ConnId operations that occur outside item processing (e.g. during search and pre-processing).
     */
    @NotNull private final ConnIdOperationsListener globalConnIdOperationsListener;

    public IterativeActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
            @NotNull String shortName,
            @NotNull SpecificsSupplier<AE, AES> specificsSupplier) {
        super(context);
        this.transientExecutionStatistics = new TransientActivityExecutionStatistics();
        this.shortName = shortName;
        this.contextDescription = "";
        this.beans = taskExecution.getBeans();
        //noinspection unchecked
        this.executionSpecifics = specificsSupplier.supply((AE) this);
        this.reportingOptions = executionSpecifics.getDefaultReportingOptions()
                .cloneWithConfiguration(context.getActivity().getDefinition().getReportingDefinition().getBean());
        this.errorHandlingStrategyExecutor = new ErrorHandlingStrategyExecutor(getActivity(), getRunningTask(),
                getDefaultErrorAction(), beans);
        this.globalConnIdOperationsListener = new GlobalConnIdOperationsListener();
    }

    protected @NotNull ActivityExecutionResult executeLocal(OperationResult result)
            throws ActivityExecutionException, CommonException {

        LOGGER.trace("{}: Starting with local coordinator task {}", shortName, getRunningTask());

        try {
            enableGlobalConnIdOperationsListener();

            transientExecutionStatistics.recordExecutionStart();

            executionSpecifics.beforeExecution(result);
            doExecute(result);
            executionSpecifics.afterExecution(result);

            ActivityExecutionResult executionResult = createExecutionResult();

            LOGGER.trace("{} run finished (task {}, execution result {})", shortName, getRunningTask(),
                    executionResult);

            return executionResult;

        } finally {
            disableGlobalConnIdOperationsListener();
            getActivityState().getConnIdOperationsReport().flush(getRunningTask(), result);
        }
    }

    /**
     * Bucketed version of the execution.
     */
    private void doExecute(OperationResult result)
            throws ActivityExecutionException, CommonException {

        RunningTask task = taskExecution.getRunningTask();
        boolean initialExecution = true;

        bucketingSituation = determineBucketingSituation();

//        resetWorkStateAndStatisticsIfWorkComplete(result);
//        startCollectingStatistics(task, handler);

        for (; task.canRun(); initialExecution = false) {

            bucket = getWorkBucket(initialExecution, result);
            if (bucket == null) {
                LOGGER.trace("No (next) work bucket within {}, exiting", task);
                break;
            }

            boolean complete = false;
            try {
                if (!task.canRun()) {
                    break;
                }

                complete = executeOrAnalyzeOrSkipSingleBucket(result);
                if (!complete) {
                    break;
                }

            } finally {
                if (!complete) {
                    // This is either when the task was stopped (canRun is false or there's an stopping exception)
                    // or an unhandled exception occurred.
                    //
                    // This most probably means that the task is going to be suspended. So let us release the buckets
                    // to allow their processing by other workers.
                    releaseAllBucketsWhenWorker(result);
                }
            }
        }
    }

    private boolean shouldProcessBucket(OperationResult result) {
        ExpressionType condition = getActivity().getControlFlowDefinition().getBucketProcessingCondition();
        if (condition == null) {
            return true;
        }

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_BUCKET, bucket, WorkBucketType.class);

        try {
            return ExpressionUtil.evaluateConditionDefaultTrue(variables, condition, null,
                    beans.expressionFactory, "bucket condition expression", getRunningTask(), result);
        } catch (CommonException e) {
            throw new SystemException("Couldn't evaluate bucket processing condition: " + e.getMessage(), e);
        }
    }

    @NotNull
    private ActivityItemProcessingStatistics getLiveItemProcessing() {
        return activityState.getLiveStatistics().getLiveItemProcessing();
    }

    private WorkBucketType getWorkBucket(boolean initialExecution, OperationResult result) {
        RunningTask task = taskExecution.getRunningTask();

        WorkBucketType bucket;
        try {
            GetBucketOperationOptions options = GetBucketOperationOptionsBuilder.anOptions()
                    .withDistributionDefinition(activity.getDefinition().getDistributionDefinition())
                    .withFreeBucketWaitTime(FREE_BUCKET_WAIT_TIME)
                    .withCanRun(task::canRun)
                    .withExecuteInitialWait(initialExecution)
                    .withImplicitSegmentationResolver(executionSpecifics)
                    .withIsScavenger(isScavenger(task))
                    .build();
            bucket = beans.bucketingManager.getWorkBucket(bucketingSituation.coordinatorTaskOid,
                    bucketingSituation.workerTaskOid, activity.getPath(), options, getLiveBucketManagementStatistics(), result);
            task.refresh(result); // We want to have the most current state of the running task.
        } catch (InterruptedException e) {
            LOGGER.trace("InterruptedExecution in getWorkBucket for {}", task);
            if (!task.canRun()) {
                return null;
            } else {
                LoggingUtils.logUnexpectedException(LOGGER, "Unexpected InterruptedException in {}", e, task);
                throw new SystemException("Unexpected InterruptedException: " + e.getMessage(), e);
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't allocate a work bucket for task {}", t, task);
            throw new SystemException("Couldn't allocate a work bucket for task: " + t.getMessage(), t);
        }
        return bucket;
    }

    private boolean isScavenger(RunningTask task) {
        return BucketingUtil.isScavenger(task.getActivitiesStateOrClone(), getActivityPath());
    }

    private void releaseAllBucketsWhenWorker(OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (bucketingSituation.workerTaskOid != null) {
            beans.bucketingManager.releaseAllWorkBucketsFromWorker(bucketingSituation.coordinatorTaskOid,
                    bucketingSituation.workerTaskOid, getActivityPath(), getLiveBucketManagementStatistics(), result);
        }
    }

    private void completeWorkBucketAndCommitProgress(OperationResult result) throws ActivityExecutionException {
        try {

            beans.bucketingManager.completeWorkBucket(bucketingSituation.coordinatorTaskOid, bucketingSituation.workerTaskOid,
                    getActivityPath(), bucket.getSequentialNumber(), getLiveBucketManagementStatistics(), result);

            activityState.getLiveProgress().onCommitPoint();
            activityState.updateProgressAndStatisticsNoCommit();

            // TODO update also the task-level statistics

            activityState.flushPendingModificationsChecked(result);
        } catch (CommonException e) {
            throw new ActivityExecutionException("Couldn't complete work bucket", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    private ActivityBucketManagementStatistics getLiveBucketManagementStatistics() {
        return activityState.getLiveStatistics().getLiveBucketManagement();
    }

    /**
     * Execute or analyze or skip a single bucket.
     *
     * @return true if the bucket was completed
     */
    private boolean executeOrAnalyzeOrSkipSingleBucket(OperationResult result) throws ActivityExecutionException, CommonException {
        if (!shouldProcessBucket(result)) {
            return skipSingleBucket(result);
        }

        prepareItemSource(result);

        if (isBucketsAnalysis()) {
            return analyzeSingleBucket(result);
        } else {
            return executeSingleBucket(result);
        }
    }

    private boolean skipSingleBucket(OperationResult result) throws ActivityExecutionException {
        LOGGER.debug("Skipping bucket {} because bucket processing condition evaluated to false", bucket);
        // Actually we could go without committing progress, but it does no harm, so we keep it here.
        completeWorkBucketAndCommitProgress(result);
        return true;
    }

    private boolean analyzeSingleBucket(OperationResult result) throws CommonException, ActivityExecutionException {
        Integer bucketSize = determineExpectedTotal(result);
        if (bucketSize != null) {
            LOGGER.info("Bucket size is {} for {}", bucketSize, bucket);
        } else {
            LOGGER.warn("Couldn't determine bucket size while analyzing bucket {}", bucket);
        }

        reportBucketAnalyzed(bucketSize, result);

        // Actually we could go without committing progress, but it does no harm, so we keep it here.
        completeWorkBucketAndCommitProgress(result);

        return true;
    }

    /**
     * @return true if the bucket was completed
     */
    private boolean executeSingleBucket(OperationResult result) throws ActivityExecutionException, CommonException {

        BucketExecutionRecord record = new BucketExecutionRecord(getLiveItemProcessing());

        executionSpecifics.beforeBucketExecution(result);

        setExpectedTotal(result);

        coordinator = setupCoordinatorAndWorkerThreads();
        try {
            iterateOverItemsInBucket(result);
        } finally {
            // This is redundant in the case of live sync event handling (because the handler gets a notification when all
            // items are submitted, and must stop the threads in order to allow provisioning to update the token).
            //
            // But overall, it is necessary to do this here in order to avoid endless waiting if any exception occurs.
            coordinator.finishProcessing(result);
        }

        executionSpecifics.afterBucketExecution(result);

        // TODO reconsider this: why do we update in-memory representation only?
        getRunningTask()
                .updateStatisticsInTaskPrism(true);

        new StatisticsLogger(this)
                .logBucketCompletion();

        boolean complete = canRun() && !errorState.wasStoppingExceptionEncountered();

        if (complete) {
            record.end(getLiveItemProcessing());

            completeWorkBucketAndCommitProgress(result);

            // We want to report bucket as completed only after it's really marked as completed.
            reportBucketCompleted(record, result);
        }

        return complete;
    }

    /**
     * Prepares the item source. E.g. for search-iterative tasks we prepare object type, query, and options here.
     *
     * Iterative activities delegate this method fully to the plugin. However, search-based activities provide
     * their own default implementation.
     */
    abstract protected void prepareItemSource(OperationResult result) throws ActivityExecutionException, CommonException;

    private ActivityExecutionResult createExecutionResult() {
        if (!canRun()) {
            return ActivityExecutionResult.interrupted();
        }

        Throwable stoppingException = errorState.getStoppingException();
        if (stoppingException != null) {
            // TODO In the future we should distinguish between permanent and temporary errors here.
            return ActivityExecutionResult.exception(FATAL_ERROR, PERMANENT_ERROR, stoppingException);
        } else if (transientExecutionStatistics.getErrors() > 0) {
            return ActivityExecutionResult.finished(PARTIAL_ERROR);
        } else {
            return ActivityExecutionResult.success();
        }
    }

    private void setExpectedTotal(OperationResult result) throws CommonException {
        Integer expectedTotal = determineExpectedTotal(result);
        getRunningTask().setExpectedTotal(toLong(expectedTotal));
        getRunningTask().flushPendingModifications(result);
    }

    /**
     * Determines "expected total" for the activity.
     * E.g. for search-iterative tasks we count the objects here. (Except for bucketed executions.)
     *
     * @return null if no value could be determined or is not applicable
     */
    protected abstract @Nullable Integer determineExpectedTotal(OperationResult opResult) throws CommonException;

    /**
     * Starts the item source (e.g. `searchObjectsIterative` call or `synchronize` call) and begins processing items
     * generated by it. Returns when the source finishes.
     *
     * For example:
     *
     * - for search-based tasks, this call returns immediately after the iterative search is over;
     * - for live sync task, this returns after all changes were fetched and acknowledged, and the resulting token was written;
     * - for async update task, this returns also after all changes were fetched and acknowledged and confirmed to the source.
     */
    protected abstract void iterateOverItemsInBucket(OperationResult result) throws CommonException;

    /**
     * Creates the processing coordinator and worker threads.
     */
    private ProcessingCoordinator<I> setupCoordinatorAndWorkerThreads() {
        ProcessingCoordinator<I> coordinator = new ProcessingCoordinator<>(getWorkerThreadsCount(), getRunningTask(), beans.taskManager);
        coordinator.createWorkerThreads(getReportingOptions());
        return coordinator;
    }

    public long getStartTimeMillis() {
        return transientExecutionStatistics.startTimeMillis;
    }

    public boolean isMultithreaded() {
        return coordinator.isMultithreaded();
    }

    private Integer getWorkerThreadsCount() {
        return getActivity().getDistributionDefinition().getWorkerThreads();
    }

    /**
     * Fails if worker threads are defined. To be used in tasks that do not support multithreading.
     */
    public void ensureNoWorkerThreads() {
        int threads = getWorkerThreadsCount();
        if (threads != 0) {
            throw new UnsupportedOperationException("Unsupported number of worker threads: " + threads +
                    ". This task cannot be run with worker threads. Please remove workerThreads task "
                    + "extension property and/or workerThreads distribution definition item or set its value to 0.");
        }
    }

    public @NotNull String getShortName() {
        return shortName;
    }

    public @NotNull String getShortNameUncapitalized() {
        return StringUtils.uncapitalize(shortName);
    }

    public @NotNull String getContextDescription() {
        return contextDescription;
    }

    /**
     * Inserts a space before context description if it's not empty.
     */
    public @NotNull String getContextDescriptionSpaced() {
        return !contextDescription.isEmpty() ? " " + contextDescription : "";
    }

    public void setContextDescription(String value) {
        this.contextDescription = ObjectUtils.defaultIfNull(value, "");
    }

    ErrorHandlingStrategyExecutor.FollowUpAction handleError(@NotNull OperationResultStatus status,
            @NotNull Throwable exception, ItemProcessingRequest<?> request, OperationResult result) {
        return errorHandlingStrategyExecutor.handleError(status, exception, request.getObjectOidToRecordRetryTrigger(), result);
    }

    /**
     * @return Default error action if no policy is specified or if no policy entry matches.
     */
    protected @NotNull abstract ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction();

    public @NotNull ActivityReportingOptions getReportingOptions() {
        return reportingOptions;
    }

    public @NotNull String getRootTaskOid() {
        return getRunningTask().getRootTaskOid();
    }

    protected @NotNull Task getRootTask(OperationResult result) throws SchemaException {
        String rootTaskOid = getRootTaskOid();
        RunningTask task = getRunningTask();
        if (task.getOid().equals(rootTaskOid)) {
            return task;
        } else {
            try {
                return beans.taskManager.getTaskPlain(rootTaskOid, result);
            } catch (ObjectNotFoundException e) {
                // This is quite unexpected so it can be rethrown as SystemException
                throw new SystemException("The root task was not found", e);
            }
        }
    }

    @Override
    public boolean doesSupportStatistics() {
        return true;
    }

    @Override
    public boolean doesSupportSynchronizationStatistics() {
        return reportingOptions.isEnableSynchronizationStatistics();
    }

    @Override
    public boolean doesSupportActionsExecuted() {
        return reportingOptions.isEnableActionsExecutedStatistics();
    }

    public ProcessingCoordinator<I> getCoordinator() {
        return coordinator;
    }

    @NotNull public TransientActivityExecutionStatistics getTransientExecutionStatistics() {
        return transientExecutionStatistics;
    }

    public abstract boolean processItem(@NotNull ItemProcessingRequest<I> request, @NotNull RunningTask workerTask,
            OperationResult result) throws ActivityExecutionException, CommonException;

    @Override
    protected @NotNull ActivityState determineActivityStateForCounters(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        ActivityState explicit = executionSpecifics.useOtherActivityStateForCounters(result);
        if (explicit != null) {
            return explicit;
        }

        if (activityState.isWorker()) {
            return getCoordinatorActivityState();
        } else {
            return activityState;
        }
    }

    /** Returns activity state of the coordinator task. Assuming we are in worker task. */
    private ActivityState getCoordinatorActivityState() {
        Task parentTask = java.util.Objects.requireNonNull(
                getRunningTask().getParentTask(), "No parent task");
        return new OtherActivityState(
                parentTask,
                parentTask.getActivitiesStateOrClone(),
                getActivityPath(),
                getActivityStateDefinition().getWorkStateTypeName(),
                beans);
    }

    public @NotNull AES getExecutionSpecifics() {
        return executionSpecifics;
    }

    @NotNull ItemsReport getItemsReport() {
        return activityState.getItemsReport();
    }

    @NotNull ConnIdOperationsReport getConnIdOperationsReport() {
        return activityState.getConnIdOperationsReport();
    }

    private void reportBucketCompleted(BucketExecutionRecord executionRecord, OperationResult result) {
        if (shouldReportBuckets()) {
            activityState.getBucketsReport().recordBucketCompleted(
                    new BucketProcessingRecordType(PrismContext.get())
                            .sequentialNumber(bucket.getSequentialNumber())
                            .content(bucket.getContent())
                            .size(executionRecord.getTotalSize())
                            .itemsSuccessfullyProcessed(executionRecord.success)
                            .itemsFailed(executionRecord.failure)
                            .itemsSkipped(executionRecord.skip)
                            .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(executionRecord.startTimestamp))
                            .endTimestamp(XmlTypeConverter.createXMLGregorianCalendar(executionRecord.endTimestamp))
                            .duration(executionRecord.getDuration()),
                    getRunningTask(), result);
        }
    }

    private void reportBucketAnalyzed(Integer size, OperationResult result) {
        if (shouldReportBuckets()) {
            activityState.getBucketsReport().recordBucketCompleted(
                    new BucketProcessingRecordType(PrismContext.get())
                            .sequentialNumber(bucket.getSequentialNumber())
                            .content(bucket.getContent())
                            .size(size),
                    getRunningTask(), result);
        }
    }

    private boolean shouldReportBuckets() {
        return activityState.getBucketsReport().isEnabled();
    }

    boolean shouldReportItems() {
        return activityState.getItemsReport().isEnabled();
    }

    boolean shouldReportConnIdOperations() {
        return activityState.getConnIdOperationsReport().isEnabled();
    }

    boolean shouldReportInternalOperations() {
        return activityState.getInternalOperationsReport().isEnabled();
    }

    @FunctionalInterface
    public interface SpecificsSupplier<AE extends IterativeActivityExecution<?, ?, ?, ?, ?, ?>,
            AES extends IterativeActivityExecutionSpecifics> {
        AES supply(AE activityExecution);
    }

    public WorkBucketType getBucket() {
        return bucket;
    }

    private @NotNull BucketingSituation determineBucketingSituation() {
        if (getActivityState().isWorker()) {
            return BucketingSituation.worker(getRunningTask());
        } else {
            return BucketingSituation.standalone(getRunningTask());
        }
    }

    private static class BucketingSituation {
        @NotNull private final String coordinatorTaskOid;
        @Nullable private final String workerTaskOid;

        private BucketingSituation(@NotNull String coordinatorTaskOid, @Nullable String workerTaskOid) {
            this.coordinatorTaskOid = coordinatorTaskOid;
            this.workerTaskOid = workerTaskOid;
        }

        public static BucketingSituation worker(RunningTask worker) {
            return new BucketingSituation(
                    Objects.requireNonNull(
                            worker.getParentTask(),
                            "No parent task for worker " + worker)
                            .getOid(),
                    worker.getOid());
        }

        public static BucketingSituation standalone(RunningTask task) {
            return new BucketingSituation(task.getOid(), null);
        }
    }

    /** Contains data needed to create a bucket completion record for the sake of reporting. */

    private static class BucketExecutionRecord {

        private final long startTimestamp;
        private long endTimestamp;

        private final int successAtStart;
        private final int failureAtStart;
        private final int skipAtStart;

        private int success;
        private int failure;
        private int skip;

        BucketExecutionRecord(@NotNull ActivityItemProcessingStatistics startStats) {
            startTimestamp = System.currentTimeMillis();
            ActivityItemProcessingStatisticsType statsBean = startStats.getValueCopy();
            successAtStart = getItemsProcessedWithSuccess(statsBean);
            failureAtStart = getItemsProcessedWithFailure(statsBean);
            skipAtStart = getItemsProcessedWithSkip(statsBean);
        }

        public void end(@NotNull ActivityItemProcessingStatistics endStats) {
            endTimestamp = System.currentTimeMillis();
            ActivityItemProcessingStatisticsType statsBean = endStats.getValueCopy();
            success = getItemsProcessedWithSuccess(statsBean) - successAtStart;
            failure = getItemsProcessedWithFailure(statsBean) - failureAtStart;
            skip = getItemsProcessedWithSkip(statsBean) - skipAtStart;
        }

        private int getTotalSize() {
            return success + failure + skip;
        }

        private long getDuration() {
            return endTimestamp - startTimestamp;
        }
    }

    void enableGlobalConnIdOperationsListener() {
        if (shouldReportConnIdOperations()) {
            getRunningTask().registerConnIdOperationsListener(globalConnIdOperationsListener);
        }
    }

    void disableGlobalConnIdOperationsListener() {
        if (shouldReportConnIdOperations()) {
            getRunningTask().unregisterConnIdOperationsListener(globalConnIdOperationsListener);
        }
    }

    /**
     * Listener for ConnId operations outside item processing.
     */
    private class GlobalConnIdOperationsListener implements ConnIdOperationsListener {

        @Override
        public void onConnIdOperationEnd(@NotNull ConnIdOperation operation) {
            getConnIdOperationsReport().addRecord(
                    operation.toOperationRecordBean()
                            .bucketSequentialNumber(bucket != null ? bucket.getSequentialNumber() : null));
        }
    }
}
