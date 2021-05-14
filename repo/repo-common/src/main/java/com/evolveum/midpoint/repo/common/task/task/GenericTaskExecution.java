/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.task;

import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.task.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.task.execution.ActivityContext.RootActivityContext;

import com.evolveum.midpoint.repo.common.task.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecution;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecutionResult;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
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

    private ActivityExecution rootActivityExecution;

    GenericTaskExecution(@NotNull RunningTask task, @NotNull GenericTaskHandler genericTaskHandler) {
        this.task = task;
        this.genericTaskHandler = genericTaskHandler;
    }

    @NotNull
    @Override
    public TaskRunResult run(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException {

        createActivityExecution(result);

        LOGGER.trace("Root activity execution object before execution:\n{}", rootActivityExecution.debugDumpLazily());
        ActivityExecutionResult executionResult = rootActivityExecution.execute(result);
        LOGGER.trace("Root activity execution object after execution ({})\n{}",
                executionResult.shortDumpLazily(), rootActivityExecution.debugDumpLazily());

        return executionResult.getTaskRunResult();
    }

    private <WD extends AbstractWorkDefinition> void createActivityExecution(OperationResult result) throws SchemaException {
        ActivityDefinition<WD> activityDefinition = ActivityDefinition.createRoot(this);
        rootActivityExecution = getBeans().activityHandlerRegistry
                .getHandler(activityDefinition)
                .createExecution(new RootActivityContext<>(activityDefinition, this), result);
    }

    @Override
    public @NotNull RunningTask getTask() {
        return task;
    }

    @Override
    public @NotNull CommonTaskBeans getBeans() {
        return genericTaskHandler.getBeans();
    }
}
