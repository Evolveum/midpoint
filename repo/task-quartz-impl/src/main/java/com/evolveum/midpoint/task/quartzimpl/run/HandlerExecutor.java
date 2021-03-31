/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.TaskBeans;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

/**
 * @author katka
 *
 */
@Component
public class HandlerExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(HandlerExecutor.class);
    private static final String DOT_CLASS = HandlerExecutor.class.getName() + ".";

    @Autowired private TaskBeans beans;

    @NotNull
    public TaskRunResult executeHandler(RunningTaskQuartzImpl task, TaskPartitionDefinitionType partition, TaskHandler handler, OperationResult executionResult) {
        try {
            if (handler instanceof WorkBucketAwareTaskHandler) {
                return new BucketAwareHandlerExecution(task, (WorkBucketAwareTaskHandler) handler, partition, beans)
                        .execute(executionResult);
            } else {
                return executePlainTaskHandler(task, partition, handler, executionResult);
            }
        } catch (ExitExecutionException e) {
            return e.runResult;
        }
    }

    @NotNull
    private TaskRunResult executePlainTaskHandler(RunningTaskQuartzImpl task, TaskPartitionDefinitionType partition,
            TaskHandler handler, OperationResult result) throws ExitExecutionException {
        try {
            startCollectingStatistics(task, handler);

            LOGGER.trace("Executing non-bucketed task handler {}", handler.getClass().getName());
            TaskRunResult runResult = handler.run(task, partition);
            LOGGER.trace("runResult is {} for {}", runResult, task);

            updateAndStoreStatisticsIntoRepository(task, result);

            checkNullRunResult(task, runResult);
            return runResult;
        } catch (Throwable t) {
            return processHandlerException(task, t);
        }
    }

    static TaskRunResult processHandlerException(RunningTaskQuartzImpl task, Throwable t) throws ExitExecutionException {
        LOGGER.error("Task handler threw unexpected exception: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task, t);
        throw new ExitExecutionException(task, "Task handler threw unexpected exception: " + t.getMessage(), t);
    }

    static void checkNullRunResult(RunningTask task, TaskRunResult runResult) throws ExitExecutionException {
        if (runResult == null) {                // Obviously an error in task handler
            LOGGER.error("Unable to record run finish: task returned null result");
            throw new ExitExecutionException(task, "Task returned null result", null);
        }
    }

    static void startCollectingStatistics(RunningTask task, TaskHandler handler) {
        task.startCollectingStatistics(handler.getStatisticsCollectionStrategy());
    }

    static void updateAndStoreStatisticsIntoRepository(RunningTaskQuartzImpl task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        try {
            task.updateAndStoreStatisticsIntoRepository(true, result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store operation statistics to {}", e, task);
            // intentionally continuing
        }
    }

    @NotNull private static TaskRunResult createFailureTaskRunResult(RunningTask task, String message, Throwable t) {
        TaskRunResult runResult = createRunResult(task);
        if (t != null) {
            runResult.getOperationResult().recordFatalError(message, t);
        } else {
            runResult.getOperationResult().recordFatalError(message);
        }
        runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
        return runResult;
    }

    @NotNull static TaskRunResult createSuccessTaskRunResult(RunningTask task) {
        TaskRunResult runResult = createRunResult(task);
        runResult.getOperationResult().recordSuccess();
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        return runResult;
    }

    @NotNull static TaskRunResult createInterruptedTaskRunResult(RunningTask task) {
        TaskRunResult runResult = createRunResult(task);
        runResult.getOperationResult().recordSuccess();
        runResult.setRunResultStatus(TaskRunResultStatus.INTERRUPTED);
        return runResult;
    }

    private static TaskRunResult createRunResult(RunningTask task) {
        TaskRunResult runResult = new TaskRunResult();
        OperationResult opResult;
        if (task.getResult() != null) {
            opResult = task.getResult();
        } else {
            opResult = new OperationResult(DOT_CLASS + (DOT_CLASS + "executeHandler"));
        }
        runResult.setOperationResult(opResult);
        return runResult;
    }

    /**
     * Exception signalling we should exit the (plain or bucketed) handler execution.
     */
    @Experimental
    static class ExitExecutionException extends Exception {

        @NotNull final TaskRunResult runResult;

        ExitExecutionException(@NotNull TaskRunResult runResult) {
            this.runResult = runResult;
        }

        ExitExecutionException(RunningTask task, String message, Throwable cause) {
            this.runResult = createFailureTaskRunResult(task, message, cause);
        }
    }
}
