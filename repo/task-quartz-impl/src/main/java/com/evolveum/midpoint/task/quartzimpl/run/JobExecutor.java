/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.base.MoreObjects;
import org.quartz.*;
import org.springframework.security.core.Authentication;

import static com.evolveum.midpoint.task.quartzimpl.run.GroupLimitsChecker.*;
import static com.evolveum.midpoint.task.quartzimpl.run.StopJobException.Severity.*;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Executes a Quartz job i.e. midPoint task.
 */
@DisallowConcurrentExecution
public class JobExecutor implements InterruptableJob {

    private static final Trace LOGGER = TraceManager.getTrace(JobExecutor.class);

    private static final String DOT_CLASS = JobExecutor.class.getName() + ".";
    public static final String OP_EXECUTE = DOT_CLASS + "execute";

    /**
     * JobExecutor is instantiated at each execution of the task, so we can safely store
     * the task here.
     *
     * http://quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-03
     * "Each (and every) time the scheduler executes the job, it creates a new instance of
     * the class before calling its execute(..) method."
     */
    private volatile RunningTaskQuartzImpl task;

    /** Quartz execution context. To be used from the handling thread only. */
    private JobExecutionContext context;

    /**
     * This is a result used for task run preparation. It is written into the task on selected occasions.
     * See {@link #closeFlawedTaskRecordingResult(OperationResult)}}.
     */
    private OperationResult executionResult;

