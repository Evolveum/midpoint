/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.util.List;

/**
 * @author mederly
 */
public class TaskDebugUtil {

    public static String dumpTaskTree(Task rootTask, OperationResult result) throws SchemaException {
        StringBuilder sb = new StringBuilder();
        dumpTaskTree(sb, 0, rootTask, result);
        return sb.toString();
    }

    private static void dumpTaskTree(StringBuilder sb, int indent, Task task, OperationResult result) throws SchemaException {
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(task)
                .append(" [es:").append(task.getExecutionState())
                .append(", rs:").append(task.getResultStatus())
                .append(", p:").append(task.getProgress())
                .append(", n:").append(task.getNode())
                .append("]").append("\n");
        List<Task> subtasks = task.listSubtasks(result);
        for (Task subtask : subtasks) {
            dumpTaskTree(sb, indent + 1, subtask, result);
        }
    }

    public static String dumpTaskTree(TaskType rootTask) {
        StringBuilder sb = new StringBuilder();
        dumpTaskTree(sb, 0, rootTask);
        return sb.toString();
    }

    private static void dumpTaskTree(StringBuilder sb, int indent, TaskType task) {
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(task)
                .append(" [es:").append(task.getExecutionStatus())
                .append(", rs:").append(task.getResultStatus())
                .append(", p:").append(task.getProgress())
                .append(", n:").append(task.getNode())
                .append("]").append("\n");
        for (ObjectReferenceType subRef : task.getSubtaskRef()) {
            //noinspection unchecked
            PrismObject<TaskType> subtask = subRef.asReferenceValue().getObject();
            if (subtask != null) {
                dumpTaskTree(sb, indent + 1, subtask.asObjectable());
            } else {
                throw new IllegalStateException("Subtask " + subRef + " in " + task + " is not resolved");
            }
        }
    }
}
