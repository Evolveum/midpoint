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

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkersManagementType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class TaskUtil {

	public static List<String> tasksToOids(List<Task> tasks) {
		return tasks.stream().map(Task::getOid).collect(Collectors.toList());
	}

	public static TaskWorkManagementType adaptHandlerUri(PrismProperty<TaskWorkManagementType> cfg,
			String handlerUri) {
		if (cfg == null || cfg.getRealValue() == null) {
			return null;
		} else if (cfg.getRealValue().getWorkers() != null && cfg.getRealValue().getWorkers().getHandlerUri() != null) {
			return cfg.getRealValue();
		} else {
			TaskWorkManagementType clone = cfg.getRealValue().clone();
			if (clone.getWorkers() == null) {
				clone.setWorkers(new WorkersManagementType());
			}
			clone.getWorkers().setHandlerUri(handlerUri);
			return clone;
		}
	}
}
