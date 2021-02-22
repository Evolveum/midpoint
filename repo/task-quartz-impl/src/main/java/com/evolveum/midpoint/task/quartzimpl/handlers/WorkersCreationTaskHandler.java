/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.handlers;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
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
 */
@Component
public class WorkersCreationTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(WorkersCreationTaskHandler.class);
    public static final String HANDLER_URI = TaskConstants.WORKERS_CREATION_TASK_HANDLER_URI;

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired private RepositoryService repositoryService;

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        OperationResult opResult = new OperationResult(WorkersCreationTaskHandler.class.getName()+".run");
        TaskRunResult runResult = new TaskRunResult();
        runResult.setProgress(task.getProgress());
        runResult.setOperationResult(opResult);

        try {
            setOrCheckTaskKind(task, opResult);
            List<? extends Task> workers = task.listSubtasks(true, opResult);
            boolean clean = task.getWorkState() == null || Boolean.TRUE.equals(task.getWorkState().isAllWorkComplete());
            // todo consider checking that the subtask is really a worker (workStateConfiguration.taskKind)
            if (clean) {
                List<Task> notClosedNorSuspended = workers.stream()
                        .filter(w -> !w.isClosed() && !w.isSuspended())
                        .collect(Collectors.toList());
                if (!notClosedNorSuspended.isEmpty()) {
                    LOGGER.warn("Couldn't (re)create worker tasks because the work is done but the following ones are not closed nor suspended: {}", notClosedNorSuspended);
                    opResult.recordFatalError("Couldn't (re)create worker tasks because the work is done but the following ones are not closed nor suspended: " + notClosedNorSuspended);
                    runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
                    return runResult;
                }
            } else {
                List<? extends Task> notClosed = workers.stream()
                        .filter(w -> !w.isClosed())
                        .collect(Collectors.toList());
                if (!notClosed.isEmpty()) {
                    LOGGER.warn("Couldn't (re)create worker tasks because the work is not done and the following ones are not closed yet: {}", notClosed);
                    opResult.recordFatalError("Couldn't (re)create worker tasks because the work is not done and the following ones are not closed yet: " + notClosed);
                    runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
                    return runResult;
                }
            }
            if (deleteWorkersAndWorkState(workers, task, opResult, runResult)) {
                return runResult;
            }
            WorkersReconciliationOptions options = new WorkersReconciliationOptions();
            options.setDontCloseWorkersWhenWorkDone(true);
            taskManager.reconcileWorkers(task.getOid(), options, opResult);

            // TODO what if the task was suspended in the meanwhile?
            ((TaskQuartzImpl) task).makeWaiting(TaskWaitingReasonType.OTHER_TASKS, TaskUnpauseActionType.RESCHEDULE);  // i.e. close for single-run tasks
            task.flushPendingModifications(opResult);
            taskManager.resumeTasks(TaskUtil.tasksToOids(task.listSubtasks(true, opResult)), opResult);
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
            ItemDelta<?, ?> itemDelta = prismContext.deltaFor(TaskType.class)
                    .item(TaskType.F_WORK_MANAGEMENT, TaskWorkManagementType.F_TASK_KIND)
                    .replace(TaskKindType.COORDINATOR)
                    .asItemDelta();
            task.modify(itemDelta);
            task.flushPendingModifications(opResult);
        } else if (taskKind != TaskKindType.COORDINATOR) {
            throw new IllegalStateException("Task has incompatible task kind; expected " + TaskKindType.COORDINATOR +
                    " but having: " + task.getWorkManagement() + " in " + task);
        }
    }

    // returns true in case of problem
    private boolean deleteWorkersAndWorkState(List<? extends Task> workers, Task task, OperationResult opResult, TaskRunResult runResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        deleteWorkState(task, opResult);
        for (Task worker : workers) {
            try {
                List<? extends Task> workerSubtasks = worker.listSubtasks(true, opResult);
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

    private void deleteWorkState(Task task, OperationResult opResult) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_WORK_STATE).replace()
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, task.getOid(), itemDeltas, opResult);
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
