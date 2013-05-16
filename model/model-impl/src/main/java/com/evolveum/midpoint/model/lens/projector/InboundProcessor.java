/*
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.lens.projector;

import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.common.filter.FilterManager;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Processor that takes changes from accounts and synchronization deltas and updates user attributes if necessary
 * (by creating secondary user object delta {@link ObjectDelta}).
 *
 * @author lazyman
 * @author Radovan Semancik
 */
@Component
public class InboundProcessor {

    public static final String PROCESS_INBOUND_HANDLING = InboundProcessor.class.getName() + ".processInbound";
    private static final Trace LOGGER = TraceManager.getTrace(InboundProcessor.class);

    @Autowired(required = true)
    private PrismContext prismContext;
    @Autowired(required = true)
    private FilterManager<Filter> filterManager;
    @Autowired(required = true)
    private MappingFactory mappingFactory;

    <F extends ObjectType, P extends ObjectType> void processInbound(LensContext<F,P> context, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		LOGGER.trace("Skipping inbound processing because focus is null");
    		return;
    	}
    	if (focusContext.isDelete()) {
    		LOGGER.trace("Skipping inbound processing because focus is being deleted");
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		LOGGER.trace("Skipping inbound processing because focus is not user");
    		// We can do this only for user.
    		return;
    	}
    	
    	LensContext<UserType,ShadowType> usContext = (LensContext<UserType,ShadowType>) context;
    	LensFocusContext<UserType> userContext = (LensFocusContext<UserType>)focusContext;
    	
    	OperationResult subResult = result.createSubresult(PROCESS_INBOUND_HANDLING);

        ObjectDelta<UserType> userSecondaryDelta = userContext.getProjectionWaveSecondaryDelta();

        if (userSecondaryDelta != null && ChangeType.DELETE.equals(userSecondaryDelta.getChangeType())) {
            //we don't need to do inbound if we are deleting this user
            return;
        }

