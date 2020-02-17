/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManagerException;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.api.UseThreadInterrupt;
import com.evolveum.midpoint.task.quartzimpl.InternalTaskInterface;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImplUtil;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DiagnosticInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchedulerInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskGroupExecutionLimitationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Manages task threads (clusterwide). Concerned mainly with stopping threads and querying their state.
 *
 * @author Pavol Mederly
 */
public class ExecutionManager {

    private static final Trace LOGGER = TraceManager.getTrace(ExecutionManager.class);

    private static final String DOT_CLASS = ExecutionManager.class.getName() + ".";

    // the following values would be (some day) part of TaskManagerConfiguration
    private static final long WAIT_FOR_COMPLETION_INITIAL = 100;            // initial waiting time (for task or tasks to be finished); it is doubled at each step
    private static final long WAIT_FOR_COMPLETION_MAX = 1600;                // max waiting time (in one step) for task(s) to be finished
    private static final long INTERRUPT_TASK_THREAD_AFTER = 5000;           // how long to wait before interrupting task thread (if UseThreadInterrupt = 'whenNecessary')

    private static final long ALLOWED_CLUSTER_STATE_INFORMATION_AGE = 1500L;

    private TaskManagerQuartzImpl taskManager;
    private LocalNodeManager localNodeManager;
    private RemoteNodesManager remoteNodesManager;
    private TaskSynchronizer taskSynchronizer;

    private Scheduler quartzScheduler;

    private ClusterStatusInformation lastClusterStatusInformation = null;

