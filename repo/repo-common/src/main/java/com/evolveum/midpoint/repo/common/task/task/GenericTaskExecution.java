/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.task;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.state.LegacyProgressUpdater;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTreeRealizationStateType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.repo.common.activity.ActivityTree;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

/**
 * Execution of a generic task, i.e. task that is driven by definition of its activities.
 *
 * TODO Consider renaming to `ActivityTaskExecution`
 */
public class GenericTaskExecution implements TaskExecution {

    private static final Trace LOGGER = TraceManager.getTrace(GenericTaskExecution.class);

    @NotNull private final RunningTask runningTask;
    @NotNull private final GenericTaskHandler genericTaskHandler;

    private ActivityTree activityTree;
    private ActivityPath localRootPath;
    private Activity<?, ?> localRootActivity;

    GenericTaskExecution(@NotNull RunningTask runningTask, @NotNull GenericTaskHandler genericTaskHandler) {
        this.runningTask = runningTask;
        this.genericTaskHandler = genericTaskHandler;
    }

    @NotNull
    @Override
    public TaskRunResult run(OperationResult result) throws TaskException {

        try {
            activityTree = ActivityTree.create(getRootTask(), getBeans());
            localRootPath = ActivityStateUtil.getLocalRootPath(runningTask.getWorkState());

            localRootActivity = activityTree.getActivity(localRootPath);
            localRootActivity.setLocalRoot(true);
        } catch (CommonException e) {
            throw new TaskException("Couldn't initialize activity tree", FATAL_ERROR, PERMANENT_ERROR, e);
        }

        if (localRootActivity.isSkipped()) {
            LOGGER.trace("Local root activity is skipped, exiting");
            return TaskRunResult.createNotApplicableTaskRunResult();
        }

        logStart();

        try {
            if (isRootExecution()) {
                setupTaskArchetypeIfNeeded(result);
                updateStateOnRootExecutionStart(result);
            }

            AbstractActivityExecution<?, ?, ?> localRootExecution = localRootActivity.createExecution(this, result);
            ActivityExecutionResult executionResult = localRootExecution.execute(result);

            if (isRootExecution()) {
                updateStateOnRootExecutionEnd(executionResult, result);
            }

            logEnd(localRootExecution, executionResult);
            return executionResult.createTaskRunResult();
        } catch (ActivityExecutionException e) {
            // These should be only really unexpected ones. So we won't bother with e.g. updating the tree state.
            logException(e);
            throw e.toTaskException();
        } catch (Throwable t) {
            logException(t);
            throw t;
        }
    }

    private void setupTaskArchetypeIfNeeded(OperationResult result) throws ActivityExecutionException {
        if (genericTaskHandler.isAvoidAutoAssigningArchetypes()) {
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
            throw new ActivityExecutionException("Couldn't setup the task archetype", FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    /**
     * On root execution start we have to cleanup
     */
    private void updateStateOnRootExecutionStart(OperationResult result) throws ActivityExecutionException {
        // Note that we prepare new realization also if the realization is in preparation, i.e. we were
        // interrupted during previous realization reparation action.
        if (isFirstRealization() || isRealizationComplete() || isRealizationInPreparation()) {
            prepareNewRealization(result);
        }
        activityTree.updateRealizationState(ActivityTreeRealizationStateType.IN_PROGRESS, result);
    }

    private void prepareNewRealization(OperationResult result) throws ActivityExecutionException {
        activityTree.updateRealizationState(ActivityTreeRealizationStateType.IN_PREPARATION, result);
        activityTree.purgeState(this, result);
    }

    private void updateStateOnRootExecutionEnd(ActivityExecutionResult executionResult, OperationResult result) throws ActivityExecutionException {
        // TODO clean up things related to !canRun & finished
        if (getRunningTask().canRun() && executionResult.isFinished()) {
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

    private boolean isRootExecution() {
        return localRootActivity.doesTaskExecuteTreeRootActivity(runningTask);
    }

    private void logStart() {
        LOGGER.trace("Starting generic task execution (is root execution = {}):\n"
                + " - local root path: '{}'\n"
                + " - local root activity: {}\n"
                + " - activity tree:\n{}",
                isRootExecution(), localRootPath, localRootActivity,
                activityTree.debugDumpLazily(2));
        if (isRootExecution()) {
            LOGGER.trace("Tree realization state: {}", activityTree.getRealizationState());
        }
    }

    private void logEnd(AbstractActivityExecution<?, ?, ?> localRootExecution, ActivityExecutionResult executionResult) {
        LOGGER.trace("Local root activity execution object after execution ({})\n{}",
                executionResult.shortDumpLazily(), localRootExecution.debugDumpLazily());
    }

    private void logException(Throwable t) {
        LOGGER.debug("Activity tree execution finished with exception: {}", t.getMessage(), t);
    }

    @NotNull
    @Override
    public RunningTask getRunningTask() {
        return runningTask;
    }

    @NotNull
    @Override
    public CommonTaskBeans getBeans() {
        return genericTaskHandler.getBeans();
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
