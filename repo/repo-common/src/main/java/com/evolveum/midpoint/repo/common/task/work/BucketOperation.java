/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil.findBucketByNumber;

import static com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil.getCurrentActivityId;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Represents a bucket operation (get, complete, release).
 */
class BucketOperation {

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

    /**
     * OID of the worker task. For standalone situations, this is the only task we work with.
     */
    @NotNull final String workerTaskOid;

    /**
     * Loaded worker task. Shouldn't be null.
     */
    Task workerTask;

    /** Container ID of the part work state in the worker task. */
    Long workerPartPcvId;

    /**
     * Loaded coordinator task. It is null for standalone worker tasks.
     */
    Task coordinatorTask;

    /** Container ID of the part work state in the coordinator task. */
    Long coordinatorPartPcvId;

    /**
     * Helper object used for statistics-keeping.
     */
    final BucketOperationStatisticsKeeper statisticsKeeper;

    // Useful beans

    final WorkStateManager workStateManager;
    final TaskManager taskManager;
    final RepositoryService repositoryService;
    final PrismContext prismContext;

    BucketOperation(@NotNull String workerTaskOid, WorkBucketStatisticsCollector collector, WorkStateManager workStateManager) {
        this.workStateManager = workStateManager;
        this.taskManager = workStateManager.getTaskManager();
        this.repositoryService = workStateManager.getRepositoryService();
        this.prismContext = workStateManager.getPrismContext();
        this.workerTaskOid = workerTaskOid;
        this.statisticsKeeper = new BucketOperationStatisticsKeeper(collector);
    }

    public boolean isStandalone() {
        return TaskWorkStateUtil.isStandalone(workerTask.getWorkState());
    }

    private BucketsProcessingRoleType getWorkerTaskRole() {
        return TaskWorkStateUtil.getBucketsProcessingRole(workerTask.getWorkState());
    }

    private BucketsProcessingRoleType getCoordinatorTaskRole() {
        return TaskWorkStateUtil.getBucketsProcessingRole(coordinatorTask.getWorkState());
    }

    void loadTasks(OperationResult result, boolean assumePartWorkStateInWorker) throws ObjectNotFoundException, SchemaException {
        workerTask = taskManager.getTaskPlain(workerTaskOid, result);

        if (assumePartWorkStateInWorker) {
            setupPartWorkStatePcvIdInWorkerTaskRequired();
        } else {
            findOrCreatePartWorkStateInWorkerTask(result);
        }
        assert workerPartPcvId != null;

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
        stateCheck(coordinatorTask != null, "No coordinator task for worker task %s", workerTask);
        stateCheck(getCoordinatorTaskRole() == BucketsProcessingRoleType.COORDINATOR,
                "Coordinator task for worker task %s is not marked as such: %s", workerTask, coordinatorTask);
        setupPartWorkStatePcvIdInCoordinatorTaskRequired();
    }

    List<WorkBucketType> getWorkerTaskBuckets() {
        return TaskWorkStateUtil.getBuckets(workerTask.getWorkState());
    }

    ActivityDefinitionType getWorkerTaskPartDefinition() {
        return TaskWorkStateUtil.getPartDefinition(workerTask.getActivityDefinitionOrClone(), workerTask.getCurrentPartId());
    }

    ActivityDefinitionType getCoordinatorTaskPartDefinition() {
        return TaskWorkStateUtil.getPartDefinition(coordinatorTask.getActivityDefinitionOrClone(), coordinatorTask.getCurrentPartId());
    }

    @NotNull
    private WorkDistributionType getCoordinatorWorkManagement() {
        return requireNonNull(
                TaskWorkStateUtil.getWorkDistribution(coordinatorTask.getActivityDefinitionOrClone(),
                        getCurrentActivityId(coordinatorTask.getWorkState())),
                () -> "No work management for the current part in coordinator task " + coordinatorTask);
    }

    @NotNull WorkBucketsManagementType getCoordinatorBucketingConfig() {
        return requireNonNull(
                getCoordinatorWorkManagement().getBuckets(),
                () -> "No bucketing configuration for the current part in coordinator task " + coordinatorTask);
    }

    @NotNull TaskPartWorkStateType getWorkerTaskPartWorkStateRequired() {
        return requireNonNull(TaskWorkStateUtil.getCurrentPartWorkState(workerTask.getWorkState()),
                () -> "No current part work state in " + workerTask);
    }

    @NotNull TaskPartWorkStateType getCoordinatorTaskPartWorkStateRequired() {
        return requireNonNull(TaskWorkStateUtil.getCurrentPartWorkState(coordinatorTask.getWorkState()),
                () -> "No current part work state in coordinator task " + coordinatorTask);
    }

