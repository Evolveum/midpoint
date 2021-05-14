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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages workers for so-called multi-node tasks.
 */
@Component
public class WorkersCreationTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(WorkersCreationTaskHandler.class);
    public static final String HANDLER_URI = TaskConstants.WORKERS_CREATION_TASK_HANDLER_URI;

    private static final String OP_RUN = WorkersCreationTaskHandler.class.getName() + ".run";

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired private RepositoryService repositoryService;

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public TaskRunResult run(@NotNull RunningTask task) throws StopHandlerExecutionException {
        OperationResult opResult = new OperationResult(OP_RUN);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);
        runResult.setProgress(null); // We intentionally do not set the progress to avoid counting it in the progress of task tree

        try {
            setOrCheckTaskKind(task, opResult);

            deleteWorkStateAndWorkers(task, opResult, runResult);
            createWorkers(task, opResult);
            makeTaskWaitingAndResumeWorkers(task, opResult);

            runResult.setRunResultStatus(TaskRunResultStatus.IS_WAITING);
            return runResult;

        } catch (StopHandlerExecutionException e) {
            throw e;
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't (re)create workers for {}", t, task);
            opResult.recordFatalError("Couldn't (re)create workers", t);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        } finally {
            opResult.computeStatusIfUnknown();
        }
    }

    private void setOrCheckTaskKind(Task task, OperationResult opResult)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
//        TaskKindType taskKind = task.getWorkManagement() != null ? task.getWorkManagement().getTaskKind() : null;
//        if (taskKind == null) {
//            ItemDelta<?, ?> itemDelta = prismContext.deltaFor(TaskType.class)
//                    .item(TaskType.F_WORK_MANAGEMENT, TaskWorkManagementType.F_TASK_KIND)
//                    .replace(TaskKindType.COORDINATOR)
//                    .asItemDelta();
//            task.modify(itemDelta);
//            task.flushPendingModifications(opResult);
//        } else if (taskKind != TaskKindType.COORDINATOR) {
//            throw new IllegalStateException("Task has incompatible task kind; expected " + TaskKindType.COORDINATOR +
//                    " but having: " + task.getWorkManagement() + " in " + task);
//        }
    }

    private void checkWorkersComplete(List<? extends Task> workers, RunningTask task, OperationResult opResult, TaskRunResult runResult) throws StopHandlerExecutionException {
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
                throw new StopHandlerExecutionException(runResult);
            }
        } else {
            List<? extends Task> notClosed = workers.stream()
                    .filter(w -> !w.isClosed())
                    .collect(Collectors.toList());
            if (!notClosed.isEmpty()) {
                LOGGER.warn("Couldn't (re)create worker tasks because the work is not done and the following ones are not closed yet: {}", notClosed);
                opResult.recordFatalError("Couldn't (re)create worker tasks because the work is not done and the following ones are not closed yet: " + notClosed);
                runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
                throw new StopHandlerExecutionException(runResult);
            }
        }
    }

    private void makeTaskWaitingAndResumeWorkers(RunningTask task, OperationResult opResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        // TODO what if the task was suspended in the meanwhile?
        // reschedule means close for single-run tasks
        ((TaskQuartzImpl) task).makeWaitingForOtherTasks(TaskUnpauseActionType.RESCHEDULE); // keeping exec state RUNNING
        task.flushPendingModifications(opResult);
        taskManager.resumeTasks(TaskUtil.tasksToOids(task.listSubtasks(true, opResult)), opResult);
        LOGGER.info("Worker tasks were successfully created for coordinator {}", task);
    }

    private void createWorkers(RunningTask task, OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        WorkersReconciliationOptions options = new WorkersReconciliationOptions();
        options.setDontCloseWorkersWhenWorkDone(true);
        taskManager.reconcileWorkers(task.getOid(), options, opResult);
    }

    private void deleteWorkStateAndWorkers(RunningTask task, OperationResult opResult, TaskRunResult runResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, StopHandlerExecutionException {

        List<? extends Task> workers = task.listSubtasks(true, opResult);
        checkWorkersComplete(workers, task, opResult, runResult);

        deleteWorkState(task, opResult);

        for (Task worker : workers) {
            try {
                List<? extends Task> workerSubtasks = worker.listSubtasks(true, opResult);
                if (!workerSubtasks.isEmpty()) {
                    LOGGER.warn("Couldn't recreate worker task {} because it has its own subtasks: {}", worker, workerSubtasks);
                    opResult.recordFatalError("Couldn't recreate worker task " + worker + " because it has its own subtasks: " + workerSubtasks);
                    runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
                    throw new StopHandlerExecutionException(runResult);
                }
                taskManager.deleteTask(worker.getOid(), opResult);
            } catch (ObjectNotFoundException | SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete worker task {} (coordinator {})", e, worker, task);
            }
        }
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
