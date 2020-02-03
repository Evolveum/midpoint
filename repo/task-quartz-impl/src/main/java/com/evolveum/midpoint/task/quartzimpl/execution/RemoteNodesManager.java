/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStatusType;
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

    private static final Trace LOGGER = TraceManager.getTrace(RemoteNodesManager.class);

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
     * @param clusterInfo A structure to which information should be added
     * @param node Node which to query
     */
    void addNodeStatusFromRemoteNode(ClusterStatusInformation clusterInfo, PrismObject<NodeType> node, OperationResult parentResult) {
        NodeType nodeBean = node.asObjectable();
        OperationResult result = parentResult.createSubresult(RemoteNodesManager.class.getName() + ".addNodeStatusFromRemoteNode");
        result.addParam("node", nodeBean.getNodeIdentifier());
        try {
            if (nodeBean.getOperationalStatus() == NodeOperationalStatusType.DOWN) {
                nodeBean.setExecutionStatus(NodeExecutionStatusType.DOWN);
                clusterInfo.addNodeInfo(nodeBean);
                result.recordStatus(OperationResultStatus.SUCCESS, "Node is down");
            } else if (nodeBean.getOperationalStatus() == NodeOperationalStatusType.STARTING) {
                nodeBean.setExecutionStatus(NodeExecutionStatusType.STARTING);
                clusterInfo.addNodeInfo(nodeBean);
                result.recordStatus(OperationResultStatus.SUCCESS, "Node is starting");
            } else if (!taskManager.getClusterManager().isCheckingIn(nodeBean)) {
                nodeBean.setExecutionStatus(NodeExecutionStatusType.NOT_CHECKING_IN);
                clusterInfo.addNodeInfo(nodeBean);
                result.recordStatus(OperationResultStatus.SUCCESS, "Node is not checking in");
            } else {
                if (taskManager.getConfiguration().isUseJmx()) {
                    jmxConnector.addNodeStatusUsingJmx(clusterInfo, nodeBean, result);
                } else {
                    restConnector.addNodeStatus(clusterInfo, nodeBean, result);
                }
            }
        } catch (Throwable t) {
            result.recordFatalError("Couldn't get status from remote node", t);
        } finally {
            result.computeStatusIfUnknown();
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
