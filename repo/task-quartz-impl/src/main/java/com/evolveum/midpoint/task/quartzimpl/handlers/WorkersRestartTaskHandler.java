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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 *
 */
@Component
public class WorkersRestartTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(WorkersRestartTaskHandler.class);
	public static final String HANDLER_URI = TaskConstants.WORKERS_RESTART_TASK_HANDLER_URI;

	@Autowired private TaskManager taskManager;
	@Autowired private PrismContext prismContext;
	@Autowired private RepositoryService repositoryService;

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@Override
	public TaskRunResult run(Task task) {
		
		OperationResult opResult = new OperationResult(WorkersRestartTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);

		try {
			List<Task> workers = task.listSubtasks(true, opResult);
			List<Task> workersNotClosed = workers.stream()
					.filter(w -> w.getExecutionStatus() != TaskExecutionStatus.CLOSED)
					.collect(Collectors.toList());
			// todo consider checking that the subtask is really a worker (workStateConfiguration.taskKind)
			if (!workersNotClosed.isEmpty()) {
				LOGGER.warn("Couldn't restart worker tasks because the following ones are not closed yet: {}", workersNotClosed);
				opResult.recordFatalError("Couldn't restart worker tasks because the following ones are not closed yet: " + workersNotClosed);
				runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
				return runResult;
			}

			deleteBuckets(task, opResult);
			Collection<String> oidsToStart = new HashSet<>();
			for (Task worker : workers) {
				try {
					deleteBuckets(worker, opResult);
					oidsToStart.add(worker.getOid());
				} catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete buckets from worker task {} (coordinator {})", e, worker, task);
				}
			}
			taskManager.scheduleTasksNow(oidsToStart, opResult);
			LOGGER.debug("Worker tasks were started: coordinator = {}, workers = {}", task, oidsToStart);

		} catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't restart workers for {}", e, task);
			opResult.recordFatalError("Couldn't restart workers", e);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}

		opResult.computeStatusIfUnknown();
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		return runResult;
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
