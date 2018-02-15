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
 * Task handler for provisioning propagation of many resources.
 * 
 * The search in this task handler is somehow reversed. The task is searching for resources
 * and then the task internally looks for pending changes.
 * 
 * Here we assume that there will be large number of resources, but there will be much smaller
 * number of changes.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class MultiPropagationTaskHandler extends AbstractSearchIterativeTaskHandler<ResourceType, MultiPropagationResultHandler> {
	
	public static final String HANDLER_URI = SchemaConstants.NS_PROVISIONING_TASK + "/propagation/multi-handler-3";
	
	// WARNING! This task handler is efficiently singleton!
 	// It is a spring bean and it is supposed to handle all search task instances
 	// Therefore it must not have task-specific fields. It can only contain fields specific to
 	// all tasks of a specified type

    @Autowired private TaskManager taskManager;
    @Autowired private ShadowCacheFactory shadowCacheFactory;
    
    private static final Trace LOGGER = TraceManager.getTrace(MultiPropagationTaskHandler.class);
    
    public MultiPropagationTaskHandler() {
        super("Provisioning propagation (multi)", OperationConstants.PROVISIONING_PROPAGATION);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
        setEnableSynchronizationStatistics(false);
    }
    
    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }
    
    @Override
	protected MultiPropagationResultHandler createHandler(TaskRunResult runResult, Task coordinatorTask,
			OperationResult opResult) {
    	
    	ShadowCache shadowCache = shadowCacheFactory.getShadowCache(ShadowCacheFactory.Mode.STANDARD);
    	MultiPropagationResultHandler handler = new MultiPropagationResultHandler(coordinatorTask, getTaskOperationPrefix(), taskManager, repositoryService, shadowCache);
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
	protected ObjectQuery createQuery(MultiPropagationResultHandler handler, TaskRunResult runResult, Task coordinatorTask,
			OperationResult opResult) throws SchemaException {
		ObjectQuery objectQuery = createQueryFromTask(handler, runResult, coordinatorTask, opResult);
		LOGGER.trace("Resource query: {}", objectQuery);
		return objectQuery;
		
	}

	@Override
	protected Class<? extends ObjectType> getType(Task task) {
		return ResourceType.class;
	}

}
