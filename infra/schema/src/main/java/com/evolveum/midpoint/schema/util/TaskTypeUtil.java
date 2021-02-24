/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.ActionsExecutedInformation;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Collections.singletonList;

/**
 * TODO
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

    /**
     * Returns the number of item processing failures from this task and its subtasks.
     */
    public static int getItemsProcessedWithFailureFromTree(TaskType task, PrismContext prismContext) {
        OperationStatsType stats = getOperationStatsFromTree(task, prismContext); // TODO avoid aggregation
        return getItemsProcessedWithFailure(stats);
    }

    public static int getItemsProcessedWithSuccess(TaskType task) {
        return getItemsProcessedWithSuccess(task.getOperationStats());
    }

    public static int getItemsProcessedWithFailure(TaskType task) {
        return getItemsProcessedWithFailure(task.getOperationStats());
    }

    public static int getItemsProcessedWithSuccess(OperationStatsType stats) {
        return stats != null ? getItemsProcessedWithSuccess(stats.getIterativeTaskInformation()) : 0;
    }

    public static int getItemsProcessedWithSuccess(IterativeTaskInformationType info) {
        if (info != null) {
            return getCounts(getProcessingComponents(info), TaskTypeUtil::isSuccess);
        } else {
            return 0;
        }
    }

    public static int getItemsProcessedWithFailure(OperationStatsType stats) {
        return stats != null ? getItemsProcessedWithFailure(stats.getIterativeTaskInformation()) : 0;
    }

    public static int getItemsProcessedWithFailure(IterativeTaskInformationType info) {
        if (info != null) {
            return getCounts(getProcessingComponents(info), TaskTypeUtil::isFailure);
        } else {
            return 0;
        }
    }

    /**
     * Provides aggregated operation statistics from this task and all its subtasks.
     * Works with stored operation stats, obviously. (We have no task instances here.)
     *
     * Assumes that the task has all subtasks filled-in.
     */
    public static OperationStatsType getOperationStatsFromTree(TaskType task, PrismContext prismContext) {
        if (!isPartitionedMaster(task) && !isWorkStateHolder(task)) {
           return task.getOperationStats();
        }

        OperationStatsType aggregate = new OperationStatsType(prismContext)
                .synchronizationInformation(new SynchronizationInformationType())
                .actionsExecutedInformation(new ActionsExecutedInformationType());

        Stream<TaskType> subTasks = TaskTypeUtil.getAllTasksStream(task);
        subTasks.forEach(subTask -> {
            OperationStatsType operationStatsBean = subTask.getOperationStats();
            if (operationStatsBean != null) {
                IterativeTaskInformation.addToSummary(aggregate.getIterativeTaskInformation(), operationStatsBean.getIterativeTaskInformation());
                SynchronizationInformation.addTo(aggregate.getSynchronizationInformation(), operationStatsBean.getSynchronizationInformation());
                ActionsExecutedInformation.addTo(aggregate.getActionsExecutedInformation(), operationStatsBean.getActionsExecutedInformation());
            }
        });
        return aggregate;
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

    /**
     * Returns the number of "iterations" i.e. how many times an item was processed by this task.
     * It is useful e.g. to provide average values for performance indicators.
     */
    public static Integer getItemsProcessed(TaskType task) {
        return getItemsProcessed(task.getOperationStats());
    }

    /**
     * Returns the number of "iterations" i.e. how many times an item was processed by this task.
     * It is useful e.g. to provide average values for performance indicators.
     */
    public static Integer getItemsProcessed(OperationStatsType statistics) {
        if (statistics == null) {
            return null;
        }

        List<? extends IterativeItemsProcessingInformationType> components =
                getProcessingComponentsNullable(statistics.getIterativeTaskInformation());
        if (components == null) {
            return null;
        }

        return getItemsProcessed(components);
    }

    @NotNull
    private static List<? extends IterativeItemsProcessingInformationType> getProcessingComponents(IterativeTaskInformationType info) {
        return emptyIfNull(getProcessingComponentsNullable(info));
    }

    /**
     * Returns a list of iterative info statistics components (e.g. by part).
     * Or null if no such information is available.
     */
    @Nullable
    private static List<? extends IterativeItemsProcessingInformationType> getProcessingComponentsNullable(IterativeTaskInformationType info) {
        if (info == null) {
            return null;
        } else if (info.getSummary() != null) {
            return singletonList(info.getSummary());
        } else {
            return info.getPart();
        }
    }

    private static int getItemsProcessed(List<? extends IterativeItemsProcessingInformationType> components) {
        return getCounts(components, set -> true);
    }

    private static int getCounts(List<? extends IterativeItemsProcessingInformationType> components,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        return components.stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(Objects::nonNull)
                .filter(itemSetFilter)
                .mapToInt(p -> or0(p.getCount()))
                .sum();
    }

    /**
     * Returns object that was last successfully processed by given task.
     */
    public static String getLastSuccessObjectName(TaskType task) {
        OperationStatsType stats = task.getOperationStats();
        if (stats == null || stats.getIterativeTaskInformation() == null) {
            return null;
        } else {
            return getLastProcessedObjectName(stats.getIterativeTaskInformation(), TaskTypeUtil::isSuccess);
        }
    }

    public static String getLastProcessedObjectName(IterativeTaskInformationType info, Predicate<ProcessedItemSetType> itemSetFilter) {
        List<? extends IterativeItemsProcessingInformationType> components =
                getProcessingComponents(info);
        ProcessedItemType lastSuccess = components.stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(itemSetFilter)
                .map(ProcessedItemSetType::getLastItem)
                .filter(Objects::nonNull)
                .max(Comparator.nullsFirst(Comparator.comparing(item -> XmlTypeConverter.toMillis(item.getEndTimestamp()))))
                .orElse(null);
        return lastSuccess != null ? lastSuccess.getName() : null;
    }

    public static boolean isSuccess(ProcessedItemSetType set) {
        return set.getOutcome() != null && set.getOutcome().getOutcome() == ItemProcessingOutcomeType.SUCCESS;
    }

    public static boolean isFailure(ProcessedItemSetType set) {
        return set.getOutcome() != null && set.getOutcome().getOutcome() == ItemProcessingOutcomeType.FAILURE;
    }

}
