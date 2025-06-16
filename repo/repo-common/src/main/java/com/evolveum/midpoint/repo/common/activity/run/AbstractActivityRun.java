/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.repo.common.activity.run.state.ActivityProgress.Counters.COMMITTED;
import static com.evolveum.midpoint.repo.common.activity.run.state.ActivityProgress.Counters.UNCOMMITTED;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.*;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.repo.common.activity.*;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityExecutionModeDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityReportingDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRulesContext;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRulesProcessor;
import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedActivityPolicyRule;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityProgress;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.DummyOperationImpl;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.ActivityThresholdPolicyViolationException;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Implements (represents) a run (execution) of an activity in the current task.
 *
 * Responsibilities _at this [highest] level of abstraction_:
 *
 * . Maintains links to other activity framework objects:
 *
 * - {@link #taskRun} ({@link ActivityBasedTaskRun})
 * - {@link #activity} ({@link Activity})
 * - {@link #activityState} ({@link ActivityState}) (and its {@link ActivityStateDefinition}), including the state holding
 * thresholds-supporting counters
 *
 * . Provides methods for navigation to more distant objects of the framework and other auxiliary objects (beans).
 *
 * . Provides skeleton of the execution - see {@link #run(OperationResult)}, managing (among others):
 * a. activity state initialization and closing,
 * b. execution of "before run" code,
 * c. conversion of exceptions into {@link ActivityRunResult} (such conversion is done at various other levels, btw),
 * d. start/end logging,
 * e. updating task statistics,
 * f. sending notifications.
 *
 * +
 * Some of these duties are related to ones of {@link LocalActivityRun#runInternal(OperationResult)}
 *
 * @param <WD> Definition of the work that this activity has to do.
 * @param <AH> Type of the activity handler.
 * @param <WS> Type of the activity work (business) state.
 */
public abstract class AbstractActivityRun<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType> implements ExecutionSupport, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractActivityRun.class);

    /**
     * The task run in context of which this activity run takes place.
     */
    @NotNull protected final ActivityBasedTaskRun taskRun;

    /**
     * Definition of the activity. Contains the definition of the work.
     */
    @NotNull protected final Activity<WD, AH> activity;

    /**
     * Captures traits of the activity state (e.g. if it has to be created).
     */
    @NotNull final ActivityStateDefinition<WS> activityStateDefinition;

    /**
     * The "live" version of the activity state.
     */
    @NotNull protected final CurrentActivityState<WS> activityState;

    /**
     * Activity state object where [threshold] counters and policy states for the current activity reside.
     * By default, it is the activity state for the current standalone activity (e.g. reconciliation).
     *
     * Lazily evaluated.
     *
     * Guarded by {@link #activityStateForThresholdsLock}.
     */
    private ActivityState activityStateForThresholds;

    private final Object activityStateForThresholdsLock = new Object();

    /** When did this run start? */
    protected Long startTimestamp;

    /** When did this run end? */
    protected Long endTimestamp;

    /** Was the instance fully initialized? This is to ensure e.g. {@link #reportingCharacteristics} can be created. */
    private boolean instanceReady;

    /**
     * Reporting characteristics of this kind of activity run. Can be used only after the concrete instance is
     * ready (i.e. fully initialized)! This is because the instance initialization goes down the inheritance hierarchy
     * (from super classes to sub-classes) and the characteristics are defined progressively, usually being finalized
     * at the lowest levels. Therefore, the super classes must _not_ ask about reporting characteristics during their
     * own initialization.
     *
     * TODO Isn't there a cleaner way how to do this?
     */
    @NotNull final Lazy<ActivityReportingCharacteristics> reportingCharacteristics =
            Lazy.from(this::createReportingCharacteristics);

    @NotNull final SimulationSupport simulationSupport;

    @NotNull final ActivityPolicyRulesContext activityPolicyRulesContext = new ActivityPolicyRulesContext();

    protected AbstractActivityRun(@NotNull ActivityRunInstantiationContext<WD, AH> context) {
        this.taskRun = context.getTaskRun();
        this.activity = context.getActivity();
        this.activityStateDefinition = determineActivityStateDefinition();
        this.activityState = new CurrentActivityState<>(this);
        this.simulationSupport = new SimulationSupport(this);
    }

    /**
     * This method should be called only after the concrete instance is fully initialized.
     */
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        assertInstanceReady();
        return new ActivityReportingCharacteristics();
    }

    private void assertInstanceReady() {
        stateCheck(instanceReady, "Instance is not ready: %s", this);
    }

    protected void setInstanceReady() {
        instanceReady = true;
    }

    /**
     * Called during initialization. Should not access reporting characteristics.
     */
    protected ActivityStateDefinition<WS> determineActivityStateDefinition() {
        //noinspection unchecked
        return (ActivityStateDefinition<WS>) activity.getActivityStateDefinition();
    }

    /**
     * Returns task run that contains this activity run.
     */
    public @NotNull ActivityBasedTaskRun getTaskRun() {
        return taskRun;
    }

    public @NotNull Activity<WD, AH> getActivity() {
        return activity;
    }

    public CommonTaskBeans getBeans() {
        return taskRun.getBeans();
    }

    /**
     * Initializes activity state data in the running task.
     */
    void initializeState(OperationResult result) throws ActivityRunException {
        activityState.initialize(result);
    }

    /**
     * Runs the activity.
     *
     * This method is responsible for carrying out the work, e.g. recomputing all the users.
     * For pure- or semi-composite activities it is also responsible for creating the children runs.
     *
     * Note that the work can be delegated to other (asynchronous) tasks. This is the case of worker tasks in multi-node
     * task run, or of activities executed as separate subtasks.
     *
     * @see LocalActivityRun#runInternal(OperationResult)
     */
    public @NotNull ActivityRunResult run(OperationResult result) throws ActivityRunException {

        initializeState(result);

        if (activityState.isComplete()) {
            logComplete();
            return ActivityRunResult.finished(activityState.getResultStatus());
        }

        ActivityPolicyRulesProcessor processor = new ActivityPolicyRulesProcessor(this);
        processor.collectRules();

        noteStartTimestamp();
        logStart();

        ActivityRunResult runResult = runTreatingExceptions(result);

        noteEndTimestampIfNone();
        logEnd(runResult);

        updateAndCloseActivityState(runResult, result);

        if (activityState.isComplete()) {
            // TODO Is this really called only once on activity completion? Not sure about distributed/delegated ones.
            onActivityRealizationComplete(result);
            sendActivityRealizationCompleteEvent(result);

            try {
                // this evaluation handles activity policy rules with "below" constraints (at the end of activity run)
                processor.evaluateAndEnforceRules(null, result);
            } catch (ThresholdPolicyViolationException e) {
                throw new ActivityRunException(
                        "Threshold policy violation", FATAL_ERROR, PERMANENT_ERROR, e);
            } catch (CommonException e) {
                throw new ActivityRunException(
                        "Couldn't evaluate and enforce activity policy rules", FATAL_ERROR, PERMANENT_ERROR, e);
            }
        }

        return runResult;
    }

    /**
     * Takes a note when the current run started.
     *
     * BEWARE! Not all runs are written to the activity state. Namely, runs of distributing/delegating
     * activities other than initial ones (when subtasks are created) are not recorded.
     */
    private void noteStartTimestamp() {
        startTimestamp = System.currentTimeMillis();
    }

    /**
     * The children may note end timestamp by themselves, if they need the timestamp earlier.
     * All of this is done to ensure there is a single "end timestamp". We assume these events
     * occur almost in one instant.
     */
    void noteEndTimestampIfNone() {
        if (endTimestamp == null) {
            endTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Executes the activity, converting any exceptions into appropriate {@link ActivityRunResult} instances.
     */
    private @NotNull ActivityRunResult runTreatingExceptions(OperationResult result) {
        try {
            invokePreRunnable(result);
            return runInternal(result);
        } catch (Exception e) {
            return ActivityRunResult.handleException(e, result, this);
        }
    }

    /** Temporary implementation. */
    private void invokePreRunnable(OperationResult result) throws ActivityRunException, CommonException {
        if (!(activity instanceof EmbeddedActivity)) {
            return;
        }
        EmbeddedActivity<WD, AH> embeddedActivity = (EmbeddedActivity<WD, AH>) activity;

        if (this instanceof DelegatingActivityRun) {
            return; // We want this to run only for local + distributing runs
        }

        PreRunnable<WD, AH> preRunnable = embeddedActivity.getPreRunnable();
        if (preRunnable == null) {
            return;
        }

        preRunnable.run(embeddedActivity, getRunningTask(), result);
    }

    /**
     * Carries out the actual run of this activity.
     */
    protected abstract @NotNull ActivityRunResult runInternal(OperationResult result)
            throws ActivityRunException, CommonException;

    /**
     * Updates the activity state with the result of the run.
     * Stores also the live values of progress/statistics into the current task.
     */
    private void updateAndCloseActivityState(ActivityRunResult runResult, OperationResult result)
            throws ActivityRunException {

        activityState.updateProgressAndStatisticsNoCommit();

        runResult.close(
                getTaskRun().canRun(), activityState.getResultStatus());

        OperationResultStatus currentResultStatus = runResult.getOperationResultStatus();
        if (runResult.isFinished()) {
            // Note the asymmetry: "in progress" (IN_PROGRESS_LOCAL, IN_PROGRESS_DISTRIBUTED, IN_PROGRESS_DELEGATED)
            // states, along with the timestamp, are written in subclasses. The "complete" state, along with the timestamp,
            // is written here.
            activityState.markComplete(currentResultStatus, endTimestamp);
        } else if (currentResultStatus != null && currentResultStatus != activityState.getResultStatus()) {
            activityState.setResultStatus(currentResultStatus);
        }

        try {
            getRunningTask()
                    .updateAndStoreStatisticsIntoRepository(true, result); // Contains implicit task flush
        } catch (CommonException e) {
            throw new ActivityRunException(
                    "Couldn't update task when updating and closing activity state", FATAL_ERROR, PERMANENT_ERROR, e);
        }

        activityState.close();
    }

    private void logStart() {
        LOGGER.debug("{}: Starting run of activity with identifier '{}' and path '{}' (local: '{}') with work state "
                        + "prism item path: {}",
                getClass().getSimpleName(), activity.getIdentifier(), activity.getPath(), activity.getLocalPath(),
                activityState.getItemPath());
    }

    private void logEnd(ActivityRunResult runResult) {
        LOGGER.debug("{}: Finished run of activity with identifier '{}' and path '{}' (local: {}) with result: {} "
                        + "(took: {} msecs)",
                getClass().getSimpleName(), activity.getIdentifier(), activity.getPath(), activity.getLocalPath(),
                runResult, endTimestamp - startTimestamp);
    }

    private void logComplete() {
        LOGGER.debug("{}: Skipped run of activity with identifier '{}' and path '{}' (local: {}) as it was already executed",
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
            DebugUtil.debugDumpWithLabelLn(sb, "task run", taskRun.shortDump(), indent + 1);
        }
        DebugUtil.debugDumpWithLabelLn(sb, "State", activityState, indent + 1);
        debugDumpExtra(sb, indent);
        return sb.toString();
    }

    protected void debugDumpExtra(StringBuilder sb, int indent) {
    }

    public @NotNull ActivityPath getActivityPath() {
        return activity.getPath();
    }

    public AbstractActivityRun<?, ?, ?> getLocalParentRun() {
        if (activity.isLocalRoot()) {
            return null;
        }

        Activity<?, ?> parentActivity = activity.getParent();
        if (parentActivity != null) {
            return parentActivity.getRun();
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
        return taskRun.getRunningTask();
    }

    @SuppressWarnings("WeakerAccess")
    protected @NotNull ActivityTreeStateOverview getTreeStateOverview() {
        return activity.getTree().getTreeStateOverview();
    }

    protected ActivityRunResult standardRunResult() {
        return ActivityRunResult.standardResult(canRun());
    }

    /** Finished (with specified status), or interrupted. */
    protected ActivityRunResult standardRunResult(@Nullable OperationResultStatus status) {
        if (canRun()) {
            return new ActivityRunResult(status, FINISHED);
        } else {
            return ActivityRunResult.interrupted();
        }
    }

    public boolean canRun() {
        return taskRun.canRun();
    }

    /**
     * Returns true if the work (business) state should be created right on activity run initialization,
     * along with the rest of the state.
     *
     * Maybe we should provide this customization in the "specifics" interface for iterative activities.
     */
    public boolean shouldCreateWorkStateOnInitialization() {
        return true;
    }

    public boolean areStatisticsSupported() {
        return reportingCharacteristics.get().areStatisticsSupported();
    }

    public boolean isProgressSupported() {
        return reportingCharacteristics.get().isProgressSupported();
    }

    private boolean areProgressCommitPointsSupported() {
        return reportingCharacteristics.get().areProgressCommitPointsSupported();
    }

    public boolean areSynchronizationStatisticsSupported() {
        return reportingCharacteristics.get().areSynchronizationStatisticsSupported();
    }

    public boolean areActionsExecutedStatisticsSupported() {
        return reportingCharacteristics.get().areActionsExecutedStatisticsSupported();
    }

    public boolean areRunRecordsSupported() {
        return reportingCharacteristics.get().areRunRecordsSupported();
    }

    public void incrementProgress(@NotNull QualifiedItemProcessingOutcomeType outcome) {
        ActivityProgress.Counters counters = areProgressCommitPointsSupported() ? UNCOMMITTED : COMMITTED;
        activityState.getLiveProgress().increment(outcome, counters);
    }

    public @NotNull ActivityStateDefinition<WS> getActivityStateDefinition() {
        return activityStateDefinition;
    }

    @Override
    public Map<String, Integer> incrementCounters(@NotNull CountersGroup counterGroup,
            @NotNull Collection<String> countersIdentifiers, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        synchronized (activityStateForThresholdsLock) {
            if (activityStateForThresholds == null) {
                activityStateForThresholds = determineActivityStateForThresholds(result);
            }
        }
        return activityStateForThresholds.incrementCounters(counterGroup, countersIdentifiers, result);
    }

    public Map<String, ActivityPolicyStateType> updateActivityPolicyState(
            @NotNull Collection<ActivityPolicyStateType> states, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        synchronized (activityStateForThresholdsLock) {
            if (activityStateForThresholds == null) {
                activityStateForThresholds = determineActivityStateForThresholds(result);
            }
        }
        return activityStateForThresholds.updatePolicies(states, result);
    }

    protected @NotNull ActivityState determineActivityStateForThresholds(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return activityState;
    }

    @Override
    public @NotNull ExecutionModeType getActivityExecutionMode() {
        return activity.getDefinition().getExecutionMode();
    }

    // TODO sort these methods out

    private boolean isPreview() {
        return getActivityExecutionMode() == ExecutionModeType.PREVIEW;
    }

    private boolean isShadowManagementPreview() {
        return getActivityExecutionMode() == ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW;
    }

    private boolean isAnyPreview() {
        return isPreview() || isShadowManagementPreview();
    }

    public boolean isDryRun() {
        return getActivityExecutionMode() == ExecutionModeType.DRY_RUN;
    }

    protected boolean isFullExecution() {
        return getActivityExecutionMode() == ExecutionModeType.FULL;
    }

    public boolean isNoExecution() {
        return getActivityExecutionMode() == ExecutionModeType.NONE;
    }

    public boolean isBucketAnalysis() {
        return getActivityExecutionMode() == ExecutionModeType.BUCKET_ANALYSIS;
    }

    public int getItemsProcessed() {
        return getActivityState().getLiveStatistics().getLiveItemProcessing().getItemsProcessed();
    }

    public boolean isNonScavengingWorker() {
        return isWorker() && !activityState.isScavenger();
    }

    public boolean isWorker() {
        return activityState.isWorker();
    }

    public @NotNull WD getWorkDefinition() {
        return activity.getWorkDefinition();
    }

    @NotNull ActivityExecutionModeDefinition getExecutionModeDefinition() {
        return getActivityDefinition().getExecutionModeDefinition();
    }

    protected @NotNull ActivityDefinition<WD> getActivityDefinition() {
        return activity.getDefinition();
    }

    @NotNull ActivityReportingDefinition getReportingDefinition() {
        return getActivityDefinition().getReportingDefinition();
    }

    public @NotNull ActivityReportingCharacteristics getReportingCharacteristics() {
        return requireNonNull(reportingCharacteristics.get());
    }

    public long getStartTimestampRequired() {
        return requireNonNull(
                startTimestamp,
                () -> "no start timestamp in " + this);
    }

    @Override
    public Operation recordIterativeOperationStart(@NotNull IterativeOperationStartInfo info) {
        if (areStatisticsSupported()) {
            return getActivityState().getLiveStatistics().getLiveItemProcessing()
                    .recordOperationStart(info);
        } else {
            return new DummyOperationImpl(info);
        }
    }

    private void sendActivityRealizationCompleteEvent(OperationResult result) {
        for (ActivityListener activityListener : emptyIfNull(getBeans().activityListeners)) {
            try {
                activityListener.onActivityRealizationComplete(this, getRunningTask(), result);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER,
                        "Activity listener {} failed when processing 'activity realization complete' event for {}", e,
                        activityListener, this);
            }
        }
    }

    public void sendActivityPolicyRuleTriggeredEvent(EvaluatedActivityPolicyRule policyRule, OperationResult result) {
        for (ActivityListener activityListener : emptyIfNull(getBeans().activityListeners)) {
            try {
                activityListener.onActivityPolicyRuleTriggered(this, policyRule, getRunningTask(), result);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER,
                        "Activity listener {} failed when processing 'activity policy rule triggered' event for {}", e,
                        activityListener, this);
            }
        }
    }

    /** Returns the name for diagnostic purposes, e.g. when logging an error. */
    @NotNull String getDiagName() {
        RunningTask task = getRunningTask();
        return getActivityPath().toDebugName() + " activity in '" + task.getName() + "' task (OID " + task.getOid() + ")";
    }

    /**
     * Called when the activity realization starts.
     *
     * - For delegated activities this is _after_ the delegation occurred. (I.e. in the delegate run.)
     * - For distributed activities this is before any of the workers are started.
     * - For non-delegated, non-distributed (local-only) activities this is when the local run starts the first time.
     *
     * Overall, this should happen exactly once per activity realization.
     *
     * In subclasses: Do not forget to call the implementation in the super-class.
     */
    @SuppressWarnings("WeakerAccess")
    protected void onActivityRealizationStart(OperationResult result) throws ActivityRunException {
        // The simulation result is created for the whole activity realization.
        // When the activity execution is suspended and resumed, the result should stay the same.
        // The "processed object" records in resumed execution will be appended to it.
        simulationSupport.getOrCreateSimulationResult(result);
    }

    /**
     * Called when the activity realization is complete. It should be called at most once for any given activity.
     * (Regardless of its delegation or distribution.)
     *
     * Planned e.g. for closing the simulation result (for computing statistics, etc).
     *
     * TODO this is something like a placeholder for now -- probably it will NOT work in the current implementation!
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected void onActivityRealizationComplete(OperationResult result) throws ActivityRunException {
        simulationSupport.closeSimulationResultIfOpenedHere(result);
    }

    /**
     * Use this to disallow running activities that do not honor preview and/or dry-run mode, to avoid any confusion of the user.
     */
    protected void ensureNoPreviewNorDryRun() {
        ensureNoPreview();
        ensureNoDryRun();
    }

    private void ensureNoPreview() {
        if (isAnyPreview()) {
            throw new IllegalStateException("This activity cannot be run in simulation (preview) mode");
        }
    }

    protected void ensureNoDryRun() {
        if (isDryRun()) {
            throw new IllegalStateException("This activity cannot be run in dry run mode");
        }
    }

    protected void ensureFullExecution() {
        ExecutionModeType mode = getActivityExecutionMode();
        if (mode != ExecutionModeType.FULL) {
            throw new IllegalStateException("This activity can be run in full execution mode only. Requested mode: " + mode);
        }
    }

    public @NotNull ActivityPolicyRulesContext getActivityPolicyRulesContext() {
        return activityPolicyRulesContext;
    }
}
