/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

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
                .append(" [").append(task.getExecutionStatus())
                .append(", ").append(task.getProgress())
                .append(", ").append(task.getNode())
                .append("]").append("\n");
        List<Task> subtasks = task.listSubtasks(result);
        for (Task subtask : subtasks) {
            dumpTaskTree(sb, indent + 1, subtask, result);
        }
    }
}
