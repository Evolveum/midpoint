/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import static java.util.Collections.singleton;

import static com.evolveum.midpoint.task.api.TaskManager.*;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.task.api.TaskManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.TaskListenerRegistry;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.execution.TaskStopper;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Suspends the tasks.
 */
@Component
class SuspendAndDeleteHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SuspendAndDeleteHelper.class);

    @Autowired private TaskStopper taskStopper;
    @Autowired private TaskListenerRegistry listenerRegistry;
    @Autowired private TaskRetriever taskRetriever;
    @Autowired private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private LocalScheduler localScheduler;

    @Autowired private CloseHelper closeHelper;

    public boolean suspendTask(TaskQuartzImpl task, long waitTime, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        LOGGER.info("Suspending task {}; {}.", task, waitingInfo(waitTime));
        suspendTaskNoWait(task, result);
        return waitForTaskToStop(task, waitTime, result);
    }

    public boolean suspendTaskNoExceptions(TaskQuartzImpl task, long waitTime, boolean suspendDependents, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        LOGGER.info("Suspending task {}; {}.", task, waitingInfo(waitTime));
        suspendTaskNoWaitNoExceptions(task, result);
        boolean stopped = waitForTaskToStop(task, waitTime, result);
        if (suspendDependents) {
            suspendDependentsIfPossible(task, waitTime, result);
        }
        return stopped;
    }

    private void suspendDependentsIfPossible(TaskQuartzImpl task, long waitTime, OperationResult result) throws ObjectNotFoundException, SchemaException {
        LOGGER.debug("suspendDependentsIfPossible starting for {}", task);
        int suspended = 0;

        List<Task> dependents = task.listDependents(result);
        LOGGER.debug("dependents: {}", dependents);
        for (Task dependent : dependents) {
            if (suspendTaskNoExceptions((TaskQuartzImpl) dependent, waitTime, true, result)) {
                suspended++;
            }
        }

        TaskQuartzImpl parentTask = task.getParentTask(result);
        LOGGER.debug("parent: {}", parentTask);
        if (parentTask != null) {
            if (suspendTaskNoExceptions(parentTask, waitTime, true, result)) {
                suspended++;
            }
        }

        LOGGER.debug("suspendDependentsIfPossible finished for {}; suspended {} task(s)", task, suspended);
    }

    public void suspendAndCloseTaskNoException(TaskQuartzImpl task, long waitTime, OperationResult result) {
        LOGGER.info("Suspending and closing task {}; {}.", task, waitingInfo(waitTime));
        suspendTaskNoWaitNoExceptions(task, result);
        waitForTaskToStop(task, waitTime, result);
        closeTaskNoExceptions(result, task);
    }

    public boolean suspendTasks(Collection<String> taskOids, long waitTime, OperationResult result) {
        List<TaskQuartzImpl> tasks = taskRetriever.resolveTaskOids(taskOids, result);
        LOGGER.info("Suspending tasks {}; {}.", tasks, waitingInfo(waitTime));
        return suspendTasksInternal(tasks, waitTime, result);
    }

    public boolean suspendTaskTree(String rootTaskOid, long waitTime, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        List<TaskQuartzImpl> allTasks = new ArrayList<>();
        TaskQuartzImpl root = taskRetriever.getTaskPlain(rootTaskOid, result);
        LOGGER.info("Suspending task tree for {}; {}.", root, waitingInfo(waitTime));
        allTasks.add(root);
        allTasks.addAll(root.listSubtasksDeeply(true, result));
        return suspendTasksInternal(allTasks, waitTime, result);
    }

    public void suspendAndDeleteTasks(Collection<String> taskOids, long waitTime, boolean alsoSubtasks, OperationResult result) {
        List<TaskQuartzImpl> tasksToBeDeleted = getTasksToBeDeleted(taskOids, alsoSubtasks, result);
        suspendReadyTasks(tasksToBeDeleted, waitTime, result);
        deleteTasks(tasksToBeDeleted, result);
    }

    public void suspendAndDeleteTask(String taskOid, long suspendTimeout, boolean alsoSubtasks, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        List<TaskQuartzImpl> tasksToBeDeleted = getTasksToBeDeleted(taskOid, alsoSubtasks, result);
        suspendReadyTasks(tasksToBeDeleted, suspendTimeout, result);
        deleteTasks(tasksToBeDeleted, result);
    }

    private void suspendReadyTasks(List<TaskQuartzImpl> tasks, long waitTime, OperationResult result) {
        List<TaskQuartzImpl> tasksToBeSuspended = new ArrayList<>();
        for (TaskQuartzImpl task : tasks) {
            if (task.getSchedulingState() == TaskSchedulingStateType.READY) {
                tasksToBeSuspended.add(task);
            }
        }

        // now suspend the tasks before deletion
        if (!tasksToBeSuspended.isEmpty()) {
            suspendTasksInternal(tasksToBeSuspended, waitTime, result);
        }
    }

    @NotNull
    private List<TaskQuartzImpl> getTasksToBeDeleted(Collection<String> taskOids, boolean alsoSubtasks, OperationResult result) {
        List<TaskQuartzImpl> tasksToBeDeleted = new ArrayList<>();
        for (String oid : taskOids) {
            try {
                tasksToBeDeleted.addAll(getTasksToBeDeleted(oid, alsoSubtasks, result));
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Error when retrieving task {} or its subtasks before the deletion. "
                        + "Skipping the deletion for this task.", e, oid);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Error when retrieving task {} or its subtasks before the deletion. "
                        + "Skipping the deletion for this task.", e, oid);
            }
        }
        return tasksToBeDeleted;
    }

    @NotNull
    private List<TaskQuartzImpl> getTasksToBeDeleted(String taskOid, boolean alsoSubtasks, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        List<TaskQuartzImpl> tasksToBeDeleted = new ArrayList<>();
        TaskQuartzImpl thisTask = taskRetriever.getTaskPlain(taskOid, result);
        tasksToBeDeleted.add(thisTask);
        if (alsoSubtasks) {
            tasksToBeDeleted.addAll(thisTask.listSubtasksDeeply(true, result));
        }
        return tasksToBeDeleted;
    }

    private boolean suspendTasksInternal(Collection<TaskQuartzImpl> tasks, long waitForStop, OperationResult result) {
        LOGGER.trace("suspendTasksInternal: tasks = {}, waitForStop = {}", tasks, waitForStop);
        for (TaskQuartzImpl task : tasks) {
            suspendTaskNoWaitNoExceptions(task, result);
        }
        return waitForTasksToStop(tasks, waitForStop, result);
    }

    private void suspendTaskNoWaitNoExceptions(TaskQuartzImpl task, OperationResult result) {
        try {
            suspendTaskNoWait(task, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't suspend task because it does not exist; task = {}", e, task);
        } catch (SchemaException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't suspend task because of an unexpected exception; task = {}",
                    e, task);
        }
    }

    private void closeTaskNoExceptions(OperationResult result, Task task) {
        try {
            closeHelper.closeTask(task, result);
        } catch (ObjectNotFoundException | SchemaException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't close task {}", e, task);
        }
    }

    private boolean waitForTaskToStop(TaskQuartzImpl task, long waitForStop, OperationResult result) {
        return waitForTasksToStop(singleton(task), waitForStop, result);
    }

    private boolean waitForTasksToStop(Collection<TaskQuartzImpl> tasks, long waitForStop, OperationResult result) {
        //noinspection SimplifiableIfStatement
        if (waitForStop != DO_NOT_STOP) {
            return taskStopper.stopTasksRunAndWait(tasks, null, waitForStop, true, result);
        } else {
            return false;
        }
    }

    private void suspendTaskNoWait(TaskQuartzImpl task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        argCheck(task.getOid() != null,
                "Only persistent tasks can be suspended/closed; task %s is transient", task);

        if (task.getSchedulingState() == TaskSchedulingStateType.READY ||
                task.getSchedulingState() == TaskSchedulingStateType.WAITING) {
            try {
                List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_EXECUTION_STATE).replace(TaskExecutionStateType.SUSPENDED)
                        .item(TaskType.F_SCHEDULING_STATE).replace(TaskSchedulingStateType.SUSPENDED)
                        .item(TaskType.F_STATE_BEFORE_SUSPEND).replace(task.getExecutionState())
                        .item(TaskType.F_SCHEDULING_STATE_BEFORE_SUSPEND).replace(task.getSchedulingState())
                        .asItemDeltas();
                task.applyDeltasImmediate(itemDeltas, result);
            } catch (ObjectAlreadyExistsException e) {
                throw new SystemException(e);
            }
        }
        localScheduler.pauseTaskJob(task, result);
        // even if this will not succeed, by setting the execution status to SUSPENDED we hope the task
        // thread will exit on next iteration (does not apply to single-run tasks, of course)
    }

    private String waitingInfo(long waitForStop) {
        if (waitForStop == WAIT_INDEFINITELY) {
            return "stop tasks, and wait for their completion (if necessary)";
        } else if (waitForStop == DO_NOT_WAIT) {
            return "stop tasks, but do not wait";
        } else if (waitForStop == DO_NOT_STOP) {
            return "do not stop tasks";
        } else {
            return "stop tasks and wait " + waitForStop + " ms for their completion (if necessary)";
        }
    }

    private void deleteTasks(List<TaskQuartzImpl> tasksToBeDeleted, OperationResult result) {
        for (Task task : tasksToBeDeleted) {
            try {
                deleteTask(task.getOid(), result);
            } catch (ObjectNotFoundException e) {   // in all cases (even RuntimeException) the error is already put into result
                LoggingUtils.logException(LOGGER, "Error when deleting task {}", e, task);
            } catch (SchemaException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Error when deleting task {}", e, task);
            }
        }
    }

    public void deleteTask(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        TaskQuartzImpl task = taskRetriever.getTaskPlain(oid, result);
        if (task.getNode() != null) {
            result.recordWarning("Deleting a task that seems to be currently executing on node " + task.getNode());
        }
        listenerRegistry.notifyTaskDeleted(task, result);
        try {
            repositoryService.deleteObject(TaskType.class, oid, result);
        } finally {
            localScheduler.deleteTaskFromQuartz(oid, false, result);
        }

        // We could consider unpausing the dependents here. However, they might be already deleted,
        // so we have to be prepared to get ObjectNotFoundException here. Or perhaps to unpause something
        // that will be deleted milliseconds later (in the case of task tree deletion). Therefore it is
        // better NOT to unpause anything -- and to avoid relying on waiting on tasks that are going to be deleted.
        // TODO Reconsider. What if there are suspended subtasks that are deleted, and their parents should
        //  be then unpaused? Maybe the forceful close of the task should be introduced.
    }

    void deleteTaskTree(String rootOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        TaskQuartzImpl root = taskRetriever.getTaskPlain(rootOid, result);
        LOGGER.debug("Deleting task tree {}", root);
        List<TaskQuartzImpl> allTasks = new ArrayList<>(root.listSubtasksDeeply(true, result));
        allTasks.add(root);
        deleteTasks(allTasks, result);
    }

    void markClosedTaskSuspended(String taskOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        try {
            repositoryService.modifyObjectDynamically(TaskType.class, taskOid, null,
                    task -> {
                        TaskExecutionStateType state = task.getExecutionState();
                        if (state == TaskExecutionStateType.CLOSED) {
                            return prismContext.deltaFor(TaskType.class)
                                    .item(TaskType.F_EXECUTION_STATE).replace(TaskExecutionStateType.SUSPENDED)
                                    .item(TaskType.F_SCHEDULING_STATE).replace(TaskSchedulingStateType.SUSPENDED)
                                    .item(TaskType.F_STATE_BEFORE_SUSPEND).replace(TaskExecutionStateType.RUNNABLE)
                                    .item(TaskType.F_SCHEDULING_STATE_BEFORE_SUSPEND).replace(TaskSchedulingStateType.READY)
                                    .asItemDeltas();
                        } else if (state == TaskExecutionStateType.SUSPENDED) {
                            return List.of();
                        } else {
                            throw new IllegalStateException("Couldn't mark closed task as suspended, "
                                    + "because the state is " + state);
                        }
                    }, null, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected ObjectAlreadyExistsException: " + e.getMessage(), e);
        }
    }
}
