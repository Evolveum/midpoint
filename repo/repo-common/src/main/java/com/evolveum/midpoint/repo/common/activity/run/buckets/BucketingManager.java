/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BucketProgressOverviewType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityBucketManagementStatistics;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content.WorkBucketContentHandler;
import com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content.WorkBucketContentHandlerRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Responsible for managing task work state:
 *
 * 1. Obtains new buckets to be processed: {@link #getWorkBucket(String, String, ActivityPath, GetBucketOperationOptions, ActivityBucketManagementStatistics, OperationResult)}.
 * 2. Marks buckets as complete: {@link #completeWorkBucket(String, String, ActivityPath, int, ActivityBucketManagementStatistics, Consumer, OperationResult)}.
 * 3. Releases work buckets in case they are not going to be processed: {@link #releaseWorkBucket(String, String, ActivityPath, int, ActivityBucketManagementStatistics, OperationResult)}.
 * 4. Computes query narrowing for given work bucket: {@link #narrowQueryForWorkBucket(Class, ObjectQuery, ActivityDistributionDefinition, ItemDefinitionProvider, WorkBucketType)}.
 *
 * (The last method should be probably moved to a separate class.)
 */
@Component
public class BucketingManager {

    @Autowired private CommonTaskBeans beans;
    @Autowired private WorkBucketContentHandlerRegistry handlerRegistry;

    /**
     * Obtains work bucket. If no free work buckets are currently present it tries to create one.
     * If there is already delegated work bucket for given worker task, it is returned.
     */
    public WorkBucketType getWorkBucket(@NotNull String coordinatorTaskOid, @Nullable String workerTaskOid,
            @NotNull ActivityPath activityPath, @Nullable GetBucketOperationOptions options,
            ActivityBucketManagementStatistics statistics, @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
        return new GetBucketOperation(coordinatorTaskOid, workerTaskOid, activityPath, statistics, options, beans)
                .execute(result);
    }

    /**
     * Marks a work bucket as complete.
     */
    public void completeWorkBucket(@NotNull String coordinatorTaskOid, @Nullable String workerTaskOid,
            @NotNull ActivityPath activityPath, int sequentialNumber,
            @Nullable ActivityBucketManagementStatistics statistics,
            @Nullable Consumer<BucketProgressOverviewType> bucketProgressConsumer,
            @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        new CompleteBucketOperation(coordinatorTaskOid, workerTaskOid, activityPath, statistics,
                bucketProgressConsumer, beans, sequentialNumber)
                .execute(result);
    }

    /**
     * Releases work bucket.
     */
    public void releaseWorkBucket(@NotNull String coordinatorTaskOid, @NotNull String workerTaskOid,
            @NotNull ActivityPath activityPath, int sequentialNumber,
            ActivityBucketManagementStatistics statistics, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        new ReleaseBucketsOperation(coordinatorTaskOid, workerTaskOid, activityPath, statistics, beans, sequentialNumber)
                .execute(result);
    }

    /**
     * Releases all work buckets from a suspended worker.
     *
     * Will change in the future - there are some preconditions to be checked within the modification operation.
     */
    @Experimental
    public void releaseAllWorkBucketsFromWorker(@NotNull String coordinatorTaskOid, @NotNull String workerTaskOid,
            @NotNull ActivityPath activityPath, ActivityBucketManagementStatistics statistics, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        new ReleaseBucketsOperation(coordinatorTaskOid, workerTaskOid, activityPath, statistics, beans, null)
                .execute(result);
    }

    /**
     * Narrows a query by taking specified bucket into account.
     */
    public ObjectQuery narrowQueryForWorkBucket(@NotNull Class<? extends Containerable> type, ObjectQuery query,
            @NotNull ActivityDistributionDefinition distributionDefinition,
            @Nullable ItemDefinitionProvider itemDefinitionProvider,
            @NotNull WorkBucketType workBucket)
            throws SchemaException {

        WorkBucketContentHandler contentHandler = handlerRegistry.getHandler(workBucket.getContent());

        AbstractWorkSegmentationType segmentationConfig =
                BucketingUtil.getWorkSegmentationConfiguration(distributionDefinition.getBuckets());

        List<ObjectFilter> conjunctionMembers = new ArrayList<>(
                contentHandler.createSpecificFilters(workBucket, segmentationConfig, type, itemDefinitionProvider));

        return ObjectQueryUtil.addConjunctions(query, conjunctionMembers);
    }
}
