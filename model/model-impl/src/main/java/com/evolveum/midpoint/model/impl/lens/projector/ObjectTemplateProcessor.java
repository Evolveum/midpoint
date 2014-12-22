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
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * Processor to handle object template.
 * 
 * @author Radovan Semancik
 * 
 */
@Component
public class ObjectTemplateProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

	@Autowired(required = true)
	private MappingFactory mappingFactory;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private PasswordPolicyProcessor passwordPolicyProcessor;
	
	@Autowired(required = true)
	private ModelObjectResolver modelObjectResolver;
	
	@Autowired(required = true)
	private ActivationComputer activationComputer;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
    private MappingEvaluationHelper mappingHelper;
	
	public <F extends FocusType> void processTemplate(LensContext<F> context, ObjectTemplateMappingEvaluationPhaseType phase, 
			XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, ObjectAlreadyExistsException {
		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext.isDelete()) {
    		LOGGER.trace("Skipping processing of object template: focus delete");
    		return;
    	}
    	
    	ObjectTemplateType objectTemplate = context.getFocusTemplate();
		if (objectTemplate == null) {
			// No applicable template
			LOGGER.trace("Skipping processing of object template: no object template");
			return;
		}
		
		int iteration = focusContext.getIteration();
		String iterationToken = focusContext.getIterationToken();
		ObjectDeltaObject<F> focusOdo = focusContext.getObjectDeltaObject();
		PrismObjectDefinition<F> focusDefinition = getFocusDefinition(focusContext.getObjectTypeClass());
						
		LOGGER.trace("Applying {} to {}, iteration {} ({}), phase {}", 
				new Object[]{objectTemplate, focusContext.getObjectNew(), iteration, iterationToken, phase});
						
		Map<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap = new HashMap<>();
		
		XMLGregorianCalendar nextRecomputeTime = collectTripleFromTemplate(context, objectTemplate, phase, focusOdo, outputTripleMap,
				iteration, iterationToken, now, objectTemplate.toString(), task, result);
								
		Collection<ItemDelta<? extends PrismValue>> itemDeltas = computeItemDeltas(outputTripleMap, focusOdo, focusDefinition, "object template "+objectTemplate+ " for focus "+focusOdo.getAnyObject());
		
		focusContext.applyProjectionWaveSecondaryDeltas(itemDeltas);
				
		if (nextRecomputeTime != null) {
			
			boolean alreadyHasTrigger = false;
			PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
			if (objectCurrent != null) {
				for (TriggerType trigger: objectCurrent.asObjectable().getTrigger()) {
					if (RecomputeTriggerHandler.HANDLER_URI.equals(trigger.getHandlerUri()) &&
							nextRecomputeTime.equals(trigger.getTimestamp())) {
								alreadyHasTrigger = true;
								break;
					}
				}
			}
			
			if (!alreadyHasTrigger) {
				PrismObjectDefinition<F> objectDefinition = focusContext.getObjectDefinition();
				PrismContainerDefinition<TriggerType> triggerContDef = objectDefinition.findContainerDefinition(ObjectType.F_TRIGGER);
				ContainerDelta<TriggerType> triggerDelta = triggerContDef.createEmptyDelta(new ItemPath(ObjectType.F_TRIGGER));
				PrismContainerValue<TriggerType> triggerCVal = triggerContDef.createValue();
				triggerDelta.addValueToAdd(triggerCVal);
				TriggerType triggerType = triggerCVal.asContainerable();
				triggerType.setTimestamp(nextRecomputeTime);
				triggerType.setHandlerUri(RecomputeTriggerHandler.HANDLER_URI);
				
				focusContext.swallowToProjectionWaveSecondaryDelta(triggerDelta);
			}
		}

	}

	<F extends FocusType> Collection<ItemDelta<? extends PrismValue>> computeItemDeltas(Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
			ObjectDeltaObject<F> focusOdo, PrismObjectDefinition<F> focusDefinition, String contextDesc) throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
		Collection<ItemDelta<? extends PrismValue>> itemDeltas = new ArrayList<>();
		for (Entry<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> entry: outputTripleMap.entrySet()) {
			ItemPath itemPath = entry.getKey();
			DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>> outputTriple = entry.getValue();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Computed triple for {}:\n{}", itemPath, outputTriple.debugDump());
			}
			ItemDelta<? extends PrismValue> apropriItemDelta = null;
