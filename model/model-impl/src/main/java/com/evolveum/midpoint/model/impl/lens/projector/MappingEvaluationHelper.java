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
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
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
//	public <V extends PrismValue, F extends FocusType, O extends ObjectType> void evaluateMappingSetProjection(
//			Collection<MappingType> mappingTypes, String mappingDesc,
//			XMLGregorianCalendar now, MappingInitializer<V> initializer, MappingOutputProcessor<V> processor,
//			Item<V> aPrioriValue, ItemDelta<V> aPrioriDelta, PrismObject<? extends ObjectType> aPrioriObject,
//			Boolean evaluateCurrent, MutableBoolean strongMappingWasUsed,
//			LensContext<F> context, LensElementContext<O> targetContext, 
//			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		
	public <V extends PrismValue, T extends ObjectType, F extends FocusType> void evaluateMappingSetProjection(
			MappingEvaluatorHelperParams<V,T,F> params,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		String mappingDesc = params.getMappingDesc();
		LensElementContext<T> targetContext = params.getTargetContext();
		PrismObjectDefinition<T> targetObjectDefinition = targetContext.getObjectDefinition();
		ItemPath defaultTargetItemPath = params.getDefaultTargetItemPath();
		
		Map<ItemPath,MappingOutputStruct<V>> outputTripleMap = new HashMap<>(); 
		XMLGregorianCalendar nextRecomputeTime = null;
		Collection<MappingType> mappingTypes = params.getMappingTypes();
		Collection<Mapping<V>> mappings = new ArrayList<Mapping<V>>(mappingTypes.size());

		for (MappingType mappingType: mappingTypes) {
			
			Mapping<V> mapping = valueConstructionFactory.createMapping(mappingType, mappingDesc);
		
			if (!mapping.isApplicableToChannel(params.getContext().getChannel())) {
	        	continue;
	        }
			
			mapping.setNow(params.getNow());
			if (defaultTargetItemPath != null && targetObjectDefinition != null) {
				ItemDefinition defaultTargetItemDef = targetObjectDefinition.findItemDefinition(defaultTargetItemPath);
				mapping.setDefaultTargetDefinition(defaultTargetItemDef);
				mapping.setDefaultTargetPath(defaultTargetItemPath);
			} else {
				mapping.setDefaultTargetDefinition(params.getTargetItemDefinition());
				mapping.setDefaultTargetPath(defaultTargetItemPath);
			}
			mapping.setTargetContext(targetObjectDefinition);
			
			// Initialize mapping (using Inversion of Control)
			params.getInitializer().initialize(mapping);
			
			Boolean timeConstraintValid = mapping.evaluateTimeConstraintValid(result);
			
			if (params.getEvaluateCurrent() != null) {
				if (params.getEvaluateCurrent() && !timeConstraintValid) {
					continue;
				}
				if (!params.getEvaluateCurrent() && timeConstraintValid) {
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
			
			ItemPath mappingOutputPath = mapping.getOutputPath();
			if (params.isFixTarget() && mappingOutputPath != null && defaultTargetItemPath != null && !mappingOutputPath.equivalent(defaultTargetItemPath)) {
				throw new ExpressionEvaluationException("Target cannot be overridden in "+mappingDesc);
			}
			
			if (params.getAPrioriTargetDelta() != null && mappingOutputPath != null) {
				ItemDelta<PrismValue> aPrioriItemDelta = params.getAPrioriTargetDelta().findItemDelta(mappingOutputPath);
				if (mapping.getStrength() != MappingStrengthType.STRONG) {
		        	if (aPrioriItemDelta != null && !aPrioriItemDelta.isEmpty()) {
		        		continue;
		        	}
		        }
			}
						
			LensUtil.evaluateMapping(mapping, params.getContext(), task, result);
			
			PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
			if (mappingOutputTriple != null) {

				MappingOutputStruct<V> mappingOutputStruct = outputTripleMap.get(mappingOutputPath);
				if (mappingOutputStruct == null) {
					mappingOutputStruct = new MappingOutputStruct<>();
					outputTripleMap.put(mappingOutputPath, mappingOutputStruct);
				}
				
                if (mapping.getStrength() == MappingStrengthType.STRONG) {
                	mappingOutputStruct.setStrongMappingWasUsed(true);
                }

                PrismValueDeltaSetTriple<V> outputTriple = mappingOutputStruct.getOutputTriple();
				if (outputTriple == null) {
					mappingOutputStruct.setOutputTriple(mappingOutputTriple);
				} else {
					outputTriple.merge(mappingOutputTriple);
				}
			}
			
		}

		
		// Second pass, evaluate only weak mappings
		for (Mapping<V> mapping: mappings) {

			ItemPath mappingOutputPath = mapping.getOutputPath();
			if (params.isFixTarget() && mappingOutputPath != null && defaultTargetItemPath != null && !mappingOutputPath.equivalent(defaultTargetItemPath)) {
				throw new ExpressionEvaluationException("Target cannot be overridden in "+mappingDesc);
			}
			
			MappingOutputStruct<V> mappingOutputStruct = outputTripleMap.get(mappingOutputPath);
			if (mappingOutputStruct == null) {
				mappingOutputStruct = new MappingOutputStruct<>();
				outputTripleMap.put(mappingOutputPath, mappingOutputStruct);
			}
			
			PrismValueDeltaSetTriple<V> outputTriple = mappingOutputStruct.getOutputTriple();
			
			Item<?> aPrioriTargetItem = null;
			PrismObject<T> aPrioriTargetObject = params.getAPrioriTargetObject();
			if (aPrioriTargetObject != null && mappingOutputPath != null) {
				aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
			}
			if ((aPrioriTargetItem == null || aPrioriTargetItem.isEmpty()) && outputTriple == null) {
				
				if (mapping.getStrength() != MappingStrengthType.WEAK) {
					continue;
				}

				LensUtil.evaluateMapping(mapping, params.getContext(), task, result);

				PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
				if (mappingOutputTriple != null) {
					if (outputTriple == null) {
						mappingOutputStruct.setOutputTriple(mappingOutputTriple);
					} else {
						outputTriple.merge(mappingOutputTriple);
					}
				}

			}
		}
		
		MappingOutputProcessor<V> processor = params.getProcessor();
		for (Entry<ItemPath, MappingOutputStruct<V>> outputTripleMapEntry: outputTripleMap.entrySet()) {
			ItemPath mappingOutputPath = outputTripleMapEntry.getKey();
			MappingOutputStruct<V> mappingOutputStruct = outputTripleMapEntry.getValue();
			PrismValueDeltaSetTriple<V> outputTriple = mappingOutputStruct.getOutputTriple();
			
			if (processor != null) {
				processor.process(mappingOutputPath, outputTriple);
			} else {
			
				if (outputTriple == null) {
		    		LOGGER.trace("{} expression resulted in null triple for {}, skipping", mappingDesc, targetContext);
		            continue;
		    	}
				
				ItemDefinition targetItemDefinition = null;
				if (mappingOutputPath != null) {
					targetItemDefinition = targetObjectDefinition.findItemDefinition(mappingOutputPath);
					if (targetItemDefinition == null) {
						throw new SchemaException("No definition for item "+mappingOutputPath+" in "+targetObjectDefinition);
					}
				} else {
					targetItemDefinition = params.getTargetItemDefinition();
				}
				ItemDelta<V> targetItemDelta = targetItemDefinition.createEmptyDelta(mappingOutputPath);
				
				Item<V> aPrioriTargetItem = null;
				PrismObject<T> aPrioriTargetObject = params.getAPrioriTargetObject();
				if (aPrioriTargetObject != null) {
					aPrioriTargetItem = (Item<V>) aPrioriTargetObject.findItem(mappingOutputPath);
				}
				
				if (targetContext.isAdd()) {
		        	
		        	Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();
			        if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			            LOGGER.trace("{} resulted in null or empty value for {}, skipping", mappingDesc, targetContext);
			            continue;
			        }
			        targetItemDelta.setValuesToReplace(PrismValue.cloneCollection(nonNegativeValues));
			        
		        } else {
	
		            // if we have fresh information (full shadow) AND the mapping used to derive the information was strong,
		            // we will consider all values (zero & plus sets) -- otherwise, we take only the "plus" (i.e. changed) set
	
		            // the first case is necessary, because in some situations (e.g. when mapping is changed)
		            // the evaluator sees no differences w.r.t. real state, even if there is a difference
		            // - and we must have a way to push new information onto the resource
	
		            Collection<V> valuesToReplace;
	
		            if (params.hasFullTargetObject() && mappingOutputStruct.isStrongMappingWasUsed()) {
		                valuesToReplace = outputTriple.getNonNegativeValues();
		            } else {
		                valuesToReplace = outputTriple.getPlusSet();
		            }
	
		        	if (valuesToReplace != null && !valuesToReplace.isEmpty()) {
	
		                // if what we want to set is the same as is already in the shadow, we skip that
		                // (we insist on having full shadow, to be sure we work with current data)
	
		                if (params.hasFullTargetObject() && targetContext.isFresh() && aPrioriTargetItem != null) {
		                	Collection<V> valuesPresent = aPrioriTargetItem.getValues();
		                	if (PrismValue.equalsRealValues(valuesPresent, valuesToReplace)) {
		                        LOGGER.trace("{} resulted in existing values for {}, skipping creation of a delta", mappingDesc, targetContext);
		                        continue;
		                	}
		                }
		                targetItemDelta.setValuesToReplace(PrismValue.cloneCollection(valuesToReplace));
		        	}
		        	
		        }
		        
		        if (targetItemDelta.isEmpty()) {
		        	continue;
		        }
		        
		        LOGGER.trace("{} adding new delta for {}: {}", new Object[]{mappingDesc, targetContext, targetItemDelta});
		        targetContext.swallowToSecondaryDelta(targetItemDelta);
			}
			
		}
		
		// Figure out recompute time
		
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
			if (params.getAPrioriTargetObject() != null) {
				for (TriggerType trigger: params.getAPrioriTargetObject().asObjectable().getTrigger()) {
					if (RecomputeTriggerHandler.HANDLER_URI.equals(trigger.getHandlerUri()) &&
							nextRecomputeTime.equals(trigger.getTimestamp())) {
								alreadyHasTrigger = true;
								break;
					}
				}
			}
			
			if (!alreadyHasTrigger) {
				PrismContainerDefinition<TriggerType> triggerContDef = targetObjectDefinition.findContainerDefinition(ObjectType.F_TRIGGER);
				ContainerDelta<TriggerType> triggerDelta = triggerContDef.createEmptyDelta(new ItemPath(ObjectType.F_TRIGGER));
				PrismContainerValue<TriggerType> triggerCVal = triggerContDef.createValue();
				triggerDelta.addValueToAdd(triggerCVal);
				TriggerType triggerType = triggerCVal.asContainerable();
				triggerType.setTimestamp(nextRecomputeTime);
				triggerType.setHandlerUri(RecomputeTriggerHandler.HANDLER_URI);

				targetContext.swallowToSecondaryDelta(triggerDelta);
			}
		}
	}
	
	private class MappingOutputStruct<V extends PrismValue> {
		private PrismValueDeltaSetTriple<V> outputTriple = null;
		private boolean strongMappingWasUsed = false;

		public PrismValueDeltaSetTriple<V> getOutputTriple() {
			return outputTriple;
		}
		
		public void setOutputTriple(PrismValueDeltaSetTriple<V> outputTriple) {
			this.outputTriple = outputTriple;
		}

		public boolean isStrongMappingWasUsed() {
			return strongMappingWasUsed;
		}

		public void setStrongMappingWasUsed(boolean strongMappingWasUsed) {
			this.strongMappingWasUsed = strongMappingWasUsed;
		}
		
		
	}
	
	

}