    /** Useful Spring beans. */
    private static TaskBeans beans;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            this.context = context;
            this.executionResult = OperationResult.newResult(OP_EXECUTE)
                    .notRecordingValues() // TEMPORARY
                    .build();
            executeInternal(executionResult);
        } catch (StopJobException e) {
            e.log(LOGGER);
            if (e.getCause() != null) {
                throw new JobExecutionException(e.getMessage(), e.getCause());
            }
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unexpected exception occurred during task execution", e);
            // We do not need to propagate this.
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unexpected exception occurred during task execution", t);
            // This is e.g. an Error, so let us propagate that to Quartz.
            // (But actually nothing special is done with it there.)
            throw t;
        }
    }

    private void executeInternal(OperationResult result)
            throws StopJobException, SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        stateCheck(beans != null, "Task manager beans are not correctly set");

        String oid = context.getJobDetail().getKey().getName();
        LOGGER.debug("Starting execution of task {}", oid);

        fetchTheTask(oid, result);

        checkTaskReady();
        fixTaskExecutionInformation(result);
        checkLocalSchedulerRunning(result);

        boolean isRecovering = applyThreadStopActionForRecoveringJobs(result);

        checkGroupLimits(result);

        boolean nodeAndStateSet = false; // To know if we should reset task node/state information.
        boolean taskRegistered = false; // To know if we should unregister the task.
        TaskHandler handler = null;
        try {
            checkForConcurrentExecution(result);

            setExecutionNodeAndState(result);
            nodeAndStateSet = true;

            task.setExecutingThread(Thread.currentThread());
            beans.localNodeState.registerRunningTask(task);
            taskRegistered = true;

            setupThreadLocals();

            handler = getHandler(result);

            LOGGER.debug("Task thread run STARTING: {}, handler = {}, isRecovering = {}", task, handler, isRecovering);
            beans.listenerRegistry.notifyTaskThreadStart(task, isRecovering, result);

            setupSecurityContext(result);

            executeHandler(handler, result);

        } finally {

            unsetSecurityContext();

            LOGGER.debug("Task thread run FINISHED: {}, handler = {}", task, handler);
            beans.listenerRegistry.notifyTaskThreadFinish(task, result);

            unsetThreadLocals();

            task.setExecutingThread(null);
            if (taskRegistered) {
                beans.localNodeState.unregisterRunningTask(task);
            }

            if (nodeAndStateSet) {
                resetTaskExecutionNodeAndState(result);
            }

            if (!task.canRun()) {
                processTaskStop(result);
            }

            // this is only a safety net; because we've waited for children just after executing a handler
            waitForTransientChildrenAndCloseThem(result);
        }
    }

    private void executeHandler(TaskHandler handler, OperationResult result) throws StopJobException {
        new TaskCycleExecutor(task, handler, this, beans)
                .execute(result);
    }

    private void setupSecurityContext(OperationResult result) throws StopJobException {
        PrismObject<? extends FocusType> taskOwner = task.getOwner(result);
        if (taskOwner == null) {
            String oid = task.getOwnerRef() != null ? task.getOwnerRef().getOid() : null;
            result.recordFatalError("Task owner couldn't be resolved: " + oid);
            suspendFlawedTaskRecordingResult(result);
            throw new StopJobException(ERROR, "Task owner couldn't be resolved: %s", null, oid);
        }

        try {
            // just to be sure we won't run the owner-setting login with any garbage security context (see MID-4160)
            beans.securityContextManager.setupPreAuthenticatedSecurityContext((Authentication) null);
            beans.securityContextManager.setupPreAuthenticatedSecurityContext(taskOwner, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            suspendFlawedTaskRecordingResult(result);
            throw new StopJobException(UNEXPECTED_ERROR, "Couldn't set security context for task %s", t, task);
        }
    }

    private void unsetSecurityContext() {
        beans.securityContextManager.setupPreAuthenticatedSecurityContext((Authentication) null);
    }

    private void setupThreadLocals() {
        beans.cacheConfigurationManager.setThreadLocalProfiles(task.getCachingProfiles());
        OperationResult.setThreadLocalHandlingStrategy(task.getOperationResultHandlingStrategyName());
    }

    private void unsetThreadLocals() {
        beans.cacheConfigurationManager.unsetThreadLocalProfiles();
        OperationResult.setThreadLocalHandlingStrategy(null);
    }

    private TaskHandler getHandler(OperationResult result) throws StopJobException {
        String handlerUri = task.getHandlerUri();

        TaskHandler handler = beans.handlerRegistry.getHandler(handlerUri);
        if (handler != null) {
            return handler;
        }

        LOGGER.error("No handler for URI '{}', task {} - closing it.", handlerUri, task);
        closeFlawedTaskRecordingResult(result);
        throw new StopJobException();
    }

    /**
     * In case of leftover tasks, let us fix the execution state and node information.
     */
    private void fixTaskExecutionInformation(OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException {
        assert task.getSchedulingState() == TaskSchedulingStateType.READY;

        if (context.isRecovering()) {
            LOGGER.info("Task {} is recovering", task);
        }

        if (task.getNode() != null) {
            LOGGER.info("Clearing executing node information (was: {}) for {}", task.getNode(), task);
            task.setNode(null);
        }
        if (task.getExecutionState() != TaskExecutionStateType.RUNNABLE) {
            LOGGER.info("Fixing execution state from {} to RUNNABLE for {}", task.getExecutionState(), task);
            task.setExecutionState(TaskExecutionStateType.RUNNABLE);
        }
        task.flushPendingModifications(result);
        // Not handling any exceptions here. If we cannot write such simple
        // information into the task, something is seriously broken.
    }

    /**
     * Returns whether the job is recovering.
     */
    private boolean applyThreadStopActionForRecoveringJobs(OperationResult result) throws StopJobException {
        if (!context.isRecovering()) {
            return false;
        }

        ThreadStopActionType action = MoreObjects.firstNonNull(task.getThreadStopAction(), ThreadStopActionType.RESTART);
        switch (action) {
            case CLOSE:
                LOGGER.info("Closing recovered non-resilient task {}", task);
                closeTask(task, result);
                throw new StopJobException();
            case SUSPEND:
                LOGGER.info("Suspending recovered non-resilient task {}", task);
                // Using DO_NOT_STOP because we are the task that is to be suspended
                beans.taskStateManager.suspendTaskNoException(task, TaskManager.DO_NOT_STOP, result);
                throw new StopJobException();
            case RESTART:
                LOGGER.info("Recovering resilient task {}", task);
                return true;
            case RESCHEDULE:
                if (task.isRecurring() && task.isLooselyBound()) {
                    LOGGER.info("Recovering resilient task with RESCHEDULE thread stop action - exiting the execution, "
                            + "the task will be rescheduled; task = {}", task);
                    throw new StopJobException();
                } else {
                    LOGGER.info("Recovering resilient task {}", task);
                    return true;
                }
            default:
                throw new SystemException("Unknown value of ThreadStopAction: " + action + " for task " + task);
        }
    }

    private void checkLocalSchedulerRunning(OperationResult result) throws StopJobException {
        // if task manager is stopping or stopped, stop this task immediately
        // this can occur in rare situations, see https://support.evolveum.com/wp/1167
        if (beans.localScheduler.isRunningChecked()) {
            return;
        }

        LOGGER.warn("Task was started while task manager is not running: exiting and rescheduling (if needed)");
        processTaskStop(result);
        throw new StopJobException();
    }

    private void checkTaskReady() throws StopJobException {
        if (task.isReady()) {
            return;
        }

        // We intentionally do not remove the Quartz trigger, as the task could be re-executed later in the correct state.
        // (On the other hand this could mean accumulation of warning messages, if Quartz re-attempts to run this task.
        // But that should resolve automatically on the next task synchronization, e.g. when a node is started.)

        throw new StopJobException(WARNING, "A non-ready task %s (state is e:%s/s:%s) was attempted to execute. "
                + "Exiting the execution.", null, task, task.getExecutionState(), task.getSchedulingState());
    }

    private void fetchTheTask(String oid, OperationResult result) throws StopJobException {
        try {
            TaskQuartzImpl taskWithResult = beans.taskRetriever.getTaskWithResult(oid, result);

            if (taskWithResult.getTaskIdentifier() == null) {
                taskWithResult = generateTaskIdentifier(taskWithResult, result);
            }

            ParentAndRoot parentAndRoot = taskWithResult.getParentAndRoot(result);
            task = beans.taskInstantiator.toRunningTaskInstance(taskWithResult, parentAndRoot.root, parentAndRoot.parent);
        } catch (ObjectNotFoundException e) {
            beans.localScheduler.deleteTaskFromQuartz(oid, false, result);
            throw new StopJobException(ERROR, "Task with OID %s no longer exists. "
                    + "Removed the Quartz job and exiting the execution routine.", e, oid);
        } catch (Throwable t) {
            throw new StopJobException(UNEXPECTED_ERROR, "Task with OID %s could not be retrieved. "
                    + "Please correct the problem or resynchronize midPoint repository with Quartz job store. "
                    + "Now exiting the execution routine.", t, oid);
        }
    }

    private static TaskQuartzImpl generateTaskIdentifier(TaskQuartzImpl task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        String taskOid = task.getOid();
        String newIdentifier = beans.lightweightIdentifierGenerator.generate().toString();

        beans.repositoryService.modifyObject(
                TaskType.class,
                taskOid,
                beans.prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_TASK_IDENTIFIER)
                        .replace(newIdentifier)
                        .asItemDeltas(),
                result);

        LOGGER.info("Generated identifier {} for task {}", newIdentifier, task);

        var reloadedTask = beans.taskRetriever.getTaskWithResult(taskOid, result);
        stateNonNull(reloadedTask.getTaskIdentifier(), "Still no identifier in %s", reloadedTask);
        return reloadedTask;
    }

    /**
     * This is a sanity check against concurrent executions of a task in a cluster.
     * Quartz should take care of it but it looks like it sometimes fails to do that
     * (e.g. when the time is not synchronized enough).
     */
    private void checkForConcurrentExecution(OperationResult result)
            throws ObjectNotFoundException, SchemaException, StopJobException {
        if (beans.configuration.isCheckForTaskConcurrentExecution()) {
            new ConcurrentExecutionChecker(task, beans)
                    .check(result);
        }
    }

    private void setExecutionNodeAndState(OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        task.setExecutionState(TaskExecutionStateType.RUNNING);
        task.setNode(beans.configuration.getNodeId());
        task.flushPendingModifications(result);
    }

    private void resetTaskExecutionNodeAndState(OperationResult result) {
        try {
            task.setNode(null);
            task.flushPendingModifications(result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reset task execution node information for {}", e, task);
        }

        try {
            task.refresh(result);
            // If the task was suspended or closed or whatever in the meanwhile, most probably the new value is reflected here.
            if (task.getSchedulingState() == TaskSchedulingStateType.READY) {
                // But to be sure, let us do preconditions-based modification
                try {
                    task.setExecutionAndSchedulingStateImmediate(TaskExecutionStateType.RUNNABLE, TaskSchedulingStateType.READY,
                            TaskSchedulingStateType.READY, result);
                } catch (PreconditionViolationException e) {
                    LOGGER.trace("The scheduling state was no longer READY. Let us refresh the task.", e);
                    task.refresh(result);
                    resetExecutionState(result);
                }
            } else {
                resetExecutionState(result);
            }
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't set execution state information for {}", e, task);
        }
    }

    private void resetExecutionState(OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException {
        TaskExecutionStateType newExecutionState = getNewExecutionState();
        if (newExecutionState != null) {
            task.setExecutionState(newExecutionState);
            task.flushPendingModifications(result);
        }
    }

    private TaskExecutionStateType getNewExecutionState() {
        if (task.getSchedulingState() == null) {
            LOGGER.error("No scheduling state in {}. Setting execution state to SUSPENDED.", task);
            return TaskExecutionStateType.SUSPENDED;
        }
        return switch (task.getSchedulingState()) {
            case SUSPENDED -> TaskExecutionStateType.SUSPENDED;
            case CLOSED -> TaskExecutionStateType.CLOSED;
            case READY -> TaskExecutionStateType.RUNNABLE; // Not much probable, but can occur in theory.

            // The current execution state should be OK. It is because the switch to WAITING was done internally
            // by the task handler, and was accompanied by the change in the execution state.
            case WAITING -> null;
        };
    }

    private void checkGroupLimits(OperationResult result) throws StopJobException {
        GroupLimitsChecker checker = new GroupLimitsChecker(task, beans);
        RescheduleTime rescheduleTime = checker.checkIfAllowedToRun(result);
        if (rescheduleTime == null) {
            return; // everything ok
        }

        if (!rescheduleTime.regular) {
            try {
                beans.localScheduler.rescheduleLater(task, rescheduleTime.timestamp);
            } catch (Exception e) {
                throw new StopJobException(UNEXPECTED_ERROR, "Couldn't reschedule task " + task + " (rescheduled because" +
                        " of execution constraints): " + e.getMessage(), e);
            }
        }
        throw new StopJobException();
    }

    void waitForTransientChildrenAndCloseThem(OperationResult result) {
        beans.lightweightTaskManager.waitForTransientChildrenAndCloseThem(task, result);
    }

    /**
     * Called when we learned that the task was externally stopped. It can occur on:
     *
     *  - task suspension,
     *  - node shutdown,
     *  - node scheduler stop,
     *  - node threads deactivation,
     *  - or when the task is started but local scheduler is stopped (should not occur).
     *
     * We need to act accordingly (except for when the task has been suspended):
     *
     *  - reschedule resilient tasks
     *  - close/suspend non-resilient tasks
     */
    private void processTaskStop(OperationResult executionResult) {

        try {
            task.refresh(executionResult);

            if (!task.isReady()) {
                LOGGER.trace("processTaskStop: task scheduling status is not READY (it is {}), "
                                + "so ThreadStopAction does not apply; task = {}", task.getSchedulingState(), task);
                return;
            }

            ThreadStopActionType action = MoreObjects.firstNonNull(task.getThreadStopAction(), ThreadStopActionType.RESTART);
            switch (action) {
                case CLOSE:
                    LOGGER.info("Closing non-resilient task on node shutdown; task = {}", task);
                    closeTask(task, executionResult);
                    break;
                case SUSPEND:
                    LOGGER.info("Suspending non-resilient task on node shutdown; task = {}", task);
                    // We must NOT wait here, as we would wait infinitely.
                    // And we do not have to stop the task neither, because we are that task.
                    beans.taskStateManager.suspendTaskNoException(task, TaskManager.DO_NOT_STOP, executionResult);
                    break;
                case RESTART:
                    LOGGER.info("Rescheduling resilient task to run immediately; task = {}", task);
                    beans.taskStateManager.scheduleTaskNow(task, executionResult);
                    break;
                case RESCHEDULE:
                    if (task.isRecurring() && task.isLooselyBound()) {
                        // nothing to do, task will be automatically started by Quartz on next trigger fire time
                    } else {
                        // for tightly-bound tasks we do not know next schedule time, so we run them immediately
                        beans.taskStateManager.scheduleTaskNow(task, executionResult);
                    }
                    break;
                default:
                    throw new SystemException("Unknown value of ThreadStopAction: " + action + " for task " + task);
            }
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "ThreadStopAction cannot be applied because the task no longer exists: {}",
                    e, task);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "ThreadStopAction cannot be applied because of unexpected exception: {}",
                    e, task);
        }
    }

    // Note that the result is most probably == executionResult. But it is no problem.
    void closeFlawedTaskRecordingResult(OperationResult result) {
        LOGGER.info("Closing flawed task {}", task);
        try {
            task.setResultImmediate(executionResult, result);
        } catch (ObjectNotFoundException  e) {
            LoggingUtils.logException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        }
        closeTask(task, result);
    }

    // TODO reconsider when we should close and when we should suspend a flawed task.
    //  In general we use the latter if the issue can be resolved externally (e.g. missing task owner object).
    //  We use the former if the task is problematic in itself (e.g. missing handler URI).
    //  Anyway, maybe suspending is the best option overall.
    //
    // Note that the result is most probably == executionResult. But it is no problem.
    private void suspendFlawedTaskRecordingResult(OperationResult result) {
        LOGGER.info("Suspending flawed task {}", task);
        try {
            task.setResultImmediate(executionResult, result);
        } catch (ObjectNotFoundException  e) {
            LoggingUtils.logException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        }
        beans.taskStateManager.suspendTaskNoException(task, TaskManager.DO_NOT_STOP, result);
    }

    private void closeTask(RunningTaskQuartzImpl task, OperationResult result) {
        try {
            beans.taskStateManager.closeTask(task, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot close task {}, because it does not exist in repository.", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot close task {} due to schema exception", e, task);
        } catch (SystemException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot close task {} due to system exception", e, task);
        }
    }

    @Override
    public void interrupt() {
        boolean interruptsAlways = beans.configuration.getUseThreadInterrupt() == UseThreadInterrupt.ALWAYS;
        boolean interruptsMaybe = beans.configuration.getUseThreadInterrupt() != UseThreadInterrupt.NEVER;
        if (task != null) {
            LOGGER.trace("Trying to shut down the task {}, executing in thread {}", task, task.getExecutingThread());
            task.unsetCanRun();
            for (RunningLightweightTaskImpl subtask : task.getRunnableOrRunningLightweightAsynchronousSubtasks()) {
                subtask.unsetCanRun();
                // if we want to cancel the Future using interrupts, we have to do it now
                // because after calling cancel(false) subsequent calls to cancel(true) have no effect whatsoever
                subtask.cancel(interruptsMaybe);
            }
            if (interruptsAlways) {
                sendThreadInterrupt(false); // subtasks were interrupted by their futures
            }
        }
    }

    public void sendThreadInterrupt() {
        sendThreadInterrupt(true);
    }

    // beware: Do not touch task prism here, because this method can be called asynchronously
    private void sendThreadInterrupt(boolean alsoSubtasks) {
        Thread thread = task.getExecutingThread();
        if (thread != null) { // in case this method would be (mistakenly?) called after the execution is over
            LOGGER.trace("Calling Thread.interrupt on thread {}.", thread);
            thread.interrupt();
            LOGGER.trace("Thread.interrupt was called on thread {}.", thread);
        }
        if (alsoSubtasks) {
            for (RunningLightweightTaskImpl subtask : task.getRunningLightweightAsynchronousSubtasks()) {
                subtask.cancel(true);
            }
        }
    }

    public Thread getExecutingThread() {
        return task.getExecutingThread();
    }

    /**
     * Ugly hack - this class is instantiated not by Spring but explicitly by Quartz.
     */
    public static void setTaskManagerQuartzImpl(TaskManagerQuartzImpl taskManager) {
        beans = taskManager.getBeans();
    }
}
