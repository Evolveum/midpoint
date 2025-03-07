/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets;

import static com.evolveum.midpoint.util.MiscUtil.*;

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

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;

import com.evolveum.midpoint.repo.common.activity.run.buckets.GetBucketOperationAttempt.Situation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.BucketFactory;
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

    /** This is to limit sample size if probabilities are used. */
    private static final int MAX_RANDOM_SAMPLING_INTERVAL = 1000;

    @NotNull private final GetBucketOperationOptions options;

    /** Generates new buckets under configuration provided by options. */
    private BucketFactory bucketFactory;

    /** If doing sampling, we try to get more buckets. All but the last are immediately marked as COMPLETE. */
    private int bucketsToGet;

    GetBucketOperation(@NotNull String coordinatorTaskOid, @Nullable String workerTaskOid, @NotNull ActivityPath activityPath,
            ActivityBucketManagementStatistics statisticsCollector,
            @Nullable GetBucketOperationOptions options, CommonTaskBeans beans) {
        super(coordinatorTaskOid, workerTaskOid, activityPath, statisticsCollector,
                GetBucketOperationOptions.getProgressConsumer(options), beans);
        this.options = requireNonNullElseGet(options, GetBucketOperationOptions::standard);
    }

    /**
     * @return Bucket that should be processed (or null if there's none). Note that the state of the bucket
     * is not relevant; it may be READY or DELEGATED.
     */
    public WorkBucketType execute(OperationResult result) throws SchemaException, ObjectNotFoundException,
            ObjectAlreadyExistsException, InterruptedException {

        bucketFactory = BucketFactory.create(
                options.getDistributionDefinition(),
                options.getImplicitSegmentationResolver(),
                beans);

        this.bucketsToGet = determineBucketsToGet();

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

        TaskType coordinatorTask = plainRepositoryService.getObject(TaskType.class, coordinatorTaskOid, null, result)
                .asObjectable();

        GetBucketOperationAttempt attempt =
                new GetBucketOperationAttempt(coordinatorTask, workerTaskOid, activityPath, bucketFactory,
                        bucketsToGet, bucketProgressHolder);

        attempt.execute();
        bucketProgressHolder.passValue();

        if (!attempt.getModifications().isEmpty()) {
            plainRepositoryService.modifyObject(TaskType.class, coordinatorTaskOid, attempt.getModifications(), result);
        }

        if (attempt.getBucketToUse() != null) {
            recordNonNullReturn(attempt);
            return attempt.getBucketToUse();
        }

        // Nothing found!

        stateCheck(attempt.isDefinite(), "Nothing was found with indefinite answer in standalone mode");
        markWorkComplete(result);
        recordNothingFoundDefinite();
        return null;
    }

    private WorkBucketType getBucketMultiNode(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, InterruptedException {

        for (;;) {

            Holder<GetBucketOperationAttempt> lastAttemptHolder = new Holder<>();
            ModifyObjectResult<TaskType> modifyResult = plainRepositoryService.modifyObjectDynamically(TaskType.class,
                    coordinatorTaskOid, null,
                    existingCoordinatorTask -> {
                        var coordinatorTask = existingCoordinatorTask.clone(); // todo check if the code below can change the data
                        GetBucketOperationAttempt attempt =
                                new GetBucketOperationAttempt(coordinatorTask, workerTaskOid, activityPath,
                                        bucketFactory, bucketsToGet, bucketProgressHolder);
                        lastAttemptHolder.setValue(attempt);
                        attempt.execute();
                        return attempt.getModifications();
                    }, null, result);

            bucketProgressHolder.passValue();

            // Let us record the conflicts encountered. Note that we ignore conflicts encountered in previous iterations
            // of the outer "for" cycle, i.e. when a scavenger hits "no more buckets" situation.
            statisticsKeeper.setConflictCounts(modifyResult);

            GetBucketOperationAttempt lastAttempt =
                    Objects.requireNonNull(lastAttemptHolder.getValue(), "no last attempt recorded");

            if (lastAttempt.getBucketToUse() != null) {
                recordNonNullReturn(lastAttempt);
                return lastAttempt.getBucketToUse();
            }

            // Nothing found!

            if (!options.isScavenger()) {
                markScavengingIfNotYet(result);
                recordNothingFoundForNonScavenger();
                return null;
            }

            if (lastAttempt.isDefinite() || options.getFreeBucketWaitTime() == 0L) {
                markWorkComplete(result);
                recordNothingFoundDefinite();
                return null;
            }

            long toWait = getRemainingTimeToWait();
            if (toWait <= 0) {
                markWorkComplete(result); // TODO really marking work as complete?
                recordNothingFoundWithWaitTimeElapsed();
                return null;
            }

            sleep(toWait);
            reclaimWronglyAllocatedBuckets(result);
            // We continue even if we could not find any wrongly allocated
            // bucket -- maybe someone else found them before us, so we could use them.
        }
    }

    /**
     * Recording situation when there is a bucket to be returned.
     * (The cases where there is no bucket are treated separately for standalone/workers cases.
     */
    private void recordNonNullReturn(@NotNull GetBucketOperationAttempt attempt) {
        @NotNull Situation situation = attempt.getSituationRequired();
        switch (situation) {
            case FOUND_DELEGATED_TO_ME:
                recordFoundDelegated(attempt);
                break;
            case FOUND_READY:
                recordFoundReady(attempt);
                break;
            case CREATED_NEW:
                recordCreatedNew(attempt);
                break;
            default:
                throw new AssertionError(situation);
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
        ActivityDistributionDefinition distribution = options.getDistributionDefinition();
        WorkAllocationDefinitionType allocation = distribution != null ?
                distribution.getBuckets().getAllocation() : null;
        return allocation != null && allocation.getWorkAllocationInitialDelay() != null ?
                allocation.getWorkAllocationInitialDelay() : 0; // TODO workStateManager.getConfiguration().getWorkAllocationInitialDelay();
    }

    private void sleep(long toWait) throws InterruptedException {
        BucketsDefinitionType bucketing =
                options.getDistributionDefinition() != null ?
                        options.getDistributionDefinition().getBuckets() : null;
        long waitStart = System.currentTimeMillis();
        long sleepFor = Math.min(toWait, getFreeBucketWaitInterval(bucketing));
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

    private long getFreeBucketWaitInterval(BucketsDefinitionType bucketing) {
        WorkAllocationDefinitionType ac = bucketing != null ? bucketing.getAllocation() : null;
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

    private void recordFoundDelegated(@NotNull GetBucketOperationAttempt attempt) {
        LOGGER.trace("Returning already delegated bucket for {}: {}", workerTaskOid, attempt.getBucketToUse());
        statisticsKeeper.register(GET_WORK_BUCKET_FOUND_DELEGATED);
    }

    private void recordFoundReady(@NotNull GetBucketOperationAttempt attempt) {
        CONTENTION_LOGGER.trace("Existing bucket acquired after {} ms (conflicts: {}) in {}: {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid,
                attempt.getBucketToUse());
        statisticsKeeper.register(GET_WORK_BUCKET_FOUND_READY);
    }

    private void recordCreatedNew(@NotNull GetBucketOperationAttempt attempt) {
        CONTENTION_LOGGER.trace("New bucket(s) acquired after {} ms (retries: {}) in {}: {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid,
                attempt.getBucketToUse());
        statisticsKeeper.register(GET_WORK_BUCKET_CREATED_NEW);
    }

    private void recordNothingFoundWithWaitTimeElapsed() {
        CONTENTION_LOGGER.trace("'No bucket' found (wait time elapsed) after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid);
        statisticsKeeper.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_WAIT_TIME_ELAPSED);
    }

    private void recordNothingFoundDefinite() {
        CONTENTION_LOGGER.trace("'No bucket' found after {} ms (conflicts: {}) in {}",
                System.currentTimeMillis() - statisticsKeeper.start, statisticsKeeper.conflictCount, workerTaskOid);
        statisticsKeeper.register(GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE);
    }

    private void recordNothingFoundForNonScavenger() {
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
        Holder<Integer> reclaimingHolder = new Holder<>(0);

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

        if (reclaimingHolder.getValue() > 0) {
            LOGGER.info("Reclaimed {} buckets in {}", reclaimingHolder.getValue(), coordinatorTaskOid);
        } else {
            LOGGER.debug("Reclaimed no buckets in {}", coordinatorTaskOid);
        }
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
                workState.getTaskRole() == TaskRoleType.WORKER &&
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

    private int determineBucketsToGet() {
        ActivityDistributionDefinition def = options.getDistributionDefinition();
        if (def == null || def.getBuckets() == null || def.getBuckets().getSampling() == null) {
            return 1;
        }

        BucketsSamplingDefinitionType sampling = def.getBuckets().getSampling();
        var regular = sampling.getRegular();
        var random = sampling.getRandom();
        argCheck(regular == null || random == null, "Both regular and random sampling is selected");

        int interval;
        if (regular != null) {
            argCheck(regular.getInterval() == null || regular.getSampleSize() == null,
                    "Both interval and sample size are configured");
            if (regular.getInterval() != null) {
                interval = regular.getInterval();
                argCheck(interval > 0, "Specified sampling interval is less than 1: %s", interval);
            } else if (regular.getSampleSize() != null) {
                int numberOfBuckets = getNumberOfBucketsForSampling();
                interval = Math.max(1, numberOfBuckets / regular.getSampleSize());
            } else {
                throw new IllegalArgumentException("Regular sampling is selected but not configured");
            }
        } else if (random != null) {
            double probability;
            if (random.getSampleSize() != null) {
                probability = (double) random.getSampleSize() / getNumberOfBucketsForSampling();
            } else if (random.getProbability() != null) {
                probability = random.getProbability();
                argCheck(probability >= 0 && probability <= 1,
                        "Probability is not in [0;1] interval: %s", probability);
            } else {
                throw new IllegalArgumentException("Random sampling is selected but not configured");
            }
            interval = getIntervalForProbability(probability);
        } else {
            throw new IllegalArgumentException("Sampling is selected but neither regular nor random variant is set");
        }

        LOGGER.info("Using sampling interval of {}", interval); // todo
        stateCheck(interval > 0, "Computed interval is less than 1: %s", interval);
        return interval;
    }

    private int getIntervalForProbability(double probability) {
        // We are too lazy to compute the interval mathematically. So using random generator.

        int interval;
        for (interval = 1; interval < MAX_RANDOM_SAMPLING_INTERVAL; interval++) {
            if (Math.random() < probability) {
                break;
            }
        }
        return interval;
    }

    private int getNumberOfBucketsForSampling() {
        Integer numberOfBuckets = bucketFactory.estimateNumberOfBuckets();
        stateCheck(numberOfBuckets != null, "Couldn't compute sampling parameters because the total number "
                + "of buckets is not known.");
        return numberOfBuckets;
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "options", options, indent + 1);
    }
}
