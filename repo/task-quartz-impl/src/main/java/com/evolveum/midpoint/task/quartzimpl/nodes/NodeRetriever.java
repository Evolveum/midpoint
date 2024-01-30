/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.nodes;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformationRetriever;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class NodeRetriever {

    private static final Trace LOGGER = TraceManager.getTrace(NodeRetriever.class);

    @Autowired private ClusterStatusInformationRetriever clusterStatusInformationRetriever;
    @Autowired private RepositoryService repositoryService;
    @Autowired private TaskManagerQuartzImpl taskManager;

    /**
     * Gets nodes from repository and adds runtime information to them (taken from ClusterStatusInformation).
     */
    @NotNull public SearchResultList<PrismObject<NodeType>> searchNodes(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {

        ClusterStatusInformation csi = clusterStatusInformationRetriever
                .getClusterStatusInformation(options, NodeType.class, true, result);

        List<PrismObject<NodeType>> nodesInRepository;
        try {
            nodesInRepository = repositoryService.searchObjects(NodeType.class, query, options, result);
        } catch (SchemaException e) {
            result.recordFatalError("Couldn't get nodes from repository: " + e.getMessage());
            throw e;
        }

        List<PrismObject<NodeType>> list = new ArrayList<>();

        if (csi != null) {
            for (PrismObject<NodeType> nodeInRepositoryPrism : nodesInRepository) {
                NodeType returnedNode = nodeInRepositoryPrism.asObjectable();

                NodeType nodeRuntimeInfo = csi.findNodeById(returnedNode.getNodeIdentifier());
                if (nodeRuntimeInfo != null) {
                    returnedNode.setExecutionState(nodeRuntimeInfo.getExecutionState());
                    returnedNode.setErrorState(nodeRuntimeInfo.getErrorState());
                    returnedNode.setConnectionResult(nodeRuntimeInfo.getConnectionResult());
                } else {
                    // node is in repo, but no information on it is present in CSI
                    // (should not occur except for some temporary conditions, because CSI contains info on all nodes from repo)
                    returnedNode.setExecutionState(NodeExecutionStateType.COMMUNICATION_ERROR);
                    returnedNode.setConnectionResult(createFakeErrorOperationResult());
                }
                list.add(returnedNode.asPrismObject());
            }
        } else {
            list = nodesInRepository;
        }
        LOGGER.trace("searchNodes returning {}", list);
        return new SearchResultList<>(list);
    }

    private OperationResultType createFakeErrorOperationResult() {
        OperationResult r = new OperationResult("connect");
        if (!taskManager.isClusteringAvailable()) {
            // Special case: clustering not supported.
            // In the future, we should improve the error handling here - in general.
            r.recordFatalError("Clustering is not available (no subscription)");
        } else {
            r.recordFatalError("Node not known at this moment");
        }
        return r.createOperationResultType();
    }
}
