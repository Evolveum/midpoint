/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.common.rest.MidpointXmlProvider;
import com.evolveum.midpoint.common.rest.MidpointYamlProvider;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.RestAuthenticationMethod;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.ClusterExecutionOptions;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 *  Helps with the intra-cluster remote code execution.
 */
@Component
public class ClusterExecutionHelperImpl implements ClusterExecutionHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ClusterExecutionHelperImpl.class);

    @Autowired private PrismContext prismContext;
    @Autowired private TaskManager taskManager;
    @Autowired private RepositoryService repositoryService;
    @Autowired private Protector protector;

    @Autowired private MidpointXmlProvider<?> xmlProvider;
    @Autowired private MidpointJsonProvider<?> jsonProvider;
    @Autowired private MidpointYamlProvider<?> yamlProvider;

    private static final String DOT_CLASS = ClusterExecutionHelperImpl.class.getName() + ".";

    @Override
    public void execute(@NotNull ClientCode code, ClusterExecutionOptions options, String context, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "execute");

        if (!taskManager.isClustered()) {
            LOGGER.trace("Node is not part of a cluster, skipping remote code execution");
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Node not in cluster");
            return;
        }

        var otherClusterNodes = searchOtherClusterNodes(context, result);
        if (otherClusterNodes == null) {
            return;
        }

        for (PrismObject<NodeType> node : otherClusterNodes) {
            try {
                execute(node.asObjectable(), code, options, context, result);
            } catch (SchemaException|RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute operation ({}) on node {}", e, context, node);
            }
        }
        result.computeStatus();
    }

    private SearchResultList<PrismObject<NodeType>> searchOtherClusterNodes(String context, OperationResult result) {
        try {
            String nodeId = taskManager.getNodeId();
            ObjectQuery query = prismContext.queryFor(NodeType.class).not().item(NodeType.F_NODE_IDENTIFIER).eq(nodeId).build();
            return taskManager.searchObjects(NodeType.class, query, null, result);
        } catch (SchemaException e) {
            LOGGER.warn("Couldn't find nodes to execute remote operation on them ({}). Skipping it.", context, e);
            result.recordFatalError("Couldn't find nodes to execute remote operation on them (" + context + "). Skipping it.", e);
            return null;
        }
    }

    @Override
    public void execute(@NotNull String nodeOid, @NotNull ClientCode code, ClusterExecutionOptions options, String context,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        PrismObject<NodeType> node = repositoryService.getObject(NodeType.class, nodeOid, null, parentResult);
        execute(node.asObjectable(), code, options, context, parentResult);
    }

    @Override
    public PrismObject<NodeType> executeWithFallback(@Nullable String nodeOid, @NotNull ClientCode code,
            ClusterExecutionOptions options, String context, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "executeWithFallback");
        try {
            if (nodeOid != null) {
                PrismObject<NodeType> node = null;
                try {
                    node = repositoryService.getObject(NodeType.class, nodeOid, null, result);
                } catch (Throwable t) {
                    LOGGER.info("Couldn't get node '{}' - will try other nodes to execute '{}', if they are available: {}",
                            nodeOid, context, t.getMessage(), t);
                }
                if (node != null && tryExecute(node.asObjectable(), code, options, context, result)) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "Succeeded on suggested node");
                    return node;
                }
            }
            var otherClusterNodes = searchOtherClusterNodes(context, result);
            if (otherClusterNodes != null) {
                for (PrismObject<NodeType> otherNode : otherClusterNodes) {
                    if (nodeOid == null || !nodeOid.equals(otherNode.getOid())) {
                        if (tryExecute(otherNode.asObjectable(), code, options, context, result)) {
                            String identifier = otherNode.asObjectable().getNodeIdentifier();
                            LOGGER.info("Operation '{}' succeeded on node '{}'", context, identifier);
                            result.recordStatus(OperationResultStatus.SUCCESS, "Succeeded on " + identifier);
                            return otherNode;
                        }
                    }
                }
            }
            return null;
        } catch (Throwable t) {
            result.recordFatalError(t); // should not occur
            throw t;
        } finally {
            result.computeStatusIfUnknown(); // "UNKNOWN" should occur only if no node succeeds
        }
    }

    /**
     * @return true if successful
     */
    private boolean tryExecute(@NotNull NodeType node, @NotNull ClientCode code,
            ClusterExecutionOptions options, String context, OperationResult parentResult) {
        try {
            OperationResult executionResult = execute(node, code, options, context, parentResult);
            return executionResult.isSuccess();
        } catch (Throwable t) {
            LOGGER.info("Remote execution of '{}' failed on node '{}' (will try other nodes, if available)", context, node, t);
            return false;
        }
    }

    @Override
    public OperationResult execute(@NotNull NodeType node, @NotNull ClientCode code,
            ClusterExecutionOptions options, String context, OperationResult parentResult)
            throws SchemaException {
        OperationResult result = parentResult.createSubresult(DOT_CLASS + "execute.node");
        String nodeIdentifier = node.getNodeIdentifier();
        result.addParam("node", nodeIdentifier);
        try {
            boolean isDead = node.getOperationalState() == NodeOperationalStateType.DOWN;
            boolean isUpAndAlive = taskManager.isUpAndAlive(node);
            if (isUpAndAlive || ClusterExecutionOptions.isTryAllNodes(options) ||
                    !isDead && ClusterExecutionOptions.isTryNodesInTransition(options)) {
                try {
                    WebClient client = createClient(node, options, context);
                    if (client != null) {
                        code.execute(client, node, result);
                    } else {
                        result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Node " + nodeIdentifier +
                                " couldn't be contacted. Maybe URL is not known?"); // todo better error reporting
                    }
                } catch (SchemaException | RuntimeException t) {
                    result.recordFatalError(
                            "Couldn't invoke operation (" + context + ") on node " + nodeIdentifier + ": " + t.getMessage(), t);
                    throw t;
                }
            } else {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Node " + nodeIdentifier +
                        " is not running (operational state = " + node.getOperationalState() +
                        ", last check in time = " + node.getLastCheckInTime());
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
        return result;
    }

    private WebClient createClient(NodeType node, ClusterExecutionOptions options, String context) throws SchemaException {
        String baseUrl;
        if (node.getUrl() != null) {
            baseUrl = node.getUrl();
        } else {
            LOGGER.warn("Node URL is not known, skipping remote execution ({}) for node {}", context, node.getNodeIdentifier());
            return null;
        }

        String url = baseUrl + "/ws/cluster";
        LOGGER.debug("Going to execute '{}' on '{}'", context, url);
        WebClient client = WebClient.create(url, Arrays.asList(xmlProvider, jsonProvider, yamlProvider));
        if (!ClusterExecutionOptions.isSkipDefaultAccept(options)) {
            client.accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml");
        }
        client.type(MediaType.APPLICATION_XML);
        NodeType localNode = taskManager.getLocalNode();
        ProtectedStringType protectedSecret = localNode.getSecret();
        if (protectedSecret == null) {
            throw new SchemaException("No secret is set for local node " + localNode);
        }
        String secret;
        try {
            secret = protector.decryptString(protectedSecret);
        } catch (EncryptionException e) {
            throw new SystemException("Couldn't decrypt local node secret: " + e.getMessage(), e);
        }
        client.header("Authorization", RestAuthenticationMethod.CLUSTER.getMethod() + " " + Base64Utility.encode(secret.getBytes()));
        return client;
    }

    @Override
    public <T> T extractResult(Response response, Class<T> expectedClass) throws SchemaException {
        if (response.hasEntity()) {
            String body = response.readEntity(String.class);
            if (expectedClass == null || Object.class.equals(expectedClass)) {
                return prismContext.parserFor(body).fastAddOperations().parseRealValue();
            } else {
                return prismContext.parserFor(body).fastAddOperations().parseRealValue(expectedClass);
            }
        } else {
            return null;
        }
    }
}
