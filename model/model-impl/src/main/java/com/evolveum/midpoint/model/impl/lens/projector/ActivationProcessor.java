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

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.SynchronizationIntent;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationValidityCapabilityType;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The processor that takes care of user activation mapping to an account (outbound direction).
 * 
 * @author Radovan Semancik
 */
@Component
public class ActivationProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ActivationProcessor.class);

	private static final QName SHADOW_EXISTS_PROPERTY_NAME = new QName(SchemaConstants.NS_C, "shadowExists");
	private static final QName LEGAL_PROPERTY_NAME = new QName(SchemaConstants.NS_C, "legal");
	private static final QName FOCUS_EXISTS_PROPERTY_NAME = new QName(SchemaConstants.NS_C, "focusExists");
	
	private PrismObjectDefinition<UserType> userDefinition;
	private PrismContainerDefinition<ActivationType> activationDefinition;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingEvaluationHelper mappingHelper;

    public <O extends ObjectType, F extends FocusType> void processActivation(LensContext<O> context,
    		LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
    	
    	LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext != null && !FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for user.
//    		processActivationMetadata(context, projectionContext, now, result);
    		return;
    	}
    	
    	processActivationFocal((LensContext<F>)context, projectionContext, now, task, result);
    }
    
    private <F extends FocusType> void processActivationFocal(LensContext<F> context, 
    		LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		processActivationMetadata(context, projectionContext, now, result);
    		return;
    	}
    	processActivationUserCurrent(context, projectionContext, now, task, result);
    	processActivationMetadata(context, projectionContext, now, result);
    	processActivationUserFuture(context, projectionContext, now, task, result);
    }

    public <F extends FocusType> void processActivationUserCurrent(LensContext<F> context, LensProjectionContext accCtx, 
    		XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {

    	String accCtxDesc = accCtx.toHumanReadableString();
    	SynchronizationPolicyDecision decision = accCtx.getSynchronizationPolicyDecision();
    	SynchronizationIntent synchronizationIntent = accCtx.getSynchronizationIntent();
    	
    	if (decision == SynchronizationPolicyDecision.BROKEN) {
    		LOGGER.trace("Broken projection {}, skipping further activation processing", accCtxDesc);
    		return;
    	}
    	if (decision != null) {
    		throw new IllegalStateException("Decision "+decision+" already present for projection "+accCtxDesc);
    	}
    	
    	if (accCtx.isThombstone() || synchronizationIntent == SynchronizationIntent.UNLINK) {
    		accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.UNLINK);
    		LOGGER.trace("Evaluated decision for {} to {}, skipping further activation processing", accCtxDesc, SynchronizationPolicyDecision.UNLINK);
    		return;
    	}
    	
    	if (synchronizationIntent == SynchronizationIntent.DELETE || accCtx.isDelete()) {
    		// TODO: is this OK?
    		accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.DELETE);
    		LOGGER.trace("Evaluated decision for {} to {}, skipping further activation processing", accCtxDesc, SynchronizationPolicyDecision.DELETE);
    		return;
    	}
    	    	
    	boolean shadowShouldExist = evaluateExistenceMapping(context, accCtx, now, true, task, result);
    	
    	LOGGER.trace("Evaluated intended existence of projection {} to {}", accCtxDesc, shadowShouldExist);
    	
    	// Let's reconcile the existence intent (shadowShouldExist) and the synchronization intent in the context

    	LensProjectionContext lowerOrderContext = LensUtil.findLowerOrderContext(context, accCtx);
    	
    	if (synchronizationIntent == null || synchronizationIntent == SynchronizationIntent.SYNCHRONIZE) {
	    	if (shadowShouldExist) {
	    		accCtx.setActive(true);
	    		if (accCtx.isExists()) {
	    			if (lowerOrderContext != null && lowerOrderContext.isDelete()) {
    					// HACK HACK HACK
    					decision = SynchronizationPolicyDecision.DELETE;
    				} else {
    					decision = SynchronizationPolicyDecision.KEEP;
    				}
	    		} else {
	    			if (lowerOrderContext != null) {
	    				if (lowerOrderContext.isDelete()) {
	    					// HACK HACK HACK
	    					decision = SynchronizationPolicyDecision.DELETE;
	    				} else {
		    				// If there is a lower-order context then that one will be ADD
		    				// and this one is KEEP. When the execution comes to this context
		    				// then the projection already exists
		    				decision = SynchronizationPolicyDecision.KEEP;
	    				}
	    			} else {
	    				decision = SynchronizationPolicyDecision.ADD;
	    			}
	    		}
	    	} else {
	    		// Delete
	    		if (accCtx.isExists()) {
	    			decision = SynchronizationPolicyDecision.DELETE;
	    		} else {
	    			// we should delete the entire context, but then we will lost track of what
	    			// happened. So just ignore it.
	    			decision = SynchronizationPolicyDecision.IGNORE;
	    			// if there are any triggers then move them to focus. We may still need them.
	    			LensUtil.moveTriggers(accCtx, context.getFocusContext());
	    		}
	    	}
	    	
    	} else if (synchronizationIntent == SynchronizationIntent.ADD) {
    		if (shadowShouldExist) {
    			accCtx.setActive(true);
    			if (accCtx.isExists()) {
    				// Attempt to add something that is already there, but should be OK
	    			decision = SynchronizationPolicyDecision.KEEP;
	    		} else {
	    			decision = SynchronizationPolicyDecision.ADD;
	    		}
    		} else {
    			throw new PolicyViolationException("Request to add projection "+accCtxDesc+" but the activation policy decided that it should not exist");
    		}
    		
    	} else if (synchronizationIntent == SynchronizationIntent.KEEP) {
	    	if (shadowShouldExist) {
	    		accCtx.setActive(true);
	    		if (accCtx.isExists()) {
	    			decision = SynchronizationPolicyDecision.KEEP;
	    		} else {
	    			decision = SynchronizationPolicyDecision.ADD;
	    		}
	    	} else {
	    		throw new PolicyViolationException("Request to keep projection "+accCtxDesc+" but the activation policy decided that it should not exist");
	    	}
    		
    	} else {
    		throw new IllegalStateException("Unknown sync intent "+synchronizationIntent);
    	}
    	
    	LOGGER.trace("Evaluated decision for projection {} to {}", accCtxDesc, decision);
    	
    	accCtx.setSynchronizationPolicyDecision(decision);
    	
        PrismObject<F> focusNew = context.getFocusContext().getObjectNew();
        if (focusNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("focusNew is null, skipping activation processing of {}", accCtxDesc);
            return;
        }
    	
    	if (decision == SynchronizationPolicyDecision.UNLINK || decision == SynchronizationPolicyDecision.DELETE) {
    		LOGGER.trace("Decision is {}, skipping activation properties processing for {}", decision, accCtxDesc);
    		return;
    	}
    	
    	ResourceObjectTypeDefinitionType resourceAccountDefType = accCtx.getResourceObjectTypeDefinitionType();
        if (resourceAccountDefType == null) {
            LOGGER.trace("No refined object definition, therefore also no activation outbound definition, skipping activation processing for account " + accCtxDesc);
            return;
        }
        ResourceActivationDefinitionType activationType = resourceAccountDefType.getActivation();
        if (activationType == null) {
            LOGGER.trace("No activation definition in projection {}, skipping activation properties processing", accCtxDesc);
            return;
        }
        
        ActivationCapabilityType capActivation = ResourceTypeUtil.getEffectiveCapability(accCtx.getResource(), ActivationCapabilityType.class);
        if (capActivation == null) {
        	LOGGER.trace("Skipping activation status and validity processing because {} has no activation capability", accCtx.getResource());
        	return;
        }

        ActivationStatusCapabilityType capStatus = capActivation.getStatus();
        ActivationValidityCapabilityType capValidFrom = capActivation.getValidFrom();
        ActivationValidityCapabilityType capValidTo = capActivation.getValidTo();
        
        if (capStatus != null) {
	    	evaluateActivationMapping(context, accCtx,
	    			activationType.getAdministrativeStatus(),
	    			SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
	    			capActivation, now, true, ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart(), task, result);
        } else {
        	LOGGER.trace("Skipping activation status processing because {} does not have activation status capability", accCtx.getResource());
        }

        ResourceBidirectionalMappingType validFromMappingType = activationType.getValidFrom();
        if (validFromMappingType == null || validFromMappingType.getOutbound() == null) {
        	LOGGER.trace("Skipping activation validFrom processing because {} does not have appropriate outbound mapping", accCtx.getResource());
        } else if (capValidFrom == null && !ExpressionUtil.hasExplicitTarget(validFromMappingType.getOutbound())) {
        	LOGGER.trace("Skipping activation validFrom processing because {} does not have activation validFrom capability nor outbound mapping with explicit target", accCtx.getResource());
        } else {
	    	evaluateActivationMapping(context, accCtx, activationType.getValidFrom(),
	    			SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM, 
	    			null, now, true, ActivationType.F_VALID_FROM.getLocalPart(), task, result);
        }
	
        ResourceBidirectionalMappingType validToMappingType = activationType.getValidTo();
        if (validToMappingType == null || validToMappingType.getOutbound() == null) {
        	LOGGER.trace("Skipping activation validTo processing because {} does not have appropriate outbound mapping", accCtx.getResource());
        } else if (capValidFrom == null && !ExpressionUtil.hasExplicitTarget(validToMappingType.getOutbound())) {
        	LOGGER.trace("Skipping activation validTo processing because {} does not have activation validTo capability nor outbound mapping with explicit target", accCtx.getResource());
        } else {
	    	evaluateActivationMapping(context, accCtx, activationType.getValidTo(),
	    			SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO, 
	    			null, now, true, ActivationType.F_VALID_TO.getLocalPart(), task, result);
	    }
    	
    }

    public <F extends FocusType> void processActivationMetadata(LensContext<F> context, LensProjectionContext accCtx, 
    		XMLGregorianCalendar now, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
    	ObjectDelta<ShadowType> projDelta = accCtx.getDelta();
    	if (projDelta == null) {
    		return;
    	}
    	
    	PropertyDelta<ActivationStatusType> statusDelta = projDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
    	
    	if (statusDelta != null && !statusDelta.isDelete()) {
    		// timestamps
    		PrismProperty<ActivationStatusType> statusPropNew = (PrismProperty<ActivationStatusType>) statusDelta.getItemNew();
    		ActivationStatusType statusNew = statusPropNew.getRealValue();
			PropertyDelta<XMLGregorianCalendar> timestampDelta = LensUtil.createActivationTimestampDelta(statusNew,
					now, getActivationDefinition(), OriginType.OUTBOUND);
    		accCtx.swallowToSecondaryDelta(timestampDelta);
    		
    		// disableReason
    		if (statusNew == ActivationStatusType.DISABLED) {
    			PropertyDelta<String> disableReasonDelta = projDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_DISABLE_REASON);
    			if (disableReasonDelta == null) {
    				String disableReason = null;
    				ObjectDelta<ShadowType> projPrimaryDelta = accCtx.getPrimaryDelta();
    				ObjectDelta<ShadowType> projSecondaryDelta = accCtx.getSecondaryDelta();
    				if (projPrimaryDelta != null 
    						&& projPrimaryDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS) != null
    						&& (projSecondaryDelta == null || projSecondaryDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS) == null)) {
    						disableReason = SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT;
    				} else if (accCtx.isLegal()) {
						disableReason = SchemaConstants.MODEL_DISABLE_REASON_MAPPED;
					} else {
						disableReason = SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION;
					}
    				
    				PrismPropertyDefinition<String> disableReasonDef = activationDefinition.findPropertyDefinition(ActivationType.F_DISABLE_REASON);
    				disableReasonDelta = disableReasonDef.createEmptyDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON));
    				disableReasonDelta.setValueToReplace(new PrismPropertyValue<String>(disableReason, OriginType.OUTBOUND, null));
    				accCtx.swallowToSecondaryDelta(disableReasonDelta);
    			}
    		}
    	}
    	
    }
    
    public <F extends FocusType> void processActivationUserFuture(LensContext<F> context, LensProjectionContext accCtx,
    		XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
    	String accCtxDesc = accCtx.toHumanReadableString();
    	SynchronizationPolicyDecision decision = accCtx.getSynchronizationPolicyDecision();
    	SynchronizationIntent synchronizationIntent = accCtx.getSynchronizationIntent();
    	
    	if (accCtx.isThombstone() || decision == SynchronizationPolicyDecision.BROKEN 
    			|| decision == SynchronizationPolicyDecision.IGNORE 
    			|| decision == SynchronizationPolicyDecision.UNLINK || decision == SynchronizationPolicyDecision.DELETE) {
    		return;
    	}
    	
    	accCtx.recompute();
    	
    	evaluateExistenceMapping(context, accCtx, now, false, task, result);
    	
        PrismObject<F> focusNew = context.getFocusContext().getObjectNew();
        if (focusNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("focusNew is null, skipping activation processing of {}", accCtxDesc);
            return;
        }
    	    	
    	ResourceObjectTypeDefinitionType resourceAccountDefType = accCtx.getResourceObjectTypeDefinitionType();
        if (resourceAccountDefType == null) {
            return;
        }
        ResourceActivationDefinitionType activationType = resourceAccountDefType.getActivation();
        if (activationType == null) {
            return;
        }
        
        ActivationCapabilityType capActivation = ResourceTypeUtil.getEffectiveCapability(accCtx.getResource(), ActivationCapabilityType.class);
        if (capActivation == null) {
        	return;
        }

        ActivationStatusCapabilityType capStatus = capActivation.getStatus();
        ActivationValidityCapabilityType capValidFrom = capActivation.getValidFrom();
        ActivationValidityCapabilityType capValidTo = capActivation.getValidTo();
        
        if (capStatus != null) {
        	
	    	evaluateActivationMapping(context, accCtx, activationType.getAdministrativeStatus(),
	    			SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
	    			capActivation, now, false, ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart(), task, result);	    	
        }

        if (capValidFrom != null) {
	    	evaluateActivationMapping(context, accCtx, activationType.getAdministrativeStatus(),
	    			SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM, 
	    			null, now, false, ActivationType.F_VALID_FROM.getLocalPart(), task, result);
        }
	
        if (capValidTo != null) {
	    	evaluateActivationMapping(context, accCtx, activationType.getAdministrativeStatus(),
	    			SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO, 
	    			null, now, false, ActivationType.F_VALID_FROM.getLocalPart(), task, result);
	    }
    	
    }

    
    private <F extends FocusType> boolean evaluateExistenceMapping(final LensContext<F> context,
    		final LensProjectionContext accCtx, final XMLGregorianCalendar now, final boolean current,
            Task task, final OperationResult result)
    				throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	final String accCtxDesc = accCtx.toHumanReadableString();
    	
    	final Boolean legal = accCtx.isLegal();
    	if (legal == null) {
    		throw new IllegalStateException("Null 'legal' for "+accCtxDesc);
    	}
    	
    	ResourceObjectTypeDefinitionType resourceAccountDefType = accCtx.getResourceObjectTypeDefinitionType();
        if (resourceAccountDefType == null) {
            return legal;
        }
        ResourceActivationDefinitionType activationType = resourceAccountDefType.getActivation();
        if (activationType == null) {
            return legal;
        }
        ResourceBidirectionalMappingType existenceType = activationType.getExistence();
        if (existenceType == null) {
            return legal;
        }
        List<MappingType> outbound = existenceType.getOutbound();
        if (outbound == null || outbound.isEmpty()) {
        	// "default mapping"
            return legal;
        }
        
        MappingInitializer<PrismPropertyValue<Boolean>> initializer = new MappingInitializer<PrismPropertyValue<Boolean>>() {
			@Override
			public void initialize(Mapping<PrismPropertyValue<Boolean>> existenceMapping) throws SchemaException {
				// Source: legal
		        ItemDeltaItem<PrismPropertyValue<Boolean>> legalSourceIdi = getLegalIdi(accCtx); 
		        Source<PrismPropertyValue<Boolean>> legalSource 
		        	= new Source<PrismPropertyValue<Boolean>>(legalSourceIdi, ExpressionConstants.VAR_LEGAL);
				existenceMapping.setDefaultSource(legalSource);

				// Source: focusExists
		        ItemDeltaItem<PrismPropertyValue<Boolean>> focusExistsSourceIdi = getFocusExistsIdi(context.getFocusContext()); 
		        Source<PrismPropertyValue<Boolean>> focusExistsSource 
		        	= new Source<PrismPropertyValue<Boolean>>(focusExistsSourceIdi, ExpressionConstants.VAR_FOCUS_EXISTS);
				existenceMapping.addSource(focusExistsSource);
				
				// Variable: focus
				existenceMapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, context.getFocusContext().getObjectDeltaObject());

		        // Variable: user (for convenience, same as "focus")
				existenceMapping.addVariableDefinition(ExpressionConstants.VAR_USER, context.getFocusContext().getObjectDeltaObject());
				
				// Variable: shadow
				existenceMapping.addVariableDefinition(ExpressionConstants.VAR_SHADOW, accCtx.getObjectDeltaObject());
				
				// Variable: resource
				existenceMapping.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, accCtx.getResource());
				
		        existenceMapping.setOriginType(OriginType.OUTBOUND);
		        existenceMapping.setOriginObject(accCtx.getResource());				
            }

        };
        
        final MutableBoolean output = new MutableBoolean(false);
        
        MappingOutputProcessor<PrismPropertyValue<Boolean>> processor = new MappingOutputProcessor<PrismPropertyValue<Boolean>>() {
			@Override
			public void process(ItemPath mappingOutputPath,
					PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple) throws ExpressionEvaluationException {
				if (outputTriple == null) {
					// The "default existence mapping"
					output.setValue(legal);
					return;
				}
				        
				Collection<PrismPropertyValue<Boolean>> nonNegativeValues = outputTriple.getNonNegativeValues();
		        if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
		        	throw new ExpressionEvaluationException("Activation existence expression resulted in null or empty value for projection " + accCtxDesc);
		        }
		        if (nonNegativeValues.size() > 1) {
		        	throw new ExpressionEvaluationException("Activation existence expression resulted in too many values ("+nonNegativeValues.size()+") for projection " + accCtxDesc);
		        }
		    	
		        output.setValue(nonNegativeValues.iterator().next().getValue());
			}
		};
        
        MappingEvaluatorHelperParams<PrismPropertyValue<Boolean>, ShadowType, F> params = new MappingEvaluatorHelperParams<>();
        params.setMappingTypes(outbound);
        params.setMappingDesc("outbound existence mapping in projection " + accCtxDesc);
        params.setNow(now);
        params.setInitializer(initializer);
		params.setProcessor(processor);
        params.setAPrioriTargetObject(accCtx.getObjectOld());
        params.setEvaluateCurrent(current);
        params.setTargetContext(accCtx);
        params.setFixTarget(true);
        params.setContext(context);
        
        PrismPropertyDefinition<Boolean> shadowExistsDef = new PrismPropertyDefinition<Boolean>(SHADOW_EXISTS_PROPERTY_NAME,
        		DOMUtil.XSD_BOOLEAN, prismContext);
        shadowExistsDef.setMinOccurs(1);
        shadowExistsDef.setMaxOccurs(1);
        params.setTargetItemDefinition(shadowExistsDef);
		mappingHelper.evaluateMappingSetProjection(params, task, result);
        
