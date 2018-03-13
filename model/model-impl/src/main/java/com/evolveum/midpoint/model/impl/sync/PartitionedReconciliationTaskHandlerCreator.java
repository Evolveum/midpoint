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
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.StaticTaskPartitioningStrategy.StaticTaskPartition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateConfigurationType;
import org.jetbrains.annotations.Nullable;
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

	@PostConstruct
	private void initialize() {
		StaticTaskPartition first = new StaticTaskPartition();
		//first.setTaskNameTemplate("{masterTaskName} (1 - finishing operations)");
		first.addDependent(2);
		StaticTaskPartition second = new StaticTaskPartition() {
			@Override
			public String getHandlerUriTemplate(Task masterTask) {
				if (hasWorkStateConfig2(masterTask)) {
					return TaskConstants.WORKERS_CREATION_TASK_HANDLER_URI;
				} else {
					return null;        // the default one
				}
			}

			@Override
			public TaskWorkStateConfigurationType getWorkStateConfiguration(Task masterTask) {
				return getWorkStateConfig2(masterTask);
			}
		};
		//second.setTaskNameTemplate("{masterTaskName} (2 - resource reconciliation)");
		second.addDependent(3);
		StaticTaskPartition third = new StaticTaskPartition() {
			@Override
			public String getHandlerUriTemplate(Task masterTask) {
				if (hasWorkStateConfig3(masterTask)) {
					return TaskConstants.WORKERS_CREATION_TASK_HANDLER_URI;
				} else {
					return null;        // the default one
				}
			}

			@Override
			public TaskWorkStateConfigurationType getWorkStateConfiguration(Task masterTask) {
				return getWorkStateConfig3(masterTask);
			}
		};
		//third.setTaskNameTemplate("{masterTaskName} (3 - shadow reconciliation)");
		StaticTaskPartitioningStrategy partitioningStrategy = new StaticTaskPartitioningStrategy();
		partitioningStrategy.setCopyMasterExtension(true);
		partitioningStrategy.setTaskPartitions(first, second, third);

		taskManager.createAndRegisterPartitioningTaskHandler(HANDLER_URI, partitioningStrategy);
	}

	@Nullable
	private TaskWorkStateConfigurationType getWorkStateConfig2(Task masterTask) {
		PrismProperty<TaskWorkStateConfigurationType> cfg = masterTask.getExtensionProperty(
				SchemaConstants.MODEL_EXTENSION_RESOURCE_RECONCILIATION_WORK_STATE_CONFIGURATION);
		return TaskUtil.adaptHandlerUri(cfg, ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_2);
	}

	private boolean hasWorkStateConfig2(Task masterTask) {
		PrismProperty<TaskWorkStateConfigurationType> cfg = masterTask.getExtensionProperty(
				SchemaConstants.MODEL_EXTENSION_RESOURCE_RECONCILIATION_WORK_STATE_CONFIGURATION);
		return cfg != null && cfg.getRealValue() != null;
	}

	@Nullable
	private TaskWorkStateConfigurationType getWorkStateConfig3(Task masterTask) {
		PrismProperty<TaskWorkStateConfigurationType> cfg = masterTask.getExtensionProperty(
				SchemaConstants.MODEL_EXTENSION_SHADOW_RECONCILIATION_WORK_STATE_CONFIGURATION);
		return TaskUtil.adaptHandlerUri(cfg, ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_3);
	}

	private boolean hasWorkStateConfig3(Task masterTask) {
		PrismProperty<TaskWorkStateConfigurationType> cfg = masterTask.getExtensionProperty(
				SchemaConstants.MODEL_EXTENSION_SHADOW_RECONCILIATION_WORK_STATE_CONFIGURATION);
		return cfg != null && cfg.getRealValue() != null;
	}

}
