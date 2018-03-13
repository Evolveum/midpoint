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

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.StaticTaskPartitioningStrategy.StaticTaskPartition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateConfigurationType;
import org.jetbrains.annotations.Nullable;

/**
 * @author mederly
 *
 */
public class PartitionedMockWorkBucketsTaskHandlerCreator {

	private TaskManager taskManager;

	public PartitionedMockWorkBucketsTaskHandlerCreator(TaskManager taskManager) {
		this.taskManager = taskManager;
	}

	@SuppressWarnings("Duplicates")
	public void initializeAndRegister(String handlerUri) {
		StaticTaskPartition first = new StaticTaskPartition();
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
		StaticTaskPartitioningStrategy partitioningStrategy = new StaticTaskPartitioningStrategy();
		partitioningStrategy.setCopyMasterExtension(true);
		partitioningStrategy.setTaskPartitions(first, second, third);

		taskManager.createAndRegisterPartitioningTaskHandler(handlerUri, partitioningStrategy);
	}

	@Nullable
	private TaskWorkStateConfigurationType getWorkStateConfig2(Task masterTask) {
		PrismProperty<TaskWorkStateConfigurationType> cfg = masterTask.getExtensionProperty(
				SchemaConstants.MODEL_EXTENSION_RESOURCE_RECONCILIATION_WORK_STATE_CONFIGURATION);
		return TaskUtil.adaptHandlerUri(cfg, AbstractTaskManagerTest.PARTITIONED_WB_TASK_HANDLER_URI_2);
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
		return TaskUtil.adaptHandlerUri(cfg, AbstractTaskManagerTest.PARTITIONED_WB_TASK_HANDLER_URI_2);
	}

	private boolean hasWorkStateConfig3(Task masterTask) {
		PrismProperty<TaskWorkStateConfigurationType> cfg = masterTask.getExtensionProperty(
				SchemaConstants.MODEL_EXTENSION_SHADOW_RECONCILIATION_WORK_STATE_CONFIGURATION);
		return cfg != null && cfg.getRealValue() != null;
	}
}
