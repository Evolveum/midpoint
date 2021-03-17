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
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.VersionPrecondition;
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
import com.evolveum.midpoint.util.backoff.BackoffComputer;
import com.evolveum.midpoint.util.backoff.ExponentialBackoffComputer;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil.findBucketByNumber;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Responsible for managing task work state.
 *
 * @author mederly
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
//    private static final String GET_WORK_BUCKET_RECLAIMED_NONE = "getWorkBucket.reclaimedNone";
//    private static final String GET_WORK_BUCKET_RECLAIMED_SOME = "getWorkBucket.reclaimedSome";
//    private static final String GET_WORK_BUCKET_RECLAIM_ABORTED = "getWorkBucket.reclaimAborted";
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
        private Task coordinatorTask;           // null for standalone worker tasks
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

        void registerConflictOccurred(long wastedTime) {
            conflictCount++;
            conflictWastedTime += wastedTime;
        }

        void registerWaitTime(long waitTime) {
            assert isGetOperation;
            bucketWaitCount++;
            bucketWaitTime += waitTime;
        }

        void registerReclaim(int count) {
            bucketsReclaimed += count;
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
     * @pre task is persistent and has work state management configured
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
        WorkSegmentationStrategy workStateStrategy = strategyFactory.createStrategy(workManagement);
        setOrUpdateEstimatedNumberOfBuckets(ctx.coordinatorTask, workStateStrategy, result);

waitForAvailableBucket:    // this cycle exits when something is found OR when a definite 'no more buckets' answer is received
        for (;;) {
            BackoffComputer backoffComputer = createBackoffComputer(workManagement);
            int retry = 0;
waitForConflictLessUpdate: // this cycle exits when coordinator task update succeeds
            for (;;) {
                long attemptStart = System.currentTimeMillis();
                TaskWorkStateType coordinatorWorkState = getWorkStateOrNew(ctx.coordinatorTask);
                GetBucketResult response = workStateStrategy.getBucket(coordinatorWorkState);
                LOGGER.trace("getWorkBucketMultiNode: workStateStrategy returned {} for worker task {}, coordinator {}", response, ctx.workerTask, ctx.coordinatorTask);
                try {
                    if (response instanceof NewBuckets) {
                        NewBuckets newBucketsResponse = (NewBuckets) response;
                        int selected = newBucketsResponse.selected;
                        List<WorkBucketType> newCoordinatorBuckets = new ArrayList<>(coordinatorWorkState.getBucket());
                        for (int i = 0; i < newBucketsResponse.newBuckets.size(); i++) {
                            if (i == selected) {
                                newCoordinatorBuckets.add(newBucketsResponse.newBuckets.get(i).clone()
                                        .state(WorkBucketStateType.DELEGATED)
                                        .workerRef(ctx.workerTask.getOid(), TaskType.COMPLEX_TYPE));
                            } else {
                                newCoordinatorBuckets.add(newBucketsResponse.newBuckets.get(i).clone());
                            }
                        }
                        repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
                                bucketsReplaceDeltas(newCoordinatorBuckets),
                                bucketsReplacePrecondition(coordinatorWorkState.getBucket()), null, result);
                        repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
                                bucketsAddDeltas(newBucketsResponse.newBuckets.subList(selected, selected+1)), null, result);
                        CONTENTION_LOGGER.trace("New bucket(s) acquired after {} ms (conflicts: {}) in {}", System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
                        ctx.register(GET_WORK_BUCKET_CREATED_NEW);
                        return newBucketsResponse.newBuckets.get(selected);
                    } else if (response instanceof FoundExisting) {
                        FoundExisting existingResponse = (FoundExisting) response;
                        repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
                                bucketStateChangeDeltas(existingResponse.bucket, WorkBucketStateType.DELEGATED, ctx.workerTask.getOid()),
                                bucketUnchangedPrecondition(existingResponse.bucket), null, result);
                        WorkBucketType foundBucket = existingResponse.bucket.clone();
                        repositoryService.modifyObject(TaskType.class, ctx.workerTask.getOid(),
                                bucketsAddDeltas(singletonList(foundBucket)), null, result);
                        CONTENTION_LOGGER.trace("Existing bucket acquired after {} ms (conflicts: {}) in {}", System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
                        ctx.register(GET_WORK_BUCKET_DELEGATED);
                        return foundBucket;
                    } else if (response instanceof NothingFound) {
                        if (!ctx.workerTask.isScavenger()) {
                            CONTENTION_LOGGER.trace("'No bucket' found (and not a scavenger) after {} ms (conflicts: {}) in {}", System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
                            ctx.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_NOT_SCAVENGER);
                            return null;
                        } else if (((NothingFound) response).definite || freeBucketWaitTime == 0L) {
                            markWorkComplete(ctx.coordinatorTask, result);       // TODO also if response is not definite?
                            CONTENTION_LOGGER.trace("'No bucket' found after {} ms (conflicts: {}) in {}", System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
                            ctx.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE);
                            return null;
                        } else {
                            long waitDeadline = freeBucketWaitTime >= 0 ? ctx.start + freeBucketWaitTime : Long.MAX_VALUE;
                            long toWait = waitDeadline - System.currentTimeMillis();
                            if (toWait <= 0) {
                                markWorkComplete(ctx.coordinatorTask, result);       // TODO also if response is not definite?
                                CONTENTION_LOGGER.trace("'No bucket' found (wait time elapsed) after {} ms (conflicts: {}) in {}", System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
                                ctx.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_WAIT_TIME_ELAPSED);
                                return null;
                            }
                            //System.out.println("*** No free work bucket -- waiting ***");
                            long waitStart = System.currentTimeMillis();
                            long sleepFor = Math.min(toWait, getFreeBucketWaitInterval(workManagement));
                            CONTENTION_LOGGER.trace("Entering waiting for free bucket (waiting for {}) - after {} ms (conflicts: {}) in {}",
                                    sleepFor, System.currentTimeMillis() - ctx.start, ctx.conflictCount, ctx.workerTask);
                            dynamicSleep(sleepFor, ctx);
                            ctx.registerWaitTime(System.currentTimeMillis() - waitStart);
                            ctx.reloadCoordinatorTask(result);
                            ctx.reloadWorkerTask(result);
                            reclaimWronglyAllocatedBuckets(ctx, result);
                            // we continue even if we could not find any wrongly allocated bucket -- maybe someone else found
                            // them before us
                            continue waitForAvailableBucket;
                        }
                    } else {
                        throw new AssertionError(response);
                    }
                } catch (PreconditionViolationException e) {
                    retry++;
                    long delay;
                    try {
                        delay = backoffComputer.computeDelay(retry);
                    } catch (BackoffComputer.NoMoreRetriesException e1) {
                        String message =
                                "Couldn't allocate work bucket because of repeated database conflicts (retry limit reached); coordinator task = "
                                        + ctx.coordinatorTask;
                        CONTENTION_LOGGER.error(message, e1);
                        throw new SystemException(message, e1);
                    }
                    String message = "getWorkBucketMultiNode: conflict; continuing as retry #{}; waiting {} ms in {}, worker {}";
                    Object[] objects = { retry, delay, ctx.coordinatorTask, ctx.workerTask, e };
                    CONTENTION_LOGGER.debug(message, objects);
                    LOGGER.info(message, objects);      // todo change to trace
                    dynamicSleep(delay, ctx);
                    ctx.reloadCoordinatorTask(result);
                    ctx.reloadWorkerTask(result);
                    ctx.registerConflictOccurred(System.currentTimeMillis() - attemptStart);
                    //noinspection UnnecessaryContinue,UnnecessaryLabelOnContinueStatement
                    continue waitForConflictLessUpdate;
                }
            }
        }
    }

    private BackoffComputer createBackoffComputer(TaskWorkManagementType workManagement) {
        WorkAllocationConfigurationType ac = workManagement != null && workManagement.getBuckets() != null ?
                workManagement.getBuckets().getAllocation() : null;
        TaskManagerConfiguration c = configuration;
        int workAllocationMaxRetries = ac != null && ac.getWorkAllocationMaxRetries() != null ?
                ac.getWorkAllocationMaxRetries() : c.getWorkAllocationMaxRetries();
        long workAllocationRetryIntervalBase = ac != null && ac.getWorkAllocationRetryIntervalBase() != null ?
                ac.getWorkAllocationRetryIntervalBase() : c.getWorkAllocationRetryIntervalBase();
        int workAllocationRetryExponentialThreshold = ac != null && ac.getWorkAllocationRetryExponentialThreshold() != null ?
                ac.getWorkAllocationRetryExponentialThreshold() : c.getWorkAllocationRetryExponentialThreshold();
        Long workAllocationRetryIntervalLimit = ac != null && ac.getWorkAllocationRetryIntervalLimit() != null ?
                ac.getWorkAllocationRetryIntervalLimit() : c.getWorkAllocationRetryIntervalLimit();
        return new ExponentialBackoffComputer(workAllocationMaxRetries, workAllocationRetryIntervalBase, workAllocationRetryExponentialThreshold, workAllocationRetryIntervalLimit);
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
            throws SchemaException, PreconditionViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
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
                    LOGGER.info("Reclaiming wrongly allocated work bucket {} from worker task {}", bucket, workerOid);
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
            // As for the precondition we use the whole task state (reflected by version). The reason is that if the work
            // state originally contains (wrongly) DELEGATED bucket plus e.g. last COMPLETE one, and this bucket is reclaimed
            // by two subtasks at once, each of them see the same state afterwards: DELEGATED + COMPLETE. The solution would
            // be either to enhance the delegated bucket with some information (like to whom it is delegated), or this one.
            // In the future we might go the former way; as it would make reclaiming much efficient - not requiring to read
            // the whole task tree.
            repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
                    bucketsReplaceDeltas(newState.getBucket()),
                    new VersionPrecondition<>(ctx.coordinatorTask.getVersion()), null, result);
            ctx.reloadCoordinatorTask(result);
            ctx.registerReclaim(reclaiming);
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
        LOGGER.trace("Completing work bucket {} in {} (coordinator {})", workerTaskOid, ctx.workerTask, ctx.coordinatorTask);
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
            repositoryService.modifyObject(TaskType.class, ctx.coordinatorTask.getOid(),
                    modifications, bucketUnchangedPrecondition(bucket), null, result);
        } catch (PreconditionViolationException e) {
            throw new IllegalStateException("Unexpected concurrent modification of work bucket " + bucket + " in " + ctx.coordinatorTask, e);
        }
        ((TaskQuartzImpl) ctx.coordinatorTask).applyModificationsTransient(modifications);
        compressCompletedBuckets(ctx.coordinatorTask, result);
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
        compressCompletedBuckets(ctx.workerTask, result);
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

    private void compressCompletedBuckets(Task task, OperationResult result)
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
            repositoryService.modifyObject(TaskType.class, task.getOid(), deleteItemDeltas, null, result);
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

    private ModificationPrecondition<TaskType> bucketsReplacePrecondition(List<WorkBucketType> originalBuckets) {
        // performance is not optimal but OK for precondition checking
        return taskObject -> cloneNoId(originalBuckets).equals(cloneNoId(getWorkStateOrNew(taskObject.asObjectable()).getBucket()));
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

    private List<WorkBucketType> cloneNoId(List<WorkBucketType> buckets) {
        return buckets.stream().map(this::cloneNoId)
                .collect(Collectors.toCollection(() -> new ArrayList<>(buckets.size())));
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
            // temporary info level logging
            LOGGER.info("executeInitialDelayForMultiNode: waiting {} ms in {}", delay, ctx.workerTask);
            dynamicSleep(delay, ctx.canRunSupplier);
        }
    }
}
