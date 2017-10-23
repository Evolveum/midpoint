/*
 * Copyright (c) 2013-2017 Evolveum
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

import java.util.*;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.repo.common.expression.ValuePolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialPolicyEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationValidityCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class MappingEvaluator {

	private static final Trace LOGGER = TraceManager.getTrace(MappingEvaluator.class);

    @Autowired private MappingFactory mappingFactory;
    @Autowired private CredentialsProcessor credentialsProcessor;
    @Autowired private ContextLoader contextLoader;

    public static final List<QName> FOCUS_VARIABLE_NAMES = Arrays.asList(ExpressionConstants.VAR_FOCUS, ExpressionConstants.VAR_USER);

	public <V extends PrismValue, D extends ItemDefinition, F extends ObjectType> void evaluateMapping(
			Mapping<V,D> mapping, LensContext<F> lensContext, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
    	evaluateMapping(mapping, lensContext, null, task, parentResult);
    }

    public <V extends PrismValue, D extends ItemDefinition, F extends ObjectType> void evaluateMapping(
			Mapping<V,D> mapping, LensContext<F> lensContext, LensProjectionContext projContext, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
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

    // TODO: unify OutboundProcessor.evaluateMapping() with MappingEvaluator.evaluateOutboundMapping(...)
    public <T, F extends FocusType> void evaluateOutboundMapping(final LensContext<F> context,
			final LensProjectionContext projCtx, List<MappingType> outboundMappings,
			final ItemPath focusPropertyPath, final ItemPath projectionPropertyPath,
			final MappingInitializer<PrismPropertyValue<T>,PrismPropertyDefinition<T>> initializer, MappingOutputProcessor<PrismPropertyValue<T>> processor,
			XMLGregorianCalendar now, final boolean evaluateCurrent, boolean evaluateWeak,
   			String desc, final Task task, final OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

    	String projCtxDesc = projCtx.toHumanReadableString();
        PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();

        MappingInitializer<PrismPropertyValue<T>,PrismPropertyDefinition<T>> internalInitializer =
			builder -> {

				builder.addVariableDefinitions(Utils.getDefaultExpressionVariables(context, projCtx).getMap());

		        builder.originType(OriginType.OUTBOUND);
				builder.originObject(projCtx.getResource());

				initializer.initialize(builder);

				return builder;
			};

		MappingEvaluatorParams<PrismPropertyValue<T>, PrismPropertyDefinition<T>, ShadowType, F> params = new MappingEvaluatorParams<>();
		params.setMappingTypes(outboundMappings);
		params.setMappingDesc(desc + " in projection " + projCtxDesc);
		params.setNow(now);
		params.setInitializer(internalInitializer);
		params.setProcessor(processor);
		params.setTargetLoader(new ProjectionMappingLoader<>(context, projCtx, contextLoader));
		params.setAPrioriTargetObject(shadowNew);
		params.setAPrioriTargetDelta(LensUtil.findAPrioriDelta(context, projCtx));
		params.setTargetContext(projCtx);
		params.setDefaultTargetItemPath(projectionPropertyPath);
		if (context.getFocusContext() != null) {
			params.setSourceContext(context.getFocusContext().getObjectDeltaObject());
		}
		params.setEvaluateCurrent(evaluateCurrent);
		params.setEvaluateWeak(evaluateWeak);
		params.setContext(context);
		params.setHasFullTargetObject(projCtx.hasFullShadow());
		evaluateMappingSetProjection(params, task, result);
    }

	public <V extends PrismValue, D extends ItemDefinition, T extends ObjectType, F extends FocusType> Map<ItemPath,MappingOutputStruct<V>> evaluateMappingSetProjection(
			MappingEvaluatorParams<V,D,T,F> params,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

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
			String mappingName = null;
			if (mappingType.getName() != null) {
				mappingName = mappingType.getName();
			}

			if (!mappingBuilder.isApplicableToChannel(params.getContext().getChannel())) {
				LOGGER.trace("Mapping {} not applicable to channel, skipping {}", mappingName, params.getContext().getChannel());
	        	continue;
	        }

			mappingBuilder.now(params.getNow());
			if (defaultTargetItemPath != null && targetObjectDefinition != null) {
				D defaultTargetItemDef = targetObjectDefinition.findItemDefinition(defaultTargetItemPath);
				mappingBuilder.defaultTargetDefinition(defaultTargetItemDef);
				mappingBuilder.defaultTargetPath(defaultTargetItemPath);
			} else {
				mappingBuilder.defaultTargetDefinition(params.getTargetItemDefinition());
				mappingBuilder.defaultTargetPath(defaultTargetItemPath);
			}
			mappingBuilder.targetContext(targetObjectDefinition);

			if (params.getSourceContext() != null) {
				mappingBuilder.sourceContext(params.getSourceContext());
			}

			// Initialize mapping (using Inversion of Control)
			mappingBuilder = params.getInitializer().initialize(mappingBuilder);

			Mapping<V,D> mapping = mappingBuilder.build();
			Boolean timeConstraintValid = mapping.evaluateTimeConstraintValid(task, result);

			if (params.getEvaluateCurrent() != null) {
				if (params.getEvaluateCurrent() && !timeConstraintValid) {
					LOGGER.trace("Mapping {} is non-current, but evulating current mappings, skipping {}", mappingName, params.getContext().getChannel());
					continue;
				}
				if (!params.getEvaluateCurrent() && timeConstraintValid) {
					LOGGER.trace("Mapping {} is current, but evulating non-current mappings, skipping {}", mappingName, params.getContext().getChannel());
					continue;
				}
			}

			mappings.add(mapping);
		}

		boolean hasFullTargetObject = params.hasFullTargetObject();
		PrismObject<T> aPrioriTargetObject = params.getAPrioriTargetObject();

		LOGGER.trace("Going to process {} mappings for {}", mappings.size(), mappingDesc);

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

			if (isMeaningful(mappingOutputTriple)) {

				MappingOutputStruct<V> mappingOutputStruct = outputTripleMap.get(mappingOutputPath);
				if (mappingOutputStruct == null) {
					mappingOutputStruct = new MappingOutputStruct<>();
					outputTripleMap.put(mappingOutputPath, mappingOutputStruct);
				}

                if (mapping.getStrength() == MappingStrengthType.STRONG) {
                	mappingOutputStruct.setStrongMappingWasUsed(true);

                	if (!hasFullTargetObject && params.getTargetLoader() != null && aPrioriTargetObject != null && aPrioriTargetObject.getOid() != null) {
    					if (!params.getTargetLoader().isLoaded()) {
    						aPrioriTargetObject = params.getTargetLoader().load("strong mapping", task, result);
    						LOGGER.trace("Loaded object because of strong mapping: {}", aPrioriTargetObject);
    						hasFullTargetObject = true;
    					}
    				}
                }

                PrismValueDeltaSetTriple<V> outputTriple = mappingOutputStruct.getOutputTriple();
				if (outputTriple == null) {
					mappingOutputStruct.setOutputTriple(mappingOutputTriple);
				} else {
					outputTriple.merge(mappingOutputTriple);
				}

			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Output triple of mapping {} is NOT meaningful", mapping.getContextDescription());
				}
			}

		}

		if (params.isEvaluateWeak()) {
			// Second pass, evaluate only weak mappings
			for (Mapping<V,D> mapping: mappings) {

				if (mapping.getStrength() != MappingStrengthType.WEAK) {
					continue;
				}

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
				if (outputTriple != null) {
					// Previous mapping produced output. We do not need to evaluate weak mapping.
					// TODO: this is not entirely correct. Previous mapping might have deleted all
					// values. Also we may need the output of the weak mapping to correctly process
					// non-tolerant values (to avoid removing the value that weak mapping produces).
					// MID-3847
					continue;
				}

				Item<V,D> aPrioriTargetItem = null;
				if (aPrioriTargetObject != null && mappingOutputPath != null) {
					aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
				}
				if (hasNoValue(aPrioriTargetItem)) {

					mappingOutputStruct.setWeakMappingWasUsed(true);

					evaluateMapping(mapping, params.getContext(), task, result);

					PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
					if (mappingOutputTriple != null) {

						// This may be counter-intuitive to load object after the mapping is executed
						// But the mapping may not be activated (e.g. condition is false). And in that
						// case we really do not want to trigger object loading.
						// This is all not right. See MID-3847
						if (!hasFullTargetObject && params.getTargetLoader() != null && aPrioriTargetObject != null && aPrioriTargetObject.getOid() != null) {
							if (!params.getTargetLoader().isLoaded()) {
								aPrioriTargetObject = params.getTargetLoader().load("weak mapping", task, result);
								LOGGER.trace("Loaded object because of weak mapping: {}", aPrioriTargetObject);
								hasFullTargetObject = true;
							}
						}
						if (aPrioriTargetObject != null && mappingOutputPath != null) {
							aPrioriTargetItem = aPrioriTargetObject.findItem(mappingOutputPath);
						}
						if (!hasNoValue(aPrioriTargetItem)) {
							continue;
						}

						if (outputTriple == null) {
							mappingOutputStruct.setOutputTriple(mappingOutputTriple);
						} else {
							outputTriple.merge(mappingOutputTriple);
						}
					}

				}
			}
		}

		MappingOutputProcessor<V> processor = params.getProcessor();
		for (Entry<ItemPath, MappingOutputStruct<V>> outputTripleMapEntry: outputTripleMap.entrySet()) {
			ItemPath mappingOutputPath = outputTripleMapEntry.getKey();
			MappingOutputStruct<V> mappingOutputStruct = outputTripleMapEntry.getValue();
			PrismValueDeltaSetTriple<V> outputTriple = mappingOutputStruct.getOutputTriple();

			boolean defaultProcessing = true;

			if (processor != null) {
				LOGGER.trace("Executing processor to process mapping evaluation results: {}", processor);
				defaultProcessing = processor.process(mappingOutputPath, mappingOutputStruct);
			}

			if (defaultProcessing) {

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

		            if (hasFullTargetObject && mappingOutputStruct.isStrongMappingWasUsed()) {
		                valuesToReplace = outputTriple.getNonNegativeValues();
		            } else {
		                valuesToReplace = outputTriple.getPlusSet();
		            }

		            if (LOGGER.isTraceEnabled()) {
		            	LOGGER.trace("{}: hasFullTargetObject={}, isStrongMappingWasUsed={}, valuesToReplace={}",
		            			new Object[]{mappingDesc, hasFullTargetObject,
		            					mappingOutputStruct.isStrongMappingWasUsed(), valuesToReplace});
		            }

		        	if (valuesToReplace != null && !valuesToReplace.isEmpty()) {

		                // if what we want to set is the same as is already in the shadow, we skip that
		                // (we insist on having full shadow, to be sure we work with current data)

		                if (hasFullTargetObject && targetContext.isFresh() && aPrioriTargetItem != null) {
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
		
		return outputTripleMap;
	}

    private <V extends PrismValue> boolean isMeaningful(PrismValueDeltaSetTriple<V> mappingOutputTriple) {
		if (mappingOutputTriple == null) {
			// this means: mapping not applicable
			return false;
		}
		if (mappingOutputTriple.isEmpty()) {
			// this means: no value produced
			return true;
		}
		if (mappingOutputTriple.getMinusSet() != null &&
				(mappingOutputTriple.getZeroSet() == null || mappingOutputTriple.getZeroSet().isEmpty()) &&
				(mappingOutputTriple.getPlusSet() == null || mappingOutputTriple.getPlusSet().isEmpty())) {
			// Minus deltas are always meaningful, even with hashing (see below)
			// This may be used e.g. to remove existing password.
			return true;
		}
		if (hasNoOrHashedValuesOnly(mappingOutputTriple.getMinusSet()) && hasNoOrHashedValuesOnly(mappingOutputTriple.getZeroSet()) && hasNoOrHashedValuesOnly(mappingOutputTriple.getPlusSet())) {
			// Used to skip application of mapping that produces only hashed protected values.
			// Those values are useless, e.g. to set new password. If we would consider them as
			// meaningful then a normal mapping with such values may prohibit application of
			// a weak mapping. We want weak mapping in this case, e.g. to set a randomly-generated password.
			// Not entirely correct. Maybe we need to filter this out in some other way?
			return false;
		}
		return true;
	}

    // Not entirely correct. Maybe we need to filter this out in some other way?
    private <V extends PrismValue> boolean hasNoOrHashedValuesOnly(Collection<V> set) {
    	if (set == null) {
			return true;
		}
		for (V pval: set) {
			Object val = pval.getRealValue();
			if (val instanceof ProtectedStringType) {
				if (!((ProtectedStringType)val).isHashed()) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
    }

	private boolean hasNoValue(Item aPrioriTargetItem) {
		return aPrioriTargetItem == null
				|| (aPrioriTargetItem.isEmpty() && !aPrioriTargetItem.isIncomplete());
	}

	public <V extends PrismValue, D extends ItemDefinition , F extends FocusType> Mapping<V, D> createFocusMapping(final MappingFactory mappingFactory,
    		final LensContext<F> context, final MappingType mappingType, ObjectType originObject,
			ObjectDeltaObject<F> focusOdo, AssignmentPathVariables assignmentPathVariables, PrismObject<SystemConfigurationType> configuration,
			XMLGregorianCalendar now, String contextDesc, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	Integer iteration = null;
    	String iterationToken = null;
    	if (focusOdo.getNewObject() != null) {
    		F focusNewType = focusOdo.getNewObject().asObjectable();
    		iteration = focusNewType.getIteration();
    		iterationToken = focusNewType.getIterationToken();
    	} else if (focusOdo.getOldObject() != null) {
    		F focusOldType = focusOdo.getOldObject().asObjectable();
    		iteration = focusOldType.getIteration();
    		iterationToken = focusOldType.getIterationToken();
    	}
    	return createFocusMapping(mappingFactory, context, mappingType, originObject, focusOdo, focusOdo.getAnyObject(), assignmentPathVariables,
    			iteration, iterationToken, configuration, now, contextDesc, task, result);
    }

    public <V extends PrismValue, D extends ItemDefinition, F extends FocusType, T extends FocusType> Mapping<V, D> createFocusMapping(
    		final MappingFactory mappingFactory, final LensContext<F> context, final MappingType mappingType, ObjectType originObject,
			ObjectDeltaObject<F> focusOdo, PrismObject<T> defaultTargetObject, AssignmentPathVariables assignmentPathVariables,
			Integer iteration, String iterationToken, PrismObject<SystemConfigurationType> configuration,
			XMLGregorianCalendar now, String contextDesc, final Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		if (!Mapping.isApplicableToChannel(mappingType, context.getChannel())) {
			LOGGER.trace("Mapping {} not applicable to channel {}, skipping.", mappingType, context.getChannel());
			return null;
		}

		ValuePolicyResolver stringPolicyResolver = new ValuePolicyResolver() {
			private ItemPath outputPath;
			private ItemDefinition outputDefinition;
			@Override
			public void setOutputPath(ItemPath outputPath) {
				this.outputPath = outputPath;
			}

			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
				this.outputDefinition = outputDefinition;
			}

			@Override
			public ValuePolicyType resolve() {
				// TODO need to switch to ObjectValuePolicyEvaluator
				if (outputDefinition.getName().equals(PasswordType.F_VALUE)) {
					ValuePolicyType passwordPolicy = credentialsProcessor.determinePasswordPolicy(context.getFocusContext(), task, result);
					if (passwordPolicy == null) {
						return null;
					}
					return passwordPolicy;
				}
				if (mappingType.getExpression() != null){
					List<JAXBElement<?>> evaluators = mappingType.getExpression().getExpressionEvaluator();
					if (evaluators != null) {
						for (JAXBElement jaxbEvaluator : evaluators) {
							Object object = jaxbEvaluator.getValue();
							if (object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null) {
								ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
								try {
									ValuePolicyType valuePolicyType = mappingFactory.getObjectResolver().resolve(ref, ValuePolicyType.class,
											null, "resolving value policy for generate attribute "+ outputDefinition.getName()+" value", task, new OperationResult("Resolving value policy"));
									if (valuePolicyType != null) {
										return valuePolicyType;
									}
								} catch (CommonException ex) {
									throw new SystemException(ex.getMessage(), ex);
								}
							}
						}

					}
				}
				return null;

			}
		};

		ExpressionVariables variables = new ExpressionVariables();
		FOCUS_VARIABLE_NAMES.forEach(name -> variables.addVariableDefinition(name, focusOdo));
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION, iteration);
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken);
		variables.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, configuration);
		variables.addVariableDefinition(ExpressionConstants.VAR_OPERATION, context.getFocusContext().getOperation().getValue());

		Collection<V> targetValues = computeTargetValues(mappingType.getTarget(), defaultTargetObject, variables, mappingFactory.getObjectResolver(), contextDesc, task, result);

		Mapping.Builder<V,D> mappingBuilder = mappingFactory.<V,D>createMappingBuilder(mappingType, contextDesc)
				.sourceContext(focusOdo)
				.targetContext(defaultTargetObject.getDefinition())
				.variables(variables)
				.originalTargetValues(targetValues)
				.originType(OriginType.USER_POLICY)
				.originObject(originObject)
				.valuePolicyResolver(stringPolicyResolver)
				.rootNode(focusOdo)
				.now(now);

		mappingBuilder = LensUtil.addAssignmentPathVariables(mappingBuilder, assignmentPathVariables);

		Mapping<V,D> mapping = mappingBuilder.build();

		ItemPath itemPath = mapping.getOutputPath();
        if (itemPath == null) {
            // no output element, i.e. this is a "validation mapping"
            return mapping;
        }

		if (defaultTargetObject != null) {
			Item<V,D> existingTargetItem = (Item<V,D>) defaultTargetObject.findItem(itemPath);
			if (existingTargetItem != null && !existingTargetItem.isEmpty()
					&& mapping.getStrength() == MappingStrengthType.WEAK) {
				LOGGER.trace("Mapping {} is weak and target already has a value {}, skipping.", mapping, existingTargetItem);
				return null;
			}
		}

		return mapping;
	}

    private <V extends PrismValue, F extends FocusType> Collection<V> computeTargetValues(VariableBindingDefinitionType target,
			Object defaultTargetContext, ExpressionVariables variables, ObjectResolver objectResolver, String contextDesc,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (target == null) {
			// Is this correct? What about default targets?
			return null;
		}

		ItemPathType itemPathType = target.getPath();
		if (itemPathType == null) {
			// Is this correct? What about default targets?
			return null;
		}
		ItemPath path = itemPathType.getItemPath();

		Object object = ExpressionUtil.resolvePath(path, variables, defaultTargetContext, objectResolver, contextDesc, task, result);
		if (object == null) {
			return new ArrayList<>();
		} else if (object instanceof Item) {
			return ((Item) object).getValues();
		} else if (object instanceof PrismValue) {
			return (List<V>) Collections.singletonList((PrismValue) object);
		} else if (object instanceof ItemDeltaItem) {
			ItemDeltaItem<V, ?> idi = (ItemDeltaItem<V, ?>) object;
			PrismValueDeltaSetTriple<V> triple = idi.toDeltaSetTriple();
			return triple != null ? triple.getNonNegativeValues() : new ArrayList<V>();
		} else {
			throw new IllegalStateException("Unsupported target value(s): " + object.getClass() + " (" + object + ")");
		}
	}

}
