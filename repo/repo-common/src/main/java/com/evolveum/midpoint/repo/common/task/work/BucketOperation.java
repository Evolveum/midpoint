/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import static com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketingStateType.F_BUCKET;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityWorkStateType.F_BUCKETING;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Represents a bucket operation (get, complete, release).
 */
class BucketOperation implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(BucketOperation.class);

    private static final String CONTENTION_LOG_NAME = BucketOperation.class.getName() + ".contention";
    static final Trace CONTENTION_LOGGER = TraceManager.getTrace(CONTENTION_LOG_NAME);

    static final String GET_WORK_BUCKET_FOUND_SELF_ALLOCATED = "getWorkBucket.foundSelfAllocated";
    static final String GET_WORK_BUCKET_CREATED_NEW = "getWorkBucket.createdNew";
    static final String GET_WORK_BUCKET_DELEGATED = "getWorkBucket.delegated";
    static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_DEFINITE = "getWorkBucket.noMoreBucketsDefinite";
    static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_NOT_SCAVENGER = "getWorkBucket.noMoreBucketsNotScavenger";
    static final String GET_WORK_BUCKET_NO_MORE_BUCKETS_WAIT_TIME_ELAPSED = "getWorkBucket.NoMoreBucketsWaitTimeElapsed";
    static final String COMPLETE_WORK_BUCKET = "completeWorkBucket";
    static final String RELEASE_WORK_BUCKET = "releaseWorkBucket";

    /** TODO */
    @NotNull final ActivityPath activityPath;

    /**
     * OID of the worker task. For standalone situations, this is the only task we work with.
     */
    @NotNull final String workerTaskOid;

    /**
     * Loaded worker task. Shouldn't be null.
     */
    Task workerTask;

    /** Path to work state in the worker task. */
    ItemPath workerStatePath;

    /**
     * Loaded coordinator task. It is null for standalone worker tasks.
     */
    Task coordinatorTask;

    /** Path to work state in the coordinator task. */
    ItemPath coordinatorStatePath;

    /**
     * Helper object used for statistics-keeping.
     */
    final BucketOperationStatisticsKeeper statisticsKeeper;

    // Useful beans

    final WorkStateManager workStateManager;
    final TaskManager taskManager;
    final RepositoryService repositoryService;
    final PrismContext prismContext;

    BucketOperation(@NotNull String workerTaskOid, @NotNull ActivityPath activityPath,
            WorkBucketStatisticsCollector collector, WorkStateManager workStateManager) {
        this.workStateManager = workStateManager;
        this.taskManager = workStateManager.getTaskManager();
        this.repositoryService = workStateManager.getRepositoryService();
        this.prismContext = workStateManager.getPrismContext();
        this.workerTaskOid = workerTaskOid;
        this.activityPath = activityPath;
        this.statisticsKeeper = new BucketOperationStatisticsKeeper(collector);
    }

    public boolean isStandalone() {
        return TaskWorkStateUtil.isStandalone(workerTask.getWorkState(), workerStatePath);
    }

    private BucketsProcessingRoleType getWorkerTaskRole() {
        return TaskWorkStateUtil.getBucketsProcessingRole(workerTask.getWorkState(), workerStatePath);
    }

    private BucketsProcessingRoleType getCoordinatorTaskRole() {
        return TaskWorkStateUtil.getBucketsProcessingRole(coordinatorTask.getWorkState(), coordinatorStatePath);
    }

    void loadTasks(OperationResult result) throws ObjectNotFoundException, SchemaException {
        workerTask = taskManager.getTaskPlain(workerTaskOid, result);
        workerStatePath = TaskWorkStateUtil.getWorkStatePath(workerTask.getWorkState(), activityPath);

        BucketsProcessingRoleType workerRole = getWorkerTaskRole();
        if (workerRole == BucketsProcessingRoleType.WORKER) {
            loadCoordinatorTask(result);
        } else if (workerRole == null || workerRole == BucketsProcessingRoleType.STANDALONE) {
            coordinatorTask = null;
        } else {
            throw new IllegalStateException("Wrong bucket processing role for worker task " + workerTask + ": " + workerRole);
        }
    }

    private void loadCoordinatorTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
        coordinatorTask = workerTask.getParentTask(result);
        coordinatorStatePath = TaskWorkStateUtil.getWorkStatePath(coordinatorTask.getWorkState(), activityPath);

        stateCheck(coordinatorTask != null, "No coordinator task for worker task %s", workerTask);
        stateCheck(getCoordinatorTaskRole() == BucketsProcessingRoleType.COORDINATOR,
                "Coordinator task for worker task %s is not marked as such: %s", workerTask, coordinatorTask);
    }

    List<WorkBucketType> getWorkerTaskBuckets() {
        return getBuckets(getWorkerTaskActivityWorkState());
    }

    @NotNull
    private WorkDistributionType getCoordinatorWorkManagement() {
        return requireNonNull(
                TaskWorkStateUtil.getWorkDistribution(coordinatorTask.getRootActivityDefinitionOrClone(),
                        getCurrentActivityId(coordinatorTask.getWorkState())),
                () -> "No work management for the current part in coordinator task " + coordinatorTask);
    }

    @NotNull WorkBucketsManagementType getCoordinatorBucketingConfig() {
        return requireNonNull(
                getCoordinatorWorkManagement().getBuckets(),
                () -> "No bucketing configuration for the current part in coordinator task " + coordinatorTask);
    }

    @NotNull ActivityWorkStateType getWorkerTaskActivityWorkState() {
        return getTaskActivityWorkState(workerTask);
    }

    @NotNull ActivityWorkStateType getCoordinatorTaskPartWorkState() {
        return getTaskActivityWorkState(coordinatorTask);
    }

    @NotNull
    ActivityWorkStateType getTaskActivityWorkState(Task workerTask) {
        return requireNonNull(TaskWorkStateUtil.getActivityWorkStateRequired(workerTask.getWorkState(), workerStatePath),
                () -> "No current part work state in " + workerTask +
                        " (activity path: " + activityPath + ", item path: " + workerStatePath);
    }

    Collection<ItemDelta<?, ?>> bucketsReplaceDeltas(ItemPath statePath, List<WorkBucketType> buckets) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(statePath.append(F_BUCKETING, F_BUCKET))
                .replaceRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
    }

    Collection<ItemDelta<?, ?>> bucketsAddDeltas(ItemPath statePath, List<WorkBucketType> buckets) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(statePath.append(F_BUCKETING, F_BUCKET))
                .addRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
    }

    @SuppressWarnings("SameParameterValue")
    Collection<ItemDelta<?, ?>> bucketStateChangeDeltas(ItemPath statePath, WorkBucketType bucket, WorkBucketStateType newState)
            throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(createBucketPath(statePath, bucket).append(WorkBucketType.F_STATE))
                .replace(newState).asItemDeltas();
    }

    @NotNull
    private ItemPath createBucketPath(ItemPath statePath, WorkBucketType bucket) {
        return statePath.append(F_BUCKETING, F_BUCKET, bucket.getId());
    }

    Collection<ItemDelta<?, ?>> bucketStateChangeDeltas(ItemPath statePath, WorkBucketType bucket, WorkBucketStateType newState,
            String workerOid) throws SchemaException {
        ItemPath bucketPath = createBucketPath(statePath, bucket);
        Collection<?> workerRefs = workerOid != null ?
                singletonList(new ObjectReferenceType().oid(workerOid).type(TaskType.COMPLEX_TYPE)) : emptyList();

        return prismContext.deltaFor(TaskType.class)
                .item(bucketPath.append(WorkBucketType.F_STATE)).replace(newState)
                .item(bucketPath.append(WorkBucketType.F_WORKER_REF)).replaceRealValues(workerRefs)
                .asItemDeltas();
    }

    Collection<ItemDelta<?, ?>> bucketDeleteDeltas(ItemPath statePath, WorkBucketType bucket) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(statePath.append(F_BUCKETING, F_BUCKET))
                .delete(bucket.clone()).asItemDeltas();
    }

    ModificationPrecondition<TaskType> bucketUnchangedPrecondition(WorkBucketType originalBucket) {
        return taskObject -> {
            WorkBucketType currentBucket = findBucketByNumber(
                    getBuckets(taskObject.asObjectable().getWorkState(), activityPath),
                    originalBucket.getSequentialNumber());
            // performance is not optimal but OK for precondition checking
            return currentBucket != null && cloneNoId(currentBucket).equals(cloneNoId(originalBucket));
        };
    }

    void deleteBucketFromWorker(int sequentialNumber, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ObjectAlreadyExistsException {
        ActivityWorkStateType workState = getWorkerTaskActivityWorkState();
        WorkBucketType workerBucket = TaskWorkStateUtil.findBucketByNumber(getBuckets(workState), sequentialNumber);
        if (workerBucket == null) {
            throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber +
                    " in worker task " + workerTask);
        }
        repositoryService.modifyObject(TaskType.class, workerTask.getOid(),
                bucketDeleteDeltas(workerStatePath, workerBucket), result);
    }

    void checkWorkerRefOnDelegatedBucket(WorkBucketType bucket) {
        if (bucket.getWorkerRef() == null) {
            LOGGER.warn("DELEGATED bucket without workerRef: {}", bucket);
        } else if (!workerTask.getOid().equals(bucket.getWorkerRef().getOid())) {
            LOGGER.warn("DELEGATED bucket with workerRef ({}) different from the current worker task ({}): {}",
                    bucket.getWorkerRef().getOid(), workerTask, bucket);
        }
    }

    private WorkBucketType cloneNoId(WorkBucketType bucket) {
        return bucket.clone().id(null);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Activity path", String.valueOf(activityPath), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Worker task OID", workerTaskOid, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Worker task", workerTask, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Worker activity state path", String.valueOf(workerStatePath), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Coordinator task", coordinatorTask, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Coordinator activity state path", String.valueOf(coordinatorStatePath), indent + 1);
        extendDebugDump(sb, indent);
        return sb.toString();
    }

    protected void extendDebugDump(StringBuilder sb, int indent) {
    }
}
