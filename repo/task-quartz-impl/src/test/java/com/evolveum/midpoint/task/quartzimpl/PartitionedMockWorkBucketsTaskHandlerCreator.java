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
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.StaticTaskPartitionsDefinition;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskPartitionsDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author mederly
 *
 */
public class PartitionedMockWorkBucketsTaskHandlerCreator {

	private TaskManager taskManager;
	private PrismContext prismContext;

	public PartitionedMockWorkBucketsTaskHandlerCreator(TaskManager taskManager,
			PrismContext prismContext) {
		this.taskManager = taskManager;
		this.prismContext = prismContext;
	}

	@SuppressWarnings("Duplicates")
	public void initializeAndRegister(String handlerUri) {
		taskManager.createAndRegisterPartitioningTaskHandler(handlerUri, this::createPartitioningDefinition);
	}

	// mimics PartitionedReconciliationTaskHandlerCreator
	private TaskPartitionsDefinition createPartitioningDefinition(Task masterTask) {
		TaskPartitionsDefinitionType definitionInTask = masterTask.getWorkManagement() != null ?
				masterTask.getWorkManagement().getPartitions() : null;
		TaskPartitionsDefinitionType partitionsDefinition = definitionInTask != null ?
				definitionInTask.clone() : new TaskPartitionsDefinitionType();
		partitionsDefinition.setCount(3);
		partitionsDefinition.setCopyMasterExtension(true);
		return new StaticTaskPartitionsDefinition(partitionsDefinition,
				prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class));
	}
}
