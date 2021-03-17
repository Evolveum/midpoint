/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.task.api.WorkBucketAwareTaskHandler;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskBeans;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.evolveum.midpoint.task.quartzimpl.run.HandlerExecutor.*;

/**
 * An execution of bucket-aware task handler.
 *
 * This execution has quite a lot of internal state, so it deserves a separate class: this one.
 */
class BucketAwareHandlerExecution {
    private static final Trace LOGGER = TraceManager.getTrace(BucketAwareHandlerExecution.class);

    private static final long FREE_BUCKET_WAIT_TIME = -1; // indefinitely

    @NotNull private final RunningTaskQuartzImpl task;
    @NotNull private final WorkBucketAwareTaskHandler handler;
    @NotNull private final TaskBeans beans;
    @Nullable private final TaskPartitionDefinitionType partition;

    /**
     * Current task run result.
     */
    private TaskWorkBucketProcessingResult runResult;

    /**
     * Are we in the initial execution (i.e. processing first bucket)?
     */
    private boolean initialExecution = true;

    public BucketAwareHandlerExecution(@NotNull RunningTaskQuartzImpl task, @NotNull WorkBucketAwareTaskHandler handler,
            @Nullable TaskPartitionDefinitionType partition, @NotNull TaskBeans beans) {
        this.task = task;
        this.partition = partition;
        this.handler = handler;
        this.beans = beans;
    }

    @NotNull public TaskRunResult execute(OperationResult result) throws ExitExecutionException {

        deleteWorkStateIfWorkComplete(result);

        startCollectingOperationStatsForInitialExecution(task, handler);

        for (; task.canRun(); initialExecution = false) {

            WorkBucketType bucket = getWorkBucket(result);
            if (bucket == null) {
                LOGGER.trace("No (next) work bucket within {}, exiting", task);
                runResult = handler.onNoMoreBuckets(task, runResult);
                break;
            }

            if (!initialExecution) {
                startCollectingOperationStatsForFurtherExecutions(task, handler);
            }

            executeHandlerForBucket(bucket, result);

            if (runResult.isBucketComplete()) {
                completeWorkBucketAndUpdateStructuredProgress(bucket, result);
            } else {
                // There is no point in continuing with bucket not completed, because the bucket would simply get
                // re-processed by this task. Moreover, when we are here, it means that !task.canRun() anyway.
                break;
            }
        }
        //noinspection ReplaceNullCheck
        if (runResult != null) {
            return runResult;
        } else {
            // Maybe we were stopped before the first execution. Or there are no buckets.
            return createSuccessTaskRunResult(task);
        }
    }

    /**
     * Besides marking the bucket as complete we also move "open" counters in the structured progress to the "closed" state.
     */
    private void completeWorkBucketAndUpdateStructuredProgress(WorkBucketType bucket, OperationResult result)
            throws ExitExecutionException {
        try {
            beans.workStateManager.completeWorkBucket(task.getOid(), bucket.getSequentialNumber(),
                    task.getWorkBucketStatisticsCollector(), result);
            task.updateStructuredProgressOnWorkBucketCompletion();
            storeOperationStatsPersistently(task, result);
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't complete work bucket for task {}", e, task);
            throw new ExitExecutionException(task, "Couldn't complete work bucket: " + e.getMessage(), e);
        }
    }

    private void executeHandlerForBucket(WorkBucketType bucket, OperationResult result) throws ExitExecutionException {
        try {
            LOGGER.trace("Executing handler {} with work bucket of {} for {}", handler.getClass().getName(), bucket, task);
            runResult = handler.run(task, bucket, partition, runResult);
            LOGGER.trace("runResult is {} for {}", runResult, task);

            storeOperationStatsPersistently(task, result);

            checkNullRunResult(task, runResult);

        } catch (Throwable t) {
            processHandlerException(task, t);
        }
    }

    private WorkBucketType getWorkBucket(OperationResult result) throws ExitExecutionException {
        WorkBucketType bucket;
        try {
            bucket = beans.workStateManager.getWorkBucket(task.getOid(), FREE_BUCKET_WAIT_TIME, task::canRun, initialExecution,
                    task.getWorkBucketStatisticsCollector(), result);
        } catch (InterruptedException e) {
            LOGGER.trace("InterruptedExecution in getWorkBucket for {}", task);
            if (!task.canRun()) {
                throw new ExitExecutionException(createInterruptedTaskRunResult(task));
            } else {
                LoggingUtils.logUnexpectedException(LOGGER, "Unexpected InterruptedException in {}", e, task);
                throw new ExitExecutionException(task, "Unexpected InterruptedException: " + e.getMessage(), e);
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't allocate a work bucket for task {}", t, task);
            throw new ExitExecutionException(task, "Couldn't allocate a work bucket for task: " + t.getMessage(), t);
        }
        return bucket;
    }

    private void deleteWorkStateIfWorkComplete(OperationResult executionResult) {
        if (task.getWorkState() != null && Boolean.TRUE.equals(task.getWorkState().isAllWorkComplete())) {
            LOGGER.debug("Work is marked as complete; restarting it in task {}", task);
            try {
                List<ItemDelta<?, ?>> itemDeltas = beans.prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_WORK_STATE).replace()
                        .asItemDeltas();
                task.applyDeltasImmediate(itemDeltas, executionResult);
            } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove work state from (completed) task {}, continuing", e, task);
            }
        }
    }
}
