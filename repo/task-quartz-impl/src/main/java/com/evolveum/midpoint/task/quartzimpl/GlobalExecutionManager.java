/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

import java.util.*;

/**
 * Manages task threads (clusterwide). Concerned mainly with stopping threads and querying their state.
 *
 * @author Pavol Mederly
 */
public class GlobalExecutionManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(GlobalExecutionManager.class);

    // the following values would be (some day) part of TaskManagerConfiguration
    private static final long WAIT_FOR_COMPLETION_INITIAL = 100;			// initial waiting time (for task or tasks to be finished); it is doubled at each step
    private static final long WAIT_FOR_COMPLETION_MAX = 1600;				// max waiting time (in one step) for task(s) to be finished
    private static final long INTERRUPT_TASK_THREAD_AFTER = 5000;           // how long to wait before interrupting task thread (if UseThreadInterrupt = 'whenNecessary')

    private TaskManagerQuartzImpl taskManager;
    private Scheduler quartzScheduler;

    public GlobalExecutionManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    /*
     * ==================== NODE-LEVEL METHODS (WITH EFFECTS) ====================
     */

    public void stopScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".stopScheduler");
        result.addParam("nodeIdentifier", nodeIdentifier);
        if (isCurrentNode(nodeIdentifier)) {
            getLocalExecutionManager().stopSchedulerLocally(result);
        } else {
            getRemoteNodesManager().stopRemoteScheduler(nodeIdentifier, result);
        }
        result.recordSuccessIfUnknown();
    }


    public boolean stopSchedulerAndTasks(String nodeIdentifier, long timeToWait) {
        if (isCurrentNode(nodeIdentifier)) {
            return getLocalExecutionManager().stopSchedulerAndTasksLocally(timeToWait, new OperationResult("dummy"));      // TODO operation result
        } else {
            // TODO: use Cluster Manager
            throw new UnsupportedOperationException();
        }
    }

    public boolean stopSchedulersAndTasks(List<String> nodeList, long timeToWait, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".stopSchedulersAndTasks");
        result.addParam("nodeList", nodeList);
        result.addParam("timeToWait", timeToWait);

        LOGGER.info("Stopping schedulers and tasks on nodes: {}, waiting {} ms for task(s) shutdown.", nodeList, timeToWait);

        for (String nodeIdentifier : nodeList) {
            stopScheduler(nodeIdentifier, result);
        }
        ClusterStatusInformation csi = getClusterStatusInformation(true, result);
        Set<ClusterStatusInformation.TaskInfo> taskInfoList = csi.getTasksOnNodes(nodeList);

        LOGGER.debug("{} task(s) found on nodes that are going down, stopping them.", taskInfoList.size());

        Set<Task> tasks = new HashSet<Task>();
        for (ClusterStatusInformation.TaskInfo taskInfo : taskInfoList) {
            try {
                tasks.add(taskManager.getTask(taskInfo.getOid(), result));
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Task {} that was about to be stopped does not exist. Ignoring it.", e, taskInfo.getOid());
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Task {} that was about to be stopped cannot be read due to schema problem. Ignoring it.", e, taskInfo.getOid());
            }
        }

        boolean stopped = stopTasksAndWait(tasks, timeToWait, true, result);
        LOGGER.trace("All tasks stopped = " + stopped);
        result.recordSuccessIfUnknown();
        return stopped;
    }


    public void startScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(this.getClass().getName() + ".startScheduler");
        result.addParam("nodeIdentifier", nodeIdentifier);
        if (isCurrentNode(nodeIdentifier)) {
            getLocalExecutionManager().startSchedulerLocally(result);
        } else {
            getRemoteNodesManager().startRemoteScheduler(nodeIdentifier, result);
        }
        result.recordSuccessIfUnknown();
    }

    /*
     * ==================== NODE-LEVEL METHODS (QUERIES) ====================
     */

    ClusterStatusInformation getClusterStatusInformation(boolean clusterwide, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(GlobalExecutionManager.class.getName() + ".getClusterStatusInformation");
        result.addParam("clusterwide", clusterwide);

        ClusterStatusInformation retval = new ClusterStatusInformation();

        if (clusterwide) {
            for (PrismObject<NodeType> node : taskManager.getAllNodes(result)) {
                try {
                    addNodeAndTaskInformation(retval, node, result);
                } catch (TaskManagerException e) {
                    LoggingUtils.logException(LOGGER, "Cannot get node/task information from node {}", e, node.getName());
                }
            }
        } else {
            try {
                addNodeAndTaskInformation(retval, taskManager.getNodePrism(), result);
            } catch (TaskManagerException e) {
                LoggingUtils.logException(LOGGER, "Cannot get node/task information from local node", e);
            }
        }
        LOGGER.debug("cluster state information = " + retval.dump());

        result.recomputeStatus();
        return retval;
    }

    private void addNodeAndTaskInformation(ClusterStatusInformation info, PrismObject<NodeType> node, OperationResult parentResult) throws TaskManagerException {

        OperationResult result = parentResult.createSubresult(GlobalExecutionManager.class.getName() + ".addNodeAndTaskInformation");
        result.addParam("node", node);

        if (isCurrentNode(node)) {

            LOGGER.trace("Getting node and task info from the current node ({})", node.asObjectable().getNodeIdentifier());

            List<ClusterStatusInformation.TaskInfo> taskInfoList = new ArrayList<ClusterStatusInformation.TaskInfo>();
            Set<Task> tasks = taskManager.getLocalExecutionManager().getLocallyRunningTasks(result);
            for (Task task : tasks) {
                taskInfoList.add(new ClusterStatusInformation.TaskInfo(task.getOid()));
            }
            Node nodeInfo = new Node(node);
            nodeInfo.setNodeExecutionStatus(taskManager.getLocalNodeExecutionStatus());
            nodeInfo.setNodeErrorStatus(taskManager.getLocalNodeErrorStatus());

            info.addNodeAndTaskInfo(nodeInfo, taskInfoList);

        } else {    // if remote

            LOGGER.debug("Getting running task info from remote node ({}, {})", node.asObjectable().getNodeIdentifier(), node.asObjectable().getHostname());
            getRemoteNodesManager().addNodeStatusFromRemoteNode(info, node, result);
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
     * @throws TaskManagerException if the task list can be obtained
     */
    boolean stopAllTasksOnThisNodeAndWait(long timeToWait, OperationResult parentResult) throws TaskManagerException {
        OperationResult result = parentResult.createSubresult(GlobalExecutionManager.class.getName() + ".stopAllTasksOnThisNodeAndWait");
        result.addParam("timeToWait", timeToWait);

        LOGGER.info("Stopping all tasks on local node");
        Set<Task> tasks = getLocalExecutionManager().getLocallyRunningTasks(result);
        boolean retval = stopTasksAndWait(tasks, timeToWait, false, result);
        result.recordSuccessIfUnknown();
        return retval;
    }

    // does not throw exceptions: it tries hard to stop the tasks, if something breaks, it just return 'false'
    boolean stopTasksAndWait(Collection<Task> tasks, long waitTime, boolean clusterwide, OperationResult parentResult) {

        OperationResult result = createOperationResult(GlobalExecutionManager.class.getName() + ".stopTasksAndWait");
        result.addParam("tasks", tasks);
        result.addParam("waitTime", waitTime);
        result.addParam("clusterwide", clusterwide);

        if (tasks.isEmpty()) {
            result.recordSuccess();
            return true;
        }

        LOGGER.trace("Stopping tasks " + tasks + " (waiting " + waitTime + " msec); clusterwide = " + clusterwide);

        for (Task task : tasks)
            stopTask(task, clusterwide, result);

        boolean stopped = waitForTaskCompletion(tasks, waitTime, clusterwide, result);
        result.recordSuccessIfUnknown();
        return stopped;
    }

//    boolean stopTaskAndWait(Task task, long waitTime, boolean clusterwide) {
//        ArrayList<Task> list = new ArrayList<Task>(1);
//        list.add(task);
//        return stopTasksAndWait(list, waitTime, clusterwide);
//    }

    // returns true if tasks are down
    private boolean waitForTaskCompletion(Collection<Task> tasks, long maxWaitTime, boolean clusterwide, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(GlobalExecutionManager.class.getName() + ".waitForTaskCompletion");
        result.addParam("tasks", tasks);
        result.addParam("maxWaitTime", maxWaitTime);
        result.addParam("clusterwide", clusterwide);

        boolean interruptExecuted = false;

        LOGGER.trace("Waiting for task(s) " + tasks + " to complete, at most for " + maxWaitTime + " ms.");

        Set<String> oids = new HashSet<String>();
        for (Task t : tasks)
            if (t.getOid() != null)
                oids.add(t.getOid());

        long singleWait = WAIT_FOR_COMPLETION_INITIAL;
        long started = System.currentTimeMillis();

        for(;;) {

            boolean isAnythingExecuting = false;
            ClusterStatusInformation rtinfo = getClusterStatusInformation(clusterwide, result);
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
                    getLocalExecutionManager().interruptLocalTaskThread(oid);
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


    private void stopTask(Task task, boolean clusterwide, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(GlobalExecutionManager.class.getName() + ".stopTask");
        result.addParam("task", task);
        result.addParam("clusterwide", clusterwide);

        LOGGER.info("Stopping task " + task + "; clusterwide = " + clusterwide);
        String oid = task.getOid();

        // if the task runs locally or !clusterwide
        if (getLocalExecutionManager().isTaskThreadActiveLocally(oid) || !clusterwide) {
            getLocalExecutionManager().stopLocalTask(oid, result);
        } else {
            stopRemoteTask(task, result);
        }
    }

    private void stopRemoteTask(Task task, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(GlobalExecutionManager.class.getName() + ".stopRemoteTask");
        result.addParam("task", task);

        LOGGER.trace("Interrupting remote task {} - first finding where it currently runs", task);

        ClusterStatusInformation info = getClusterStatusInformation(true, result);
        Node node = info.findNodeInfoForTask(task.getOid());

        if (node == null) {
            LOGGER.info("Asked to interrupt task {} but did not find it running at any node.", task);
        } else {
            getRemoteNodesManager().stopRemoteTask(task.getOid(), node, result);
        }

        result.recordSuccessIfUnknown();

    }



    /*
     * ---------- TASK SCHEDULING METHODS ----------
     */

    void unscheduleTask(Task task) throws TaskManagerException {
        TriggerKey triggerKey = TaskQuartzImplUtil.createTriggerKeyForTask(task);
        try {
            quartzScheduler.unscheduleJob(triggerKey);
        } catch (SchedulerException e) {
            throw new TaskManagerException("Cannot unschedule task " + task, e);
        }
    }

    public void removeTaskFromQuartz(String oid) throws TaskManagerException {
        JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTaskOid(oid);
        try {
            quartzScheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            throw new TaskManagerException("Cannot delete task " + oid + " from Quartz job store", e);
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
     * Various Auxiliary methods
     */

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(GlobalExecutionManager.class.getName() + "." + methodName);
    }

    private ClusterManager getClusterManager() {
        return taskManager.getClusterManager();
    }

    private LocalExecutionManager getLocalExecutionManager() {
        return taskManager.getLocalExecutionManager();
    }

    private RemoteNodesManager getRemoteNodesManager() {
        return taskManager.getRemoteNodesManager();
    }

    public void setQuartzScheduler(Scheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;
    }

    Scheduler getQuartzScheduler() {
        return quartzScheduler;
    }

    private NodeRegistrar getNodeRegistrar() {
        return taskManager.getNodeRegistrar();
    }

    private boolean isCurrentNode(String nodeIdentifier) {
        return getNodeRegistrar().isCurrentNode(nodeIdentifier);
    }

    private boolean isCurrentNode(PrismObject<NodeType> node) {
        return getNodeRegistrar().isCurrentNode(node);
    }

    private TaskManagerConfiguration getConfiguration() {
        return taskManager.getConfiguration();
    }

}

