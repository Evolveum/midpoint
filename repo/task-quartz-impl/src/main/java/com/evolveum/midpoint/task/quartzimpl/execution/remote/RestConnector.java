/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.task.quartzimpl.execution.remote;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RemoteExecutionHelper;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.task.quartzimpl.execution.ExecutionManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchedulerInformationType;

import javax.ws.rs.core.Response;

/**
 * Manages remote nodes using REST.
 */
public class RestConnector {

    private static final transient Trace LOGGER = TraceManager.getTrace(RestConnector.class);

    private TaskManagerQuartzImpl taskManager;

    public RestConnector(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    public void addNodeStatus(ClusterStatusInformation info, NodeType nodeInfo, OperationResult result) {
        RemoteExecutionHelper remoteExecutionHelper = taskManager.getRemoteExecutionHelper();
        remoteExecutionHelper.execute(nodeInfo, (client, result1) -> {
            client.path("/scheduler/information");
            Response response = client.get();
            Response.StatusType statusInfo = response.getStatusInfo();
            LOGGER.debug("Querying remote scheduler information on {} finished with status {}: {}", nodeInfo.getNodeIdentifier(),
                    statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
            if (statusInfo.getFamily() == Response.Status.Family.SUCCESSFUL) {
                try {
                    SchedulerInformationType schedulerInfo = remoteExecutionHelper
                            .extractResult(response, SchedulerInformationType.class);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Received from {}:\n{}", nodeInfo.getNodeIdentifier(),
                                taskManager.getPrismContext().xmlSerializer().serializeRealValue(schedulerInfo));
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
        }, "get scheduler information", result);
    }

    private NodeType getNode(String nodeIdentifier, OperationResult result) {
        try {
            return taskManager.getClusterManager().getNodeById(nodeIdentifier, result).asObjectable();
        } catch (ObjectNotFoundException e) {
            result.recordFatalError("A node with identifier " + nodeIdentifier + " does not exist.");
            return null;
        }
    }

    public void stopRemoteScheduler(NodeType node, OperationResult result) {
        RemoteExecutionHelper remoteExecutionHelper = taskManager.getRemoteExecutionHelper();
        remoteExecutionHelper.execute(node, (client, result1) -> {
            client.path("/scheduler/stop");
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
        }, "stop scheduler", result);
    }

    public void startRemoteScheduler(NodeType node, OperationResult result) {
        RemoteExecutionHelper remoteExecutionHelper = taskManager.getRemoteExecutionHelper();
        remoteExecutionHelper.execute(node, (client, result1) -> {
            client.path("/scheduler/start");
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
        }, "start scheduler", result);
    }

    public void stopRemoteTask(String oid, NodeType node, OperationResult result) {
        RemoteExecutionHelper remoteExecutionHelper = taskManager.getRemoteExecutionHelper();
        remoteExecutionHelper.execute(node, (client, result1) -> {
            client.path("/tasks/" + oid + "/stop");
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
        }, "stop task", result);
    }

    private TaskManagerConfiguration getConfiguration() {
        return taskManager.getConfiguration();
    }

    private ExecutionManager getGlobalExecutionManager() {
        return taskManager.getExecutionManager();
    }

    private ClusterManager getClusterManager() {
        return taskManager.getClusterManager();
    }

}
