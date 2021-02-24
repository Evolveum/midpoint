/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.run;

import java.util.List;

import com.evolveum.midpoint.task.api.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * @author katka
 *
 */
@Component
public class HandlerExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(HandlerExecutor.class);
    private static final String DOT_CLASS = HandlerExecutor.class.getName() + ".";

    private static final long FREE_BUCKET_WAIT_TIME = -1;        // indefinitely

    @Autowired private PrismContext prismCtx;
    @Autowired private TaskManager taskManager;

    @NotNull
    public TaskRunResult executeHandler(RunningTaskQuartzImpl task, TaskPartitionDefinitionType partition, TaskHandler handler, OperationResult executionResult) {
        if (handler instanceof WorkBucketAwareTaskHandler) {
            return executeWorkBucketAwareTaskHandler(task, partition, (WorkBucketAwareTaskHandler) handler, executionResult);
        } else {
            return executePlainTaskHandler(task, partition, handler);
        }
    }

    @NotNull
    private TaskRunResult executePlainTaskHandler(RunningTask task, TaskPartitionDefinitionType partition, TaskHandler handler) {
        TaskRunResult runResult;
        try {
            LOGGER.trace("Executing handler {}", handler.getClass().getName());
            task.startCollectingOperationStats(handler.getStatisticsCollectionStrategy(), true);
            runResult = handler.run(task, partition);
            task.storeOperationStatsDeferred();
            if (runResult == null) {                // Obviously an error in task handler
                LOGGER.error("Unable to record run finish: task returned null result");
                runResult = createFailureTaskRunResult(task, "Unable to record run finish: task returned null result", null);
            }
        } catch (Throwable t) {
            LOGGER.error("Task handler threw unexpected exception: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task, t);
            runResult = createFailureTaskRunResult(task, "Task handler threw unexpected exception: " + t.getMessage(), t);
        }
        return runResult;
    }

    @NotNull
    private TaskRunResult executeWorkBucketAwareTaskHandler(RunningTaskQuartzImpl task, TaskPartitionDefinitionType taskPartition, WorkBucketAwareTaskHandler handler, OperationResult executionResult) {
        WorkStateManager workStateManager = (WorkStateManager) taskManager.getWorkStateManager();

        if (task.getWorkState() != null && Boolean.TRUE.equals(task.getWorkState().isAllWorkComplete())) {
            LOGGER.debug("Work is marked as complete; restarting it in task {}", task);
            try {
                List<ItemDelta<?, ?>> itemDeltas = prismCtx.deltaFor(TaskType.class)
                        .item(TaskType.F_WORK_STATE).replace()
                        .asItemDeltas();
                task.applyDeltasImmediate(itemDeltas, executionResult);
            } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove work state from (completed) task {} -- continuing", e, task);
            }
        }

        task.startCollectingOperationStats(handler.getStatisticsCollectionStrategy(), true);

        TaskWorkBucketProcessingResult runResult = null;
        for (boolean initialBucket = true; ; initialBucket = false) {
            WorkBucketType bucket;
            try {
                try {
                    bucket = workStateManager.getWorkBucket(task.getOid(), FREE_BUCKET_WAIT_TIME, task::canRun, initialBucket,
                            task.getWorkBucketStatisticsCollector(), executionResult);
                } catch (InterruptedException e) {
                    LOGGER.trace("InterruptedExecution in getWorkBucket for {}", task);
                    if (task.canRun()) {
                        throw new IllegalStateException("Unexpected InterruptedException: " + e.getMessage(), e);
                    } else {
                        return createInterruptedTaskRunResult(task);
                    }
                }
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't allocate a work bucket for task {} (coordinator {})", t, task, null);
                return createFailureTaskRunResult(task, "Couldn't allocate a work bucket for task: " + t.getMessage(), t);
            }
            if (bucket == null) {
                LOGGER.trace("No (next) work bucket within {}, exiting", task);
                runResult = handler.onNoMoreBuckets(task, runResult);
                return runResult != null ? runResult : createSuccessTaskRunResult(task);
            }
            try {
                if (!initialBucket) {
                    task.startCollectingOperationStats(handler.getStatisticsCollectionStrategy(), false);
                }
                LOGGER.trace("Executing handler {} with work bucket of {} for {}", handler.getClass().getName(), bucket, task);
                runResult = handler.run(task, bucket, taskPartition, runResult);
                LOGGER.trace("runResult is {} for {}", runResult, task);
                task.storeOperationStatsDeferred();

                if (runResult == null) {                // Obviously an error in task handler
                    LOGGER.error("Unable to record run finish: task returned null result");
                    //releaseWorkBucketChecked(bucket, executionResult);
                    return createFailureTaskRunResult(task, "Unable to record run finish: task returned null result", null);
                }
            } catch (Throwable t) {
                LOGGER.error("Task handler threw unexpected exception: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task, t);
                //releaseWorkBucketChecked(bucket, executionResult);
                return createFailureTaskRunResult(task, "Task handler threw unexpected exception: " + t.getMessage(), t);
            }
            if (!runResult.isBucketComplete()) {
                return runResult;
            }
            try {
                ((WorkStateManager) taskManager.getWorkStateManager()).completeWorkBucket(task.getOid(), bucket.getSequentialNumber(),
                        task.getWorkBucketStatisticsCollector(), executionResult);
            } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't complete work bucket for task {}", e, task);
                return createFailureTaskRunResult(task, "Couldn't complete work bucket: " + e.getMessage(), e);
            }
            if (!task.canRun() || !runResult.isShouldContinue()) {
                return runResult;
            }
        }
    }

    @NotNull
    private TaskRunResult createFailureTaskRunResult(RunningTask task, String message, Throwable t) {
        TaskRunResult runResult = new TaskRunResult();
        OperationResult opResult;
        if (task.getResult() != null) {
            opResult = task.getResult();
        } else {
            opResult = createOperationResult(DOT_CLASS + "executeHandler");
        }
        if (t != null) {
            opResult.recordFatalError(message, t);
        } else {
            opResult.recordFatalError(message);
        }
        runResult.setOperationResult(opResult);
        runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
        return runResult;
    }

    @NotNull
    private TaskRunResult createSuccessTaskRunResult(RunningTask task) {
        TaskRunResult runResult = new TaskRunResult();
        OperationResult opResult;
        if (task.getResult() != null) {
            opResult = task.getResult();
        } else {
            opResult = createOperationResult(DOT_CLASS + "executeHandler");
        }
        opResult.recordSuccess();
        runResult.setOperationResult(opResult);
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        return runResult;
    }

    @NotNull
    private TaskRunResult createInterruptedTaskRunResult(RunningTask task) {
        TaskRunResult runResult = new TaskRunResult();
        OperationResult opResult;
        if (task.getResult() != null) {
            opResult = task.getResult();
        } else {
            opResult = createOperationResult(DOT_CLASS + "executeHandler");
        }
        opResult.recordSuccess();
        runResult.setOperationResult(opResult);
        runResult.setRunResultStatus(TaskRunResultStatus.INTERRUPTED);
        return runResult;
    }

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(DOT_CLASS + methodName);
    }

}
