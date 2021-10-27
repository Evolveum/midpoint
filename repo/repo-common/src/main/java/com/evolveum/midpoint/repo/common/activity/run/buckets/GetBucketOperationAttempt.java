/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets;

import static com.evolveum.midpoint.repo.common.activity.run.buckets.BucketOperation.bucketStateChangeDeltas;
import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.getActivityStateRequired;
import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.getStateItemPath;
import static com.evolveum.midpoint.schema.util.task.BucketingUtil.getNumberOfBuckets;
import static com.evolveum.midpoint.schema.util.task.BucketingUtil.getWorkerOid;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketingStateType.F_NUMBER_OF_BUCKETS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType.F_BUCKETING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketStateType.*;

import java.util.*;
import java.util.stream.Stream;

import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.PassingHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BucketProgressOverviewType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.BucketFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Executes/represents an attempt to obtain a bucket in a given coordinator (buckets-holding) task.
 *
 * See {@link #execute()} method.
 */
class GetBucketOperationAttempt {

    private static final Trace LOGGER = TraceManager.getTrace(GetBucketOperationAttempt.class);

    /**
     * OID of the worker task (null iff standalone).
     */
    @Nullable private final String workerOid;

    /** Receives "after" state of the progress. */
    private final PassingHolder<BucketProgressOverviewType> bucketProgressHolder;

    /**
     * Item path for the current bucketed activity state.
     */
    @NotNull private final ItemPath activityStateItemPath;

    /**
     * Current state of the activity.
     */
    @NotNull private final ActivityStateType activityState;

    /**
     * Current list of buckets. It is a "working copy": updated by this class, but only to know the effect
     * of the modifications planned.
     */
    @NotNull private final List<WorkBucketType> currentBuckets;

    /**
     * Modifications to be applied to the task. After they are applied successfully,
     * the buckets are considered to be obtained. (If the application fails because of
     * concurrency, the "get bucket" operation is retried in another attempt.)
     */
    @NotNull private final Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();

    /**
     * Buckets to be added. They are swallowed into modifications at the end.
     * (To aggregate them into one modification.)
     *
     * They are detached (cloned), ready to be used in the delta.
     */
    @NotNull private final List<WorkBucketType> bucketsToAdd = new ArrayList<>();

    /**
     * Situation that occurred (for reporting).
     *
     * Must be non-null after successful execution of {@link #execute()}.
     */
    private Situation situation;

    /**
     * Bucket to be returned to the caller.
     */
    private WorkBucketType bucketToUse;

    /**
     * How many buckets should we get? Normally the number is 1.
     *
     * But in sampling mode with sample size of N we want to get N buckets,
     * where first N-1 are created in the COMPLETE state, and only the N-th is READY or DELEGATED.
     *
     * The exception is when we want to return already-delegated bucket. Then no sampling is done.
     */
    private int numberOfBucketsToGet;

    /**
     * Configured allocator that generates buckets.
     */
    @NotNull private final BucketFactory bucketFactory;

    GetBucketOperationAttempt(@NotNull TaskType task, @Nullable String workerOid, @NotNull ActivityPath activityPath,
            @NotNull BucketFactory bucketFactory, int numberOfBucketsToGet,
            @NotNull PassingHolder<BucketProgressOverviewType> bucketProgressHolder) {
        this.workerOid = workerOid;
        this.bucketProgressHolder = bucketProgressHolder;
        this.activityStateItemPath = getStateItemPath(task.getActivityState(), activityPath);
        this.activityState = getActivityStateRequired(task.getActivityState(), activityStateItemPath);
        this.currentBuckets = BucketingUtil.getBuckets(activityState);
        this.bucketFactory = bucketFactory;
        this.numberOfBucketsToGet = numberOfBucketsToGet;
    }

    /**
     * Obtains a bucket. Skips buckets when sampling is used.
     * Potentially pre-creates buckets if batch allocation is used.
     *
     * This method can be called from {@link RepositoryService#modifyObjectDynamically(Class, String, Collection,
     * RepositoryService.ModificationsSupplier, RepoModifyOptions, OperationResult)} method (in case
     * of coordinator-workers scenario), or simply as part of `getObject` - compute changes - `modifyObject`
     * process (in case of standalone scenario).
     */
    void execute() throws SchemaException {

        argCheck(numberOfBucketsToGet > 0, "Number of buckets to get is less than 1: %s", numberOfBucketsToGet);

        setOrUpdateEstimatedNumberOfBuckets();

        if (workerOid != null) {
            offerExistingBuckets(
                    getSelfDelegatedBucketsStream());
            if (numberOfBucketsToGet == 0) {
                situation = Situation.FOUND_DELEGATED_TO_ME;
                return;
            }
        }

        offerExistingBuckets(
                getReadyBucketsStream());

        if (numberOfBucketsToGet == 0) {
            situation = Situation.FOUND_READY;
            return;
        }

        offerNewBuckets(
                bucketFactory.createNewBuckets(currentBuckets, numberOfBucketsToGet));

        if (numberOfBucketsToGet == 0) {
            situation = Situation.CREATED_NEW;
            return;
        }

        // If there remained some self-delegated buckets, the number of buckets to get is 0 and we are not here.
        assert getSelfDelegatedBucketsStream().findAny().isEmpty();

        situation = anyBucketsDelegated() ?
                Situation.NOTHING_MORE_SOME_DELEGATED :
                Situation.NOTHING_MORE_DEFINITE;
    }

    private boolean anyBucketsDelegated() {
        return currentBuckets.stream()
                .anyMatch(b -> b.getState() == DELEGATED);
    }

    @NotNull
    private Stream<WorkBucketType> getReadyBucketsStream() {
        return currentBuckets.stream()
                .filter(b -> b.getState() == READY);
    }

    private Stream<WorkBucketType> getSelfDelegatedBucketsStream() {
        return workerOid != null ?
                currentBuckets.stream()
                        .filter(b -> BucketingUtil.isDelegatedTo(b, workerOid)) :
                Stream.empty();
    }

    /**
     * Offers existing buckets for processing:
     *
     * - skipping first N-1 of them,
     * - giving the N-th for use,
     * - not touching the rest.
     *
     * If numberOfBucketsToGet is non-zero at exit, then all existing buckets were consumed.
     */
    private void offerExistingBuckets(Stream<WorkBucketType> buckets) {
        Iterator<WorkBucketType> iterator = buckets.iterator();
        while (iterator.hasNext()) {
            WorkBucketType bucket = iterator.next();
            if (numberOfBucketsToGet == 0) {
                return; // leaving remaining existing buckets intact
            } else if (numberOfBucketsToGet == 1) {
                markExistingBucketToUse(bucket);
            } else {
                markExistingBucketSkipped(bucket);
            }
            numberOfBucketsToGet--;
        }
    }

    /**
     * Offers new buckets for processing:
     *
     * - skipping first N-1 of them,
     * - giving the N-th for use,
     * - adding the rest for future use.
     */
    private void offerNewBuckets(List<WorkBucketType> buckets) {
        for (WorkBucketType bucket : buckets) {
            if (numberOfBucketsToGet == 0) {
                markNewBucketForFutureUse(bucket);
            } else if (numberOfBucketsToGet == 1) {
                markNewBucketToUse(bucket);
            } else {
                markNewBucketSkipped(bucket);
            }
            if (numberOfBucketsToGet > 0) {
                numberOfBucketsToGet--;
            }
        }
        swallowBucketsToAdd();
    }

    private void markExistingBucketToUse(@NotNull WorkBucketType bucket) {
        if (workerOid != null) {
            if (!BucketingUtil.isDelegatedTo(bucket, workerOid)) {
                bucket.state(DELEGATED)
                        .workerRef(workerOid, TaskType.COMPLEX_TYPE);
                swallow(bucketStateChangeDeltas(activityStateItemPath, bucket, DELEGATED, workerOid));
            }
        } else {
            if (bucket.getState() != READY || getWorkerOid(bucket) != null) {
                bucket.state(READY)
                        .workerRef(null);
                swallow(bucketStateChangeDeltas(activityStateItemPath, bucket, READY, null));
            }
        }
        bucketToUse = bucket.clone();
    }

    private void markNewBucketToUse(@NotNull WorkBucketType bucket) {
        if (workerOid != null) {
            bucket.state(DELEGATED)
                    .workerRef(workerOid, TaskType.COMPLEX_TYPE);
        } else {
            bucket.state(READY)
                    .workerRef(null);
        }
        swallow(bucket);
        bucketToUse = bucket.clone();
    }

    private void markExistingBucketSkipped(@NotNull WorkBucketType bucket) {
        LOGGER.debug("Marking existing bucket as COMPLETE because of sampling: {}", bucket);
        bucket.state(COMPLETE);
        swallow(bucketStateChangeDeltas(activityStateItemPath, bucket, COMPLETE));
    }

    private void markNewBucketSkipped(@NotNull WorkBucketType bucket) {
        LOGGER.debug("Marking new bucket as COMPLETE because of sampling: {}", bucket);
        bucket.state(COMPLETE);
        swallow(bucket);
    }

    private void markNewBucketForFutureUse(@NotNull WorkBucketType bucket) {
        LOGGER.debug("Marking new bucket as COMPLETE because of sampling: {}", bucket);
        if (workerOid != null) {
            bucket.state(DELEGATED)
                    .workerRef(workerOid, TaskType.COMPLEX_TYPE);
        } else {
            bucket.state(READY)
                    .workerRef(null);
        }
        swallow(bucket);
    }

    private void setOrUpdateEstimatedNumberOfBuckets() throws SchemaException {
        Integer number = bucketFactory.estimateNumberOfBuckets();
        if (number != null && !number.equals(getNumberOfBuckets(activityState))) {
            List<ItemDelta<?, ?>> numberOfBucketsMods = PrismContext.get().deltaFor(TaskType.class)
                    .item(activityStateItemPath.append(F_BUCKETING, F_NUMBER_OF_BUCKETS))
                    .replace(number)
                    .asItemDeltas();
            LOGGER.trace("Going to set # of buckets:\n{}", DebugUtil.debugDumpLazily(numberOfBucketsMods, 1));
            modifications.addAll(numberOfBucketsMods);
        }

        // The number of complete buckets is not changed by this operation, so we can report the progress right now.
        bucketProgressHolder.accept(
                new BucketProgressOverviewType()
                        .totalBuckets(number)
                        .completeBuckets(BucketingUtil.getCompleteBucketsNumber(currentBuckets)));
    }

    private void swallow(Collection<ItemDelta<?, ?>> modifications) {
        this.modifications.addAll(modifications);
    }

    private void swallow(WorkBucketType bucket) {
        bucketsToAdd.add(bucket.cloneWithoutId());
    }

    private void swallowBucketsToAdd() {
        swallow(
                BucketOperation.bucketsAddDeltas(activityStateItemPath, bucketsToAdd));
    }

    @NotNull Collection<? extends ItemDelta<?, ?>> getModifications() {
        return modifications;
    }

    WorkBucketType getBucketToUse() {
        return bucketToUse;
    }

    Situation getSituationRequired() {
        return Objects.requireNonNull(situation, "no situation");
    }

    public boolean isDefinite() {
        return situation == Situation.NOTHING_MORE_DEFINITE;
    }

    enum Situation {
        FOUND_DELEGATED_TO_ME, FOUND_READY, CREATED_NEW, NOTHING_MORE_SOME_DELEGATED, NOTHING_MORE_DEFINITE
    }
}
