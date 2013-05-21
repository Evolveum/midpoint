/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens.projector;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationValidityCapabilityType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;

    public <F extends ObjectType, P extends ObjectType> void processActivation(LensContext<F,P> context, LensProjectionContext<P> projectionContext, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	processActivationUser((LensContext<UserType,ShadowType>) context, (LensProjectionContext<ShadowType>)projectionContext, result);
    }

    public void processActivationUser(LensContext<UserType,ShadowType> context, LensProjectionContext<ShadowType> accCtx, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
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
    	
    	boolean shadowShouldExist = evaluateExistenceMapping(context, accCtx, result);
    	
    	LOGGER.trace("Evaluated intended existence of projection {} to {}", accCtxDesc, shadowShouldExist);
    	
    	// Let's reconcile the existence intent (shadowShouldExist) and the synchronization intent in the context
    	
    	
    	
    	if (synchronizationIntent == null || synchronizationIntent == SynchronizationIntent.SYNCHRONIZE) {
	    	if (shadowShouldExist) {
	    		accCtx.setActive(true);
	    		if (accCtx.isExists()) {
	    			decision = SynchronizationPolicyDecision.KEEP;
	    		} else {
	    			decision = SynchronizationPolicyDecision.ADD;
	    		}
	    	} else {
	    		decision = SynchronizationPolicyDecision.DELETE;
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
    	
        PrismObject<UserType> focusNew = context.getFocusContext().getObjectNew();
        if (focusNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("focusNew is null, skipping activation processing of {}", accCtxDesc);
            return;
        }
    	
    	if (decision == SynchronizationPolicyDecision.UNLINK || decision == SynchronizationPolicyDecision.DELETE) {
    		LOGGER.trace("Decision is {}, skipping activation properties processing for {}", decision, accCtxDesc);
    		return;
    	}
    	
    	ResourceObjectTypeDefinitionType resourceAccountDefType = accCtx.getResourceAccountTypeDefinitionType();
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
        	
	    	evaluateActivationMapping(context, accCtx, activationType.getAdministrativeStatus(),
	    			SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
	    			capActivation, ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart(), result);
        } else {
        	LOGGER.trace("Skipping activation status processing because {} does not have activation status capability", accCtx.getResource());
        }

        if (capValidFrom != null) {
	    	evaluateActivationMapping(context, accCtx, activationType.getAdministrativeStatus(),
	    			SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM, 
	    			null, ActivationType.F_VALID_FROM.getLocalPart(), result);
        } else {
        	LOGGER.trace("Skipping activation validFrom processing because {} does not have activation validFrom capability", accCtx.getResource());
        }
	
        if (capValidTo != null) {
	    	evaluateActivationMapping(context, accCtx, activationType.getAdministrativeStatus(),
	    			SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO, 
	    			null, ActivationType.F_VALID_FROM.getLocalPart(), result);
	    } else {
	    	LOGGER.trace("Skipping activation validTo processing because {} does not have activation validTo capability", accCtx.getResource());
	    }
    	
    }
    
    private boolean evaluateExistenceMapping(LensContext<UserType,ShadowType> context, LensProjectionContext<ShadowType> accCtx, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	String accCtxDesc = accCtx.toHumanReadableString();
    	
    	Boolean legal = accCtx.isLegal();
    	if (legal == null) {
    		throw new IllegalStateException("Null 'legal' for "+accCtxDesc);
    	}
    	
    	ResourceObjectTypeDefinitionType resourceAccountDefType = accCtx.getResourceAccountTypeDefinitionType();
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
        MappingType outbound = existenceType.getOutbound();
        if (outbound == null) {
            return legal;
        }
    	
    	Mapping<PrismPropertyValue<Boolean>> existenceMapping =
        	valueConstructionFactory.createMapping(outbound, 
        		"outbound existence mapping in projection " + accCtxDesc);

        if (!existenceMapping.isApplicableToChannel(context.getChannel())) {
        	return legal;
        }
        
        // Target
        PrismPropertyDefinition<Boolean> shadowExistsDef = new PrismPropertyDefinition<Boolean>(SHADOW_EXISTS_PROPERTY_NAME,
        		SHADOW_EXISTS_PROPERTY_NAME, DOMUtil.XSD_BOOLEAN, prismContext);
        shadowExistsDef.setMinOccurs(1);
        shadowExistsDef.setMaxOccurs(1);
		existenceMapping.setDefaultTargetDefinition(shadowExistsDef);
		
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
		
        existenceMapping.setOriginType(OriginType.OUTBOUND);
        existenceMapping.setOriginObject(accCtx.getResource());
        existenceMapping.evaluate(result);
        
        PrismProperty<Boolean> shadowExistsProp = (PrismProperty<Boolean>) existenceMapping.getOutput();
        if (shadowExistsProp == null || shadowExistsProp.isEmpty()) {
        	throw new ExpressionEvaluationException("Activation existence expression resulted in null or empty value for projection " + accCtxDesc);
        }
    	
        return shadowExistsProp.getRealValue();
    }
    
	private <T> void evaluateActivationMapping(LensContext<UserType,ShadowType> context, LensProjectionContext<ShadowType> accCtx, 
   			ResourceBidirectionalMappingType bidirectionalMappingType, ItemPath focusPropertyPath, ItemPath projectionPropertyPath,
   			ActivationCapabilityType capActivation, String desc, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        
   		String accCtxDesc = accCtx.toHumanReadableString();

        if (bidirectionalMappingType == null) {
            LOGGER.trace("No '{}' definition in activation in projection {}, skipping", desc, accCtxDesc);
            return;
        }
        MappingType outbound = bidirectionalMappingType.getOutbound();
        if (outbound == null) {
            LOGGER.trace("No outbound definition in '{}' definition in activation in projection {}, skipping", desc, accCtxDesc);
            return;
        }
   		
        // Mapping
        Mapping<PrismPropertyValue<T>> mapping =
        	valueConstructionFactory.createMapping(outbound, 
        		desc + " outbound activation mapping in projection " + accCtxDesc);

        if (!mapping.isApplicableToChannel(context.getChannel())) {
        	return;
        }
        
        if (mapping.getStrength() != MappingStrengthType.STRONG) {
			ObjectDelta<ShadowType> projectionDelta = accCtx.getDelta();
	        PropertyDelta<T> shadowPropertyDelta = null;
	        if (projectionDelta != null) {
	        	shadowPropertyDelta = projectionDelta.findPropertyDelta(projectionPropertyPath);
	        }
        	if (shadowPropertyDelta != null && !shadowPropertyDelta.isEmpty()) {
        		return;
        	}
        }
                
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
        ItemDeltaItem<PrismPropertyValue<Boolean>> legalIdi = getLegalIdi(accCtx);
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

		// Target
        PrismObjectDefinition<ShadowType> shadowDefinition = 
            	prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
            PrismPropertyDefinition<T> shadowPropertyDefinition = 
            	shadowDefinition.findPropertyDefinition(projectionPropertyPath);
        mapping.setDefaultTargetDefinition(shadowPropertyDefinition);
		
        mapping.setOriginType(OriginType.OUTBOUND);
        mapping.setOriginObject(accCtx.getResource());
        
        // Evaluate
        mapping.evaluate(result);
        
        PropertyDelta<T> projectionPropertyDelta = PropertyDelta.createDelta(projectionPropertyPath, ShadowType.class, prismContext);
        
        if (accCtx.isAdd()) {
        	
	        PrismProperty<T> projectionPropertyNew = (PrismProperty<T>) mapping.getOutput();
	        if (projectionPropertyNew == null || projectionPropertyNew.isEmpty()) {
	            LOGGER.trace("Activation '{}' expression resulted in null or empty value for projection {}, skipping", desc, accCtxDesc);
	            return;
	        }
	        projectionPropertyDelta.setValuesToReplace(PrismValue.cloneCollection(projectionPropertyNew.getValues()));
	        
        } else {
        	
        	PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mapping.getOutputTriple();
        	if (outputTriple != null) {
	        	Collection<PrismPropertyValue<T>> plusSet = outputTriple.getPlusSet();
	        	if (plusSet != null && !plusSet.isEmpty()) {
	        		projectionPropertyDelta.setValuesToReplace(PrismValue.cloneCollection(plusSet));
	        	}
        	}
        	
        }
        
        if (!projectionPropertyDelta.isEmpty()) {
	        LOGGER.trace("Adding new '{}' delta for account {}: {}", new Object[]{desc, accCtxDesc, projectionPropertyDelta});
	        accCtx.addToSecondaryDelta(projectionPropertyDelta);
        }
    }

	private ItemDeltaItem<PrismPropertyValue<Boolean>> getLegalIdi(LensProjectionContext<ShadowType> accCtx) throws SchemaException {
		Boolean legal = accCtx.isLegal();
		Boolean legalOld = accCtx.isLegalOld();
		
		PrismPropertyDefinition<Boolean> legalDef = new PrismPropertyDefinition<Boolean>(LEGAL_PROPERTY_NAME,
        		LEGAL_PROPERTY_NAME, DOMUtil.XSD_BOOLEAN, prismContext);
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

	private ItemDeltaItem<PrismPropertyValue<Boolean>> getFocusExistsIdi(LensFocusContext<UserType> lensFocusContext) throws SchemaException {
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
				FOCUS_EXISTS_PROPERTY_NAME, DOMUtil.XSD_BOOLEAN, prismContext);
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

	
}
