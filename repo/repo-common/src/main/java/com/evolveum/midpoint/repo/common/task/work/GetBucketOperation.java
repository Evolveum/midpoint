/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import static java.util.Objects.requireNonNullElseGet;

import static com.evolveum.midpoint.schema.util.task.BucketingUtil.getBuckets;
import static com.evolveum.midpoint.schema.util.task.BucketingUtil.getWorkerOid;
import static com.evolveum.midpoint.schema.util.task.work.BucketingConstants.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketingStateType.F_SCAVENGING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketingStateType.F_WORK_COMPLETE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType.F_BUCKETING;

import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.common.activity.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator.Response.FoundExisting;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator.Response.NewBuckets;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator.Response.NothingFound;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Implements "get bucket" operation.
 */
public class GetBucketOperation extends BucketOperation {

    private static final Trace LOGGER = TraceManager.getTrace(GetBucketOperation.class);

    private static final long DYNAMIC_SLEEP_INTERVAL = 100L;

    @NotNull private final GetBucketOperationOptions options;

    /**
     * Generates new buckets under configuration provided by options.
     */
    private BucketAllocator bucketAllocator;

    GetBucketOperation(@NotNull String coordinatorTaskOid, @Nullable String workerTaskOid, @NotNull ActivityPath activityPath,
            ActivityBucketManagementStatistics statisticsCollector,
            @Nullable GetBucketOperationOptions options, CommonTaskBeans beans) {
        super(coordinatorTaskOid, workerTaskOid, activityPath, statisticsCollector, beans);
        this.options = requireNonNullElseGet(options, GetBucketOperationOptions::standard);
    }

    /**
     * @return Bucket that should be processed (or null if there's none). Note that the state of the bucket
     * is not relevant; it may be READY or DELEGATED.
     */
    public WorkBucketType execute(OperationResult result) throws SchemaException, ObjectNotFoundException,
            ObjectAlreadyExistsException, InterruptedException {

        bucketAllocator = BucketAllocator.create(
                options.getDistributionDefinition(),
                options.getImplicitSegmentationResolver(),
                beans);

        try {
            if (isStandalone()) {
                return getBucketStandalone(result);
            } else {
                executeInitialDelayForMultiNode();
                return getBucketMultiNode(result);
            }
        } catch (Throwable t) {
            statisticsKeeper.register("getWorkBucket." + t.getClass().getSimpleName());
            throw t;
        }
    }

    private WorkBucketType getBucketStandalone(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        TaskType coordinator = plainRepositoryService.getObject(TaskType.class, coordinatorTaskOid, null, result)
                .asObjectable();

        GetBucketOperationAttempt attempt =
                new GetBucketOperationAttempt(coordinator, workerTaskOid, activityPath, bucketAllocator);

        attempt.execute();

        assert attempt.getAlreadyDelegatedBucket() == null;

        BucketAllocator.Response response = Objects.requireNonNull(
                attempt.getAllocatorResponse(), "no allocator response");

        plainRepositoryService.modifyObject(TaskType.class, coordinatorTaskOid, attempt.getModifications(), result);

        if (response instanceof NewBuckets) {
            return onNewBucket((NewBuckets) response);
        } if (response instanceof FoundExisting) {
            return onFoundExisting((FoundExisting) response);
        } else if (response instanceof NothingFound) {
            if (((NothingFound) response).definite) {
                onNothingFoundDefinite(result);
                return null;
            } else {
                throw new AssertionError("Unexpected 'indefinite' answer when looking for next bucket in a standalone task: " + coordinator);
            }
        } else {
            throw new AssertionError(response);
        }
    }

