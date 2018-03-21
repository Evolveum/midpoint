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
package com.evolveum.midpoint.task.quartzimpl.handlers;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Creates generic partitioning task handler.
 *
 * @author mederly
 *
 */
@Component
public class GenericPartitioningTaskHandlerCreator {

	public static final String HANDLER_URI = TaskConstants.GENERIC_PARTITIONING_TASK_HANDLER_URI;

	@Autowired private TaskManager taskManager;
	@Autowired private PrismContext prismContext;

	@PostConstruct
	private void initialize() {
		taskManager.createAndRegisterPartitioningTaskHandler(HANDLER_URI, this::createPartitionsDefinition);
	}

	private TaskPartitionsDefinition createPartitionsDefinition(Task masterTask) {
		if (masterTask.getWorkManagement() == null || masterTask.getWorkManagement().getPartitions() == null) {
			throw new IllegalStateException("No partitions definition in task " + masterTask);
		}
		return new StaticTaskPartitionsDefinition(masterTask.getWorkManagement().getPartitions(),
				prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class));
	}
}
