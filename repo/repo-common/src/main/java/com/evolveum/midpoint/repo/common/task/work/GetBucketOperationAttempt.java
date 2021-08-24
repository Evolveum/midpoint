/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import static com.evolveum.midpoint.repo.common.task.work.BucketOperation.bucketStateChangeDeltas;
import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.getActivityStateRequired;
import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.getStateItemPath;
import static com.evolveum.midpoint.schema.util.task.BucketingUtil.getNumberOfBuckets;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketingStateType.F_NUMBER_OF_BUCKETS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType.F_BUCKETING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketStateType.DELEGATED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator.Response.FoundExisting;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator.Response.NewBuckets;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketAllocator.Response.NothingFound;
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
     * The current value of the coordinator (i.e. buckets-holding) task.
     */
    @NotNull private final TaskType task;

    /**
     * OID of the worker task (null iff standalone).
     */
    @Nullable private final String workerOid;

    /**
     * Item path for the current bucketed activity state.
     */
    @NotNull private final ItemPath activityStateItemPath;

    /**
     * Current state of the activity.
     */
    @NotNull private final ActivityStateType activityState;

    /**
     * Current list of buckets. It is not updated directly in this class!
     */
    @NotNull private final List<WorkBucketType> buckets;

    /**
     * Configured allocator that generates buckets.
     */
    @NotNull private final BucketAllocator bucketAllocator;

    /**
     * Modifications to be applied to the task. After they are applied successfully,
     * the buckets are considered to be obtained. (If the application fails because of
     * concurrency, the "get bucket" operation is retried in another attempt.)
     */
    @NotNull private final Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();

    /**
     * If present, we have found a bucket that was already delegated. So no allocation was needed to do;
     * and no modifications were needed either.
     */
    private WorkBucketType alreadyDelegatedBucket;

    /**
     * Response of the bucket allocator. Null if the allocator was not involved.
     */
    private BucketAllocator.Response allocatorResponse;

    GetBucketOperationAttempt(@NotNull TaskType task, @Nullable String workerOid, @NotNull ActivityPath activityPath,
            @NotNull BucketAllocator bucketAllocator) {
        this.task = task;
        this.workerOid = workerOid;
        this.activityStateItemPath = getStateItemPath(task.getActivityState(), activityPath);
        this.activityState = getActivityStateRequired(task.getActivityState(), activityStateItemPath);
        this.buckets = BucketingUtil.getBuckets(activityState);
        this.bucketAllocator = bucketAllocator;
    }

    /**
     * Obtains a bucket. This method can be called from {@link RepositoryService#modifyObjectDynamically(Class, String, Collection, RepositoryService.ModificationsSupplier, RepoModifyOptions, OperationResult)}
     * method (in case of coordinator-workers scenario), or simply as part of `getObject` - compute changes - `modifyObject`
     * process (in case of standalone scenario).
     */
    void execute() throws SchemaException {

        if (workerOid != null) {
            Optional<WorkBucketType> delegatedToWorker = buckets.stream()
                    .filter(b -> BucketingUtil.isDelegatedTo(b, workerOid))
                    .findAny(); // usually at most one
            if (delegatedToWorker.isPresent()) {
                alreadyDelegatedBucket = delegatedToWorker.get();
                return;
            }
        }

        setOrUpdateEstimatedNumberOfBuckets();

        allocatorResponse = bucketAllocator.getBucket(buckets);
        LOGGER.trace("Bucket allocator returned {} for task {}, worker {}", allocatorResponse, task, workerOid);

        if (allocatorResponse instanceof NewBuckets) {
            handleCreatedNew();
        } else if (allocatorResponse instanceof FoundExisting) {
            handleFoundExisting();
        } else if (allocatorResponse instanceof NothingFound) {
            // Nothing to do for now.
        } else {
            throw new AssertionError(allocatorResponse);
        }
    }

    private void handleCreatedNew() throws SchemaException {
        NewBuckets newBucketsResponse = (NewBuckets) allocatorResponse;
        List<WorkBucketType> bucketsToAdd = new ArrayList<>();
        for (int i = 0; i < newBucketsResponse.newBuckets.size(); i++) {
            WorkBucketType newBucket = newBucketsResponse.newBuckets.get(i);
            if (workerOid != null) {
                bucketsToAdd.add(newBucket.clone() // maybe cloning not even needed here
                        .state(DELEGATED)
                        .workerRef(workerOid, TaskType.COMPLEX_TYPE));
            } else {
                bucketsToAdd.add(newBucket);
            }
        }
        modifications.addAll(
                BucketOperation.bucketsAddDeltas(activityStateItemPath, bucketsToAdd));
    }

    private void handleFoundExisting() throws SchemaException {
        if (workerOid != null) {
            FoundExisting foundExistingResponse = (FoundExisting) allocatorResponse;
            modifications.addAll(
                    bucketStateChangeDeltas(activityStateItemPath, foundExistingResponse.bucket, DELEGATED, workerOid));
        } else {
            // bucket is READY, no changes are needed
        }
    }

    private void setOrUpdateEstimatedNumberOfBuckets() throws SchemaException {
        Integer number = bucketAllocator.estimateNumberOfBuckets();
        if (number != null && !number.equals(getNumberOfBuckets(activityState))) {
            List<ItemDelta<?, ?>> numberOfBucketsMods = PrismContext.get().deltaFor(TaskType.class)
                    .item(activityStateItemPath.append(F_BUCKETING, F_NUMBER_OF_BUCKETS))
                    .replace(number)
                    .asItemDeltas();
            LOGGER.trace("Going to set # of buckets:\n{}", DebugUtil.debugDumpLazily(numberOfBucketsMods, 1));
            modifications.addAll(numberOfBucketsMods);
        }
    }

    @NotNull Collection<? extends ItemDelta<?, ?>> getModifications() {
        return modifications;
    }

    WorkBucketType getAlreadyDelegatedBucket() {
        return alreadyDelegatedBucket;
    }

    BucketAllocator.Response getAllocatorResponse() {
        return allocatorResponse;
    }
}
