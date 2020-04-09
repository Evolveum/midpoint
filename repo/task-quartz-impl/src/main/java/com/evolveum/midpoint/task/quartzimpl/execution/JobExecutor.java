/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImplUtil;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.quartz.*;
import org.springframework.security.core.Authentication;

import javax.xml.datatype.Duration;
import java.util.*;
import java.util.Objects;

@DisallowConcurrentExecution
public class JobExecutor implements InterruptableJob {

    private static TaskManagerQuartzImpl taskManagerImpl;
    private static final String DOT_CLASS = JobExecutor.class.getName() + ".";

    /*
     * Ugly hack - this class is instantiated not by Spring but explicitly by Quartz.
     */
    public static void setTaskManagerQuartzImpl(TaskManagerQuartzImpl managerImpl) {
        taskManagerImpl = managerImpl;
    }

    private static final Trace LOGGER = TraceManager.getTrace(JobExecutor.class);

    private static final long WATCHFUL_SLEEP_INCREMENT = 500;

    private static final int DEFAULT_RESCHEDULE_TIME_FOR_GROUP_LIMIT = 60;
    private static final int RESCHEDULE_TIME_RANDOMIZATION_INTERVAL = 3;

    /*
     * JobExecutor is instantiated at each execution of the task, so we can store
     * the task here.
     *
     * http://quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-03
     * "Each (and every) time the scheduler executes the job, it creates a new instance of
     * the class before calling its execute(..) method."
     */
    private volatile RunningTaskQuartzImpl task;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        OperationResult executionResult = createOperationResult("execute");

        if (taskManagerImpl == null) {
            LOGGER.error("TaskManager not correctly set for JobExecutor, exiting the execution routine.");
            return;
        }

        // get the task instance
        String oid = context.getJobDetail().getKey().getName();
        try {
            task = taskManagerImpl.createRunningTask(taskManagerImpl.getTaskWithResult(oid, executionResult));
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Task with OID {} no longer exists, removing Quartz job and exiting the execution routine.", e, oid);
            taskManagerImpl.getExecutionManager().removeTaskFromQuartz(oid, executionResult);
            return;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Task with OID {} cannot be retrieved because of schema exception. Please correct the problem or resynchronize midPoint repository with Quartz job store. Now exiting the execution routine.", e, oid);
            return;
        } catch (RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Task with OID {} could not be retrieved, exiting the execution routine.", e, oid);
            return;
        }

