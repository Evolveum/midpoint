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
package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.StaticTaskPartitioningDefinition;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskPartitioningDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Creates the task handler for partitioned reconciliation. (A bit awkward idea but it stems from the fact that the handler
 * itself resides in task-quartz-impl. This class serves only as the point of instantiation and configuration of that handler.)
 *
 * ---
 * Task handler created here is responsible for creating subtasks that would deal with particular phases
 * of the reconciliation process:
 *
 * 1. finishing operations
 * 2. iterating through resource objects and synchronizing them
 * 3. iterating through shadows and checking their existence
 *
 * @author mederly
 *
 */
@Component
public class PartitionedReconciliationTaskHandlerCreator {

	public static final String HANDLER_URI = ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI;

	@Autowired private TaskManager taskManager;
	@Autowired private PrismContext prismContext;

	@PostConstruct
	private void initialize() {
		taskManager.createAndRegisterPartitioningTaskHandler(HANDLER_URI, this::createPartitioningDefinition);
	}

	private TaskPartitioningDefinition createPartitioningDefinition(Task masterTask) {
		TaskPartitionsDefinitionType definitionInTask = masterTask.getWorkManagement() != null ?
				masterTask.getWorkManagement().getPartitions() : null;
		TaskPartitionsDefinitionType partitioningDefinition = definitionInTask != null ?
				definitionInTask.clone() : new TaskPartitionsDefinitionType();
		partitioningDefinition.setCount(3);
		partitioningDefinition.setCopyMasterExtension(true);
		return new StaticTaskPartitioningDefinition(partitioningDefinition,
				prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class));
	}
}
