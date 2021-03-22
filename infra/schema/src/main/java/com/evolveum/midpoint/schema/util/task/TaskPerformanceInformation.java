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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Quickly hacked "API" presenting task performance information.
 *
 * Use with care. Will change in next midPoint release.
 *
 * TODO deduplicate with {@link TaskProgressInformation}.
 */
@Experimental
public class TaskPerformanceInformation implements DebugDumpable, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(TaskPerformanceInformation.class);

    /**
     * Information on the progress in individual parts. Indexed by part URI.
     */
    private final Map<String, TaskPartPerformanceInformation> parts = new HashMap<>();

    private TaskPerformanceInformation() {
    }

    /**
     * Precondition: the task contains fully retrieved and resolved subtasks.
     */
    public static TaskPerformanceInformation fromTaskTree(TaskType task) {
        if (TaskWorkStateUtil.isPartitionedMaster(task)) {
            return fromPartitionedMaster(task);
        } else {
            return fromOtherTask(task);
        }
    }

    @NotNull
    private static TaskPerformanceInformation fromPartitionedMaster(TaskType task) {
        Map<String, TaskPartPerformanceInformation> partsByUri = new HashMap<>();
        List<ObjectReferenceType> subtasks = task.getSubtaskRef();

        for (ObjectReferenceType subtaskRef : subtasks) {
            TaskType subtask = (TaskType)
                    requireNonNull(subtaskRef.asReferenceValue().getObject(),
                            () -> "Task " + task + " has unresolved subtask: " + subtaskRef)
                            .asObjectable();

            TaskPerformanceInformation subInfo = fromOtherTask(subtask);
            if (subInfo.parts.size() > 1) {
                LOGGER.warn("Partitioned task has more than one part - ignoring: {}\n{}", subtask, subInfo.parts);
            } else {
                partsByUri.putAll(subInfo.parts);
            }
        }

        TaskPerformanceInformation info = new TaskPerformanceInformation();
        info.parts.putAll(partsByUri);
        return info;
    }

    /**
     * Not a partitioned master.
     */
    @NotNull
    private static TaskPerformanceInformation fromOtherTask(@NotNull TaskType task) {
        TaskPerformanceInformation info = new TaskPerformanceInformation();
        StructuredTaskProgressType progress = TaskProgressUtil.getStructuredProgressFromTree(task);
        OperationStatsType operationStats = TaskOperationStatsUtil.getOperationStatsFromTree(task, PrismContext.get());
        if (operationStats != null && operationStats.getIterativeTaskInformation() != null) {
            for (IterativeTaskPartItemsProcessingInformationType part : operationStats.getIterativeTaskInformation().getPart()) {
                info.addPart(TaskPartPerformanceInformation.forPart(part, progress));
            }
        }
        return info;
    }

    private void addPart(TaskPartPerformanceInformation part) {
        parts.put(part.getPartUri(), part);
    }

    public Map<String, TaskPartPerformanceInformation> getParts() {
        return parts;
    }

    @Override
    public String toString() {
        return "TaskPerformanceInformation{" +
                "parts=" + parts +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabel(sb, "Parts", parts, indent + 1);
        return sb.toString();
    }
}