    Collection<ItemDelta<?, ?>> bucketsReplaceDeltas(long pcvId, List<WorkBucketType> buckets) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_PART, pcvId, TaskPartWorkStateType.F_BUCKET)
                .replaceRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
    }

    Collection<ItemDelta<?, ?>> bucketsAddDeltas(long pcvId, List<WorkBucketType> buckets) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_PART, pcvId, TaskPartWorkStateType.F_BUCKET)
                .addRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
    }

    @SuppressWarnings("SameParameterValue")
    Collection<ItemDelta<?, ?>> bucketStateChangeDeltas(long pcvId, WorkBucketType bucket, WorkBucketStateType newState)
            throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(createBucketPath(pcvId, bucket).append(WorkBucketType.F_STATE))
                .replace(newState).asItemDeltas();
    }

    @NotNull
    private ItemPath createBucketPath(long pcvId, WorkBucketType bucket) {
        return ItemPath.create(TaskType.F_WORK_STATE, TaskWorkStateType.F_PART, pcvId, TaskPartWorkStateType.F_BUCKET, bucket.getId());
    }

    Collection<ItemDelta<?, ?>> bucketStateChangeDeltas(long pcvId, WorkBucketType bucket, WorkBucketStateType newState,
            String workerOid) throws SchemaException {
        ItemPath bucketPath = createBucketPath(pcvId, bucket);
        Collection<?> workerRefs = workerOid != null ?
                singletonList(new ObjectReferenceType().oid(workerOid).type(TaskType.COMPLEX_TYPE)) : emptyList();

        return prismContext.deltaFor(TaskType.class)
                .item(bucketPath.append(WorkBucketType.F_STATE)).replace(newState)
                .item(bucketPath.append(WorkBucketType.F_WORKER_REF)).replaceRealValues(workerRefs)
                .asItemDeltas();
    }

    Collection<ItemDelta<?, ?>> bucketDeleteDeltas(long pcvId, WorkBucketType bucket) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_PART, pcvId, TaskPartWorkStateType.F_BUCKET)
                .delete(bucket.clone()).asItemDeltas();
    }

    ModificationPrecondition<TaskType> bucketUnchangedPrecondition(WorkBucketType originalBucket) {
        return taskObject -> {
            WorkBucketType currentBucket = findBucketByNumber(
                    TaskWorkStateUtil.getBuckets(taskObject.asObjectable().getWorkState()),
                    originalBucket.getSequentialNumber());
            // performance is not optimal but OK for precondition checking
            return currentBucket != null && cloneNoId(currentBucket).equals(cloneNoId(originalBucket));
        };
    }

    void deleteBucketFromWorker(int sequentialNumber, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ObjectAlreadyExistsException {
        TaskPartWorkStateType workState = getWorkerTaskPartWorkStateRequired();
        WorkBucketType workerBucket = TaskWorkStateUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
        if (workerBucket == null) {
            throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber +
                    " in worker task " + workerTask);
        }
        repositoryService.modifyObject(TaskType.class, workerTask.getOid(),
                bucketDeleteDeltas(workerPartPcvId, workerBucket), result);
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

    private void setupPartWorkStatePcvIdInWorkerTaskRequired() {
        TaskPartWorkStateType partWorkState = getWorkerTaskPartWorkStateRequired();
        stateCheck(partWorkState.getId() != null, "Null part work state id in %s: %s", workerTask, partWorkState);
        workerPartPcvId = partWorkState.getId();
    }

    private void setupPartWorkStatePcvIdInCoordinatorTaskRequired() {
        TaskPartWorkStateType partWorkState = getCoordinatorTaskPartWorkStateRequired();
        stateCheck(partWorkState.getId() != null, "Null part work state id in coordinator task %s: %s",
                coordinatorTask, partWorkState);
        coordinatorPartPcvId = partWorkState.getId();
    }

    private void findOrCreatePartWorkStateInWorkerTask(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Long pcvId = findPartWorkStatePcvIdInWorkerTask();
        if (pcvId != null) {
            LOGGER.trace("findOrCreatePartWorkStateInWorkerTask: found existing part with PCV ID {}", pcvId);
            workerPartPcvId = pcvId;
            return;
        }

        createPartWorkStateInWorkerTask(result);
        loadTasks(result, true);
    }

    private void createPartWorkStateInWorkerTask(OperationResult result) throws SchemaException, ObjectNotFoundException {
        String currentActivityId = getCurrentActivityId(workerTask.getWorkState());
        LOGGER.trace("No activity work state found for activity id {}, creating new one", currentActivityId);

        TaskPartWorkStateType newPartWorkState =
                new TaskPartWorkStateType(prismContext)
                        .partId(currentActivityId)
                        .bucketsProcessingRole(BucketsProcessingRoleType.STANDALONE);
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE, TaskWorkStateType.F_PART).add(newPartWorkState)
                .asItemDeltas();
        try {
            repositoryService.modifyObject(TaskType.class, workerTaskOid, itemDeltas, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected already exists exception: " + e.getMessage(), e);
        }
    }

    @Nullable
    private Long findPartWorkStatePcvIdInWorkerTask() {
        TaskWorkStateType workState = workerTask.getWorkState();
        String currentPartId = getCurrentActivityId(workState);
        TaskPartWorkStateType partWorkState = TaskWorkStateUtil.getPartWorkState(workState, currentPartId);
        if (partWorkState != null) {
            stateCheck(partWorkState.getId() != null, "Null part work state id in %s: %s", workerTask, partWorkState);
            return partWorkState.getId();
        } else {
            return null;
        }
    }
}
