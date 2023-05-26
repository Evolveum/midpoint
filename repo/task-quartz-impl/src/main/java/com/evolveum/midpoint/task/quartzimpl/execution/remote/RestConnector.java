/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.execution.remote;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.ClusterExecutionOptions;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchedulerInformationType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.Response;

/**
 * Manages remote nodes using REST.
 */
@Component
public class RestConnector {

    private static final Trace LOGGER = TraceManager.getTrace(RestConnector.class);

    @Autowired private ClusterExecutionHelper clusterExecutionHelper;
    @Autowired private PrismContext prismContext;

    public void addNodeStatus(ClusterStatusInformation info, NodeType nodeInfo, OperationResult result) throws SchemaException {
        clusterExecutionHelper.execute(nodeInfo, (client, actualNode, result1) -> {
            client.path(TaskConstants.GET_LOCAL_SCHEDULER_INFORMATION_REST_PATH);
            Response response = client.get();
            Response.StatusType statusInfo = response.getStatusInfo();
            LOGGER.debug("Querying remote scheduler information on {} finished with status {}: {}", nodeInfo.getNodeIdentifier(),
                    statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
            if (statusInfo.getFamily() == Response.Status.Family.SUCCESSFUL) {
                try {
                    SchedulerInformationType schedulerInfo = clusterExecutionHelper
                            .extractResult(response, SchedulerInformationType.class);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Received from {}:\n{}", nodeInfo.getNodeIdentifier(),
                                prismContext.xmlSerializer().serializeRealValue(schedulerInfo));
                    }
                    info.addNodeAndTaskInfo(schedulerInfo);
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse scheduler information from remote node {}", e, nodeInfo.getNodeIdentifier());
                }
            } else {
                LOGGER.warn("Querying remote scheduler information on {} finished with status {}: {}", nodeInfo.getNodeIdentifier(),
                        statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
            }
            response.close();
        }, null, "get scheduler information", result);
    }

    public void stopRemoteScheduler(NodeType node, OperationResult result) throws SchemaException {
        clusterExecutionHelper.execute(node, (client, actualNode, result1) -> {
            client.path(TaskConstants.STOP_LOCAL_SCHEDULER_REST_PATH);
            Response response = client.post(null);
            Response.StatusType statusInfo = response.getStatusInfo();
            LOGGER.debug("Stopping remote scheduler on {} finished with status {}: {}", node.getNodeIdentifier(),
                    statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
            if (statusInfo.getFamily() != Response.Status.Family.SUCCESSFUL) {
                LOGGER.warn("Stopping scheduler on {} finished with status {}: {}", node.getNodeIdentifier(),
                        statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
                result1.recordFatalError("Stopping remote scheduler finished with status " + statusInfo.getStatusCode() + ": " + statusInfo.getReasonPhrase());
            }
            response.close();
        }, new ClusterExecutionOptions().tryAllNodes(), "stop scheduler", result);
    }

    public void startRemoteScheduler(NodeType node, OperationResult result) throws SchemaException {
        clusterExecutionHelper.execute(node, (client, actualNode, result1) -> {
            client.path(TaskConstants.START_LOCAL_SCHEDULER_REST_PATH);
            Response response = client.post(null);
            Response.StatusType statusInfo = response.getStatusInfo();
            LOGGER.debug("Starting remote scheduler on {} finished with status {}: {}", node.getNodeIdentifier(),
                    statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
            if (statusInfo.getFamily() != Response.Status.Family.SUCCESSFUL) {
                LOGGER.warn("Starting scheduler on {} finished with status {}: {}", node.getNodeIdentifier(),
                        statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
                result1.recordFatalError("Starting remote scheduler finished with status " + statusInfo.getStatusCode() + ": " + statusInfo.getReasonPhrase());
            }
            response.close();
        }, new ClusterExecutionOptions().tryAllNodes(), "start scheduler", result);
    }

    public void stopRemoteTaskRun(String oid, NodeType node, OperationResult result) throws SchemaException {
        clusterExecutionHelper.execute(node, (client, actualNode, result1) -> {
            client.path(TaskConstants.STOP_LOCAL_TASK_REST_PATH_PREFIX + oid + TaskConstants.STOP_LOCAL_TASK_REST_PATH_SUFFIX);
            Response response = client.post(null);
            Response.StatusType statusInfo = response.getStatusInfo();
            LOGGER.debug("Stopping task {} on {} finished with status {}: {}", oid, node.getNodeIdentifier(),
                    statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
            if (statusInfo.getFamily() != Response.Status.Family.SUCCESSFUL) {
                LOGGER.warn("Stopping task {} on {} finished with status {}: {}", oid, node.getNodeIdentifier(),
                        statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
                result1.recordFatalError("Stopping remote task finished with status " + statusInfo.getStatusCode() + ": " + statusInfo.getReasonPhrase());
            }
            response.close();
        }, new ClusterExecutionOptions().tryAllNodes(), "stop task", result);
    }
}
