/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.handlers;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskPartitionsDefinition.TaskPartitionDefinition;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.util.template.StringSubstitutorUtil;
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

    private static final Trace LOGGER = TraceManager.getTrace(PartitioningTaskHandler.class);

    private static final String DEFAULT_HANDLER_URI = "{masterTaskHandlerUri}#{index}";

    private TaskManagerQuartzImpl taskManager;
    private Function<Task, TaskPartitionsDefinition> partitionsDefinitionSupplier;

    public PartitioningTaskHandler(TaskManagerQuartzImpl taskManager, Function<Task, TaskPartitionsDefinition> partitionsDefinitionSupplier) {
        this.taskManager = taskManager;
        this.partitionsDefinitionSupplier = partitionsDefinitionSupplier;
    }

    @Override
    public TaskRunResult run(RunningTask masterTaskUntyped, TaskPartitionDefinitionType partition) {

        RunningTaskQuartzImpl masterTask = (RunningTaskQuartzImpl) masterTaskUntyped;

        OperationResult opResult = new OperationResult(PartitioningTaskHandler.class.getName()+".run");
        TaskRunResult runResult = new TaskRunResult();
        runResult.setProgress(masterTask.getProgress());
        runResult.setOperationResult(opResult);

        try {
            setOrCheckTaskKind(masterTask, opResult);

            List<TaskQuartzImpl> subtasks = checkSubtasksClosed(masterTask, opResult, runResult);

            boolean subtasksPresent;
            TaskPartitionsDefinition partitionsDefinition = partitionsDefinitionSupplier.apply(masterTask);
            boolean durablePartitions = partitionsDefinition.isDurablePartitions(masterTask);
            if (durablePartitions) {
                subtasksPresent = !subtasks.isEmpty();
                if (subtasksPresent) {
                    checkSubtasksCorrect(subtasks, partitionsDefinition, masterTask, opResult, runResult);
                }
            } else {
                // subtasks cleanup
                taskManager.suspendAndDeleteTasks(TaskUtil.tasksToOids(subtasks), TaskManager.DO_NOT_WAIT, true, opResult);
                subtasksPresent = false;
            }

            if (!subtasksPresent) {     // either not durable, or durable but no subtasks (yet)
                createAndStartSubtasks(partitionsDefinition, masterTask, opResult);
            } else {
                scheduleSubtasksNow(subtasks, masterTask, opResult);
            }
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't (re)create/restart partitioned subtasks for {}", e, masterTask);
            opResult.recordFatalError("Couldn't (re)create/restart partitioned subtasks", e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        } catch (ExitHandlerException e) {
            return e.getRunResult();
        }
        runResult.setProgress(runResult.getProgress() + 1);
        opResult.computeStatusIfUnknown();
        runResult.setRunResultStatus(TaskRunResultStatus.IS_WAITING);
        return runResult;
    }

    private void setOrCheckTaskKind(Task masterTask, OperationResult opResult)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        TaskKindType taskKind = masterTask.getWorkManagement() != null ? masterTask.getWorkManagement().getTaskKind() : null;
        if (taskKind == null) {
            ItemDelta<?, ?> itemDelta = getPrismContext().deltaFor(TaskType.class)
                    .item(TaskType.F_WORK_MANAGEMENT, TaskWorkManagementType.F_TASK_KIND)
                    .replace(TaskKindType.PARTITIONED_MASTER)
                    .asItemDelta();
            masterTask.modify(itemDelta);
            masterTask.flushPendingModifications(opResult);
        } else if (taskKind != TaskKindType.PARTITIONED_MASTER) {
            throw new IllegalStateException("Partitioned task has incompatible task kind: " + masterTask.getWorkManagement() + " in " + masterTask);
        }
    }

    /**
     * Just a basic check of the subtasks - in the future, we could check them completely.
     */
    private void checkSubtasksCorrect(List<? extends Task> subtasks, TaskPartitionsDefinition partitionsDefinition, Task masterTask,
            OperationResult opResult, TaskRunResult runResult) throws ExitHandlerException {
        int expectedCount = partitionsDefinition.getCount(masterTask);
        if (subtasks.size() != expectedCount) {
            String message = "Couldn't restart subtasks tasks because their number (" + subtasks.size() +
                    ") does not match expected count (" + expectedCount + "): " + masterTask;
            LOGGER.warn("{}", message);
            opResult.recordFatalError(message);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            throw new ExitHandlerException(runResult);
        }
    }

    private void createAndStartSubtasks(TaskPartitionsDefinition partitionsDefinition, TaskQuartzImpl masterTask,
            OperationResult opResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        List<Task> subtasksCreated;
        try {
            subtasksCreated = createSubtasks(partitionsDefinition, masterTask, opResult);
        } catch (Throwable t) {
            List<TaskQuartzImpl> subtasksToRollback = masterTask.listSubtasks(opResult);
            taskManager.suspendAndDeleteTasks(TaskUtil.tasksToOids(subtasksToRollback), TaskManager.DO_NOT_WAIT, true,
                    opResult);
            throw t;
        }
        masterTask.makeWaiting(TaskWaitingReasonType.OTHER_TASKS, TaskUnpauseActionType.RESCHEDULE);  // i.e. close for single-run tasks
        masterTask.flushPendingModifications(opResult);
        List<Task> subtasksToResume = subtasksCreated.stream()
                .filter(Task::isSuspended)
                .collect(Collectors.toList());
        taskManager.resumeTasks(TaskUtil.tasksToOids(subtasksToResume), opResult);
        LOGGER.info("Partitioned subtasks were successfully created and started for master {}", masterTask);
    }

    private List<TaskQuartzImpl> checkSubtasksClosed(TaskQuartzImpl masterTask, OperationResult opResult, TaskRunResult runResult)
            throws SchemaException, ExitHandlerException {
        List<TaskQuartzImpl> subtasks = masterTask.listSubtasks(opResult);
        List<TaskQuartzImpl> subtasksNotClosed = subtasks.stream()
                .filter(w -> !w.isClosed())
                .collect(Collectors.toList());
        if (!subtasksNotClosed.isEmpty()) {
            LOGGER.warn("Couldn't (re)create/restart subtasks tasks because the following ones are not closed yet: {}", subtasksNotClosed);
            opResult.recordFatalError("Couldn't (re)create/restart subtasks because the following ones are not closed yet: " + subtasksNotClosed);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            throw new ExitHandlerException(runResult);
        }
        return subtasks;
    }

    private void scheduleSubtasksNow(List<TaskQuartzImpl> subtasks, TaskQuartzImpl masterTask, OperationResult opResult) throws SchemaException,
            ObjectAlreadyExistsException, ObjectNotFoundException {
        masterTask.makeWaiting(TaskWaitingReasonType.OTHER_TASKS, TaskUnpauseActionType.RESCHEDULE);  // i.e. close for single-run tasks
        masterTask.flushPendingModifications(opResult);

        Set<String> dependents = getDependentTasksIdentifiers(subtasks);
        // first set dependents to waiting, and only after that start runnables
        for (TaskQuartzImpl subtask : subtasks) {
            if (dependents.contains(subtask.getTaskIdentifier())) {
                subtask.makeWaiting(TaskWaitingReasonType.OTHER_TASKS, TaskUnpauseActionType.EXECUTE_IMMEDIATELY);
                subtask.flushPendingModifications(opResult);
            }
        }
        for (Task subtask : subtasks) {
            if (!dependents.contains(subtask.getTaskIdentifier())) {
                taskManager.scheduleTaskNow(subtask, opResult);
            }
        }
    }

    private Set<String> getDependentTasksIdentifiers(List<? extends Task> subtasks) {
        Set<String> rv = new HashSet<>();
        for (Task subtask : subtasks) {
            rv.addAll(subtask.getDependents());
        }
        return rv;
    }

    /**
     * Creates subtasks: either suspended (these will be resumed after everything is prepared) or waiting (if they
     * have dependencies).
     */
    private List<Task> createSubtasks(TaskPartitionsDefinition partitionsDefinition,
            Task masterTask, OperationResult opResult)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        boolean sequential = partitionsDefinition.isSequentialExecution(masterTask);
        List<String> subtaskOids = new ArrayList<>();
        int count = partitionsDefinition.getCount(masterTask);
        for (int i = 1; i <= count; i++) {
            subtaskOids.add(createSubtask(i, partitionsDefinition, sequential, masterTask, opResult));
        }
        List<Task> subtasks = new ArrayList<>(subtaskOids.size());
        for (String subtaskOid : subtaskOids) {
            subtasks.add(taskManager.getTaskPlain(subtaskOid, opResult));
        }
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
                if (dependent.isSuspended()) {
                    ((TaskQuartzImpl) dependent).makeWaiting(TaskWaitingReasonType.OTHER_TASKS, TaskUnpauseActionType.EXECUTE_IMMEDIATELY);
                    dependent.flushPendingModifications(opResult);
                }
            }
            subtask.flushPendingModifications(opResult);
        }
        return subtasks;
    }

    private String createSubtask(int index, TaskPartitionsDefinition partitionsDefinition,
            boolean sequential, Task masterTask, OperationResult opResult) throws SchemaException, ObjectAlreadyExistsException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("index", String.valueOf(index));
        replacements.put("masterTaskName", String.valueOf(masterTask.getName().getOrig()));
        replacements.put("masterTaskHandlerUri", masterTask.getHandlerUri());

        TaskPartitionDefinition partition = partitionsDefinition.getPartition(masterTask, index);

        TaskType subtask = new TaskType(getPrismContext());

        String nameTemplate = applyDefaults(
                p -> p.getName(masterTask),
                ps -> ps.getName(masterTask),
                "{masterTaskName} ({index})", partition, partitionsDefinition);
        String name = StringSubstitutorUtil.simpleExpand(nameTemplate, replacements);
        subtask.setName(PolyStringType.fromOrig(name));

        TaskWorkManagementType workManagement = applyDefaults(
                p -> p.getWorkManagement(masterTask),
                ps -> ps.getWorkManagement(masterTask),
                null, partition, partitionsDefinition);
        // work management is updated and stored into subtask later

        TaskExecutionEnvironmentType executionEnvironment = applyDefaults(
                p -> p.getExecutionEnvironment(masterTask),
                ps -> ps.getExecutionEnvironment(masterTask),
                masterTask.getExecutionEnvironment(), partition, partitionsDefinition);
        subtask.setExecutionEnvironment(CloneUtil.clone(executionEnvironment));

        String handlerUriTemplate = applyDefaults(
                p -> p.getHandlerUri(masterTask),
                ps -> ps.getHandlerUri(masterTask),
                null,
                partition, partitionsDefinition);
        String handlerUri = StringSubstitutorUtil.simpleExpand(handlerUriTemplate, replacements);
        if (handlerUri == null) {
            // The default for coordinator-based partitions is to put default handler into workers configuration
            // - but only if both partition and workers handler URIs are null. This is to be revisited some day.
            if (isCoordinator(workManagement)) {
                handlerUri = TaskConstants.WORKERS_CREATION_TASK_HANDLER_URI;
                if (workManagement.getWorkers() != null && workManagement.getWorkers().getHandlerUri() == null) {
                    workManagement = workManagement.clone();
                    workManagement.getWorkers().setHandlerUri(
                            StringSubstitutorUtil.simpleExpand(DEFAULT_HANDLER_URI, replacements));
                }
            } else {
                handlerUri = StringSubstitutorUtil.simpleExpand(DEFAULT_HANDLER_URI, replacements);
            }
        }
        subtask.setHandlerUri(handlerUri);
        subtask.setWorkManagement(workManagement);

        subtask.setExecutionStatus(TaskExecutionStateType.SUSPENDED);
        subtask.setSchedulingState(TaskSchedulingStateType.SUSPENDED);
        subtask.setOwnerRef(CloneUtil.clone(masterTask.getOwnerRef()));
        subtask.setCategory(masterTask.getCategory());
        subtask.setObjectRef(CloneUtil.clone(masterTask.getObjectRefOrClone()));
        subtask.setRecurrence(TaskRecurrenceType.SINGLE);
        subtask.setParent(masterTask.getTaskIdentifier());
        boolean copyMasterExtension = applyDefaults(
                p -> p.isCopyMasterExtension(masterTask),
                ps -> ps.isCopyMasterExtension(masterTask),
                false, partition, partitionsDefinition);
        if (copyMasterExtension) {
            PrismContainer<?> masterExtension = masterTask.getExtensionClone();
            if (masterExtension != null) {
                subtask.asPrismObject().add(masterExtension);
            }
        }
        ExtensionType extensionFromPartition = partition.getExtension(masterTask);
        if (extensionFromPartition != null) {
            //noinspection unchecked
            subtask.asPrismContainerValue().findOrCreateContainer(TaskType.F_EXTENSION).getValue()
                    .mergeContent(extensionFromPartition.asPrismContainerValue(), Collections.emptyList());
        }

        applyDeltas(subtask, partition.getOtherDeltas(masterTask));
        applyDeltas(subtask, partitionsDefinition.getOtherDeltas(masterTask));

        if (sequential) {
            if (subtask.getWorkManagement() == null) {
                subtask.setWorkManagement(new TaskWorkManagementType(getPrismContext()));
            }
            subtask.getWorkManagement().setPartitionSequentialNumber(index);
        }
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
        return taskManager.getPrismContext();
    }

    private void applyDeltas(TaskType subtask, Collection<ItemDelta<?, ?>> deltas) throws SchemaException {
        ItemDeltaCollectionsUtil.applyTo(deltas, subtask.asPrismContainerValue());
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value(); // todo
    }
}
