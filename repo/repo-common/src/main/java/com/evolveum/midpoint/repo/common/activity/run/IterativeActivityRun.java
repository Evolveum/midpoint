/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.PARTIAL_ERROR;
import static com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil.*;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.HALTING_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import java.util.Objects;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.processing.ProcessingCoordinator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.activity.run.reports.ConnIdOperationsReport;
import com.evolveum.midpoint.repo.common.activity.run.reports.ItemsReport;

import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.reporting.ConnIdOperation;
import com.evolveum.midpoint.task.api.ConnIdOperationsListener;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityItemProcessingStatistics;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.repo.common.activity.run.buckets.GetBucketOperationOptions;
import com.evolveum.midpoint.repo.common.activity.run.buckets.GetBucketOperationOptions.GetBucketOperationOptionsBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Represents a run of an iterative activity: either plain iterative one or search-based one.
 *
 * Responsibilities at this level of abstraction:
 *
 * 1. Orchestrates the basic run cycle - see {@link #runLocally(OperationResult)}:
 *
 * a. calls before/after execution "hook" methods + the main execution routine,
 * b. generates the execution result based on kind(s) of error(s) encountered.
 *
 * 2. Orchestrates the basic bucket execution cycle - see {@link #processOrAnalyzeOrSkipSingleBucket(OperationResult)}:
 *
 * a. item source preparation,
 * b. before/after bucket execution "hook" methods, along with main {@link #iterateOverItemsInBucket(OperationResult)} method,
 * c. sets up and winds down the coordinator,
 *
 */
public abstract class IterativeActivityRun<
        I,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends LocalActivityRun<WD, AH, WS>
        implements ExecutionSupport, IterativeActivityRunSpecifics {

    private static final Trace LOGGER = TraceManager.getTrace(IterativeActivityRun.class);

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
     * Expected progress (overall) if it was determined for the current run.
     * It is used to avoid re-counting objects if there's no bucketing.
     * So only fresh values are stored here.
     */
    private Integer expectedTotal;

    /**
     * Information needed to manage buckets.
     *
     * Determined on the run start.
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
     * Maintains selected statistical information related to processing items in the current run.
     * It is like a simplified version of {@link ActivityItemProcessingStatistics} that cover all the runs
     * (and sometimes all the realizations) of an activity.
     */
    @NotNull protected final TransientActivityRunStatistics transientRunStatistics;

    /** Useful Spring beans. */
    @NotNull protected final CommonTaskBeans beans;

    /**
     * Listener for ConnId operations that occur outside item processing (e.g. during search and pre-processing).
     */
    @NotNull private final ConnIdOperationsListener globalConnIdOperationsListener;

    /**
     * Number of buckets announced to the activity tree state overview. Kept here to eliminate redundant updates.
     */
    private Integer numberOfBucketsAnnounced;

    public IterativeActivityRun(@NotNull ActivityRunInstantiationContext<WD, AH> context, @NotNull String shortName) {
        super(context);
        this.transientRunStatistics = new TransientActivityRunStatistics();
        this.shortName = shortName;
        this.contextDescription = "";
        this.beans = taskRun.getBeans();
        this.errorHandlingStrategyExecutor = new ErrorHandlingStrategyExecutor(getActivity(), getDefaultErrorAction(), beans);
        this.globalConnIdOperationsListener = new GlobalConnIdOperationsListener();

        getReportingDefinition().applyDefaults(reportingCharacteristics);
    }

    protected final @NotNull ActivityRunResult runLocally(OperationResult result)
            throws ActivityRunException, CommonException {

        LOGGER.trace("{}: Starting with local coordinator task {}", shortName, getRunningTask());

        String originalChannel = getRunningTask().getChannel();
        try {
            enableGlobalConnIdOperationsListener();
            overrideTaskChannelIfNeeded();

            transientRunStatistics.recordRunStart(getStartTimestampRequired());

            beforeRun(result);
            setTaskObjectRef(result); // this method is intentionally run after "beforeRun" (we need the resource ref)

            doRun(result);

            afterRun(result);

            ActivityRunResult runResult = createRunResult();

            LOGGER.trace("{} run finished (task {}, run result {})", shortName, getRunningTask(), runResult);

            return runResult;

        } finally {
            disableGlobalConnIdOperationsListener();
            cancelTaskChannelOverride(originalChannel);
            getActivityState().getConnIdOperationsReport().flush(getRunningTask(), result);
        }
    }

    private void cancelTaskChannelOverride(String originalChannel) {
        getRunningTask().setChannel(originalChannel);
    }

    private void overrideTaskChannelIfNeeded() {
        String channelOverride = getChannelOverride();
        if (channelOverride != null) {
            getRunningTask().setChannel(channelOverride);
        }
    }

    /** Channel URI that should be set into the task during this activity run. (If not null.) */
    protected @Nullable String getChannelOverride() {
        return null;
    }

    /**
     * Bucketed version of the run.
     */
    private void doRun(OperationResult result)
            throws ActivityRunException, CommonException {

        RunningTask task = taskRun.getRunningTask();
        boolean initialRun = true;

        bucketingSituation = determineBucketingSituation();

        setExpectedTotal(result);

        for (; task.canRun(); initialRun = false) {

            bucket = getWorkBucket(initialRun, result);
            if (bucket == null) {
                LOGGER.trace("No (next) work bucket within {}, exiting", task);
                break;
            }

            boolean complete = false;
            try {
                if (!task.canRun()) {
                    break;
                }

                complete = processOrAnalyzeOrSkipSingleBucket(result);
                pruneResult(result);

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

    /**
     * Keeps the result of reasonable size:
     *
     * 1. removes successful minor subresults
     * 2. summarizes operations (if there is a lot of buckets - see MID-7830)
     *
     * We catch all exceptions here, because we don't want the task to fail on these checks.
     * We also want to execute the summarization even if the cleanup fails.
     */
    private void pruneResult(OperationResult result) {
        try {
            // We cannot clean up the current (root) result, as it is not closed yet. So we do that on subresults.
            // (This means that minor subresult of the current root result will survive, but let's them be.
            // Hopefully they will be eliminated later e.g. when the result is finally stored into the task.)
            result.getSubresults().forEach(
                    OperationResult::cleanupResultDeeply);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't clean up the operation result in {}", e, this);
        }

        try {
            result.summarize();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't summarize the operation result in {}", e, this);
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

    private WorkBucketType getWorkBucket(boolean initialRun, OperationResult result)
            throws ActivityRunException {

        RunningTask task = taskRun.getRunningTask();

        Holder<BucketProgressOverviewType> bucketProgressHolder = new Holder<>();

        WorkBucketType bucket;
        try {
            GetBucketOperationOptions options = GetBucketOperationOptionsBuilder.anOptions()
                    .withDistributionDefinition(activity.getDefinition().getDistributionDefinition())
                    .withFreeBucketWaitTime(FREE_BUCKET_WAIT_TIME)
                    .withCanRun(task::canRun)
                    .withExecuteInitialWait(initialRun)
                    .withImplicitSegmentationResolver(this)
                    .withIsScavenger(isScavenger(task))
                    .withBucketProgressConsumer(bucketProgressHolder)
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

        announceNumberOfBuckets(bucketProgressHolder.getValue(), result);
        return bucket;
    }

    private void announceNumberOfBuckets(BucketProgressOverviewType bucketProgress, OperationResult result)
            throws ActivityRunException {
        if (bucketProgress != null && !Objects.equals(bucketProgress.getTotalBuckets(), numberOfBucketsAnnounced)) {
            getTreeStateOverview().updateBucketAndItemProgress(this, bucketProgress, result);
            numberOfBucketsAnnounced = bucketProgress.getTotalBuckets();
        }
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

    private void completeWorkBucketAndUpdateStatistics(OperationResult result) throws ActivityRunException {
        try {

            Holder<BucketProgressOverviewType> bucketProgressHolder = new Holder<>();
            beans.bucketingManager.completeWorkBucket(bucketingSituation.coordinatorTaskOid, bucketingSituation.workerTaskOid,
                    getActivityPath(), bucket.getSequentialNumber(), getLiveBucketManagementStatistics(),
                    bucketProgressHolder, result);

            activityState.getLiveProgress().onCommitPoint();
            activityState.updateProgressAndStatisticsNoCommit();

            // Note that we do not need to call the following method when bucket is not complete:
            // in such cases the activity finishes, so the task stats are updated on activity run end.
            getRunningTask()
                    .updateAndStoreStatisticsIntoRepository(true, result); // Contains implicit task flush

            getTreeStateOverview()
                    .updateBucketAndItemProgress(this, bucketProgressHolder.getValue(), result);

        } catch (CommonException e) {
            throw new ActivityRunException("Couldn't complete work bucket", FATAL_ERROR, PERMANENT_ERROR, e);
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
    private boolean processOrAnalyzeOrSkipSingleBucket(OperationResult result) throws ActivityRunException, CommonException {
        if (!shouldProcessBucket(result)) {
            return skipSingleBucket(result);
        }

        prepareItemSourceForCurrentBucket(result);

        if (isBucketAnalysis()) {
            return analyzeSingleBucket(result);
        } else {
            return processSingleBucket(result);
        }
    }

    private boolean skipSingleBucket(OperationResult result) throws ActivityRunException {
        LOGGER.debug("Skipping bucket {} because bucket processing condition evaluated to false", bucket);
        // Actually we could go without committing progress, but it does no harm, so we keep it here.
        completeWorkBucketAndUpdateStatistics(result);
        return true;
    }

    private boolean analyzeSingleBucket(OperationResult result) throws CommonException, ActivityRunException {
        Integer bucketSize = determineCurrentBucketSize(result);
        if (bucketSize != null) {
            LOGGER.info("Bucket size is {} for {}", bucketSize, bucket);
        } else {
            LOGGER.warn("Couldn't determine bucket size while analyzing bucket {}", bucket);
        }

        reportBucketAnalyzed(bucketSize, result);

        // Actually we could go without committing progress, but it does no harm, so we keep it here.
        completeWorkBucketAndUpdateStatistics(result);

        return true;
    }

    /**
     * @return true if the bucket was completed
     */
    private boolean processSingleBucket(OperationResult result) throws ActivityRunException, CommonException {

        BucketProcessingRecord record = new BucketProcessingRecord(getLiveItemProcessing());

        beforeBucketProcessing(result);

        setExpectedInCurrentBucket(result);

        coordinator = setupCoordinatorAndWorkerThreads();

        var oldTx = getRunningTask().setSimulationTransaction(
                simulationSupport.openSimulationTransaction(result));
        try {
            iterateOverItemsInBucket(result);
        } finally {
            // This is redundant in the case of live sync event handling (because the handler gets a notification when all
            // items are submitted, and must stop the threads in order to allow provisioning to update the token).
            //
            // But overall, it is necessary to do this here in order to avoid endless waiting if any exception occurs.
            coordinator.finishProcessing(result);
            getRunningTask().setSimulationTransaction(oldTx);
        }

        afterBucketProcessing(result);

        boolean complete = canRun() && !errorState.wasStoppingExceptionEncountered();

        new StatisticsLogger(this)
                .logBucketCompletion(complete);

        if (complete) {
            record.end(getLiveItemProcessing());

            completeWorkBucketAndUpdateStatistics(result);

            // We want to report bucket as completed only after it's really marked as completed.
            reportBucketCompleted(record, result);

            // We close the simulation result part only when the bucket is really completed. This means that when
            // a simulation task is suspended, the information from the currently processed bucket(s) is not available.
            // If we would commit it, the overall statistics would get broken when the task is resumed. If we want to see
            // the incomplete statistics, we'd need to store them separately - just like we do for progress information.
            simulationSupport.commitSimulationTransaction(result);
        }

        return complete;
    }

    /**
     * Prepares the item source. E.g. for search-iterative tasks we prepare object type, query, and options here.
     *
     * Iterative activities delegate this method fully to the plugin. However, search-based activities provide
     * their own default implementation.
     */
    abstract protected void prepareItemSourceForCurrentBucket(OperationResult result)
            throws ActivityRunException, CommonException;

    private ActivityRunResult createRunResult() {
        if (!canRun()) {
            return ActivityRunResult.interrupted();
        }

        Throwable stoppingException = errorState.getStoppingException();
        if (stoppingException != null) {
            // TODO In the future we should distinguish between permanent and temporary errors here.

            if (stoppingException instanceof ThresholdPolicyViolationException) {
                return ActivityRunResult.exception(FATAL_ERROR, HALTING_ERROR, stoppingException);
            }

            return ActivityRunResult.exception(FATAL_ERROR, PERMANENT_ERROR, stoppingException);
        } else if (transientRunStatistics.getErrors() > 0) {
            return ActivityRunResult
                    .finished(PARTIAL_ERROR)
                    .message(transientRunStatistics.getLastErrorMessage());
        } else {
            return ActivityRunResult.success();
        }
    }

    private void setExpectedTotal(OperationResult result) throws CommonException, ActivityRunException {
        Integer knownExpectedTotal = activityState.getLiveProgress().getExpectedTotal();

        Integer expectedTotal;
        if (isWorker()) {
            LOGGER.trace("Expected total progress is not supported for worker tasks yet.");
            // We'd need something executed before distributing activity creates the workers.
            expectedTotal = null;
        } else if (!shouldDetermineOverallSize(result)) {
            expectedTotal = null;
        } else if (getReportingDefinition().isCacheOverallSize()) {
            if (knownExpectedTotal != null) {
                return; // no need to set anything
            } else {
                this.expectedTotal = expectedTotal = determineOverallSize(result);
            }
        } else {
            this.expectedTotal = expectedTotal = determineOverallSize(result);
        }

        if (!Objects.equals(expectedTotal, knownExpectedTotal)) {
            activityState.getLiveProgress().setExpectedTotal(expectedTotal);
            activityState.updateProgressNoCommit();
            activityState.flushPendingTaskModificationsChecked(result);
        }
    }

    private boolean shouldDetermineOverallSize(OperationResult result) throws ActivityRunException, CommonException {
        ActivityOverallItemCountingOptionType option = getReportingDefinition().getDetermineOverallSize();
        switch (option) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case WHEN_IN_REPOSITORY:
                return isInRepository(result);
            default:
                throw new AssertionError(option);
        }
    }

    private void setExpectedInCurrentBucket(OperationResult result) throws CommonException, ActivityRunException {
        Integer bucketSize;
        if (expectedTotal != null && isNotBucketed()) {
            bucketSize = expectedTotal;
            LOGGER.trace("Determined bucket size from expected progress obtained earlier in this run: {}", bucketSize);
        } else if (shouldDetermineBucketSize(result)) {
            bucketSize = determineCurrentBucketSize(result);
            LOGGER.trace("Determined bucket size: {}", bucketSize);
        } else {
            bucketSize = null;
        }

        activityState.getLiveProgress().setExpectedInCurrentBucket(bucketSize);
        activityState.updateProgressNoCommit();
        activityState.flushPendingTaskModificationsChecked(result);
    }

    private boolean shouldDetermineBucketSize(OperationResult result) throws ActivityRunException, CommonException {
        ActivityItemCountingOptionType option = getReportingDefinition().getDetermineBucketSize();
        switch (option) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case WHEN_NOT_BUCKETED:
                return isNotBucketed();
            case WHEN_IN_REPOSITORY:
                return isInRepository(result);
            case WHEN_IN_REPOSITORY_AND_NOT_BUCKETED:
                return isInRepository(result) && isNotBucketed();
            default:
                throw new AssertionError(option);
        }
    }

    /**
     * BEWARE! assumes that bucket is already set. So use this method only in {@link #processSingleBucket(OperationResult)}!
     */
    private boolean isNotBucketed() {
        assert bucket != null;
        return !BucketingUtil.hasLimitations(bucket);
    }

    /** Do we execute over items in repository? (Maybe the name should be changed.) */
    protected abstract boolean isInRepository(OperationResult result) throws ActivityRunException, CommonException;

    /**
     * Determines expected progress (overall size) for the activity.
     * E.g. for search-based activities we count the objects here (overall).
     *
     * @return null if no value could be determined or is not applicable
     */
    public @Nullable Integer determineOverallSize(OperationResult result)
            throws CommonException, ActivityRunException {
        return null;
    }

    /**
     * Determines the current bucket size.
     * E.g. for search-based activities we count the objects here (in current bucket).
     *
     * @return null if no value could be determined or is not applicable
     */
    public @Nullable Integer determineCurrentBucketSize(OperationResult result) throws CommonException {
        return null;
    }

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
    private ProcessingCoordinator<I> setupCoordinatorAndWorkerThreads() throws ConfigurationException {
        ProcessingCoordinator<I> coordinator = new ProcessingCoordinator<>(getWorkerThreadsCount(), this);
        coordinator.createWorkerThreads();
        return coordinator;
    }

    public final boolean isMultithreaded() {
        return coordinator.isMultithreaded();
    }

    private Integer getWorkerThreadsCount() {
        return getActivity().getDistributionDefinition().getWorkerThreads();
    }

    /**
     * Fails if worker threads are defined. To be used in tasks that do not support multithreading.
     */
    protected final void ensureNoWorkerThreads() {
        int threads = getWorkerThreadsCount();
        if (threads != 0) {
            throw new UnsupportedOperationException("Unsupported number of worker threads: " + threads +
                    ". This task cannot be run with worker threads. Please remove workerThreads task "
                    + "extension property and/or workerThreads distribution definition item or set its value to 0.");
        }
    }

    /**
     * Fails if there is any parallelism within this activity: worker threads or worker tasks.
     * It is to avoid unintended parallel execution like the one in MID-7861.
     *
     * Note that this method does not preclude parallel execution with a different activity.
     * But that is not enabled by default, so it's safe to assume it will not be configured by mistake.
     */
    protected final void ensureNoParallelism() {
        ensureNotInWorkerTask(null);
        ensureNoWorkerThreads();
    }

    public final @NotNull String getShortName() {
        return shortName;
    }

    final @NotNull String getShortNameUncapitalized() {
        return StringUtils.uncapitalize(shortName);
    }

    public final @NotNull String getContextDescription() {
        return contextDescription;
    }

    /**
     * Inserts a space before context description if it's not empty.
     */
    public final @NotNull String getContextDescriptionSpaced() {
        return !contextDescription.isEmpty() ? " " + contextDescription : "";
    }

    public final void setContextDescription(String value) {
        this.contextDescription = ObjectUtils.defaultIfNull(value, "");
    }

    public final ErrorHandlingStrategyExecutor.FollowUpAction handleError(@NotNull OperationResultStatus status,
            @NotNull Throwable exception, ItemProcessingRequest<?> request, OperationResult result) {
        return errorHandlingStrategyExecutor.handleError(status, exception, request.getObjectOidToRecordRetryTrigger(), result);
    }

    /**
     * @return Default error action if no policy is specified or if no policy entry matches.
     */
    protected @NotNull abstract ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction();

    public final @NotNull String getRootTaskOid() {
        return getRunningTask().getRootTaskOid();
    }

    /**
     * Updates statistics in the coordinator task (including TL if it's safe to do so).
     *
     * If needed, also updates the statistics in the repository.
     *
     * Statistics updated in the task:
     *  - task.operationStats,
     *  - progress (both activity-based and legacy),
     *  - activity statistics: items, synchronization, actions executed, bucketing operations
     *
     * Note that using modifyObjectDynamically would be perhaps better, but the current use of last update timestamp
     * ensures that there will not be concurrent updates of the coordinator coming from its worker threads.
     */
    public void updateStatistics(boolean updateThreadLocalStatistics, OperationResult result)
            throws SchemaException, ObjectNotFoundException{
        RunningTask coordinatorTask = getRunningTask();

        coordinatorTask.updateOperationStatsInTaskPrism(
                updateThreadLocalStatistics && canUpdateThreadLocalStatistics());
        coordinatorTask.storeStatisticsIntoRepositoryIfTimePassed(getActivityStatUpdater(), result);
    }

    /**
     * Returns true if it's safe to update TL statistics in coordinator. Normally, it is so.
     * A notable exception is asynchronous update using AMQP (an experimental feature for now).
     * The reason is that the message handling occurs in a thread different from the task thread.
     * See MID-7464.
     */
    protected boolean canUpdateThreadLocalStatistics() {
        return true;
    }

    private Runnable getActivityStatUpdater() {
        return () -> {
            try {
                activityState.updateProgressAndStatisticsNoCommit();
            } catch (ActivityRunException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update activity statistics in the task {}", e,
                        getRunningTask());
                // Ignoring the exception
            }
        };
    }

    @NotNull public final TransientActivityRunStatistics getTransientRunStatistics() {
        return transientRunStatistics;
    }

    public abstract boolean processItem(@NotNull ItemProcessingRequest<I> request, @NotNull RunningTask workerTask,
            OperationResult result) throws ActivityRunException, CommonException;

    @Override
    protected final @NotNull ActivityState determineActivityStateForThresholds(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        ActivityState explicit = useOtherActivityStateForCounters(result);
        if (explicit != null) {
            return explicit;
        }

        if (isWorker()) {
            return getCoordinatorActivityState();
        } else {
            return activityState;
        }
    }

    /** Returns fresh activity state of the coordinator task. Assuming we are in worker task. */
    @SuppressWarnings("unused") // but may be helpful in the future
    private ActivityState getFreshCoordinatorActivityState(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return activityState.getCurrentActivityStateInParentTask(true,
                getActivityStateDefinition().getWorkStateTypeName(), result);
    }

    @NotNull
    public final ItemsReport getItemsReport() {
        return activityState.getItemsReport();
    }

    @NotNull
    public final ConnIdOperationsReport getConnIdOperationsReport() {
        return activityState.getConnIdOperationsReport();
    }

    private void reportBucketCompleted(BucketProcessingRecord processingRecord, OperationResult result) {
        if (shouldReportBuckets()) {
            activityState.getBucketsReport().recordBucketCompleted(
                    new BucketProcessingRecordType()
                            .sequentialNumber(bucket.getSequentialNumber())
                            .content(bucket.getContent())
                            .size(processingRecord.getTotalSize())
                            .itemsSuccessfullyProcessed(processingRecord.success)
                            .itemsFailed(processingRecord.failure)
                            .itemsSkipped(processingRecord.skip)
                            .startTimestamp(XmlTypeConverter.createXMLGregorianCalendar(processingRecord.startTimestamp))
                            .endTimestamp(XmlTypeConverter.createXMLGregorianCalendar(processingRecord.endTimestamp))
                            .duration(processingRecord.getDuration()),
                    getRunningTask(), result);
        }
    }

    private void reportBucketAnalyzed(Integer size, OperationResult result) {
        if (shouldReportBuckets()) {
            activityState.getBucketsReport().recordBucketCompleted(
                    new BucketProcessingRecordType()
                            .sequentialNumber(bucket.getSequentialNumber())
                            .content(bucket.getContent())
                            .size(size),
                    getRunningTask(), result);
        }
    }

    private boolean shouldReportBuckets() {
        return activityState.getBucketsReport().isEnabled();
    }

    public final boolean shouldReportItems() {
        return activityState.getItemsReport().isEnabled();
    }

    public final boolean shouldReportConnIdOperations() {
        return activityState.getConnIdOperationsReport().isEnabled();
    }

    public final boolean shouldReportInternalOperations() {
        return activityState.getInternalOperationsReport().isEnabled();
    }

    public final WorkBucketType getBucket() {
        return bucket;
    }

    private @NotNull BucketingSituation determineBucketingSituation() {
        if (getActivityState().isWorker()) {
            return BucketingSituation.worker(getRunningTask());
        } else {
            return BucketingSituation.standalone(getRunningTask());
        }
    }

    public final void enableGlobalConnIdOperationsListener() {
        if (shouldReportConnIdOperations()) {
            getRunningTask().registerConnIdOperationsListener(globalConnIdOperationsListener);
        }
    }

    public final void disableGlobalConnIdOperationsListener() {
        if (shouldReportConnIdOperations()) {
            getRunningTask().unregisterConnIdOperationsListener(globalConnIdOperationsListener);
        }
    }

    public @NotNull ErrorState getErrorState() {
        return errorState;
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

    private static class BucketProcessingRecord {

        private final long startTimestamp;
        private long endTimestamp;

        private final int successAtStart;
        private final int failureAtStart;
        private final int skipAtStart;

        private int success;
        private int failure;
        private int skip;

        BucketProcessingRecord(@NotNull ActivityItemProcessingStatistics startStats) {
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
