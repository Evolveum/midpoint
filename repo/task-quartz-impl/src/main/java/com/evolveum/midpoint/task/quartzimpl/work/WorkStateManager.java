/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.statistics.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy.GetBucketResult;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy.GetBucketResult.FoundExisting;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy.GetBucketResult.NewBuckets;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategy.GetBucketResult.NothingFound;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.WorkSegmentationStrategyFactory;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.content.WorkBucketContentHandler;
import com.evolveum.midpoint.task.quartzimpl.work.segmentation.content.WorkBucketContentHandlerRegistry;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil.findBucketByNumber;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Responsible for managing task work state.
 */
@Component
public class WorkStateManager {

    private static final Trace LOGGER = TraceManager.getTrace(WorkStateManager.class);
    private static final Trace CONTENTION_LOGGER = TraceManager.getTrace(TaskManagerQuartzImpl.CONTENTION_LOG_NAME);

    private static final String GET_WORK_BUCKET_FOUND_SELF_ALLOCATED = "getWorkBucket.foundSelfAllocated";
    private static final String GET_WORK_BUCKET_CREATED_NEW = "getWorkBucket.createdNew";
    private static final String GET_WORK_BUCKET_DELEGATED = "getWorkBucket.delegated";
    private static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE = "getWorkBucket.noMoreBucketsDefinite";
    private static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_NOT_SCAVENGER = "getWorkBucket.noMoreBucketsNotScavenger";
    private static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_WAIT_TIME_ELAPSED = "getWorkBucket.NoMoreBucketsWaitTimeElapsed";
    private static final String COMPLETE_WORK_BUCKET = "completeWorkBucket";
    private static final String RELEASE_WORK_BUCKET = "releaseWorkBucket";

    @Autowired private TaskManager taskManager;
    @Autowired private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private WorkSegmentationStrategyFactory strategyFactory;
    @Autowired private WorkBucketContentHandlerRegistry handlerFactory;
    @Autowired private TaskManagerConfiguration configuration;

    private static final long DYNAMIC_SLEEP_INTERVAL = 100L;

    private Long freeBucketWaitIntervalOverride = null;

    private class Context {
        private final long start = System.currentTimeMillis();
        private Task workerTask;
        private Task coordinatorTask; // null for standalone worker tasks
        private final Supplier<Boolean> canRunSupplier;
        private final WorkBucketStatisticsCollector collector;
        private final boolean isGetOperation;
        private int conflictCount = 0;
        private long conflictWastedTime = 0;
        private int bucketWaitCount = 0;
        private long bucketWaitTime = 0;
        private int bucketsReclaimed = 0;

        Context(Supplier<Boolean> canRunSupplier, WorkBucketStatisticsCollector collector, boolean isGetOperation) {
            this.canRunSupplier = canRunSupplier;
            this.collector = collector;
            this.isGetOperation = isGetOperation;
        }

        public boolean isStandalone() {
            if (workerTask.getWorkManagement() == null) {
                return true;
            }
            TaskKindType kind = workerTask.getWorkManagement().getTaskKind();
            return kind == null || kind == TaskKindType.STANDALONE;
        }

        void reloadCoordinatorTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
            coordinatorTask = taskManager.getTaskPlain(coordinatorTask.getOid(), null, result);
        }

