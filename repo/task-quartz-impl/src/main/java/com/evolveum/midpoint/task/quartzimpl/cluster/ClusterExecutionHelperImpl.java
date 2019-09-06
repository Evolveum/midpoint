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
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.TerminateSessionEventType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 *  Helps with the intra-cluster remote code execution.
 */
@Component
public class ClusterExecutionHelperImpl implements ClusterExecutionHelper{

	private static final Trace LOGGER = TraceManager.getTrace(ClusterExecutionHelperImpl.class);

	@Autowired private PrismContext prismContext;
	@Autowired private TaskManager taskManager;
	@Autowired private RepositoryService repositoryService;
	@Autowired private Protector protector;

	@Autowired private MidpointXmlProvider xmlProvider;
	@Autowired private MidpointJsonProvider jsonProvider;
	@Autowired private MidpointYamlProvider yamlProvider;

	private static final String DOT_CLASS = ClusterExecutionHelperImpl.class.getName() + ".";

	@Override
	public void execute(@NotNull BiConsumer<WebClient, OperationResult> code, String context, OperationResult parentResult) {

		OperationResult result = parentResult.createSubresult(DOT_CLASS + "execute");


		if (!taskManager.isClustered()) {
			LOGGER.trace("Node is not part of a cluster, skipping remote code execution");
			result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Node not in cluster");
			return;
		}

		SearchResultList<PrismObject<NodeType>> otherClusterNodes = searchOtherClusterNodes(context, result);
		if (otherClusterNodes == null) {
			return;
		}

		for (PrismObject<NodeType> node : otherClusterNodes.getList()) {
			try {
				execute(node.asObjectable(), code, context, result);
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
			SearchResultList<PrismObject<NodeType>> otherClusterNodes = taskManager.searchObjects(NodeType.class, query, null, result);
			return otherClusterNodes;
		} catch (SchemaException e) {
			LOGGER.warn("Couldn't find nodes to execute remote operation on them ({}). Skipping it.", context, e);
			result.recordFatalError("Couldn't find nodes to execute remote operation on them (" + context + "). Skipping it.", e);
			return null;
		}
	}

	@Override
	public void execute(@NotNull String nodeOid, @NotNull BiConsumer<WebClient, OperationResult> code, String context,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		PrismObject<NodeType> node = repositoryService.getObject(NodeType.class, nodeOid, null, parentResult);
		execute(node.asObjectable(), code, context, parentResult);
	}

	@Override
	public void execute(@NotNull NodeType node, @NotNull BiConsumer<WebClient, OperationResult> code, String context, OperationResult parentResult)
			throws SchemaException {
		OperationResult result = parentResult.createSubresult(DOT_CLASS + "execute.node");
		String nodeIdentifier = node.getNodeIdentifier();
		result.addParam("node", nodeIdentifier);

		try {

			WebClient client = createClient(node, context);
			if (client == null) {
				return;
			}
			code.accept(client, result);
			result.computeStatusIfUnknown();
		} catch (SchemaException | RuntimeException t) {
			result.recordFatalError("Couldn't invoke operation (" + context + ") on node " + nodeIdentifier + ": " + t.getMessage(), t);
			throw t;
		}
	}

	private WebClient createClient(NodeType node, String context) throws SchemaException {
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
		client.accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml");
		client.type(MediaType.APPLICATION_XML);
		NodeType localNode = taskManager.getLocalNode();
		ProtectedStringType protectedSecret = localNode != null ? localNode.getSecret() : null;
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
				return prismContext.parserFor(body).parseRealValue();
			} else {
				return prismContext.parserFor(body).parseRealValue(expectedClass);
			}
		} else {
			return null;
		}
	}

}
