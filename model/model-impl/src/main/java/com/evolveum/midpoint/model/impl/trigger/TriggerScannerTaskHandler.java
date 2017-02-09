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

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.util.AbstractScannerResultHandler;
import com.evolveum.midpoint.model.impl.util.AbstractScannerTaskHandler;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_TRIGGER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType.F_TIMESTAMP;

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
	
	public static final String HANDLER_URI = ModelPublicConstants.TRIGGER_SCANNER_TASK_HANDLER_URI;
        	
	private static final transient Trace LOGGER = TraceManager.getTrace(TriggerScannerTaskHandler.class);
	
	@Autowired
	private TriggerHandlerRegistry triggerHandlerRegistry;
	
	public TriggerScannerTaskHandler() {
        super(ObjectType.class, "Trigger scan", OperationConstants.TRIGGER_SCAN);
    }

	// task OID -> handlerUri -> OID+TriggerID; cleared on task start
	// we use plain map, as it is much easier to synchronize explicitly than to play with ConcurrentMap methods
	private Map<String,Map<String,Set<String>>> processedTriggersMap = new HashMap<>();

	private synchronized void initProcessedTriggers(Task coordinatorTask) {
		Validate.notNull(coordinatorTask.getOid(), "Task OID is null");
		processedTriggersMap.put(coordinatorTask.getOid(), new HashMap<>());
	}

	// TODO fix possible (although very small) memory leak occurring when task finishes unsuccessfully
	private synchronized void cleanupProcessedOids(Task coordinatorTask) {
		Validate.notNull(coordinatorTask.getOid(), "Task OID is null");
		processedTriggersMap.remove(coordinatorTask.getOid());
	}

	private synchronized boolean triggerAlreadySeen(Task coordinatorTask, String handlerUri, String oidPlusTriggerId) {
		Validate.notNull(coordinatorTask.getOid(), "Coordinator task OID is null");
		Map<String,Set<String>> taskTriggersMap = processedTriggersMap.get(coordinatorTask.getOid());
		if (taskTriggersMap == null) {
			throw new IllegalStateException("ProcessedTriggers map was not initialized for task = " + coordinatorTask);
		}
		Set<String> processedTriggers = taskTriggersMap.get(handlerUri);
		if (processedTriggers != null) {
			return !processedTriggers.add(oidPlusTriggerId);
		} else {
			processedTriggers = new HashSet<>();
			processedTriggers.add(oidPlusTriggerId);
			taskTriggersMap.put(handlerUri, processedTriggers);
			return false;
		}
	}

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@Override
	protected Class<? extends ObjectType> getType(Task task) {
		return ObjectType.class;		// TODO - is this ok???
	}

	@Override
	protected ObjectQuery createQuery(AbstractScannerResultHandler<ObjectType> handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {

		initProcessedTriggers(task);

		ObjectQuery query = new ObjectQuery();
		ObjectFilter filter;

		if (handler.getLastScanTimestamp() == null) {
			filter = QueryBuilder.queryFor(ObjectType.class, prismContext)
					.item(F_TRIGGER, F_TIMESTAMP).le(handler.getThisScanTimestamp())
					.buildFilter();
		} else {
			filter = QueryBuilder.queryFor(ObjectType.class, prismContext)
					.exists(F_TRIGGER)
						.block()
							.item(F_TIMESTAMP).gt(handler.getLastScanTimestamp())
							.and().item(F_TIMESTAMP).le(handler.getThisScanTimestamp())
						.endBlock()
					.buildFilter();
		}
		
		query.setFilter(filter);
		return query;
	}

	@Override
	protected void finish(AbstractScannerResultHandler<ObjectType> handler, TaskRunResult runResult, Task task, OperationResult opResult)
			throws SchemaException {
		super.finish(handler, runResult, task, opResult);
		cleanupProcessedOids(task);
	}

	@Override
	protected AbstractScannerResultHandler<ObjectType> createHandler(TaskRunResult runResult, final Task coordinatorTask,
			OperationResult opResult) {
		
		AbstractScannerResultHandler<ObjectType> handler = new AbstractScannerResultHandler<ObjectType>(
				coordinatorTask, TriggerScannerTaskHandler.class.getName(), "trigger", "trigger task", taskManager) {
			@Override
			protected boolean handleObject(PrismObject<ObjectType> object, Task workerTask, OperationResult result) throws CommonException {
				fireTriggers(this, object, workerTask, coordinatorTask, result);
				return true;
			}
		};
        handler.setStopOnError(false);
        return handler;
	}

	private void fireTriggers(AbstractScannerResultHandler<ObjectType> handler, PrismObject<ObjectType> object, Task workerTask, Task coordinatorTask,
			OperationResult result) throws SchemaException,
			ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException, 
			ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismContainer<TriggerType> triggerContainer = object.findContainer(F_TRIGGER);
		if (triggerContainer == null) {
			LOGGER.warn("Strange thing, attempt to fire triggers on {}, but it does not have trigger container", object);
		} else {
			List<PrismContainerValue<TriggerType>> triggerCVals = triggerContainer.getValues();
			if (triggerCVals.isEmpty()) {
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
							fireTrigger(triggerType, object, workerTask, coordinatorTask, result);
							removeTrigger(object, triggerCVal, workerTask);
						} else {
							LOGGER.trace("Trigger {} is not hot (timestamp={}, thisScanTimestamp={}, lastScanTimestamp={})",
									triggerType, timestamp,
									handler.getThisScanTimestamp(), handler.getLastScanTimestamp());
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

	private void fireTrigger(TriggerType trigger, PrismObject<ObjectType> object, Task workerTask, Task coordinatorTask, OperationResult result) {
		String handlerUri = trigger.getHandlerUri();
		if (handlerUri == null) {
			LOGGER.warn("Trigger without handler URI in {}", object);
			return;
		}
		LOGGER.debug("Firing trigger {} in {}: id={}", handlerUri, object, trigger.getId());
		TriggerHandler handler = triggerHandlerRegistry.getHandler(handlerUri);
		if (handler == null) {
			LOGGER.warn("No registered trigger handler for URI {}", trigger);
		} else if (triggerAlreadySeen(coordinatorTask, handlerUri, object.getOid()+":"+trigger.getId())) {
			LOGGER.trace("Handler {} already executed for {}:{}", trigger, ObjectTypeUtil.toShortString(object), trigger.getId());
		} else {
			try {
				handler.handle(object, trigger, workerTask, result);
				// Properly handle everything that the handler spits out. We do not want this task to die.
				// Looks like the impossible happens and checked exceptions can somehow get here. Hence the heavy artillery below.
			} catch (Throwable e) {
				LOGGER.error("Trigger handler {} executed on {} thrown an error: {}", handler, object, e.getMessage(), e);
				result.recordPartialError(e);
			}
		}
	}

	private void removeTrigger(PrismObject<ObjectType> object, PrismContainerValue<TriggerType> triggerCVal, Task task) {
		PrismContainerDefinition<TriggerType> triggerContainerDef = triggerCVal.getParent().getDefinition();
		ContainerDelta<TriggerType> triggerDelta = triggerContainerDef.createEmptyDelta(new ItemPath(F_TRIGGER));
		triggerDelta.addValuesToDelete(triggerCVal.clone());
		Collection<? extends ItemDelta> modifications = MiscSchemaUtil.createCollection(triggerDelta);
		// This is detached result. It will not take part of the task result. We do not really care.
		OperationResult result = new OperationResult(TriggerScannerTaskHandler.class.getName()+".removeTrigger");
		try {
			repositoryService.modifyObject(object.getCompileTimeClass(), object.getOid(), modifications, result);
			result.computeStatus();
			task.recordObjectActionExecuted(object, ChangeType.MODIFY, null);
		} catch (ObjectNotFoundException e) {
			// Object is gone. Ergo there are no triggers left. Ergo the trigger was removed.
			// Ergo this is not really an error.
			task.recordObjectActionExecuted(object, ChangeType.MODIFY, e);
			LOGGER.trace("Unable to remove trigger from {}: {} (but this is probably OK)", object, e.getMessage(), e);
		} catch (SchemaException | ObjectAlreadyExistsException e) {
			task.recordObjectActionExecuted(object, ChangeType.MODIFY, e);
			LOGGER.error("Unable to remove trigger from {}: {}", object, e.getMessage(), e);
		} catch (Throwable t) {
			task.recordObjectActionExecuted(object, ChangeType.MODIFY, t);
			throw t;
		} finally {
			task.markObjectActionExecutedBoundary();		// maybe OK (absolute correctness is not quite important here)
		}
	}

}
