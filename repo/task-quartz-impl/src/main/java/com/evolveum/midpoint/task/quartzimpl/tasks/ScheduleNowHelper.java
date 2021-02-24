/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import static com.evolveum.midpoint.task.quartzimpl.tasks.TaskStateManager.clearTaskOperationResult;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;

@Component
class ScheduleNowHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ScheduleNowHelper.class);

    @Autowired private TaskRetriever taskRetriever;
    @Autowired private TaskManagerConfiguration configuration;
    @Autowired private LocalScheduler localScheduler;

    public void scheduleTaskNow(TaskQuartzImpl task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        /*
         *  Note: we clear task operation result because this is what a user would generally expect when re-running a task
         *  (MID-1920). We do NOT do that on each task run e.g. to have an ability to see last task execution status
         *  during a next task run. (When the interval between task runs is too short, e.g. for live sync tasks.)
         */
        if (task.isClosed()) {
            clearTaskOperationResult(task, result);
            reRunClosedTask(task, result);
        } else if (task.getSchedulingState() == TaskSchedulingStateType.READY) {
            clearTaskOperationResult(task, result);
            scheduleRunnableTaskNow(task, result);
        } else if (task.getSchedulingState() == TaskSchedulingStateType.WAITING) {
            clearTaskOperationResult(task, result);
            scheduleWaitingTaskNow(task, result);
        } else {
            String message = "Task " + task + " cannot be run now, because it is not in RUNNABLE nor CLOSED state. State is " + task.getSchedulingState();
            result.recordFatalError(message);
            LOGGER.error(message);
        }
    }

    public void scheduleCoordinatorAndWorkersNow(String coordinatorOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        TaskQuartzImpl coordinatorTask = taskRetriever.getTaskPlain(coordinatorOid, result);
        TaskSchedulingStateType state = coordinatorTask.getSchedulingState();
        switch (state) {
            case CLOSED:
            case READY:
                // hoping that the task handler will do what is needed (i.e. recreate or restart workers)
                scheduleTaskNow(coordinatorTask, result);
                break;
            case WAITING:
                // this means that workers are either busy (runnable) or are suspended; administrator should do something with that
                String msg1 =
                        "Coordinator " + coordinatorTask + " cannot be run now, because it is in WAITING scheduling state. " +
                                "Please check and resolve state of its worker tasks.";
                LOGGER.error(msg1);
                result.recordFatalError(msg1);
                break;
            case SUSPENDED:
                String msg2 =
                        "Coordinator " + coordinatorTask + " cannot be run now, because it is in SUSPENDED state. " +
                                "Please use appropriate method to schedule its execution.";
                LOGGER.error(msg2);
                result.recordFatalError(msg2);
                break;
            default:
                throw new IllegalStateException("Coordinator " + coordinatorTask + " is in unsupported state: " + state);
        }
    }

    private void scheduleRunnableTaskNow(TaskQuartzImpl task, OperationResult result) {
        // for loosely-bound, recurring, interval-based tasks we reschedule the task in order to start immediately
        // and then continue after specified interval (i.e. NOT continue according to original schedule) - MID-1410
        if (!configuration.isRunNowKeepsOriginalSchedule() && task.isLooselyBound() && task.isRecurring() && task.hasScheduleInterval()) {
            LOGGER.trace("'Run now' for task invoked: unscheduling and rescheduling it; task = {}", task);
            localScheduler.unscheduleTask(task, result);
            task.synchronizeWithQuartzWithTriggerRecreation(result);
        } else {
            // otherwise, we simply add another trigger to this task
            localScheduler.addTriggerNowForTask(task, result);
        }
    }

    // experimental, todo revise the logic
    void scheduleWaitingTaskNow(TaskQuartzImpl task, OperationResult result) {
        stateCheck(task.getSchedulingState() == TaskSchedulingStateType.WAITING,
                "Task is not waiting: %s (%s)", task, task.getSchedulingState());
        try {
            localScheduler.addJobIfNeeded(task);
            task.setExecutionAndSchedulingStateImmediate(
                    TaskExecutionStateType.RUNNABLE, TaskSchedulingStateType.READY,
                    TaskSchedulingStateType.WAITING, result);
            localScheduler.addTriggerNowForTask(task, result);
        } catch (SchedulerException | ObjectNotFoundException | SchemaException | PreconditionViolationException e) {
            String message = "Waiting task " + task + " cannot be scheduled: " + e.getMessage();
            result.recordFatalError(message, e);
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
        }
    }

    private void reRunClosedTask(TaskQuartzImpl task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (task.getSchedulingState() != TaskSchedulingStateType.CLOSED) {
            String message = "Task " + task + " cannot be re-run, because it is not in CLOSED state.";
            result.recordFatalError(message);
            LOGGER.error(message);
            return;
        }

        if (!task.isSingle()) {
            String message = "Closed recurring task " + task + " cannot be re-run, because this operation is not available for recurring tasks. Please use RESUME instead.";
            result.recordWarning(message);
            LOGGER.warn(message);
            return;
        }

        task.synchronizeWithQuartz(result); // this should remove any triggers (btw, is it needed?)
        task.setExecutionAndSchedulingStateImmediate(TaskExecutionStateType.RUNNABLE, TaskSchedulingStateType.READY, result);
        task.synchronizeWithQuartzWithTriggerRecreation(result);
    }
}
