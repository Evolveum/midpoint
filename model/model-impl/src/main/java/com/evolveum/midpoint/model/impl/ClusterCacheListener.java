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
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.InfrastructureConfigurationType;
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
		
		if (!FunctionLibraryType.class.equals(type)) {
			LOGGER.trace("Type {} not yet supported for cache clearance. Skipping.", type);
			return;
		}
		
		Task task = taskManager.createTaskInstance("invalidateCache");
		OperationResult result = task.getResult();
		
		SearchResultList<PrismObject<NodeType>> resultList;
		try {
			String nodeId = taskManager.getNodeId();
			ObjectQuery query = QueryBuilder.queryFor(NodeType.class, prismContext).item(NodeType.F_NODE_IDENTIFIER).eq(nodeId).build();
			resultList = modelService.searchObjects(NodeType.class, query, null, task, result);
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException
				| ConfigurationException | ExpressionEvaluationException e) {
			LOGGER.warn("Cannot find nodes for clearing cache on them. Skipping..");
			return;
		}
		
		SystemConfigurationType systemConfig;
		try {
			systemConfig = modelInteractionService.getSystemConfiguration(result);
		} catch (ObjectNotFoundException | SchemaException e) {
			LOGGER.warn("Cannot load system configuration. Cannot determine the url for REST calls without it.");
			return;
		}
		InfrastructureConfigurationType infraConfig = systemConfig.getInfrastructure();
		if (infraConfig == null) {
			LOGGER.warn("Cannot find infrastructure configuration, skipping cache clearing.");
			return;
		}
		
		String clusterHttpPattern = infraConfig.getIntraClusterHttpUrlPattern();
		if (StringUtils.isBlank(clusterHttpPattern)) {
			LOGGER.warn("No intra cluster http url pattern specified, skipping cache clearing");
			return;
		}
		
		for (PrismObject<NodeType> node : resultList.getList()) {
			NodeType nodeType = node.asObjectable();
			
			String ipAddress = nodeType.getNodeIdentifier();
			
			String httpPattern = clusterHttpPattern.replace("$host", nodeType.getHostname() + ":8080");
			
			WebClient client = WebClient.create(httpPattern + "/ws/rest");
			client.header("Authorization", RestAuthenticationMethod.CLUSTER + " " + Base64Utility.encode((ipAddress).getBytes()));
			
			client.path("/event/" + ObjectTypes.getRestTypeFromClass(type));
			Response response = client.post(null);

			LOGGER.info("Cluster-wide cache clearance finished with status {}, {}", response.getStatusInfo().getStatusCode(), response.getStatusInfo().getReasonPhrase());
			
		}
		
		
	}

}
