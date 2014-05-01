/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.model.lens.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class MappingEvaluationHelper {
	
	private static final Trace LOGGER = TraceManager.getTrace(MappingEvaluationHelper.class);
	
	@Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;

    /**
     * strongMappingWasUsed: Returns true here if the value was (at least partly) determined by a strong mapping.
     * Used to know whether (when doing reconciliation) this value should be forcibly put onto the resource, even
     * if it was not changed (i.e. if it's only in the zero set).
     */
	public <V extends PrismValue, F extends FocusType> PrismValueDeltaSetTriple<V> evaluateMappingSetProjection(Collection<MappingType> mappingTypes, String mappingDesc,
			XMLGregorianCalendar now, MappingInitializer<V> initializer, 
			Item<V> aPrioriValue, ItemDelta<V> aPrioriDelta, PrismObject<? extends ObjectType> aPrioriObject,
			Boolean evaluateCurrent, MutableBoolean strongMappingWasUsed,
			LensContext<F> context, LensProjectionContext accCtx, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		PrismValueDeltaSetTriple<V> outputTriple = null;
		XMLGregorianCalendar nextRecomputeTime = null;
		Collection<Mapping<V>> mappings = new ArrayList<Mapping<V>>(mappingTypes.size());

        if (strongMappingWasUsed != null) {
            strongMappingWasUsed.setValue(false);
        }
		
		for (MappingType mappingType: mappingTypes) {
			
			Mapping<V> mapping = valueConstructionFactory.createMapping(mappingType, mappingDesc);
		
			if (!mapping.isApplicableToChannel(context.getChannel())) {
	        	continue;
	        }
			
			mapping.setNow(now);
			
			// Initialize mapping (using Inversion of Control)
			initializer.initialize(mapping);
			
			Boolean timeConstraintValid = mapping.evaluateTimeConstraintValid(result);
			
			if (evaluateCurrent != null) {
				if (evaluateCurrent && !timeConstraintValid) {
					continue;
				}
				if (!evaluateCurrent && timeConstraintValid) {
					continue;
				}
			}
			
			mappings.add(mapping);
		}
		
		for (Mapping<V> mapping: mappings) {
			
			if (mapping.getStrength() == MappingStrengthType.WEAK) {
				// Evaluate weak mappings in a second run.
				continue;
			}
			
			if (mapping.getStrength() != MappingStrengthType.STRONG) {
	        	if (aPrioriDelta != null && !aPrioriDelta.isEmpty()) {
	        		continue;
	        	}
	        }
						
			LensUtil.evaluateMapping(mapping, context, task, result);
			
			PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
			if (mappingOutputTriple != null) {

                if (mapping.getStrength() == MappingStrengthType.STRONG && strongMappingWasUsed != null) {
                    strongMappingWasUsed.setValue(true);
                }

				if (outputTriple == null) {
					outputTriple = mappingOutputTriple;
				} else {
					outputTriple.merge(mappingOutputTriple);
				}
			}
			
		}

		if ((aPrioriValue == null || aPrioriValue.isEmpty()) && outputTriple == null) {
			// Second pass, evaluate only weak mappings
			for (Mapping<V> mapping: mappings) {

				if (mapping.getStrength() != MappingStrengthType.WEAK) {
					continue;
				}

				LensUtil.evaluateMapping(mapping, context, task, result);

				PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
				if (mappingOutputTriple != null) {
					if (outputTriple == null) {
						outputTriple = mappingOutputTriple;
					} else {
						outputTriple.merge(mappingOutputTriple);
					}
				}

			}
		}
		
		for (Mapping<V> mapping: mappings) {
			XMLGregorianCalendar mappingNextRecomputeTime = mapping.getNextRecomputeTime();
			if (mappingNextRecomputeTime != null) {
				if (nextRecomputeTime == null || nextRecomputeTime.compare(mappingNextRecomputeTime) == DatatypeConstants.GREATER) {
					nextRecomputeTime = mappingNextRecomputeTime;
				}
			}
		}
			
		if (nextRecomputeTime != null) {
			
			boolean alreadyHasTrigger = false;
			if (aPrioriObject != null) {
				for (TriggerType trigger: aPrioriObject.asObjectable().getTrigger()) {
					if (RecomputeTriggerHandler.HANDLER_URI.equals(trigger.getHandlerUri()) &&
							nextRecomputeTime.equals(trigger.getTimestamp())) {
								alreadyHasTrigger = true;
								break;
					}
				}
			}
			
			if (!alreadyHasTrigger) {
				PrismObjectDefinition<ShadowType> objectDefinition = accCtx.getObjectDefinition();
				PrismContainerDefinition<TriggerType> triggerContDef = objectDefinition.findContainerDefinition(ObjectType.F_TRIGGER);
				ContainerDelta<TriggerType> triggerDelta = triggerContDef.createEmptyDelta(new ItemPath(ObjectType.F_TRIGGER));
				PrismContainerValue<TriggerType> triggerCVal = triggerContDef.createValue();
				triggerDelta.addValueToAdd(triggerCVal);
				TriggerType triggerType = triggerCVal.asContainerable();
				triggerType.setTimestamp(nextRecomputeTime);
				triggerType.setHandlerUri(RecomputeTriggerHandler.HANDLER_URI);

				accCtx.swallowToSecondaryDelta(triggerDelta);
			}
		}		
		
		return outputTriple;
	}

}
