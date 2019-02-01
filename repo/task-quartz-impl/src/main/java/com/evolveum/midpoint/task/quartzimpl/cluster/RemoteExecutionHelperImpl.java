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

package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.RestAuthenticationMethod;
import com.evolveum.midpoint.task.api.RemoteExecutionHelper;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InfrastructureConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;
import java.util.function.BiConsumer;

/**
 *  Helps with the intra-cluster remote code execution.
 */
@Component
public class RemoteExecutionHelperImpl implements RemoteExecutionHelper, SystemConfigurationChangeListener {

	private static final Trace LOGGER = TraceManager.getTrace(RemoteExecutionHelperImpl.class);

	@Autowired private PrismContext prismContext;
	@Autowired private TaskManager taskManager;
	@Autowired private Protector protector;
	@Autowired private SystemConfigurationChangeDispatcher configurationChangeDispatcher;

	private static final String DOT_CLASS = RemoteExecutionHelperImpl.class.getName() + ".";

	private InfrastructureConfigurationType infrastructureConfiguration;

	@PostConstruct
	public void init() {
		configurationChangeDispatcher.registerListener(this);
	}

	@Override
	public boolean update(@Nullable SystemConfigurationType value) {
		infrastructureConfiguration = value != null ? value.getInfrastructure() : null;
		return true;
	}

	@Override
	public void execute(BiConsumer<WebClient, OperationResult> code, String context, OperationResult parentResult) {

		OperationResult result = parentResult.createSubresult(DOT_CLASS + "execute");
		String nodeId = taskManager.getNodeId();

		SearchResultList<PrismObject<NodeType>> otherClusterNodes;
		try {
			ObjectQuery query = prismContext.queryFor(NodeType.class).not().item(NodeType.F_NODE_IDENTIFIER).eq(nodeId).build();
			otherClusterNodes = taskManager.searchObjects(NodeType.class, query, null, result);
		} catch (SchemaException e) {
			LOGGER.warn("Couldn't find nodes to execute remote operation on them ({}). Skipping it.", context, e);
			result.recordFatalError("Couldn't find nodes to execute remote operation on them (" + context + "). Skipping it.", e);
			return;
		}

		for (PrismObject<NodeType> node : otherClusterNodes.getList()) {
			execute(node.asObjectable(), code, context, result);
		}
		result.computeStatus();
	}

	@Override
	public void execute(NodeType node, BiConsumer<WebClient, OperationResult> code, String context, OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(DOT_CLASS + "execute.node");
		String nodeIdentifier = node.getNodeIdentifier();
		result.addParam("node", nodeIdentifier);

		String httpUrlPattern = infrastructureConfiguration != null
				? infrastructureConfiguration.getIntraClusterHttpUrlPattern()
				: null;

		try {
			String baseUrl;
			if (node.getUrl() != null) {
				baseUrl = node.getUrl();
			} else {
				if (StringUtils.isBlank(httpUrlPattern)) {
					LOGGER.warn("Node URL nor intra-cluster URL pattern specified, skipping remote execution ({}) for node {}",
							context, node.getNodeIdentifier());
					return;
				}
				baseUrl = httpUrlPattern
						.replace("$host", node.getHostname())
						.replace("$port", String.valueOf(node.getRestPort()));
			}

			String url = baseUrl + "/ws/rest";
			LOGGER.debug("Going to execute '{}' on '{}'", context, url);
			WebClient client = WebClient.create(url);
			if (node.getSecret() == null) {
				throw new SchemaException("No secret known for target node " + node.getNodeIdentifier());
			}
			String secret = protector.decryptString(node.getSecret());
			client.header("Authorization", RestAuthenticationMethod.CLUSTER.getMethod() + " " + Base64Utility.encode(secret.getBytes()));
			code.accept(client, result);
			result.computeStatusIfUnknown();
		} catch (Throwable t) {
			result.recordFatalError("Couldn't invoke operation (" + context + ") on node " + nodeIdentifier + ": " + t.getMessage(), t);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't invoke operation ({}) on node {}", t, context, nodeIdentifier);
		}
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