//			boolean addUnchangedValues = focusContext.isAdd();
			// We need to add unchanged values otherwise the unconditional mappings will not be applied
			boolean addUnchangedValues = true;
			ItemDelta<? extends PrismValue> itemDelta = LensUtil.consolidateTripleToDelta(itemPath, (DeltaSetTriple)outputTriple,
					focusDefinition.findItemDefinition(itemPath), apropriItemDelta, focusOdo.getNewObject(), null, null, 
					addUnchangedValues, true, false, contextDesc, true);
			
			itemDelta.simplify();
			itemDelta.validate(contextDesc);
			itemDeltas.add(itemDelta);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Computed delta:\n{}", itemDelta.debugDump());
			}
		}
		return itemDeltas;
	}

	private <F extends FocusType> XMLGregorianCalendar collectTripleFromTemplate(LensContext<F> context,
			ObjectTemplateType objectTemplateType, ObjectTemplateMappingEvaluationPhaseType phase, ObjectDeltaObject<F> userOdo,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
			int iteration, String iterationToken,
			XMLGregorianCalendar now, String contextDesc, Task task, OperationResult result)
					throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		XMLGregorianCalendar nextRecomputeTime = null;
		
		// Process includes
		for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
			PrismObject<ObjectTemplateType> includeObject = includeRef.asReferenceValue().getObject();
			if (includeObject == null) {
				ObjectTemplateType includeObjectType = modelObjectResolver.resolve(includeRef, ObjectTemplateType.class, 
						null, "include reference in "+objectTemplateType + " in " + contextDesc, result);
				includeObject = includeObjectType.asPrismObject();
				// Store resolved object for future use (e.g. next waves).
				includeRef.asReferenceValue().setObject(includeObject);
			}
			LOGGER.trace("Including template {}", includeObject);
			ObjectTemplateType includeObjectType = includeObject.asObjectable();
			XMLGregorianCalendar includeNextRecomputeTime = collectTripleFromTemplate(context, includeObjectType, phase, userOdo, 
					outputTripleMap, iteration, iterationToken, 
					now, "include "+includeObject+" in "+objectTemplateType + " in " + contextDesc, task, result);
			if (includeNextRecomputeTime != null) {
				if (nextRecomputeTime == null || nextRecomputeTime.compare(includeNextRecomputeTime) == DatatypeConstants.GREATER) {
					nextRecomputeTime = includeNextRecomputeTime;
				}
			}
		}
		
		// Process own mappings
		Collection<ObjectTemplateMappingType> mappings = objectTemplateType.getMapping();
		XMLGregorianCalendar templateNextRecomputeTime = collectTripleFromMappings(mappings, phase, context, objectTemplateType, userOdo, 
				outputTripleMap, iteration, iterationToken, now, contextDesc, task, result);
		if (templateNextRecomputeTime != null) {
			if (nextRecomputeTime == null || nextRecomputeTime.compare(templateNextRecomputeTime) == DatatypeConstants.GREATER) {
				nextRecomputeTime = templateNextRecomputeTime;
			}
		}
		
		return nextRecomputeTime;
	}
	
	
	private <V extends PrismValue, F extends FocusType> XMLGregorianCalendar collectTripleFromMappings(
			Collection<ObjectTemplateMappingType> mappings, ObjectTemplateMappingEvaluationPhaseType phase, LensContext<F> context,
			ObjectTemplateType objectTemplateType, ObjectDeltaObject<F> userOdo,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
			int iteration, String iterationToken,
			XMLGregorianCalendar now, String contextDesc, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		XMLGregorianCalendar nextRecomputeTime = null;
		
		for (ObjectTemplateMappingType mappingType : mappings) {
			ObjectTemplateMappingEvaluationPhaseType mappingPhase = mappingType.getEvaluationPhase();
			if (mappingPhase == null) {
				mappingPhase = ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS;
			}
			if (mappingPhase != phase) {
				continue;
			}
			Mapping<V> mapping = LensUtil.createFocusMapping(mappingFactory, context, mappingType, objectTemplateType, userOdo, 
					null, iteration, iterationToken, context.getSystemConfiguration(), now, contextDesc, result);
			if (mapping == null) {
				continue;
			}
			
			Boolean timeConstraintValid = mapping.evaluateTimeConstraintValid(result);
			
			if (timeConstraintValid != null && !timeConstraintValid) {
				// Delayed mapping. Just schedule recompute time
				XMLGregorianCalendar mappingNextRecomputeTime = mapping.getNextRecomputeTime();
				LOGGER.trace("Evaluation of mapping {} delayed to {}", mapping, mappingNextRecomputeTime);
				if (mappingNextRecomputeTime != null) {
					if (nextRecomputeTime == null || nextRecomputeTime.compare(mappingNextRecomputeTime) == DatatypeConstants.GREATER) {
						nextRecomputeTime = mappingNextRecomputeTime;
					}
				}
				continue;
			}
			
			LensUtil.evaluateMapping(mapping, context, task, result);
			
			ItemPath itemPath = mapping.getOutputPath();
            if (itemPath == null) {
                continue;
            }
			DeltaSetTriple<ItemValueWithOrigin<V>> outputTriple = ItemValueWithOrigin.createOutputTriple(mapping);
			if (outputTriple == null) {
				continue;
			}
			DeltaSetTriple<ItemValueWithOrigin<V>> mapTriple = (DeltaSetTriple<ItemValueWithOrigin<V>>) outputTripleMap.get(itemPath);
			if (mapTriple == null) {
				outputTripleMap.put(itemPath, outputTriple);
			} else {
				mapTriple.merge(outputTriple);
			}
		}
		
		return nextRecomputeTime;
	}

	private <F extends ObjectType> PrismObjectDefinition<F> getFocusDefinition(Class<F> focusClass) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
	}
	
}