        try {
            for (LensProjectionContext<ShadowType> accountContext : usContext.getProjectionContexts()) {
            	ResourceShadowDiscriminator rat = accountContext.getResourceShadowDiscriminator();
            	
            	ObjectDelta<ShadowType> aPrioriDelta = getAPrioriDelta(context, accountContext);
            	
            	if (!accountContext.isDoReconciliation() && aPrioriDelta == null) {
            		LOGGER.trace("Skipping processing of inbound expressions for account {}: no reconciliation and no a priori delta", rat);
            		continue;
            	}
//                LOGGER.trace("Processing inbound expressions for account {} starting", rat);

            	ObjectDelta<ShadowType> accountDelta = accountContext.getDelta();
                if (accountDelta != null && ChangeType.DELETE.equals(accountDelta.getChangeType())) {
                    //we don't need to do inbound if account was deleted
                    continue;
                }

                RefinedObjectClassDefinition accountDefinition = accountContext.getRefinedAccountDefinition();
                if (accountDefinition == null) {
                    LOGGER.error("Definition for account type {} not found in the context, but it " +
                            "should be there, dumping context:\n{}", rat, context.dump());
                    throw new IllegalStateException("Definition for account type " + rat
                            + " not found in the context, but it should be there");
                }

                
                processInboundExpressionsForAccount(usContext, accountContext, accountDefinition, aPrioriDelta, result);
            }

        } finally {
            subResult.computeStatus();
        }
    }

    private void processInboundExpressionsForAccount(LensContext<UserType,ShadowType> context, 
    		LensProjectionContext<ShadowType> accContext,
            RefinedObjectClassDefinition accountDefinition, ObjectDelta<ShadowType> aPrioriDelta, OperationResult result)
    		throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	
        if (aPrioriDelta == null && accContext.getObjectOld() == null) {
            LOGGER.trace("Nothing to process in inbound, both a priori delta and account old were null.");
            return;
        }

        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getProjectionWaveSecondaryDelta();
        
        PrismObject<ShadowType> accountOld = accContext.getObjectOld();
        PrismObject<ShadowType> accountNew = accContext.getObjectNew();
        for (QName accountAttributeName : accountDefinition.getNamesOfAttributesWithInboundExpressions()) {
            PropertyDelta<?> accountAttributeDelta = null;
            if (aPrioriDelta != null) {
                accountAttributeDelta = aPrioriDelta.findPropertyDelta(new ItemPath(SchemaConstants.C_ATTRIBUTES), accountAttributeName);
                if (accountAttributeDelta == null) {
                    LOGGER.trace("Skipping inbound for {} in {}: Account a priori delta exists, but doesn't have change for processed property.",
                    		accountAttributeName, accContext.getResourceShadowDiscriminator());
                    continue;
                }
            }

            RefinedAttributeDefinition attrDef = accountDefinition.getAttributeDefinition(accountAttributeName);
            List<MappingType> inboundMappingTypes = attrDef.getInboundMappingTypes();
            LOGGER.trace("Processing inbound for {} in {}; ({} mappings)", new Object[]{
            		PrettyPrinter.prettyPrint(accountAttributeName), accContext.getResourceShadowDiscriminator(), (inboundMappingTypes != null ? inboundMappingTypes.size() : 0)});

            for (MappingType inboundMappingType : inboundMappingTypes) {
            	
                PropertyDelta<?> userPropertyDelta = null;
                if (aPrioriDelta != null) {
                    LOGGER.trace("Processing inbound from a priori delta.");
                    userPropertyDelta = evaluateInboundMapping(context, inboundMappingType, accountAttributeName, null, accountAttributeDelta, 
                    		context.getFocusContext().getObjectNew(), accountNew, accContext.getResource(), result);
                } else if (accountOld != null) {
                	if (!accContext.isFullShadow()) {
                		throw new SystemException("Attempt to execute inbound expression on account shadow (not full account)");
                	}
                    LOGGER.trace("Processing inbound from account sync absolute state (oldAccount).");
                    PrismProperty<?> oldAccountProperty = accountOld.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, accountAttributeName));
                    userPropertyDelta = evaluateInboundMapping(context, inboundMappingType, accountAttributeName, oldAccountProperty, null, 
                    		context.getFocusContext().getObjectNew(), accountNew, accContext.getResource(), result);
                }

                if (userPropertyDelta != null && !userPropertyDelta.isEmpty()) {
                    LOGGER.trace("Created delta (from inbound expression) \n{}", new Object[]{userPropertyDelta.debugDump(3)});
                    context.getFocusContext().swallowToProjectionWaveSecondaryDelta(userPropertyDelta);
                    context.recomputeFocus();
                } else {
                    LOGGER.trace("Created delta (from inbound expression) was null or empty.");
                }
            }
        }
        processSpecialPropertyInbound(accountDefinition.getCredentialsInbound(), SchemaConstants.PATH_PASSWORD_VALUE,
        		context.getFocusContext().getObjectNew(), accContext, accountDefinition, context, result);
        
        processSpecialPropertyInbound(accountDefinition.getActivationBidirectionalMappingType(ActivationType.F_ADMINISTRATIVE_STATUS), SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
        		context.getFocusContext().getObjectNew(), accContext, accountDefinition, context, result);        
        processSpecialPropertyInbound(accountDefinition.getActivationBidirectionalMappingType(ActivationType.F_VALID_FROM), SchemaConstants.PATH_ACTIVATION_VALID_FROM,
        		context.getFocusContext().getObjectNew(), accContext, accountDefinition, context, result);
        processSpecialPropertyInbound(accountDefinition.getActivationBidirectionalMappingType(ActivationType.F_VALID_TO), SchemaConstants.PATH_ACTIVATION_VALID_TO,
        		context.getFocusContext().getObjectNew(), accContext, accountDefinition, context, result);
    }

    /**
	 * A priori delta is a delta that was executed in a previous "step". That means it is either delta from a previous
	 * wave or a sync delta (in wave 0).
	 */
	private <F extends ObjectType, P extends ObjectType> ObjectDelta<ShadowType> getAPrioriDelta(LensContext<F,P> context, LensProjectionContext<ShadowType> accountContext) throws SchemaException {
		int wave = context.getProjectionWave();
		if (wave == 0) {
			return accountContext.getSyncDelta();
		}
		if (wave == accountContext.getWave() + 1) {
			// If this resource was processed in a previous wave ....
			return accountContext.getDelta();
		}
		return null;
	}

	private boolean checkWeakSkip(Mapping<?> inbound, PrismObject<UserType> newUser) throws SchemaException {
        if (inbound.getStrength() != MappingStrengthType.WEAK) {
        	return false;
        }
        if (newUser == null) {
        	return false;
        }
        PrismProperty<?> property = newUser.findProperty(inbound.getOutputPath());
        if (property != null && !property.isEmpty()) {
            return true;
        }
        return false;
    }
    
    private <A,U> PropertyDelta<U> evaluateInboundMapping(final LensContext<UserType,ShadowType> context, 
    		MappingType inboundMappingType, 
    		QName accountAttributeName, PrismProperty<A> oldAccountProperty, PropertyDelta<A> accountAttributeDelta,
            PrismObject<UserType> newUser, PrismObject<ShadowType> account, ResourceType resource, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	
    	if (oldAccountProperty != null && oldAccountProperty.hasRaw()) {
        	throw new SystemException("Property "+oldAccountProperty+" has raw parsing state, such property cannot be used in inbound expressions");
        }
    	
    	Mapping<PrismPropertyValue<U>> mapping = mappingFactory.createMapping(inboundMappingType, 
    			"inbound expression for "+accountAttributeName+" in "+resource);
    	
    	if (!mapping.isApplicableToChannel(context.getChannel())) {
    		return null;
    	}
    	
    	Source<PrismPropertyValue<A>> defaultSource = new Source<PrismPropertyValue<A>>(oldAccountProperty, accountAttributeDelta, null, ExpressionConstants.VAR_INPUT);
    	defaultSource.recompute();
		mapping.setDefaultSource(defaultSource);
		mapping.setTargetContext(getUserDefinition());
    	mapping.addVariableDefinition(ExpressionConstants.VAR_USER, newUser);
    	mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, newUser);
    	mapping.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, account);
    	mapping.addVariableDefinition(ExpressionConstants.VAR_SHADOW, account);
		mapping.setStringPolicyResolver(createStringPolicyResolver(context));
		mapping.setOriginType(OriginType.INBOUND);
		mapping.setOriginObject(resource);
    	
    	if (checkWeakSkip(mapping, newUser)) {
            LOGGER.trace("Skipping because of weak mapping type");
            return null;
        }
        
        ItemPath targetUserPropertyPath = mapping.getOutputPath();
		PrismProperty<U> targetUserProperty = newUser.findProperty(targetUserPropertyPath);
        PrismPropertyDefinition targetPropertyDef = newUser.getDefinition().findPropertyDefinition(targetUserPropertyPath);
        if (targetPropertyDef == null) {
        	throw new SchemaException("No definition for user property "+targetUserPropertyPath+", cannot process inbound expression in "+resource);
        }
        
        PropertyDelta<U> outputUserPropertydelta = new PropertyDelta<U>(targetUserPropertyPath, targetPropertyDef);
    	
        mapping.evaluate(result);
    	
    	PrismValueDeltaSetTriple<PrismPropertyValue<U>> triple = mapping.getOutputTriple();
    	// Meaning of the resulting triple:
    	//   values in PLUS set will be added     (valuesToAdd in delta)
    	//   values in MINUS set will be removed  (valuesToDelete in delta)
    	//   values in ZERO set will be compared with existing values in user property
    	//                  the differences will be added to delta
    	
    	if (LOGGER.isTraceEnabled()) {
    		LOGGER.trace("Inbound value construction for {} returned triple:\n{}", accountAttributeName, triple == null ? "null" : triple.debugDump());
    	}
        
    	if (triple != null) {
    		
	        if (triple.getPlusSet() != null) {
	            for (PrismPropertyValue<U> value : triple.getPlusSet()) {
	
	                if (targetUserProperty != null && targetUserProperty.hasRealValue(value)) {
	                    continue;
	                }
	
	                //if property is not multi value replace existing attribute
	                if (targetUserProperty != null && !targetUserProperty.getDefinition().isMultiValue() && !targetUserProperty.isEmpty()) {
	                    Collection<PrismPropertyValue<U>> replace = new ArrayList<PrismPropertyValue<U>>();
	                    replace.add(value);
	                    outputUserPropertydelta.setValuesToReplace(replace);
	                } else {
	                    outputUserPropertydelta.addValueToAdd(value);
	                }
	            }
	        }
	        if (triple.getMinusSet() != null) {
	            LOGGER.trace("Checking account sync property delta values to delete");
	            for (PrismPropertyValue<U> value : triple.getMinusSet()) {
	
	                if (targetUserProperty == null || targetUserProperty.hasRealValue(value)) {
	                	if (!outputUserPropertydelta.isReplace()) {
	                		// This is not needed if we are going to replace. In fact it might cause an error.
	                		outputUserPropertydelta.addValueToDelete(value);
	                	}
	                }
	            }
	        }
	        
	        if (triple.hasZeroSet()) {
		        PrismProperty<U> sourceProperty = targetPropertyDef.instantiate();
		    	sourceProperty.addAll(PrismValue.cloneCollection(triple.getZeroSet()));
		        if (targetUserProperty != null) {
		            LOGGER.trace("Simple property comparing user property {} to computed property {} ",
		                    new Object[]{targetUserProperty, sourceProperty});
		            //simple property comparing if user property exists
		            PropertyDelta<U> diffDelta = targetUserProperty.diff(sourceProperty);
		            if (diffDelta != null) {
		            	diffDelta.setName(ItemPath.getName(targetUserPropertyPath.last()));
		            	diffDelta.setParentPath(targetUserPropertyPath.allExceptLast());
		            	outputUserPropertydelta.merge(diffDelta);
		            }
		        } else {
		            if (sourceProperty != null) {
		                LOGGER.trace("Adding user property because inbound say so (account doesn't contain that value)");
		                //if user property doesn't exist we have to add it (as delta), because inbound say so
		                outputUserPropertydelta.addValuesToAdd(sourceProperty.getClonedValues());
		            }
		            //we don't have to create delta, because everything is alright
		            LOGGER.trace("We don't have to create delta, everything is alright.");
		        }
	        }
    	}

        // if no changes were generated return null
        return outputUserPropertydelta.isEmpty() ? null : outputUserPropertydelta;
    }

	private StringPolicyResolver createStringPolicyResolver(final LensContext<UserType, ShadowType> context) {
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
				if (!outputDefinition.getName().equals(PasswordType.F_VALUE)) {
					return null;
				}
				ValuePolicyType passwordPolicy = context.getGlobalPasswordPolicy();
				if (passwordPolicy == null) {
					return null;
				}
				return passwordPolicy.getStringPolicy();
			}
		};
		return stringPolicyResolver;
	}

	private <T> PrismPropertyValue<T> filterValue(PrismPropertyValue<T> propertyValue, List<ValueFilterType> filters) {
        PrismPropertyValue<T> filteredValue = propertyValue.clone();
        filteredValue.setOriginType(OriginType.INBOUND);

        if (filters == null || filters.isEmpty()) {
            return filteredValue;
        }

        for (ValueFilterType filter : filters) {
            Filter filterInstance = filterManager.getFilterInstance(filter.getType(), filter.getAny());
            filterInstance.apply(filteredValue);
        }

        return filteredValue;
    }
	
	/**
     * Processing for special (fixed-schema) properties such as credentials and activation. 
     */
    private void processSpecialPropertyInbound(ResourceBidirectionalMappingType biMappingType, ItemPath sourcePath,
            PrismObject<UserType> newUser, LensProjectionContext<ShadowType> accContext, 
            RefinedObjectClassDefinition accountDefinition, LensContext<UserType,ShadowType> context, 
            OperationResult opResult) throws SchemaException {
    	if (biMappingType == null) {
    		return;
    	}
    	processSpecialPropertyInbound(biMappingType.getInbound(), sourcePath, newUser, accContext, accountDefinition, context, opResult);
    }

    /**
     * Processing for special (fixed-schema) properties such as credentials and activation. 
     */
    private void processSpecialPropertyInbound(MappingType inboundMappingType, ItemPath sourcePath,
            PrismObject<UserType> newUser, LensProjectionContext<ShadowType> accContext, 
            RefinedObjectClassDefinition accountDefinition, LensContext<UserType,ShadowType> context, 
            OperationResult opResult) throws SchemaException {
    	
        if (inboundMappingType == null || newUser == null || !accContext.isFullShadow()) {
            return;
        }

        Mapping<PrismPropertyValue<?>> mapping = mappingFactory.createMapping(inboundMappingType, 
        		"inbound mapping for "+sourcePath+" in "+accContext.getResource());
        
        if (!mapping.isApplicableToChannel(context.getChannel())) {
        	return;
        }

        PrismProperty<?> property = newUser.findOrCreateProperty(sourcePath);
        if (mapping.getStrength() == MappingStrengthType.WEAK && !property.isEmpty()) {
            //inbound will be constructed only if initial == false or initial == true and value doesn't exist
            return;
        }
        
        ObjectDelta<UserType> userPrimaryDelta = context.getFocusContext().getPrimaryDelta();
        if (userPrimaryDelta != null) {
        	PropertyDelta primaryPropDelta = userPrimaryDelta.findPropertyDelta(sourcePath);
        	if (primaryPropDelta != null && primaryPropDelta.isReplace()) {
        		// Replace primary delta overrides any inbound
        		return;
        	}
        }

        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getProjectionWaveSecondaryDelta();
        if (userSecondaryDelta != null) {
	        PropertyDelta<?> delta = userSecondaryDelta.findPropertyDelta(sourcePath);
	        if (delta != null) {
	            //remove delta if exists, it will be handled by inbound
	            userSecondaryDelta.getModifications().remove(delta);
	        }
        }

        if (accContext.getObjectNew() == null) {
            accContext.recompute();
            if (accContext.getObjectNew() == null) {
                // Still null? something must be really wrong here.
                String message = "Recomputing account " + accContext.getResourceShadowDiscriminator()
                        + " results in null new account. Something must be really broken.";
                LOGGER.error(message);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Account context:\n{}", accContext.dump());
                }
                throw new SystemException(message);
            }
        }

        ItemDeltaItem<PrismPropertyValue<?>> sourceIdi = accContext.getObjectDeltaObject().findIdi(sourcePath);
        Source<PrismPropertyValue<?>> source = new Source<PrismPropertyValue<?>>(sourceIdi.getItemOld(), sourceIdi.getDelta(), 
        		sourceIdi.getItemOld(), ExpressionConstants.VAR_INPUT);
		mapping.setDefaultSource(source);
		
    	mapping.setDefaultTargetDefinition(property.getDefinition());
    	
    	mapping.addVariableDefinition(ExpressionConstants.VAR_USER, newUser);
    	mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, newUser);
    	
    	PrismObject<ShadowType> accountNew = accContext.getObjectNew();
    	mapping.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, accountNew);
    	mapping.addVariableDefinition(ExpressionConstants.VAR_SHADOW, accountNew);
    	
    	mapping.setStringPolicyResolver(createStringPolicyResolver(context));
    	mapping.setOriginType(OriginType.INBOUND);
    	mapping.setOriginObject(accContext.getResource());
    	
        PrismProperty result;
        try {
        	mapping.evaluate(opResult);
            result = (PrismProperty) mapping.getOutput();
        } catch (SchemaException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SchemaException(ex.getMessage(), ex);
        }

        PropertyDelta<?> delta = property.diff(result);
        if (delta != null && !delta.isEmpty()) {
        	delta.setParentPath(sourcePath.allExceptLast());
        	context.getFocusContext().swallowToProjectionWaveSecondaryDelta(delta);
        }
    }
    
    private PrismObjectDefinition<UserType> getUserDefinition() {
		return prismContext.getSchemaRegistry().getObjectSchema()
				.findObjectDefinitionByCompileTimeClass(UserType.class);
	}
}
