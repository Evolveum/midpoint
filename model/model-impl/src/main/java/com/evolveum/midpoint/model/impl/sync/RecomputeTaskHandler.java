/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.sync;

import java.util.List;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.lens.ChangeExecutor;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * The task hander for user recompute.
 * 
 *  This handler takes care of executing recompute "runs". The task will iterate over all users
 *  and recompute their assignments and expressions. This is needed after the expressions are changed,
 *  e.g in resource outbound expressions or in a role definition.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class RecomputeTaskHandler extends AbstractSearchIterativeTaskHandler<UserType, AbstractSearchIterativeResultHandler<UserType>> {

	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/recompute/handler-3";

    @Autowired(required=true)
	private TaskManager taskManager;
	
	@Autowired(required=true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	
	@Autowired(required=true)
	private PrismContext prismContext;

    @Autowired(required = true)
    private ProvisioningService provisioningService;

    @Autowired(required = true)
    private ContextFactory contextFactory;
    
    @Autowired(required = true)
    private Clockwork clockwork;
    
    @Autowired
    private ChangeExecutor changeExecutor;
    	
	private static final transient Trace LOGGER = TraceManager.getTrace(RecomputeTaskHandler.class);
	
	public RecomputeTaskHandler() {
        super(UserType.class, "Recompute users", OperationConstants.RECOMPUTE);
		setLogFinishInfo(true);
    }

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	@Override
	protected ObjectQuery createQuery(AbstractSearchIterativeResultHandler<UserType> handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
        QueryType queryFromTask = getObjectQueryTypeFromTask(task);
        if (queryFromTask != null) {
            ObjectQuery query = QueryJaxbConvertor.createObjectQuery(UserType.class, queryFromTask, prismContext);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Using object query from the task: {}", query.debugDump());
            }
            return query;
        } else {
		    // Search all objects
		    return new ObjectQuery();
        }
	}
	
	@Override
	protected AbstractSearchIterativeResultHandler<UserType> createHandler(TaskRunResult runResult, final Task coordinatorTask,
			OperationResult opResult) {
		
		AbstractSearchIterativeResultHandler<UserType> handler = new AbstractSearchIterativeResultHandler<UserType>(
				coordinatorTask, RecomputeTaskHandler.class.getName(), "recompute", "recompute task", taskManager) {
			@Override
			protected boolean handleObject(PrismObject<UserType> user, Task workerTask, OperationResult result) throws CommonException {
				recomputeUser(user, workerTask, result);
				return true;
			}
		};
        handler.setStopOnError(false);
        return handler;
	}

	private void recomputeUser(PrismObject<UserType> user, Task task, OperationResult result) throws SchemaException, 
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException, 
			ConfigurationException, PolicyViolationException, SecurityViolationException {
		LOGGER.trace("Recomputing user {}", user);

		LensContext<UserType> syncContext = contextFactory.createRecomputeContext(user, task, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recomputing of user {}: context:\n{}", user, syncContext.debugDump());
		}
		clockwork.run(syncContext, task, result);
		LOGGER.trace("Recomputing of user {}: {}", user, result.getStatus());
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.USER_RECOMPUTATION;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
