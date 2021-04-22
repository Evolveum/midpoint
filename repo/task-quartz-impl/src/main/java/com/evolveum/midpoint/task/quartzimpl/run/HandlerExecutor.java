/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.task.api.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author katka
 *
 */
@Component
public class HandlerExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(HandlerExecutor.class);

    @NotNull TaskRunResult executeHandler(RunningTaskQuartzImpl task, TaskHandler handler, OperationResult executionResult) {
        try {
            try {
                // TODO?
                startCollectingStatistics(task, handler);

                LOGGER.trace("Executing non-bucketed task handler {}", handler.getClass().getName());
                TaskRunResult runResult;
                try {
                    runResult = handler.run(task);
                } catch (ExitExecutionException e) {
                    runResult = e.getRunResult();
                }
                LOGGER.trace("runResult is {} for {}", runResult, task);

                // TODO?
                updateAndStoreStatisticsIntoRepository(task, executionResult);

                checkNullRunResult(task, runResult);
                return runResult;
            } catch (Throwable t) {
                return processHandlerException(task, t);
            }
        } catch (ExitExecutionException e) {
            return e.getRunResult();
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
}
