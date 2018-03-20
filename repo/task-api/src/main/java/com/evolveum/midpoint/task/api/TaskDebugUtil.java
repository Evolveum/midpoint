/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
