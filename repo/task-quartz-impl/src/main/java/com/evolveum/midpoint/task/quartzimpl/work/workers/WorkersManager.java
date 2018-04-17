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

package com.evolveum.midpoint.task.quartzimpl.work.workers;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.TemplateUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Manages worker tasks.
 *
 * @author mederly
 */
@Component
public class WorkersManager {

	private static final Trace LOGGER = TraceManager.getTrace(WorkersManager.class);

	@Autowired private PrismContext prismContext;
	@Autowired private TaskManager taskManager;
	@Autowired private RepositoryService repositoryService;

	public void reconcileWorkers(String coordinatorTaskOid, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		Task coordinatorTask = taskManager.getTask(coordinatorTaskOid, result);
		if (coordinatorTask.getKind() != TaskKindType.COORDINATOR) {
			throw new IllegalArgumentException("Task is not a coordinator task: " + coordinatorTask);
		}
		List<Task> currentWorkers = new ArrayList<>(coordinatorTask.listSubtasks(result));
		Map<WorkerKey, WorkerTasksPerNodeConfigurationType> perNodeConfigurationMap = new HashMap<>();
		MultiValuedMap<String, WorkerKey> shouldBeWorkers = createWorkerKeys(coordinatorTask, perNodeConfigurationMap, result);

		int startingWorkersCount = currentWorkers.size();
		int startingShouldBeWorkersCount = shouldBeWorkers.size();

		currentWorkers.sort(Comparator.comparing(t -> {
			if (t.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
				if (t.getNodeAsObserved() != null) {
					return 0;
				} else if (t.getNode() != null) {
					return 1;
				} else {
					return 2;
				}
			} else if (t.getExecutionStatus() == TaskExecutionStatus.SUSPENDED) {
				return 3;
			} else if (t.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
				return 4;
			} else {
				return 5;
			}
		}));

		LOGGER.trace("Before reconciliation:\nCurrent workers: {}\nShould be workers: {}", currentWorkers, shouldBeWorkers);

		int matched = matchWorkers(currentWorkers, shouldBeWorkers);
		int renamed = renameWorkers(currentWorkers, shouldBeWorkers, result);
		int suspended = suspendExecutingWorkers(currentWorkers, result);
		MovedSuspended movedSuspended = moveWorkers(currentWorkers, shouldBeWorkers, result);
		int created = createWorkers(coordinatorTask, shouldBeWorkers, perNodeConfigurationMap, result);
		result.recordStatus(OperationResultStatus.SUCCESS, "Worker reconciliation finished. " +
				"Original workers: " + startingWorkersCount + ", should be: " + startingShouldBeWorkersCount + ", matched: " + matched +
				", renamed: " + renamed + ", suspended because executing: " + suspended + ", moved: " + movedSuspended.moved +
				", suspended because runnable: " + movedSuspended.suspended + ", created: " + created + " worker task(s)");
	}

	/**
	 * The easiest step: we just match current workers with the 'should be' state. Matching items are deleted from both sides.
	 */
	private int matchWorkers(List<Task> currentWorkers, MultiValuedMap<String, WorkerKey> shouldBeWorkers) {
		int count = 0;
		for (Task currentWorker : new ArrayList<>(currentWorkers)) {
			WorkerKey currentWorkerKey = new WorkerKey(currentWorker);
			if (shouldBeWorkers.containsValue(currentWorkerKey)) {
				shouldBeWorkers.removeMapping(currentWorkerKey.group, currentWorkerKey);
				currentWorkers.remove(currentWorker);
				count++;
			}
		}
		LOGGER.trace("After matchWorkers (result: {}):\nCurrent workers: {}\nShould be workers: {}", count, currentWorkers, shouldBeWorkers);
		return count;
	}

	/**
	 * Going through the groups and renaming wrongly-named tasks to the correct names.
	 */
	private int renameWorkers(List<Task> currentWorkers, MultiValuedMap<String, WorkerKey> shouldBeWorkers, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		int count = 0;
		for (String shouldBeGroup : shouldBeWorkers.keySet()) {
			Collection<WorkerKey> shouldBeWorkersInGroup = shouldBeWorkers.get(shouldBeGroup);
			for (Task currentWorker : new ArrayList<>(currentWorkers)) {
				if (Objects.equals(shouldBeGroup, currentWorker.getGroup())) {
					if (!shouldBeWorkersInGroup.isEmpty()) {
						WorkerKey nextWorker = shouldBeWorkersInGroup.iterator().next();
						renameWorker(currentWorker, nextWorker.name, result);
						currentWorkers.remove(currentWorker);
						shouldBeWorkersInGroup.remove(nextWorker);
						count++;
					} else {
						break;      // no more workers for this group
					}
				}
			}
		}
		LOGGER.trace("After renameWorkers (result: {}):\nCurrent workers: {}\nShould be workers: {}", count, currentWorkers, shouldBeWorkers);
		return count;
	}

