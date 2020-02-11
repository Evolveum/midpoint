/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test;

import com.evolveum.midpoint.schema.util.TaskPartitioningInformation;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Extract of key task progress information used to test progress-reporting mechanisms.
 */
public class TaskProgressExtract implements DebugDumpable {

    private final long progress;
    private final Long expectedTotalProgress;
    private final int objectsProcessed;
    private final Integer expectedBuckets;
    private final int completedBuckets;
    private final TaskPartitioningInformation partitioningInformation;

    private TaskProgressExtract(long progress, Long expectedTotalProgress, int objectsProcessed, Integer expectedBuckets, int completedBuckets,
            TaskPartitioningInformation partitioningInformation) {
        this.progress = progress;
        this.expectedTotalProgress = expectedTotalProgress;
        this.objectsProcessed = objectsProcessed;
        this.expectedBuckets = expectedBuckets;
        this.completedBuckets = completedBuckets;
        this.partitioningInformation = partitioningInformation;
    }

    public static TaskProgressExtract fromTask(TaskType task) {
        if (TaskTypeUtil.isPartitionedMaster(task)) {
            TaskPartitioningInformation partitioningInformation = TaskPartitioningInformation.fromTask(task);
            if (partitioningInformation.getFirstIncompletePartitionNumber() != null) {
                return fromPartition(partitioningInformation.getFirstIncompletePartitionTask(), partitioningInformation);
            } else if (partitioningInformation.getLastCompletePartitionNumber() != null) {
                return fromPartition(partitioningInformation.getLastCompletePartitionTask(), partitioningInformation);
            } else {
                // no partitions?
                return new TaskProgressExtract(0, null, 0, null, 0, partitioningInformation);
            }
        } else {
            return fromPartition(task, null);
        }
    }

    private static TaskProgressExtract fromPartition(TaskType task, TaskPartitioningInformation partitioningInformation) {
        return new TaskProgressExtract(
                getTaskProgress(task), getTaskExpectedTotal(task),
                getObjectsProcessed(task),
                TaskWorkStateTypeUtil.getExpectedBuckets(task),
                TaskWorkStateTypeUtil.getCompleteBucketsNumber(task), partitioningInformation);
    }

    private static long getTaskProgress(TaskType task) {
        assert !TaskTypeUtil.isPartitionedMaster(task);
        if (TaskTypeUtil.isCoordinator(task)) {
            List<TaskType> subtasks = TaskTypeUtil.getResolvedSubtasks(task);
            return subtasks.stream().mapToLong(t -> defaultIfNull(t.getProgress(), 0L)).sum();
        } else {
            return defaultIfNull(task.getProgress(), 0L);
        }
    }

    private static Long getTaskExpectedTotal(TaskType task) {
        assert !TaskTypeUtil.isPartitionedMaster(task);
        if (TaskTypeUtil.isCoordinator(task)) {
            List<TaskType> subtasks = TaskTypeUtil.getResolvedSubtasks(task);
            if (subtasks.stream().noneMatch(t -> t.getExpectedTotal() == null)) {
                return subtasks.stream().mapToLong(TaskType::getExpectedTotal).sum();
            } else {
                return null;
            }
        } else {
            return task.getExpectedTotal();
        }
    }

    private static int getObjectsProcessed(TaskType task) {
        assert !TaskTypeUtil.isPartitionedMaster(task);
        if (TaskTypeUtil.isCoordinator(task)) {
            List<TaskType> subtasks = TaskTypeUtil.getResolvedSubtasks(task);
            return subtasks.stream().mapToInt(TaskTypeUtil::getObjectsProcessed).sum();
        } else {
            return TaskTypeUtil.getObjectsProcessed(task);
        }
    }

    public long getProgress() {
        return progress;
    }

    public Integer getExpectedBuckets() {
        return expectedBuckets;
    }

    public int getCompletedBuckets() {
        return completedBuckets;
    }

    public TaskPartitioningInformation getPartitioningInformation() {
        return partitioningInformation;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpWithLabelLn(sb, "Progress", progress, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Expected total progress", expectedTotalProgress, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Objects processed", objectsProcessed, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Expected buckets", expectedBuckets, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Completed buckets", completedBuckets, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Partitioning information", partitioningInformation, indent);
        return sb.toString();
    }
}
