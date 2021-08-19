/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;

import com.evolveum.midpoint.repo.common.task.work.segmentation.ImplicitSegmentationResolver;

import com.evolveum.midpoint.task.api.Task;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.activity.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.WorkBucketContentHandler;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.WorkBucketContentHandlerRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Responsible for managing task work state:
 *
 * 1. Obtains new buckets to be processed: {@link #getWorkBucket(String, ActivityPath, ActivityDistributionDefinition, long, Supplier, boolean, ActivityBucketManagementStatistics, ImplicitSegmentationResolver, OperationResult)}.
 * 2. Marks buckets as complete: {@link #completeWorkBucket(String, ActivityPath, int, ActivityBucketManagementStatistics, OperationResult)}.
 * 3. Releases work buckets in case they are not going to be processed: {@link #releaseWorkBucket(String, ActivityPath, int, ActivityBucketManagementStatistics, OperationResult)}.
 * 4. Computes query narrowing for given work bucket: {@link #narrowQueryForWorkBucket(Class, ObjectQuery, ActivityDistributionDefinition, ItemDefinitionProvider, WorkBucketType)}.
 *
 * (The last method should be probably moved to a separate class.)
 */
@Component
public class BucketingManager {

    @Autowired private CommonTaskBeans beans;
    @Autowired private WorkBucketContentHandlerRegistry handlerRegistry;

    private Long freeBucketWaitIntervalOverride;
    private Long freeBucketWaitTimeOverride;

    /**
     * Allocates work bucket. If no free work buckets are currently present it tries to create one.
     * If there is already allocated work bucket for given worker task, it is returned.
     *
     * WE ASSUME THIS METHOD IS CALLED FROM THE WORKER TASK; SO IT IS NOT NECESSARY TO SYNCHRONIZE ACCESS TO THIS TASK WORK STATE.
     */
    private WorkBucketType getWorkBucket(@NotNull String workerTaskOid,
            @NotNull ActivityPath activityPath, @NotNull ActivityDistributionDefinition distributionDefinition,
            long freeBucketWaitTime, Supplier<Boolean> canRun, boolean executeInitialWait,
            ActivityBucketManagementStatistics statistics, ImplicitSegmentationResolver implicitSegmentationResolver,
            @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
        long realFreeBucketWaitTime = freeBucketWaitTimeOverride != null ? freeBucketWaitTimeOverride : freeBucketWaitTime;
        GetBucketOperation.Options options = new GetBucketOperation.Options(realFreeBucketWaitTime, executeInitialWait);
        return new GetBucketOperation(workerTaskOid, activityPath, distributionDefinition, statistics, beans, canRun, options,
                implicitSegmentationResolver)
                .execute(result);
    }

    @VisibleForTesting
    public WorkBucketType getWorkBucket(@NotNull String workerTaskOid,
            @NotNull ActivityDistributionDefinition distributionDefinition, int freeBucketWaitTime,
            @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
        return getWorkBucket(workerTaskOid, ActivityPath.empty(), distributionDefinition, freeBucketWaitTime, null,
                false, null, null, result);
    }

    /**
     * Marks a work bucket as complete. Should be called from the worker task.
     */
    public void completeWorkBucket(String workerTaskOid, ActivityPath activityPath, int sequentialNumber,
            ActivityBucketManagementStatistics statistics, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        new CompleteBucketOperation(workerTaskOid, activityPath, statistics, beans, sequentialNumber)
                .execute(result);
    }

    /**
     * Releases work bucket. Should be called from the worker task.
     */
    public void releaseWorkBucket(String workerTaskOid, ActivityPath activityPath, int sequentialNumber,
            ActivityBucketManagementStatistics statistics, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        new ReleaseBucketOperation(workerTaskOid, activityPath, statistics, beans, sequentialNumber)
                .execute(result);
    }

    /**
     * Releases all work buckets from a suspended worker.
     */
    public void releaseAllWorkBucketsFromSuspendedWorker(Task workerTask, ActivityPath activityPath,
            ActivityBucketManagementStatistics statistics, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        new ReleaseAllBucketsOperation(workerTask, activityPath, statistics, beans)
                .execute(result);
    }

    /**
     * Narrows a query by taking specified bucket into account.
     */
    public ObjectQuery narrowQueryForWorkBucket(@NotNull Class<? extends ObjectType> type, ObjectQuery query,
            @NotNull ActivityDistributionDefinition distributionDefinition,
            @Nullable ItemDefinitionProvider itemDefinitionProvider,
            @NotNull WorkBucketType workBucket)
            throws SchemaException {

        WorkBucketContentHandler contentHandler = handlerRegistry.getHandler(workBucket.getContent());

        AbstractWorkSegmentationType segmentationConfig =
                BucketingUtil.getWorkSegmentationConfiguration(distributionDefinition.getBuckets());

        List<ObjectFilter> conjunctionMembers = new ArrayList<>(
                contentHandler.createSpecificFilters(workBucket, segmentationConfig, type, itemDefinitionProvider));

        return ObjectQueryUtil.addConjunctions(query, beans.prismContext, conjunctionMembers);
    }

    public void setFreeBucketWaitTimeOverride(Long value) {
        this.freeBucketWaitTimeOverride = value;
    }

    public void setFreeBucketWaitIntervalOverride(Long value) {
        this.freeBucketWaitIntervalOverride = value;
    }

    Long getFreeBucketWaitIntervalOverride() {
        return freeBucketWaitIntervalOverride;
    }

    // PUBLIC INTERFACE (moved from task manager)

    public WorkBucketType getWorkBucket(@NotNull RunningTask task,
            @NotNull ActivityPath activityPath, @NotNull ActivityDistributionDefinition distributionDefinition,
            long freeBucketWaitTime, boolean executeInitialWait, @Nullable ActivityBucketManagementStatistics statistics,
            @Nullable ImplicitSegmentationResolver implicitSegmentationResolver,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, InterruptedException {
        return getWorkBucket(task.getOid(), activityPath, distributionDefinition, freeBucketWaitTime, task::canRun,
                executeInitialWait, statistics, implicitSegmentationResolver, result);
    }

    public void completeWorkBucket(@NotNull RunningTask task, @NotNull ActivityPath activityPath,
            @NotNull WorkBucketType bucket, @Nullable ActivityBucketManagementStatistics statistics,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        completeWorkBucket(task.getOid(), activityPath, bucket.getSequentialNumber(),
                statistics, result);
    }

    // + narrowQueryForWorkBucket
}
