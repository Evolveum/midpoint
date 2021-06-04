/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil.getBuckets;

public class CompleteBucketOperation extends BucketOperation {

    private static final Trace LOGGER = TraceManager.getTrace(CompleteBucketOperation.class);

    private final int sequentialNumber;

    CompleteBucketOperation(BucketingManager bucketingManager, @NotNull String workerTaskOid,
            @NotNull ActivityPath activityPath, WorkBucketStatisticsCollector collector, int sequentialNumber) {
        super(workerTaskOid, activityPath, collector, bucketingManager);
        this.sequentialNumber = sequentialNumber;
    }

    public void execute(OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {

        loadTasks(result);
        LOGGER.trace("Completing work bucket #{} in {} (coordinator {})", sequentialNumber, workerTask, coordinatorTask);

        if (isStandalone()) {
            completeWorkBucketStandalone(result);
        } else {
            completeWorkBucketMultiNode(result);
        }
    }

    private void completeWorkBucketStandalone(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        ActivityStateType partWorkState = getWorkerTaskActivityWorkState();
        WorkBucketType bucket = TaskWorkStateUtil.findBucketByNumber(getBuckets(partWorkState), sequentialNumber);
        if (bucket == null) {
            throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + workerTask);
        }
        if (bucket.getState() != WorkBucketStateType.READY && bucket.getState() != null) {
            throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + workerTask
                    + " cannot be marked as complete, as it is not ready; its state = " + bucket.getState());
        }
        Collection<ItemDelta<?, ?>> modifications = bucketStateChangeDeltas(workerStatePath, bucket, WorkBucketStateType.COMPLETE);
        repositoryService.modifyObject(TaskType.class, workerTask.getOid(), modifications, null, result);
        workerTask.applyModificationsTransient(modifications);
        workerTask.applyDeltasImmediate(modifications, result);
        compressCompletedBuckets(workerTask, workerStatePath, result);
        statisticsKeeper.register(COMPLETE_WORK_BUCKET);
    }

    private void completeWorkBucketMultiNode(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        ActivityStateType workState = getCoordinatorTaskPartWorkState();
        WorkBucketType bucket = TaskWorkStateUtil.findBucketByNumber(getBuckets(workState), sequentialNumber);
        if (bucket == null) {
            throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + coordinatorTask);
        }
        if (bucket.getState() != WorkBucketStateType.DELEGATED) {
            throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + coordinatorTask
                    + " cannot be marked as complete, as it is not delegated; its state = " + bucket.getState());
        }
        checkWorkerRefOnDelegatedBucket(bucket);
        Collection<ItemDelta<?, ?>> modifications =
                bucketStateChangeDeltas(coordinatorStatePath, bucket, WorkBucketStateType.COMPLETE);
        try {
            ModifyObjectResult<TaskType> modifyObjectResult = repositoryService.modifyObject(TaskType.class,
                    coordinatorTask.getOid(), modifications, bucketUnchangedPrecondition(bucket), null, result);
            statisticsKeeper.addToConflictCounts(modifyObjectResult);
        } catch (PreconditionViolationException e) {
            throw new IllegalStateException("Unexpected concurrent modification of work bucket " + bucket + " in " + coordinatorTask, e);
        }
        coordinatorTask.applyModificationsTransient(modifications);
        compressCompletedBuckets(coordinatorTask, coordinatorStatePath, result);
        deleteBucketFromWorker(sequentialNumber, result);
        statisticsKeeper.register(COMPLETE_WORK_BUCKET);
    }

    private void compressCompletedBuckets(Task task, ItemPath statePath, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        List<WorkBucketType> buckets = new ArrayList<>(getBuckets(task.getWorkState(), activityPath));
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
            deleteItemDeltas.addAll(bucketDeleteDeltas(statePath, completeBuckets.get(i)));
        }
        LOGGER.trace("Compression of completed buckets: deleting {} buckets before last completed one in {}", deleteItemDeltas.size(), task);
        // these buckets should not be touched by anyone (as they are already completed); so we can execute without preconditions
        if (!deleteItemDeltas.isEmpty()) {
            ModifyObjectResult<TaskType> modifyObjectResult =
                    repositoryService.modifyObject(TaskType.class, task.getOid(), deleteItemDeltas, null, result);
            statisticsKeeper.addToConflictCounts(modifyObjectResult);
        }
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "sequentialNumber", sequentialNumber, indent + 1);
    }
}