    public ExecutionManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
        this.localNodeManager = new LocalNodeManager(taskManager);
        this.remoteNodesManager = new RemoteNodesManager(taskManager);
        this.taskSynchronizer = new TaskSynchronizer(taskManager);
    }

    /*
     * ==================== NODE-LEVEL METHODS (WITH EFFECTS) ====================
     */

    public void stopScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".stopScheduler");
        result.addParam("nodeIdentifier", nodeIdentifier);
        if (isCurrentNode(nodeIdentifier)) {
            localNodeManager.stopScheduler(result);
        } else {
            remoteNodesManager.stopRemoteScheduler(nodeIdentifier, result);
        }
        if (result.isUnknown()) {
            result.computeStatus();
        }
    }

    public boolean stopSchedulersAndTasks(Collection<String> nodeIdentifiers, long timeToWait, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".stopSchedulersAndTasks");
        result.addArbitraryObjectCollectionAsParam("nodeList", nodeIdentifiers);
        result.addParam("timeToWait", timeToWait);

        LOGGER.info("Stopping schedulers and tasks on nodes: {}, waiting {} ms for task(s) shutdown.", nodeIdentifiers, timeToWait);

        for (String nodeIdentifier : nodeIdentifiers) {
            stopScheduler(nodeIdentifier, result);
        }
        ClusterStatusInformation csi = getClusterStatusInformation(true, false, result);
        Set<ClusterStatusInformation.TaskInfo> taskInfoList = csi.getTasksOnNodes(nodeIdentifiers);

        LOGGER.debug("{} task(s) found on nodes that are going down, stopping them.", taskInfoList.size());

        List<Task> tasks = new ArrayList<>();
        for (ClusterStatusInformation.TaskInfo taskInfo : taskInfoList) {
            try {
                tasks.add(taskManager.getTask(taskInfo.getOid(), result));
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Task {} that was about to be stopped does not exist. Ignoring it.", e, taskInfo.getOid());
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Task {} that was about to be stopped cannot be read due to schema problem. Ignoring it.", e, taskInfo.getOid());
            }
        }

        boolean stopped = stopTasksRunAndWait(tasks, csi, timeToWait, true, result);
        LOGGER.trace("All tasks stopped = " + stopped);
        result.recordSuccessIfUnknown();
        return stopped;
    }

    public void startScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".startScheduler");
        result.addParam("nodeIdentifier", nodeIdentifier);
        if (isCurrentNode(nodeIdentifier)) {
            localNodeManager.startScheduler(result);
        } else {
            remoteNodesManager.startRemoteScheduler(nodeIdentifier, result);
        }
    }

    public boolean isLocalNodeRunning() {
        return localNodeManager.isRunning();
    }

    /*
     * ==================== NODE-LEVEL METHODS (QUERIES) ====================
     */

    public ClusterStatusInformation getClusterStatusInformation(boolean clusterwide, boolean allowCached, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(ExecutionManager.class.getName() + ".getClusterStatusInformation");
        result.addParam("clusterwide", clusterwide);

        if (allowCached && clusterwide && lastClusterStatusInformation != null && lastClusterStatusInformation.isFresh(ALLOWED_CLUSTER_STATE_INFORMATION_AGE)) {
            result.recordSuccess();
            return lastClusterStatusInformation;
        }

        ClusterStatusInformation retval = new ClusterStatusInformation();

        if (clusterwide) {
            for (PrismObject<NodeType> node : taskManager.getClusterManager().getAllNodes(result)) {
                addNodeAndTaskInformation(retval, node, result);
            }
        } else {
            addNodeAndTaskInformation(retval, taskManager.getClusterManager().getLocalNodeObject(), result);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cluster state information = {}", retval.dump());
        }
        if (clusterwide) {
            lastClusterStatusInformation = retval;
        }

        result.recomputeStatus();
        return retval;
    }

    private void addNodeAndTaskInformation(ClusterStatusInformation info, PrismObject<NodeType> node, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(ExecutionManager.class.getName() + ".addNodeAndTaskInformation");
        result.addParam("node", node);

        if (isCurrentNode(node)) {

            LOGGER.trace("Getting node and task info from the current node ({})", node.asObjectable().getNodeIdentifier());

//            List<ClusterStatusInformation.TaskInfo> taskInfoList = new ArrayList<>();
//            Set<Task> tasks = localNodeManager.getLocallyRunningTasks(result);
//            for (Task task : tasks) {
//                taskInfoList.add(new ClusterStatusInformation.TaskInfo(task.getOid()));
//            }
//            node.asObjectable().setExecutionStatus(localNodeManager.getLocalNodeExecutionStatus());
//            node.asObjectable().setErrorStatus(taskManager.getLocalNodeErrorStatus());

            SchedulerInformationType schedulerInformation = getLocalSchedulerInformation(result);
            if (schedulerInformation.getNode() == null) {   // shouldn't occur
                schedulerInformation.setNode(node.asObjectable());
            }
            info.addNodeAndTaskInfo(schedulerInformation);

        } else {    // if remote

            LOGGER.debug("Getting running task info from remote node ({}, {})", node.asObjectable().getNodeIdentifier(), node.asObjectable().getHostname());
            remoteNodesManager.addNodeStatusFromRemoteNode(info, node, result);
        }

        result.recordSuccessIfUnknown();
    }


    /*
     * ==================== TASK-LEVEL METHODS ====================
     *
     * ---------- STOP TASK AND WAIT METHODS ----------
     */

    /**
     * Signals all running tasks that they have to finish. Waits for their completion.
     *
     * Terminology: STOP TASK means "tell the task to stop" (using any appropriate means)
     *
     * @param timeToWait How long to wait (milliseconds); 0 means forever.
     * @return true if all the tasks finished within time allotted, false otherwise.
     */
    public boolean stopAllTasksOnThisNodeAndWait(long timeToWait, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "stopAllTasksOnThisNodeAndWait");
        result.addParam("timeToWait", timeToWait);

        LOGGER.info("Stopping all tasks on local node");
        Collection<Task> tasks = localNodeManager.getLocallyRunningTasks(result);
        boolean retval = stopTasksRunAndWait(tasks, null, timeToWait, false, result);
        result.computeStatus();
        return retval;
    }

    /**
     * Stops given set of tasks and waits for their completion.
     *
     * @param tasks Tasks to stop.
     * @param csi Cluster status information. Must be relatively current, i.e. got AFTER a moment preventing new tasks
     *            to be scheduled (e.g. when suspending tasks, CSI has to be taken after tasks have been unscheduled;
     *            when stopping schedulers, CSI has to be taken after schedulers were stopped). May be null; in that case
     *            the method will query nodes themselves.
     * @param waitTime How long to wait for task stop. Value less than zero means no wait will be performed.
     * @param clusterwide If false, only tasks running on local node will be stopped.
     * @return
     *
     * Note: does not throw exceptions: it tries hard to stop the tasks, if something breaks, it just return 'false'
     */
    public boolean stopTasksRunAndWait(Collection<Task> tasks, ClusterStatusInformation csi, long waitTime, boolean clusterwide, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_CLASS + "stopTasksRunAndWait");
        result.addArbitraryObjectCollectionAsParam("tasks", tasks);
        result.addParam("waitTime", waitTime);
        result.addParam("clusterwide", clusterwide);

        if (tasks.isEmpty()) {
            result.recordSuccess();
            return true;
        }

        LOGGER.trace("Stopping tasks " + tasks + " (waiting " + waitTime + " msec); clusterwide = " + clusterwide);

        if (clusterwide && csi == null) {
            csi = getClusterStatusInformation(true, false, result);
        }

        for (Task task : tasks) {
            stopTaskRun(task, csi, clusterwide, result);
        }

        boolean stopped = false;
        if (waitTime >= 0) {
            stopped = waitForTaskRunCompletion(tasks, waitTime, clusterwide, result);
        }
        result.recordSuccessIfUnknown();
        return stopped;
    }

