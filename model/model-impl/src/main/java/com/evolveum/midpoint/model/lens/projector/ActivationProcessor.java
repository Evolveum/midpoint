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
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

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
    	
    	evaluateActivationMapping(context, accCtx, activationType.getAdministrativeStatus(),
    			SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
    			ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart(), result);
    
    	// TODO: validFrom, validTo
    	
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
        		SHADOW_EXISTS_PROPERTY_NAME, DOMUtil.XSD_STRING, prismContext);
        shadowExistsDef.setMinOccurs(1);
        shadowExistsDef.setMaxOccurs(1);
		existenceMapping.setDefaultTargetDefinition(shadowExistsDef);
		
		// Source: legal
		PrismPropertyDefinition<Boolean> legalDef = new PrismPropertyDefinition<Boolean>(LEGAL_PROPERTY_NAME,
        		LEGAL_PROPERTY_NAME, DOMUtil.XSD_STRING, prismContext);
		legalDef.setMinOccurs(1);
		legalDef.setMaxOccurs(1);
		PrismProperty<Boolean> legalProp = legalDef.instantiate();
		legalProp.add(new PrismPropertyValue<Boolean>(legal));
        ItemDeltaItem<PrismPropertyValue<Boolean>> sourceIdi = new ItemDeltaItem<PrismPropertyValue<Boolean>>(legalProp); 
        Source<PrismPropertyValue<Boolean>> source = new Source<PrismPropertyValue<Boolean>>(sourceIdi, ExpressionConstants.VAR_LEGAL);
		existenceMapping.setDefaultSource(source);
				
		// TODO: Var: focusExists
		
        existenceMapping.setOriginType(OriginType.OUTBOUND);
        existenceMapping.setOriginObject(accCtx.getResource());
        existenceMapping.evaluate(result);
        
        PrismProperty<Boolean> shadowExistsProp = (PrismProperty<Boolean>) existenceMapping.getOutput();
        if (shadowExistsProp == null || shadowExistsProp.isEmpty()) {
        	throw new ExpressionEvaluationException("Activation existence expression resulted in null or empty value for projection " + accCtxDesc);
        }
    	
        return shadowExistsProp.getRealValue();
    }
    
    public void decide(LensContext<UserType,ShadowType> context, LensProjectionContext<ShadowType> accCtx, boolean shadowShouldExist, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	
    	
    	
    	
    	
    	
    }
    
   	private <T> void evaluateActivationMapping(LensContext<UserType,ShadowType> context, LensProjectionContext<ShadowType> accCtx, 
   			ResourceBidirectionalMappingType bidirectionalMappingType, ItemPath focusPropertyPath, ItemPath projectionPropertyPath,
   			String desc, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        
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
        
        // Source
        ItemDeltaItem<PrismPropertyValue<ActivationStatusType>> sourceIdi = context.getFocusContext().getObjectDeltaObject().findIdi(focusPropertyPath);
        Source<PrismPropertyValue<ActivationStatusType>> source = new Source<PrismPropertyValue<ActivationStatusType>>(sourceIdi, ExpressionConstants.VAR_INPUT);
		mapping.setDefaultSource(source);
        
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

}
