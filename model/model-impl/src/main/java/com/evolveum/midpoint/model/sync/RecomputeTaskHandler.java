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
package com.evolveum.midpoint.model.sync;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.ModelConstants;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.importer.ImportConstants;
import com.evolveum.midpoint.model.lens.ChangeExecutor;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.ContextFactory;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.util.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.model.util.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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
public class RecomputeTaskHandler extends AbstractSearchIterativeTaskHandler<UserType> {

	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/recompute/handler-2";

    @Autowired(required=true)
	private TaskManager taskManager;
	
	@Autowired(required=true)
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
    }

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	@Override
	protected ObjectQuery createQuery(TaskRunResult runResult, Task task, OperationResult opResult) {
		// Search all objects
		return new ObjectQuery();
	}
	
	@Override
	protected AbstractSearchIterativeResultHandler<UserType> createHandler(TaskRunResult runResult, final Task task,
			OperationResult opResult) {
		
		AbstractSearchIterativeResultHandler<UserType> handler = new AbstractSearchIterativeResultHandler<UserType>(
				task, RecomputeTaskHandler.class.getName(), "recompute", "recompute task") {
			@Override
			protected boolean handleObject(PrismObject<UserType> user, OperationResult result) throws CommonException {
				recomputeUser(user, task, result);
				return true;
			}
		};
		
		return handler;
	}

	private void recomputeUser(PrismObject<UserType> user, Task task, OperationResult result) throws SchemaException, 
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException, 
			ConfigurationException, PolicyViolationException, SecurityViolationException {
		LOGGER.trace("Recomputing user {}", user);

		LensContext<UserType, ShadowType> syncContext = contextFactory.createRecomputeContext(user, task, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Recomputing of user {}: context:\n{}", user, syncContext.dump());
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
