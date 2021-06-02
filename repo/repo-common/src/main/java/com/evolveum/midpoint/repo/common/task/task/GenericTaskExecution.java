/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
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

    @NotNull private final RunningTask task;
    @NotNull private final GenericTaskHandler genericTaskHandler;

    private ActivityTree activityTree;

    GenericTaskExecution(@NotNull RunningTask task, @NotNull GenericTaskHandler genericTaskHandler) {
        this.task = task;
        this.genericTaskHandler = genericTaskHandler;
    }

    @NotNull
    @Override
    public TaskRunResult run(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException {

        activityTree = ActivityTree.create(this);

        LOGGER.trace("Activity tree before execution:\n{}", activityTree.debugDumpLazily());

        AbstractActivityExecution<?, ?> rootExecution = activityTree.getRootActivity()
                .createExecution(this, result);
        ActivityExecutionResult executionResult = rootExecution.execute(result);

        LOGGER.trace("Root activity execution object after execution ({})\n{}",
                executionResult.shortDumpLazily(), rootExecution.debugDumpLazily());

        return executionResult.getTaskRunResult();
    }

    @NotNull
    @Override
    public RunningTask getTask() {
        return task;
    }

    @NotNull
    @Override
    public CommonTaskBeans getBeans() {
        return genericTaskHandler.getBeans();
    }

    public ActivityTree getActivityTree() {
        return activityTree;
    }
}
