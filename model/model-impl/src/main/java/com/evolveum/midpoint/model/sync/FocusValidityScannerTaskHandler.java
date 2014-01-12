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
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.ModelConstants;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.importer.ImportConstants;
import com.evolveum.midpoint.model.lens.ChangeExecutor;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.ContextFactory;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.util.AbstractScannerResultHandler;
import com.evolveum.midpoint.model.util.AbstractScannerTaskHandler;
import com.evolveum.midpoint.model.util.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.model.util.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.GreaterFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class FocusValidityScannerTaskHandler extends AbstractScannerTaskHandler<UserType, AbstractScannerResultHandler<UserType>> {

	// WARNING! This task handler is efficiently singleton!
	// It is a spring bean and it is supposed to handle all search task instances
	// Therefore it must not have task-specific fields. It can only contain fields specific to
	// all tasks of a specified type
	
	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/focus-validation-scanner/handler-2";

	@Autowired(required = true)
	private ProvisioningService provisioningService;
	
	@Autowired(required = true)
	private ContextFactory contextFactory;
	
    @Autowired(required = true)
    private Clockwork clockwork;
        
	private static final transient Trace LOGGER = TraceManager.getTrace(FocusValidityScannerTaskHandler.class);

	public FocusValidityScannerTaskHandler() {
        super(UserType.class, "Focus validity scan", OperationConstants.FOCUS_VALIDITY_SCAN);
    }

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	@Override
	protected ObjectQuery createQuery(AbstractScannerResultHandler<UserType> handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
		ObjectQuery query = new ObjectQuery();
		ObjectFilter filter;
		PrismObjectDefinition<UserType> focusObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
		PrismContainerDefinition<ActivationType> activationContainerDef = focusObjectDef.findContainerDefinition(FocusType.F_ACTIVATION);
		
		XMLGregorianCalendar lastScanTimestamp = handler.getLastScanTimestamp();
		XMLGregorianCalendar thisScanTimestamp = handler.getThisScanTimestamp();
		if (lastScanTimestamp == null) {
			filter = OrFilter.createOr(
						LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef, 
								thisScanTimestamp, true),
						LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef, 
								thisScanTimestamp, true));
		} else {
			filter = OrFilter.createOr(
						AndFilter.createAnd(
							GreaterFilter.createGreater(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef, 
									lastScanTimestamp, false),
							LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), focusObjectDef, 
									thisScanTimestamp, true)),
						AndFilter.createAnd(
							GreaterFilter.createGreater(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef, 
									lastScanTimestamp, false),
							LessFilter.createLess(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), focusObjectDef, 
									thisScanTimestamp, true)));			
		}
		
		query.setFilter(filter);
		return query;
	}
	
	@Override
	protected AbstractScannerResultHandler<UserType> createHandler(TaskRunResult runResult, final Task task,
			OperationResult opResult) {
		
		AbstractScannerResultHandler<UserType> handler = new AbstractScannerResultHandler<UserType>(
				task, FocusValidityScannerTaskHandler.class.getName(), "recompute", "recompute task") {
			@Override
			protected boolean handleObject(PrismObject<UserType> user, OperationResult result) throws CommonException {
				recomputeUser(user, task, result);
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
		LOGGER.trace("Recomputing of user {}: context:\n{}", user, syncContext.dump());
		clockwork.run(syncContext, task, result);
		LOGGER.trace("Recomputing of user {}: {}", user, result.getStatus());
	}
	
}
