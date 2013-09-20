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

import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.common.filter.FilterManager;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
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
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
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
    @Autowired
    private ProvisioningService provisioningService;

    <F extends FocusType> void processInbound(LensContext<F> context, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
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
    	
    	OperationResult subResult = result.createSubresult(PROCESS_INBOUND_HANDLING);

        ObjectDelta<F> userSecondaryDelta = focusContext.getProjectionWaveSecondaryDelta();

        if (userSecondaryDelta != null && ChangeType.DELETE.equals(userSecondaryDelta.getChangeType())) {
            //we don't need to do inbound if we are deleting this user
            return;
        }

        try {
            for (LensProjectionContext accountContext : context.getProjectionContexts()) {
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

                
                processInboundExpressionsForAccount(context, accountContext, accountDefinition, aPrioriDelta, result);
            }

        } finally {
            subResult.computeStatus();
        }
    }

    private <F extends FocusType> void processInboundExpressionsForAccount(LensContext<F> context, 
    		LensProjectionContext accContext,
            RefinedObjectClassDefinition accountDefinition, ObjectDelta<ShadowType> aPrioriDelta, OperationResult result)
    		throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	
        if (aPrioriDelta == null && accContext.getObjectCurrent() == null) {
            LOGGER.trace("Nothing to process in inbound, both a priori delta and current account were null.");
            return;
        }

        PrismObject<ShadowType> accountCurrent = accContext.getObjectCurrent();
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
            
            if (attrDef.isIgnored(LayerType.MODEL)) {
            	LOGGER.trace("Skipping inbound for attribute {} in {} because the attribute is ignored", new Object[]{
                		PrettyPrinter.prettyPrint(accountAttributeName), accContext.getResourceShadowDiscriminator()});
            	continue;
            }
            
            List<MappingType> inboundMappingTypes = attrDef.getInboundMappingTypes();
            LOGGER.trace("Processing inbound for {} in {}; ({} mappings)", new Object[]{
            		PrettyPrinter.prettyPrint(accountAttributeName), accContext.getResourceShadowDiscriminator(), (inboundMappingTypes != null ? inboundMappingTypes.size() : 0)});

            if (!inboundMappingTypes.isEmpty()) {
            	
            	PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
            	if (limitations != null) {
            		PropertyAccessType access = limitations.getAccess();
            		if (access != null) {
            			if (access.isRead() == null || !access.isRead()) {
            				LOGGER.warn("Inbound mapping for non-readable attribute {} in {}, skipping", 
            						accountAttributeName, accContext.getHumanReadableName());
            				continue;
            			}
            		}
            	}
            	
	            for (MappingType inboundMappingType : inboundMappingTypes) {
	            	
	            	// There are two processing options:
	            	//
	            	//  * If we have a delta as an input we will proceed in relative mode, applying mappings on the delta.
	            	//    This usually happens when a delta comes from a sync notification or if there is a primary projection delta.
	            	//
	            	//  * if we do NOT have a delta then we will proceed in absolute mode. In that mode we will apply the
	            	//    mappings to the absolute projection state that we got from provisioning. This is a kind of "inbound reconciliation".
	            	
	                PropertyDelta<?> userPropertyDelta = null;
	                if (aPrioriDelta != null) {
	                    LOGGER.trace("Processing inbound from a priori delta.");
	                    userPropertyDelta = evaluateInboundMapping(context, inboundMappingType, accountAttributeName, null, accountAttributeDelta, 
	                    		context.getFocusContext().getObjectNew(), accountNew, accContext.getResource(), result);
	                } else if (accountCurrent != null) {
	                	if (!accContext.isFullShadow()) {
	                		LOGGER.warn("Attempted to execute inbound expression on account shadow {} WITHOUT full account. Trying to load the account now.", accContext.getOid());      // todo change to trace level eventually
                            Throwable failure = null;
                            try {
                                LensUtil.loadFullAccount(accContext, provisioningService, result);
                            } catch (ObjectNotFoundException e) {
                                failure = e;
                            } catch (SecurityViolationException e) {
                                failure = e;
                            } catch (CommunicationException e) {
                                failure = e;
                            } catch (ConfigurationException e) {
                                failure = e;
                            }
                            if (failure != null) {
                                LOGGER.warn("Couldn't load account with shadow OID {} because of {}, setting context as broken and skipping inbound processing on it", accContext.getOid(), failure.getMessage());
                                accContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
                                return;
                            }
                            if (!accContext.isFullShadow()) {
                                LOGGER.warn("Couldn't load account with shadow OID {}, setting context as broken and skipping inbound processing on it", accContext.getOid());
                                accContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
                                return;
                            }
                        }
	                    LOGGER.trace("Processing inbound from account sync absolute state (oldAccount).");
	                    PrismProperty<?> oldAccountProperty = accountCurrent.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, accountAttributeName));
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
	private <F extends FocusType> ObjectDelta<ShadowType> getAPrioriDelta(LensContext<F> context, 
			LensProjectionContext accountContext) throws SchemaException {
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

	private <F extends FocusType> boolean checkWeakSkip(Mapping<?> inbound, PrismObject<F> newUser) throws SchemaException {
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
    
    private <A,U, F extends FocusType> PropertyDelta<U> evaluateInboundMapping(final LensContext<F> context, 
    		MappingType inboundMappingType, 
    		QName accountAttributeName, PrismProperty<A> oldAccountProperty, PropertyDelta<A> accountAttributeDelta,
            PrismObject<F> newUser, PrismObject<ShadowType> account, ResourceType resource, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	
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
    	mapping.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource);
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
    	
        LensUtil.evaluateMapping(mapping, context, result);
    	
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
	                    replace.add(value.clone());
	                    outputUserPropertydelta.setValuesToReplace(replace);
	                } else {
	                    outputUserPropertydelta.addValueToAdd(value.clone());
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
	        
    	} else { // tripple == null
    		
    		if (accountAttributeDelta == null) {
    			
    			// This is the case of "inbound reconciliation" which is quite special. The tripple returned null
    			// which means that there was nothing in the input and (unsurprisingly) no change. If the input was empty
    			// then we need to make sure that the output (focus property) is also empty. Otherwise we miss the
    			// re-sets of projection values to empty values and cannot propagate them.
    			
    			if (targetUserProperty != null && !targetUserProperty.isEmpty()) {
    				outputUserPropertydelta.setValuesToReplace();
    			}
    		}
    		
    	}

        // if no changes were generated return null
        return outputUserPropertydelta.isEmpty() ? null : outputUserPropertydelta;
    }

	private <F extends FocusType> StringPolicyResolver createStringPolicyResolver(final LensContext<F> context) {
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
    private <F extends FocusType> void processSpecialPropertyInbound(ResourceBidirectionalMappingType biMappingType, ItemPath sourcePath,
            PrismObject<F> newUser, LensProjectionContext accContext, 
            RefinedObjectClassDefinition accountDefinition, LensContext<F> context, 
            OperationResult opResult) throws SchemaException {
    	if (biMappingType == null) {
    		return;
    	}
    	processSpecialPropertyInbound(biMappingType.getInbound(), sourcePath, newUser, accContext, accountDefinition, context, opResult);
    }

    private <F extends FocusType> void processSpecialPropertyInbound(MappingType inboundMappingType, ItemPath sourcePath,
            PrismObject<F> newUser, LensProjectionContext accContext, 
            RefinedObjectClassDefinition accountDefinition, LensContext<F> context, 
            OperationResult opResult) throws SchemaException {
    	if (inboundMappingType == null) {
    		return;
    	}
    	Collection<MappingType> inboundMappingTypes = new ArrayList<MappingType>(1);
    	inboundMappingTypes.add(inboundMappingType);
    	processSpecialPropertyInbound(inboundMappingTypes, sourcePath, newUser, accContext, accountDefinition, context, opResult);
    }
    
    /**
     * Processing for special (fixed-schema) properties such as credentials and activation. 
     */
    private <F extends FocusType> void processSpecialPropertyInbound(Collection<MappingType> inboundMappingTypes, ItemPath sourcePath,
            PrismObject<F> newUser, LensProjectionContext accContext, 
            RefinedObjectClassDefinition accountDefinition, LensContext<F> context, 
            OperationResult opResult) throws SchemaException {
    	
        if (inboundMappingTypes == null || inboundMappingTypes.isEmpty() || newUser == null || !accContext.isFullShadow()) {
            return;
        }

        // TODO: fix multiple inbounds
        Mapping<PrismPropertyValue<?>> mapping = mappingFactory.createMapping(inboundMappingTypes.iterator().next(), 
        		"inbound mapping for "+sourcePath+" in "+accContext.getResource());
        
        if (!mapping.isApplicableToChannel(context.getChannel())) {
        	return;
        }

        PrismProperty<?> property = newUser.findOrCreateProperty(sourcePath);
        if (mapping.getStrength() == MappingStrengthType.WEAK && !property.isEmpty()) {
            //inbound will be constructed only if initial == false or initial == true and value doesn't exist
            return;
        }
        
        ObjectDelta<F> userPrimaryDelta = context.getFocusContext().getPrimaryDelta();
        if (userPrimaryDelta != null) {
        	PropertyDelta primaryPropDelta = userPrimaryDelta.findPropertyDelta(sourcePath);
        	if (primaryPropDelta != null && primaryPropDelta.isReplace()) {
        		// Replace primary delta overrides any inbound
        		return;
        	}
        }

        ObjectDelta<F> userSecondaryDelta = context.getFocusContext().getProjectionWaveSecondaryDelta();
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
    	mapping.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, accContext.getResource());
    	
    	mapping.setStringPolicyResolver(createStringPolicyResolver(context));
    	mapping.setOriginType(OriginType.INBOUND);
    	mapping.setOriginObject(accContext.getResource());
    	
        PrismProperty result;
        try {
        	LensUtil.evaluateMapping(mapping, context, opResult);
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
