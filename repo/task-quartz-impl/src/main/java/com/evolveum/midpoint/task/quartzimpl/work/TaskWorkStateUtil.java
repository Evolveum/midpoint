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

package com.evolveum.midpoint.task.quartzimpl.work;

import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.task.api.Task;

import java.util.List;

/**
 * Companion to TaskWorkStateTypeUtil. However, here are methods that cannot be implemented in schema layer
 * e.g. because they require Task objects.
 *
 * @author mederly
 */
public class TaskWorkStateUtil {

	public static Task findWorkerByBucketNumber(List<Task> workers, int sequentialNumber) {
		for (Task worker : workers) {
			if (worker.getWorkState() != null && TaskWorkStateTypeUtil
					.findBucketByNumber(worker.getWorkState().getBucket(), sequentialNumber) != null) {
				return worker;
			}
		}
		return null;
	}

}
