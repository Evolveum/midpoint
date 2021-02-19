/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.work.workers;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.template.StringSubstitutorUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Manages worker tasks.
 */
@Component
public class WorkersManager {

    private static final Trace LOGGER = TraceManager.getTrace(WorkersManager.class);

    @Autowired private PrismContext prismContext;
    @Autowired private TaskManager taskManager;
    @Autowired private RepositoryService repositoryService;

    public void reconcileWorkers(String coordinatorTaskOid, WorkersReconciliationOptions options, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        Task coordinatorTask = taskManager.getTaskPlain(coordinatorTaskOid, result);
        if (coordinatorTask.getKind() != TaskKindType.COORDINATOR) {
            throw new IllegalArgumentException("Task is not a coordinator task: " + coordinatorTask);
        }
        List<Task> currentWorkers = new ArrayList<>(coordinatorTask.listSubtasks(true, result));
        Map<WorkerKey, WorkerTasksPerNodeConfigurationType> perNodeConfigurationMap = new HashMap<>();
        MultiValuedMap<String, WorkerKey> shouldBeWorkers = createWorkerKeys(coordinatorTask, perNodeConfigurationMap, result);

        int startingWorkersCount = currentWorkers.size();
        int startingShouldBeWorkersCount = shouldBeWorkers.size();

        currentWorkers.sort(Comparator.comparing(t -> {
            if (t.getExecutionState() == TaskExecutionStateType.RUNNABLE) {
                if (t.getNodeAsObserved() != null) {
                    return 0;
                } else if (t.getNode() != null) {
                    return 1;
                } else {
                    return 2;
                }
            } else if (t.getExecutionState() == TaskExecutionStateType.SUSPENDED) {
                return 3;
            } else if (t.getExecutionState() == TaskExecutionStateType.CLOSED) {
                return 4;
            } else {
                return 5;
            }
        }));

        LOGGER.trace("Before reconciliation:\nCurrent workers: {}\nShould be workers: {}", currentWorkers, shouldBeWorkers);

        int matched = matchWorkers(currentWorkers, shouldBeWorkers);
        int renamed = renameWorkers(currentWorkers, shouldBeWorkers, result);
        int closedExecuting = closeExecutingWorkers(currentWorkers, result);
        MovedClosed movedClosed = moveWorkers(currentWorkers, shouldBeWorkers, result);
        int created = createWorkers(coordinatorTask, shouldBeWorkers, perNodeConfigurationMap, result);

        // TODO ensure that enough scavengers are present

        TaskWorkStateType workState = coordinatorTask.getWorkState();
        Integer closedBecauseDone = null;
        if (isCloseWorkersOnWorkDone(options) && workState != null && Boolean.TRUE.equals(workState.isAllWorkComplete())) {
            closedBecauseDone = closeAllWorkers(coordinatorTask, result);
        }
        result.recordStatus(OperationResultStatus.SUCCESS, "Worker reconciliation finished. " +
                "Original workers: " + startingWorkersCount + ", should be: " + startingShouldBeWorkersCount + ", matched: " + matched +
                ", renamed: " + renamed + ", closed because executing: " + closedExecuting + ", moved: " + movedClosed.moved +
                ", closed because superfluous: " + movedClosed.closed + ", created: " + created + " worker task(s)." +
                (closedBecauseDone != null && closedBecauseDone > 0 ? " Closed " + closedBecauseDone + " workers because the work is done." : ""));
    }

    private boolean isCloseWorkersOnWorkDone(WorkersReconciliationOptions options) {
        return options == null || !options.isDontCloseWorkersWhenWorkDone();
    }

