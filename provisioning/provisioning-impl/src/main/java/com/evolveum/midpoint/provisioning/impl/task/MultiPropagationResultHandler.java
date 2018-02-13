/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.provisioning.impl.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class MultiPropagationResultHandler extends AbstractSearchIterativeResultHandler<ResourceType> {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(MultiPropagationResultHandler.class);
	
	private final RepositoryService repositoryService;
	private final ShadowCache shadowCache;

	public MultiPropagationResultHandler(Task coordinatorTask, String taskOperationPrefix, TaskManager taskManager, RepositoryService repositoryService, ShadowCache shadowCache) {
		super(coordinatorTask, taskOperationPrefix, "propagation", "multipropagation", taskManager);
		this.repositoryService = repositoryService;
		this.shadowCache = shadowCache;
	}

	@Override
	protected boolean handleObject(PrismObject<ResourceType> resource, Task workerTask, OperationResult taskResult)
			throws CommonException, PreconditionViolationException {
		
		LOGGER.trace("Propagating provisioning operations on {}", resource);
		ObjectQuery query = new ObjectQuery();
		ObjectFilter filter = QueryBuilder.queryFor(ShadowType.class, resource.getPrismContext())
				.item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
				.and()
				.exists(ShadowType.F_PENDING_OPERATION)
			.buildFilter();
		query.setFilter(filter);
		
		ResultHandler<ShadowType> handler = 
				(shadow, result) -> {
					propagateShadowOperations(resource, shadow, workerTask, result);
					return true;
				};
		
		repositoryService.searchObjectsIterative(ShadowType.class, query, handler, null, false, taskResult);
		
		LOGGER.trace("Propagation of {} done", resource);
		
		return true;
	}

	protected void propagateShadowOperations(PrismObject<ResourceType> resource, PrismObject<ShadowType> shadow, Task workerTask, OperationResult result) {
		try {
			shadowCache.propagateOperations(resource, shadow, workerTask, result);
		} catch (CommonException | GenericFrameworkException e) {
			throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
		}
	}
	
}