	private void renameWorker(Task currentWorker, String newName, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_NAME).replace(PolyString.fromOrig(newName))
				.asItemDeltas();
		LOGGER.info("Renaming worker task {} to {}", currentWorker, newName);
		repositoryService.modifyObject(TaskType.class, currentWorker.getOid(), itemDeltas, result);
	}

	private int suspendExecutingWorkers(List<Task> currentWorkers, OperationResult result) {
		int count = 0;
		for (Task worker : new ArrayList<>(currentWorkers)) {
			if (worker.getExecutionStatus() == TaskExecutionStatus.RUNNABLE && worker.getNodeAsObserved() != null) {
				LOGGER.info("Suspending misplaced worker task {}", worker);
				taskManager.suspendTask(worker, TaskManager.DO_NOT_WAIT, result);
				currentWorkers.remove(worker);
				count++;
			}
		}
		LOGGER.trace("After suspendExecutingWorkers (result: {}):\nCurrent workers: {}", count, currentWorkers);
		return count;
	}

	class MovedSuspended {
		int moved, suspended;
		MovedSuspended(int moved, int suspended) {
			this.moved = moved;
			this.suspended = suspended;
		}
	}

	/**
	 * Moving workers to correct groups (and renaming them if needed).
	 * We assume none of the workers are currently being executed.
	 */
	private MovedSuspended moveWorkers(List<Task> currentWorkers, MultiValuedMap<String, WorkerKey> shouldBeWorkers, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		int moved = 0, suspended = 0;
		Iterator<WorkerKey> shouldBeIterator = shouldBeWorkers.values().iterator();
		for (Task worker : new ArrayList<>(currentWorkers)) {
			if (shouldBeIterator.hasNext()) {
				WorkerKey shouldBeNext = shouldBeIterator.next();
				moveWorker(worker, shouldBeNext, result);
				currentWorkers.remove(worker);
				shouldBeIterator.remove();
				moved++;
			} else {
				if (worker.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
					LOGGER.info("Suspending superfluous worker task {}", worker);
					taskManager.suspendTask(worker, TaskManager.DO_NOT_WAIT, result);
					suspended++;
				}
			}
		}
		LOGGER.trace("After moveWorkers (result: {} moved, {} suspended):\nCurrent workers: {}\nShould be workers: {}", moved,
				suspended, currentWorkers, shouldBeWorkers);
		return new MovedSuspended(moved, suspended);
	}

	/**
	 * Finally, create remaining workers.
	 */
	private int createWorkers(Task coordinatorTask, MultiValuedMap<String, WorkerKey> keysToCreate,
			Map<WorkerKey, WorkerTasksPerNodeConfigurationType> perNodeConfigurationMap,
			OperationResult result)
			throws SchemaException, ObjectAlreadyExistsException {

		TaskExecutionStatusType workerExecutionStatus;
		if (coordinatorTask.getExecutionStatus() == null) {
			throw new IllegalStateException("Null executionStatus of " + coordinatorTask);
		}
		switch (coordinatorTask.getExecutionStatus()) {
			case WAITING: workerExecutionStatus = TaskExecutionStatusType.RUNNABLE; break;
			case SUSPENDED: workerExecutionStatus = TaskExecutionStatusType.SUSPENDED; break;
			case CLOSED: workerExecutionStatus = TaskExecutionStatusType.CLOSED; break;             // not very useful
			case RUNNABLE: workerExecutionStatus = TaskExecutionStatusType.SUSPENDED; break;
			default: throw new IllegalStateException("Unsupported executionStatus of " + coordinatorTask + ": " + coordinatorTask.getExecutionStatus());
		}

		int count = 0;
		TaskType coordinatorTaskBean = coordinatorTask.getTaskType();
		TaskWorkManagementType wsCfg = coordinatorTask.getWorkManagement();
		WorkersManagementType workersCfg = wsCfg.getWorkers();

		for (WorkerKey keyToCreate : keysToCreate.values()) {
			TaskType worker = new TaskType(prismContext);
			worker.setName(PolyStringType.fromOrig(keyToCreate.name));
			if (keyToCreate.group != null) {
				worker.beginExecutionConstraints().group(keyToCreate.group).end();
			}
			worker.setHandlerUri(workersCfg.getHandlerUri());

			applyDeltas(worker, workersCfg.getOtherDeltas());
			applyDeltas(worker, perNodeConfigurationMap.get(keyToCreate).getOtherDeltas());

			worker.setExecutionStatus(workerExecutionStatus);
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
			LOGGER.info("Creating worker task on {}: {}", keyToCreate.group, keyToCreate.name);
			taskManager.addTask(worker.asPrismObject(), result);
			count++;
		}
		return count;
	}

	private void applyDeltas(TaskType worker, List<ItemDeltaType> deltas) throws SchemaException {
		Collection<? extends ItemDelta> itemDeltas = DeltaConvertor.toModifications(deltas, worker.asPrismObject().getDefinition());
		ItemDelta.applyTo(itemDeltas, worker.asPrismContainerValue());
	}

	private void moveWorker(Task worker, WorkerKey shouldBe, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(TaskType.F_EXECUTION_CONSTRAINTS, TaskExecutionConstraintsType.F_GROUP).replace(shouldBe.group)
				.item(TaskType.F_NAME).replace(PolyString.fromOrig(shouldBe.name))
				.asItemDeltas();
		LOGGER.info("Moving worker task {} to {} as {}", worker, shouldBe.group, shouldBe.name);
		taskManager.modifyTask(worker.getOid(), itemDeltas, result);
	}

	class WorkerKey {
		final String group;
		final String name;

		WorkerKey(String group, String name) {
			this.group = group;
			this.name = name;
		}

		WorkerKey(Task worker) {    // objects created by this constructor should be used only for matching and comparisons
			this(worker.getExecutionGroup(), PolyString.getOrig(worker.getName()));
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof WorkerKey))
				return false;
			WorkerKey workerKey = (WorkerKey) o;
			return Objects.equals(group, workerKey.group) &&
					Objects.equals(name, workerKey.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(group, name);
		}

		@Override
		public String toString() {
			return "[" + group + ", " + name + "]";
		}
	}

	private MultiValuedMap<String, WorkerKey> createWorkerKeys(Task task,
			Map<WorkerKey, WorkerTasksPerNodeConfigurationType> perNodeConfigurationMap,
			OperationResult opResult)
			throws SchemaException {
		TaskWorkManagementType wsCfg = task.getWorkManagement();
		WorkersManagementType workersCfg = wsCfg.getWorkers();
		if (workersCfg == null) {
			throw new IllegalStateException("Workers configuration is missing: " + task);
		}
		MultiValuedMap<String, WorkerKey> rv = new ArrayListValuedHashMap<>();
		for (WorkerTasksPerNodeConfigurationType perNodeConfig : getWorkersPerNode(workersCfg)) {
			for (String nodeIdentifier : getNodeIdentifiers(perNodeConfig, opResult)) {
				int count = defaultIfNull(perNodeConfig.getCount(), 1);
				for (int index = 1; index <= count; index++) {
					WorkerKey key = createWorkerKey(nodeIdentifier, index, perNodeConfig, workersCfg, task);
					rv.put(key.group, key);
					perNodeConfigurationMap.put(key, perNodeConfig);
				}
			}
		}
		return rv;
	}

	private WorkerKey createWorkerKey(String nodeIdentifier, int index, WorkerTasksPerNodeConfigurationType perNodeConfig,
			WorkersManagementType workersCfg, Task coordinatorTask) {
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

		String name = TemplateUtil.replace(nameTemplate, replacements);

		String executionGroupTemplate = defaultIfNull(perNodeConfig.getExecutionGroup(), "{node}");
		String executionGroup = MiscUtil.nullIfEmpty(TemplateUtil.replace(executionGroupTemplate, replacements));

		return new WorkerKey(executionGroup, name);
	}

	private List<WorkerTasksPerNodeConfigurationType> getWorkersPerNode(WorkersManagementType workersCfg) {
		if (!workersCfg.getWorkersPerNode().isEmpty()) {
			return workersCfg.getWorkersPerNode();
		} else {
			return singletonList(new WorkerTasksPerNodeConfigurationType());
		}
	}

	private Collection<String> getNodeIdentifiers(WorkerTasksPerNodeConfigurationType perNodeConfig, OperationResult opResult)
			throws SchemaException {
		if (!perNodeConfig.getNodeIdentifier().isEmpty()) {
			return perNodeConfig.getNodeIdentifier();
		} else {
			SearchResultList<PrismObject<NodeType>> nodes = taskManager.searchObjects(NodeType.class, null, null, opResult);
			return nodes.stream()
					.filter(n -> n.asObjectable().getExecutionStatus() == NodeExecutionStatusType.RUNNING)
					.map(n -> n.asObjectable().getNodeIdentifier())
					.collect(Collectors.toSet());
		}
	}
}
