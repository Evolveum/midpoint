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
package com.evolveum.midpoint.model.impl.trigger;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.util.AbstractScannerResultHandler;
import com.evolveum.midpoint.model.impl.util.AbstractScannerTaskHandler;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.GreaterFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class TriggerScannerTaskHandler extends AbstractScannerTaskHandler<ObjectType, AbstractScannerResultHandler<ObjectType>> {

	// WARNING! This task handler is efficiently singleton!
	// It is a spring bean and it is supposed to handle all search task instances
	// Therefore it must not have task-specific fields. It can only contain fields specific to
	// all tasks of a specified type
	
	public static final String HANDLER_URI = SchemaConstants.NS_MODEL + "/trigger/scanner/handler-3";
        	
	private static final transient Trace LOGGER = TraceManager.getTrace(TriggerScannerTaskHandler.class);
	
	@Autowired(required=true)
	private TriggerHandlerRegistry triggerHandlerRegistry;
	
	@Autowired(required=true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	public TriggerScannerTaskHandler() {
        super(ObjectType.class, "Trigger scan", OperationConstants.TRIGGER_SCAN);
    }

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
		
	@Override
	protected ObjectQuery createQuery(AbstractScannerResultHandler<ObjectType> handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
		ObjectQuery query = new ObjectQuery();
		ObjectFilter filter;
		PrismObjectDefinition<UserType> focusObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
		PrismContainerDefinition<TriggerType> triggerContainerDef = focusObjectDef.findContainerDefinition(ObjectType.F_TRIGGER);
		
		if (handler.getLastScanTimestamp() == null) {
			filter = LessFilter.createLess(new ItemPath(ObjectType.F_TRIGGER, TriggerType.F_TIMESTAMP), focusObjectDef, 
								handler.getThisScanTimestamp(), true);
		} else {
			filter = AndFilter.createAnd(
							GreaterFilter.createGreater(new ItemPath(ObjectType.F_TRIGGER, TriggerType.F_TIMESTAMP), focusObjectDef, 
									handler.getLastScanTimestamp(), false),
							LessFilter.createLess(new ItemPath(ObjectType.F_TRIGGER, TriggerType.F_TIMESTAMP), focusObjectDef, 
									handler.getThisScanTimestamp(), true));
		}
		
		query.setFilter(filter);
		return query;
	}
	
	@Override
	protected AbstractScannerResultHandler<ObjectType> createHandler(TaskRunResult runResult, Task coordinatorTask,
			OperationResult opResult) {
		
		AbstractScannerResultHandler<ObjectType> handler = new AbstractScannerResultHandler<ObjectType>(
				coordinatorTask, TriggerScannerTaskHandler.class.getName(), "trigger", "trigger task", taskManager) {
			@Override
			protected boolean handleObject(PrismObject<ObjectType> object, Task workerTask, OperationResult result) throws CommonException {
				fireTriggers(this, object, workerTask, result);
				return true;
			}
		};
        handler.setStopOnError(false);
        return handler;
	}

	private void fireTriggers(AbstractScannerResultHandler<ObjectType> handler, PrismObject<ObjectType> object, Task task, OperationResult result) throws SchemaException, 
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException, 
			ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismContainer<TriggerType> triggerContainer = object.findContainer(ObjectType.F_TRIGGER);
		if (triggerContainer == null) {
			LOGGER.warn("Strange thing, attempt to fire triggers on {}, but it does not have trigger container", object);
		} else {
			List<PrismContainerValue<TriggerType>> triggerCVals = triggerContainer.getValues();
			if (triggerCVals == null || triggerCVals.isEmpty()) {
				LOGGER.warn("Strange thing, attempt to fire triggers on {}, but it does not have any triggers in trigger container", object);
			} else {
				LOGGER.trace("Firing triggers for {} ({} triggers)", object, triggerCVals.size());
				for (PrismContainerValue<TriggerType> triggerCVal: triggerCVals) {
					TriggerType triggerType = triggerCVal.asContainerable();
					XMLGregorianCalendar timestamp = triggerType.getTimestamp();
					if (timestamp == null) {
						LOGGER.warn("Trigger without a timestamp in {}", object);
					} else {
						if (isHot(handler, timestamp)) {
							String handlerUri = triggerType.getHandlerUri();
							if (handlerUri == null) {
								LOGGER.warn("Trigger without handler URI in {}", object);
							} else {
								fireTrigger(handlerUri, object, task, result);
								removeTrigger(object, triggerCVal);
							}
						} else {
							LOGGER.trace("Trigger {} is not hot (timestamp={}, thisScanTimestamp={}, lastScanTimestamp={})", 
									new Object[]{triggerType, timestamp, 
									handler.getThisScanTimestamp(), handler.getLastScanTimestamp()});
						}
					}
				}
			}
		}
	}

	/**
	 * Returns true if the timestamp is in the "range of interest" for this scan run. 
	 */
	private boolean isHot(AbstractScannerResultHandler<ObjectType> handler, XMLGregorianCalendar timestamp) {
		if (handler.getThisScanTimestamp().compare(timestamp) == DatatypeConstants.LESSER) {
			return false;
		}
		return handler.getLastScanTimestamp() == null || handler.getLastScanTimestamp().compare(timestamp) == DatatypeConstants.LESSER;
	}

	private void fireTrigger(String handlerUri, PrismObject<ObjectType> object, Task task, OperationResult result) {
		LOGGER.debug("Firing trigger {} in {}", handlerUri, object);
		TriggerHandler handler = triggerHandlerRegistry.getHandler(handlerUri);
		if (handler == null) {
			LOGGER.warn("No registered trigger handler for URI {}", handlerUri);
		} else {
			try {
				
				handler.handle(object, task, result);
				
			// Properly handle everything that the handler spits out. We do not want this task to die.
			// Looks like the impossible happens and checked exceptions can somehow get here. Hence the heavy artillery below.
			} catch (Throwable e) {
				LOGGER.error("Trigger handler {} executed on {} thrown an error: {}", new Object[] { handler, object, e.getMessage(), e});
				result.recordPartialError(e);
			}
		}
	}

	private void removeTrigger(PrismObject<ObjectType> object, PrismContainerValue<TriggerType> triggerCVal) {
		PrismContainerDefinition<TriggerType> triggerContainerDef = triggerCVal.getParent().getDefinition();
		ContainerDelta<TriggerType> triggerDelta = (ContainerDelta<TriggerType>) triggerContainerDef.createEmptyDelta(new ItemPath(ObjectType.F_TRIGGER));
		triggerDelta.addValuesToDelete(triggerCVal.clone());
		Collection<? extends ItemDelta> modifications = MiscSchemaUtil.createCollection(triggerDelta);
		// This is detached result. It will not take part of the task result. We do not really care.
		OperationResult result = new OperationResult(TriggerScannerTaskHandler.class.getName()+".removeTrigger");
		try {
			repositoryService.modifyObject(object.getCompileTimeClass(), object.getOid(), modifications , result);
			result.computeStatus();
		} catch (ObjectNotFoundException e) {
			// Object is gone. Ergo there are no triggers left. Ergo the trigger was removed.
			// Ergo this is not really an error.
			LOGGER.trace("Unable to remove trigger from {}: {} (but this is probably OK)", new Object[]{object, e.getMessage(), e});
		} catch (SchemaException e) {
			LOGGER.error("Unable to remove trigger from {}: {}", new Object[]{object, e.getMessage(), e});
		} catch (ObjectAlreadyExistsException e) {
			LOGGER.error("Unable to remove trigger from {}: {}", new Object[]{object, e.getMessage(), e});
		}
	}

}
