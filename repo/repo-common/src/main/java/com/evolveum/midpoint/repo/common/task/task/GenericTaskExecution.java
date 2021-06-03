/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.task;

import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
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

/**
 * Execution of a generic task, i.e. task that is driven by definition of its activities.
 */
public class GenericTaskExecution implements TaskExecution {

    private static final Trace LOGGER = TraceManager.getTrace(GenericTaskExecution.class);

    @NotNull private final RunningTask runningTask;
    @NotNull private final GenericTaskHandler genericTaskHandler;

    private ActivityTree activityTree;
    private ActivityPath localRootPath;
    private Activity<?, ?> localRoot;

    GenericTaskExecution(@NotNull RunningTask runningTask, @NotNull GenericTaskHandler genericTaskHandler) {
        this.runningTask = runningTask;
        this.genericTaskHandler = genericTaskHandler;
    }

    @NotNull
    @Override
    public TaskRunResult run(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException {

        localRootPath = TaskWorkStateUtil.getLocalRootPath(runningTask.getWorkState());
        activityTree = ActivityTree.create(getRootTask(), getBeans());
        localRoot = activityTree.getActivity(localRootPath);
        localRoot.setLocalRoot(true);

        logStart();

        AbstractActivityExecution<?, ?> localRootExecution = localRoot.createExecution(this, result);
        ActivityExecutionResult executionResult = localRootExecution.execute(result);

        logEnd(localRootExecution, executionResult);

        return executionResult.getTaskRunResult();
    }

    private void logStart() {
        LOGGER.trace("Activity tree before execution (local root = '{}'):\n{}",
                localRootPath, activityTree.debugDumpLazily());
    }

    private void logEnd(AbstractActivityExecution<?, ?> localRootExecution, ActivityExecutionResult executionResult) {
        LOGGER.trace("Local root activity execution object after execution ({})\n{}",
                executionResult.shortDumpLazily(), localRootExecution.debugDumpLazily());
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

    public ActivityTree getActivityTree() {
        return activityTree;
    }

    public Activity<?, ?> getLocalRoot() {
        return localRoot;
    }
}
