/*
 * Copyright (c) 2013-2015 Evolveum
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
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class MappingEvaluator {
	
	private static final Trace LOGGER = TraceManager.getTrace(MappingEvaluator.class);
	
	@Autowired
    private PrismContext prismContext;

    @Autowired
    private MappingFactory mappingFactory;

    public <V extends PrismValue, D extends ItemDefinition, F extends ObjectType> void evaluateMapping(
			Mapping<V,D> mapping, LensContext<F> lensContext, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	evaluateMapping(mapping, lensContext, null, task, parentResult);
    }
    
    public <V extends PrismValue, D extends ItemDefinition, F extends ObjectType> void evaluateMapping(
			Mapping<V,D> mapping, LensContext<F> lensContext, LensProjectionContext projContext, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	ExpressionEnvironment<F> env = new ExpressionEnvironment<>();
		env.setLensContext(lensContext);
		env.setProjectionContext(projContext);
		env.setCurrentResult(parentResult);
		env.setCurrentTask(task);
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
		ObjectType originObject = mapping.getOriginObject();
		String objectOid, objectName, objectTypeName;
		if (originObject != null) {
			objectOid = originObject.getOid();
			objectName = String.valueOf(originObject.getName());
			objectTypeName = originObject.getClass().getSimpleName();
		} else {
			objectOid = objectName = objectTypeName = null;
		}
		String mappingName = mapping.getItemName() != null ? mapping.getItemName().getLocalPart() : null;
		long start = System.currentTimeMillis();
		try {
			task.recordState("Started evaluation of mapping " + mapping.getMappingContextDescription() + ".");
			mapping.evaluate(task, parentResult);
			task.recordState("Successfully finished evaluation of mapping " + mapping.getMappingContextDescription() + " in " + (System.currentTimeMillis()-start) + " ms.");
		} catch (IllegalArgumentException e) {
			task.recordState("Evaluation of mapping " + mapping.getMappingContextDescription() + " finished with error in " + (System.currentTimeMillis()-start) + " ms.");
			throw new IllegalArgumentException(e.getMessage()+" in "+mapping.getContextDescription(), e);
		} finally {
			task.recordMappingOperation(objectOid, objectName, objectTypeName, mappingName, System.currentTimeMillis() - start);
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
			if (lensContext.getDebugListener() != null) {
				lensContext.getDebugListener().afterMappingEvaluation(lensContext, mapping);
			}
		}
	}
		
	public <V extends PrismValue, D extends ItemDefinition, T extends ObjectType, F extends FocusType> void evaluateMappingSetProjection(
			MappingEvaluatorParams<V,D,T,F> params,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		String mappingDesc = params.getMappingDesc();
		LensElementContext<T> targetContext = params.getTargetContext();
		PrismObjectDefinition<T> targetObjectDefinition = targetContext.getObjectDefinition();
		ItemPath defaultTargetItemPath = params.getDefaultTargetItemPath();
		
		Map<ItemPath,MappingOutputStruct<V>> outputTripleMap = new HashMap<>(); 
		XMLGregorianCalendar nextRecomputeTime = null;
		Collection<MappingType> mappingTypes = params.getMappingTypes();
		Collection<Mapping<V,D>> mappings = new ArrayList<>(mappingTypes.size());

		for (MappingType mappingType: mappingTypes) {
			
			Mapping.Builder<V,D> mappingBuilder = mappingFactory.createMappingBuilder(mappingType, mappingDesc);
		
			if (!mappingBuilder.isApplicableToChannel(params.getContext().getChannel())) {
	        	continue;
	        }
			
			mappingBuilder.setNow(params.getNow());
			if (defaultTargetItemPath != null && targetObjectDefinition != null) {
				D defaultTargetItemDef = targetObjectDefinition.findItemDefinition(defaultTargetItemPath);
				mappingBuilder.setDefaultTargetDefinition(defaultTargetItemDef);
				mappingBuilder.setDefaultTargetPath(defaultTargetItemPath);
			} else {
				mappingBuilder.setDefaultTargetDefinition(params.getTargetItemDefinition());
				mappingBuilder.setDefaultTargetPath(defaultTargetItemPath);
			}
			mappingBuilder.setTargetContext(targetObjectDefinition);
			
			// Initialize mapping (using Inversion of Control)
			mappingBuilder = params.getInitializer().initialize(mappingBuilder);

			Mapping<V,D> mapping = mappingBuilder.build();
			Boolean timeConstraintValid = mapping.evaluateTimeConstraintValid(task, result);
			
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
		
		for (Mapping<V,D> mapping: mappings) {
			
			if (mapping.getStrength() == MappingStrengthType.WEAK) {
				// Evaluate weak mappings in a second run.
				continue;
			}
			
			ItemPath mappingOutputPath = mapping.getOutputPath();
			if (params.isFixTarget() && mappingOutputPath != null && defaultTargetItemPath != null && !mappingOutputPath.equivalent(defaultTargetItemPath)) {
				throw new ExpressionEvaluationException("Target cannot be overridden in "+mappingDesc);
			}
			
			if (params.getAPrioriTargetDelta() != null && mappingOutputPath != null) {
				ItemDelta<?,?> aPrioriItemDelta = params.getAPrioriTargetDelta().findItemDelta(mappingOutputPath);
				if (mapping.getStrength() != MappingStrengthType.STRONG) {
		        	if (aPrioriItemDelta != null && !aPrioriItemDelta.isEmpty()) {
		        		continue;
		        	}
		        }
			}
						
			evaluateMapping(mapping, params.getContext(), task, result);
			
			PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Output triple of mapping {}\n{}", mapping.getContextDescription(),
						mappingOutputTriple==null?null:mappingOutputTriple.debugDump(1));
			}
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
		for (Mapping<V,D> mapping: mappings) {

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
			
			Item<V,D> aPrioriTargetItem = null;
			PrismObject<T> aPrioriTargetObject = params.getAPrioriTargetObject();
			if (aPrioriTargetObject != null && mappingOutputPath != null) {
				aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
			}
			if ((aPrioriTargetItem == null || aPrioriTargetItem.isEmpty()) && outputTriple == null) {
				
				if (mapping.getStrength() != MappingStrengthType.WEAK) {
					continue;
				}

				evaluateMapping(mapping, params.getContext(), task, result);

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
				ItemDelta<V,D> targetItemDelta = targetItemDefinition.createEmptyDelta(mappingOutputPath);
				
				Item<V,D> aPrioriTargetItem = null;
				PrismObject<T> aPrioriTargetObject = params.getAPrioriTargetObject();
				if (aPrioriTargetObject != null) {
					aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
				}
				
				// WARNING
				// Following code seems to be wrong. It is not very relativisic. It seems to always
				// go for replace.
				// It seems that it is only used for activation mappings (outbout and inbound). As
				// these are quite special single-value properties then it seems to work fine
				// (with the exception of MID-3418). Todo: make it more relativistic: MID-3419
				
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
		            
		            if (LOGGER.isTraceEnabled()) {
		            	LOGGER.trace("{}: hasFullTargetObject={}, isStrongMappingWasUsed={}, valuesToReplace={}", 
		            			new Object[]{mappingDesc, params.hasFullTargetObject(), 
		            					mappingOutputStruct.isStrongMappingWasUsed(), valuesToReplace});
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
		                
		        	} else if (outputTriple.hasMinusSet()) {
		        		LOGGER.trace("{} resulted in null or empty value for {} and there is a minus set, resetting it (replace with empty)", mappingDesc, targetContext);
		        		targetItemDelta.setValueToReplace();
		        		
		        	} else {
		        		LOGGER.trace("{} resulted in null or empty value for {}, skipping", mappingDesc, targetContext);
		        	}
		        	
		        }
		        
		        if (targetItemDelta.isEmpty()) {
		        	continue;
		        }
		        
		        LOGGER.trace("{} adding new delta for {}: {}", mappingDesc, targetContext, targetItemDelta);
		        targetContext.swallowToSecondaryDelta(targetItemDelta);
			}
			
		}
		
		// Figure out recompute time
		
		for (Mapping<V,D> mapping: mappings) {
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
