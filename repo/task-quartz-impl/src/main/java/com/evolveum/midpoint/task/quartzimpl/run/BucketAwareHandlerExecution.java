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

import java.util.ArrayList;
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

    BucketAwareHandlerExecution(@NotNull RunningTaskQuartzImpl task, @NotNull WorkBucketAwareTaskHandler handler,
            @Nullable TaskPartitionDefinitionType partition, @NotNull TaskBeans beans) {
        this.task = task;
        this.partition = partition;
        this.handler = handler;
        this.beans = beans;
    }

    @NotNull public TaskRunResult execute(OperationResult result) throws ExitExecutionException {

        resetWorkStateAndStatisticsIfWorkComplete(result);

        for (; task.canRun(); initialExecution = false) {

            // We must start collecting statistics inside this cycle. The reason is that - in multithreaded scenarios -
            // some of the statistics exist only in LATs (e.g. iteration statistics) and so their source form simply disappear
            // on previous bucket end. Their aggregated form is preserved in this running (non-LAT) parent, but is overwritten
            // as soon as next cycle starts with new LATs. Therefore we copy the aggregated form into internal structures
            // as initial values, to be preserved.
            startCollectingStatistics(task, handler);

            WorkBucketType bucket = getWorkBucket(result);
            if (bucket == null) {
                LOGGER.trace("No (next) work bucket within {}, exiting", task);
                task.markStructuredProgressAsComplete();
                runResult = handler.onNoMoreBuckets(task, runResult, result);
                break;
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

        task.updateAndStoreStatisticsIntoRepository(true, result);

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
            task.changeStructuredProgressOnWorkBucketCompletion();
            updateAndStoreStatisticsIntoRepository(task, result);
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

            updateAndStoreStatisticsIntoRepository(task, result);

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

    private void resetWorkStateAndStatisticsIfWorkComplete(OperationResult result) throws ExitExecutionException {
        if (isAllWorkComplete()) {
            LOGGER.debug("Work is marked as complete; restarting it in task {}", task);
            try {
                List<ItemDelta<?, ?>> itemDeltas = new ArrayList<>();
                itemDeltas.add(beans.prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_WORK_STATE).replace()
                        .asItemDelta());

                if (handler.getStatisticsCollectionStrategy().isStartFromZero()) {
                    LOGGER.debug("Resetting all statistics in task {} on start", task);
                    itemDeltas.addAll(
                            beans.prismContext.deltaFor(TaskType.class)
                                    .item(TaskType.F_PROGRESS).replace()
                                    .item(TaskType.F_STRUCTURED_PROGRESS).replace()
                                    .item(TaskType.F_OPERATION_STATS).replace()
                                    .asItemDeltas());
                }
                task.modify(itemDeltas);
                task.flushPendingModifications(result);
            } catch (Throwable t) {
                throw new ExitExecutionException(task, "Couldn't reset work state and/or statistics at start", t);
            }
        }
    }

    private boolean isAllWorkComplete() {
        return task.getWorkState() != null && Boolean.TRUE.equals(task.getWorkState().isAllWorkComplete());
    }
}
