/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.VersionPrecondition;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketContentFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.util.DebugUtil;
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

import java.util.*;
import java.util.Objects;
import java.util.function.Supplier;

import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.getActivityWorkStateRequired;
import static com.evolveum.midpoint.schema.util.task.BucketingUtil.getBuckets;

import static com.evolveum.midpoint.schema.util.task.BucketingUtil.getNumberOfBuckets;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketingStateType.F_NUMBER_OF_BUCKETS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketingStateType.F_WORK_COMPLETE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType.F_BUCKETING;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Implements "get bucket" operation.
 */
public class GetBucketOperation extends BucketOperation {

    private static final Trace LOGGER = TraceManager.getTrace(GetBucketOperation.class);

    private static final long DYNAMIC_SLEEP_INTERVAL = 100L;

    @NotNull private final ActivityDistributionDefinition distributionDefinition;
    private final Supplier<Boolean> canRunSupplier;
    private final Options options;

    GetBucketOperation(@NotNull String workerTaskOid, @NotNull ActivityPath activityPath,
            @NotNull ActivityDistributionDefinition distributionDefinition,
            WorkBucketStatisticsCollector statisticsCollector, BucketingManager bucketingManager,
            Supplier<Boolean> canRunSupplier, Options options) {
        super(workerTaskOid, activityPath, statisticsCollector, bucketingManager);
        this.distributionDefinition = distributionDefinition;
        this.canRunSupplier = canRunSupplier;
        this.options = options;
    }

    public WorkBucketType execute(OperationResult result) throws SchemaException, ObjectNotFoundException,
            ObjectAlreadyExistsException, InterruptedException {

        loadTasks(result);

        LOGGER.trace("Task(s) loaded within 'get bucket' operation:\n{}", debugDumpLazily());

        try {
            if (isStandalone()) {
                return getWorkBucketStandalone(result);
            } else {
                return getWorkBucketMultiNode(result);
            }
        } catch (Throwable t) {
            statisticsKeeper.register("getWorkBucket." + t.getClass().getSimpleName());
            throw t;
        }
    }

