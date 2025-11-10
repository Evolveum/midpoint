/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.task;

import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.ABORTED;
import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.FINISHED;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;

import java.util.Objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RestartActivityPolicyActionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.repo.common.activity.ActivityTree;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyProcessorHelper;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.state.LegacyProgressUpdater;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTreeRealizationStateType;

/**
 * This class groups everything related to an execution (a run) of a task related somehow to an activity.
 *
 * It may be a task that executes the activity locally, a task that orchestrates execution of the activity on other nodes,
 * a task that just delegates to another task, etc.
 */
public class ActivityBasedTaskRun implements TaskRun {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityBasedTaskRun.class);

    private static final int DEFAULT_RESTART_DELAY = 5000;

    /** After how many attempts should we stop increasing the delay exponentially? */
    private static final int EXPONENTIAL_BACKOFF_LIMIT = 10;

    @NotNull private final RunningTask runningTask;
    @NotNull private final ActivityBasedTaskHandler activityBasedTaskHandler;

    private ActivityTree activityTree;

    /** Path of {@link #localRootActivity}. */
    private ActivityPath localRootPath;

    /**
     * The activity that is the local root for this task. I.e. what part of the activity tree is executed within this task.
     *
     * @see Activity#localRoot
     */
    private Activity<?, ?> localRootActivity;

    ActivityBasedTaskRun(@NotNull RunningTask runningTask, @NotNull ActivityBasedTaskHandler activityBasedTaskHandler) {
        this.runningTask = runningTask;
        this.activityBasedTaskHandler = activityBasedTaskHandler;
    }

    @NotNull
    @Override
    public TaskRunResult run(OperationResult result) throws TaskException {

        try {
            activityTree = ActivityTree.create(getRootTask());
            localRootPath = ActivityStateUtil.getLocalRootPath(runningTask.getWorkState());

            localRootActivity = activityTree.getActivity(localRootPath);
            localRootActivity.setLocalRoot();
        } catch (CommonException e) {
            throw new TaskException("Couldn't initialize activity tree", FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR, e);
        }

        if (localRootActivity.isSkipped()) {
            LOGGER.trace("Local root activity is skipped, exiting");
            return TaskRunResult.createNotApplicableTaskRunResult();
        }

        logStart();

        try {
            if (isRootRun()) {
                setupTaskArchetypeIfNeeded(result);
                updateStateOnRootRunStart(result);
            }

            AbstractActivityRun<?, ?, ?> localRootRun = localRootActivity.createRun(this, result);

            ActivityPolicyProcessorHelper.setCurrentActivityRun(localRootRun);

            ActivityRunResult runResult = localRootRun.run(result);

            if (isRootRun()) {
                updateStateOnRootRunEnd(runResult, result);
            }

            var taskRunResult = createTaskRunResult(localRootRun, runResult);

            logEnd(localRootRun, runResult, taskRunResult);

            return taskRunResult;
        } catch (ActivityRunException e) {
            // These should be only really unexpected ones. So we won't bother with e.g. updating the tree state.
            // Nor we handle activity aborts here.
            logException(e);
            var taskRunResultStatus = Objects.requireNonNullElse(
                    e.getRunResultStatus().toTaskRunResultStatus(), TaskRunResultStatus.PERMANENT_ERROR);
            throw new TaskException(e.getMessage(), e.getOpResultStatus(), taskRunResultStatus, e.getCause());
        } catch (Throwable t) {
            logException(t);
            throw t;
        } finally {
            ActivityPolicyProcessorHelper.clearCurrentActivityRun();
        }
    }

    private TaskRunResult createTaskRunResult(AbstractActivityRun<?, ?, ?> localRootRun, ActivityRunResult activityRunResult) {
        var throwable = activityRunResult.getThrowable();
        var message = activityRunResult.getMessage();

        TaskRunResult runResult = new TaskRunResult();

        runResult.setOperationResultStatus(activityRunResult.getOperationResultStatus());

        runResult.setThrowable(throwable);

        if (message != null) {
            runResult.setMessage(message);
        } else if (throwable != null) {
            runResult.setMessage(throwable.getMessage());
        }

        var taskRunResultStatus = determineTaskRunResultStatus(localRootRun, activityRunResult);
        runResult.setRunResultStatus(taskRunResultStatus);
        if (taskRunResultStatus == TaskRunResultStatus.RESTART_REQUESTED) {
            runResult.setRestartAfter(determineRestartAfter(localRootRun, activityRunResult));
        }
        // progress is intentionally kept null (meaning "do not update it in the task")
        return runResult;
    }

    private TaskRunResultStatus determineTaskRunResultStatus(
            AbstractActivityRun<?, ?, ?> localRootRun, ActivityRunResult activityRunResult) {
        var activityRunResultStatus = Objects.requireNonNullElse(activityRunResult.getRunResultStatus(), FINISHED);
        var taskRunResultStatus = activityRunResultStatus.toTaskRunResultStatus();
        if (taskRunResultStatus != null) {
            return taskRunResultStatus;
        }

        assert activityRunResultStatus == ABORTED;
        var activityState = localRootRun.getActivityState();
        if (activityState.isBeingRestartedOrSkipped() && !activityState.isWorker()) {
            // We are at the place where restart/skip is being processed: decide accordingly
            if (activityState.isBeingRestarted()) {
                return TaskRunResultStatus.RESTART_REQUESTED;
            } else {
                assert activityState.isBeingSkipped();
                return TaskRunResultStatus.FINISHED;
            }
        } else {
            // We are not at the place where restart/skip is being processed, so let's just close this task
            return TaskRunResultStatus.FINISHED;
        }
    }

    private long determineRestartAfter(AbstractActivityRun<?, ?, ?> localRootRun, ActivityRunResult activityRunResult) {
        RestartActivityPolicyActionType action;
        int executionAttempt;
        if (activityRunResult.isRestartRequested()) {
            var restartRequestingInformation = activityRunResult.getRestartRequestingInformationRequired();
            action = restartRequestingInformation.restartAction();
            executionAttempt = restartRequestingInformation.executionAttempt();
        } else {
            var activityState = localRootRun.getActivityState();
            assert activityState.isBeingRestarted();
            action = activityState.getRestartPolicyActionRequired();
            executionAttempt = activityState.getExecutionAttempt();
        }

        long baseDelay = Objects.requireNonNullElse(action.getDelay(), DEFAULT_RESTART_DELAY);
        long computedDelay;
        if (baseDelay <= 0) {
            computedDelay = 0;
            LOGGER.trace("No delay for activity restart as per policy action configuration");
        } else {
            computedDelay = baseDelay * (1L << Math.min(executionAttempt - 1, EXPONENTIAL_BACKOFF_LIMIT)) * 1000L;
            LOGGER.trace("Base delay = {} ms, execution attempt = {}, computed delay = {} ms",
                    baseDelay, executionAttempt, computedDelay);
        }
        return computedDelay;
    }

    private void setupTaskArchetypeIfNeeded(OperationResult result) throws ActivityRunException {
        if (activityBasedTaskHandler.isAvoidAutoAssigningArchetypes()) {
            return;
        }
        try {
            RunningTask task = getRunningTask();
            String defaultArchetypeOid = activityTree.getRootActivity().getHandler().getDefaultArchetypeOid();
            if (defaultArchetypeOid != null) {
                task.addArchetypeInformationIfMissing(defaultArchetypeOid);
                task.flushPendingModifications(result);
            }
        } catch (CommonException e) {
            throw new ActivityRunException(
                    "Couldn't setup the task archetype", FATAL_ERROR, ActivityRunResultStatus.PERMANENT_ERROR, e);
        }
    }

    /**
     * On root run start we have to cleanup
     */
    private void updateStateOnRootRunStart(OperationResult result) throws ActivityRunException {
        // Note that we prepare new realization also if the realization is in preparation, i.e. we were
        // interrupted during previous realization reparation action.
        if (isFirstRealization() || isRealizationComplete() || isRealizationInPreparation()) {
            prepareNewRealization(result);

            // this will not flush the task run identifier, it will be flushed
            // later together with the realization state
            activityTree.createTaskRunIdentifier();
            activityTree.recordTaskRunHistoryStart();
        }
        activityTree.updateRealizationState(ActivityTreeRealizationStateType.IN_PROGRESS, result);
    }

    private void prepareNewRealization(OperationResult result) throws ActivityRunException {
        activityTree.updateRealizationState(ActivityTreeRealizationStateType.IN_PREPARATION, result);
        activityTree.purgeState(this, result);
    }

    private void updateStateOnRootRunEnd(ActivityRunResult runResult, OperationResult result) throws ActivityRunException {
        // TODO clean up things related to !canRun & finished
        if (getRunningTask().canRun() && runResult.isFinished()) {
            // this will not flush the task run history, it will be flushed
            // later together with the realization state
            activityTree.recordTaskRunHistoryEnd();

            activityTree.updateRealizationState(ActivityTreeRealizationStateType.COMPLETE, result);
        }
    }

    private boolean isFirstRealization() {
        return activityTree.getRealizationState() == null;
    }

    private boolean isRealizationComplete() {
        return activityTree.getRealizationState() == ActivityTreeRealizationStateType.COMPLETE;
    }

    private boolean isRealizationInPreparation() {
        return activityTree.getRealizationState() == ActivityTreeRealizationStateType.IN_PREPARATION;
    }

    private boolean isRootRun() {
        return localRootActivity.doesTaskExecuteTreeRootActivity(runningTask);
    }

    private void logStart() {
        LOGGER.trace("""
                        Starting activity-based task run (is root run = {}):
                         - local root path: '{}'
                         - local root activity: {}
                         - activity tree:
                        {}""",
                isRootRun(), localRootPath, localRootActivity,
                activityTree.debugDumpLazily(2));
        if (isRootRun()) {
            LOGGER.trace("Tree realization state: {}", activityTree.getRealizationState());
        }
    }

    private void logEnd(AbstractActivityRun<?, ?, ?> localRootRun, ActivityRunResult runResult, TaskRunResult taskRunResult) {
        LOGGER.trace("""
                        Local root activity run ends.
                         - activity run result: {}
                         - task run result: {}
                         - AbstractActivityRun object:
                        {}""",
                runResult.shortDumpLazily(),
                taskRunResult,
                localRootRun.debugDumpLazily(3));
    }

    private void logException(Throwable t) {
        LOGGER.debug("Activity tree run finished with exception: {}", t.getMessage(), t);
    }

    @NotNull
    @Override
    public RunningTask getRunningTask() {
        return runningTask;
    }

    @NotNull
    @Override
    public CommonTaskBeans getBeans() {
        return activityBasedTaskHandler.getBeans();
    }

    @SuppressWarnings("unused")
    public ActivityTree getActivityTree() {
        return activityTree;
    }

    public Activity<?, ?> getLocalRootActivity() {
        return localRootActivity;
    }

    @Override
    public Long heartbeat() {
        return LegacyProgressUpdater.compute(this);
    }


}
