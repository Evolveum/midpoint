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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * @author mederly
 */
public class TaskTypeUtil {

	/**
	 * Returns a stream of the task and all of its subtasks.
	 */
	@NotNull
	public static Stream<TaskType> getAllTasksStream(TaskType root) {
		return Stream.concat(Stream.of(root),
				root.getSubtask().stream().flatMap(TaskTypeUtil::getAllTasksStream));
	}
}
