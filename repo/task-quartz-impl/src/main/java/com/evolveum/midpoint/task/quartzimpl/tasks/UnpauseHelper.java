/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
class UnpauseHelper {

    private static final Trace LOGGER = TraceManager.getTrace(UnpauseHelper.class);

    @Autowired private TaskRetriever taskRetriever;
    @Autowired private CloseHelper closeHelper;
    @Autowired private ScheduleNowHelper scheduleNowHelper;

    boolean unpauseTask(TaskQuartzImpl task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException {

        // Here can a race condition occur. If the parent was WAITING but has become SUSPENDED in the meanwhile,
        // this test could pass (seeing WAITING status) but the following unpause action is mistakenly executed
        // on suspended task, overwriting SUSPENDED status!
        //
        // Therefore scheduleWaitingTaskNow and makeWaitingTaskRunnable must make sure the task is (still) waiting.
        // The closeTask method is OK even if the task has become suspended in the meanwhile.
        if (task.getSchedulingState() != TaskSchedulingStateType.WAITING) {
            String message = "Attempted to unpause a task that is not in the WAITING state (task = " + task
                    + ", scheduling state = " + task.getSchedulingState();
            LOGGER.error(message);
            result.recordFatalError(message);
            return false;
        }

        TaskUnpauseActionType action = getUnpauseAction(task);
        switch (action) {
            case EXECUTE_IMMEDIATELY:
                LOGGER.debug("Unpausing task using 'executeImmediately' action (scheduling it now): {}", task);
                scheduleNowHelper.scheduleWaitingTaskNow(task, result);
                break;
            case RESCHEDULE:
                if (task.isRecurring()) {
                    LOGGER.debug("Unpausing recurring task using 'reschedule' action (making it runnable): {}", task);
                    makeWaitingTaskRunnable(task, result);
                } else {
                    LOGGER.debug("Unpausing task using 'reschedule' action (closing it, because the task is single-run): {}", task);
                    closeHelper.closeTask(task, result);
                }
                break;
            case CLOSE:
                LOGGER.debug("Unpausing task using 'close' action: {}", task);
                closeHelper.closeTask(task, result);
                break;
            default:
                throw new IllegalStateException("Unsupported unpause action: " + action);
        }
        return true;
    }

    @NotNull
    private TaskUnpauseActionType getUnpauseAction(TaskQuartzImpl task) {
        if (task.getUnpauseAction() != null) {
            return task.getUnpauseAction();
        } else if (task.isSingle()) {
            return TaskUnpauseActionType.EXECUTE_IMMEDIATELY;
        } else {
            return TaskUnpauseActionType.RESCHEDULE;
        }
    }

    private void makeWaitingTaskRunnable(TaskQuartzImpl task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException {
        try {
            task.setExecutionAndSchedulingStateImmediate(
                    TaskExecutionStateType.RUNNABLE, TaskSchedulingStateType.READY,
                    TaskSchedulingStateType.WAITING, result);
            task.synchronizeWithQuartz(result);
        } catch (ObjectNotFoundException e) {
            String message = "A task cannot be made runnable, because it does not exist; task = " + task;
            LoggingUtils.logException(LOGGER, message, e);
            throw e;
        } catch (SchemaException | PreconditionViolationException e) {
            String message = "A task cannot be made runnable; task = " + task;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            throw e;
        }
    }

    /**
     * Caller must ensure that the task is closed or deleted!
     */
    void unpauseDependentsIfPossible(TaskQuartzImpl task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        LOGGER.debug("unpauseDependentsIfPossible starting for {}", task);
        int unpaused = 0;

        List<Task> dependents = task.listDependents(result);
        LOGGER.debug("dependents: {}", dependents);
        for (Task dependent : dependents) {
            if (unpauseTaskIfPossible((TaskQuartzImpl) dependent, result)) {
                unpaused++;
            }
        }

        TaskQuartzImpl parentTask = task.getParentTask(result);
        LOGGER.debug("parent: {}", parentTask);
        if (parentTask != null) {
            if (unpauseTaskIfPossible(parentTask, result)) {
                unpaused++;
            }
        }

        LOGGER.debug("unpauseDependentsIfPossible finished for {}; unpaused {} task(s)", task, unpaused);
    }

    /** @return true if unpaused */
    boolean unpauseTaskIfPossible(TaskQuartzImpl task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (task.getSchedulingState() != TaskSchedulingStateType.WAITING ||
                task.getWaitingReason() != TaskWaitingReasonType.OTHER_TASKS) {
            LOGGER.debug("Not considering task for unpausing {} because the state does not match: {}/{}",
                    task, task.getSchedulingState(), task.getWaitingReason());
            return false;
        }

        var subtasks = task.listSubtasks(result);
        if (!arePrerequisitesSatisfied(task, subtasks, true)) {
            return false;
        }

        var otherPrerequisites = taskRetriever.listPrerequisiteTasks(task, result);
        if (!arePrerequisitesSatisfied(task, otherPrerequisites, false)) {
            return false;
        }

        LOGGER.debug("All prerequisites of {} are satisfied, unpausing the task", task);
        try {
            return unpauseTask(task, result);
        } catch (PreconditionViolationException e) {
            LoggingUtils.logUnexpectedException(
                    LOGGER, "Task cannot be unpaused because it is no longer in WAITING state -- ignoring", e, this);
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean arePrerequisitesSatisfied(Task task, Collection<TaskQuartzImpl> prerequisites, boolean areSubtasks) {
        LOGGER.trace("Checking {} prerequisites (are subtasks: {}) for waiting task {}", prerequisites.size(), areSubtasks, task);
        for (Task prerequisite : prerequisites) {
            // Checking if the task is a worker task. They have more relaxed rules for closing their parent.
            // Note this is a slight violation of the layering principle (as the coordinator-worker relationship is dealt
            // with in repo-common module), but probably OK for now.
            var subtaskRole = areSubtasks ?
                    prerequisite.getPropertyRealValue(
                            TaskType.F_ACTIVITY_STATE.append(TaskActivityStateType.F_TASK_ROLE), TaskRoleType.class) :
                    null; // we ignore the role if the prerequisite is not a subtask
            if (subtaskRole == TaskRoleType.WORKER) {
                // We are more relaxed here, as the coordinator will check the work state itself.
                // If the work is not done, it will simply remain in the waiting state.
                if (prerequisite.isRunning()) {
                    LOGGER.debug(
                            "Prerequisite {} of {} is not satisfied, as it is a running worker subtask (state: {}/{})",
                            prerequisite, task, prerequisite.getExecutionState(), prerequisite.getSchedulingState());
                    return false;
                }
            } else {
                // Otherwise, we must wait for the prerequisite to be 100% done.
                if (!prerequisite.isClosed()) {
                    LOGGER.debug(
                            "Prerequisite {} of {} is not satisfied, as it is not a worker subtask and not closed (state: {}/{})",
                            prerequisite, task, prerequisite.getExecutionState(), prerequisite.getSchedulingState());
                    return false;
                }
            }
        }
        return true;
    }
}