//    boolean stopTaskAndWait(Task task, long waitTime, boolean clusterwide) {
//        ArrayList<Task> list = new ArrayList<Task>(1);
//        list.add(task);
//        return stopTasksRunAndWait(list, waitTime, clusterwide);
//    }

    // returns true if tasks are down
    private boolean waitForTaskRunCompletion(Collection<Task> tasks, long maxWaitTime, boolean clusterwide, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(ExecutionManager.class.getName() + ".waitForTaskRunCompletion");
        result.addArbitraryObjectCollectionAsParam("tasks", tasks);
        result.addParam("maxWaitTime", maxWaitTime);
        result.addParam("clusterwide", clusterwide);

        boolean interruptExecuted = false;

        LOGGER.trace("Waiting for task(s) {} to complete, at most for {} ms.", tasks, maxWaitTime);

        Set<String> oids = new HashSet<>();
        for (Task t : tasks) {
            if (t.getOid() != null) {
                oids.add(t.getOid());
            }
        }

        long singleWait = WAIT_FOR_COMPLETION_INITIAL;
        long started = System.currentTimeMillis();

        for(;;) {

            boolean isAnythingExecuting = false;
            ClusterStatusInformation rtinfo = getClusterStatusInformation(clusterwide, false, result);
            for (String oid : oids) {
                if (rtinfo.findNodeInfoForTask(oid) != null) {
                    isAnythingExecuting = true;
                    break;
                }
            }

            if (!isAnythingExecuting) {
                String message = "The task(s), for which we have been waiting for, have finished.";
                LOGGER.trace(message);
                result.recordStatus(OperationResultStatus.SUCCESS, message);
                return true;
            }

            if (maxWaitTime > 0 && System.currentTimeMillis() - started >= maxWaitTime) {
                String message = "Wait time has elapsed without (some of) tasks being stopped. Finishing waiting for task(s) completion.";
                LOGGER.trace(message);
                result.recordWarning(message);
                return false;
            }

            if (getConfiguration().getUseThreadInterrupt() == UseThreadInterrupt.WHEN_NECESSARY && !interruptExecuted &&
                    System.currentTimeMillis() - started >= INTERRUPT_TASK_THREAD_AFTER) {

                LOGGER.info("Some tasks have not completed yet, sending their threads the 'interrupt' signal (if running locally).");
                for (String oid : oids) {
                    localNodeManager.interruptLocalTaskThread(oid);
                }
                interruptExecuted = true;
            }

            LOGGER.trace("Some tasks have not completed yet, waiting for " + singleWait + " ms (max: " + maxWaitTime + ")");
            try {
                Thread.sleep(singleWait);
            } catch (InterruptedException e) {
                LOGGER.trace("Waiting interrupted" + e);
            }

            if (singleWait < WAIT_FOR_COMPLETION_MAX) {
                singleWait *= 2;
            }
        }
    }


    // if clusterwide, csi must not be null
    // on entry we do not know if the task is really running
    private void stopTaskRun(Task task, ClusterStatusInformation csi, boolean clusterwide, OperationResult parentResult) {

        String oid = task.getOid();

        LOGGER.trace("stopTaskRun: task = {}, csi = {}, clusterwide = {}", task, csi, clusterwide);

        if (!clusterwide) {
            stopLocalTaskIfRunning(oid, parentResult);
        } else {
            NodeType node = csi.findNodeInfoForTask(task.getOid());
            if (node != null) {
                if (taskManager.getClusterManager().isCurrentNode(node.getNodeIdentifier())) {
                    stopLocalTaskIfRunning(oid, parentResult);
                } else {
                    remoteNodesManager.stopRemoteTaskRun(task.getOid(), node, parentResult);
                }
            }
        }
    }

    private void stopLocalTaskIfRunning(String oid, OperationResult parentResult) {
        if (localNodeManager.isTaskThreadActiveLocally(oid)) {
            localNodeManager.stopLocalTaskRun(oid, parentResult);
        }
    }


    /*
     * ---------- TASK SCHEDULING METHODS ----------
     */

    public void unscheduleTask(Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "unscheduleTask");
        //TriggerKey triggerKey = TaskQuartzImplUtil.createTriggerKeyForTask(task);
        JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTask(task);
        try {
            for (Trigger trigger : quartzScheduler.getTriggersOfJob(jobKey)) {
                quartzScheduler.unscheduleJob(trigger.getKey());
            }
            result.recordSuccess();
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot unschedule task {}", e, task);
            result.recordFatalError("Cannot unschedule task " + task, e);
        }
    }

    /**
     * Removes task from quartz. On error, creates a subresult in parent OperationResult. (On success, does nothing to keep ORs from becoming huge.)
     *
     * @param oid Task OID
     * @return true if the job was successfully removed.
     */
    public boolean removeTaskFromQuartz(String oid, OperationResult parentResult) {
        JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTaskOid(oid);
        try {
            quartzScheduler.deleteJob(jobKey);
            return true;
        } catch (SchedulerException e) {
            String message = "Cannot delete task " + oid + " from Quartz job store";
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            parentResult.createSubresult(DOT_CLASS + "removeTaskFromQuartz").recordFatalError(message, e);
            return false;
        }
    }


    /*
    * ==================== THREAD QUERY METHODS ====================
    */

