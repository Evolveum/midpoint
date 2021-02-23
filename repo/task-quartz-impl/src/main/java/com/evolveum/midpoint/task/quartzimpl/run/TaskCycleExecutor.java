package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;

import com.evolveum.midpoint.task.quartzimpl.TaskBeans;

import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
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
    public static final String OP_EXECUTE_RECURRING_TASK = DOT_CLASS + "executeRecurringTask";

    @NotNull private final RunningTaskQuartzImpl task;
    @NotNull private final TaskHandler handler;
    @NotNull private final JobExecutor jobExecutor;
    @NotNull private final TaskBeans beans;

    private static final long WATCHFUL_SLEEP_INCREMENT = 500;

    public TaskCycleExecutor(@NotNull RunningTaskQuartzImpl task, @NotNull TaskHandler handler,
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
            LOGGER.error("Tasks must be either recurrent or single-run. This one is neither. Sorry.");
            result.recordFatalError("Tasks must be either recurrent or single-run. This one is neither. Sorry.");
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
                    LOGGER.trace("CycleRunner loop: task is loosely bound, exiting the execution cycle");
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
        TaskRunResult runResult = executeHandler(handler, result);
        processCycleRunFinish(runResult, result);
        return runResult;
    }

    @NotNull
    private TaskRunResult executeHandler(TaskHandler handler, OperationResult result) {
        if (task.getResult() == null) {
            LOGGER.warn("Task without operation result found, please check the task creation/retrieval/update code: {}", task);
            task.setResultTransient(TaskQuartzImpl.createUnnamedTaskResult());
        }

        TaskRunResult runResult;
        try {
            runResult = beans.handlerExecutor.executeHandler(task, null, handler, result);
        } finally {
            // TEMPORARY see MID-6343; TODO implement correctly!
            beans.counterManager.cleanupCounters(task.getOid());
        }

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
            setCategoryIfMissing();
            setOrMigrateChannelUri();
            setNewOperationResult();
            task.flushPendingModifications(result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot process run start for task {}", e, task);
            throw new SystemException("Cannot process cycle run start: " + e.getMessage(), e);
        }
    }

    private void setCategoryIfMissing() {
        if (task.getCategory() == null) {
            task.setCategory(task.getCategoryFromHandler());
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
                task.setProgress(runResult.getProgress());
            }
            if (runResult.getOperationResult() != null) {
                try {
                    OperationResult taskResult = runResult.getOperationResult().clone();
                    taskResult.cleanupResult();
                    taskResult.summarize(true);
                    task.setResult(taskResult);
                } catch (Throwable ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Problem with task result cleanup/summarize - continuing with raw result", ex);
                    task.setResult(runResult.getOperationResult());
                }
            }
            task.setLastRunFinishTimestamp(System.currentTimeMillis());
            task.storeOperationStatsDeferred(); // maybe redundant, but better twice than never at all
            task.flushPendingModifications(result);
            task.refresh(result);
        } catch (ObjectNotFoundException ex) {
            LoggingUtils.logException(LOGGER, "Cannot process cycle run finish for {}", ex, task);
            throw new StopTaskException();
        } catch (ObjectAlreadyExistsException | SchemaException | RuntimeException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot process cycle run finish for {}", ex, task);
        }
    }

    private void treatRunResultStatusForSingleTask(TaskRunResult runResult, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.INTERRUPTED) {
            LOGGER.trace("Task was interrupted. No need to change the task state. Task = {}", task);
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR) {
            LOGGER.info("Task encountered temporary error. Suspending it. Task = {}", task);
            beans.taskStateManager.suspendTaskNoException(task, TaskManager.DO_NOT_STOP, result);
        } else if (runResult.getRunResultStatus() == TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR) {
            LOGGER.trace("Task encountered permanent error. Closing it. Task = {}", task);
            beans.taskStateManager.closeTask(task, result);
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
        while (System.currentTimeMillis() <= until) {
            try {
                //noinspection BusyWait
                Thread.sleep(WATCHFUL_SLEEP_INCREMENT);
            } catch (InterruptedException e) {
                // safely ignored
            }
            if (!task.canRun()) {
                LOGGER.trace("Sleep interrupted, task.canRun is false");
                throw new StopTaskException();
            }
        }
        LOGGER.trace("Sleeping done");
    }
}
