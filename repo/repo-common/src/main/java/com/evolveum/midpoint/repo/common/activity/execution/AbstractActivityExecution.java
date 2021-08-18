/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import static com.evolveum.midpoint.repo.common.activity.state.ActivityProgress.Counters.COMMITTED;
import static com.evolveum.midpoint.repo.common.activity.state.ActivityProgress.Counters.UNCOMMITTED;

import java.util.Collection;
import java.util.Map;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.*;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.state.ActivityProgress;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.repo.common.activity.state.CurrentActivityState;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.task.task.GenericTaskExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStatePersistenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

/**
 * Implements (represents) an execution of an activity.
 *
 * Responsibilities _at this [highest] level of abstraction_:
 *
 * 1. During execution - see {@link #execute(OperationResult)}:
 *    a. initializes activity state (if needed),
 *    b. skips execution of the activity if the realization state is `complete`,
 *    c. executes "before execution" and the real code
 *    d. handles exceptions thrown by the execution code, converting them into {@link ActivityExecutionResult}
 *       (such conversion is done at various other levels, btw),
 *    e. logs the start/end,
 *    f. updates execution and result (op) status in the repository,
 *
 * 2. Maintains links to other activity framework objects: task execution, activity, activity state (and its definition),
 * activity state for counters.
 *
 * 3. Provides methods for navigation to more distant objects of the framework and other auxiliary objects (beans).
 *
 * @param <WD> Definition of the work that this activity has to do.
 * @param <AH> Type of the activity handler.
 * @param <WS> Type of the activity work (business) state.
 */
public abstract class AbstractActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType> implements ExecutionSupport, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractActivityExecution.class);

    /**
     * The task execution in context of which this activity execution takes place.
     */
    @NotNull protected final GenericTaskExecution taskExecution;

    /**
     * Definition of the activity. Contains the definition of the work.
     */
    @NotNull protected final Activity<WD, AH> activity;

    /**
     * Captures traits of the activity state (e.g. if it has to be created).
     */
    @NotNull protected final ActivityStateDefinition<WS> activityStateDefinition;

    /**
     * The "live" version of the activity state.
     */
    @NotNull protected final CurrentActivityState<WS> activityState;

    /**
     * Activity state object where [threshold] counters for the current activity reside.
     * By default it is the activity state for the current standalone activity (e.g. reconciliation).
     *
     * Lazily evaluated.
     *
     * Guarded by {@link #activityStateForCountersLock}.
     */
    private ActivityState activityStateForCounters;

    private final Object activityStateForCountersLock = new Object();

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

    /**
     * Returns task execution that contains this activity execution.
     */
    public @NotNull GenericTaskExecution getTaskExecution() {
        return taskExecution;
    }

    public @NotNull Activity<WD, AH> getActivity() {
        return activity;
    }

    public CommonTaskBeans getBeans() {
        return taskExecution.getBeans();
    }

    /**
     * Initializes activity state data in the running task.
     */
    void initializeState(OperationResult result) throws ActivityExecutionException {
        activityState.initialize(result);
    }

    /**
     * Executes the activity.
     *
     * This method is responsible for carrying out the work, e.g. recomputing all the users.
     * For pure- or semi-composite activities it is also responsible for creating the children executions.
     *
     * Note that the work can be delegated to other (asynchronous) tasks. This is the case of worker tasks in multi-node
     * task execution, or of activities executed as separate subtasks.
     */
    public @NotNull ActivityExecutionResult execute(OperationResult result) throws ActivityExecutionException {

        initializeState(result);

        if (activityState.isComplete()) {
            logComplete();
            return ActivityExecutionResult.finished(activityState.getResultStatus());
        }

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
        } catch (Exception e) {
            return ActivityExecutionResult.handleException(e, this);
        }
    }

    /* TODO better name */
    private void executeBeforeExecutionRunner(OperationResult result) throws ActivityExecutionException, CommonException {
        if (!(activity instanceof EmbeddedActivity)) {
            return;
        }
        if (this instanceof DelegatingActivityExecution) {
            return; // We want this to run only for local + distributing executions
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

    @NotNull ActivityTreeStateOverview getTreeStateOverview() {
        return activity.getTree().getTreeStateOverview();
    }

    protected ActivityExecutionResult standardExecutionResult() {
        return ActivityExecutionResult.standardResult(canRun());
    }

    public boolean canRun() {
        return taskExecution.canRun();
    }

    /**
     * @return true if the work (business) state should be created right on activity execution initialization,
     * along with the rest of the state
     *
     * Maybe we should provide this customization in the "specifics" interface for iterative activities.
     */
    public boolean shouldCreateWorkStateOnInitialization() {
        return true;
    }

    public @NotNull ActivityStatePersistenceType getPersistenceType() {
        return ActivityStatePersistenceType.SINGLE_REALIZATION;
    }

    public @NotNull PrismContext getPrismContext() {
        return getBeans().prismContext;
    }

    public abstract boolean doesSupportStatistics();

    public boolean doesSupportProgress() {
        return doesSupportStatistics();
    }

    public boolean doesSupportSynchronizationStatistics() {
        return doesSupportStatistics();
    }

    public boolean doesSupportActionsExecuted() {
        return doesSupportStatistics();
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

    public int getItemsProcessed() {
        return getActivityState().getLiveStatistics().getLiveItemProcessing().getItemsProcessed();
    }
}
