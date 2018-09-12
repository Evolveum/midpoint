/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.impl;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.security.NodeAuthenticationToken;
import com.evolveum.midpoint.model.impl.security.RestAuthenticationMethod;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@Component
public class ClusterCacheListener implements CacheListener {
	
	private static final Trace LOGGER = TraceManager.getTrace(ClusterCacheListener.class);
	
	@Autowired private ModelService modelService;
	@Autowired private ModelInteractionService modelInteractionService;
	@Autowired private TaskManager taskManager;
	@Autowired private CacheDispatcher cacheDispatcher;
	@Autowired private PrismContext prismContext;
	
	@PostConstruct
	public void addListener() {
		cacheDispatcher.registerCacheListener(this);
	}

	@Override
	public <O extends ObjectType> void invalidateCache(Class<O> type, String oid) {
		
		if (!FunctionLibraryType.class.equals(type) && !SystemConfigurationType.class.equals(type)) {
			LOGGER.trace("Type {} not yet supported for cache clearance. Skipping.", type);
			return;
		}
		
		String nodeId = taskManager.getNodeId();
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof NodeAuthenticationToken) {
			LOGGER.trace("Skipping cluster-wide cache invalidation as this is already a remotely-invoked invalidateCache() call");
			return;
		}

		Task task = taskManager.createTaskInstance("invalidateCache");
		OperationResult result = task.getResult();
		
		SearchResultList<PrismObject<NodeType>> otherClusterNodes;
		try {
			ObjectQuery query = QueryBuilder.queryFor(NodeType.class, prismContext).not().item(NodeType.F_NODE_IDENTIFIER).eq(nodeId).build();
			otherClusterNodes = modelService.searchObjects(NodeType.class, query, null, task, result);
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException
				| ConfigurationException | ExpressionEvaluationException e) {
			LOGGER.warn("Cannot find nodes for clearing cache on them. Skipping.", e);
			return;
		}
		
		SystemConfigurationType systemConfig;
		try {
			systemConfig = modelInteractionService.getSystemConfiguration(result);
		} catch (ObjectNotFoundException | SchemaException e) {
			LOGGER.warn("Cannot load system configuration. Cannot determine the URL for REST calls without it"
					+ " (unless specified explicitly for individual nodes)");
			systemConfig = null;
		}

		for (PrismObject<NodeType> node : otherClusterNodes.getList()) {
			NodeType nodeType = node.asObjectable();

			String baseUrl;
			if (nodeType.getUrl() != null) {
				baseUrl = nodeType.getUrl();
			} else {
				String httpUrlPattern = systemConfig != null && systemConfig.getInfrastructure() != null
						? systemConfig.getInfrastructure().getIntraClusterHttpUrlPattern()
						: null;
				if (StringUtils.isBlank(httpUrlPattern)) {
					LOGGER.warn("Node URL nor intra-cluster URL pattern specified, skipping cache clearing for node {}",
							nodeType.getNodeIdentifier());
					continue;
				}
				baseUrl = httpUrlPattern.replace("$host", nodeType.getHostname());
			}

			WebClient client = WebClient.create(baseUrl + "/ws/rest");
			client.header("Authorization", RestAuthenticationMethod.CLUSTER.getMethod());// + " " + Base64Utility.encode((nodeIdentifier).getBytes()));
			client.path("/event/" + ObjectTypes.getRestTypeFromClass(type));
			Response response = client.post(null);

			LOGGER.info("Cluster-wide cache clearance finished with status {}, {}", response.getStatusInfo().getStatusCode(),
					response.getStatusInfo().getReasonPhrase());
		}
	}
}
