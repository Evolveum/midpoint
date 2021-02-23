/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.repo.api.CounterManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.run.HandlerExecutor;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author katka
 */
@Component
public class LightweightPartitioningTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(LightweightPartitioningTaskHandler.class);

    private static final String HANDLER_URI = TaskConstants.LIGHTWEIGHT_PARTITIONING_TASK_HANDLER_URI;

    @Autowired private TaskManager taskManager;
    @Autowired private HandlerExecutor handlerExecutor;
    @Autowired private CounterManager counterManager;
    @Autowired private PrismContext prismContext;

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
        taskManager.registerDeprecatedHandlerUri(TaskConstants.LIGHTWEIGHT_PARTITIONING_TASK_HANDLER_URI_DEPRECATED, this);
    }

    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType taskPartition) {
        OperationResult opResult = new OperationResult(LightweightPartitioningTaskHandler.class.getName()+".run");
        TaskRunResult runResult = new TaskRunResult();

        runResult.setProgress(task.getProgress());
        runResult.setOperationResult(opResult);

        if (taskPartition != null && taskPartition.getWorkManagement() != null) {
            throw new UnsupportedOperationException("Work management not supported in partitions for lightweight partitioning task");
        }

        TaskPartitionsDefinitionType partitionsDefinition = task.getWorkManagement().getPartitions();
        List<TaskPartitionDefinitionType> partitions = new ArrayList<>(partitionsDefinition.getPartition());
        Comparator<TaskPartitionDefinitionType> comparator =
                (partition1, partition2) -> {

                    Validate.notNull(partition1);
                    Validate.notNull(partition2);

                    Integer index1 = partition1.getIndex();
                    Integer index2 = partition2.getIndex();

                    if (index1 == null) {
                        if (index2 == null) {
                            return 0;
                        }
                        return -1;
                    }

                    if (index2 == null) {
                        return -1;
                    }

                    return index1.compareTo(index2);
                };

        partitions.sort(comparator);

        Iterator<TaskPartitionDefinitionType> partitionsIterator = partitions.iterator();
        while (partitionsIterator.hasNext()) {
            TaskPartitionDefinitionType partition = partitionsIterator.next();
            TaskHandler handler = taskManager.getHandler(partition.getHandlerUri());
            LOGGER.trace("Starting to execute handler {} defined in partition {}", handler, partition);

            if (task.getChannel() == null) {
                task.setChannel(handler.getDefaultChannel());
            }

            TaskRunResult subHandlerResult = handlerExecutor.executeHandler((RunningTaskQuartzImpl) task, partition, handler, opResult);
            OperationResult subHandlerOpResult = subHandlerResult.getOperationResult();
            opResult.addSubresult(subHandlerOpResult);
            runResult = subHandlerResult;
            runResult.setProgress(task.getProgress());

            if (!canContinue(task, subHandlerResult)) {
                break;
            }

            if (subHandlerOpResult.isError()) {
                break;
            }

            try {
                LOGGER.trace("Cleaning up work state in task {}, workState: {}", task, task.getWorkState());
                cleanupWorkState(task, runResult.getOperationResult());
            } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
                LOGGER.error("Unexpected error during cleaning work state: " + e.getMessage(), e);
                throw new IllegalStateException(e);
            }

            if (partitionsIterator.hasNext()) {
                counterManager.resetCounters(task.getOid());
            }

        }

        runResult.setProgress(runResult.getProgress() + 1);
        opResult.computeStatusIfUnknown();
        counterManager.cleanupCounters(task.getOid());

        return runResult;
    }

    private void cleanupWorkState(RunningTask runningTask, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        //noinspection unchecked
        ContainerDelta<TaskWorkStateType> containerDelta = (ContainerDelta<TaskWorkStateType>) prismContext
                .deltaFor(TaskType.class).item(TaskType.F_WORK_STATE).replace().asItemDelta();
        ((TaskQuartzImpl) runningTask).applyDeltasImmediate(MiscUtil.createCollection(containerDelta), parentResult);
    }

    private boolean canContinue(RunningTask task, TaskRunResult runResult) {
        if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResultStatus.INTERRUPTED) {
            // first, if a task was interrupted, we do not want to change its status
            LOGGER.trace("Task was interrupted, exiting the execution cycle. Task = {}", task);
            return false;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.TEMPORARY_ERROR) {
            LOGGER.trace("Task encountered temporary error, continuing with the execution cycle. Task = {}", task);
            return false;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.PERMANENT_ERROR) {
            LOGGER.info("Task encountered permanent error, suspending the task. Task = {}", task);
            return false;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED) {
            LOGGER.trace("Task handler finished, continuing with the execution cycle. Task = {}", task);
            return true;
        } else if (runResult.getRunResultStatus() == TaskRunResultStatus.IS_WAITING) {
            LOGGER.trace("Task switched to waiting state, exiting the execution cycle. Task = {}", task);
            return true;
        } else {
            throw new IllegalStateException("Invalid value for Task's runResultStatus: " + runResult.getRunResultStatus() + " for task " + task);
        }
    }

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromZero()
                .maintainIterationStatistics()
                .maintainSynchronizationStatistics()
                .maintainActionsExecutedStatistics();
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
