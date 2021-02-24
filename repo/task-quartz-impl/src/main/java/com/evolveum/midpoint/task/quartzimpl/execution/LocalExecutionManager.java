/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.execution;

import java.util.Collection;
import java.util.Set;

import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;

import com.evolveum.midpoint.task.quartzimpl.tasks.TaskRetriever;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.LocalNodeState;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchedulerInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Manages tasks on the local node.
 *
 * TODO decide where to put these methods
 */
@Component
public class LocalExecutionManager {

    private static final Trace LOGGER = TraceManager.getTrace(LocalExecutionManager.class);

    private static final String DOT_CLASS = LocalExecutionManager.class.getName() + ".";
    public static final String OP_STOP_ALL_TASKS_ON_THIS_NODE_AND_WAIT = DOT_CLASS + "stopAllTasksOnThisNodeAndWait";

    @Autowired private TaskStopper taskStopper;
    @Autowired private LocalScheduler localScheduler;
    @Autowired private LocalNodeState localNodeState;
    @Autowired private TaskManagerQuartzImpl taskManager;
    @Autowired private TaskRetriever taskRetriever;

    public SchedulerInformationType getLocalSchedulerInformation(OperationResult result) {
        SchedulerInformationType info = new SchedulerInformationType();
        NodeType node = localNodeState.getLocalNode();
        if (node != null) {
            node.setSecret(null);
            node.setSecretUpdateTimestamp(null);
            node.setTaskExecutionLimitations(null);
        }
        info.setNode(node);
        for (String oid : localScheduler.getLocallyRunningTasksOids(result)) {
            TaskType task = new TaskType(taskManager.getPrismContext()).oid(oid);
            info.getExecutingTask().add(task);
        }
        result.computeStatus();
        return info;
    }

    public boolean stopSchedulerAndTasks(long timeToWait, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(LocalScheduler.class.getName() + ".stopSchedulerAndTasks");
        result.addParam("timeToWait", timeToWait);
        try {
            if (localScheduler == null) {
                result.recordNotApplicable();
                return true;
            }
            localScheduler.pauseScheduler(result);
            boolean tasksStopped = stopAllTasksOnThisNodeAndWait(timeToWait, result);
            LOGGER.info("Scheduler stopped; " + (tasksStopped ? "all task threads have been stopped as well." : "some task threads may still run."));
            return tasksStopped;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Signals all running tasks that they have to finish. Waits for their completion.
     *
     * Terminology: STOP TASK means "tell the task to stop" (using any appropriate means)
     *
     * @param timeToWait How long to wait (milliseconds); 0 means forever.
     * @return true if all the tasks finished within time allotted, false otherwise.
     */
    private boolean stopAllTasksOnThisNodeAndWait(long timeToWait, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_STOP_ALL_TASKS_ON_THIS_NODE_AND_WAIT);
        result.addParam("timeToWait", timeToWait);

        try {
            LOGGER.info("Stopping all tasks on local node");
            Set<String> oids = localScheduler.getLocallyRunningTasksOids(result);
            Collection<Task> tasks = taskRetriever.resolveTaskOidsSafely(oids, result);
            return taskStopper.stopTasksRunAndWait(tasks, null, timeToWait, false, result);
        } catch (Throwable e) {
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Unexpected failure during ExecutionManager shutdown", e);
            result.recordFatalError(e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }
}
