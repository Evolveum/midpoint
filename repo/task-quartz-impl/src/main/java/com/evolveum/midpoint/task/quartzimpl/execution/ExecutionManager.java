/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.*;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.jetbrains.annotations.NotNull;
import org.quartz.*;

import java.util.*;

/**
 * Manages task threads (clusterwide). Concerned mainly with stopping threads and querying their state.
 *
 * @author Pavol Mederly
 */
public class ExecutionManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(ExecutionManager.class);

    private static final String DOT_CLASS = ExecutionManager.class.getName() + ".";

    // the following values would be (some day) part of TaskManagerConfiguration
    private static final long WAIT_FOR_COMPLETION_INITIAL = 100;			// initial waiting time (for task or tasks to be finished); it is doubled at each step
    private static final long WAIT_FOR_COMPLETION_MAX = 1600;				// max waiting time (in one step) for task(s) to be finished
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

        Set<Task> tasks = new HashSet<>();
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
            addNodeAndTaskInformation(retval, taskManager.getClusterManager().getNodePrism(), result);
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

            List<ClusterStatusInformation.TaskInfo> taskInfoList = new ArrayList<>();
            Set<Task> tasks = localNodeManager.getLocallyRunningTasks(result);
            for (Task task : tasks) {
                taskInfoList.add(new ClusterStatusInformation.TaskInfo(task.getOid()));
            }
            node.asObjectable().setExecutionStatus(localNodeManager.getLocalNodeExecutionStatus());
            node.asObjectable().setErrorStatus(taskManager.getLocalNodeErrorStatus());

            info.addNodeAndTaskInfo(node.asObjectable(), taskInfoList);

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
        Set<Task> tasks = localNodeManager.getLocallyRunningTasks(result);
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

        for (Task task : tasks)
            stopTaskRun(task, csi, clusterwide, result);

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

        LOGGER.trace("Waiting for task(s) " + tasks + " to complete, at most for " + maxWaitTime + " ms.");

        Set<String> oids = new HashSet<>();
        for (Task t : tasks)
            if (t.getOid() != null)
                oids.add(t.getOid());

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

            if (singleWait < WAIT_FOR_COMPLETION_MAX)
                singleWait *= 2;
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

    public void synchronizeTask(TaskQuartzImpl task, OperationResult result) {
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
						nextRetryTrigger);		// retrieveRetryTime is always true here
			} else {
            	return new TaskManagerQuartzImpl.NextStartTimes(null, null);		// shouldn't occur
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

    public Set<Task> getLocallyRunningTasks(OperationResult parentResult) {
        return localNodeManager.getLocallyRunningTasks(parentResult);

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
        taskSynchronizer.synchronizeTask((TaskQuartzImpl) task, result);        // this should remove any triggers
        ((TaskQuartzImpl) task).setRecreateQuartzTrigger(true);
        ((TaskQuartzImpl) task).setExecutionStatusImmediate(TaskExecutionStatus.RUNNABLE, result);  // this will create the trigger
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
        if (!getConfiguration().isRunNowKeepsOriginalSchedule() && task.isLooselyBound() && task.isCycle() && task.getSchedule() != null
                && task.getSchedule().getInterval() != null && task.getSchedule().getInterval() != 0) {
            LOGGER.trace("'Run now' for task invoked: unscheduling and rescheduling it; task = {}", task);
            unscheduleTask(task, result);
            ((TaskQuartzImpl) task).setRecreateQuartzTrigger(true);
            synchronizeTask((TaskQuartzImpl) task, result);
            result.computeStatus();
            return;
        }

        // otherwise, we simply add another trigger to this task
        Trigger now = TaskQuartzImplUtil.createTriggerNowForTask(task);
        try {
            quartzScheduler.scheduleJob(now);
            result.recordSuccess();
        } catch (SchedulerException e) {
            String message = "Task " + task + " cannot be scheduled: " + e.getMessage();
            result.recordFatalError(message, e);
            LOGGER.error(message);
        }
    }

    // nodeId should not be the current node
    void redirectTaskToNode(@NotNull Task task, @NotNull NodeType node, @NotNull OperationResult result) {
        remoteNodesManager.redirectTaskToNode(task, node, result);
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
}

