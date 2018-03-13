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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskPartitioningStrategy.TaskPartitionInformation;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.TemplateUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskUnpauseActionType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Task that partitions the work into subtasks.
 * Partitioning is driven by a TaskPartitioningStrategy.
 *
 * @author mederly
 */
public class PartitioningTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(PartitioningTaskHandler.class);

	private TaskManager taskManager;
	private TaskPartitioningStrategy partitioningStrategy;

	public PartitioningTaskHandler(TaskManager taskManager, TaskPartitioningStrategy partitioningStrategy) {
		this.taskManager = taskManager;
		this.partitioningStrategy = partitioningStrategy;
	}

	@Override
	public TaskRunResult run(Task masterTask) {
		
		OperationResult opResult = new OperationResult(PartitioningTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();
		runResult.setProgress(masterTask.getProgress());
		runResult.setOperationResult(opResult);

		try {
			// subtasks cleanup
			List<Task> subtasks = masterTask.listSubtasks(opResult);
			List<Task> subtasksNotClosed = subtasks.stream()
					.filter(w -> w.getExecutionStatus() != TaskExecutionStatus.CLOSED)
					.collect(Collectors.toList());
			if (!subtasksNotClosed.isEmpty()) {
				LOGGER.warn("Couldn't (re)create subtasks tasks because the following ones are not closed yet: {}", subtasksNotClosed);
				opResult.recordFatalError("Couldn't (re)create worker tasks because the following ones are not closed yet: " + subtasksNotClosed);
				runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
				return runResult;
			}
			taskManager.suspendAndDeleteTasks(TaskUtil.tasksToOids(subtasks), TaskManager.DO_NOT_WAIT, true, opResult);

			// subtasks creation
			List<Task> subtasksCreated;
			try {
				subtasksCreated = createSubtasks(masterTask, opResult);
			} catch (Throwable t) {
				List<Task> subtasksToRollback = masterTask.listSubtasks(opResult);
				taskManager.suspendAndDeleteTasks(TaskUtil.tasksToOids(subtasksToRollback), TaskManager.DO_NOT_WAIT, true, opResult);
				throw t;
			}
			masterTask.makeWaiting(TaskWaitingReason.OTHER_TASKS, TaskUnpauseActionType.RESCHEDULE);  // i.e. close for single-run tasks
			masterTask.savePendingModifications(opResult);
			List<Task> subtasksToResume = subtasksCreated.stream()
					.filter(t -> t.getExecutionStatus() == TaskExecutionStatus.SUSPENDED)
					.collect(Collectors.toList());
			taskManager.resumeTasks(TaskUtil.tasksToOids(subtasksToResume), opResult);
			LOGGER.info("Partitioned subtasks were successfully created and started for master {}", masterTask);
		} catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't (re)create partitioned subtasks for {}", e, masterTask);
			opResult.recordFatalError("Couldn't (re)create partitioned subtasks", e);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}
		runResult.setProgress(runResult.getProgress() + 1);
		opResult.computeStatusIfUnknown();
		runResult.setRunResultStatus(TaskRunResultStatus.IS_WAITING);
		return runResult;
	}

	/**
	 * Creates subtasks: either suspended (these will be resumed after everything is prepared) or waiting (if they
	 * have dependencies).
	 */
	private List<Task> createSubtasks(Task masterTask, OperationResult opResult)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		List<String> subtaskOids = new ArrayList<>();
		for (int i = 1; i <= partitioningStrategy.getPartitionsCount(masterTask); i++) {
			subtaskOids.add(createSubtask(i, masterTask, opResult));
		}
		List<Task> subtasks = new ArrayList<>(subtaskOids.size());
		for (String subtaskOid : subtaskOids) {
			subtasks.add(taskManager.getTask(subtaskOid, opResult));
		}
		for (int i = 1; i <= partitioningStrategy.getPartitionsCount(masterTask); i++) {
			Task subtask = subtasks.get(i - 1);
			TaskPartitionInformation partition = partitioningStrategy.getPartition(masterTask, i);
			for (Integer dependentIndex : partition.getDependents()) {
				Task dependent = subtasks.get(dependentIndex - 1);
				subtask.addDependent(dependent.getTaskIdentifier());
				if (dependent.getExecutionStatus() == TaskExecutionStatus.SUSPENDED) {
					dependent.makeWaiting(TaskWaitingReason.OTHER_TASKS, TaskUnpauseActionType.EXECUTE_IMMEDIATELY);
					dependent.savePendingModifications(opResult);
				}
			}
			subtask.savePendingModifications(opResult);
		}
		return subtasks;
	}

	private String createSubtask(int index, Task masterTask, OperationResult opResult) throws SchemaException, ObjectAlreadyExistsException {
		Map<String, String> replacements = new HashMap<>();
		replacements.put("index", String.valueOf(index));
		replacements.put("masterTaskName", String.valueOf(masterTask.getName().getOrig()));
		replacements.put("masterTaskHandlerUri", masterTask.getHandlerUri());

		TaskPartitionInformation partition = partitioningStrategy.getPartition(masterTask, index);

		TaskType masterTaskBean = masterTask.getTaskType();
		TaskType subtask = new TaskType(getPrismContext());

		String nameTemplate = applyDefaults(
				p -> p.getTaskNameTemplate(masterTask),
				ps -> ps.getTaskNameTemplate(masterTask),
				"{masterTaskName} ({index})", partition);
		String name = TemplateUtil.replace(nameTemplate, replacements);
		subtask.setName(PolyStringType.fromOrig(name));

		String handlerUriTemplate = applyDefaults(
				p -> p.getHandlerUriTemplate(masterTask),
				ps -> ps.getHandlerUriTemplate(masterTask),
				"{masterTaskHandlerUri}#{index}", partition);
		String handlerUri = TemplateUtil.replace(handlerUriTemplate, replacements);
		subtask.setHandlerUri(handlerUri);

		subtask.setExecutionStatus(TaskExecutionStatusType.SUSPENDED);
		subtask.setOwnerRef(CloneUtil.clone(masterTaskBean.getOwnerRef()));
		subtask.setCategory(masterTask.getCategory());
		subtask.setObjectRef(CloneUtil.clone(masterTaskBean.getObjectRef()));
		subtask.setRecurrence(TaskRecurrenceType.SINGLE);
		subtask.setParent(masterTask.getTaskIdentifier());
		boolean copyMasterExtension = applyDefaults(
				p -> p.getCopyMasterExtension(masterTask),
				ps -> ps.getCopyMasterExtension(masterTask),
				false, partition);
		if (copyMasterExtension) {
			PrismContainer<Containerable> masterExtension = masterTaskBean.asPrismObject().findContainer(TaskType.F_EXTENSION);
			if (masterTaskBean.getExtension() != null) {
				subtask.asPrismObject().add(masterExtension.clone());
			}
		}
		boolean copyWorkStateConfiguration = applyDefaults(
				p -> p.getCopyWorkStateConfiguration(masterTask),
				ps -> ps.getCopyWorkStateConfiguration(masterTask),
				false, partition);
		if (copyWorkStateConfiguration) {
			subtask.setWorkStateConfiguration(CloneUtil.clone(masterTask.getWorkStateConfiguration()));
		} else {
			subtask.setWorkStateConfiguration(partition.getWorkStateConfiguration(masterTask));
		}

		applyDeltas(subtask, partition.getOtherDeltas(masterTask));
		applyDeltas(subtask, partitioningStrategy.getOtherDeltas(masterTask));
		LOGGER.debug("Partitioned subtask to be created:\n{}", subtask.asPrismObject().debugDumpLazily());

		return taskManager.addTask(subtask.asPrismObject(), opResult);
	}

	private <T> T applyDefaults(Function<TaskPartitionInformation, T> localGetter, Function<TaskPartitioningStrategy, T> globalGetter,
			T defaultValue, TaskPartitionInformation partitionInformation) {
		T localValue = localGetter.apply(partitionInformation);
		if (localValue != null) {
			return localValue;
		}
		T globalValue = globalGetter.apply(partitioningStrategy);
		if (globalValue != null) {
			return globalValue;
		}
		return defaultValue;
	}

	private PrismContext getPrismContext() {
		return ((TaskManagerQuartzImpl) taskManager).getPrismContext();
	}

	private void applyDeltas(TaskType subtask, Collection<ItemDelta<?, ?>> deltas) throws SchemaException {
		ItemDelta.applyTo(deltas, subtask.asPrismContainerValue());
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }
}