    private WorkBucketType getWorkBucketStandalone(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        BucketAllocator allocator = BucketAllocator.create(distributionDefinition, bucketingManager.getStrategyFactory());

        setOrUpdateEstimatedNumberOfBuckets(workerTask, workerStatePath, allocator.getContentFactory(), result);

        ActivityStateType workState = getWorkerTaskActivityWorkState();
        BucketAllocator.Response response = allocator.getBucket(getBuckets(workState));
        LOGGER.trace("getWorkBucketStandalone: segmentation strategy returned {} for standalone task {}", response, workerTask);

        if (response instanceof BucketAllocator.Response.FoundExisting) {
            statisticsKeeper.register(GET_WORK_BUCKET_FOUND_SELF_ALLOCATED);
            return ((BucketAllocator.Response.FoundExisting) response).bucket;
        } else if (response instanceof BucketAllocator.Response.NewBuckets) {
            BucketAllocator.Response.NewBuckets newBucketsResponse = (BucketAllocator.Response.NewBuckets) response;
            repositoryService.modifyObject(TaskType.class, workerTask.getOid(),
                    bucketsAddDeltas(workerStatePath, newBucketsResponse.newBuckets), null, result);
            statisticsKeeper.register(GET_WORK_BUCKET_CREATED_NEW);
            return newBucketsResponse.newBuckets.get(newBucketsResponse.selected);
        } else if (response instanceof BucketAllocator.Response.NothingFound) {
            if (!((BucketAllocator.Response.NothingFound) response).definite) {
                throw new AssertionError("Unexpected 'indefinite' answer when looking for next bucket in a standalone task: " + workerTask);
            }
            statisticsKeeper.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE);
            markWorkComplete(workerTask, workerStatePath, result);
            return null;
        } else {
            throw new AssertionError(response);
        }
    }

    private void setOrUpdateEstimatedNumberOfBuckets(Task task, ItemPath statePath, BucketContentFactory contentFactory,
            OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        @NotNull ActivityStateType workState = getActivityWorkStateRequired(task.getWorkState(), statePath);

        Integer number = contentFactory.estimateNumberOfBuckets();
        if (number != null && !number.equals(getNumberOfBuckets(workState))) {
            List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                    .item(statePath.append(F_BUCKETING, F_NUMBER_OF_BUCKETS))
                    .replace(number)
                    .asItemDeltas();
            repositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, result);
        }
    }

    private WorkBucketType getWorkBucketMultiNode(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, InterruptedException {

        WorkBucketType selfAllocated = findSelfAllocatedBucket();
        if (selfAllocated != null) {
            statisticsKeeper.register(GET_WORK_BUCKET_FOUND_SELF_ALLOCATED);
            LOGGER.trace("Returning self-allocated bucket for {}: {}", workerTaskOid, selfAllocated);
            return selfAllocated;
        }

        executeInitialDelayForMultiNode();

        WorkBucketsManagementType bucketingConfig = getCoordinatorBucketingConfig();
        BucketAllocator allocator = BucketAllocator.create(bucketingConfig, bucketingManager.getStrategyFactory());

        setOrUpdateEstimatedNumberOfBuckets(coordinatorTask, coordinatorStatePath, allocator.getContentFactory(), result);

        for (;;) {

            Holder<BucketAllocator.Response> lastAllocatorResponseHolder = new Holder<>();
            ModifyObjectResult<TaskType> modifyResult = repositoryService.modifyObjectDynamically(TaskType.class,
                    coordinatorTask.getOid(), null,
                    coordinatorTask -> computeCoordinatorModifications(coordinatorTask, allocator, lastAllocatorResponseHolder),
                    null, result);

            // We ignore conflicts encountered in previous iterations (scavenger hitting 'no more buckets' situation).
            statisticsKeeper.setConflictCounts(modifyResult);

            BucketAllocator.Response response =
                    Objects.requireNonNull(lastAllocatorResponseHolder.getValue(), "no last getBucket result");

            if (response instanceof BucketAllocator.Response.NewBuckets) {
                return recordNewBucketInWorkerTask((BucketAllocator.Response.NewBuckets) response, result);
            } if (response instanceof BucketAllocator.Response.FoundExisting) {
                return recordExistingBucketInWorkerTask((BucketAllocator.Response.FoundExisting) response, result);
            } else if (response instanceof BucketAllocator.Response.NothingFound) {
                if (!ActivityStateUtil.isScavenger(workerTask.getWorkState(), activityPath)) {
                    processNothingFoundForNonScavenger();
                    return null;
                } else if (((BucketAllocator.Response.NothingFound) response).definite || options.freeBucketWaitTime == 0L) {
                    processNothingFoundDefinite(result);
                    return null;
                } else {
                    long toWait = getRemainingTimeToWait();
                    if (toWait <= 0) {
                        processNothingFoundWithWaitTimeElapsed(result);
                        return null;
                    } else {
                        sleep(toWait, bucketingConfig);
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

    private WorkBucketType findSelfAllocatedBucket() {
        List<WorkBucketType> buckets = getWorkerTaskBuckets();
        if (buckets.isEmpty()) {
            return null;
        }
        return buckets.stream()
                .filter(b -> b.getState() == WorkBucketStateType.READY)
                .min(Comparator.comparingInt(WorkBucketType::getSequentialNumber)) // Must it be the first one? Probably not.
                .orElse(null);
    }

    private void executeInitialDelayForMultiNode() throws InterruptedException {
        if (options.executeInitialWait) {
            long delay = (long) (Math.random() * getInitialDelay());
            if (delay != 0) {
                LOGGER.debug("executeInitialDelayForMultiNode: waiting {} ms in {}", delay, workerTask);
                dynamicSleep(delay);
            }
        }
    }

    private long getInitialDelay() {
        WorkBucketsManagementType bucketingConfig = getCoordinatorBucketingConfig();
        WorkAllocationConfigurationType ac = bucketingConfig.getAllocation();
        return ac != null && ac.getWorkAllocationInitialDelay() != null ?
                ac.getWorkAllocationInitialDelay() : 0; // TODO workStateManager.getConfiguration().getWorkAllocationInitialDelay();
    }

    private void sleep(long toWait, WorkBucketsManagementType bucketingConfig) throws InterruptedException {
        long waitStart = System.currentTimeMillis();
        long sleepFor = Math.min(toWait, getFreeBucketWaitInterval(bucketingConfig));
        CONTENTION_LOGGER.trace("Entering waiting for free bucket (waiting for {}) - after {} ms (conflicts: {}) in {}",
                sleepFor, System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTask);
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
        return canRunSupplier == null || BooleanUtils.isTrue(canRunSupplier.get());
    }

    private long getFreeBucketWaitInterval(WorkBucketsManagementType bucketingConfig) {
        WorkAllocationConfigurationType ac = bucketingConfig != null ? bucketingConfig.getAllocation() : null;
        if (ac != null && ac.getWorkAllocationFreeBucketWaitInterval() != null) {
            return ac.getWorkAllocationFreeBucketWaitInterval();
        } else {
            Long freeBucketWaitIntervalOverride = bucketingManager.getFreeBucketWaitIntervalOverride();
            return freeBucketWaitIntervalOverride != null ? freeBucketWaitIntervalOverride :
                    20000; // TODO workStateManager.getConfiguration().getWorkAllocationDefaultFreeBucketWaitInterval();
        }
    }

    private void processNothingFoundWithWaitTimeElapsed(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        markWorkComplete(coordinatorTask, coordinatorStatePath, result); // TODO also if response is not definite?
        CONTENTION_LOGGER.trace("'No bucket' found (wait time elapsed) after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTask);
        statisticsKeeper.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_WAIT_TIME_ELAPSED);
    }

    private long getRemainingTimeToWait() {
        long waitDeadline = options.freeBucketWaitTime >= 0 ? statisticsKeeper.start + options.freeBucketWaitTime : Long.MAX_VALUE;
        return waitDeadline - System.currentTimeMillis();
    }

    private void processNothingFoundDefinite(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        markWorkComplete(coordinatorTask, coordinatorStatePath, result);
        CONTENTION_LOGGER.trace("'No bucket' found after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTask);
        statisticsKeeper.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE);
    }

    private WorkBucketType recordNewBucketInWorkerTask(BucketAllocator.Response.NewBuckets newBucketsResult, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        WorkBucketType selectedBucket = newBucketsResult.newBuckets.get(newBucketsResult.selected).clone();
        repositoryService.modifyObject(TaskType.class, workerTask.getOid(),
                bucketsAddDeltas(workerStatePath, singletonList(selectedBucket)), null, result);
        CONTENTION_LOGGER.info("New bucket(s) acquired after {} ms (retries: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTask);
        statisticsKeeper.register(GET_WORK_BUCKET_CREATED_NEW);
        return selectedBucket;
    }

    private WorkBucketType recordExistingBucketInWorkerTask(BucketAllocator.Response.FoundExisting foundExistingResult,
            OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        WorkBucketType foundBucket = foundExistingResult.bucket.clone();
        repositoryService.modifyObject(TaskType.class, workerTask.getOid(),
                bucketsAddDeltas(workerStatePath, singletonList(foundBucket)), null, result);
        CONTENTION_LOGGER.trace("Existing bucket acquired after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTask);
        statisticsKeeper.register(GET_WORK_BUCKET_DELEGATED);
        return foundBucket;
    }

    private void processNothingFoundForNonScavenger() {
        CONTENTION_LOGGER.trace("'No bucket' found (and not a scavenger) after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTask);
        statisticsKeeper.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_NOT_SCAVENGER);
    }

    /**
     * Computes modifications to the current work state aimed to obtain new buckets, if possible.
     *
     * We assume that the updated coordinator task has the same part work state PCV ID as the original one.
     *
     * BEWARE!!! This is invoked from the dynamic modification code, so it must NOT access stored coordinator task.
     * It may access coordinator state path, as it is assumed not to change.
     */
    private Collection<? extends ItemDelta<?, ?>> computeCoordinatorModifications(TaskType coordinatorTask,
            BucketAllocator allocator, Holder<BucketAllocator.Response> lastAllocatorResponseHolder)
            throws SchemaException {

        ActivityStateType workState = getTaskActivityWorkState(coordinatorTask);
        BucketAllocator.Response response = allocator.getBucket(getBuckets(workState));
        lastAllocatorResponseHolder.setValue(response);
        LOGGER.trace("computeWorkBucketModifications: bucket allocator returned {} for worker task {}, coordinator {}",
                response, workerTask, coordinatorTask);
        if (response instanceof BucketAllocator.Response.NewBuckets) {
            return computeCoordinatorModificationsForNewBuckets(workState, (BucketAllocator.Response.NewBuckets) response);
        } else if (response instanceof BucketAllocator.Response.FoundExisting) {
            return computeCoordinatorModificationsForExistingBucket((BucketAllocator.Response.FoundExisting) response);
        } else if (response instanceof BucketAllocator.Response.NothingFound) {
            return emptyList(); // Nothing to do for now.
        } else {
            throw new AssertionError(response);
        }
    }

    private Collection<ItemDelta<?, ?>> computeCoordinatorModificationsForNewBuckets(ActivityStateType workState,
            BucketAllocator.Response.NewBuckets response) throws SchemaException {
        List<WorkBucketType> newCoordinatorBuckets = new ArrayList<>(getBuckets(workState));
        for (int i = 0; i < response.newBuckets.size(); i++) {
            if (i == response.selected) {
                newCoordinatorBuckets.add(response.newBuckets.get(i).clone()
                        .state(WorkBucketStateType.DELEGATED)
                        .workerRef(workerTask.getOid(), TaskType.COMPLEX_TYPE));
            } else {
                newCoordinatorBuckets.add(response.newBuckets.get(i).clone());
            }
        }
        return bucketsReplaceDeltas(coordinatorStatePath, newCoordinatorBuckets);
    }

    private Collection<ItemDelta<?, ?>> computeCoordinatorModificationsForExistingBucket(BucketAllocator.Response.FoundExisting response)
            throws SchemaException {
        return bucketStateChangeDeltas(coordinatorStatePath, response.bucket, WorkBucketStateType.DELEGATED, workerTask.getOid());
    }

    /**
     * For each allocated work bucket we check if it is allocated to existing and non-closed child task.
     * Returns true if there was something to reclaim.
     */
    private void reclaimWronglyAllocatedBuckets(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        reloadCoordinatorTask(result);
        ActivityStateType partWorkState = getCoordinatorTaskPartWorkState();
        ActivityStateType newState = partWorkState.clone();
        int reclaiming = 0;
        Set<String> deadWorkers = new HashSet<>();
        Set<String> liveWorkers = new HashSet<>();
        for (WorkBucketType bucket : getBuckets(newState)) {
            if (bucket.getState() == WorkBucketStateType.DELEGATED) {
                String workerOid = getWorkerOid(bucket);
                if (isDead(workerOid, deadWorkers, liveWorkers, result)) {
                    LOGGER.info("Will reclaim wrongly allocated work bucket {} from worker task {}", bucket, workerOid);
                    bucket.setState(WorkBucketStateType.READY);
                    bucket.setWorkerRef(null);
                    // TODO modify also the worker if it exists (maybe)
                    reclaiming++;
                }
            }
        }
        LOGGER.trace("Reclaiming wrongly allocated buckets found {} buckets to reclaim in {}", reclaiming, coordinatorTask);
        if (reclaiming > 0) {
            CONTENTION_LOGGER.debug("Reclaiming wrongly allocated buckets found {} buckets to reclaim in {}", reclaiming, coordinatorTask);
            try {
                // As for the precondition we use the whole task state (reflected by version). The reason is that if the work
                // state originally contains (wrongly) DELEGATED bucket plus e.g. last COMPLETE one, and this bucket is reclaimed
                // by two subtasks at once, each of them see the same state afterwards: READY + COMPLETE.
                repositoryService.modifyObject(TaskType.class, coordinatorTask.getOid(),
                        bucketsReplaceDeltas(coordinatorStatePath, getBuckets(newState)),
                        new VersionPrecondition<>(coordinatorTask.getVersion()), null, result);
                statisticsKeeper.addReclaims(reclaiming);
            } catch (PreconditionViolationException e) {
                LOGGER.info("Concurrent modification of work state in {}. {} wrongly allocated bucket(s) will "
                        + "not be reclaimed in this run.", coordinatorTask, reclaiming);
            }
            reloadCoordinatorTask(result);
        }
    }

    @Nullable
    private String getWorkerOid(WorkBucketType bucket) {
        return bucket.getWorkerRef() != null ? bucket.getWorkerRef().getOid() : null;
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

    private void markWorkComplete(Task task, ItemPath statePath, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                .item(statePath.append(F_BUCKETING, F_WORK_COMPLETE))
                .replace(true)
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, result);
    }

    private void reloadCoordinatorTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
        coordinatorTask = taskManager.getTaskPlain(coordinatorTask.getOid(), null, result);
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "distributionDefinition", distributionDefinition, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "options", String.valueOf(options), indent + 1);
    }

    static class Options {

        private final long freeBucketWaitTime;
        private final boolean executeInitialWait;

        public Options(long freeBucketWaitTime, boolean executeInitialWait) {
            this.freeBucketWaitTime = freeBucketWaitTime;
            this.executeInitialWait = executeInitialWait;
        }

        @Override
        public String toString() {
            return "Options{" +
                    "freeBucketWaitTime=" + freeBucketWaitTime +
                    ", executeInitialWait=" + executeInitialWait +
                    '}';
        }
    }
}