//    boolean isTaskThreadActiveClusterwide(String oid) {
//        ClusterStatusInformation info = getClusterStatusInformation(true);
//        return info.findNodeInfoForTask(oid) != null;
//    }


    /*
     * Various auxiliary methods
     */

//    private OperationResult createOperationResult(String methodName) {
//        return new OperationResult(ExecutionManager.class.getName() + "." + methodName);
//    }
//
//    private ClusterManager getClusterManager() {
//        return taskManager.getClusterManager();
//    }

    void setQuartzScheduler(Scheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;
    }

    public Scheduler getQuartzScheduler() {
        return quartzScheduler;
    }

    private boolean isCurrentNode(String nodeIdentifier) {
        return taskManager.getClusterManager().isCurrentNode(nodeIdentifier);
    }

    private boolean isCurrentNode(PrismObject<NodeType> node) {
        return taskManager.isCurrentNode(node);
    }

    private TaskManagerConfiguration getConfiguration() {
        return taskManager.getConfiguration();
    }

    public void shutdownLocalScheduler() throws TaskManagerException {
        localNodeManager.shutdownScheduler();
    }

    /**
     *  Robust version of 'shutdownScheduler', ignores exceptions, shuts down the scheduler only if not shutdown already.
     *  Used for emergency situations, e.g. node error.
     */
    public void shutdownLocalSchedulerChecked() {

        try {
            localNodeManager.shutdownScheduler();
        } catch (TaskManagerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot shutdown scheduler.", e);
        }
    }


    public boolean stopSchedulerAndTasksLocally(long timeToWait, OperationResult result) {
        return localNodeManager.stopSchedulerAndTasks(timeToWait, result);
    }

    public void synchronizeTask(Task task, OperationResult result) {
        taskSynchronizer.synchronizeTask(task, result);
    }

    public TaskManagerQuartzImpl.NextStartTimes getNextStartTimes(@NotNull String oid, boolean retrieveNextRunStartTime,
            boolean retrieveRetryTime, OperationResult result) {
        try {
            if (retrieveNextRunStartTime && !retrieveRetryTime) {
                Trigger standardTrigger = quartzScheduler.getTrigger(TaskQuartzImplUtil.createTriggerKeyForTaskOid(oid));
                result.recordSuccess();
                return new TaskManagerQuartzImpl.NextStartTimes(standardTrigger, null);
            } else if (retrieveNextRunStartTime || retrieveRetryTime) {
                List<? extends Trigger> triggers = quartzScheduler
                        .getTriggersOfJob(TaskQuartzImplUtil.createJobKeyForTaskOid(oid));
                Trigger standardTrigger = null;
                Trigger nextRetryTrigger = null;
                for (Trigger trigger : triggers) {
                    if (oid.equals(trigger.getKey().getName())) {
                        standardTrigger = trigger;
                    } else {
                        if (willOccur(trigger) && (nextRetryTrigger == null || isBefore(trigger, nextRetryTrigger))) {
                            nextRetryTrigger = trigger;
                        }
                    }
                }
                result.recordSuccess();
                return new TaskManagerQuartzImpl.NextStartTimes(
                        retrieveNextRunStartTime ? standardTrigger : null,
                        nextRetryTrigger);        // retrieveRetryTime is always true here
            } else {
                return new TaskManagerQuartzImpl.NextStartTimes(null, null);        // shouldn't occur
            }
        } catch (SchedulerException e) {
            String message = "Cannot determine next start times for task with OID " + oid;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordFatalError(message, e);
            return null;
        }
    }

    // null means "never"
    private boolean isBefore(Trigger t1, Trigger t2) {
        Date date1 = t1.getNextFireTime();
        Date date2 = t2.getNextFireTime();
        return date1 != null
                && (date2 == null || date1.getTime() < date2.getTime());
    }

    private boolean willOccur(Trigger t) {
        return t.getNextFireTime() != null && t.getNextFireTime().getTime() >= System.currentTimeMillis();
    }

    public boolean synchronizeJobStores(OperationResult result) {
        return taskSynchronizer.synchronizeJobStores(result);
    }

    public Set<String> getLocallyRunningTasksOids(OperationResult parentResult) {
        return localNodeManager.getLocallyRunningTasksOids(parentResult);
    }

    public Collection<Task> getLocallyRunningTasks(OperationResult parentResult) {
        return localNodeManager.getLocallyRunningTasks(parentResult);
    }

    public SchedulerInformationType getLocalSchedulerInformation(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "getLocalSchedulerInformation");
        try {
            SchedulerInformationType info = new SchedulerInformationType();
            NodeType localNode = getLocalNode();
            if (localNode != null) {
                localNode.setSecret(null);
                localNode.setSecretUpdateTimestamp(null);
                localNode.setTaskExecutionLimitations(null);
            }
            info.setNode(localNode);
            for (String oid: getLocallyRunningTasksOids(result)) {
                TaskType task = new TaskType(taskManager.getPrismContext()).oid(oid);
                info.getExecutingTask().add(task);
            }
            result.computeStatus();
            return info;
        } catch (Throwable t) {
            result.recordFatalError("Couldn't get scheduler information: " + t.getMessage(), t);
            throw t;
        }
    }

    public void stopLocalScheduler(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "stopLocalScheduler");
        try {
            localNodeManager.stopScheduler(result);
            result.computeStatus();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't stop local scheduler: " + t.getMessage(), t);
            throw t;
        }
    }

    public void startLocalScheduler(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "stopLocalScheduler");
        try {
            localNodeManager.startScheduler(result);
            result.computeStatus();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't start local scheduler: " + t.getMessage(), t);
            throw t;
        }
    }

    public void stopLocalTask(String oid, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "stopLocalTask");
        try {
            localNodeManager.stopLocalTaskRun(oid, result);
            // TODO if interrupting is set to WHEN_NECESSARY we should check if the task stops within 5 seconds
            //  and if not, interrupt the thread. However, what if the task was stopped and then restarted? We
            //  should check for that situation. So let's ignore conditional interruption for now.
            result.computeStatus();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't interrupt local task: " + t.getMessage(), t);
            throw t;
        }
    }

    /**
     * @return current local node information, updated with local node execution and error status.
     * Returned value is fresh, so it can be modified as needed.
     */
    @Nullable
    public NodeType getLocalNode() {
        PrismObject<NodeType> localNode = taskManager.getClusterManager().getLocalNodeObject();
        if (localNode == null) {
            return null;
        }
        NodeType node = localNode.clone().asObjectable();
        node.setExecutionStatus(localNodeManager.getLocalNodeExecutionStatus());
        node.setErrorStatus(taskManager.getLocalNodeErrorStatus());
        return node;
    }

    public void initializeLocalScheduler() throws TaskManagerInitializationException {
        localNodeManager.initializeScheduler();
    }

    public void reRunClosedTask(Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "reRunClosedTask");

        if (task.getExecutionStatus() != TaskExecutionStatus.CLOSED) {
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
        taskSynchronizer.synchronizeTask(task, result);        // this should remove any triggers
        ((InternalTaskInterface) task).setRecreateQuartzTrigger(true);
        ((InternalTaskInterface) task).setExecutionStatusImmediate(TaskExecutionStatus.RUNNABLE, result);  // this will create the trigger
        result.recordSuccess();

        // note that if scheduling (not executes before/after) prevents the task from running, it will not run!
    }

    public void scheduleRunnableTaskNow(Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "scheduleRunnableTaskNow");

        if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
            String message = "Task " + task + " cannot be scheduled, because it is not in RUNNABLE state.";
            result.recordFatalError(message);
            LOGGER.error(message);
            return;
        }

        // for loosely-bound, recurring, interval-based tasks we reschedule the task in order to start immediately
        // and then continue after specified interval (i.e. NOT continue according to original schedule) - MID-1410
        if (!getConfiguration().isRunNowKeepsOriginalSchedule() && task.isLooselyBound() && task.isRecurring() && task.hasScheduleInterval()) {
            LOGGER.trace("'Run now' for task invoked: unscheduling and rescheduling it; task = {}", task);
            unscheduleTask(task, result);
            ((InternalTaskInterface) task).setRecreateQuartzTrigger(true);
            synchronizeTask(task, result);
        } else {
            // otherwise, we simply add another trigger to this task
            addTriggerNowForTask(task, result);
        }
        result.recordSuccessIfUnknown();
    }

    // experimental
    public void scheduleWaitingTaskNow(Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "scheduleWaitingTaskNow");

        if (task.getExecutionStatus() != TaskExecutionStatus.WAITING) {
            String message = "Task " + task + " cannot be scheduled as waiting task, because it is not in WAITING state.";
            result.recordFatalError(message);
            LOGGER.error(message);
            return;
        }

        try {
            if (!quartzScheduler.checkExists(TaskQuartzImplUtil.createJobKeyForTask(task))) {
                quartzScheduler.addJob(TaskQuartzImplUtil.createJobDetailForTask(task), false);
            }
            ((InternalTaskInterface) task).setExecutionStatusImmediate(TaskExecutionStatus.RUNNABLE, TaskExecutionStatusType.WAITING, parentResult);
        } catch (SchedulerException | ObjectNotFoundException | SchemaException | PreconditionViolationException e) {
            String message = "Waiting task " + task + " cannot be scheduled: " + e.getMessage();
            result.recordFatalError(message, e);
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            return;
        }
        addTriggerNowForTask(task, result);
        result.recordSuccessIfUnknown();
    }

    private void addTriggerNowForTask(Task task, OperationResult result) {
        Trigger now = TaskQuartzImplUtil.createTriggerNowForTask(task);
        try {
            quartzScheduler.scheduleJob(now);
        } catch (SchedulerException e) {
            String message = "Task " + task + " cannot be scheduled: " + e.getMessage();
            result.recordFatalError(message, e);
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
        }
    }

    public void pauseTaskJob(Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "pauseTaskJob");
        JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTask(task);
        TriggerKey standardTriggerKey = TaskQuartzImplUtil.createTriggerKeyForTask(task);
        try {
            for (Trigger trigger : quartzScheduler.getTriggersOfJob(jobKey)) {
                if (standardTriggerKey.equals(trigger.getKey())) {
                    LOGGER.trace("Suspending {}: pausing standard trigger {}", task, trigger);
                    quartzScheduler.pauseTrigger(trigger.getKey());
                } else {
                    LOGGER.trace("Suspending {}: deleting non-standard trigger {}", task, trigger);
                    quartzScheduler.unscheduleJob(trigger.getKey());
                }
            }
            result.recordSuccess();
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot pause job for task {}", e, task);
            result.recordFatalError("Cannot pause job for task " + task, e);
        }
    }

    public void setLocalExecutionLimitations(NodeType node) {
        setLocalExecutionLimitations(quartzScheduler, node.getTaskExecutionLimitations());
    }

    public void setLocalExecutionLimitations(TaskExecutionLimitationsType limitations) {
        setLocalExecutionLimitations(quartzScheduler, limitations);
    }

    void setLocalExecutionLimitations(Scheduler scheduler, TaskExecutionLimitationsType limitations) {
        try {
            Map<String, Integer> newLimits = new HashMap<>();
            if (limitations != null) {
                for (TaskGroupExecutionLimitationType limit : limitations.getGroupLimitation()) {
                    newLimits.put(MiscUtil.nullIfEmpty(limit.getGroupName()), limit.getLimit());
                }
            } else {
                // no limits -> everything is allowed
            }
            Map<String, Integer> oldLimits = scheduler.getExecutionLimits();    // just for the logging purposes
            scheduler.setExecutionLimits(newLimits);
            if (oldLimits == null || !new HashMap<>(oldLimits).equals(newLimits)) {
                LOGGER.info("Quartz scheduler execution limits set to: {} (were: {})", newLimits, oldLimits);
            }
        } catch (SchedulerException e) {
            // should never occur, as local scheduler shouldn't throw such exceptions
            throw new SystemException("Couldn't set local Quartz scheduler execution capabilities: " + e.getMessage(), e);
        }
    }

    public Thread getTaskThread(String oid) {
        return localNodeManager.getLocalTaskThread(oid);
    }

    public String getRunningTasksThreadsDump(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(ExecutionManager.DOT_CLASS + "getRunningTasksThreadsDump");
        try {
            Collection<Task> locallyRunningTasks = taskManager.getLocallyRunningTasks(result);
            StringBuilder output = new StringBuilder();
            for (Task task : locallyRunningTasks) {
                try {
                    output.append(getTaskThreadsDump(task.getOid(), result));
                } catch (SchemaException | ObjectNotFoundException | RuntimeException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get task thread dump for {}", e, task);
                    output.append("Couldn't get task thread dump for ").append(task).append("\n\n");
                }
            }
            result.computeStatus();
            return output.toString();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't get thread dump for running tasks: " + t.getMessage(), t);
            throw t;
        }
    }

    public String recordRunningTasksThreadsDump(String cause, OperationResult parentResult) throws ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(ExecutionManager.DOT_CLASS + "recordRunningTasksThreadsDump");
        try {
            Collection<Task> locallyRunningTasks = taskManager.getLocallyRunningTasks(result);
            StringBuilder output = new StringBuilder();
            for (Task task : locallyRunningTasks) {
                try {
                    output.append(recordTaskThreadsDump(task.getOid(), cause, result));
                } catch (SchemaException | ObjectNotFoundException | RuntimeException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get task thread dump for {}", e, task);
                    output.append("Couldn't get task thread dump for ").append(task).append("\n\n");
                }
            }
            result.computeStatus();
            return output.toString();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't record thread dump for running tasks: " + t.getMessage(), t);
            throw t;
        }
    }

    public String getTaskThreadsDump(String taskOid, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        StringBuilder output = new StringBuilder();
        OperationResult result = parentResult.createSubresult(ExecutionManager.DOT_CLASS + "getTaskThreadsDump");
        try {
            TaskQuartzImpl task = taskManager.getTask(taskOid, parentResult);
            RunningTask localTask = taskManager.getLocallyRunningTaskByIdentifier(task.getTaskIdentifier());
            Thread rootThread = taskManager.getTaskThread(taskOid);
            if (localTask == null || rootThread == null) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Task " + task + " is not running locally");
                return null;
            }
            output.append("*** Root thread for task ").append(task).append(":\n\n");
            addTaskInfo(output, localTask, rootThread);
            for (RunningTask subtask : localTask.getLightweightAsynchronousSubtasks()) {
                Thread thread = ((RunningTaskQuartzImpl) subtask).getExecutingThread();
                output.append("** Information for lightweight asynchronous subtask ").append(subtask).append(":\n\n");
                addTaskInfo(output, subtask, thread);
            }
            result.recordSuccess();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't get task threads dump: " + t.getMessage(), t);
            throw t;
        }
        return output.toString();
    }

    private void addTaskInfo(StringBuilder output, RunningTask localTask, Thread thread) {
        output.append("Execution state: ").append(localTask.getExecutionStatus()).append("\n");
        output.append("Progress: ").append(localTask.getProgress());
        if (localTask.getExpectedTotal() != null) {
            output.append(" of ").append(localTask.getExpectedTotal());
        }
        output.append("\n");
        OperationStatsType stats = localTask.getAggregatedLiveOperationStats();
        IterativeTaskInformationType info = stats != null ? stats.getIterativeTaskInformation() : null;
        if (info != null) {
            output.append("Total success count: ").append(info.getTotalSuccessCount()).append(", failure count: ").append(info.getTotalFailureCount()).append("\n");
            output.append("Current object: ").append(info.getCurrentObjectOid()).append(" ").append(info.getCurrentObjectName()).append("\n");
            if (info.getLastSuccessObjectOid() != null || info.getLastSuccessObjectName() != null) {
                output.append("Last object (success): ").append(info.getLastSuccessObjectOid())
                        .append(" (").append(info.getLastSuccessObjectName()).append(") at ")
                        .append(info.getLastSuccessEndTimestamp()).append("\n");
            }
            if (info.getLastFailureObjectOid() != null || info.getLastFailureObjectName() != null) {
                output.append("Last object (failure): ").append(info.getLastFailureObjectOid())
                        .append(" (").append(info.getLastFailureObjectName()).append(") at ")
                        .append(info.getLastFailureEndTimestamp()).append("\n");
            }
        }
        output.append("\n");
        if (thread != null) {
            output.append(MiscUtil.takeThreadDump(thread));
        } else {
            output.append("(no thread for this task)");
        }
        output.append("\n\n");
    }

    public String recordTaskThreadsDump(String taskOid, String cause, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        StringBuilder output = new StringBuilder();

        OperationResult result = parentResult.createSubresult(ExecutionManager.DOT_CLASS + "recordTaskThreadDump");
        result.addParam("taskOid", taskOid);
        result.addParam("cause", cause);
        try {
            String dump = getTaskThreadsDump(taskOid, result);
            if (dump != null) {
                LOGGER.debug("Thread dump for task {}:\n{}", taskOid, dump);
                DiagnosticInformationType event = new DiagnosticInformationType()
                        .timestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                        .type(SchemaConstants.TASK_THREAD_DUMP_URI)
                        .cause(cause)
                        .nodeIdentifier(taskManager.getNodeId())
                        .content(dump);
                taskManager.getRepositoryService().addDiagnosticInformation(TaskType.class, taskOid, event, result);
                output.append("Thread dump for task ").append(taskOid).append(" was recorded.\n");
                result.computeStatus();
            } else {
                output.append("Thread dump for task ").append(taskOid).append(" was NOT recorded as it couldn't be obtained.\n");
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Unable to get threads dump for task " +
                        taskOid + "; it is probably not running locally.");
            }
        } catch (Throwable t) {
            result.recordFatalError("Couldn't take thread dump: " + t.getMessage(), t);
            throw t;
        }
        return output.toString();
    }
}

