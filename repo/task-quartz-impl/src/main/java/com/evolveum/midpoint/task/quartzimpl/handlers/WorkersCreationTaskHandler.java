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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.TemplateUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

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
			if (createWorkers(task, opResult)) {
				return runResult;
			}
			task.makeWaiting(TaskWaitingReason.OTHER_TASKS, TaskUnpauseActionType.RESCHEDULE);  // i.e. close for single-run tasks
			task.savePendingModifications(opResult);
			LOGGER.info("Worker tasks were successfully created for coordinator {}", task);
		} catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't restart workers for {}", e, task);
			opResult.recordFatalError("Couldn't restart workers", e);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}
		runResult.setProgress(runResult.getProgress() + 1);
		opResult.computeStatusIfUnknown();
		runResult.setRunResultStatus(TaskRunResultStatus.IS_WAITING);
		return runResult;
	}

	// returns true if there's a problem
	private boolean createWorkers(Task task, OperationResult opResult)
			throws SchemaException, ObjectAlreadyExistsException {
		TaskWorkManagementType wsCfg = task.getWorkManagement();
		if (wsCfg == null || wsCfg.getTaskKind() != TaskKindType.COORDINATOR) {
			throw new IllegalStateException("Work state configuration missing or task kind is not 'coordinator': " + task);
		}
		WorkersManagementType workersCfg = wsCfg.getWorkers();
		if (workersCfg == null) {
			throw new IllegalStateException("Workers configuration is missing: " + task);
		}
		for (WorkerTasksPerNodeConfigurationType perNodeConfig : getWorkersPerNode(workersCfg)) {
			for (String nodeIdentifier : getNodeIdentifiers(perNodeConfig, opResult)) {
				int count = defaultIfNull(perNodeConfig.getCount(), 1);
				for (int index = 1; index <= count; index++) {
					createWorker(nodeIdentifier, index, perNodeConfig, workersCfg, task, opResult);
				}
			}
		}
		return false;
	}

	private void createWorker(String nodeIdentifier, int index, WorkerTasksPerNodeConfigurationType perNodeConfig,
			WorkersManagementType workersCfg, Task coordinatorTask, OperationResult opResult)
			throws SchemaException, ObjectAlreadyExistsException {
		Map<String, String> replacements = new HashMap<>();
		replacements.put("node", nodeIdentifier);
		replacements.put("index", String.valueOf(index));
		replacements.put("coordinatorTaskName", coordinatorTask.getName().getOrig());

		String nameTemplate;
		if (perNodeConfig.getTaskName() != null) {
			nameTemplate = perNodeConfig.getTaskName();
		} else if (workersCfg.getTaskName() != null) {
			nameTemplate = workersCfg.getTaskName();
		} else {
			nameTemplate = "{coordinatorTaskName} ({node}:{index})";
		}

		TaskType coordinatorTaskBean = coordinatorTask.getTaskType();
		TaskType worker = new TaskType(prismContext);

		String name = TemplateUtil.replace(nameTemplate, replacements);
		worker.setName(PolyStringType.fromOrig(name));

		String executionGroupTemplate = defaultIfNull(perNodeConfig.getExecutionGroup(), "{node}");
		String executionGroup = MiscUtil.nullIfEmpty(TemplateUtil.replace(executionGroupTemplate, replacements));
		if (executionGroup != null) {
			worker.beginExecutionConstraints().group(executionGroup).end();
		}

		worker.setHandlerUri(workersCfg.getHandlerUri());

		applyDeltas(worker, workersCfg.getOtherDeltas());
		applyDeltas(worker, perNodeConfig.getOtherDeltas());

		worker.setExecutionStatus(TaskExecutionStatusType.RUNNABLE);
		worker.setOwnerRef(CloneUtil.clone(coordinatorTaskBean.getOwnerRef()));
		worker.setCategory(coordinatorTask.getCategory());
		worker.setObjectRef(CloneUtil.clone(coordinatorTaskBean.getObjectRef()));
		worker.setRecurrence(TaskRecurrenceType.SINGLE);
		worker.setParent(coordinatorTask.getTaskIdentifier());
		worker.beginWorkManagement().taskKind(TaskKindType.WORKER);
		PrismContainer<Containerable> coordinatorExtension = coordinatorTaskBean.asPrismObject().findContainer(TaskType.F_EXTENSION);
		if (coordinatorTaskBean.getExtension() != null) {
			worker.asPrismObject().add(coordinatorExtension.clone());
		}
		taskManager.addTask(worker.asPrismObject(), opResult);
	}

	private void applyDeltas(TaskType worker, List<ItemDeltaType> deltas) throws SchemaException {
		Collection<? extends ItemDelta> itemDeltas = DeltaConvertor.toModifications(deltas, worker.asPrismObject().getDefinition());
		ItemDelta.applyTo(itemDeltas, worker.asPrismContainerValue());
	}

	private Collection<String> getNodeIdentifiers(WorkerTasksPerNodeConfigurationType perNodeConfig, OperationResult opResult)
			throws SchemaException {
		if (!perNodeConfig.getNodeIdentifier().isEmpty()) {
			return perNodeConfig.getNodeIdentifier();
		} else {
			SearchResultList<PrismObject<NodeType>> nodes = repositoryService.searchObjects(NodeType.class, null, null, opResult);
			return nodes.stream().map(n -> n.asObjectable().getNodeIdentifier()).collect(Collectors.toSet());
		}
	}

	private List<WorkerTasksPerNodeConfigurationType> getWorkersPerNode(WorkersManagementType workersCfg) {
		if (!workersCfg.getWorkersPerNode().isEmpty()) {
			return workersCfg.getWorkersPerNode();
		} else {
			return singletonList(new WorkerTasksPerNodeConfigurationType());
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
