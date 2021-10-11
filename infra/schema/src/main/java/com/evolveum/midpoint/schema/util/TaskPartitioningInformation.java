/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Information on task partitioning state - more usable than distributed task work state in child tasks.
 */
public class TaskPartitioningInformation implements DebugDumpable, ShortDumpable {
    private final int allPartitions;
    private final int completePartitions;
    private final Integer firstIncompletePartitionNumber;
    private final Integer lastCompletePartitionNumber;
    @NotNull private final Map<Integer, TaskType> partitionsMap;

    private TaskPartitioningInformation(int allPartitions, int completePartitions, Integer firstIncompletePartitionNumber,
            Integer lastCompletePartitionNumber, @NotNull Map<Integer, TaskType> partitionsMap) {
        this.allPartitions = allPartitions;
        this.completePartitions = completePartitions;
        this.firstIncompletePartitionNumber = firstIncompletePartitionNumber;
        this.lastCompletePartitionNumber = lastCompletePartitionNumber;
        this.partitionsMap = Collections.unmodifiableMap(new TreeMap<>(partitionsMap));
    }

    /**
     * @pre task is partitioned master and contains fully retrieved and resolved subtasks
     */
    public static TaskPartitioningInformation fromTask(TaskType task) {
        if (TaskTypeUtil.isPartitionedMaster(task)) {
            Map<Integer, TaskType> partitionsMap = new HashMap<>();
            List<ObjectReferenceType> subtasks = task.getSubtaskRef();
            int allPartitions = subtasks.size();
            int completePartitions = 0;
            Integer firstIncompleteSequentialNumber = null;
            Integer lastCompletePartitionNumber = null;
            for (ObjectReferenceType subtaskRef : subtasks) {
                //noinspection unchecked
                PrismObject<TaskType> subtaskObject = subtaskRef.asReferenceValue().getObject();
                if (subtaskObject == null) {
                    throw new IllegalArgumentException("Task " + task + " has unresolved subtask: " + subtaskRef);
                }
                TaskType subtask = subtaskObject.asObjectable();
                Integer subtaskPartitionNumber = TaskWorkStateTypeUtil.getPartitionSequentialNumber(subtask);
                partitionsMap.put(subtaskPartitionNumber, subtask);
                if (subtask.getExecutionStatus() == TaskExecutionStatusType.CLOSED) {
                    completePartitions++;
                    if (lastCompletePartitionNumber == null ||
                            subtaskPartitionNumber != null && subtaskPartitionNumber > lastCompletePartitionNumber) {
                        lastCompletePartitionNumber = subtaskPartitionNumber;
                    }
                } else {
                    if (firstIncompleteSequentialNumber == null ||
                            subtaskPartitionNumber != null && subtaskPartitionNumber < firstIncompleteSequentialNumber) {
                        firstIncompleteSequentialNumber = subtaskPartitionNumber;
                    }
                }
            }
            return new TaskPartitioningInformation(allPartitions, completePartitions, firstIncompleteSequentialNumber,
                    lastCompletePartitionNumber, partitionsMap);
        } else {
            throw new IllegalArgumentException("Task is not partitioned master: " + task);
        }
    }

    @SuppressWarnings("unused")
    public int getAllPartitions() {
        return allPartitions;
    }

    @SuppressWarnings("unused")
    public int getCompletePartitions() {
        return completePartitions;
    }

    @SuppressWarnings("unused")
    public Integer getFirstIncompletePartitionNumber() {
        return firstIncompletePartitionNumber;
    }

    public Integer getLastCompletePartitionNumber() {
        return lastCompletePartitionNumber;
    }

    @NotNull
    @SuppressWarnings("unused")
    public Map<Integer, TaskType> getPartitionsMap() {
        return partitionsMap;
    }

    @NotNull
    public TaskType getFirstIncompletePartitionTask() {
        if (firstIncompletePartitionNumber != null) {
            TaskType rv = partitionsMap.get(firstIncompletePartitionNumber);
            if (rv != null) {
                return rv;
            } else {
                throw new IllegalStateException("First incomplete partition is not in the map: "
                        + "first#=" + firstIncompletePartitionNumber + ", map=" + partitionsMap);
            }
        } else {
            throw new IllegalStateException("There's no incomplete partition: " + partitionsMap);
        }
    }

    @NotNull
    public TaskType getLastCompletePartitionTask() {
        if (lastCompletePartitionNumber != null) {
            TaskType rv = partitionsMap.get(lastCompletePartitionNumber);
            if (rv != null) {
                return rv;
            } else {
                throw new IllegalStateException("Last complete partition is not in the map: "
                        + "last#=" + lastCompletePartitionNumber + ", map=" + partitionsMap);
            }
        } else {
            throw new IllegalStateException("There's no last complete partition: " + partitionsMap);
        }
    }


    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "All partitions", allPartitions, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Complete partitions", completePartitions, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "First incomplete partition", firstIncompletePartitionNumber, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Partitions map", partitionsMap, indent);
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(completePartitions).append("/").append(allPartitions)
                .append(" (current=").append(firstIncompletePartitionNumber).append(")");
    }
}