    private Integer closeAllWorkers(Task coordinatorTask, OperationResult result) throws SchemaException {
        int count = 0;
        List<Task> workers = new ArrayList<>(coordinatorTask.listSubtasks(true, result));
        for (Task worker : workers) {
            if (worker.getExecutionState() != TaskExecutionStateType.CLOSED) {
                LOGGER.info("Closing worker because the work is done: {}", worker);
                taskManager.suspendAndCloseTaskQuietly(worker, TaskManager.DO_NOT_WAIT, result);
                count++;
            }
        }
        return count;
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
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_NAME).replace(PolyString.fromOrig(newName))
                .asItemDeltas();
        LOGGER.info("Renaming worker task {} to {}", currentWorker, newName);
        repositoryService.modifyObject(TaskType.class, currentWorker.getOid(), itemDeltas, result);
    }

    private int closeExecutingWorkers(List<Task> currentWorkers, OperationResult result) {
        int count = 0;
        for (Task worker : new ArrayList<>(currentWorkers)) {
            if (worker.getExecutionState() == TaskExecutionStateType.RUNNABLE && worker.getNodeAsObserved() != null) {
                LOGGER.info("Suspending misplaced worker task {}", worker);
                taskManager.suspendAndCloseTaskQuietly(worker, TaskManager.DO_NOT_WAIT, result);
                currentWorkers.remove(worker);
                count++;
            }
        }
        LOGGER.trace("After closeExecutingWorkers (result: {}):\nCurrent workers: {}", count, currentWorkers);
        return count;
    }

    static class MovedClosed {
        int moved, closed;

        MovedClosed(int moved, int closed) {
            this.moved = moved;
            this.closed = closed;
        }
    }

    /**
     * Moving workers to correct groups (and renaming them if needed).
     * We assume none of the workers are currently being executed.
     */
    private MovedClosed moveWorkers(List<Task> currentWorkers, MultiValuedMap<String, WorkerKey> shouldBeWorkers, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        int moved = 0, closed = 0;
        Iterator<WorkerKey> shouldBeIterator = shouldBeWorkers.values().iterator();
        for (Task worker : new ArrayList<>(currentWorkers)) {
            if (shouldBeIterator.hasNext()) {
                WorkerKey shouldBeNext = shouldBeIterator.next();
                moveWorker(worker, shouldBeNext, result);
                currentWorkers.remove(worker);
                shouldBeIterator.remove();
                moved++;
            } else {
                if (worker.getExecutionState() != TaskExecutionStateType.CLOSED) {
                    LOGGER.info("Closing superfluous worker task {}", worker);
                    taskManager.suspendAndCloseTaskQuietly(worker, TaskManager.DO_NOT_WAIT, result);
                    closed++;
                }
            }
        }
        LOGGER.trace("After moveWorkers (result: {} moved, {} closed):\nCurrent workers: {}\nShould be workers: {}", moved,
                closed, currentWorkers, shouldBeWorkers);
        return new MovedClosed(moved, closed);
    }

    /**
     * Finally, create remaining workers.
     */
    private int createWorkers(Task coordinatorTask, MultiValuedMap<String, WorkerKey> keysToCreate,
            Map<WorkerKey, WorkerTasksPerNodeConfigurationType> perNodeConfigurationMap,
            OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {

        TaskExecutionStateType workerExecutionStatus;
        if (coordinatorTask.getExecutionState() == null) {
            throw new IllegalStateException("Null executionStatus of " + coordinatorTask);
        }
        switch (coordinatorTask.getExecutionState()) {
            case WAITING:
                workerExecutionStatus = TaskExecutionStateType.RUNNABLE;
                break;
            case SUSPENDED:
            case RUNNABLE:
                workerExecutionStatus = TaskExecutionStateType.SUSPENDED;
                break;
            case CLOSED:
                workerExecutionStatus = TaskExecutionStateType.CLOSED;
                break;             // not very useful
            default:
                throw new IllegalStateException("Unsupported executionStatus of " + coordinatorTask + ": " + coordinatorTask.getExecutionState());
        }

        int count = 0;
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
            worker.setOwnerRef(CloneUtil.clone(coordinatorTask.getOwnerRef()));
            worker.setCategory(coordinatorTask.getCategory());
            worker.setObjectRef(CloneUtil.clone(coordinatorTask.getObjectRefOrClone()));
            worker.setRecurrence(TaskRecurrenceType.SINGLE);
            worker.setParent(coordinatorTask.getTaskIdentifier());
            worker.setExecutionEnvironment(CloneUtil.clone(coordinatorTask.getExecutionEnvironment()));
            TaskWorkManagementType workManagement = worker.beginWorkManagement().taskKind(TaskKindType.WORKER);
            if (keyToCreate.scavenger) {
                workManagement.setScavenger(true);
            }
            PrismContainer<?> coordinatorExtension = coordinatorTask.getExtensionClone();
            if (coordinatorExtension != null) {
                worker.asPrismObject().add(coordinatorExtension);
            }
            LOGGER.info("Creating worker task on {}: {}", keyToCreate.group, keyToCreate.name);
            taskManager.addTask(worker.asPrismObject(), result);
            count++;
        }
        return count;
    }

    private void applyDeltas(TaskType worker, List<ItemDeltaType> deltas) throws SchemaException {
        Collection<? extends ItemDelta<?, ?>> itemDeltas = DeltaConvertor.toModifications(deltas, worker.asPrismObject().getDefinition());
        ItemDeltaCollectionsUtil.applyTo(itemDeltas, worker.asPrismContainerValue());
    }

    private void moveWorker(Task worker, WorkerKey shouldBe, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_EXECUTION_CONSTRAINTS, TaskExecutionConstraintsType.F_GROUP).replace(shouldBe.group)
                .item(TaskType.F_NAME).replace(PolyString.fromOrig(shouldBe.name))
                .asItemDeltas();
        LOGGER.info("Moving worker task {} to {} as {}", worker, shouldBe.group, shouldBe.name);
        taskManager.modifyTask(worker.getOid(), itemDeltas, result);
    }

    static class WorkerKey {
        final String group;
        final String name;
        final boolean scavenger;

        WorkerKey(String group, String name, boolean scavenger) {
            this.group = group;
            this.name = name;
            this.scavenger = scavenger;
        }

        /** Objects created by this constructor should be used only for matching and comparisons. */
        WorkerKey(Task worker) {
            this(worker.getGroup(), PolyString.getOrig(worker.getName()), isScavenger(worker));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof WorkerKey)) { return false; }
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
            return "[" + group + ", " + name + (scavenger ? " (scavenger)" : "") + "]";
        }
    }

    private static boolean isScavenger(Task task) {
        return Boolean.TRUE.equals(task.getWorkManagement().isScavenger());
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
                int scavengers = defaultIfNull(perNodeConfig.getScavengers(), 1);
                for (int index = 1; index <= count; index++) {
                    WorkerKey key = createWorkerKey(nodeIdentifier, index, perNodeConfig, workersCfg, index <= scavengers, task);
                    rv.put(key.group, key);
                    perNodeConfigurationMap.put(key, perNodeConfig);
                }
            }
        }
        return rv;
    }

    private WorkerKey createWorkerKey(String nodeIdentifier, int index, WorkerTasksPerNodeConfigurationType perNodeConfig,
            WorkersManagementType workersCfg, boolean scavenger, Task coordinatorTask) {
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

        String name = StringSubstitutorUtil.simpleExpand(nameTemplate, replacements);

        String executionGroupTemplate = defaultIfNull(perNodeConfig.getExecutionGroup(), "{node}");
        String executionGroup = MiscUtil.nullIfEmpty(StringSubstitutorUtil.simpleExpand(executionGroupTemplate, replacements));

        return new WorkerKey(executionGroup, name, scavenger);
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
                    .filter(n -> n.asObjectable().getExecutionState() == NodeExecutionStateType.RUNNING)
                    .map(n -> n.asObjectable().getNodeIdentifier())
                    .collect(Collectors.toSet());
        }
    }

    public void deleteWorkersAndWorkState(String rootTaskOid, boolean deleteWorkers, long subtasksWaitTime,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        boolean suspended = taskManager.suspendTaskTree(rootTaskOid, subtasksWaitTime, result);
        if (!suspended) {
            // TODO less harsh handling
            throw new IllegalStateException("Not all tasks could be suspended. Please retry to operation.");
        }
        Task rootTask = taskManager.getTaskPlain(rootTaskOid, result);
        deleteWorkersAndWorkState(rootTask, deleteWorkers, result);
    }

    private void deleteWorkersAndWorkState(Task rootTask, boolean deleteWorkers, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        TaskKindType kind = rootTask.getKind();
        List<? extends Task> subtasks = rootTask.listSubtasks(true, result);
        if (deleteWorkers && kind == TaskKindType.COORDINATOR) {
            taskManager.suspendAndDeleteTasks(TaskUtil.tasksToOids(subtasks), TaskManager.DO_NOT_WAIT, true, result);
        } else {
            for (Task subtask : subtasks) {
                deleteWorkersAndWorkState(subtask, deleteWorkers, result);
            }
        }
        deleteWorkState(rootTask.getOid(), result);
    }

    private void deleteWorkState(String taskOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE).replace()
                .item(TaskType.F_PROGRESS).replace()
                .item(TaskType.F_EXPECTED_TOTAL).replace()
                .item(TaskType.F_OPERATION_STATS).replace()
                .item(TaskType.F_RESULT).replace()
                .item(TaskType.F_RESULT_STATUS).replace()
                .asItemDeltas();
        try {
            taskManager.modifyTask(taskOid, itemDeltas, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new IllegalStateException("Unexpected exception: " + e.getMessage(), e);
        }
    }
}
