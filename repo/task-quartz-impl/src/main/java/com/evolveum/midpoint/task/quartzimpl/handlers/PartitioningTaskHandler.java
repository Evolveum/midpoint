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
import com.evolveum.midpoint.task.api.TaskPartitionsDefinition.TaskPartitionDefinition;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.TemplateUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Task that partitions the work into subtasks.
 * Partitioning is driven by a TaskPartitionsDefinition.
 *
 * @author mederly
 */
public class PartitioningTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(PartitioningTaskHandler.class);

	private static final String DEFAULT_HANDLER_URI = "{masterTaskHandlerUri}#{index}";

	private TaskManager taskManager;
	private Function<Task, TaskPartitionsDefinition> partitionsDefinitionSupplier;

	public PartitioningTaskHandler(TaskManager taskManager, Function<Task, TaskPartitionsDefinition> partitionsDefinitionSupplier) {
		this.taskManager = taskManager;
		this.partitionsDefinitionSupplier = partitionsDefinitionSupplier;
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
		TaskPartitionsDefinition partitionsDefinition = partitionsDefinitionSupplier.apply(masterTask);
		List<String> subtaskOids = new ArrayList<>();
		int count = partitionsDefinition.getCount(masterTask);
		for (int i = 1; i <= count; i++) {
			subtaskOids.add(createSubtask(i, partitionsDefinition, masterTask, opResult));
		}
		List<Task> subtasks = new ArrayList<>(subtaskOids.size());
		for (String subtaskOid : subtaskOids) {
			subtasks.add(taskManager.getTask(subtaskOid, opResult));
		}
		boolean sequential = partitionsDefinition.isSequentialExecution(masterTask);
		for (int i = 1; i <= count; i++) {
			Task subtask = subtasks.get(i - 1);
			TaskPartitionDefinition partition = partitionsDefinition.getPartition(masterTask, i);
			Collection<Integer> dependents = new HashSet<>(partition.getDependents());
			if (sequential && i < count) {
				dependents.add(i + 1);
			}
			for (Integer dependentIndex : dependents) {
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

	private String createSubtask(int index, TaskPartitionsDefinition partitionsDefinition,
			Task masterTask, OperationResult opResult) throws SchemaException, ObjectAlreadyExistsException {
		Map<String, String> replacements = new HashMap<>();
		replacements.put("index", String.valueOf(index));
		replacements.put("masterTaskName", String.valueOf(masterTask.getName().getOrig()));
		replacements.put("masterTaskHandlerUri", masterTask.getHandlerUri());

		TaskPartitionDefinition partition = partitionsDefinition.getPartition(masterTask, index);

		TaskType masterTaskBean = masterTask.getTaskType();
		TaskType subtask = new TaskType(getPrismContext());

		String nameTemplate = applyDefaults(
				p -> p.getName(masterTask),
				ps -> ps.getName(masterTask),
				"{masterTaskName} ({index})", partition, partitionsDefinition);
		String name = TemplateUtil.replace(nameTemplate, replacements);
		subtask.setName(PolyStringType.fromOrig(name));

		TaskWorkManagementType workManagement = applyDefaults(
				p -> p.getWorkManagement(masterTask),
				ps -> ps.getWorkManagement(masterTask),
				null, partition, partitionsDefinition);
		// work management is updated and stored into subtask later

		String handlerUriTemplate = applyDefaults(
				p -> p.getHandlerUri(masterTask),
				ps -> ps.getHandlerUri(masterTask),
				null,
				partition, partitionsDefinition);
		String handlerUri = TemplateUtil.replace(handlerUriTemplate, replacements);
		if (handlerUri == null) {
			// The default for coordinator-based partitions is to put default handler into workers configuration
			// - but only if both partition and workers handler URIs are null. This is to be revisited some day.
			if (isCoordinator(workManagement)) {
				handlerUri = TaskConstants.WORKERS_CREATION_TASK_HANDLER_URI;
				if (workManagement.getWorkers() != null && workManagement.getWorkers().getHandlerUri() == null) {
					workManagement = workManagement.clone();
					workManagement.getWorkers().setHandlerUri(TemplateUtil.replace(DEFAULT_HANDLER_URI, replacements));
				}
			} else {
				handlerUri = TemplateUtil.replace(DEFAULT_HANDLER_URI, replacements);
			}
		}
		subtask.setHandlerUri(handlerUri);
		subtask.setWorkManagement(workManagement);

		subtask.setExecutionStatus(TaskExecutionStatusType.SUSPENDED);
		subtask.setOwnerRef(CloneUtil.clone(masterTaskBean.getOwnerRef()));
		subtask.setCategory(masterTask.getCategory());
		subtask.setObjectRef(CloneUtil.clone(masterTaskBean.getObjectRef()));
		subtask.setRecurrence(TaskRecurrenceType.SINGLE);
		subtask.setParent(masterTask.getTaskIdentifier());
		boolean copyMasterExtension = applyDefaults(
				p -> p.isCopyMasterExtension(masterTask),
				ps -> ps.isCopyMasterExtension(masterTask),
				false, partition, partitionsDefinition);
		if (copyMasterExtension) {
			PrismContainer<Containerable> masterExtension = masterTaskBean.asPrismObject().findContainer(TaskType.F_EXTENSION);
			if (masterTaskBean.getExtension() != null) {
				subtask.asPrismObject().add(masterExtension.clone());
			}
		}

		applyDeltas(subtask, partition.getOtherDeltas(masterTask));
		applyDeltas(subtask, partitionsDefinition.getOtherDeltas(masterTask));
		LOGGER.debug("Partitioned subtask to be created:\n{}", subtask.asPrismObject().debugDumpLazily());

		return taskManager.addTask(subtask.asPrismObject(), opResult);
	}

	private boolean isCoordinator(TaskWorkManagementType workManagement) {
		return workManagement != null && workManagement.getTaskKind() == TaskKindType.COORDINATOR;
	}

	private <T> T applyDefaults(Function<TaskPartitionDefinition, T> localGetter, Function<TaskPartitionsDefinition, T> globalGetter,
			T defaultValue, TaskPartitionDefinition localDef, TaskPartitionsDefinition globalDef) {
		T localValue = localGetter.apply(localDef);
		if (localValue != null) {
			return localValue;
		}
		T globalValue = globalGetter.apply(globalDef);
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
