/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.execution.LocalActivityExecution;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.state.ActivityItemProcessingStatistics;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

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
 * 2. Orchestrates the basic bucket execution cycle - see {@link #executeSingleBucket(OperationResult)}:
 *
 * a. item source preparation,
 * b. before/after bucket execution "hook" methods, along with main {@link #iterateOverItems(OperationResult)} method,
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
                .cloneWithConfiguration(context.getActivity().getDefinition().getSpecificReportingOptions());
        this.errorHandlingStrategyExecutor = new ErrorHandlingStrategyExecutor(getActivity(), getRunningTask(),
                getDefaultErrorAction(), beans);
    }

    protected @NotNull ActivityExecutionResult executeLocal(OperationResult result)
            throws ActivityExecutionException, CommonException {

        LOGGER.trace("{}: Starting with local coordinator task {}", shortName, getRunningTask());

        transientExecutionStatistics.recordExecutionStart();

        executionSpecifics.beforeExecution(result);

        doExecute(result);

        executionSpecifics.afterExecution(result);

        ActivityExecutionResult executionResult = createExecutionResult();

        LOGGER.trace("{} run finished (task {}, execution result {})", shortName, getRunningTask(),
                executionResult);

        return executionResult;
    }

    /**
     * Execute the activity.
     */
    protected abstract void doExecute(OperationResult result) throws ActivityExecutionException, CommonException;

    /**
     * Execute a single bucket.
     */
    void executeSingleBucket(OperationResult result) throws ActivityExecutionException, CommonException {
        prepareItemSource(result);

        beforeBucketExecution(result);

        setExpectedTotal(result);

        coordinator = setupCoordinatorAndWorkerThreads();
        try {
            iterateOverItems(result);
        } finally {
            // This is redundant in the case of live sync event handling (because the handler gets a notification when all
            // items are submitted, and must stop the threads in order to allow provisioning to update the token).
            //
            // But overall, it is necessary to do this here in order to avoid endless waiting if any exception occurs.
            coordinator.finishProcessing(result);
        }

        afterBucketExecution(result);

        // TODO reconsider this: why do we update in-memory representation only?
        getRunningTask()
                .updateStatisticsInTaskPrism(true);

        new StatisticsLogger(this)
                .logBucketCompletion();
    }

    protected abstract void beforeBucketExecution(OperationResult result) throws ActivityExecutionException, CommonException;

    protected abstract void afterBucketExecution(OperationResult result) throws ActivityExecutionException, CommonException;

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

    /**
     * Computes expected total and sets the value in the task. E.g. for search-iterative tasks we count the objects here.
     *
     * TODO reconsider
     */
    protected abstract void setExpectedTotal(OperationResult result) throws CommonException;

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
    protected abstract void iterateOverItems(OperationResult result) throws CommonException;

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

    public boolean isSimulate() {
        return getExecutionMode() == ExecutionModeType.SIMULATE;
    }

    public boolean isDryRun() {
        return getExecutionMode() == ExecutionModeType.DRY_RUN;
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

    public boolean isExcludedFromProfilingAndTracing() {
        return false;
    }

    @Override
    protected ActivityState determineActivityStateForCounters(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return executionSpecifics.useOtherActivityStateForCounters(result);
    }

    public @NotNull AES getExecutionSpecifics() {
        return executionSpecifics;
    }

    @FunctionalInterface
    public interface SpecificsSupplier<AE extends IterativeActivityExecution<?, ?, ?, ?, ?, ?>,
            AES extends IterativeActivityExecutionSpecifics> {
        AES supply(AE activityExecution);
    }
}
