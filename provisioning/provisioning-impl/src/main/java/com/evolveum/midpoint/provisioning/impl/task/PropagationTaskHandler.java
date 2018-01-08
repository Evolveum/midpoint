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

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.impl.ShadowCacheFactory;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
@Component
public class PropagationTaskHandler extends AbstractSearchIterativeTaskHandler<ShadowType, PropagationResultHandler> {
	
	public static final String HANDLER_URI = SchemaConstants.NS_PROVISIONING_TASK + "/propagation/handler-3";
	
	// WARNING! This task handler is efficiently singleton!
 	// It is a spring bean and it is supposed to handle all search task instances
 	// Therefore it must not have task-specific fields. It can only contain fields specific to
 	// all tasks of a specified type

    @Autowired private TaskManager taskManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private ShadowCacheFactory shadowCacheFactory;
    
    private static final Trace LOGGER = TraceManager.getTrace(PropagationTaskHandler.class);
    
    public PropagationTaskHandler() {
        super("Provisioning propagation", OperationConstants.PROVISIONING_PROPAGATION);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
        setEnableSynchronizationStatistics(false);
    }
    
    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }
    
    @Override
	protected PropagationResultHandler createHandler(TaskRunResult runResult, Task coordinatorTask,
			OperationResult opResult) {
    	
    	String resourceOid = coordinatorTask.getObjectOid();
    	PrismObject<ResourceType> resource;
    	try {
			resource = provisioningService.getObject(ResourceType.class, resourceOid, null, coordinatorTask, opResult);
		} catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException
				| SecurityViolationException | ExpressionEvaluationException e) {
			opResult.recordFatalError("Error resolving resource oid=" + resourceOid, e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		}
    	ShadowCache shadowCache = shadowCacheFactory.getShadowCache(ShadowCacheFactory.Mode.STANDARD);
    	PropagationResultHandler handler = new PropagationResultHandler(coordinatorTask, getTaskOperationPrefix(), taskManager, shadowCache, resource);
    	return handler;
    }

	@Override
	public String getCategoryName(Task task) {
		return TaskCategory.SYSTEM;
	}

	@Override
	public List<String> getCategoryNames() {
		return null;
	}

	@Override
	protected ObjectQuery createQuery(PropagationResultHandler handler, TaskRunResult runResult, Task task,
			OperationResult opResult) throws SchemaException {
		ObjectQuery query = new ObjectQuery();
		ObjectFilter filter = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(handler.getResource().getOid())
				.and()
				.exists(ShadowType.F_PENDING_OPERATION)
			.buildFilter();
//		ObjectFilter filter = QueryBuilder.queryFor(ShadowType.class, prismContext)
//				.exists(ShadowType.F_PENDING_OPERATION)
//			.buildFilter();

		query.setFilter(filter);
		return query;
	}

	@Override
	protected Class<? extends ObjectType> getType(Task task) {
		return ShadowType.class;
	}

}
