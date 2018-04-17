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
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 *
 */
@Component
public class WorkersCreationTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(WorkersCreationTaskHandler.class);
	public static final String HANDLER_URI = TaskConstants.WORKERS_CREATION_TASK_HANDLER_URI;

	@Autowired private TaskManager taskManager;
	@Autowired private PrismContext prismContext;
	@Autowired private RepositoryService repositoryService;

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@Override
	public TaskRunResult run(Task task) {
		
		OperationResult opResult = new OperationResult(WorkersCreationTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();
		runResult.setProgress(task.getProgress());
		runResult.setOperationResult(opResult);

		try {
			setOrCheckTaskKind(task, opResult);
			List<Task> workers = task.listSubtasks(opResult);
			List<Task> workersNotClosed = workers.stream()
					.filter(w -> w.getExecutionStatus() != TaskExecutionStatus.CLOSED)
					.collect(Collectors.toList());
			// todo consider checking that the subtask is really a worker (workStateConfiguration.taskKind)
			if (!workersNotClosed.isEmpty()) {
				LOGGER.warn("Couldn't (re)create worker tasks because the following ones are not closed yet: {}", workersNotClosed);
				opResult.recordFatalError("Couldn't (re)create worker tasks because the following ones are not closed yet: " + workersNotClosed);
				runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
				return runResult;
			}

			if (deleteWorkersAndBuckets(workers, task, opResult, runResult)) {
				return runResult;
			}
			taskManager.reconcileWorkers(task.getOid(), opResult);
			task.makeWaiting(TaskWaitingReason.OTHER_TASKS, TaskUnpauseActionType.RESCHEDULE);  // i.e. close for single-run tasks
			task.savePendingModifications(opResult);
			taskManager.resumeTasks(TaskUtil.tasksToOids(task.listSubtasks(opResult)), opResult);
			LOGGER.info("Worker tasks were successfully created for coordinator {}", task);
		} catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't (re)create workers for {}", e, task);
			opResult.recordFatalError("Couldn't (re)create workers", e);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}
		runResult.setProgress(runResult.getProgress() + 1);
		opResult.computeStatusIfUnknown();
		runResult.setRunResultStatus(TaskRunResultStatus.IS_WAITING);
		return runResult;
	}

	private void setOrCheckTaskKind(Task task, OperationResult opResult)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		TaskKindType taskKind = task.getWorkManagement() != null ? task.getWorkManagement().getTaskKind() : null;
		if (taskKind == null) {
			ItemDelta<?, ?> itemDelta = DeltaBuilder.deltaFor(TaskType.class, prismContext)
					.item(TaskType.F_WORK_MANAGEMENT, TaskWorkManagementType.F_TASK_KIND)
					.replace(TaskKindType.COORDINATOR)
					.asItemDelta();
			task.addModificationImmediate(itemDelta, opResult);
		} else if (taskKind != TaskKindType.COORDINATOR) {
			throw new IllegalStateException("Task has incompatible task kind; expected " + TaskKindType.COORDINATOR +
					" but having: " + task.getWorkManagement() + " in " + task);
		}
	}

	// returns true in case of problem
	private boolean deleteWorkersAndBuckets(List<Task> workers, Task task, OperationResult opResult, TaskRunResult runResult)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		deleteBuckets(task, opResult);
		for (Task worker : workers) {
			try {
				List<Task> workerSubtasks = worker.listSubtasks(opResult);
				if (!workerSubtasks.isEmpty()) {
					LOGGER.warn("Couldn't recreate worker task {} because it has its own subtasks: {}", worker, workerSubtasks);
					opResult.recordFatalError("Couldn't recreate worker task " + worker + " because it has its own subtasks: " + workerSubtasks);
					runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
					return true;
				}
				taskManager.deleteTask(worker.getOid(), opResult);
			} catch (ObjectNotFoundException | SchemaException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete worker task {} (coordinator {})", e, worker, task);
			}
		}
		return false;
	}

	private void deleteBuckets(Task task, OperationResult opResult) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_WORK_STATE, TaskWorkStateType.F_BUCKET).replace()
				.asItemDeltas();
		repositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, opResult);
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }
}
