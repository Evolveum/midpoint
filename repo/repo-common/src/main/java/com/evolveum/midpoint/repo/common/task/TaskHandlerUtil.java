/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.List;

/**
 * @author Pavol Mederly
 */
public class TaskHandlerUtil {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskHandlerUtil.class);

    public static void appendLastFailuresInformation(String operationNamePrefix, Task task, OperationResult result) {
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
