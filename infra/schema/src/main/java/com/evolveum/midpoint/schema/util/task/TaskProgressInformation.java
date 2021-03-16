/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartProgressType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StructuredTaskProgressType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class TaskProgressInformation implements DebugDumpable, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(TaskProgressInformation.class);

    /**
     * Total number of parts.
     */
    private final int allPartsCount;

    /**
     * Number of the part that is being currently executed or that was last executed. Starting at 1.
     */
    private final int currentPartNumber;

    /**
     * URI of the part that is being currently executed or that was last executed.
     */
    private final String currentPartUri;

    /**
     * Information on the progress in individual parts. Indexed by part URI.
     */
    private final Map<String, TaskPartProgressInformation> parts = new HashMap<>();

    private TaskProgressInformation(int allPartsCount, int currentPartNumber, String currentPartUri) {
        this.allPartsCount = allPartsCount;
        this.currentPartNumber = currentPartNumber;
        this.currentPartUri = currentPartUri;
    }

    /**
     * Precondition: the task contains fully retrieved and resolved subtasks.
     */
    public static TaskProgressInformation fromTaskTree(TaskType task) {
        if (TaskWorkStateUtil.isPartitionedMaster(task)) {
            return fromPartitionedMaster(task);
        } else {
            return fromSimpleTask(task);
        }
    }

    @NotNull
    private static TaskProgressInformation fromPartitionedMaster(TaskType task) {
        Map<String, TaskPartProgressInformation> partsByUri = new HashMap<>();
        Map<Integer, TaskPartProgressInformation> partsByIndex = new HashMap<>();
        List<ObjectReferenceType> subtasks = task.getSubtaskRef();

        for (ObjectReferenceType subtaskRef : subtasks) {
            TaskType subtask = (TaskType)
                    requireNonNull(subtaskRef.asReferenceValue().getObject(),
                            () -> "Task " + task + " has unresolved subtask: " + subtaskRef)
                            .asObjectable();

            TaskPartProgressInformation partInfo = TaskPartProgressInformation.fromPersistentSubtask(subtask);
            partsByUri.put(partInfo.getPartOrHandlerUri(), partInfo);
            Integer index = TaskWorkStateUtil.getPartitionSequentialNumber(subtask);
            if (index != null) {
                partsByIndex.put(index, partInfo);
            } else {
                LOGGER.warn("No part index in task:\n{}", subtask.debugDumpLazily());
            }
        }

        int allPartsCount = partsByUri.size();
        int completePartsCount = (int) partsByUri.entrySet().stream()
                .filter(e -> e.getValue().isComplete())
                .count();
        int currentPartNumber = Math.min(completePartsCount + 1, allPartsCount);
        TaskPartProgressInformation currentPart = partsByIndex.get(currentPartNumber);
        String currentPartUri;
        if (currentPart != null) {
            currentPartUri = currentPart.getPartOrHandlerUri();
        } else {
            LOGGER.warn("Part #{} is not among known parts in task:\n{}", currentPartNumber, task.debugDumpLazily());
            currentPartUri = null;
        }

        TaskProgressInformation info = new TaskProgressInformation(allPartsCount, currentPartNumber, currentPartUri);
        info.parts.putAll(partsByUri);
        return info;
    }

    /**
     * Simple task can be either:
     *
     * 1. internally-partitioned task with structured progress; or
     * 2. coordinator task with persistent subtasks. In this case it must be single-part.
     *
     * This method takes care for both cases.
     */
    @NotNull
    private static TaskProgressInformation fromSimpleTask(@NotNull TaskType task) {
        StructuredTaskProgressType progress = task.getStructuredProgress();
        if (progress != null && progress.getExpectedParts() != null && progress.getExpectedParts() > 1) {
            return fromInternallyPartitionedTask(task);
        } else {
            return fromOtherTask(task);
        }
    }

    /**
     * We assume the task has no buckets. Otherwise it couldn't be internally partitioned.
     */
    @NotNull
    private static TaskProgressInformation fromInternallyPartitionedTask(@NotNull TaskType task) {
        StructuredTaskProgressType progress = task.getStructuredProgress();
        int allPartsCount = progress.getExpectedParts();
        int currentPartNumber = progress.getCurrentPartNumber() != null ? progress.getCurrentPartNumber() : 1;
        String currentPartUri = progress.getCurrentPartUri();
        TaskProgressInformation info = new TaskProgressInformation(allPartsCount, currentPartNumber, currentPartUri);
        Integer currentPartExpectedTotal = task.getExpectedTotal() != null ? task.getExpectedTotal().intValue() : null;
        for (TaskPartProgressType partProgress : progress.getPart()) {
            boolean isCurrent = Objects.equals(currentPartUri, partProgress.getPartUri());
            Integer expectedTotal = isCurrent ? currentPartExpectedTotal : null;
            TaskPartProgressInformation taskPartProgressInformation =
                    TaskPartProgressInformation.fromPartProgress(partProgress, expectedTotal);
            info.addPart(taskPartProgressInformation);
        }
        return info;
    }

    /**
     * The task can have one or more buckets.
     */
    @NotNull
    private static TaskProgressInformation fromOtherTask(@NotNull TaskType task) {
        TaskProgressInformation info = new TaskProgressInformation(1, 1, null);
        info.addPart(TaskPartProgressInformation.fromSimpleTask(task));
        return info;
    }

    private void addPart(TaskPartProgressInformation part) {
        parts.put(part.getPartOrHandlerUri(), part);
    }

    public int getAllPartsCount() {
        return allPartsCount;
    }

    public int getCurrentPartNumber() {
        return currentPartNumber;
    }

    /**
     * Returns current part URI (for internally partitioned tasks) or handler URI (for physically partitioned tasks).
     *
     * TODO clarify
     */
    public String getCurrentPartUri() {
        return currentPartUri;
    }

    public TaskPartProgressInformation getCurrentPartInformation() {
        return parts.get(currentPartUri);
    }

    public Map<String, TaskPartProgressInformation> getParts() {
        return parts;
    }

    @Override
    public String toString() {
        return "TaskProgressInformation{" +
                "allPartsCount=" + allPartsCount +
                ", currentPartNumber=" + currentPartNumber +
                ", currentPartUri=" + currentPartUri +
                ", parts=" + parts +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "All parts count", allPartsCount, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Current part number", currentPartNumber, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Current part URI", currentPartUri, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Parts", parts, indent + 1);
        return sb.toString();
    }
}
