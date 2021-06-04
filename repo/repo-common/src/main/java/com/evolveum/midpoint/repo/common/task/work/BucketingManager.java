/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDistributionDefinition;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketContentFactoryCreator;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.WorkBucketContentHandler;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.WorkBucketContentHandlerRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Responsible for managing task work state:
 *
 * 1. Obtains new buckets to be processed: {@link #getWorkBucket(String, ActivityPath, ActivityDistributionDefinition, long, Supplier, boolean, WorkBucketStatisticsCollector, OperationResult)}.
 * 2. Marks buckets as complete: {@link #completeWorkBucket(String, ActivityPath, int, WorkBucketStatisticsCollector, OperationResult)}.
 * 3. Releases work buckets in case they are not going to be processed: {@link #releaseWorkBucket(String, ActivityPath, int, WorkBucketStatisticsCollector, OperationResult)}.
 * 4. Computes query narrowing for given work bucket: {@link #narrowQueryForWorkBucket(Class, ObjectQuery, ActivityDistributionDefinition, Function, WorkBucketType)}.
 *
 * (The last method should be probably moved to a separate class.)
 */
@Component
public class BucketingManager {

    @Autowired private TaskManager taskManager;
    @Autowired private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private BucketContentFactoryCreator strategyFactory;
    @Autowired private WorkBucketContentHandlerRegistry handlerFactory;

    private Long freeBucketWaitIntervalOverride;

    /**
     * Allocates work bucket. If no free work buckets are currently present it tries to create one.
     * If there is already allocated work bucket for given worker task, it is returned.
     *
     * WE ASSUME THIS METHOD IS CALLED FROM THE WORKER TASK; SO IT IS NOT NECESSARY TO SYNCHRONIZE ACCESS TO THIS TASK WORK STATE.
     */
    private WorkBucketType getWorkBucket(@NotNull String workerTaskOid,
            @NotNull ActivityPath activityPath, @NotNull ActivityDistributionDefinition distributionDefinition,
            long freeBucketWaitTime, Supplier<Boolean> canRun, boolean executeInitialWait,
            @Nullable WorkBucketStatisticsCollector collector, @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
        GetBucketOperation.Options options = new GetBucketOperation.Options(freeBucketWaitTime, executeInitialWait);
        return new GetBucketOperation(workerTaskOid, activityPath, distributionDefinition, collector, this, canRun, options)
                .execute(result);
    }

    @VisibleForTesting
    public WorkBucketType getWorkBucket(@NotNull String workerTaskOid,
            @NotNull ActivityDistributionDefinition distributionDefinition, int freeBucketWaitTime,
            @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
        return getWorkBucket(workerTaskOid, ActivityPath.empty(), distributionDefinition, freeBucketWaitTime, null,
                false, null, result);
    }

    /**
     * Marks a work bucket as complete. Should be called from the worker task.
     */
    public void completeWorkBucket(String workerTaskOid, ActivityPath activityPath, int sequentialNumber,
            WorkBucketStatisticsCollector statisticsCollector, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        new CompleteBucketOperation(this, workerTaskOid, activityPath, statisticsCollector, sequentialNumber)
                .execute(result);
    }

    /**
     * Releases work bucket. Should be called from the worker task.
     */
    public void releaseWorkBucket(String workerTaskOid, ActivityPath activityPath, int sequentialNumber,
            WorkBucketStatisticsCollector statisticsCollector, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        new ReleaseBucketOperation(this, workerTaskOid, activityPath, statisticsCollector, sequentialNumber)
                .execute(result);
    }

    /**
     * Narrows a query by taking specified bucket into account.
     */
    public ObjectQuery narrowQueryForWorkBucket(@NotNull Class<? extends ObjectType> type, ObjectQuery query,
            @NotNull ActivityDistributionDefinition distributionDefinition,
            @Nullable Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider,
            @NotNull WorkBucketType workBucket)
            throws SchemaException {

        WorkBucketContentHandler contentHandler = handlerFactory.getHandler(workBucket.getContent());

        AbstractWorkSegmentationType segmentationConfig =
                TaskWorkStateUtil.getWorkSegmentationConfiguration(distributionDefinition.getBuckets());

        List<ObjectFilter> conjunctionMembers = new ArrayList<>(
                contentHandler.createSpecificFilters(workBucket, segmentationConfig, type, itemDefinitionProvider));

        return ObjectQueryUtil.addConjunctions(query, prismContext, conjunctionMembers);
    }

    public void setFreeBucketWaitIntervalOverride(Long value) {
        this.freeBucketWaitIntervalOverride = value;
    }

    Long getFreeBucketWaitIntervalOverride() {
        return freeBucketWaitIntervalOverride;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    BucketContentFactoryCreator getStrategyFactory() {
        return strategyFactory;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    // PUBLIC INTERFACE (moved from task manager)

    public WorkBucketType getWorkBucket(@NotNull RunningTask task,
            @NotNull ActivityPath activityPath, @NotNull ActivityDistributionDefinition distributionDefinition,
            long freeBucketWaitTime, boolean executeInitialWait, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, InterruptedException {
        return getWorkBucket(task.getOid(), activityPath, distributionDefinition, freeBucketWaitTime, task::canRun,
                executeInitialWait, task.getWorkBucketStatisticsCollector(), result);
    }

    public void completeWorkBucket(@NotNull RunningTask task, @NotNull ActivityPath activityPath,
            @NotNull WorkBucketType bucket, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        completeWorkBucket(task.getOid(), activityPath, bucket.getSequentialNumber(),
                task.getWorkBucketStatisticsCollector(), result);
    }

    // + narrowQueryForWorkBucket
}
