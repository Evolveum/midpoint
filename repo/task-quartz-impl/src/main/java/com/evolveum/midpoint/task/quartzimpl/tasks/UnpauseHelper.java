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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskUnpauseActionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWaitingReasonType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

        List<TaskQuartzImpl> allPrerequisites = task.listSubtasks(result);
        allPrerequisites.addAll(taskRetriever.listPrerequisiteTasks(task, result));

        LOGGER.trace("Checking {} prerequisites for waiting task {}", allPrerequisites.size(), task);

        for (Task prerequisite : allPrerequisites) {
            if (!prerequisite.isClosed()) {
                LOGGER.debug("Prerequisite {} of {} is not closed (scheduling state = {})",
                        prerequisite, task, prerequisite.getSchedulingState());
                return false;
            }
        }
        LOGGER.debug("All prerequisites of {} are closed, unpausing the task", task);
        try {
            return unpauseTask(task, result);
        } catch (PreconditionViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Task cannot be unpaused because it is no longer in WAITING state -- ignoring", e, this);
            return false;
        }
    }
}
