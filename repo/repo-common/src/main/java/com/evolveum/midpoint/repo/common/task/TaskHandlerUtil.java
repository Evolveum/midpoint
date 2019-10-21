/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.List;

/**
 * @author Pavol Mederly
 */
public class TaskHandlerUtil {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskHandlerUtil.class);

    public static void appendLastFailuresInformation(String operationNamePrefix, RunningTask task, OperationResult result) {
        appendLastFailuresInformation(operationNamePrefix, task, false, result);
        for (Task subtask : task.getLightweightAsynchronousSubtasks()) {
            appendLastFailuresInformation(operationNamePrefix, subtask, true, result);
        }
    }
    private static void appendLastFailuresInformation(String operationNamePrefix, Task task, boolean subtask, OperationResult result) {
        List<String> failures = task.getLastFailures();
        if (!failures.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (failures.size() < IterativeTaskInformation.LAST_FAILURES_KEPT) {
                sb.append("Failures (").append(failures.size()).append(")");
            } else {
                sb.append("Last ").append(IterativeTaskInformation.LAST_FAILURES_KEPT).append(" failures");
            }
            if (subtask) {
                sb.append(" in subtask ").append(task.getName());
            }
            sb.append(":\n");
            failures.forEach(f -> sb.append(f).append("\n"));
            result.createSubresult(operationNamePrefix + ".errors")
                    .recordStatus(OperationResultStatus.NOT_APPLICABLE, sb.toString());
        }
    }

}
