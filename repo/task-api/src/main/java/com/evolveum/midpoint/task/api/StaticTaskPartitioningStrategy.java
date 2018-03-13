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

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateConfigurationType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Task partitioning strategy based on statically configured values. More restricted but easier to use.
 * However, it is extensible by subclassing - in that way, some values can be provided statically while others on-demand.
 *
 * @author mederly
 */
@SuppressWarnings("unused")
public class StaticTaskPartitioningStrategy implements TaskPartitioningStrategy {

	private TaskPartitionInformation[] taskPartitions;
	private String taskNameTemplate;
	private String handlerUriTemplate;
	private Boolean copyWorkStateConfiguration;
	private Boolean copyMasterExtension;
	@NotNull private Collection<ItemDelta<?, ?>> otherDeltas = Collections.emptySet();

	public static class StaticTaskPartition implements TaskPartitionInformation {

		private String taskNameTemplate;
		private String handlerUriTemplate;
		private Boolean copyWorkStateConfiguration;
		private TaskWorkStateConfigurationType workStateConfiguration;
		private Boolean copyMasterExtension;
		@NotNull private Collection<ItemDelta<?, ?>> otherDeltas = Collections.emptySet();
		@NotNull private Collection<Integer> dependents = new HashSet<>();

		@Override
		public String getTaskNameTemplate(Task masterTask) {
			return taskNameTemplate;
		}

		public void setTaskNameTemplate(String taskNameTemplate) {
			this.taskNameTemplate = taskNameTemplate;
		}

		@Override
		public String getHandlerUriTemplate(Task masterTask) {
			return handlerUriTemplate;
		}

		public void setHandlerUriTemplate(String handlerUriTemplate) {
			this.handlerUriTemplate = handlerUriTemplate;
		}

		@Override
		public Boolean getCopyWorkStateConfiguration(Task masterTask) {
			return copyWorkStateConfiguration;
		}

		public void setCopyWorkStateConfiguration(Boolean copyWorkStateConfiguration) {
			this.copyWorkStateConfiguration = copyWorkStateConfiguration;
		}

		@Override
		public TaskWorkStateConfigurationType getWorkStateConfiguration(Task masterTask) {
			return workStateConfiguration;
		}

		public void setWorkStateConfiguration(TaskWorkStateConfigurationType workStateConfiguration) {
			this.workStateConfiguration = workStateConfiguration;
		}

		@Override
		public Boolean getCopyMasterExtension(Task masterTask) {
			return copyMasterExtension;
		}

		public void setCopyMasterExtension(Boolean copyMasterExtension) {
			this.copyMasterExtension = copyMasterExtension;
		}

		@Override
		@NotNull
		public Collection<ItemDelta<?, ?>> getOtherDeltas(Task masterTask) {
			return otherDeltas;
		}

		public void setOtherDeltas(@NotNull Collection<ItemDelta<?, ?>> otherDeltas) {
			this.otherDeltas = otherDeltas;
		}

		@Override
		@NotNull
		public Collection<Integer> getDependents() {
			return dependents;
		}

		public void addDependent(int index) {
			dependents.add(index);
		}
	}

	@Override
	public int getPartitionsCount(Task masterTask) {
		return taskPartitions.length;
	}

	@NotNull
	@Override
	public TaskPartitionInformation getPartition(Task masterTask, int index) {
		return taskPartitions[index - 1];
	}

	public TaskPartitionInformation[] getTaskPartitions() {
		return taskPartitions;
	}

	public void setTaskPartitions(TaskPartitionInformation... taskPartitions) {
		this.taskPartitions = taskPartitions;
	}

	@Override
	public String getTaskNameTemplate(Task masterTask) {
		return taskNameTemplate;
	}

	public void setTaskNameTemplate(String taskNameTemplate) {
		this.taskNameTemplate = taskNameTemplate;
	}

	@Override
	public String getHandlerUriTemplate(Task masterTask) {
		return handlerUriTemplate;
	}

	public void setHandlerUriTemplate(String handlerUriTemplate) {
		this.handlerUriTemplate = handlerUriTemplate;
	}

	@Override
	public Boolean getCopyWorkStateConfiguration(Task masterTask) {
		return copyWorkStateConfiguration;
	}

	public void setCopyWorkStateConfiguration(Boolean copyWorkStateConfiguration) {
		this.copyWorkStateConfiguration = copyWorkStateConfiguration;
	}

	public Boolean getCopyMasterExtension(Task masterTask) {
		return copyMasterExtension;
	}

	public void setCopyMasterExtension(Boolean copyMasterExtension) {
		this.copyMasterExtension = copyMasterExtension;
	}

	@Override
	@NotNull
	public Collection<ItemDelta<?, ?>> getOtherDeltas(Task masterTask) {
		return otherDeltas;
	}

	public void setOtherDeltas(@NotNull Collection<ItemDelta<?, ?>> otherDeltas) {
		this.otherDeltas = otherDeltas;
	}
}
