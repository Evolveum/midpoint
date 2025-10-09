/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.quartzimpl.run;

/**
 * An execution of bucket-aware task handler.
 *
 * This execution has quite a lot of internal state, so it deserves a separate class: this one.
 *
 * TODO REMOVE. Keeping this class only to preserve the code, before it's moved somewhere into AbstractTaskPartExecution.
 */
class BucketAwareHandlerExecution {
//    private static final Trace LOGGER = TraceManager.getTrace(BucketAwareHandlerExecution.class);
//
//    private static final long FREE_BUCKET_WAIT_TIME = -1; // indefinitely
//
//    @NotNull private final RunningTaskQuartzImpl task;
//    @NotNull private final WorkBucketAwareTaskHandler handler;
//    @NotNull private final TaskBeans beans;
//    @Nullable private final ActivityDefinitionType partition;
//
//    /**
//     * Current task run result.
//     */
//    private TaskWorkBucketProcessingResult runResult;
//
//    /**
//     * Are we in the initial execution (i.e. processing first bucket)?
//     */
//    private boolean initialExecution = true;
//
//    BucketAwareHandlerExecution(@NotNull RunningTaskQuartzImpl task, @NotNull WorkBucketAwareTaskHandler handler,
//                                @Nullable ActivityDefinitionType partition, @NotNull TaskBeans beans) {
//        this.task = task;
//        this.partition = partition;
//        this.handler = handler;
//        this.beans = beans;
//    }
//
//    @NotNull public TaskRunResult execute(OperationResult result) throws StopHandlerExecutionException {
//
//        resetWorkStateAndStatisticsIfWorkComplete(result);
//        startCollectingStatistics(task, handler);
//
//        for (; task.canRun(); initialExecution = false) {
//
//            WorkBucketType bucket = getWorkBucket(result);
//            if (bucket == null) {
//                LOGGER.trace("No (next) work bucket within {}, exiting", task);
//                updateStructuredProgressOnNoMoreBuckets();
//                runResult = handler.onNoMoreBuckets(task, runResult, result);
//                break;
//            }
//
//            executeHandlerForBucket(bucket, result);
//
//            if (runResult.isBucketComplete()) {
//                completeWorkBucketAndUpdateStructuredProgress(bucket, result);
//            } else {
//                // There is no point in continuing with bucket not completed, because the bucket would simply get
//                // re-processed by this task. Moreover, when we are here, it means that !task.canRun() anyway.
//                break;
//            }
//        }
//
//        task.updateAndStoreStatisticsIntoRepository(true, result);
//
//        //noinspection ReplaceNullCheck
//        if (runResult != null) {
//            return runResult;
//        } else {
//            // Maybe we were stopped before the first execution. Or there are no buckets.
//            return createSuccessTaskRunResult(task);
//        }
//    }
//
//    private void updateStructuredProgressOnNoMoreBuckets() {
//        // This is for the last part of the internally-partitioned-single-bucket task;
//        // or for the (single) part of multi-bucket task.
//        task.markStructuredProgressAsComplete();
//
//        // TEMPORARY: For internally-partitioned-single-bucket tasks we have to move 'open' to 'closed' progress counters
//        // here. We couldn't do that earlier, because they are made really closed - i.e. not to be reprocessed - only after
//        // all the work is done (for such tasks). This is a temporary measure, until bucketing and partitioning are swapped
//        // in later versions of midPoint.
//        task.markAllStructuredProgressClosed();
//    }
//
//    /**
//     * Besides marking the bucket as complete we also move "open" counters in the structured progress to the "closed" state.
//     */
//    private void completeWorkBucketAndUpdateStructuredProgress(WorkBucketType bucket, OperationResult result)
//            throws StopHandlerExecutionException {
//        try {
//            beans.workStateManager.completeWorkBucket(task.getOid(), bucket.getSequentialNumber(),
//                    task.getWorkBucketStatisticsCollector(), result);
//            task.changeStructuredProgressOnWorkBucketCompletion();
//            updateAndStoreStatisticsIntoRepository(task, result);
//        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | RuntimeException e) {
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't complete work bucket for task {}", e, task);
//            throw new StopHandlerExecutionException(task, "Couldn't complete work bucket: " + e.getMessage(), e);
//        }
//    }
//
//    private void executeHandlerForBucket(WorkBucketType bucket, OperationResult result) throws StopHandlerExecutionException {
//        try {
//            LOGGER.trace("Executing handler {} with work bucket of {} for {}", handler.getClass().getName(), bucket, task);
//            runResult = handler.run(task, bucket, partition, runResult);
//            LOGGER.trace("runResult is {} for {}", runResult, task);
//
//            updateAndStoreStatisticsIntoRepository(task, result);
//
//            treatNullRunResult(task, runResult);
//
//        } catch (Throwable t) {
//            processHandlerException(task, t);
//        }
//    }
//
//    private WorkBucketType getWorkBucket(OperationResult result) throws StopHandlerExecutionException {
//        WorkBucketType bucket;
//        try {
//            bucket = beans.workStateManager.getWorkBucket(task.getOid(), FREE_BUCKET_WAIT_TIME, task::canRun, initialExecution,
//                    task.getWorkBucketStatisticsCollector(), result);
//        } catch (InterruptedException e) {
//            LOGGER.trace("InterruptedExecution in getWorkBucket for {}", task);
//            if (!task.canRun()) {
//                throw new StopHandlerExecutionException(createInterruptedTaskRunResult(task));
//            } else {
//                LoggingUtils.logUnexpectedException(LOGGER, "Unexpected InterruptedException in {}", e, task);
//                throw new StopHandlerExecutionException(task, "Unexpected InterruptedException: " + e.getMessage(), e);
//            }
//        } catch (Throwable t) {
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't allocate a work bucket for task {}", t, task);
//            throw new StopHandlerExecutionException(task, "Couldn't allocate a work bucket for task: " + t.getMessage(), t);
//        }
//        return bucket;
//    }
//
//    private void resetWorkStateAndStatisticsIfWorkComplete(OperationResult result) throws StopHandlerExecutionException {
//        if (isAllWorkComplete()) {
//            LOGGER.debug("Work is marked as complete; restarting it in task {}", task);
//            try {
//                List<ItemDelta<?, ?>> itemDeltas = new ArrayList<>();
//                itemDeltas.add(beans.prismContext.deltaFor(TaskType.class)
//                        .item(TaskType.F_WORK_STATE).replace()
//                        .asItemDelta());
//
//                if (handler.getStatisticsCollectionStrategy().isStartFromZero()) {
//                    LOGGER.debug("Resetting all statistics in task {} on start", task);
//                    itemDeltas.addAll(
//                            beans.prismContext.deltaFor(TaskType.class)
//                                    .item(TaskType.F_PROGRESS).replace()
//                                    .item(TaskType.F_OPERATION_STATS).replace()
//                                    .asItemDeltas());
//                }
//                task.modify(itemDeltas);
//                task.flushPendingModifications(result);
//            } catch (Throwable t) {
//                throw new StopHandlerExecutionException(task, "Couldn't reset work state and/or statistics at start", t);
//            }
//        }
//    }
//
//    private boolean isAllWorkComplete() {
//        return task.getWorkState() != null && Boolean.TRUE.equals(task.getWorkState().isAllWorkComplete());
//    }
}
