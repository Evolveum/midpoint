/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.run;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.RunningTaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskBeans;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import static com.evolveum.midpoint.task.quartzimpl.run.StopJobException.Severity.ERROR;

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
class ConcurrentExecutionChecker {

    private static final Trace LOGGER = TraceManager.getTrace(ConcurrentExecutionChecker.class);
    private final RunningTaskQuartzImpl task;
    private final TaskBeans beans;

    public ConcurrentExecutionChecker(RunningTaskQuartzImpl task, TaskBeans beans) {
        this.task = task;
        this.beans = beans;
    }

    public void check(OperationResult result) throws ObjectNotFoundException, SchemaException, StopJobException {
        task.refresh(result);
        String executingAtNode = task.getNode();
        if (executingAtNode == null) {
            LOGGER.trace("Current node is null, we assume no concurrent execution");
            return;
        }

        LOGGER.debug("Task {} seems to be executing on node {}", task, executingAtNode);
        if (executingAtNode.equals(beans.configuration.getNodeId())) {
            RunningTaskQuartzImpl locallyRunningTask = beans.localNodeState
                    .getLocallyRunningTaskByIdentifier(task.getTaskIdentifier());
            if (locallyRunningTask != null) {
                throw new StopJobException(ERROR, "Current task %s seems to be already running in thread %s on the"
                        + " local node. We will NOT start it here.", null, task, locallyRunningTask.getExecutingThread());
            } else {
                LOGGER.warn("Current task {} seemed to be already running on the local node but it cannot be found"
                        + " there now. Therefore we continue with the Quartz job execution.", task);
            }
        } else {
            ObjectQuery query = beans.prismContext.queryFor(NodeType.class)
                    .item(NodeType.F_NODE_IDENTIFIER).eq(executingAtNode)
                    .build();
            SearchResultList<PrismObject<NodeType>> nodes = beans.nodeRetriever.searchNodes(query, null, result);
            if (nodes.size() > 1) {
                throw new IllegalStateException("More than one node with identifier " + executingAtNode + ": " + nodes);
            } else if (nodes.size() == 1) {
                NodeType remoteNode = nodes.get(0).asObjectable();
                if (beans.clusterManager.isCheckingIn(remoteNode)) {
                    // We should probably contact the remote node and check if the task is really running there.
                    // But let's keep things simple for the time being.
                    throw new StopJobException(ERROR,
                            "Current task %s seems to be already running at node %s that is alive or starting. "
                                    + "We will NOT start it here.", null, task, remoteNode.getNodeIdentifier());
                } else {
                    LOGGER.warn("Current task {} seems to be already running at node {} but this node is not currently "
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