        void reloadWorkerTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
            workerTask = taskManager.getTaskPlain(workerTask.getOid(), null, result);
        }

        TaskWorkManagementType getWorkStateConfiguration() {
            return isStandalone() ? workerTask.getWorkManagement() : coordinatorTask.getWorkManagement();
        }

        void register(String situation) {
            if (collector != null) {
                collector.register(situation, System.currentTimeMillis() - start,
                        conflictCount, conflictWastedTime, bucketWaitCount, bucketWaitTime, bucketsReclaimed);
            }
        }

        void registerWaitTime(long waitTime) {
            assert isGetOperation;
            bucketWaitCount++;
            bucketWaitTime += waitTime;
        }

        void registerReclaim(int count) {
            bucketsReclaimed += count;
        }

        void addToConflictCounts(ModifyObjectResult<TaskType> modifyObjectResult) {
            conflictCount += modifyObjectResult.getRetries();
            conflictWastedTime += modifyObjectResult.getWastedTime();
        }

        void setConflictCounts(ModifyObjectResult<TaskType> modifyObjectResult) {
            conflictCount = modifyObjectResult.getRetries();
            conflictWastedTime = modifyObjectResult.getWastedTime();
        }
    }

    public boolean canRun(Supplier<Boolean> canRunSupplier) {
        return canRunSupplier == null || BooleanUtils.isTrue(canRunSupplier.get());
    }

    public WorkBucketType getWorkBucket(@NotNull String workerTaskOid, long freeBucketWaitTime,
            Supplier<Boolean> canRun, WorkBucketStatisticsCollector statisticsCollector, @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
        return getWorkBucket(workerTaskOid, freeBucketWaitTime, canRun, false, statisticsCollector, result);
    }

    /**
     * Allocates work bucket. If no free work buckets are currently present it tries to create one.
     * If there is already allocated work bucket for given worker task, it is returned.
     *
     * Finding/creation of free bucket is delegated to the work state management strategy.
     * This method implements mainly the act of allocation - i.e. modification of the task work state in repository.
     *
     * WE ASSUME THIS METHOD IS CALLED FROM THE WORKER TASK; SO IT IS NOT NECESSARY TO SYNCHRONIZE ACCESS TO THIS TASK WORK STATE.
     *
     * PRECONDITION: task is persistent and has work state management configured
     */
    public WorkBucketType getWorkBucket(@NotNull String workerTaskOid, long freeBucketWaitTime,
            Supplier<Boolean> canRun, boolean executeInitialWait, @Nullable WorkBucketStatisticsCollector collector,
            @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
        Context ctx = createContext(workerTaskOid, canRun, collector, true, result);
        try {
            WorkBucketType bucket = findSelfAllocatedBucket(ctx);
            if (bucket != null) {
                ctx.register(GET_WORK_BUCKET_FOUND_SELF_ALLOCATED);
                LOGGER.trace("Returning self-allocated bucket for {}: {}", workerTaskOid, bucket);
                return bucket;
            }
            if (ctx.isStandalone()) {
                return getWorkBucketStandalone(ctx, result);
            } else {
                if (executeInitialWait) {
                    executeInitialDelayForMultiNode(ctx);
                }
                return getWorkBucketMultiNode(ctx, freeBucketWaitTime, result);
            }
        } catch (Throwable t) {
            ctx.register("getWorkBucket." + t.getClass().getSimpleName());
            throw t;
        }
    }

    private WorkBucketType findSelfAllocatedBucket(Context ctx) {
        TaskWorkStateType workState = ctx.workerTask.getWorkState();
        if (workState == null || workState.getBucket().isEmpty()) {
            return null;
        }
        List<WorkBucketType> buckets = new ArrayList<>(workState.getBucket());
        TaskWorkStateUtil.sortBucketsBySequentialNumber(buckets);
        for (WorkBucketType bucket : buckets) {
            if (bucket.getState() == WorkBucketStateType.READY) {
                return bucket;
            }
        }
        return null;
    }

    private WorkBucketType getWorkBucketMultiNode(Context ctx, long freeBucketWaitTime, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, InterruptedException {
        TaskWorkManagementType workManagement = ctx.coordinatorTask.getWorkManagement();
        WorkSegmentationStrategy segmentationStrategy = strategyFactory.createStrategy(workManagement);
        setOrUpdateEstimatedNumberOfBuckets(ctx.coordinatorTask, segmentationStrategy, result);

        for (;;) {

            Holder<GetBucketResult> lastGetBucketResultHolder = new Holder<>();
            ModifyObjectResult<TaskType> modifyResult = repositoryService.modifyObjectDynamically(TaskType.class,
                    ctx.coordinatorTask.getOid(), null,
                    task -> computeWorkBucketModifications(task, ctx, segmentationStrategy, lastGetBucketResultHolder),
                    null, result);

            // We ignore conflicts encountered in previous iterations (scavenger hitting 'no more buckets' situation).
            ctx.setConflictCounts(modifyResult);

            GetBucketResult getBucketResult =
                    Objects.requireNonNull(lastGetBucketResultHolder.getValue(), "no last getBucket result");

            if (getBucketResult instanceof NewBuckets) {
                return recordNewBucketInWorkerTask(ctx, (NewBuckets) getBucketResult, result);
            } if (getBucketResult instanceof FoundExisting) {
                return recordExistingBucketInWorkerTask(ctx, (FoundExisting) getBucketResult, result);
            } else if (getBucketResult instanceof NothingFound) {
                if (!ctx.workerTask.isScavenger()) {
                    processNothingFoundForNonScavenger(ctx);
                    return null;
                } else if (((NothingFound) getBucketResult).definite || freeBucketWaitTime == 0L) {
                    processNothingFoundDefinite(ctx, result);
                    return null;
                } else {
                    long toWait = getRemainingTimeToWait(ctx, freeBucketWaitTime);
                    if (toWait <= 0) {
                        processNothingFoundButWaitTimeElapsed(ctx, result);
                        return null;
                    } else {
                        wait(toWait, ctx, workManagement);
                        reclaimWronglyAllocatedBuckets(ctx, result);
                        // We continue even if we could not find any wrongly allocated
                        // bucket -- maybe someone else found them before us, so we could use them.
                    }
                }
            } else {
                throw new AssertionError(getBucketResult);
            }
        }
    }

    private void wait(long toWait, Context ctx, TaskWorkManagementType workManagement) throws InterruptedException {
        long waitStart = System.currentTimeMillis();
        long sleepFor = Math.min(toWait, getFreeBucketWaitInterval(workManagement));
        CONTENTION_LOGGER.trace("Entering waiting for free bucket (waiting for {}) - after {} ms (conflicts: {}) in {}",
                sleepFor, System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
        dynamicSleep(sleepFor, ctx);
        ctx.registerWaitTime(System.currentTimeMillis() - waitStart);
    }

    private void processNothingFoundButWaitTimeElapsed(Context ctx, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        markWorkComplete(ctx.coordinatorTask, result); // TODO also if response is not definite?
        CONTENTION_LOGGER.trace("'No bucket' found (wait time elapsed) after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
        ctx.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_WAIT_TIME_ELAPSED);
    }

    private long getRemainingTimeToWait(Context ctx, long freeBucketWaitTime) {
        long waitDeadline = freeBucketWaitTime >= 0 ? ctx.start + freeBucketWaitTime : Long.MAX_VALUE;
        return waitDeadline - System.currentTimeMillis();
    }

    private void processNothingFoundDefinite(Context ctx, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        markWorkComplete(ctx.coordinatorTask, result);
        CONTENTION_LOGGER.trace("'No bucket' found after {} ms (conflicts: {}) in {}", System.currentTimeMillis() - ctx.start,
                ctx.conflictCount, ctx.workerTask);
        ctx.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE);
    }

    private WorkBucketType recordNewBucketInWorkerTask(Context ctx, NewBuckets newBucketsResult, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        WorkBucketType selectedBucket = newBucketsResult.newBuckets.get(newBucketsResult.selected).clone();
        repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
                bucketsAddDeltas(singletonList(selectedBucket)), null, result);
        CONTENTION_LOGGER.info("New bucket(s) acquired after {} ms (retries: {}) in {}",
                System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
        ctx.register(GET_WORK_BUCKET_CREATED_NEW);
        return selectedBucket;
    }

    private WorkBucketType recordExistingBucketInWorkerTask(Context ctx, FoundExisting foundExistingResult,
            OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        WorkBucketType foundBucket = foundExistingResult.bucket.clone();
        repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
                bucketsAddDeltas(singletonList(foundBucket)), null, result);
        CONTENTION_LOGGER.trace("Existing bucket acquired after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
        ctx.register(GET_WORK_BUCKET_DELEGATED);
        return foundBucket;
    }

    private void processNothingFoundForNonScavenger(Context ctx) {
        CONTENTION_LOGGER.trace("'No bucket' found (and not a scavenger) after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
        ctx.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_NOT_SCAVENGER);
    }

    /**
     * Computes modifications to the current work state aimed to obtain new buckets, if possible.
     */
    private Collection<? extends ItemDelta<?, ?>> computeWorkBucketModifications(TaskType coordinatorTask, Context ctx,
            WorkSegmentationStrategy segmentationStrategy, Holder<GetBucketResult> lastGetBucketResultHolder) throws SchemaException {

        TaskWorkStateType workState = getWorkStateOrNew(coordinatorTask);
        GetBucketResult getBucketResult = segmentationStrategy.getBucket(workState);
        lastGetBucketResultHolder.setValue(getBucketResult);
        LOGGER.trace("computeWorkBucketModifications: segmentationStrategy returned {} for worker task {}, coordinator {}",
                getBucketResult, ctx.workerTask, ctx.coordinatorTask);
        if (getBucketResult instanceof NewBuckets) {
            return computeModificationsForNewBuckets(ctx, workState, (NewBuckets) getBucketResult);
        } else if (getBucketResult instanceof FoundExisting) {
            return computeModificationsForExistingBucket(ctx, (FoundExisting) getBucketResult);
        } else if (getBucketResult instanceof NothingFound) {
            return emptyList(); // Nothing to do for now.
        } else {
            throw new AssertionError(getBucketResult);
        }
    }

    private Collection<ItemDelta<?, ?>> computeModificationsForNewBuckets(Context ctx, TaskWorkStateType workState,
            NewBuckets response) throws SchemaException {
        List<WorkBucketType> newCoordinatorBuckets = new ArrayList<>(workState.getBucket());
        for (int i = 0; i < response.newBuckets.size(); i++) {
            if (i == response.selected) {
                newCoordinatorBuckets.add(response.newBuckets.get(i).clone()
                        .state(WorkBucketStateType.DELEGATED)
                        .workerRef(ctx.workerTask.getOid(), TaskType.COMPLEX_TYPE));
            } else {
                newCoordinatorBuckets.add(response.newBuckets.get(i).clone());
            }
        }
        return bucketsReplaceDeltas(newCoordinatorBuckets);
    }

    private Collection<ItemDelta<?, ?>> computeModificationsForExistingBucket(Context ctx, FoundExisting response)
            throws SchemaException {
        return bucketStateChangeDeltas(response.bucket, WorkBucketStateType.DELEGATED, ctx.workerTask.getOid());
    }

    private long getFreeBucketWaitInterval(TaskWorkManagementType workManagement) {
        WorkAllocationConfigurationType ac = workManagement != null && workManagement.getBuckets() != null ?
                workManagement.getBuckets().getAllocation() : null;
        if (ac != null && ac.getWorkAllocationFreeBucketWaitInterval() != null) {
            return ac.getWorkAllocationFreeBucketWaitInterval();
        } else {
            return freeBucketWaitIntervalOverride != null ? freeBucketWaitIntervalOverride :
                    configuration.getWorkAllocationDefaultFreeBucketWaitInterval();
        }
    }

    private long getInitialDelay(TaskWorkManagementType workManagement) {
        WorkAllocationConfigurationType ac = workManagement != null && workManagement.getBuckets() != null ?
                workManagement.getBuckets().getAllocation() : null;
        return ac != null && ac.getWorkAllocationInitialDelay() != null ?
                ac.getWorkAllocationInitialDelay() : configuration.getWorkAllocationInitialDelay();
    }

    private void setOrUpdateEstimatedNumberOfBuckets(Task task, WorkSegmentationStrategy workStateStrategy, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        Integer number = workStateStrategy.estimateNumberOfBuckets(task.getWorkState());
        if (number != null && (task.getWorkState() == null || !number.equals(task.getWorkState().getNumberOfBuckets()))) {
            List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                    .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_NUMBER_OF_BUCKETS).replace(number)
                    .asItemDeltas();
            repositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, result);
        }
    }

    private void dynamicSleep(long delay, Context ctx) throws InterruptedException {
        dynamicSleep(delay, ctx.canRunSupplier);
    }

    private void dynamicSleep(long delay, Supplier<Boolean> canRunSupplier) throws InterruptedException {
        while (delay > 0) {
            if (!canRun(canRunSupplier)) {
                throw new InterruptedException();
            }
            Thread.sleep(Math.min(delay, DYNAMIC_SLEEP_INTERVAL));
            delay -= DYNAMIC_SLEEP_INTERVAL;
        }
    }

    /**
     * For each allocated work bucket we check if it is allocated to existing and non-closed child task.
     * Returns true if there was something to reclaim.
     */
    private void reclaimWronglyAllocatedBuckets(Context ctx, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ctx.reloadCoordinatorTask(result);
        ctx.reloadWorkerTask(result);
        if (ctx.coordinatorTask.getWorkState() == null) {
            return;
        }
        TaskWorkStateType newState = ctx.coordinatorTask.getWorkState().clone();
        int reclaiming = 0;
        Set<String> deadWorkers = new HashSet<>();
        Set<String> liveWorkers = new HashSet<>();
        for (WorkBucketType bucket : newState.getBucket()) {
            if (bucket.getState() == WorkBucketStateType.DELEGATED) {
                String workerOid = bucket.getWorkerRef() != null ? bucket.getWorkerRef().getOid() : null;
                if (isDead(workerOid, deadWorkers, liveWorkers, result)) {
                    LOGGER.info("Will reclaim wrongly allocated work bucket {} from worker task {}", bucket, workerOid);
                    bucket.setState(WorkBucketStateType.READY);
                    bucket.setWorkerRef(null);
                    // TODO modify also the worker if it exists (maybe)
                    reclaiming++;
                }
            }
        }
        LOGGER.trace("Reclaiming wrongly allocated buckets found {} buckets to reclaim in {}", reclaiming, ctx.coordinatorTask);
        if (reclaiming > 0) {
            CONTENTION_LOGGER.debug("Reclaiming wrongly allocated buckets found {} buckets to reclaim in {}", reclaiming, ctx.coordinatorTask);
            try {
                // As for the precondition we use the whole task state (reflected by version). The reason is that if the work
                // state originally contains (wrongly) DELEGATED bucket plus e.g. last COMPLETE one, and this bucket is reclaimed
                // by two subtasks at once, each of them see the same state afterwards: READY + COMPLETE.
                repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
                        bucketsReplaceDeltas(newState.getBucket()),
                        new VersionPrecondition<>(ctx.coordinatorTask.getVersion()), null, result);
                ctx.registerReclaim(reclaiming);
            } catch (PreconditionViolationException e) {
                LOGGER.info("Concurrent modification of work state in {}. {} wrongly allocated bucket(s) will "
                        + "not be reclaimed in this run.", ctx.coordinatorTask, reclaiming);
            }
            ctx.reloadCoordinatorTask(result);
        }
    }

    private boolean isDead(String workerOid, Set<String> deadWorkers, Set<String> liveWorkers, OperationResult result) {
        if (workerOid == null || deadWorkers.contains(workerOid)) {
            return true;
        } else if (liveWorkers.contains(workerOid)) {
            return false;
        } else {
            boolean isDead;
            try {
                PrismObject<TaskType> worker = repositoryService.getObject(TaskType.class, workerOid, null, result);
                isDead = worker.asObjectable().getSchedulingState() == TaskSchedulingStateType.CLOSED;
            } catch (ObjectNotFoundException e) {
                isDead = true;
            } catch (SchemaException e) {
                LOGGER.warn("Couldn't fetch worker from repo {} because of schema exception -- assume it's dead", workerOid, e);
                isDead = true;
            }
            if (isDead) {
                deadWorkers.add(workerOid);
                return true;
            } else {
                liveWorkers.add(workerOid);
                return false;
            }
        }
    }

    private WorkBucketType getWorkBucketStandalone(Context ctx, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        WorkSegmentationStrategy workStateStrategy = strategyFactory.createStrategy(ctx.workerTask.getWorkManagement());
        setOrUpdateEstimatedNumberOfBuckets(ctx.workerTask, workStateStrategy, result);
        TaskWorkStateType workState = getWorkStateOrNew(ctx.workerTask);
        GetBucketResult response = workStateStrategy.getBucket(workState);
        LOGGER.trace("getWorkBucketStandalone: workStateStrategy returned {} for standalone task {}", response, ctx.workerTask);
        if (response instanceof FoundExisting) {
            throw new AssertionError("Found unallocated buckets in standalone worker task on a second pass: " + ctx.workerTask);
        } else if (response instanceof NewBuckets) {
            NewBuckets newBucketsResponse = (NewBuckets) response;
            repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
                    bucketsAddDeltas(newBucketsResponse.newBuckets), null, result);
            ctx.register(GET_WORK_BUCKET_CREATED_NEW);
            return newBucketsResponse.newBuckets.get(newBucketsResponse.selected);
        } else if (response instanceof NothingFound) {
            if (!((NothingFound) response).definite) {
                throw new AssertionError("Unexpected 'indefinite' answer when looking for next bucket in a standalone task: " + ctx.workerTask);
            }
            ctx.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE);
            markWorkComplete(ctx.workerTask, result);
            return null;
        } else {
            throw new AssertionError(response);
        }
    }

    private void markWorkComplete(Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_ALL_WORK_COMPLETE).replace(true)
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, result);
    }

    private Context createContext(String workerTaskOid, Supplier<Boolean> canRun,
            WorkBucketStatisticsCollector collector, boolean isGetOperation, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Context ctx = new Context(canRun, collector, isGetOperation);
        ctx.workerTask = taskManager.getTaskPlain(workerTaskOid, result);
        TaskWorkManagementType wsConfig = ctx.workerTask.getWorkManagement();
        if (wsConfig != null && wsConfig.getTaskKind() != null && wsConfig.getTaskKind() != TaskKindType.WORKER &&
                wsConfig.getTaskKind() != TaskKindType.STANDALONE) {
            throw new IllegalStateException("Wrong task kind for worker task " + ctx.workerTask + ": " + wsConfig.getTaskKind());
        }
        if (wsConfig != null && wsConfig.getTaskKind() == TaskKindType.WORKER) {
            ctx.coordinatorTask = getCoordinatorTask(ctx.workerTask, result);
        }
        return ctx;
    }

    private Task getCoordinatorTask(Task workerTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Task parent = workerTask.getParentTask(result);
        if (parent == null) {
            throw new IllegalStateException("No coordinator task for worker task " + workerTask);
        }
        TaskWorkManagementType wsConfig = parent.getWorkManagement();
        if (wsConfig == null || wsConfig.getTaskKind() != TaskKindType.COORDINATOR) {
            throw new IllegalStateException("Coordinator task for worker task " + workerTask + " is not marked as such: " + parent);
        }
        return parent;
    }

    public void completeWorkBucket(String workerTaskOid, int sequentialNumber,
            WorkBucketStatisticsCollector workBucketStatisticsCollector, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        Context ctx = createContext(workerTaskOid, null, workBucketStatisticsCollector, false, result);
        LOGGER.trace("Completing work bucket #{} in {} (coordinator {})", sequentialNumber, ctx.workerTask, ctx.coordinatorTask);
        if (ctx.isStandalone()) {
            completeWorkBucketStandalone(ctx, sequentialNumber, result);
        } else {
            completeWorkBucketMultiNode(ctx, sequentialNumber, result);
        }
    }

    private void completeWorkBucketMultiNode(Context ctx, int sequentialNumber, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        TaskWorkStateType workState = getWorkState(ctx.coordinatorTask);
        WorkBucketType bucket = TaskWorkStateUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
        if (bucket == null) {
            throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + ctx.coordinatorTask);
        }
        if (bucket.getState() != WorkBucketStateType.DELEGATED) {
            throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + ctx.coordinatorTask
                    + " cannot be marked as complete, as it is not delegated; its state = " + bucket.getState());
        }
        checkWorkerRefOnDelegatedBucket(ctx, bucket);
        Collection<ItemDelta<?, ?>> modifications = bucketStateChangeDeltas(bucket, WorkBucketStateType.COMPLETE);
        try {
            ModifyObjectResult<TaskType> modifyObjectResult = repositoryService.modifyObject(TaskType.class,
                    ctx.coordinatorTask.getOid(), modifications, bucketUnchangedPrecondition(bucket), null, result);
            ctx.addToConflictCounts(modifyObjectResult);
        } catch (PreconditionViolationException e) {
            throw new IllegalStateException("Unexpected concurrent modification of work bucket " + bucket + " in " + ctx.coordinatorTask, e);
        }
        ((TaskQuartzImpl) ctx.coordinatorTask).applyModificationsTransient(modifications);
        compressCompletedBuckets(ctx.coordinatorTask, ctx, result);
        deleteBucketFromWorker(ctx, sequentialNumber, result);
        ctx.register(COMPLETE_WORK_BUCKET);
    }

    private void deleteBucketFromWorker(Context ctx, int sequentialNumber, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ObjectAlreadyExistsException {
        TaskWorkStateType workerWorkState = getWorkState(ctx.workerTask);
        WorkBucketType workerBucket = TaskWorkStateUtil.findBucketByNumber(workerWorkState.getBucket(), sequentialNumber);
        if (workerBucket == null) {
            //LOGGER.warn("No work bucket with sequential number of " + sequentialNumber + " in worker task " + ctx.workerTask);
            //return;
            // just during testing
            throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in worker task " + ctx.workerTask);
        }
        repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(), bucketDeleteDeltas(workerBucket), result);
    }

    private void completeWorkBucketStandalone(Context ctx, int sequentialNumber, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        TaskWorkStateType workState = getWorkState(ctx.workerTask);
        WorkBucketType bucket = TaskWorkStateUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
        if (bucket == null) {
            throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + ctx.workerTask);
        }
        if (bucket.getState() != WorkBucketStateType.READY && bucket.getState() != null) {
            throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + ctx.workerTask
                    + " cannot be marked as complete, as it is not ready; its state = " + bucket.getState());
        }
        Collection<ItemDelta<?, ?>> modifications = bucketStateChangeDeltas(bucket, WorkBucketStateType.COMPLETE);
        repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(), modifications, null, result);
        ((TaskQuartzImpl) ctx.workerTask).applyModificationsTransient(modifications);
        ((TaskQuartzImpl) ctx.workerTask).applyDeltasImmediate(modifications, result);
        compressCompletedBuckets(ctx.workerTask, ctx, result);
        ctx.register(COMPLETE_WORK_BUCKET);
    }

    /**
     * Releases work bucket.
     * Should be called from the worker task.
     */
    public void releaseWorkBucket(String workerTaskOid, int sequentialNumber, WorkBucketStatisticsCollector statisticsCollector,
            OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        Context ctx = createContext(workerTaskOid, null, statisticsCollector, false, result);
        LOGGER.trace("Releasing bucket {} in {} (coordinator {})", sequentialNumber, ctx.workerTask, ctx.coordinatorTask);
        if (ctx.isStandalone()) {
            throw new UnsupportedOperationException("Cannot release work bucket from standalone task " + ctx.workerTask);
        } else {
            releaseWorkBucketMultiNode(ctx, sequentialNumber, result);
        }
    }

    private void releaseWorkBucketMultiNode(Context ctx, int sequentialNumber, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        TaskWorkStateType workState = getWorkState(ctx.coordinatorTask);
        WorkBucketType bucket = TaskWorkStateUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
        if (bucket == null) {
            throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + ctx.coordinatorTask);
        }
        if (bucket.getState() != WorkBucketStateType.DELEGATED) {
            throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + ctx.coordinatorTask
                    + " cannot be released, as it is not delegated; its state = " + bucket.getState());
        }
        checkWorkerRefOnDelegatedBucket(ctx, bucket);
        try {
            repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
                    bucketStateChangeDeltas(bucket, WorkBucketStateType.READY, null),
                    bucketUnchangedPrecondition(bucket), null, result);
        } catch (PreconditionViolationException e) {
            // just for sure
            throw new IllegalStateException("Unexpected concurrent modification of work bucket " + bucket + " in " + ctx.coordinatorTask, e);
        }
        deleteBucketFromWorker(ctx, sequentialNumber, result);
        ctx.register(RELEASE_WORK_BUCKET);
    }

    private void checkWorkerRefOnDelegatedBucket(Context ctx, WorkBucketType bucket) {
        if (bucket.getWorkerRef() == null) {
            LOGGER.warn("DELEGATED bucket without workerRef: {}", bucket);
        } else if (!ctx.workerTask.getOid().equals(bucket.getWorkerRef().getOid())) {
            LOGGER.warn("DELEGATED bucket with workerRef ({}) different from the current worker task ({}): {}",
                    bucket.getWorkerRef().getOid(), ctx.workerTask, bucket);
        }
    }

    private void compressCompletedBuckets(Task task, Context ctx, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        List<WorkBucketType> buckets = new ArrayList<>(getWorkState(task).getBucket());
        TaskWorkStateUtil.sortBucketsBySequentialNumber(buckets);
        List<WorkBucketType> completeBuckets = buckets.stream()
                .filter(b -> b.getState() == WorkBucketStateType.COMPLETE)
                .collect(Collectors.toList());
        if (completeBuckets.size() <= 1) {
            LOGGER.trace("Compression of completed buckets: # of complete buckets is too small ({}) in {}, exiting",
                    completeBuckets.size(), task);
            return;
        }

        List<ItemDelta<?, ?>> deleteItemDeltas = new ArrayList<>();
        for (int i = 0; i < completeBuckets.size() - 1; i++) {
            deleteItemDeltas.addAll(bucketDeleteDeltas(completeBuckets.get(i)));
        }
        LOGGER.trace("Compression of completed buckets: deleting {} buckets before last completed one in {}", deleteItemDeltas.size(), task);
        // these buckets should not be touched by anyone (as they are already completed); so we can execute without preconditions
        if (!deleteItemDeltas.isEmpty()) {
            ModifyObjectResult<TaskType> modifyObjectResult =
                    repositoryService.modifyObject(TaskType.class, task.getOid(), deleteItemDeltas, null, result);
            ctx.addToConflictCounts(modifyObjectResult);
        }
    }

    private Collection<ItemDelta<?, ?>> bucketsReplaceDeltas(List<WorkBucketType> buckets) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET)
                .replaceRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
    }

    private Collection<ItemDelta<?, ?>> bucketsAddDeltas(List<WorkBucketType> buckets) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET)
                .addRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
    }

    @SuppressWarnings("SameParameterValue")
    private Collection<ItemDelta<?, ?>> bucketStateChangeDeltas(WorkBucketType bucket, WorkBucketStateType newState) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET, bucket.getId(), WorkBucketType.F_STATE)
                .replace(newState).asItemDeltas();
    }

    private Collection<ItemDelta<?, ?>> bucketStateChangeDeltas(WorkBucketType bucket, WorkBucketStateType newState,
            String workerOid) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET, bucket.getId(), WorkBucketType.F_STATE)
                    .replace(newState)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET, bucket.getId(), WorkBucketType.F_WORKER_REF)
                    .replaceRealValues(workerOid != null ? singletonList(new ObjectReferenceType().oid(workerOid).type(TaskType.COMPLEX_TYPE)) : emptyList())
                .asItemDeltas();
    }

    private Collection<ItemDelta<?, ?>> bucketDeleteDeltas(WorkBucketType bucket) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET)
                .delete(bucket.clone()).asItemDeltas();
    }

    private ModificationPrecondition<TaskType> bucketUnchangedPrecondition(WorkBucketType originalBucket) {
        return taskObject -> {
            WorkBucketType currentBucket = findBucketByNumber(getWorkStateOrNew(taskObject.asObjectable()).getBucket(),
                    originalBucket.getSequentialNumber());
            // performance is not optimal but OK for precondition checking
            return currentBucket != null && cloneNoId(currentBucket).equals(cloneNoId(originalBucket));
        };
    }

    private WorkBucketType cloneNoId(WorkBucketType bucket) {
        return bucket.clone().id(null);
    }

    @NotNull
    private TaskWorkStateType getWorkStateOrNew(Task task) {
        if (task.getWorkState() != null) {
            return task.getWorkState();
        } else {
            return new TaskWorkStateType(prismContext);
        }
    }

    @NotNull
    private TaskWorkStateType getWorkStateOrNew(TaskType task) {
        if (task.getWorkState() != null) {
            return task.getWorkState();
        } else {
            return new TaskWorkStateType(prismContext);
        }
    }

    @NotNull
    private TaskWorkStateType getWorkState(Task task) throws SchemaException {
        if (task.getWorkState() != null) {
            return task.getWorkState();
        } else {
            throw new SchemaException("No work state in task " + task);
        }
    }

    public void setFreeBucketWaitIntervalOverride(Long value) {
        this.freeBucketWaitIntervalOverride = value;
    }

    // TODO
    public ObjectQuery narrowQueryForWorkBucket(Task workerTask, ObjectQuery query, Class<? extends ObjectType> type,
            Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider,
            WorkBucketType workBucket, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Context ctx = createContext(workerTask.getOid(), () -> true, null, false, result);

        TaskWorkManagementType config = ctx.getWorkStateConfiguration();
        AbstractWorkSegmentationType bucketsConfig = TaskWorkStateUtil.getWorkSegmentationConfiguration(config);
        WorkBucketContentHandler handler = handlerFactory.getHandler(workBucket.getContent());
        List<ObjectFilter> conjunctionMembers = new ArrayList<>(
                handler.createSpecificFilters(workBucket, bucketsConfig, type, itemDefinitionProvider));

        // TODO update sorting criteria?
        return ObjectQueryUtil.addConjunctions(query, prismContext, conjunctionMembers);
    }

    private void executeInitialDelayForMultiNode(Context ctx) throws InterruptedException {
        long delay = (long) (Math.random() * getInitialDelay(ctx.coordinatorTask.getWorkManagement()));
        if (delay != 0) {
            LOGGER.debug("executeInitialDelayForMultiNode: waiting {} ms in {}", delay, ctx.workerTask);
            dynamicSleep(delay, ctx.canRunSupplier);
        }
    }
}
