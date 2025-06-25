/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.task;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.state.LegacyProgressUpdater;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTreeRealizationStateType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.repo.common.activity.ActivityTree;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
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
 * A run of an activity-based task.
 */
public class ActivityBasedTaskRun implements TaskRun {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityBasedTaskRun.class);

    @NotNull private final RunningTask runningTask;
    @NotNull private final ActivityBasedTaskHandler activityBasedTaskHandler;

    private ActivityTree activityTree;
    private ActivityPath localRootPath;
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
            throw new TaskException("Couldn't initialize activity tree", FATAL_ERROR, PERMANENT_ERROR, e);
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
            ActivityRunResult runResult = localRootRun.run(result);

            if (isRootRun()) {
                updateStateOnRootRunEnd(runResult, result);
            }

            logEnd(localRootRun, runResult);
            return runResult.createTaskRunResult();
        } catch (ActivityRunException e) {
            // These should be only really unexpected ones. So we won't bother with e.g. updating the tree state.
            logException(e);
            throw e.toTaskException();
        } catch (Throwable t) {
            logException(t);
            throw t;
        }
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
            throw new ActivityRunException("Couldn't setup the task archetype", FATAL_ERROR, PERMANENT_ERROR, e);
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
            activityTree.createTaskRunIdentifier(result);
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
        LOGGER.trace("Starting activity-based task run (is root run = {}):\n"
                + " - local root path: '{}'\n"
                + " - local root activity: {}\n"
                + " - activity tree:\n{}",
                isRootRun(), localRootPath, localRootActivity,
                activityTree.debugDumpLazily(2));
        if (isRootRun()) {
            LOGGER.trace("Tree realization state: {}", activityTree.getRealizationState());
        }
    }

    private void logEnd(AbstractActivityRun<?, ?, ?> localRootRun, ActivityRunResult runResult) {
        LOGGER.trace("Local root activity run object after task run ({})\n{}",
                runResult.shortDumpLazily(), localRootRun.debugDumpLazily());
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
