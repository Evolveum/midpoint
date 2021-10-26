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

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.schema.util.task.work.BucketingConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

public class ReleaseBucketsOperation extends BucketOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ReleaseBucketsOperation.class);

    /** null means all delegated buckets */
    private final Integer sequentialNumber;

    ReleaseBucketsOperation(@NotNull String coordinatorTaskOid, @NotNull String workerTaskOid,
            @NotNull ActivityPath activityPath, ActivityBucketManagementStatistics collector, CommonTaskBeans beans,
            Integer sequentialNumber) {
        super(coordinatorTaskOid, workerTaskOid, activityPath, collector, null, beans);
        this.sequentialNumber = sequentialNumber;
    }

    public void execute(OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        ModifyObjectResult<TaskType> modifyObjectResult;
        try {
            modifyObjectResult = plainRepositoryService.modifyObjectDynamically(TaskType.class, coordinatorTaskOid, null,
                    this::computeReleaseModifications, null, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected ObjectAlreadyExistsException: " + e.getMessage(), e);
        }

        statisticsKeeper.addToConflictCounts(modifyObjectResult);
        statisticsKeeper.register(BucketingConstants.RELEASE_WORK_BUCKET);
    }

    private @NotNull Collection<ItemDelta<?, ?>> computeReleaseModifications(@NotNull TaskType task) {
        assert workerTaskOid != null;

        List<WorkBucketType> bucketsToRelease = new ArrayList<>();
        List<WorkBucketType> currentBuckets = BucketingUtil.getBuckets(task.getActivityState(), activityPath);

        if (sequentialNumber != null) {
            WorkBucketType bucket = BucketingUtil.findBucketByNumberRequired(currentBuckets, sequentialNumber);
            checkBucketReadyOrDelegated(bucket);
            bucketsToRelease.add(bucket);
        } else {
            currentBuckets.stream()
                    .filter(b -> BucketingUtil.isDelegatedTo(b, workerTaskOid))
                    .forEach(bucketsToRelease::add);
        }
        LOGGER.trace("Releasing buckets {} in {} (delegated to {})", bucketsToRelease, task, workerTaskOid);

        ItemPath statePath = ActivityStateUtil.getStateItemPath(task.getActivityState(), activityPath);
        return bucketsStateChangeDeltas(statePath, bucketsToRelease, WorkBucketStateType.READY, null);
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "sequentialNumber", sequentialNumber, indent + 1);
    }
}
