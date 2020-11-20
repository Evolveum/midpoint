/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.statistics.ActionsExecutedInformation;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author mederly
 */
public class TaskTypeUtil {

    /**
     * Returns a stream of the task and all of its subtasks.
     */
    @NotNull
    public static Stream<TaskType> getAllTasksStream(TaskType root) {
        return Stream.concat(Stream.of(root),
                getResolvedSubtasks(root).stream().flatMap(TaskTypeUtil::getAllTasksStream));
    }

    public static List<TaskType> getResolvedSubtasks(TaskType parent) {
        List<TaskType> rv = new ArrayList<>();
        for (ObjectReferenceType childRef : parent.getSubtaskRef()) {
            if (childRef.getOid() == null && childRef.getObject() == null) {
                continue;
            }
            //noinspection unchecked
            PrismObject<TaskType> child = childRef.getObject();
            if (child != null) {
                rv.add(child.asObjectable());
            } else {
                throw new IllegalStateException("Unresolved subtaskRef in " + parent + ": " + childRef);
            }
        }
        return rv;
    }

    public static void addSubtask(TaskType parent, TaskType child, PrismContext prismContext) {
        parent.getSubtaskRef().add(ObjectTypeUtil.createObjectRefWithFullObject(child, prismContext));
    }

    //moved from GUI
    public static boolean isCoordinator(TaskType task) {
        return getKind(task) == TaskKindType.COORDINATOR;
    }

    public static boolean isPartitionedMaster(TaskType task) {
        return getKind(task) == TaskKindType.PARTITIONED_MASTER;
    }

    @NotNull
    public static TaskKindType getKind(TaskType task) {
        if (task.getWorkManagement() != null && task.getWorkManagement().getTaskKind() != null) {
            return task.getWorkManagement().getTaskKind();
        } else {
            return TaskKindType.STANDALONE;
        }
    }

    public static int getObjectsProcessed(TaskType task) {
        OperationStatsType stats = task.getOperationStats();
        if (stats == null) {
            return 0;
        }
        IterativeTaskInformationType iterativeStats = stats.getIterativeTaskInformation();
        if (iterativeStats == null) {
            return 0;
        }
        return iterativeStats.getTotalSuccessCount() + iterativeStats.getTotalFailureCount();
    }

    public static int getObjectsProcessedFailures(TaskType task, PrismContext prismContext) {
        OperationStatsType stats = getAggregatedOperationStats(task, prismContext);
        if (stats == null) {
            return 0;
        }
        IterativeTaskInformationType iterativeStats = stats.getIterativeTaskInformation();
        if (iterativeStats == null) {
            return 0;
        }
        return iterativeStats.getTotalFailureCount();
    }

    public static OperationStatsType getAggregatedOperationStats(TaskType task, PrismContext prismContext) {
        if (!isPartitionedMaster(task) && !isWorkStateHolder(task)) {
           return task.getOperationStats();
        }

        IterativeTaskInformationType iterativeTaskInformation = new IterativeTaskInformationType();
        SynchronizationInformationType synchronizationInformation = new SynchronizationInformationType();
        ActionsExecutedInformationType actionsExecutedInformation = new ActionsExecutedInformationType();

        Stream<TaskType> subTasks = TaskTypeUtil.getAllTasksStream(task);
        subTasks.forEach(subTask -> {
            OperationStatsType operationStatsType = subTask.getOperationStats();
            if (operationStatsType != null) {
                IterativeTaskInformation.addTo(iterativeTaskInformation, operationStatsType.getIterativeTaskInformation(), false);
                SynchronizationInformation.addTo(synchronizationInformation, operationStatsType.getSynchronizationInformation());
                ActionsExecutedInformation.addTo(actionsExecutedInformation, operationStatsType.getActionsExecutedInformation());
            }
        });

        return new OperationStatsType(prismContext)
                .iterativeTaskInformation(iterativeTaskInformation)
                .synchronizationInformation(synchronizationInformation)
                .actionsExecutedInformation(actionsExecutedInformation);
    }

    public static TaskType findChild(TaskType parent, String childOid) {
        for (TaskType subtask : getResolvedSubtasks(parent)) {
            if (childOid.equals(subtask.getOid())) {
                return subtask;
            }
        }
        return null;
    }

    public static boolean isWorkStateHolder(TaskType taskType) {
        return (isCoordinator(taskType) || hasBuckets(taskType)) && !isCoordinatedWorker(taskType);
    }

    private static boolean hasBuckets(TaskType taskType) {
        if (taskType.getWorkState() == null) {
            return false;
        }
        if (taskType.getWorkState().getNumberOfBuckets() != null && taskType.getWorkState().getNumberOfBuckets() > 1) {
            return true;
        }
        List<WorkBucketType> buckets = taskType.getWorkState().getBucket();
        if (buckets.size() > 1) {
            return true;
        } else {
            return buckets.size() == 1 && buckets.get(0).getContent() != null;
        }
    }

    private static boolean isCoordinatedWorker(TaskType taskType) {
        return taskType.getWorkManagement() != null && TaskKindType.WORKER == taskType.getWorkManagement().getTaskKind();
    }

    public static boolean isManageableTreeRoot(TaskType taskType) {
        return isCoordinator(taskType) || isPartitionedMaster(taskType);
    }
}
