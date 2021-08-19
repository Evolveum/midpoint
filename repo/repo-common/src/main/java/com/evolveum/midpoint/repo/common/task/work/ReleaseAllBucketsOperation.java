/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.getActivityStateRequired;
import static com.evolveum.midpoint.schema.util.task.BucketingUtil.getBuckets;
import static com.evolveum.midpoint.schema.util.task.BucketingUtil.isDelegatedTo;

import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.activity.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.schema.util.task.work.BucketingConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReleaseAllBucketsOperation extends BucketOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ReleaseAllBucketsOperation.class);

    @NotNull private final Task preloadedWorkerTask;

    ReleaseAllBucketsOperation(@NotNull Task workerTask, @NotNull ActivityPath activityPath,
            ActivityBucketManagementStatistics collector, CommonTaskBeans beans) {
        super(workerTask.getOid(), activityPath, collector, beans);
        this.preloadedWorkerTask = workerTask;
    }

    public void execute(OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        loadTasksWithPreloadedWorker(preloadedWorkerTask, result);
        LOGGER.trace("Releasing all buckets in {} (coordinator {})", workerTask, coordinatorTask);

        if (isStandalone()) {
            throw new UnsupportedOperationException("Cannot release work bucket from standalone task " + workerTask);
        } else {
            releaseAllBucketsMultiNode(result);
        }
    }

    /**
     * We first delete ready buckets from the worker. Only after successful execution, we mark these as ready in the coordinator.
     *
     * Is it possible to have a collision with this task being resumed and somehow executing these buckets? Yes.
     *
     * 1. [This thread] Makes buckets ready (causing inconsistency between worker and coordinator where buckets
     * are still marked as delegated)
     * 2. [Competing thread] Starts the worker, obtains already-delegated bucket, re-adding it as ready
     * 3. [This thread] Marks buckets in the coordinator as ready.
     * 4. [Competing thread] Fails when marking buckets as complete in the coordinator. (Or another worker processes them,
     * failing in the same way.)
     *
     * The result is that buckets are now ready in both coordinator and this worker task.
     *
     * What to do?
     *
     * 1. Alternative 1: Use multi-object transactions.
     *
     * 2. Alternative 2: Disallow execution of this task until the whole process is finished.
     * But the only serious solution would be to have a DB transaction covering modification of both tasks.
     *
     * 3. Alternative 3: Use Quartz job exclusion mechanism and create a special trigger for the worker task
     * that would run this "release all buckets" code. The positive side is that there will be no conflict with the real
     * execution of the task. The downside is that there are no guarantees of running this code. (It is not a big problem
     * if the code would not run at all. But it could run at some inappropriate time.)
     */
    private void releaseAllBucketsMultiNode(OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        List<WorkBucketType> removedBuckets = new ArrayList<>();

        try {
            plainRepositoryService.modifyObjectDynamically(TaskType.class, workerTaskOid, null, currentWorker -> {
                removedBuckets.clear();
                if (currentWorker.getSchedulingState() == TaskSchedulingStateType.SUSPENDED) {
                    removedBuckets.addAll(
                            BucketingUtil.getReadyBuckets(
                                    getActivityStateRequired(currentWorker.getActivityState(), workerStatePath)));
                } else {
                    LOGGER.warn("Couldn't release all buckets from worker, because it is (no longer) suspended: {}",
                            currentWorker);
                }
                return bucketsDeleteDeltas(workerStatePath, removedBuckets);
            }, null, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException(e);
        }

        LOGGER.info("Buckets removed from the worker task:\n{}", DebugUtil.debugDumpLazily(removedBuckets, 1)); // todo

        List<WorkBucketType> coordinatorBucketsToRelease = getCoordinatorBucketsToRelease(removedBuckets);

        LOGGER.info("Coordinator buckets to release:\n{}", DebugUtil.debugDumpLazily(coordinatorBucketsToRelease, 1)); // todo

        if (!coordinatorBucketsToRelease.isEmpty()) {
            try {
                plainRepositoryService.modifyObject(TaskType.class, coordinatorTask.getOid(),
                        bucketsStateChangeDeltas(coordinatorStatePath, coordinatorBucketsToRelease,
                                WorkBucketStateType.READY, null),
                        bucketsUnchangedPrecondition(coordinatorBucketsToRelease), null, result);
            } catch (PreconditionViolationException e) {
                // just for sure
                throw new IllegalStateException("Unexpected concurrent modification when releasing buckets "
                        + coordinatorBucketsToRelease + " in " + coordinatorTask, e);
            } catch (ObjectAlreadyExistsException e) {
                throw new SystemException(e);
            }
        }
        statisticsKeeper.register(BucketingConstants.RELEASE_ALL_WORK_BUCKETS);
    }

    private List<WorkBucketType> getCoordinatorBucketsToRelease(List<WorkBucketType> removedFromWorker) {
        List<WorkBucketType> allCoordinatorBuckets = getBuckets(getCoordinatorTaskActivityState());
        List<WorkBucketType> delegatedToWorker = allCoordinatorBuckets.stream()
                .filter(coordinatorBucket -> isDelegatedTo(coordinatorBucket, workerTaskOid))
                .collect(Collectors.toList());

        Set<Integer> removedBucketsNumbers = BucketingUtil.getSequentialNumbers(removedFromWorker);
        Set<Integer> delegatedBucketsNumbers = BucketingUtil.getSequentialNumbers(delegatedToWorker);
        if (MiscUtil.unorderedCollectionEquals(removedBucketsNumbers, delegatedBucketsNumbers)) {
            return delegatedToWorker;
        }

        LOGGER.warn("Buckets currently delegated to the worker task are not the same as buckets that were just removed from it."
                        + " Delegated: {}, Just removed: {}, Worker task: {}, Coordinator task: {}", delegatedBucketsNumbers,
                removedBucketsNumbers, workerTask, coordinatorTask);

        // We return only those delegated buckets that have their numbers present in the set of removed buckets.
        return delegatedToWorker.stream()
                .filter(bucket -> removedBucketsNumbers.contains(bucket.getSequentialNumber()))
                .collect(Collectors.toList());
    }
}
