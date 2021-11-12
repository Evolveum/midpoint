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
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.task.api.TaskRunResult.createFailureTaskRunResult;

/**
 * @author katka
 *
 */
@Component
public class HandlerExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(HandlerExecutor.class);

    @NotNull TaskRunResult executeHandler(RunningTaskQuartzImpl task, TaskHandler handler, OperationResult executionResult) {
        task.startCollectingStatistics(handler.getStatisticsCollectionStrategy());

        LOGGER.trace("Executing task handler {}", handler.getClass().getName());
        TaskRunResult runResult;
        try {
            runResult = handler.run(task);
            if (runResult == null) { // Obviously an error in task handler
                LOGGER.error("Unable to record run finish: task returned null result");
                runResult = createFailureTaskRunResult("Task returned null result", null);
            }
        } catch (TaskException e) {
            runResult = TaskRunResult.createFromTaskException(e);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Task handler threw unexpected exception: {}: {}; task = {}",
                    t, t.getClass().getName(), t.getMessage(), task);
            runResult = createFailureTaskRunResult("Task handler threw unexpected exception: " + t.getMessage(), t);
        }
        LOGGER.trace("runResult is {} for {}", runResult, task);

        updateAndStoreTaskStatisticsIntoRepository(task, executionResult);

        return runResult;
    }

    private void updateAndStoreTaskStatisticsIntoRepository(RunningTaskQuartzImpl task, OperationResult result) {
        try {
            task.updateAndStoreStatisticsIntoRepository(true, result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store operation statistics to {}", e, task);
            // intentionally continuing
        }
    }
}
