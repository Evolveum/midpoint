package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.UseThreadInterrupt;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformationRetriever;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.task.quartzimpl.quartz.TaskSynchronizer;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Takes care of task stop procedure.
 */
@Component
public class TaskStopper {

    private static final Trace LOGGER = TraceManager.getTrace(TaskStopper.class);

    private static final String DOT_CLASS = TaskStopper.class.getName() + ".";
    public static final String OP_STOP_TASKS_RUN_AND_WAIT = DOT_CLASS + "stopTasksRunAndWait";
    public static final String OP_WAIT_FOR_TASK_RUN_COMPLETION = DOT_CLASS + "waitForTaskRunCompletion";

    @Autowired private TaskManagerQuartzImpl taskManager;
    // the following values would be (some day) part of TaskManagerConfiguration
    private static final long WAIT_FOR_COMPLETION_INITIAL = 100;            // initial waiting time (for task or tasks to be finished); it is doubled at each step
    private static final long WAIT_FOR_COMPLETION_MAX = 1600;                // max waiting time (in one step) for task(s) to be finished
    private static final long INTERRUPT_TASK_THREAD_AFTER = 5000;           // how long to wait before interrupting task thread (if UseThreadInterrupt = 'whenNecessary')

    @Autowired private LocalScheduler localScheduler;
    @Autowired private RemoteSchedulers remoteSchedulers;
    @Autowired private ClusterStatusInformationRetriever clusterStatusInformationRetriever;
    @Autowired private TaskManagerConfiguration configuration;

    /**
     * Stops given set of tasks and waits for their completion.
     *
     * @param tasks Tasks to stop.
     * @param csi Cluster status information. Must be relatively current, i.e. got AFTER a moment preventing new tasks
     * to be scheduled (e.g. when suspending tasks, CSI has to be taken after tasks have been unscheduled;
     * when stopping schedulers, CSI has to be taken after schedulers were stopped). May be null; in that case
     * the method will query nodes themselves.
     * @param waitTime How long to wait for task stop. Value less than zero means no wait will be performed.
     * @param clusterwide If false, only tasks running on local node will be stopped.
     * @return Note: does not throw exceptions: it tries hard to stop the tasks, if something breaks, it just return 'false'
     */
    public boolean stopTasksRunAndWait(Collection<? extends Task> tasks, ClusterStatusInformation csi, long waitTime, boolean clusterwide,
            OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OP_STOP_TASKS_RUN_AND_WAIT);
        result.addArbitraryObjectCollectionAsParam("tasks", tasks);
        result.addParam("waitTime", waitTime);
        result.addParam("clusterwide", clusterwide);
        try {

            if (tasks.isEmpty()) {
                return true;
            }

            LOGGER.trace("Stopping tasks {} (waiting {} msec); clusterwide = {}", tasks, waitTime, clusterwide);

            if (clusterwide && csi == null) {
                try {
                    csi = clusterStatusInformationRetriever.getClusterStatusInformation(true, false, result);
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get cluster state information, continuing", e);
                }
            }

            for (Task task : tasks) {
                try {
                    stopTaskRun(task, csi, clusterwide, result);
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't request task run stop for {}, continuing", e, task);
                }
            }

            boolean stopped;
            if (waitTime >= 0) {
                stopped = waitForTaskRunCompletion(tasks, waitTime, clusterwide, result);
            } else {
                stopped = false;
            }
            result.recordSuccessIfUnknown();
            return stopped;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public boolean stopAllTasksOnNodes(Collection<String> nodeIdentifiers, long timeToWait, OperationResult result) throws SchemaException {
        ClusterStatusInformation csi = clusterStatusInformationRetriever.getClusterStatusInformation(true, false, result);
        Set<ClusterStatusInformation.TaskInfo> taskInfoList = csi.getTasksOnNodes(nodeIdentifiers);
        LOGGER.debug("{} task(s) found on nodes that are going down, stopping them.", taskInfoList.size());

        List<Task> tasks = getTasksSafely(taskInfoList, result);
        return stopTasksRunAndWait(tasks, csi, timeToWait, true, result);
    }


    @NotNull
    private List<Task> getTasksSafely(Set<ClusterStatusInformation.TaskInfo> taskInfoList, OperationResult result) {
        List<Task> tasks = new ArrayList<>();
        for (ClusterStatusInformation.TaskInfo taskInfo : taskInfoList) {
            try {
                tasks.add(taskManager.getTaskPlain(taskInfo.getOid(), result));
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Task {} that was about to be stopped does not exist. Ignoring it.", e,
                        taskInfo.getOid());
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Task {} that was about to be stopped cannot be read. Ignoring it.",
                        e, taskInfo.getOid());
            }
        }
        return tasks;
    }

    // returns true if tasks are down
    private boolean waitForTaskRunCompletion(Collection<? extends Task> tasks, long maxWaitTime, boolean clusterwide, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OP_WAIT_FOR_TASK_RUN_COMPLETION);
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

        while (true) {
            boolean isAnythingExecuting = false;
            ClusterStatusInformation rtinfo = clusterStatusInformationRetriever.getClusterStatusInformation(clusterwide, false, result);
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

            if (configuration.getUseThreadInterrupt() == UseThreadInterrupt.WHEN_NECESSARY && !interruptExecuted &&
                    System.currentTimeMillis() - started >= INTERRUPT_TASK_THREAD_AFTER) {

                LOGGER.info("Some tasks have not completed yet, sending their threads the 'interrupt' signal (if running locally).");
                for (String oid : oids) {
                    localScheduler.stopLocalTaskRunByInterrupt(oid);
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
                    remoteSchedulers.stopRemoteTaskRun(task.getOid(), node, parentResult);
                }
            }
        }
    }

    private void stopLocalTaskIfRunning(String oid, OperationResult parentResult) {
        if (localScheduler.isTaskThreadActiveLocally(oid)) {
            localScheduler.stopLocalTaskRunInStandardWay(oid, parentResult);
        }
    }

}
