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
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.evolveum.midpoint.util.MiscUtil.or0;

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
            return getCounts(info.getPart(), OutcomeKeyedCounterTypeUtil::isSuccess);
        } else {
            return 0;
        }
    }

    public static int getProgressForOutcome(StructuredTaskProgressType info, ItemProcessingOutcomeType outcome, boolean open) {
        if (info != null) {
            return getCounts(info.getPart(), getCounterFilter(outcome), open);
        } else {
            return 0;
        }
    }

    private static Predicate<OutcomeKeyedCounterType> getCounterFilter(ItemProcessingOutcomeType outcome) {
        switch (outcome) {
            case SUCCESS:
                return OutcomeKeyedCounterTypeUtil::isSuccess;
            case FAILURE:
                return OutcomeKeyedCounterTypeUtil::isFailure;
            case SKIP:
                return OutcomeKeyedCounterTypeUtil::isSkip;
            default:
                throw new AssertionError(outcome);
        }
    }

    public static int getItemsProcessedWithFailure(OperationStatsType stats) {
        return stats != null ? getItemsProcessedWithFailure(stats.getIterativeTaskInformation()) : 0;
    }

    public static int getItemsProcessedWithFailure(IterativeTaskInformationType info) {
        if (info != null) {
            return getCounts(info.getPart(), OutcomeKeyedCounterTypeUtil::isFailure);
        } else {
            return 0;
        }
    }

    public static int getItemsProcessedWithSkip(IterativeTaskInformationType info) {
        if (info != null) {
            return getCounts(info.getPart(), OutcomeKeyedCounterTypeUtil::isSkip);
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
                IterativeTaskInformation.addTo(aggregate.getIterativeTaskInformation(), operationStatsBean.getIterativeTaskInformation());
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
        if (statistics == null || statistics.getIterativeTaskInformation() == null) {
            return null;
        } else {
            return getCounts(statistics.getIterativeTaskInformation().getPart(), set -> true);
        }
    }

    private static int getCounts(List<IterativeTaskPartItemsProcessingInformationType> parts,
            Predicate<ProcessedItemSetType> itemSetFilter) {
        return parts.stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(Objects::nonNull)
                .filter(itemSetFilter)
                .mapToInt(p -> or0(p.getCount()))
                .sum();
    }

    private static int getCounts(List<TaskPartProgressType> parts,
            Predicate<OutcomeKeyedCounterType> counterFilter, boolean open) {
        return parts.stream()
                .flatMap(part -> (open ? part.getOpen() : part.getClosed()).stream())
                .filter(Objects::nonNull)
                .filter(counterFilter)
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
            return getLastProcessedObjectName(stats.getIterativeTaskInformation(), OutcomeKeyedCounterTypeUtil::isSuccess);
        }
    }

    public static String getLastProcessedObjectName(IterativeTaskInformationType info, Predicate<ProcessedItemSetType> itemSetFilter) {
        if (info == null) {
            return null;
        }
        ProcessedItemType lastSuccess = info.getPart().stream()
                .flatMap(component -> component.getProcessed().stream())
                .filter(itemSetFilter)
                .map(ProcessedItemSetType::getLastItem)
                .filter(Objects::nonNull)
                .max(Comparator.nullsFirst(Comparator.comparing(item -> XmlTypeConverter.toMillis(item.getEndTimestamp()))))
                .orElse(null);
        return lastSuccess != null ? lastSuccess.getName() : null;
    }

}
