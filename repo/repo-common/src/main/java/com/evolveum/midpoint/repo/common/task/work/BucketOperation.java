/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityBucketingStateType.F_BUCKET;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType.F_BUCKETING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketStateType.DELEGATED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketStateType.READY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Represents a bucket operation (get, complete, release).
 */
class BucketOperation implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(BucketOperation.class);

    private static final String CONTENTION_LOG_NAME = BucketOperation.class.getName() + ".contention";
    static final Trace CONTENTION_LOGGER = TraceManager.getTrace(CONTENTION_LOG_NAME);

    /**
     * OID of the coordinator task. For standalone situations, this is the only task we work with.
     */
    @NotNull final String coordinatorTaskOid;

    /**
     * OID of the worker task.
     */
    @Nullable final String workerTaskOid;

    /**
     * Path of the activity for which buckets are managed.
     */
    @NotNull final ActivityPath activityPath;

    /**
     * Helper object used for statistics-keeping.
     */
    final BucketOperationStatisticsKeeper statisticsKeeper;

    // Useful beans

    final CommonTaskBeans beans;
    final TaskManager taskManager;
    final RepositoryService plainRepositoryService;
    final PrismContext prismContext;

    BucketOperation(@NotNull String coordinatorTaskOid, @Nullable String workerTaskOid, @NotNull ActivityPath activityPath,
            ActivityBucketManagementStatistics statistics, @NotNull CommonTaskBeans beans) {
        this.coordinatorTaskOid = coordinatorTaskOid;
        this.workerTaskOid = workerTaskOid;
        this.activityPath = activityPath;
        this.beans = beans;
        this.taskManager = beans.taskManager;
        this.plainRepositoryService = beans.plainRepositoryService;
        this.prismContext = beans.prismContext;
        this.statisticsKeeper = new BucketOperationStatisticsKeeper(statistics);
    }

    public boolean isStandalone() {
        return workerTaskOid == null;
    }

    static Collection<ItemDelta<?, ?>> bucketsAddDeltas(ItemPath statePath, List<WorkBucketType> buckets) throws SchemaException {
        return PrismContext.get().deltaFor(TaskType.class)
                .item(statePath.append(F_BUCKETING, F_BUCKET))
                .addRealValues(CloneUtil.cloneCollectionMembers(buckets)).asItemDeltas();
    }

    @SuppressWarnings("SameParameterValue")
    static List<ItemDelta<?, ?>> bucketStateChangeDeltas(ItemPath statePath, WorkBucketType bucket, WorkBucketStateType newState)
            throws SchemaException {
        return PrismContext.get().deltaFor(TaskType.class)
                .item(createBucketPath(statePath, bucket).append(WorkBucketType.F_STATE))
                .replace(newState).asItemDeltas();
    }

    @NotNull
    private static ItemPath createBucketPath(ItemPath statePath, WorkBucketType bucket) {
        return statePath.append(F_BUCKETING, F_BUCKET, bucket.getId());
    }

    @SuppressWarnings("SameParameterValue")
    static Collection<ItemDelta<?, ?>> bucketsStateChangeDeltas(ItemPath statePath, Collection<WorkBucketType> buckets,
            WorkBucketStateType newState, String workerOid) throws SchemaException {
        Collection<ItemDelta<?, ?>> deltas = new ArrayList<>();
        for (WorkBucketType bucket : buckets) {
            deltas.addAll(bucketStateChangeDeltas(statePath, bucket, newState, workerOid));
        }
        return deltas;
    }

    static Collection<ItemDelta<?, ?>> bucketStateChangeDeltas(@NotNull ItemPath statePath, @NotNull WorkBucketType bucket,
            @NotNull WorkBucketStateType newState, @Nullable String workerOid) throws SchemaException {
        ItemPath bucketPath = createBucketPath(statePath, bucket);
        Collection<?> workerRefs = workerOid != null ?
                singletonList(new ObjectReferenceType().oid(workerOid).type(TaskType.COMPLEX_TYPE)) : emptyList();

        return PrismContext.get().deltaFor(TaskType.class)
                .item(bucketPath.append(WorkBucketType.F_STATE)).replace(newState)
                .item(bucketPath.append(WorkBucketType.F_WORKER_REF)).replaceRealValues(workerRefs)
                .asItemDeltas();
    }

    Collection<ItemDelta<?, ?>> bucketDeleteDeltas(ItemPath statePath, WorkBucketType bucket) throws SchemaException {
        return prismContext.deltaFor(TaskType.class)
                .item(statePath.append(F_BUCKETING, F_BUCKET))
                .delete(bucket.clone()).asItemDeltas();
    }

    void checkBucketReadyOrDelegated(@NotNull WorkBucketType bucket) {
        if (workerTaskOid != null) {
            stateCheck(bucket.getState() == DELEGATED, "Bucket %s is not delegated", bucket);
            checkWorkerRefOnDelegatedBuckets(bucket);
        } else {
            stateCheck(bucket.getState() == null || bucket.getState() == READY, "Bucket %s is not ready", bucket);
        }
    }

    private void checkWorkerRefOnDelegatedBuckets(WorkBucketType bucket) {
        assert workerTaskOid != null;
        if (bucket.getWorkerRef() == null) {
            LOGGER.warn("DELEGATED bucket without workerRef: {}", bucket);
        } else if (!workerTaskOid.equals(bucket.getWorkerRef().getOid())) {
            LOGGER.warn("DELEGATED bucket with workerRef ({}) different from the current worker task ({}): {}",
                    bucket.getWorkerRef().getOid(), workerTaskOid, bucket);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Coordinator task OID", coordinatorTaskOid, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Worker task OID", workerTaskOid, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Activity path", String.valueOf(activityPath), indent + 1);
        extendDebugDump(sb, indent);
        return sb.toString();
    }

    protected void extendDebugDump(StringBuilder sb, int indent) {
    }
}
