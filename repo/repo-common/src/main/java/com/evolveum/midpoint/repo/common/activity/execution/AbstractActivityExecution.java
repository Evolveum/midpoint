/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.*;
import com.evolveum.midpoint.repo.common.activity.state.ActivityProgress;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.repo.common.activity.state.CurrentActivityState;
import com.evolveum.midpoint.repo.common.task.task.GenericTaskExecution;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.RunningTask;

import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.Map;

import static com.evolveum.midpoint.repo.common.activity.state.ActivityProgress.Counters.COMMITTED;
import static com.evolveum.midpoint.repo.common.activity.state.ActivityProgress.Counters.UNCOMMITTED;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.SUCCESS;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

/**
 * Base class for activity executions.
 *
 * @param <WD> Definition of the work that this activity has to do.
 */
public abstract class AbstractActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType> implements ActivityExecution, ExecutionSupport {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractActivityExecution.class);

    /**
     * The task execution in context of which this activity execution takes place.
     */
    @NotNull protected final GenericTaskExecution taskExecution;

    /**
     * Definition of the activity. Contains the definition of the work.
     */
    @NotNull protected final Activity<WD, AH> activity;

    @NotNull protected final ActivityStateDefinition<WS> activityStateDefinition;

    /**
     * TODO
     */
    @NotNull protected final CurrentActivityState<WS> activityState;

    /**
     * Activity state object where counters for the current activity reside.
     * By default it is the activity state for the current standalone activity (e.g. reconciliation).
     *
     * Lazily evaluated.
     *
     * Guarded by {@link #activityStateForCountersLock}.
     */
    private ActivityState activityStateForCounters;

    private final Object activityStateForCountersLock = new Object();

    // Temporary
    private long startTimestamp;

    protected AbstractActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        this.taskExecution = context.getTaskExecution();
        this.activity = context.getActivity();
        this.activityStateDefinition = determineActivityStateDefinition();
        this.activityState = new CurrentActivityState<>(this);
    }

    protected ActivityStateDefinition<WS> determineActivityStateDefinition() {
        // TODO implement type safety here
        //noinspection unchecked
        return (ActivityStateDefinition<WS>) activity.getActivityStateDefinition();
    }

    @NotNull
    @Override
    public GenericTaskExecution getTaskExecution() {
        return taskExecution;
    }

    public @NotNull Activity<WD, AH> getActivity() {
        return activity;
    }

    public CommonTaskBeans getBeans() {
        return taskExecution.getBeans();
    }

    /**
     * Puts the activity state into operation.
     */
    @Override
    public void initializeState(OperationResult result) throws ActivityExecutionException {
        activityState.initialize(result);
    }

    @Override
    public @NotNull ActivityExecutionResult execute(OperationResult result) throws ActivityExecutionException {

        initializeState(result);

        if (activityState.isComplete()) {
            logComplete();
            return ActivityExecutionResult.finished(activityState.getResultStatus());
        }

        startTimestamp = System.currentTimeMillis();

        logStart();
        ActivityExecutionResult executionResult = executeTreatingExceptions(result);
        logEnd(executionResult);

        updateActivityState(executionResult, result);

        return executionResult;
    }

    /**
     * Executes the activity, converting any exceptions into appropriate {@link ActivityExecutionResult} instances.
     */
    @NotNull
    private ActivityExecutionResult executeTreatingExceptions(OperationResult result) {
        try {
            executeBeforeExecutionRunner(result);
            return executeInternal(result);
        } catch (ActivityExecutionException e) {
            if (e.getOpResultStatus() != SUCCESS) {
                LoggingUtils.logUnexpectedException(LOGGER, "Exception in {}", e, this);
            }
            return e.toActivityExecutionResult();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception in {}", e, this);
            return ActivityExecutionResult.exception(FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    /* TODO better name */
    private void executeBeforeExecutionRunner(OperationResult result) throws ActivityExecutionException, CommonException {
        if (!(activity instanceof EmbeddedActivity)) {
            return;
        }
        if (this instanceof DelegatingActivityExecution) {
            return; // We want this to run only for local + distributing executions - TODO TODO TODO
        }
        EmbeddedActivity<WD, AH> embeddedActivity = (EmbeddedActivity<WD, AH>) this.activity;
        BeforeExecutionRunner<WD, AH> beforeExecutionRunner = embeddedActivity.getBeforeExecutionRunner();
        if (beforeExecutionRunner == null) {
            return;
        }

        beforeExecutionRunner.run(embeddedActivity, getRunningTask(), result);
    }

    /**
     * Carries out the actual execution of this activity.
     */
    protected abstract @NotNull ActivityExecutionResult executeInternal(OperationResult result)
            throws ActivityExecutionException, CommonException;

    /**
     * Updates the activity state with the result of the execution.
     * Stores also the live values of progress/statistics into the current task.
     */
    private void updateActivityState(ActivityExecutionResult executionResult, OperationResult result)
            throws ActivityExecutionException {

        activityState.updateProgressAndStatisticsNoCommit();

        completeExecutionResult(executionResult);

        OperationResultStatus currentResultStatus = executionResult.getOperationResultStatus();
        if (executionResult.isFinished()) {
            activityState.markCompleteNoCommit(currentResultStatus);
        } else if (currentResultStatus != null && currentResultStatus != activityState.getResultStatus()) {
            activityState.setResultStatusNoCommit(currentResultStatus);
        }

        activityState.flushPendingModificationsChecked(result); // if not flushed above
    }

    /**
     * Converts null or "in progress" values into finished/interrupted/success/default ones.
     *
     * TODO Or should we require the activity execution code to do this?
     */
    private void completeExecutionResult(ActivityExecutionResult executionResult) {
        if (executionResult.getRunResultStatus() == null) {
            executionResult.setRunResultStatus(getTaskExecution().canRun() ?
                    TaskRunResult.TaskRunResultStatus.FINISHED : TaskRunResult.TaskRunResultStatus.INTERRUPTED);
        }
        if (executionResult.getOperationResultStatus() == null) {
            executionResult.setOperationResultStatus(activityState.getResultStatus());
        }
        if ((executionResult.getOperationResultStatus() == null ||
                executionResult.getOperationResultStatus() == OperationResultStatus.IN_PROGRESS) && executionResult.isFinished()) {
            executionResult.setOperationResultStatus(OperationResultStatus.SUCCESS);
        }
    }

    private void logStart() {
        LOGGER.debug("{}: Starting execution of activity with identifier '{}' and path '{}' (local: '{}') with work state "
                        + "prism item path: {}",
                getClass().getSimpleName(), activity.getIdentifier(), activity.getPath(), activity.getLocalPath(),
                activityState.getItemPath());
    }

    private void logEnd(ActivityExecutionResult executionResult) {
        LOGGER.debug("{}: Finished execution of activity with identifier '{}' and path '{}' (local: {}) with result: {}",
                getClass().getSimpleName(), activity.getIdentifier(), activity.getPath(), activity.getLocalPath(),
                executionResult);
    }

    private void logComplete() {
        LOGGER.debug("{}: Skipped execution of activity with identifier '{}' and path '{}' (local: {}) as it was already executed",
                getClass().getSimpleName(), activity.getIdentifier(), activity.getPath(), activity.getLocalPath());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "act=" + activity +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        if (activity.isRoot()) {
            DebugUtil.debugDumpWithLabelLn(sb, "task execution", taskExecution.shortDump(), indent + 1);
        }
        DebugUtil.debugDumpWithLabelLn(sb, "State", activityState, indent + 1);
        debugDumpExtra(sb, indent);
        return sb.toString();
    }

    protected void debugDumpExtra(StringBuilder sb, int indent) {
    }

    @SuppressWarnings("unused")
    public @Nullable ActivityPath getActivityLocalPath() {
        return activity.getLocalPath();
    }

    public @NotNull ActivityPath getActivityPath() {
        return activity.getPath();
    }

    public AbstractActivityExecution<?, ?, ?> getLocalParentExecution() {
        if (activity.isLocalRoot()) {
            return null;
        }

        Activity<?, ?> parentActivity = activity.getParent();
        if (parentActivity != null) {
            return parentActivity.getExecution();
        } else {
            return null;
        }
    }

    public @NotNull AH getActivityHandler() {
        return activity.getHandler();
    }

    public @NotNull CurrentActivityState<WS> getActivityState() {
        return activityState;
    }

    public @NotNull RunningTask getRunningTask() {
        return taskExecution.getRunningTask();
    }

    public @NotNull QName getWorkStateTypeName() {
        return activityStateDefinition.getWorkStateTypeName();
    }

    public @NotNull ActivityTreeStateOverview getTreeStateOverview() {
        return activity.getTree().getTreeStateOverview();
    }

    protected ActivityExecutionResult standardExecutionResult() {
        return ActivityExecutionResult.standardResult(canRun());
    }

    public boolean canRun() {
        return taskExecution.canRun();
    }

    public boolean shouldCreateWorkStateOnInitialization() {
        return true;
    }

    public @NotNull ActivityStatePersistenceType getPersistenceType() {
        return ActivityStatePersistenceType.SINGLE_REALIZATION;
    }

    public @NotNull PrismContext getPrismContext() {
        return getBeans().prismContext;
    }

    public abstract boolean supportsStatistics();

    public boolean supportsProgress() {
        return supportsStatistics(); // for now
    }

    public boolean supportsSynchronizationStatistics() {
        return supportsStatistics(); // Should be overridden in subclasses, if needed.
    }

    public boolean supportsActionsExecuted() {
        return supportsStatistics(); // Should be overridden in subclasses, if needed.
    }

    public void incrementProgress(@NotNull QualifiedItemProcessingOutcomeType outcome) {
        ActivityProgress.Counters counters = hasProgressCommitPoints() ? UNCOMMITTED : COMMITTED;
        activityState.getLiveProgress().increment(outcome, counters);
    }

    /**
     * @return True if the activity is capable of distinguishing between uncommitted and committed progress items.
     * A typical example of committing progress items is when a bucket is marked as complete: this ensures that items
     * that were processed will not be reprocessed again.
     */
    protected boolean hasProgressCommitPoints() {
        return false;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public @NotNull ActivityStateDefinition<WS> getActivityStateDefinition() {
        return activityStateDefinition;
    }

    @Override
    public Map<String, Integer> incrementCounters(@NotNull CountersGroup counterGroup,
            @NotNull Collection<String> countersIdentifiers, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        synchronized (activityStateForCountersLock) {
            if (activityStateForCounters == null) {
                activityStateForCounters = determineActivityStateForCounters(result);
            }
        }
        return activityStateForCounters.incrementCounters(counterGroup, countersIdentifiers, result);
    }

    protected ActivityState determineActivityStateForCounters(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return activityState;
    }

    @Override
    public @NotNull ExecutionModeType getExecutionMode() {
        return activity.getDefinition().getExecutionMode();
    }
}