        if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
            LOGGER.warn("Task is not in RUNNABLE state (its state is {}), exiting its execution and removing its Quartz trigger. Task = {}", task.getExecutionStatus(), task);
            try {
                context.getScheduler().unscheduleJob(context.getTrigger().getKey());
            } catch (SchedulerException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot unschedule job for a non-RUNNABLE task {}", e, task);
            }
            return;
        }

        // if task manager is stopping or stopped, stop this task immediately
        // this can occur in rare situations, see https://jira.evolveum.com/browse/MID-1167
        if (!taskManagerImpl.isRunning()) {
            LOGGER.warn("Task was started while task manager is not running: exiting and rescheduling (if needed)");
            processTaskStop(executionResult);
            return;
        }

        boolean isRecovering;

        // if this is a restart, check whether the task is resilient
        if (context.isRecovering()) {
            // reset task node (there's potentially old information from crashed node)
            try {
                if (task.getNode() != null) {
                    LOGGER.info("Resetting executing-at-node information for {} because it is recovering. Previous value was '{}'.",
                            task, task.getNode());
                    task.setNodeImmediate(null, executionResult);
                }
            } catch (ObjectNotFoundException | SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot reset executing-at-node information for recovering task {}", e, task);
            }

            if (!processTaskRecovery(executionResult)) {
                return;
            }
            isRecovering = true;
        } else {
            isRecovering = false;
        }

        if (!checkExecutionConstraints(task, executionResult)) {
            return;            // rescheduling is done within the checker method
        }

        task.setExecutingThread(Thread.currentThread());

        LOGGER.trace("execute called; task = {}, thread = {}, isRecovering = {}", task, task.getExecutingThread(), isRecovering);

        boolean executionNodeSet = false;
        boolean taskRegistered = false;
        TaskHandler handler = null;
        try {
            executionNodeSet = checkAndSetTaskExecutionNode(executionResult);
            if (!executionNodeSet) {
                return;
            }

            taskManagerImpl.registerRunningTask(task);
            taskRegistered = true;

            taskManagerImpl.getCacheConfigurationManager().setThreadLocalProfiles(task.getCachingProfiles());
            OperationResult.setThreadLocalHandlingStrategy(task.getOperationResultHandlingStrategyName());

            handler = taskManagerImpl.getHandler(task.getHandlerUri());

            LOGGER.debug("Task thread run STARTING: {}, handler = {}, isRecovering = {}", task, handler, isRecovering);
            taskManagerImpl.notifyTaskThreadStart(task, isRecovering);

            if (handler==null) {
                LOGGER.error("No handler for URI '{}', task {} - closing it.", task.getHandlerUri(), task);
                executionResult.recordFatalError("No handler for URI '" + task.getHandlerUri() + "', closing the task.");
                closeFlawedTask(task, executionResult);
                return;
                //throw new JobExecutionException("No handler for URI '" + task.getHandlerUri() + "'");
                // actually there is no point in throwing JEE; the only thing that is done with it is
                // that it is logged by Quartz
            }

            // Setup Spring Security context
            PrismObject<? extends FocusType> taskOwner = task.getOwner();
            try {
                // just to be sure we won't run the owner-setting login with any garbage security context (see MID-4160)
                taskManagerImpl.getSecurityContextManager().setupPreAuthenticatedSecurityContext((Authentication) null);
                taskManagerImpl.getSecurityContextManager().setupPreAuthenticatedSecurityContext(taskOwner);
            } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Task with OID {} cannot be executed: error setting security context", e, oid);
                return;
            }

            if (task.isRecurring()) {
                executeRecurrentTask(handler);
            } else if (task.isSingle()) {
                executeSingleTask(handler, executionResult);
            } else {
                LOGGER.error("Tasks must be either recurrent or single-run. This one is neither. Sorry.");
                executionResult.recordFatalError("Tasks must be either recurrent or single-run. This one is neither. Closing it.");
                closeFlawedTask(task, executionResult);
            }

        } finally {

            try {
                waitForTransientChildrenAndCloseThem(executionResult);              // this is only a safety net; because we've waited for children just after executing a handler

                taskManagerImpl.getCacheConfigurationManager().unsetThreadLocalProfiles();

                if (taskRegistered) {
                    taskManagerImpl.unregisterRunningTask(task);
                }

                task.setExecutingThread(null);
                if (executionNodeSet) {
                    resetTaskExecutionNode(executionResult);
                }

                if (!task.canRun()) {
                    processTaskStop(executionResult);
                }

                LOGGER.debug("Task thread run FINISHED: {}, handler = {}", task, handler);
                taskManagerImpl.notifyTaskThreadFinish(task);
            } finally {
                // "logout" this thread
                taskManagerImpl.getSecurityContextManager().setupPreAuthenticatedSecurityContext((Authentication) null);
            }
        }
    }

    /**
     * We have a suspicion that Quartz (in some cases) allows multiple instances of a given task to run concurrently.
     * So we use "node" property to check this.
     *
     * Note that if the task is being recovered, its "node" property should be already cleared.
     *
     * The following algorithm is only approximate one. It could generate false positives e.g. if a node goes down abruptly
     * (i.e. without stopping tasks cleanly) and then restarts sooner than in "nodeTimeout" (30) seconds. Therefore, its use
     * is currently optional.
     */
    private boolean checkAndSetTaskExecutionNode(OperationResult result) {
        try {
            if (taskManagerImpl.getConfiguration().isCheckForTaskConcurrentExecution()) {
                task.refresh(result);
                String executingAtNode = task.getNode();
                if (executingAtNode != null) {
                    LOGGER.debug("Task {} seems to be executing on node {}", task, executingAtNode);
                    if (executingAtNode.equals(taskManagerImpl.getNodeId())) {
                        RunningTask locallyRunningTask = taskManagerImpl
                                .getLocallyRunningTaskByIdentifier(task.getTaskIdentifier());
                        if (locallyRunningTask != null) {
                            LOGGER.error(
                                    "Current task {} seems to be already running in thread {} on the local node. We will NOT start it here.",
                                    task, ((RunningTaskQuartzImpl) locallyRunningTask).getExecutingThread());
                            return false;
                        } else {
                            LOGGER.warn("Current task {} seemed to be already running on the local node but it cannot be found"
                                    + " there. So we'll start it now.", task);
                        }
                    } else {
                        ObjectQuery query = taskManagerImpl.getPrismContext().queryFor(NodeType.class)
                                .item(NodeType.F_NODE_IDENTIFIER).eq(executingAtNode)
                                .build();
                        SearchResultList<PrismObject<NodeType>> nodes = taskManagerImpl
                                .searchObjects(NodeType.class, query, null, result);
                        if (nodes.size() > 1) {
                            throw new IllegalStateException(
                                    "More than one node with identifier " + executingAtNode + ": " + nodes);
                        } else if (nodes.size() == 1) {
                            NodeType remoteNode = nodes.get(0).asObjectable();
                            if (taskManagerImpl.isCheckingIn(remoteNode)) {
                                LOGGER.error(
                                        "Current task {} seems to be already running at node {} that is alive or starting."
                                                + " We will NOT start it here.",
                                        task, remoteNode.getNodeIdentifier());
                                // We should probably contact the remote node and check if the task is really running there.
                                // But let's keep things simple for the time being.
                                return false;
                            } else {
                                LOGGER.warn(
                                        "Current task {} seems to be already running at node {} but this node is not currently "
                                                + "checking in (last: {}). So we will start the task here.", task,
                                        remoteNode.getNodeIdentifier(), remoteNode.getLastCheckInTime());
                            }
                        } else {
                            LOGGER.warn("Current task {} seems to be already running at node {} but this node cannot be found"
                                    + "in the repository. So we will start the task here.", task, executingAtNode);
                        }
                    }
                }
            }
            task.setNodeImmediate(taskManagerImpl.getNodeId(), result);
            return true;
        } catch (Throwable t) {
            throw new SystemException("Couldn't check and set task execution node: " + t.getMessage(), t);
        }
    }

    private void resetTaskExecutionNode(OperationResult result) {
        try {
            task.setNodeImmediate(null, result);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't reset task execution node information for {}", t, task);
        }
    }

    static class GroupExecInfo {
        int limit;
        Collection<Task> tasks = new ArrayList<>();

        GroupExecInfo(Integer l) {
            limit = l != null ? l : Integer.MAX_VALUE;
        }

        public void accept(Integer limit, Task task) {
            if (limit != null && limit < this.limit) {
                this.limit = limit;
            }
            if (tasks.stream().noneMatch(t -> Objects.equals(t.getOid(), task.getOid()))) {    // just for sure
                tasks.add(task);
            }
        }

        @Override
        public String toString() {
            return "{limit=" + limit + ", tasks=" + tasks + "}";
        }
    }

    // returns false if constraints are not met (i.e. execution should finish immediately)
    private boolean checkExecutionConstraints(RunningTaskQuartzImpl task, OperationResult result) throws JobExecutionException {
        TaskExecutionConstraintsType executionConstraints = task.getExecutionConstraints();
        if (executionConstraints == null) {
            return true;
        }

        // group limits
        Map<String, GroupExecInfo> groupMap = createGroupMap(task, result);
        LOGGER.trace("groupMap = {}", groupMap);
        for (Map.Entry<String, GroupExecInfo> entry : groupMap.entrySet()) {
            String group = entry.getKey();
            int limit = entry.getValue().limit;
            Collection<Task> tasksInGroup = entry.getValue().tasks;
            if (tasksInGroup.size() >= limit) {
                RescheduleTime rescheduleTime = getRescheduleTime(executionConstraints,
                        DEFAULT_RESCHEDULE_TIME_FOR_GROUP_LIMIT, task.getNextRunStartTime(result));
                LOGGER.info("Limit of {} task(s) in group {} would be exceeded if task {} would start. Existing tasks: {}."
                                + " Will try again at {}{}.", limit, group, task, tasksInGroup, rescheduleTime.asDate(),
                        rescheduleTime.regular ? " (i.e. at the next regular run time)" : "");
                if (!rescheduleTime.regular) {
                    rescheduleLater(task, rescheduleTime.timestamp);
                }
                return false;
            }
        }

        return true;
    }

    @NotNull
    private Map<String, GroupExecInfo> createGroupMap(RunningTaskQuartzImpl task, OperationResult result) {
        Map<String, GroupExecInfo> groupMap = new HashMap<>();
        Map<String, Integer> groupsWithLimits = task.getGroupsWithLimits();
        if (!groupsWithLimits.isEmpty()) {
            groupsWithLimits.forEach((g, l) -> groupMap.put(g, new GroupExecInfo(l)));
            ClusterStatusInformation csi = taskManagerImpl.getExecutionManager()
                    .getClusterStatusInformation(true, false, result);
            for (ClusterStatusInformation.TaskInfo taskInfo : csi.getTasks()) {
                if (task.getOid().equals(taskInfo.getOid())) {
                    continue;
                }
                Task otherTask;
                try {
                    otherTask = taskManagerImpl.getTaskPlain(taskInfo.getOid(), result);
                } catch (ObjectNotFoundException e) {
                    LOGGER.debug("Couldn't find running task {} when checking execution constraints: {}", taskInfo.getOid(),
                            e.getMessage());
                    continue;
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER,
                            "Couldn't retrieve running task {} when checking execution constraints", e, taskInfo.getOid());
                    continue;
                }
                addToGroupMap(groupMap, otherTask);
            }
        }
        return groupMap;
    }

    private void addToGroupMap(Map<String, GroupExecInfo> groupMap, Task otherTask) {
        for (Map.Entry<String, Integer> otherGroupWithLimit : otherTask.getGroupsWithLimits().entrySet()) {
            String otherGroup = otherGroupWithLimit.getKey();
            GroupExecInfo groupExecInfo = groupMap.get(otherGroup);
            if (groupExecInfo != null) {
                Integer otherLimit = otherGroupWithLimit.getValue();
                groupExecInfo.accept(otherLimit, otherTask);
            }
        }
    }

    private static class RescheduleTime {
        private final long timestamp;
        private final boolean regular;
        private RescheduleTime(long timestamp, boolean regular) {
            this.timestamp = timestamp;
            this.regular = regular;
        }
        public Date asDate() {
            return new Date(timestamp);
        }
    }

    private RescheduleTime getRescheduleTime(TaskExecutionConstraintsType executionConstraints, int defaultInterval, Long nextTaskRunTime) {
        long retryAt;
        Duration retryAfter = executionConstraints != null ? executionConstraints.getRetryAfter() : null;
        if (retryAfter != null) {
            retryAt = XmlTypeConverter.toMillis(
                            XmlTypeConverter.addDuration(
                                    XmlTypeConverter.createXMLGregorianCalendar(new Date()), retryAfter));
        } else {
            retryAt = System.currentTimeMillis() + defaultInterval * 1000L;
        }
        retryAt += Math.random() * RESCHEDULE_TIME_RANDOMIZATION_INTERVAL * 1000.0;     // to avoid endless collisions
        if (nextTaskRunTime != null && nextTaskRunTime < retryAt) {
            return new RescheduleTime(nextTaskRunTime, true);
        } else {
            return new RescheduleTime(retryAt, false);
        }
    }

    private void rescheduleLater(RunningTaskQuartzImpl task, long startAt) throws JobExecutionException {
        Trigger trigger = TaskQuartzImplUtil.createTriggerForTask(task, startAt);
        try {
            taskManagerImpl.getExecutionManager().getQuartzScheduler().scheduleJob(trigger);
        } catch (SchedulerException e) {
            // TODO or handle it somehow?
            throw new JobExecutionException("Couldn't reschedule task " + task + " (rescheduled because" +
                    " of execution constraints): " + e.getMessage(), e);
        }
    }

    private void waitForTransientChildrenAndCloseThem(OperationResult result) {
        taskManagerImpl.waitForTransientChildren(task, result);

        // at this moment, there should be no executing child tasks... we just clean-up all runnables that had not started
        for (RunningTaskQuartzImpl subtask : task.getLightweightAsynchronousSubtasks()) {
            if (subtask.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
                if (subtask.getLightweightHandlerFuture() == null) {
                    LOGGER.trace("Lightweight task handler for subtask {} has not started yet; closing the task.", subtask);
                    closeTask(subtask, result);
                }
            }
        }
    }

    // returns true if the execution of the task should continue
    private boolean processTaskRecovery(OperationResult executionResult) {
        if (task.getThreadStopAction() == ThreadStopActionType.CLOSE) {
            LOGGER.info("Closing recovered non-resilient task {}", task);
            closeTask(task, executionResult);
            return false;
        } else if (task.getThreadStopAction() == ThreadStopActionType.SUSPEND) {
            LOGGER.info("Suspending recovered non-resilient task {}", task);
            taskManagerImpl.suspendTaskQuietly(task, TaskManager.DO_NOT_STOP, executionResult);        // we must NOT wait here, as we would wait infinitely -- we do not have to stop the task neither, because we are that task :)
            return false;
        } else if (task.getThreadStopAction() == null || task.getThreadStopAction() == ThreadStopActionType.RESTART) {
            LOGGER.info("Recovering resilient task {}", task);
            return true;
        } else if (task.getThreadStopAction() == ThreadStopActionType.RESCHEDULE) {
            if (task.getRecurrenceStatus() == TaskRecurrence.RECURRING && task.isLooselyBound()) {
                LOGGER.info("Recovering resilient task with RESCHEDULE thread stop action - exiting the execution, the task will be rescheduled; task = {}", task);
                return false;
            } else {
                LOGGER.info("Recovering resilient task {}", task);
                return true;
            }
        } else {
            throw new SystemException("Unknown value of ThreadStopAction: " + task.getThreadStopAction() + " for task " + task);
        }
    }

    // called when task is externally stopped (can occur on node shutdown, node scheduler stop, node threads deactivation, or task suspension)
    // we have to act (i.e. reschedule resilient tasks or close/suspend non-resilient tasks) in all cases, except task suspension
    // we recognize it by looking at task status: RUNNABLE means that the task is stopped as part of node shutdown
    private void processTaskStop(OperationResult executionResult) {

        try {
            task.refresh(executionResult);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "ThreadStopAction cannot be applied, because the task no longer exists: " + task, e);
            return;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "ThreadStopAction cannot be applied, because of schema exception. Task = " + task, e);
            return;
        }

        if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
            LOGGER.trace("processTaskStop: task execution status is not RUNNABLE (it is " + task.getExecutionStatus() + "), so ThreadStopAction does not apply; task = " + task);
            return;
        }

        if (task.getThreadStopAction() == ThreadStopActionType.CLOSE) {
            LOGGER.info("Closing non-resilient task on node shutdown; task = {}", task);
            closeTask(task, executionResult);
        } else if (task.getThreadStopAction() == ThreadStopActionType.SUSPEND) {
            LOGGER.info("Suspending non-resilient task on node shutdown; task = {}", task);
            taskManagerImpl.suspendTaskQuietly(task, TaskManager.DO_NOT_STOP, executionResult);            // we must NOT wait here, as we would wait infinitely -- we do not have to stop the task neither, because we are that task
        } else if (task.getThreadStopAction() == null || task.getThreadStopAction() == ThreadStopActionType.RESTART) {
            LOGGER.info("Node going down: Rescheduling resilient task to run immediately; task = {}", task);
            taskManagerImpl.scheduleRunnableTaskNow(task, executionResult);
        } else if (task.getThreadStopAction() == ThreadStopActionType.RESCHEDULE) {
            if (task.getRecurrenceStatus() == TaskRecurrence.RECURRING && task.isLooselyBound()) {
                // nothing to do, task will be automatically started by Quartz on next trigger fire time
            } else {
                taskManagerImpl.scheduleRunnableTaskNow(task, executionResult);     // for tightly-bound tasks we do not know next schedule time, so we run them immediately
            }
        } else {
            throw new SystemException("Unknown value of ThreadStopAction: " + task.getThreadStopAction() + " for task " + task);
        }
    }

    private void closeFlawedTask(RunningTaskQuartzImpl task, OperationResult result) {
        LOGGER.info("Closing flawed task {}", task);
        try {
            task.setResultImmediate(result, result);
        } catch (ObjectNotFoundException  e) {
            LoggingUtils.logException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        }
        closeTask(task, result);
    }

    private void closeTask(RunningTaskQuartzImpl task, OperationResult result) {
        try {
            taskManagerImpl.closeTask(task, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot close task {}, because it does not exist in repository.", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot close task {} due to schema exception", e, task);
        } catch (SystemException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot close task {} due to system exception", e, task);
        }
    }

    private void executeSingleTask(TaskHandler handler, OperationResult executionResult) {

        Validate.notNull(handler, "Task handler is null");

        try {

            TaskRunResult runResult;

            recordCycleRunStart(executionResult, handler);
            runResult = executeHandler(handler, executionResult);        // exceptions thrown by handler are handled in executeHandler()

            // we should record finish-related information before dealing with (potential) task closure/restart
            // so we place this method call before the following block
            recordCycleRunFinish(runResult, handler, executionResult);

            // should be after recordCycleRunFinish, e.g. not to overwrite task result
            task.refresh(executionResult);

            // let us treat various exit situations here...

            if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResultStatus.INTERRUPTED) {
                // first, if a task was interrupted, we do not want to change its status
                LOGGER.trace("Task was interrupted, exiting the execution routine. Task = {}", task);
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.TEMPORARY_ERROR) {
                // in case of temporary error, we want to suspend the task and exit
                LOGGER.info("Task encountered temporary error, suspending it. Task = {}", task);
                taskManagerImpl.suspendTaskQuietly(task, TaskManager.DO_NOT_STOP, executionResult);
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.RESTART_REQUESTED) {
                // in case of RESTART_REQUESTED we have to get (new) current handler and restart it
                // this is implemented by pushHandler and by Quartz
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.PERMANENT_ERROR) {
                // PERMANENT ERROR means we do not continue executing other handlers, we just close this task
                taskManagerImpl.closeTask(task, executionResult);
                task.checkDependentTasksOnClose(executionResult);       // TODO
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED || runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED_HANDLER) {
                // FINISHED/FINISHED_HANDLER means we continue with other handlers, if there are any
                task.finishHandler(executionResult);            // this also closes the task, if there are no remaining handlers
                // if there are remaining handlers, task will be re-executed by Quartz
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.IS_WAITING) {
                LOGGER.trace("Task switched to waiting state, exiting the execution routine. Task = {}", task);
            } else {
                throw new IllegalStateException("Invalid value for Task's runResultStatus: " + runResult.getRunResultStatus() + " for task " + task);
            }

        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "An exception occurred during processing of task {}", t, task);
            //throw new JobExecutionException("An exception occurred during processing of task " + task, t);
        }
    }

    private void executeRecurrentTask(TaskHandler handler) {

        try {

mainCycle:

            while (task.canRun()) {

                // executionResult should be initialized here (inside the loop), because for long-running tightly-bound
                // recurring tasks it would otherwise bloat indefinitely
                OperationResult executionResult = createOperationResult("executeTaskRun");

                if (!task.stillCanStart()) {
                    LOGGER.trace("CycleRunner loop: task latest start time ({}) has elapsed, exiting the execution cycle. Task = {}", task.getSchedule().getLatestStartTime(), task);
                    break;
                }

                LOGGER.trace("CycleRunner loop: start");

                recordCycleRunStart(executionResult, handler);
                TaskRunResult runResult = executeHandler(handler, executionResult);
                boolean canContinue = recordCycleRunFinish(runResult, handler, executionResult);
                if (!canContinue) { // in case of task disappeared
                    break;
                }

                // let us treat various exit situations here...

                if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResultStatus.INTERRUPTED) {
                    // first, if a task was interrupted, we do not want to change its status
                    LOGGER.trace("Task was interrupted, exiting the execution cycle. Task = {}", task);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.TEMPORARY_ERROR) {
                    LOGGER.trace("Task encountered temporary error, continuing with the execution cycle. Task = {}", task);
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.RESTART_REQUESTED) {
                    // in case of RESTART_REQUESTED we have to get (new) current handler and restart it
                    // this is implemented by pushHandler and by Quartz
                    LOGGER.trace("Task returned RESTART_REQUESTED state, exiting the execution cycle. Task = {}", task);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.PERMANENT_ERROR) {
                    LOGGER.info("Task encountered permanent error, suspending the task. Task = {}", task);
                    taskManagerImpl.suspendTaskQuietly(task, TaskManager.DO_NOT_STOP, executionResult);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED) {
                    LOGGER.trace("Task handler finished, continuing with the execution cycle. Task = {}", task);
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.IS_WAITING) {
                    LOGGER.trace("Task switched to waiting state, exiting the execution cycle. Task = {}", task);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED_HANDLER) {
                    LOGGER.trace("Task handler finished with FINISHED_HANDLER, calling task.finishHandler() and exiting the execution cycle. Task = {}", task);
                    task.finishHandler(executionResult);            // this also closes the task, if there are no remaining handlers
                                                                    // if there are remaining handlers, task will be re-executed by Quartz
                    break;
                } else {
                    throw new IllegalStateException("Invalid value for Task's runResultStatus: " + runResult.getRunResultStatus() + " for task " + task);
                }

                // if the task is loosely-bound, exit the loop here
                if (task.isLooselyBound()) {
                    LOGGER.trace("CycleRunner loop: task is loosely bound, exiting the execution cycle");
                    break;
                }

                // or, was the task suspended (closed, ...) remotely?
                LOGGER.trace("CycleRunner loop: refreshing task after one iteration, task = {}", task);
                try {
                    task.refresh(executionResult);
                } catch (ObjectNotFoundException ex) {
                    LOGGER.error("Error refreshing task "+task+": Object not found: "+ex.getMessage(),ex);
                    return;            // The task object in repo is gone. Therefore this task should not run any more.
                }

                if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
                    LOGGER.info("Task not in the RUNNABLE state, exiting the execution routing. State = {}, Task = {}", task.getExecutionStatus(), task);
                    break;
                }

                // Determine how long we need to sleep and hit the bed

                Integer interval = task.getScheduleInterval();
                if (interval == null) {
                    LOGGER.error("Tightly bound task " + task + " has no scheduling interval specified.");
                    break;
                }

                long lastRunStartTime = task.getLastRunStartTimestamp() == null ? 0 : task.getLastRunStartTimestamp();
                long sleepFor = lastRunStartTime + (interval * 1000) - System.currentTimeMillis();
                if (sleepFor < 0)
                    sleepFor = 0;

                LOGGER.trace("CycleRunner loop: sleep ({})", sleepFor);

                for (long time = 0; time < sleepFor + WATCHFUL_SLEEP_INCREMENT; time += WATCHFUL_SLEEP_INCREMENT) {
                    try {
                        Thread.sleep(WATCHFUL_SLEEP_INCREMENT);
                    } catch (InterruptedException e) {
                        // safely ignored
                    }
                    if (!task.canRun()) {
                        LOGGER.trace("CycleRunner loop: sleep interrupted, task.canRun == false");
                        break mainCycle;
                    }
                }

                LOGGER.trace("CycleRunner loop: refreshing task after sleep, task = {}", task);
                try {
                    task.refresh(executionResult);
                } catch (ObjectNotFoundException ex) {
                    LOGGER.error("Error refreshing task "+task+": Object not found: "+ex.getMessage(),ex);
                    return;            // The task object in repo is gone. Therefore this task should not run any more. Therefore commit seppuku
                }

                if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
                    LOGGER.info("Task not in the RUNNABLE state, exiting the execution routine. State = {}, Task = {}", task.getExecutionStatus(), task);
                    break;
                }

                LOGGER.trace("CycleRunner loop: end");
            }

        } catch (Throwable t) {
            // This is supposed to run in a thread, so this kind of heavy artillery is needed. If throwable won't be
            // caught here, nobody will catch it and it won't even get logged.
            if (task.canRun()) {
                LOGGER.error("CycleRunner got unexpected exception: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task, t);
            } else {
                LOGGER.info("CycleRunner got unexpected exception while shutting down: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task);
                LOGGER.trace("CycleRunner got unexpected exception while shutting down: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task, t);
            }
            //throw new JobExecutionException("An exception occurred during processing of task " + task, t);
        }
    }

    private TaskRunResult executeHandler(TaskHandler handler, OperationResult executionResult) {

        if (task.getResult() == null) {
            LOGGER.warn("Task without operation result found, please check the task creation/retrieval/update code: {}", task);
            task.setResultTransient(task.createUnnamedTaskResult());
        }

        TaskRunResult runResult = taskManagerImpl.getHandlerExecutor().executeHandler(task, null, handler, executionResult);

        // It is dangerous to start waiting for transient children if they were not told to finish! Make sure you signal them
        // to stop at appropriate place.
        waitForTransientChildrenAndCloseThem(executionResult);
        return runResult;
    }

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(DOT_CLASS + methodName);
    }

    private void recordCycleRunStart(OperationResult result, TaskHandler handler) {
        LOGGER.debug("Task cycle run STARTING " + task + ", handler = " + handler);
        taskManagerImpl.notifyTaskStart(task);
        try {
            task.setLastRunStartTimestamp(System.currentTimeMillis());
            if (task.getCategory() == null) {
                task.setCategory(task.getCategoryFromHandler());
            }
            OperationResult newResult = new OperationResult("run");
            newResult.setStatus(OperationResultStatus.IN_PROGRESS);
            task.setResult(newResult);                                        // MID-4033
            task.flushPendingModifications(result);
        } catch (Exception e) {    // TODO: implement correctly after clarification
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot record run start for task {}", e, task);
        }
    }

    /*
     * Returns a flag whether to continue (false if the task has disappeared)
     */
    private boolean recordCycleRunFinish(TaskRunResult runResult, TaskHandler handler, OperationResult result) {
        LOGGER.debug("Task cycle run FINISHED {}, handler = {}", task, handler);
        taskManagerImpl.notifyTaskFinish(task, runResult);
        try {
            if (runResult.getProgress() != null) {
                task.setProgress(runResult.getProgress());
            }
            task.setLastRunFinishTimestamp(System.currentTimeMillis());
            if (runResult.getOperationResult() != null) {
                try {
                    OperationResult taskResult = runResult.getOperationResult().clone();
                    taskResult.cleanupResult();
                    taskResult.summarize(true);
//                    System.out.println("Setting task result to " + taskResult);
                    task.setResult(taskResult);
                } catch (Throwable ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Problem with task result cleanup/summarize - continuing with raw result", ex);
                    task.setResult(runResult.getOperationResult());
                }
            }
            task.storeOperationStatsDeferred();     // maybe redundant, but better twice than never at all
            task.flushPendingModifications(result);

            return true;
        } catch (ObjectNotFoundException ex) {
            LoggingUtils.logException(LOGGER, "Cannot record run finish for task {}", ex, task);
            return false;
        } catch (com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot record run finish for task {}", ex, task);
            return true;
        } catch (SchemaException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unable to record run finish and close the task: {}", ex, task);
            return true;
        }
    }

    @Override
    public void interrupt() {
        boolean interruptsAlways = taskManagerImpl.getConfiguration().getUseThreadInterrupt() == UseThreadInterrupt.ALWAYS;
        boolean interruptsMaybe = taskManagerImpl.getConfiguration().getUseThreadInterrupt() != UseThreadInterrupt.NEVER;
        if (task != null) {
            LOGGER.trace("Trying to shut down the task {}, executing in thread {}", task, task.getExecutingThread());
            task.unsetCanRun();
            for (RunningTaskQuartzImpl subtask : task.getRunningLightweightAsynchronousSubtasks()) {
                subtask.unsetCanRun();
                // if we want to cancel the Future using interrupts, we have to do it now
                // because after calling cancel(false) subsequent calls to cancel(true) have no effect whatsoever
                subtask.getLightweightHandlerFuture().cancel(interruptsMaybe);
            }
            if (interruptsAlways) {
                sendThreadInterrupt(false);         // subtasks were interrupted by their futures
            }
        }
    }

    void sendThreadInterrupt() {
        sendThreadInterrupt(true);
    }

    // beware: Do not touch task prism here, because this method can be called asynchronously
    private void sendThreadInterrupt(boolean alsoSubtasks) {
        Thread thread = task.getExecutingThread();
        if (thread != null) {            // in case this method would be (mistakenly?) called after the execution is over
            LOGGER.trace("Calling Thread.interrupt on thread {}.", thread);
            thread.interrupt();
            LOGGER.trace("Thread.interrupt was called on thread {}.", thread);
        }
        if (alsoSubtasks) {
            for (RunningTaskQuartzImpl subtask : task.getRunningLightweightAsynchronousSubtasks()) {
                //LOGGER.trace("Calling Future.cancel(mayInterruptIfRunning:=true) on a future for LAT subtask {}", subtask);
                subtask.getLightweightHandlerFuture().cancel(true);
            }
        }
    }

    public Thread getExecutingThread() {
        return task.getExecutingThread();
    }
}
