/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.execution.remote.JmxConnector;
import com.evolveum.midpoint.task.quartzimpl.execution.remote.RestConnector;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.quartz.*;

/**
 * Manages remote nodes. Concerned mainly with
 * - stopping threads and querying their state,
 * - starting/stopping scheduler and querying its state.
 *
 * @author Pavol Mederly
 */

public class RemoteNodesManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(RemoteNodesManager.class);

    public static final JobKey STARTER_JOB_KEY = JobKey.jobKey("STARTER JOB");

    private TaskManagerQuartzImpl taskManager;
    private JmxConnector jmxConnector;
    private RestConnector restConnector;

    public RemoteNodesManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
        this.jmxConnector = new JmxConnector(taskManager);
        this.restConnector = new RestConnector(taskManager);
    }

    /**
     * Used exclusively for collecting running task information.
     *
     * @param info A structure to which information should be added
     * @param node Node which to query
     */
    void addNodeStatusFromRemoteNode(ClusterStatusInformation info, PrismObject<NodeType> node, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".addNodeStatusFromRemoteNode");
        NodeType nodeInfo = node.asObjectable();
        result.addParam("node", nodeInfo.getNodeIdentifier());
        try {
            if (!taskManager.getClusterManager().isUp(nodeInfo)) {
                nodeInfo.setExecutionStatus(NodeExecutionStatusType.DOWN);
                info.addNodeInfo(nodeInfo);
                result.recordStatus(OperationResultStatus.SUCCESS, "Node is down");
                return;
            }

            if (taskManager.getConfiguration().isUseJmx()) {
                jmxConnector.addNodeStatusUsingJmx(info, nodeInfo, result);
            } else {
                restConnector.addNodeStatus(info, nodeInfo, result);
            }
            result.computeStatus();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't get status from remote node", t);
        }
    }

    private NodeType getNode(String nodeIdentifier, OperationResult result) {
        try {
            return taskManager.getClusterManager().getNodeById(nodeIdentifier, result).asObjectable();
        } catch (ObjectNotFoundException e) {
            result.recordFatalError("A node with identifier " + nodeIdentifier + " does not exist.");
            return null;
        }
    }

    public void stopRemoteScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".stopRemoteScheduler");
        result.addParam("node", nodeIdentifier);
        try {
            NodeType node = getNode(nodeIdentifier, result);
            if (node == null) {
                return;     // result is already updated
            }

            if (taskManager.getConfiguration().isUseJmx()) {
                jmxConnector.stopRemoteScheduler(node, result);
            } else {
                restConnector.stopRemoteScheduler(node, result);
            }
            result.computeStatus();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't stop scheduler on remote node", t);
            // todo log the exception?
        }
    }

    public void startRemoteScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".startRemoteScheduler");
        result.addParam("node", nodeIdentifier);
        try {
            NodeType node = getNode(nodeIdentifier, result);
            if (node == null) {
                return;     // result is already updated
            }

            if (taskManager.getConfiguration().isUseJmx()) {
                jmxConnector.startRemoteScheduler(node, result);
            } else {
                restConnector.startRemoteScheduler(node, result);
            }
            result.computeStatus();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't start scheduler on remote node", t);
            // todo log the exception?
        }
    }

    private TaskManagerConfiguration getConfiguration() {
        return taskManager.getConfiguration();
    }

    private ExecutionManager getGlobalExecutionManager() {
        return taskManager.getExecutionManager();
    }

    // the task should be really running
    void stopRemoteTaskRun(String oid, NodeType node, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".stopRemoteTaskRun");
        result.addParam("oid", oid);
        result.addParam("node", node.toString());

        LOGGER.debug("Interrupting task {} running at {}", oid, getClusterManager().dumpNodeInfo(node));
	    try {
		    if (taskManager.getConfiguration().isUseJmx()) {
			    jmxConnector.stopRemoteTaskRun(oid, node, result);
		    } else {
			    restConnector.stopRemoteTask(oid, node, result);
		    }
		    result.computeStatus();
	    } catch (Throwable t) {
		    result.recordFatalError("Couldn't stop task running on remote node", t);
		    // todo log the exception?
	    }
    }

    private ClusterManager getClusterManager() {
        return taskManager.getClusterManager();
    }

}
