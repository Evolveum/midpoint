/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;

import com.evolveum.midpoint.task.quartzimpl.TaskBeans;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * Executes so called "task cycles" i.e. executions of the task handler.
 *
 * Responsibilities:
 *
 * 1. Invokes task handler
 * 2. Decides on the state change based on task run result obtained from handler
 * 3. Keeps cycle-related information in the task
 * 4. Implement recurring execution of tightly bound tasks
 *
 * TODO better name
 */
class TaskCycleExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(JobExecutor.class);

    private static final String DOT_CLASS = TaskCycleExecutor.class.getName() + ".";
    private static final String OP_EXECUTE_RECURRING_TASK = DOT_CLASS + "executeRecurringTask";

    @NotNull private final RunningTaskQuartzImpl task;
    @NotNull private final TaskHandler handler;
    @NotNull private final JobExecutor jobExecutor;
    @NotNull private final TaskBeans beans;

    private static final long WATCHFUL_SLEEP_INCREMENT = 500;

    TaskCycleExecutor(@NotNull RunningTaskQuartzImpl task, @NotNull TaskHandler handler,
            @NotNull JobExecutor jobExecutor, @NotNull TaskBeans beans) {
        this.task = task;
        this.handler = handler;
        this.jobExecutor = jobExecutor;
        this.beans = beans;
    }

    public void execute(OperationResult result) throws StopJobException {
        if (task.isSingle()) {
            executeSingleTask(result);
        } else if (task.isRecurring()) {
            executeRecurringTask();
        } else {
            LOGGER.error("Tasks must be either recurring or single-run. This one is neither. Sorry.");
            result.recordFatalError("Tasks must be either recurring or single-run. This one is neither. Sorry.");
            jobExecutor.closeFlawedTaskRecordingResult(result);
            throw new StopJobException();
        }
    }

    private void executeSingleTask(OperationResult result) {
        try {
            TaskRunResult runResult = executeTaskCycleRun(result);
            treatRunResultStatusForSingleTask(runResult, result);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "An exception occurred during processing of task {}", t, task);
        }
    }

    private void executeRecurringTask() {
        try {
            while (task.canRun()) {
                // Operation result should be initialized here (inside the loop), because for long-running tightly-bound
                // recurring tasks it would otherwise bloat indefinitely.
                OperationResult result = new OperationResult(OP_EXECUTE_RECURRING_TASK);

                checkLatestExecutionTime();

                TaskRunResult runResult = executeTaskCycleRun(result);
                treatRunResultStatusForRecurringTasks(runResult, result);

                if (task.isTightlyBound()) {
                    waitForNextRun(result);
                } else {
                    LOGGER.trace("Execution loop: task is loosely bound, exiting the execution cycle");
                    break;
                }
            }
        } catch (StopTaskException e) {
            LOGGER.trace("Stopping recurring task execution of {}", task);
        } catch (Throwable t) {
            // This is supposed to run in a thread, so this kind of heavy artillery is needed. If throwable won't be
            // caught here, nobody will catch it and it won't even get logged.
            if (task.canRun()) {
                LOGGER.error("Task cycle executor got unexpected exception: {}: {}; task = {}",
                        t.getClass().getName(), t.getMessage(), task, t);
            } else {
                LOGGER.info("Task cycle executor got unexpected exception while shutting down: {}: {}; task = {}",
                        t.getClass().getName(), t.getMessage(), task);
                LOGGER.trace("Task cycle executor got unexpected exception while shutting down: {}: {}; task = {}",
                        t.getClass().getName(), t.getMessage(), task, t);
            }
        }
    }

    /** Task is refreshed after returning from this method. */
    private TaskRunResult executeTaskCycleRun(OperationResult result) throws StopTaskException {
        processCycleRunStart(result);
        TaskRunResult runResult = executeHandler(result);
        processCycleRunFinish(runResult, result);
        return runResult;
    }

    @NotNull
    private TaskRunResult executeHandler(OperationResult result) {
        TaskRunResult runResult = beans.handlerExecutor.executeHandler(task, handler, result);

        // It is dangerous to start waiting for transient children if they were not told to finish!
        // Make sure you signal them to stop at appropriate place.
        jobExecutor.waitForTransientChildrenAndCloseThem(result);
        return runResult;
    }

    private void processCycleRunStart(OperationResult result) {
        LOGGER.debug("Task cycle run STARTING {}, handler = {}", task, handler);
        beans.listenerRegistry.notifyTaskStart(task, result);
        try {
            task.setLastRunStartTimestamp(System.currentTimeMillis());
            setOrMigrateChannelUri();
            setNewOperationResult();
            task.flushPendingModifications(result);
        } catch (Exception e) {
            throw new SystemException("Cannot process cycle run start: " + e.getMessage(), e);
        }
    }

    private void setOrMigrateChannelUri() {
        if (task.getChannel() != null) {
            Channel.Migration migration = Channel.findMigration(task.getChannel());
            if (migration != null && migration.isNeeded()) {
                task.setChannel(migration.getNewUri());
            }
        } else {
            task.setChannel(task.getChannelFromHandler());
        }
    }

    private void setNewOperationResult() {
        OperationResult newResult = new OperationResult("run");
        newResult.setStatus(OperationResultStatus.IN_PROGRESS);
        task.setResult(newResult); // MID-4033
    }

    /**
     * 1. Updates task based on run result (except for state changes).
     * 2. Records remaining cycle run information.
     * 3. Refreshes the task.
     */
    private void processCycleRunFinish(TaskRunResult runResult, OperationResult result) throws StopTaskException {
        LOGGER.debug("Task cycle run FINISHED {}, handler = {}", task, handler);
        beans.listenerRegistry.notifyTaskFinish(task, runResult, result);
        try {
            if (runResult.getProgress() != null) {
                task.setLegacyProgress(runResult.getProgress());
            }
            updateTaskResult(runResult);
            task.setLastRunFinishTimestamp(System.currentTimeMillis());
            task.flushPendingModifications(result);
            task.refresh(result);
        } catch (ObjectNotFoundException ex) {
            LoggingUtils.logException(LOGGER, "Cannot process cycle run finish for {}", ex, task);
            throw new StopTaskException();
        } catch (ObjectAlreadyExistsException | SchemaException | RuntimeException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot process cycle run finish for {}", ex, task);
        }
    }

    /** Updates task result (both live and in-prism versions) from runResult fields, recomputing it if necessary. */
    private void updateTaskResult(TaskRunResult runResult) {
        OperationResult taskResult = task.getResult();
        if (taskResult.isUnknown() || taskResult.isInProgress()) {
            taskResult.recomputeStatus();
        }
        if (runResult.getOperationResultStatus() != null) {
            taskResult.setStatus(runResult.getOperationResultStatus());
        }
        if (runResult.getMessage() != null) {
            taskResult.setMessage(runResult.getMessage());
        }
        // TODO Clean up the result before updating (summarize, remove minor operations - maybe deeply?) - see e.g. MID-7830
        task.setResult(taskResult); // This updates the result in the task prism object.
    }

    private void treatRunResultStatusForSingleTask(TaskRunResult runResult, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.INTERRUPTED) {
            LOGGER.trace("Task was interrupted. No need to change the task state. Task = {}", task);
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR) {
            LOGGER.info("Task encountered temporary error. Suspending it. Task = {}", task);
            beans.taskStateManager.suspendTaskNoException(task, TaskManager.DO_NOT_STOP, result);
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR) {
            LOGGER.trace("Task encountered permanent error. Suspending it. Task = {}", task);
            beans.taskStateManager.suspendTaskNoException(task, TaskManager.DO_NOT_STOP, result);
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.HALTING_ERROR) {
            LOGGER.trace("Task encountered halting error. Suspending it. Task = {}", task);
            beans.taskStateManager.suspendTaskNoException(task, TaskManager.DO_NOT_STOP, true, result);
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.FINISHED) {
            LOGGER.trace("Task finished normally. Closing it. Task = {}", task);
            beans.taskStateManager.closeTask(task, result);
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.IS_WAITING) {
            LOGGER.trace("Task switched to waiting state. No need to change the task state here. Task = {}", task);
        } else {
            invalidValue(runResult);
        }
    }

    private void treatRunResultStatusForRecurringTasks(TaskRunResult runResult, OperationResult result)
            throws StopTaskException {
        if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.INTERRUPTED) {
            LOGGER.trace("Task was interrupted. No need to change the task state. Stopping. Task = {}", task);
            throw new StopTaskException();
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR) {
            LOGGER.trace("Task encountered temporary error. Continuing as scheduled. Task = {}", task);
            // no stopping exception here
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR) {
            LOGGER.info("Task encountered permanent error. Suspending it. Task = {}", task);
            beans.taskStateManager.suspendTaskNoException(task, TaskManager.DO_NOT_STOP, result);
            throw new StopTaskException();
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.FINISHED) {
            LOGGER.trace("Task handler finished normally. Continuing as scheduled. Task = {}", task);
            // no stopping exception here
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.IS_WAITING) {
            LOGGER.trace("Task switched to waiting state. No need to change the task state. Stopping. Task = {}", task);
            throw new StopTaskException();
        } else {
            invalidValue(runResult);
        }
    }

    private void invalidValue(TaskRunResult runResult) {
        throw new IllegalStateException("Invalid value for Task's runResultStatus: " + runResult.getRunResultStatus() +
                " for task " + task);
    }

    private void checkLatestExecutionTime() throws StopTaskException {
        if (!task.stillCanStart()) {
            LOGGER.trace("CycleRunner loop: task latest start time ({}) has elapsed, exiting the execution cycle. "
                    + "Task = {}", task.getSchedule().getLatestStartTime(), task);
            throw new StopTaskException();
        }
    }

    private void waitForNextRun(OperationResult result) throws StopTaskException, SchemaException {
        if (!task.isReady()) {
            LOGGER.info("Task not in the READY state, exiting the execution routing. State = {}, Task = {}",
                    task.getSchedulingState(), task);
            throw new StopTaskException();
        }

        Integer interval = task.getScheduleInterval();
        if (interval == null) {
            LOGGER.error("Tightly bound task {} has no scheduling interval specified.", task);
            throw new StopTaskException();
        }

        long lastRunStartTime = task.getLastRunStartTimestamp() == null ? 0 : task.getLastRunStartTimestamp();
        long sleepUntil = lastRunStartTime + interval * 1000;

        sleepUntil(sleepUntil);

        LOGGER.trace("Refreshing task after sleep, task = {}", task);
        try {
            task.refresh(result);
        } catch (ObjectNotFoundException ex) {
            LOGGER.error("Error refreshing task "+task+": Object not found: "+ex.getMessage(),ex);
            throw new StopTaskException();
        }

        if (!task.isReady()) {
            LOGGER.info("Task not in the READY state, exiting the execution routine. State = {}, Task = {}",
                    task.getSchedulingState(), task);
            throw new StopTaskException();
        }
    }

    private void sleepUntil(long until) throws StopTaskException {
        LOGGER.trace("Sleeping until {} (for {} ms)", until, until - System.currentTimeMillis());
        MiscUtil.sleepWatchfully(until, WATCHFUL_SLEEP_INCREMENT, task::canRun);
        if (!task.canRun()) {
            LOGGER.trace("Sleep interrupted, task.canRun is false");
            throw new StopTaskException();
        }
        LOGGER.trace("Sleeping done");
    }
}
