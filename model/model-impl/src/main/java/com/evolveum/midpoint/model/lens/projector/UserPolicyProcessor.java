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
package com.evolveum.midpoint.model.lens.projector;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.ModelObjectResolver;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.trigger.RecomputeTriggerHandler;
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
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * Processor to handle user template and possible also other user "policy"
 * elements.
 * 
 * @author Radovan Semancik
 * 
 */
@Component
public class UserPolicyProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(UserPolicyProcessor.class);

	private PrismObjectDefinition<UserType> userDefinition;
	private PrismContainerDefinition<ActivationType> activationDefinition;
	
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
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
    private MappingEvaluationHelper mappingHelper;

	<F extends ObjectType, P extends ObjectType> void processUserPolicy(LensContext<F,P> context, XMLGregorianCalendar now, 
			OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, PolicyViolationException {

		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	
    	ObjectDelta<F> focusDelta = focusContext.getDelta();
    	if (focusDelta != null && focusDelta.isDelete()) {
    		return;
    	}
    	
		LensContext<UserType, ShadowType> usContext = (LensContext<UserType, ShadowType>) context;
    	//check user password if satisfies policies
		
//		PrismProperty<PasswordType> password = getPasswordValue((LensFocusContext<UserType>)focusContext);
		
//		if (password != null) {
			passwordPolicyProcessor.processPasswordPolicy((LensFocusContext<UserType>) focusContext, usContext, result);
//		}
			
		processActivation(usContext, now, result);

		applyUserTemplate(usContext, now, result);
				
	}

	private <F extends UserType> void processActivation(LensContext<F, ShadowType> context, XMLGregorianCalendar now, 
			OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		
		if (focusContext.isDelete()) {
			return;
		}
		
		TimeIntervalStatusType validityStatusNew = null;
		TimeIntervalStatusType validityStatusOld = null;
		XMLGregorianCalendar validityChangeTimestamp = null;
		
		PrismObject<F> focusNew = focusContext.getObjectNew();
		F focusNewType = focusNew.asObjectable();
		
		ActivationType activationNew = focusNewType.getActivation();
		ActivationType activationOld = null;
		
		if (activationNew != null) {
			validityStatusNew = activationComputer.getValidityStatus(activationNew, now);
			validityChangeTimestamp = activationNew.getValidityChangeTimestamp();
		}
		
		PrismObject<F> focusOld = focusContext.getObjectOld();
		if (focusOld != null) {
			F focusOldType = focusOld.asObjectable();
			activationOld = focusOldType.getActivation();
			if (activationOld != null) {
				validityStatusOld = activationComputer.getValidityStatus(activationOld, validityChangeTimestamp);
			}
		}
		
		if (validityStatusOld == validityStatusNew) {
			// No change, (almost) no work
			if (validityStatusNew != null && activationNew.getValidityStatus() == null) {
				// There was no validity change. But the status is not recorded. So let's record it so it can be used in searches. 
				recordValidityDelta(focusContext, validityStatusNew, now);
			} else {
				LOGGER.trace("Skipping validity processing because there was no change ({} -> {})", validityStatusOld, validityStatusNew);
			}
		} else {
			LOGGER.trace("Validity change {} -> {}", validityStatusOld, validityStatusNew);
			recordValidityDelta(focusContext, validityStatusNew, now);
		}
		
		ActivationStatusType effectiveStatusNew = activationComputer.getEffectiveStatus(activationNew, validityStatusNew);
		ActivationStatusType effectiveStatusOld = activationComputer.getEffectiveStatus(activationOld, validityStatusOld);
		
		if (effectiveStatusOld == effectiveStatusNew) {
			// No change, (almost) no work
			if (effectiveStatusNew != null && (activationNew == null || activationNew.getEffectiveStatus() == null)) {
				// There was no effective status change. But the status is not recorded. So let's record it so it can be used in searches. 
				recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
			} else {
				if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().hasItemDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
					LOGGER.trace("Forcing effective status delta even though there was no change ({} -> {}) because there is explicit administrativeStatus delta", effectiveStatusOld, effectiveStatusNew);
					// We need this to force the change down to the projections later in the activation processor
					// some of the mappings will use effectiveStatus as a source, therefore there has to be a delta for the mapping to work correctly
					recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
				} else {
					LOGGER.trace("Skipping effective status processing because there was no change ({} -> {})", effectiveStatusOld, effectiveStatusNew);
				}
			}
		} else {
			LOGGER.trace("Effective status change {} -> {}", effectiveStatusOld, effectiveStatusNew);
			recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
		}
	}
	
	private <F extends UserType> void recordValidityDelta(LensFocusContext<F> focusContext, TimeIntervalStatusType validityStatusNew,
			XMLGregorianCalendar now) throws SchemaException {
		PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
		
		PrismPropertyDefinition<TimeIntervalStatusType> validityStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_STATUS);
		PropertyDelta<TimeIntervalStatusType> validityStatusDelta 
				= validityStatusDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS));
		if (validityStatusNew == null) {
			validityStatusDelta.setValueToReplace();
		} else {
			validityStatusDelta.setValueToReplace(new PrismPropertyValue<TimeIntervalStatusType>(validityStatusNew, OriginType.USER_POLICY, null));
		}
		focusContext.swallowToProjectionWaveSecondaryDelta(validityStatusDelta);
		
		PrismPropertyDefinition<XMLGregorianCalendar> validityChangeTimestampDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP);
		PropertyDelta<XMLGregorianCalendar> validityChangeTimestampDelta 
				= validityChangeTimestampDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP));
		validityChangeTimestampDelta.setValueToReplace(new PrismPropertyValue<XMLGregorianCalendar>(now, OriginType.USER_POLICY, null));
		focusContext.swallowToProjectionWaveSecondaryDelta(validityChangeTimestampDelta);
	}
	
	private <F extends UserType> void recordEffectiveStatusDelta(LensFocusContext<F> focusContext, 
			ActivationStatusType effectiveStatusNew, XMLGregorianCalendar now)
			throws SchemaException {
		PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
		
		PrismPropertyDefinition<ActivationStatusType> effectiveStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_EFFECTIVE_STATUS);
		PropertyDelta<ActivationStatusType> effectiveStatusDelta 
				= effectiveStatusDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		effectiveStatusDelta.setValueToReplace(new PrismPropertyValue<ActivationStatusType>(effectiveStatusNew, OriginType.USER_POLICY, null));
		focusContext.swallowToProjectionWaveSecondaryDelta(effectiveStatusDelta);
		
		PropertyDelta<XMLGregorianCalendar> timestampDelta = LensUtil.createActivationTimestampDelta(effectiveStatusNew, now, activationDefinition, OriginType.USER_POLICY);
		focusContext.swallowToProjectionWaveSecondaryDelta(timestampDelta);
	}
	
	private void applyUserTemplate(LensContext<UserType, ShadowType> context, XMLGregorianCalendar now, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();

		ObjectTemplateType userTemplate = context.getUserTemplate();

		if (userTemplate == null) {
			// No applicable template
			LOGGER.trace("Skipping processing of user template: no user template");
			return;
		}
		
		LOGGER.trace("Applying " + userTemplate + " to " + focusContext.getObjectNew());

		ObjectDelta<UserType> userSecondaryDelta = focusContext.getProjectionWaveSecondaryDelta();
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getProjectionWavePrimaryDelta();
		ObjectDeltaObject<UserType> userOdo = focusContext.getObjectDeltaObject();
		PrismObjectDefinition<UserType> userDefinition = getUserDefinition();
		
		Map<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap 
			= new HashMap<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>>();
		
		XMLGregorianCalendar nextRecomputeTime = collectTripleFromTemplate(context, userTemplate, userOdo, outputTripleMap, now, userTemplate.toString(), result);
		
		for (Entry<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> entry: outputTripleMap.entrySet()) {
			ItemPath itemPath = entry.getKey();
			DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>> outputTriple = entry.getValue();
			
			ItemDelta<? extends PrismValue> apropriItemDelta = null;
			
			ItemDelta<? extends PrismValue> itemDelta = LensUtil.consolidateTripleToDelta(itemPath, (DeltaSetTriple)outputTriple,
					userDefinition.findItemDefinition(itemPath), apropriItemDelta, userOdo.getNewObject(), null, 
					true, true, "user template "+userTemplate, true);
			
			itemDelta.simplify();
			itemDelta.validate("user template "+userTemplate);
			
			if (itemDelta != null && !itemDelta.isEmpty()) {
				if (userPrimaryDelta == null || !userPrimaryDelta.containsModification(itemDelta)) {
					if (userSecondaryDelta == null) {
						userSecondaryDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, prismContext);
						if (focusContext.getObjectNew() != null && focusContext.getObjectNew().getOid() != null){
							userSecondaryDelta.setOid(focusContext.getObjectNew().getOid());
						}
						focusContext.setProjectionWaveSecondaryDelta(userSecondaryDelta);
					}
					userSecondaryDelta.mergeModification(itemDelta);
				}
			}
		}
		
		if (nextRecomputeTime != null) {
			
			boolean alreadyHasTrigger = false;
			PrismObject<UserType> objectCurrent = focusContext.getObjectCurrent();
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
				PrismObjectDefinition<UserType> objectDefinition = focusContext.getObjectDefinition();
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

	private XMLGregorianCalendar collectTripleFromTemplate(LensContext<UserType, ShadowType> context,
			ObjectTemplateType objectTemplateType, ObjectDeltaObject<UserType> userOdo,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
			XMLGregorianCalendar now, String contextDesc, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		XMLGregorianCalendar nextRecomputeTime = null;
		
		// Process includes
		for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
			PrismObject<ObjectTemplateType> includeObject = includeRef.asReferenceValue().getObject();
			if (includeObject == null) {
				ObjectTemplateType includeObjectType = modelObjectResolver.resolve(includeRef, ObjectTemplateType.class, "include reference in "+objectTemplateType + " in " + contextDesc, result);
				includeObject = includeObjectType.asPrismObject();
				// Store resolved object for future use (e.g. next waves).
				includeRef.asReferenceValue().setObject(includeObject);
			}
			LOGGER.trace("Including template {}", includeObject);
			ObjectTemplateType includeObjectType = includeObject.asObjectable();
			XMLGregorianCalendar includeNextRecomputeTime = collectTripleFromTemplate(context, includeObjectType, userOdo, outputTripleMap, now, "include "+includeObject+" in "+objectTemplateType + " in " + contextDesc, result);
			if (includeNextRecomputeTime != null) {
				if (nextRecomputeTime == null || nextRecomputeTime.compare(includeNextRecomputeTime) == DatatypeConstants.GREATER) {
					nextRecomputeTime = includeNextRecomputeTime;
				}
			}
		}
		
		// Process own mappings
		Collection<MappingType> mappings = objectTemplateType.getMapping();
		XMLGregorianCalendar templateNextRecomputeTime = collectTripleFromMappings(mappings, context, objectTemplateType, userOdo, outputTripleMap, now, contextDesc, result);
		if (templateNextRecomputeTime != null) {
			if (nextRecomputeTime == null || nextRecomputeTime.compare(templateNextRecomputeTime) == DatatypeConstants.GREATER) {
				nextRecomputeTime = templateNextRecomputeTime;
			}
		}
		
		return nextRecomputeTime;
	}
	
	
	private <V extends PrismValue> XMLGregorianCalendar collectTripleFromMappings(Collection<MappingType> mappings, LensContext<UserType, ShadowType> context,
			ObjectTemplateType objectTemplateType, ObjectDeltaObject<UserType> userOdo,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
			XMLGregorianCalendar now, String contextDesc, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		XMLGregorianCalendar nextRecomputeTime = null;
		
		for (MappingType mappingType : mappings) {
			Mapping<V> mapping = createMapping(context, mappingType, objectTemplateType, userOdo, now, contextDesc, result);
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
			
			LensUtil.evaluateMapping(mapping, context, result);
			
			ItemPath itemPath = mapping.getOutputPath();
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
	
	private <V extends PrismValue> Mapping<V> createMapping(final LensContext<UserType, ShadowType> context, final MappingType mappingType, ObjectTemplateType userTemplate, 
			ObjectDeltaObject<UserType> userOdo, XMLGregorianCalendar now, String contextDesc, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		Mapping<V> mapping = mappingFactory.createMapping(mappingType,
				"object template mapping in " + contextDesc
				+ " while processing user " + userOdo.getAnyObject());
		
		if (!mapping.isApplicableToChannel(context.getChannel())) {
			return null;
		}
		
		mapping.setSourceContext(userOdo);
		mapping.setTargetContext(getUserDefinition());
		mapping.setRootNode(userOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, userOdo);
		mapping.setOriginType(OriginType.USER_POLICY);
		mapping.setOriginObject(userTemplate);
		mapping.setNow(now);

		ItemDefinition outputDefinition = mapping.getOutputDefinition();
		ItemPath itemPath = mapping.getOutputPath();
		
		Item<V> existingUserItem = (Item<V>) userOdo.getNewObject().findItem(itemPath);
		if (existingUserItem != null && !existingUserItem.isEmpty() 
				&& mapping.getStrength() == MappingStrengthType.WEAK) {
			// This valueConstruction only applies if the property does not have a value yet.
			// ... but it does
			return null;
		}

		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
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
			public StringPolicyType resolve() {
				if (outputDefinition.getName().equals(PasswordType.F_VALUE)) {
					ValuePolicyType passwordPolicy = context.getGlobalPasswordPolicy();
					if (passwordPolicy == null) {
						return null;
					}
					return passwordPolicy.getStringPolicy();
				}
				if (mappingType.getExpression() != null){
					List<JAXBElement<?>> evaluators = mappingType.getExpression().getExpressionEvaluator();
					if (evaluators != null){
						for (JAXBElement jaxbEvaluator : evaluators){
							Object object = jaxbEvaluator.getValue();
							if (object != null && object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null){
								ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
								try{
								ValuePolicyType valuePolicyType = mappingFactory.getObjectResolver().resolve(ref, ValuePolicyType.class, "resolving value policy for generate attribute "+ outputDefinition.getName()+"value", new OperationResult("Resolving value policy"));
								if (valuePolicyType != null){
									return valuePolicyType.getStringPolicy();
								}
								} catch (Exception ex){
									throw new SystemException(ex.getMessage(), ex);
								}
							}
						}
						
					}
				}
				return null;
				
			}
		};
		mapping.setStringPolicyResolver(stringPolicyResolver);

		return mapping;
	}

	private <V extends PrismValue> boolean hasValue(Item<V> existingUserItem, V newValue) {
		if (existingUserItem == null) {
			return false;
		}
		return existingUserItem.contains(newValue, true);
	}


	private PrismObjectDefinition<UserType> getUserDefinition() {
		if (userDefinition == null) {
			userDefinition = prismContext.getSchemaRegistry().getObjectSchema()
				.findObjectDefinitionByCompileTimeClass(UserType.class);
		}
		return userDefinition;
	}
	
	private PrismContainerDefinition<ActivationType> getActivationDefinition() {
		if (activationDefinition == null) {
			PrismObjectDefinition<UserType> userDefinition = getUserDefinition();
			activationDefinition = userDefinition.findContainerDefinition(UserType.F_ACTIVATION);
		}
		return activationDefinition;
	}

}