//		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = mappingHelper.evaluateMappingSetProjection(
//				outbound, "outbound existence mapping in projection " + accCtxDesc,
//        		now, initializer, null, null, accCtx.getObjectOld(), current, null, context, accCtx, task, result);
    	
		return (boolean) output.getValue();

    }
    
	private <T, F extends FocusType> void evaluateActivationMapping(final LensContext<F> context, 
			final LensProjectionContext projCtx, ResourceBidirectionalMappingType bidirectionalMappingType, 
			final ItemPath focusPropertyPath, final ItemPath projectionPropertyPath,
   			final ActivationCapabilityType capActivation, XMLGregorianCalendar now, final boolean current, 
   			String desc, final Task task, final OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        
   		String accCtxDesc = projCtx.toHumanReadableString();

        if (bidirectionalMappingType == null) {
            LOGGER.trace("No '{}' definition in activation in projection {}, skipping", desc, accCtxDesc);
            return;
        }
        List<MappingType> outbound = bidirectionalMappingType.getOutbound();
        if (outbound == null || outbound.isEmpty()) {
            LOGGER.trace("No outbound definition in '{}' definition in activation in projection {}, skipping", desc, accCtxDesc);
            return;
        }
        
        ObjectDelta<ShadowType> projectionDelta = projCtx.getDelta();
        PropertyDelta<T> shadowPropertyDelta = LensUtil.findAPrioriDelta(context, projCtx, projectionPropertyPath);
        
        PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();
        PrismProperty<T> shadowPropertyNew = null;
        if (shadowNew != null) {
        	shadowPropertyNew = shadowNew.findProperty(projectionPropertyPath);
        }
   		        
        MappingInitializer<PrismPropertyValue<T>> initializer = new MappingInitializer<PrismPropertyValue<T>>() {
			@Override
			public void initialize(Mapping<PrismPropertyValue<T>> mapping) throws SchemaException {
				// Source: administrativeStatus, validFrom or validTo
		        ItemDeltaItem<PrismPropertyValue<T>> sourceIdi = context.getFocusContext().getObjectDeltaObject().findIdi(focusPropertyPath);
		        
		        if (capActivation != null) {
			        ActivationValidityCapabilityType capValidFrom = capActivation.getValidFrom();
			        ActivationValidityCapabilityType capValidTo = capActivation.getValidTo();
			        
			        // Source: computedShadowStatus
			        ItemDeltaItem<PrismPropertyValue<ActivationStatusType>> computedIdi;
			        if (capValidFrom != null && capValidTo != null) {
			        	// "Native" validFrom and validTo, directly use administrativeStatus
			        	computedIdi = context.getFocusContext().getObjectDeltaObject().findIdi(focusPropertyPath);
			        	
			        } else {
			        	// Simulate validFrom and validTo using effectiveStatus
			        	computedIdi = context.getFocusContext().getObjectDeltaObject().findIdi(SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS);
			        	
			        }
			        
			        Source<PrismPropertyValue<ActivationStatusType>> computedSource = new Source<PrismPropertyValue<ActivationStatusType>>(computedIdi, ExpressionConstants.VAR_INPUT);
			        
			        mapping.setDefaultSource(computedSource);
			        
			        Source<PrismPropertyValue<T>> source = new Source<PrismPropertyValue<T>>(sourceIdi, ExpressionConstants.VAR_ADMINISTRATIVE_STATUS);
			        mapping.addSource(source);
			        
		        } else {
		        	Source<PrismPropertyValue<T>> source = new Source<PrismPropertyValue<T>>(sourceIdi, ExpressionConstants.VAR_INPUT);
		        	mapping.setDefaultSource(source);
		        }
		        
				// Source: legal
		        ItemDeltaItem<PrismPropertyValue<Boolean>> legalIdi = getLegalIdi(projCtx);
		        Source<PrismPropertyValue<Boolean>> legalSource = new Source<PrismPropertyValue<Boolean>>(legalIdi, ExpressionConstants.VAR_LEGAL);
		        mapping.addSource(legalSource);
		        
		        // Source: focusExists
		        ItemDeltaItem<PrismPropertyValue<Boolean>> focusExistsSourceIdi = getFocusExistsIdi(context.getFocusContext()); 
		        Source<PrismPropertyValue<Boolean>> focusExistsSource 
		        	= new Source<PrismPropertyValue<Boolean>>(focusExistsSourceIdi, ExpressionConstants.VAR_FOCUS_EXISTS);
		        mapping.addSource(focusExistsSource);
		        
		        // Variable: focus
		        mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, context.getFocusContext().getObjectDeltaObject());

		        // Variable: user (for convenience, same as "focus")
		        mapping.addVariableDefinition(ExpressionConstants.VAR_USER, context.getFocusContext().getObjectDeltaObject());
		        
		        mapping.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projCtx.getResource());
				
		        mapping.setOriginType(OriginType.OUTBOUND);
		        mapping.setOriginObject(projCtx.getResource());
			}

		};

		MappingEvaluatorHelperParams<PrismPropertyValue<T>, ShadowType, F> params = new MappingEvaluatorHelperParams<>();
		params.setMappingTypes(outbound);
		params.setMappingDesc(desc + " outbound activation mapping in projection " + accCtxDesc);
		params.setNow(now);
		params.setInitializer(initializer);
		params.setProcessor(null);
		params.setAPrioriTargetObject(shadowNew);
		params.setAPrioriTargetDelta(LensUtil.findAPrioriDelta(context, projCtx));
		params.setTargetContext(projCtx);
		params.setDefaultTargetItemPath(projectionPropertyPath);
		params.setEvaluateCurrent(current);
		params.setContext(context);
		params.setHasFullTargetObject(projCtx.hasFullShadow());
		mappingHelper.evaluateMappingSetProjection(params, task, result);

    }

	private ItemDeltaItem<PrismPropertyValue<Boolean>> getLegalIdi(LensProjectionContext accCtx) throws SchemaException {
		Boolean legal = accCtx.isLegal();
		Boolean legalOld = accCtx.isLegalOld();
		
		PrismPropertyDefinition<Boolean> legalDef = new PrismPropertyDefinition<Boolean>(LEGAL_PROPERTY_NAME,
        		DOMUtil.XSD_BOOLEAN, prismContext);
		legalDef.setMinOccurs(1);
		legalDef.setMaxOccurs(1);
		PrismProperty<Boolean> legalProp = legalDef.instantiate();
		legalProp.add(new PrismPropertyValue<Boolean>(legal));
		
		if (legal == legalOld) {
			return new ItemDeltaItem<PrismPropertyValue<Boolean>>(legalProp);
		} else {
			PrismProperty<Boolean> legalPropOld = legalProp.clone();
			legalPropOld.setRealValue(legalOld);
			PropertyDelta<Boolean> legalDelta = legalPropOld.createDelta();
			legalDelta.setValuesToReplace(new PrismPropertyValue<Boolean>(legal));
			return new ItemDeltaItem<PrismPropertyValue<Boolean>>(legalPropOld, legalDelta, legalProp);
		}
	}

	private <F extends ObjectType> ItemDeltaItem<PrismPropertyValue<Boolean>> getFocusExistsIdi(
			LensFocusContext<F> lensFocusContext) throws SchemaException {
		Boolean existsOld = null;
		Boolean existsNew = null;
		
		if (lensFocusContext != null) {
			if (lensFocusContext.isDelete()) {
				existsOld = true;
				existsNew = false;
			} else if (lensFocusContext.isAdd()) {
				existsOld = false;
				existsNew = true;
			} else {
				existsOld = true;
				existsNew = true;
			}
		}
		
		PrismPropertyDefinition<Boolean> existsDef = new PrismPropertyDefinition<Boolean>(FOCUS_EXISTS_PROPERTY_NAME,
				DOMUtil.XSD_BOOLEAN, prismContext);
		existsDef.setMinOccurs(1);
		existsDef.setMaxOccurs(1);
		PrismProperty<Boolean> existsProp = existsDef.instantiate();
		
		existsProp.add(new PrismPropertyValue<Boolean>(existsNew));
		
		if (existsOld == existsNew) {
			return new ItemDeltaItem<PrismPropertyValue<Boolean>>(existsProp);
		} else {
			PrismProperty<Boolean> existsPropOld = existsProp.clone();
			existsPropOld.setRealValue(existsOld);
			PropertyDelta<Boolean> existsDelta = existsPropOld.createDelta();
			existsDelta.setValuesToReplace(new PrismPropertyValue<Boolean>(existsNew));
			return new ItemDeltaItem<PrismPropertyValue<Boolean>>(existsPropOld, existsDelta, existsProp);
		}
	}

	private PrismObjectDefinition<UserType> getUserDefinition() {
		if (userDefinition == null) {
			userDefinition = prismContext.getSchemaRegistry()
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
