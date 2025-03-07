/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.schema.util.task.work.BucketingConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class CompleteBucketOperation extends BucketOperation {

    private static final Trace LOGGER = TraceManager.getTrace(CompleteBucketOperation.class);

    private final int sequentialNumber;

    CompleteBucketOperation(@NotNull String coordinatorTaskOid, @Nullable String workerTaskOid,
            @NotNull ActivityPath activityPath, ActivityBucketManagementStatistics collector,
            @Nullable Consumer<BucketProgressOverviewType> bucketProgressConsumer, CommonTaskBeans beans,
            int sequentialNumber) {
        super(coordinatorTaskOid, workerTaskOid, activityPath, collector, bucketProgressConsumer, beans);
        this.sequentialNumber = sequentialNumber;
    }

    public void execute(OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {

        LOGGER.trace("Completing work bucket #{} in {} (worker {})", sequentialNumber, coordinatorTaskOid, workerTaskOid);
        ModifyObjectResult<TaskType> modifyObjectResult =
                plainRepositoryService.modifyObjectDynamically(TaskType.class, coordinatorTaskOid, null,
                        this::computeCompletionModifications, null, result);
        bucketProgressHolder.passValue();
        statisticsKeeper.addToConflictCounts(modifyObjectResult);
        statisticsKeeper.register(BucketingConstants.COMPLETE_WORK_BUCKET);
    }

    private @NotNull Collection<ItemDelta<?, ?>> computeCompletionModifications(@NotNull TaskType existingTask) {

        var task = existingTask.clone(); // todo check if the code below can change the data
        ActivityStateType activityState = ActivityStateUtil.getActivityStateRequired(task.getActivityState(), activityPath);
        ActivityBucketingStateType bucketing = activityState.getBucketing();
        List<WorkBucketType> buckets = CloneUtil.cloneCollectionMembers(bucketing.getBucket());

        WorkBucketType bucket = BucketingUtil.findBucketByNumberRequired(buckets, sequentialNumber);
        checkBucketReadyOrDelegated(bucket);

        bucketProgressHolder.accept(
                new BucketProgressOverviewType()
                        .totalBuckets(bucketing.getNumberOfBuckets())
                        .completeBuckets(BucketingUtil.getCompleteBucketsNumber(buckets) + 1));

        ItemPath statePath = ActivityStateUtil.getStateItemPath(task.getActivityState(), activityPath);
        List<ItemDelta<?, ?>> closingMods = bucketStateChangeDeltas(statePath, bucket, WorkBucketStateType.COMPLETE);

        WorkBucketType bucketBeforeCompletion = bucket.clone();
        bucket.setState(WorkBucketStateType.COMPLETE); // needed for compressing buckets

        Holder<Boolean> recentlyClosedBucketDeleted = new Holder<>();
        List<ItemDelta<?, ?>> compressingMods =
                compressCompletedBuckets(statePath, buckets, bucketBeforeCompletion, recentlyClosedBucketDeleted);

        if (Boolean.TRUE.equals(recentlyClosedBucketDeleted.getValue())) {
            return compressingMods;
        } else {
            return ListUtils.union(
                    closingMods,
                    compressingMods);
        }
    }
    private List<ItemDelta<?, ?>> compressCompletedBuckets(ItemPath statePath, List<WorkBucketType> currentBuckets,
            WorkBucketType closedBucketBefore, Holder<Boolean> recentlyClosedBucketDeletedHolder) {

        recentlyClosedBucketDeletedHolder.setValue(false);

        List<WorkBucketType> buckets = new ArrayList<>(currentBuckets);
        BucketingUtil.sortBucketsBySequentialNumber(buckets);
        List<WorkBucketType> completeBuckets = buckets.stream()
                .filter(b -> b.getState() == WorkBucketStateType.COMPLETE)
                .collect(Collectors.toList());
        if (completeBuckets.size() <= 1) {
            LOGGER.trace("Compression of completed buckets: # of complete buckets is too small ({}) in {}, exiting",
                    completeBuckets.size(), coordinatorTaskOid);
            return List.of();
        }

        List<ItemDelta<?, ?>> deleteItemDeltas = new ArrayList<>();
        for (int i = 0; i < completeBuckets.size() - 1; i++) {
            WorkBucketType completeBucketToDelete = completeBuckets.get(i);
            if (completeBucketToDelete.getSequentialNumber() == closedBucketBefore.getSequentialNumber()) {
                recentlyClosedBucketDeletedHolder.setValue(true);
                // We need to delete the "before" value of closed bucket (otherwise the deletion will not find the correct PCV)
                deleteItemDeltas.addAll(bucketDeleteDeltas(statePath, closedBucketBefore));
            } else {
                deleteItemDeltas.addAll(bucketDeleteDeltas(statePath, completeBucketToDelete));
            }
        }
        LOGGER.trace("Compression of completed buckets: deleting {} buckets before last completed one in {}",
                deleteItemDeltas.size(), coordinatorTaskOid);
        return deleteItemDeltas;
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "sequentialNumber", sequentialNumber, indent + 1);
    }
}
