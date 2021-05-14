/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.execution.ActivityContext;
import com.evolveum.midpoint.repo.common.task.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecutionResult;
import com.evolveum.midpoint.repo.common.task.task.TaskExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * Execution of mock opening or closing activity.
 */
public abstract class CompositeMockSubActivityExecution extends AbstractActivityExecution<CompositeMockWorkDefinition> {

    public static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/repo-common-test/extension";

    private static final Trace LOGGER = TraceManager.getTrace(CompositeMockSubActivityExecution.class);

    CompositeMockSubActivityExecution(@NotNull ActivityContext<CompositeMockWorkDefinition> context) {
        super(context);
    }

    @Override
    public @NotNull ActivityExecutionResult execute(OperationResult result)
            throws CommonException, TaskException, PreconditionViolationException {

        CompositeMockWorkDefinition workDef = activityDefinition.getWorkDefinition();

        int steps = workDef.getSteps();
        long delay = workDef.getDelay();

        LOGGER.info("Mock activity starting: id={}, steps={}, delay={}, sub-activity={}:\n{}", workDef.getIdentifier(),
                steps, delay, getSubActivity(), debugDumpLazily());

        ActivityExecutionResult executionResult = new ActivityExecutionResult();

        RunningTask task = taskExecution.getTask();

        task.incrementProgressAndStoreStatisticsIfTimePassed(result);

        if (delay > 0) {
            sleep(task, delay);
        }

        result.recordSuccess();

        // This "run" is finished. But the task goes on ...
        executionResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);

        ((CompositeMockActivityExecution) parent).recordExecution(workDef.getIdentifier() + ":" + getSubActivity());

        LOGGER.info("Mock activity finished: id={}, sub-activity={}:\n{}", workDef.getIdentifier(), getSubActivity(),
                debugDumpLazily());

        return executionResult;
    }

    private void sleep(RunningTask task, long delay) {
        LOGGER.trace("Sleeping for {} msec", delay);
        long end = System.currentTimeMillis() + delay;
        while (task.canRun() && System.currentTimeMillis() < end) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public @NotNull TaskExecution getTaskExecution() {
        return taskExecution;
    }

    abstract String getSubActivity();

    @Override
    public String toString() {
        return "CompositeMockSubActivityExecution{" +
                "activityDefinition=" + activityDefinition +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "current recorder state", ((CompositeMockActivityExecution) parent).getRecorder(), indent+1);
        return sb.toString();
    }
}
