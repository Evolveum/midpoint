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
package com.evolveum.midpoint.model.trigger;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.util.AbstractScannerTaskHandler;
import com.evolveum.midpoint.model.util.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.prism.Containerable;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class TriggerScannerTaskHandler extends AbstractScannerTaskHandler<ObjectType> {

	public static final String HANDLER_URI = SchemaConstants.NS_MODEL + "/trigger/scanner/handler-2";
        	
	private static final transient Trace LOGGER = TraceManager.getTrace(TriggerScannerTaskHandler.class);
	
	@Autowired(required=true)
	private TriggerHandlerRegistry triggerHandlerRegistry;
	
	@Autowired(required=true)
	private RepositoryService repositoryService;

	public TriggerScannerTaskHandler() {
        super(ObjectType.class, "Trigger scan", OperationConstants.TRIGGER_SCAN);
    }

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
		
	@Override
	protected ObjectQuery createQuery(TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
		ObjectQuery query = new ObjectQuery();
		ObjectFilter filter;
		PrismObjectDefinition<UserType> focusObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
		PrismContainerDefinition<TriggerType> triggerContainerDef = focusObjectDef.findContainerDefinition(ObjectType.F_TRIGGER);
		
		if (lastScanTimestamp == null) {
			filter = LessFilter.createLessFilter(new ItemPath(ObjectType.F_TRIGGER), triggerContainerDef, 
								TriggerType.F_TIMESTAMP, thisScanTimestamp, true);
		} else {
			filter = AndFilter.createAnd(
							GreaterFilter.createGreaterFilter(new ItemPath(ObjectType.F_TRIGGER), triggerContainerDef, 
									TriggerType.F_TIMESTAMP, lastScanTimestamp, false),
							LessFilter.createLessFilter(new ItemPath(ObjectType.F_TRIGGER), triggerContainerDef, 
									TriggerType.F_TIMESTAMP, thisScanTimestamp, true));
		}
		
		query.setFilter(filter);
		return query;
	}
	
	@Override
	protected AbstractSearchIterativeResultHandler<ObjectType> createHandler(TaskRunResult runResult, final Task task,
			OperationResult opResult) {
		
		AbstractSearchIterativeResultHandler<ObjectType> handler = new AbstractSearchIterativeResultHandler<ObjectType>(
				task, TriggerScannerTaskHandler.class.getName(), "trigger", "trigger task") {
			@Override
			protected boolean handleObject(PrismObject<ObjectType> object, OperationResult result) throws CommonException {
				fireTriggers(object, task, result);
				return true;
			}
		};
		
		return handler;
	}

	private void fireTriggers(PrismObject<ObjectType> object, Task task, OperationResult result) throws SchemaException, 
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException, 
			ConfigurationException, PolicyViolationException, SecurityViolationException {
		LOGGER.trace("Firing triggers for {}", object);

		PrismContainer<TriggerType> triggerContainer = object.findContainer(ObjectType.F_TRIGGER);
		if (triggerContainer == null) {
			LOGGER.warn("Strange thing, attempt to fire triggers on {}, but it does not have trigger container", object);
		} else {
			for (PrismContainerValue<TriggerType> triggerCVal: triggerContainer.getValues()) {
				TriggerType triggerType = triggerCVal.asContainerable();
				XMLGregorianCalendar timestamp = triggerType.getTimestamp();
				if (timestamp == null) {
					LOGGER.warn("Trigger without a timestamp in {}", object);
				} else {
					if (isHot(timestamp)) {
						String handlerUri = triggerType.getHandlerUri();
						if (handlerUri == null) {
							LOGGER.warn("Trigger without handler URI in {}", object);
						} else {
							fireTrigger(handlerUri, object);
							removeTrigger(object, triggerCVal);
						}
					}
				}
			}
		}
	}

	/**
	 * Returns true if the timestamp is in the "range of interest" for this scan run. 
	 */
	private boolean isHot(XMLGregorianCalendar timestamp) {
		if (thisScanTimestamp.compare(timestamp) == DatatypeConstants.GREATER) {
			return false;
		}
		return lastScanTimestamp == null || lastScanTimestamp.compare(timestamp) == DatatypeConstants.LESSER;
	}

	private void fireTrigger(String handlerUri, PrismObject<ObjectType> object) {
		LOGGER.debug("Firing trigger {} in {}", handlerUri, object);
		TriggerHandler handler = triggerHandlerRegistry.getHandler(handlerUri);
		if (handler == null) {
			LOGGER.warn("No registered trigger handler for URI {}", handlerUri);
		} else {
			handler.handle(object);
		}
	}

	private void removeTrigger(PrismObject<ObjectType> object, PrismContainerValue<TriggerType> triggerCVal) {
		PrismContainerDefinition<TriggerType> triggerContainerDef = triggerCVal.getParent().getDefinition();
		ContainerDelta<TriggerType> triggerDelta = (ContainerDelta<TriggerType>) triggerContainerDef.createEmptyDelta(new ItemPath(ObjectType.F_TRIGGER));
		triggerDelta.addValuesToDelete(triggerCVal.clone());
		Collection<? extends ItemDelta> modifications = MiscSchemaUtil.createCollection(triggerDelta);
		OperationResult result = new OperationResult(TriggerScannerTaskHandler.class.getName()+".removeTrigger");
		try {
			repositoryService.modifyObject(object.getCompileTimeClass(), object.getOid(), modifications , result);
		} catch (ObjectNotFoundException e) {
			LOGGER.error("Unable to remove trigger from {}: {}", new Object[]{object, e.getMessage(), e});
		} catch (SchemaException e) {
			LOGGER.error("Unable to remove trigger from {}: {}", new Object[]{object, e.getMessage(), e});
		} catch (ObjectAlreadyExistsException e) {
			LOGGER.error("Unable to remove trigger from {}: {}", new Object[]{object, e.getMessage(), e});
		}
	}

}
