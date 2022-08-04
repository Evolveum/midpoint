/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.cluster;

import static com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation.isFresh;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.quartzimpl.execution.LocalExecutionManager;
import com.evolveum.midpoint.task.quartzimpl.execution.remote.RestConnector;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Retrieves {@link ClusterStatusInformation} from both local and remote nodes.
 *
 * TODO finish review of this class
 */
@Component
public class ClusterStatusInformationRetriever {

    private static final Trace LOGGER = TraceManager.getTrace(ClusterStatusInformationRetriever.class);

    private static final String DOT_CLASS = ClusterStatusInformationRetriever.class.getName() + ".";
    private static final String OP_GET_CLUSTER_STATUS_INFORMATION = DOT_CLASS + "getClusterStatusInformation";
    private static final String OP_ADD_NODE_AND_TASK_INFORMATION = DOT_CLASS + "addNodeAndTaskInformation";

    private final AtomicReference<ClusterStatusInformation> lastClusterStatusInformation = new AtomicReference<>();

    @Autowired private LocalExecutionManager localExecutionManager;
    @Autowired private ClusterManager clusterManager;
    @Autowired private RestConnector restConnector;

    public ClusterStatusInformation getClusterStatusInformation(boolean clusterwide, boolean allowCached,
            OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OP_GET_CLUSTER_STATUS_INFORMATION);
        result.addParam("clusterwide", clusterwide);
        result.addParam("allowCached", allowCached);
        try {

            ClusterStatusInformation lastCsi = lastClusterStatusInformation.get();
            if (allowCached && clusterwide && isFresh(lastCsi)) {
                return lastCsi;
            }

            ClusterStatusInformation csi = new ClusterStatusInformation();
            if (clusterwide) {
                for (PrismObject<NodeType> node : clusterManager.getAllNodes(result)) {
                    addNodeAndTaskInformation(csi, node, result);
                }
            } else {
                PrismObject<NodeType> localNode = clusterManager.getLocalNodeObject();
                addNodeAndTaskInformation(csi, localNode, result);
            }
            LOGGER.debug("{}", csi.debugDumpLazily());
            if (clusterwide) {
                lastClusterStatusInformation.set(csi);
            }
            return csi;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void addNodeAndTaskInformation(ClusterStatusInformation info, PrismObject<NodeType> node,
            OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OP_ADD_NODE_AND_TASK_INFORMATION);
        result.addParam("node", node);
        try {
            if (clusterManager.isCurrentNode(node)) {
                getLocalNodeAndTaskInformation(info, node, result);
            } else {
                addNodeStatusFromRemoteNode(info, node, result);
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get node/task information from {}, continuing", t, node);
            result.recordFatalError(t);
            // intentionally not rethrowing
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void getLocalNodeAndTaskInformation(ClusterStatusInformation info, PrismObject<NodeType> node, OperationResult result) {
        LOGGER.trace("Getting node and task info from the current node ({})", node.asObjectable().getNodeIdentifier());

        SchedulerInformationType schedulerInformation = localExecutionManager.getLocalSchedulerInformation(result);
        if (schedulerInformation.getNode() == null) { // shouldn't occur
            schedulerInformation.setNode(node.asObjectable());
        }
        info.addNodeAndTaskInfo(schedulerInformation);
    }

    /**
     * Used exclusively for collecting running task information.
     *
     * @param clusterInfo A structure to which information should be added
     * @param node Node which to query
     */
    private void addNodeStatusFromRemoteNode(ClusterStatusInformation clusterInfo, PrismObject<NodeType> node, OperationResult result)
            throws SchemaException {

        LOGGER.debug("Getting running task info from remote node ({}, {})", node.asObjectable().getNodeIdentifier(),
                node.asObjectable().getHostname());

        NodeType nodeBean = node.asObjectable();
        result.addParam("node", nodeBean.getNodeIdentifier());
        if (nodeBean.getOperationalState() == NodeOperationalStateType.DOWN) {
            nodeBean.setExecutionState(NodeExecutionStateType.DOWN);
            clusterInfo.addNodeInfo(nodeBean);
            result.recordStatus(OperationResultStatus.SUCCESS, "Node is down");
        } else if (nodeBean.getOperationalState() == NodeOperationalStateType.STARTING) {
            nodeBean.setExecutionState(NodeExecutionStateType.STARTING);
            clusterInfo.addNodeInfo(nodeBean);
            result.recordStatus(OperationResultStatus.SUCCESS, "Node is starting");
        } else if (!clusterManager.isCheckingIn(nodeBean)) {
            nodeBean.setExecutionState(NodeExecutionStateType.NOT_CHECKING_IN);
            clusterInfo.addNodeInfo(nodeBean);
            result.recordStatus(OperationResultStatus.SUCCESS, "Node is not checking in");
        } else {
            restConnector.addNodeStatus(clusterInfo, nodeBean, result);
        }
    }

    // TODO better place?
    @SuppressWarnings("SameParameterValue")
    public ClusterStatusInformation getClusterStatusInformation(Collection<SelectorOptions<GetOperationOptions>> options,
            Class<? extends ObjectType> objectClass, boolean allowCached, OperationResult result) {
        boolean noFetch = GetOperationOptions.isNoFetch(SelectorOptions.findRootOptions(options));
        boolean retrieveStatus;

        if (noFetch) {
            retrieveStatus = false;
        } else {
            if (objectClass.equals(TaskType.class)) {
                retrieveStatus = SelectorOptions.hasToFetchPathNotRetrievedByDefault(TaskType.F_NODE_AS_OBSERVED, options);
            } else if (objectClass.equals(NodeType.class)) {
                retrieveStatus = true; // implement some determination algorithm if needed
            } else {
                throw new IllegalArgumentException("object class: " + objectClass);
            }
        }

        if (retrieveStatus) {
            return getClusterStatusInformation(true, allowCached, result);
        } else {
            return null;
        }
    }
}