    private WorkBucketType getBucketMultiNode(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, InterruptedException {

        for (;;) {

            Holder<GetBucketOperationAttempt> lastAttemptHolder = new Holder<>();
            ModifyObjectResult<TaskType> modifyResult = plainRepositoryService.modifyObjectDynamically(TaskType.class,
                    coordinatorTaskOid, null,
                    coordinatorTask -> {
                        GetBucketOperationAttempt attempt =
                                new GetBucketOperationAttempt(coordinatorTask, workerTaskOid, activityPath, bucketAllocator);
                        lastAttemptHolder.setValue(attempt);
                        attempt.execute();
                        return attempt.getModifications();
                    }, null, result);

            // Let us record the conflicts encountered. Note that we ignore conflicts encountered in previous iterations
            // of the outer "for" cycle, i.e. when a scavenger hits "no more buckets" situation.
            statisticsKeeper.setConflictCounts(modifyResult);

            GetBucketOperationAttempt lastAttempt =
                    Objects.requireNonNull(lastAttemptHolder.getValue(), "no last attempt recorded");

            WorkBucketType alreadyDelegated = lastAttempt.getAlreadyDelegatedBucket();
            if (alreadyDelegated != null) {
                statisticsKeeper.register(GET_WORK_BUCKET_FOUND_ALREADY_DELEGATED);
                LOGGER.trace("Returning already delegated bucket for {}: {}", workerTaskOid, alreadyDelegated);
                return alreadyDelegated;
            }

            BucketAllocator.Response response = Objects.requireNonNull(
                    lastAttempt.getAllocatorResponse(), "no allocator response");

            if (response instanceof NewBuckets) {
                return onNewBucket((NewBuckets) response);
            } if (response instanceof FoundExisting) {
                return onFoundExisting((FoundExisting) response);
            } else if (response instanceof NothingFound) {
                if (!options.isScavenger()) {
                    onNothingFoundForNonScavenger(result);
                    return null;
                } else if (((NothingFound) response).definite || options.getFreeBucketWaitTime() == 0L) {
                    onNothingFoundDefinite(result);
                    return null;
                } else {
                    long toWait = getRemainingTimeToWait();
                    if (toWait <= 0) {
                        onNothingFoundWithWaitTimeElapsed(result);
                        return null;
                    } else {
                        sleep(toWait);
                        reclaimWronglyAllocatedBuckets(result);
                        // We continue even if we could not find any wrongly allocated
                        // bucket -- maybe someone else found them before us, so we could use them.
                    }
                }
            } else {
                throw new AssertionError(response);
            }
        }
    }

    private void executeInitialDelayForMultiNode() throws InterruptedException {
        if (options.isExecuteInitialWait()) {
            long delay = (long) (Math.random() * getInitialDelay());
            if (delay != 0) {
                LOGGER.debug("executeInitialDelayForMultiNode: waiting {} ms in {}", delay, workerTaskOid);
                dynamicSleep(delay);
            }
        }
    }

    private long getInitialDelay() {
        WorkAllocationConfigurationType ac = options.getDistributionDefinition().getBuckets().getAllocation();
        return ac != null && ac.getWorkAllocationInitialDelay() != null ?
                ac.getWorkAllocationInitialDelay() : 0; // TODO workStateManager.getConfiguration().getWorkAllocationInitialDelay();
    }

    private void sleep(long toWait) throws InterruptedException {
        WorkBucketsManagementType bucketingConfig = options.getDistributionDefinition().getBuckets();
        long waitStart = System.currentTimeMillis();
        long sleepFor = Math.min(toWait, getFreeBucketWaitInterval(bucketingConfig));
        CONTENTION_LOGGER.trace("Entering waiting for free bucket (waiting for {}) - after {} ms (conflicts: {}) in {}",
                sleepFor, System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid);
        dynamicSleep(sleepFor);
        statisticsKeeper.addWaitTime(System.currentTimeMillis() - waitStart);
    }

    private void dynamicSleep(long delay) throws InterruptedException {
        while (delay > 0) {
            if (!canRun()) {
                throw new InterruptedException();
            }
            Thread.sleep(Math.min(delay, DYNAMIC_SLEEP_INTERVAL));
            delay -= DYNAMIC_SLEEP_INTERVAL;
        }
    }

    public boolean canRun() {
        return options.getCanRun() == null || Boolean.TRUE.equals(options.getCanRun().get());
    }

    private long getFreeBucketWaitInterval(WorkBucketsManagementType bucketingConfig) {
        WorkAllocationConfigurationType ac = bucketingConfig != null ? bucketingConfig.getAllocation() : null;
        if (ac != null && ac.getWorkAllocationFreeBucketWaitInterval() != null) {
            return ac.getWorkAllocationFreeBucketWaitInterval();
        } else {
            Long freeBucketWaitIntervalOverride = BucketingConfigurationOverrides.getFreeBucketWaitIntervalOverride();
            return freeBucketWaitIntervalOverride != null ? freeBucketWaitIntervalOverride :
                    20000; // TODO workStateManager.getConfiguration().getWorkAllocationDefaultFreeBucketWaitInterval();
        }
    }

    private long getRemainingTimeToWait() {
        long freeBucketWaitTime = options.getFreeBucketWaitTime();
        long waitUntil = freeBucketWaitTime >= 0 ? statisticsKeeper.start + freeBucketWaitTime : Long.MAX_VALUE;
        return waitUntil - System.currentTimeMillis();
    }

    private void onNothingFoundWithWaitTimeElapsed(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        markWorkComplete(result); // TODO also if response is not definite?
        CONTENTION_LOGGER.trace("'No bucket' found (wait time elapsed) after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid);
        statisticsKeeper.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_WAIT_TIME_ELAPSED);
    }

    private void onNothingFoundDefinite(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        markWorkComplete(result);
        CONTENTION_LOGGER.trace("'No bucket' found after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid);
        statisticsKeeper.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE);
    }

    private WorkBucketType onNewBucket(NewBuckets newBucketsResult) {
        CONTENTION_LOGGER.info("New bucket(s) acquired after {} ms (retries: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid);
        statisticsKeeper.register(GET_WORK_BUCKET_CREATED_NEW);
        return newBucketsResult.getSelectedBucket().clone(); // bucket state is not relevant
    }

    private WorkBucketType onFoundExisting(FoundExisting foundExistingResult) {
        CONTENTION_LOGGER.trace("Existing bucket acquired after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid);
        if (workerTaskOid != null) {
            statisticsKeeper.register(GET_WORK_BUCKET_DELEGATED);
        } else {
            statisticsKeeper.register(GET_WORK_BUCKET_FOUND_EXISTING);
        }
        return foundExistingResult.bucket.clone();
    }

    private void onNothingFoundForNonScavenger(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        markScavengingIfNotYet(result);
        CONTENTION_LOGGER.trace("'No bucket' found (and not a scavenger) after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid);
        statisticsKeeper.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_NOT_SCAVENGER);
    }

    /**
     * For each allocated work bucket we check if it is allocated to existing and non-closed child task.
     *
     * TODO use generalized transaction to obtain really current worker task state
     *  and then reclaim also from suspended workers
     */
    private void reclaimWronglyAllocatedBuckets(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        Set<String> liveWorkers = getLiveWorkers(result);
        Holder<Integer> reclaimingHolder = new Holder<>();

        plainRepositoryService.modifyObjectDynamically(TaskType.class, coordinatorTaskOid, null,
                task -> {
                    Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
                    ItemPath statePath = ActivityStateUtil.getStateItemPath(task.getActivityState(), activityPath);
                    List<WorkBucketType> buckets = getBuckets(task.getActivityState(), activityPath);
                    int reclaiming = 0;
                    for (WorkBucketType bucket : buckets) {
                        if (bucket.getState() == WorkBucketStateType.DELEGATED) {
                            String workerOid = getWorkerOid(bucket);
                            if (!liveWorkers.contains(workerOid)) {
                                LOGGER.info("Will try to reclaim wrongly allocated work bucket {} from worker task {}",
                                        bucket, workerOid);
                                modifications.addAll(
                                        bucketStateChangeDeltas(statePath, bucket, WorkBucketStateType.READY, null));
                                reclaiming++;
                            }
                        }
                    }
                    reclaimingHolder.setValue(reclaiming);
                    return modifications;
                }, null, result);

        LOGGER.info("Reclaimed {} buckets in {}", reclaimingHolder.getValue(), coordinatorTaskOid);
    }

    private Set<String> getLiveWorkers(OperationResult result) throws SchemaException, ObjectNotFoundException {
        Task coordinator = taskManager.getTask(coordinatorTaskOid, null, result);
        return coordinator.listSubtasks(result).stream()
                .filter(this::isRelevantWorker)
                .filter(t -> !t.isClosed())
                .map(Task::getOid)
                .collect(Collectors.toSet());
    }

    private boolean isRelevantWorker(Task worker) {
        TaskActivityStateType workState = worker.getWorkState();
        return workState != null &&
                workState.getLocalRootActivityExecutionRole() == ActivityExecutionRoleType.WORKER &&
                activityPath.equalsBean(workState.getLocalRoot());
    }

    private void markScavengingIfNotYet(OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        plainRepositoryService.modifyObjectDynamically(TaskType.class, coordinatorTaskOid, null,
                task -> {
                    if (BucketingUtil.isInScavengingPhase(task.getActivityState(), activityPath)) {
                        return List.of();
                    } else {
                        ItemPath stateItemPath = ActivityStateUtil.getStateItemPath(task.getActivityState(), activityPath);
                        return prismContext.deltaFor(TaskType.class)
                                .item(stateItemPath.append(F_BUCKETING, F_SCAVENGING)).replace(true)
                                .asItemDeltas();
                    }
                }, null, result);
    }

    private void markWorkComplete(OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        // We use dynamic modify only because we do not know the state item path without loading coordinator task first
        plainRepositoryService.modifyObjectDynamically(TaskType.class, coordinatorTaskOid, null,
                task -> {
                    ItemPath stateItemPath = ActivityStateUtil.getStateItemPath(task.getActivityState(), activityPath);
                    return prismContext.deltaFor(TaskType.class)
                            .item(stateItemPath.append(F_BUCKETING, F_WORK_COMPLETE)).replace(true)
                            .asItemDeltas();
                }, null, result);
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "options", options, indent + 1);
    }
}
