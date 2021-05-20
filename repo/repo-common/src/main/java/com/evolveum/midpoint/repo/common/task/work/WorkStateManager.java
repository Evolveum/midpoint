/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.common.task.work.segmentation.BucketContentFactoryCreator;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.WorkBucketContentHandler;
import com.evolveum.midpoint.repo.common.task.work.segmentation.content.WorkBucketContentHandlerRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Responsible for managing task work state:
 *
 * 1. Obtains new buckets to be processed: {@link #getWorkBucket(String, int, OperationResult)}.
 * 2. Marks buckets as complete: {@link #completeWorkBucket(String, int, WorkBucketStatisticsCollector, OperationResult)}.
 * 3. Releases work buckets in case they are not going to be processed: {@link #releaseWorkBucket(String, int, WorkBucketStatisticsCollector, OperationResult)}.
 * 4. Computes query narrowing for given work bucket: {@link #narrowQueryForWorkBucket(Class, ObjectQuery, Task, Function, WorkBucketType, OperationResult)}.
 *
 * (The last method should be probably moved to a separate class.)
 *
 * TODO Does this class even belong to task manager module?
 */
@Component
public class WorkStateManager {

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
    public WorkBucketType getWorkBucket(@NotNull String workerTaskOid, long freeBucketWaitTime,
            Supplier<Boolean> canRun, boolean executeInitialWait, @Nullable WorkBucketStatisticsCollector collector,
            @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
        GetBucketOperation.Options options = new GetBucketOperation.Options(freeBucketWaitTime, executeInitialWait);
        return new GetBucketOperation(workerTaskOid, collector, this, canRun, options)
                .execute(result);
    }

    @VisibleForTesting
    public WorkBucketType getWorkBucket(@NotNull String workerTaskOid, int freeBucketWaitTime, @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, InterruptedException {
        return getWorkBucket(workerTaskOid, freeBucketWaitTime, null, false, null, result);
    }

    /**
     * Marks a work bucket as complete. Should be called from the worker task.
     */
    public void completeWorkBucket(String workerTaskOid, int sequentialNumber, WorkBucketStatisticsCollector statisticsCollector,
            OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        new CompleteBucketOperation(this, workerTaskOid, statisticsCollector, sequentialNumber)
                .execute(result);
    }

    /**
     * Releases work bucket. Should be called from the worker task.
     */
    public void releaseWorkBucket(String workerTaskOid, int sequentialNumber, WorkBucketStatisticsCollector statisticsCollector,
            OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        new ReleaseBucketOperation(this, workerTaskOid, statisticsCollector, sequentialNumber)
                .execute(result);
    }

    /**
     * Narrows a query by taking specified bucket into account.
     */
    public ObjectQuery narrowQueryForWorkBucket(Class<? extends ObjectType> type, ObjectQuery query,
            Task workerTask, Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider,
            WorkBucketType workBucket, OperationResult result) throws SchemaException, ObjectNotFoundException {

        WorkBucketContentHandler contentHandler = handlerFactory.getHandler(workBucket.getContent());

        ActivityDefinitionType partDef = getCurrentPartDefinition(workerTask, result);
        AbstractWorkSegmentationType segmentationConfig =
                TaskWorkStateUtil.getWorkSegmentationConfiguration(partDef.getDistribution());

        List<ObjectFilter> conjunctionMembers = new ArrayList<>(
                contentHandler.createSpecificFilters(workBucket, segmentationConfig, type, itemDefinitionProvider));

        return ObjectQueryUtil.addConjunctions(query, prismContext, conjunctionMembers);
    }

    @NotNull
    private ActivityDefinitionType getCurrentPartDefinition(Task workerTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
        ActivityDefinitionType partDef;

        boolean standalone = TaskWorkStateUtil.isStandalone(workerTask.getWorkStateOrClone());
        if (standalone) {
            partDef = TaskWorkStateUtil.getPartDefinition(workerTask.getActivityDefinitionOrClone(), workerTask.getCurrentPartId());
            stateCheck(partDef != null, "No current part definition for standalone task %s", workerTask);
        } else {
            Task coordinatorTask = workerTask.getParentTask(result);
            stateCheck(coordinatorTask != null, "No coordinator task for worker %s", workerTask);

            partDef = TaskWorkStateUtil.getPartDefinition(coordinatorTask.getActivityDefinitionOrClone(),
                    coordinatorTask.getCurrentPartId());
            stateCheck(partDef != null, "No current part definition for coordinator %s (worker is %s)",
                    coordinatorTask, workerTask);
        }

        return partDef;
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

    public WorkBucketType getWorkBucket(@NotNull RunningTask task, long freeBucketWaitTime,
            boolean executeInitialWait, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, InterruptedException {
        return getWorkBucket(task.getOid(), freeBucketWaitTime, task::canRun, executeInitialWait,
                task.getWorkBucketStatisticsCollector(), result);
    }

    public void completeWorkBucket(@NotNull RunningTask task, @NotNull WorkBucketType bucket, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        completeWorkBucket(task.getOid(), bucket.getSequentialNumber(),
                task.getWorkBucketStatisticsCollector(), result);
    }

    // + narrowQueryForWorkBucket
}
