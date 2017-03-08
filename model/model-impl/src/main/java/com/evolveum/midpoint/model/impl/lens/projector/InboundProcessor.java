/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.common.filter.FilterManager;
import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
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

    private static final String PROCESS_INBOUND_HANDLING = InboundProcessor.class.getName() + ".processInbound";
    private static final Trace LOGGER = TraceManager.getTrace(InboundProcessor.class);

    @Autowired
    private PrismContext prismContext;
    
    @Autowired
    private FilterManager<Filter> filterManager;
    
    @Autowired
    private MappingFactory mappingFactory;
    
    @Autowired
    private ContextLoader contextLoader;
    
    @Autowired
    private PasswordPolicyProcessor passwordPolicyProcessor;
    
    @Autowired
    private MappingEvaluator mappingEvaluator;
    
    @Autowired
    private Protector protector;

    <O extends ObjectType> void processInbound(LensContext<O> context, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException {
    	LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext == null) {
            LOGGER.trace("Skipping inbound because there is no focus");
    		return;
    	}
    	if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for focus types.
            LOGGER.trace("Skipping inbound because {} is not focal type", focusContext.getObjectTypeClass());
    		return;
    	}
    	processInboundFocal((LensContext<? extends FocusType>)context, task, now, result);
    }

    private <F extends FocusType> void processInboundFocal(LensContext<F> context, Task task, XMLGregorianCalendar now,
			OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		LOGGER.trace("Skipping inbound processing because focus is null");
    		return;
    	}
    	if (focusContext.isDelete()) {
    		LOGGER.trace("Skipping inbound processing because focus is being deleted");
    		return;
    	}

        ObjectDelta<F> userSecondaryDelta = focusContext.getProjectionWaveSecondaryDelta();

        if (userSecondaryDelta != null && ChangeType.DELETE.equals(userSecondaryDelta.getChangeType())) {
            //we don't need to do inbound if we are deleting this user
            return;
        }

		OperationResult subResult = result.createMinorSubresult(PROCESS_INBOUND_HANDLING);

		try {
            for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
            	if (projectionContext.isThombstone()) {
            		if (LOGGER.isTraceEnabled()) {
            			LOGGER.trace("Skipping processing of inbound expressions for projection {} because is is thombstone", projectionContext.getHumanReadableName());
            		}
            		continue;
            	}
            	if (!projectionContext.isCanProject()){
            		if (LOGGER.isTraceEnabled()) {
            			LOGGER.trace("Skipping processing of inbound expressions for projection {}: there is a limit to propagate changes only from resource {}",
            				projectionContext.getHumanReadableName(), context.getTriggeredResourceOid());
            		}
            		continue;
            	}
            	ObjectDelta<ShadowType> aPrioriDelta = getAPrioriDelta(context, projectionContext);
            	
            	if (!projectionContext.isDoReconciliation() && aPrioriDelta == null && !LensUtil.hasDependentContext(context, projectionContext) && !projectionContext.isFullShadow()) {
            		if (LOGGER.isTraceEnabled()) {
            			LOGGER.trace("Skipping processing of inbound expressions for projection {}: no full shadow, no reconciliation, no a priori delta and no dependent context",
            					projectionContext.getHumanReadableName());
            		}
            		continue;
            	}

                RefinedObjectClassDefinition rOcDef = projectionContext.getCompositeObjectClassDefinition();
                if (rOcDef == null) {
                    LOGGER.error("Definition for projection {} not found in the context, but it " +
                            "should be there, dumping context:\n{}", projectionContext.getHumanReadableName(), context.debugDump());
                    throw new IllegalStateException("Definition for projection " + projectionContext.getHumanReadableName()
                            + " not found in the context, but it should be there");
                }

                processInboundExpressionsForProjection(context, projectionContext, rOcDef, aPrioriDelta, task, now, subResult);
            }

        } finally {
            subResult.computeStatus();
        }
    }

    private boolean isDeleteAccountDelta(LensProjectionContext accountContext) throws SchemaException {
        if (accountContext.getSyncDelta() != null && ChangeType.DELETE == accountContext.getSyncDelta().getChangeType()){
            return true;
        }

        if (accountContext.getDelta() != null && ChangeType.DELETE == accountContext.getDelta().getChangeType()){
            return true;
        }
        return false;
    }

    private <F extends FocusType> void processInboundExpressionsForProjection(LensContext<F> context,
    		LensProjectionContext projContext,
            RefinedObjectClassDefinition accountDefinition, ObjectDelta<ShadowType> aPrioriDelta, Task task, XMLGregorianCalendar now, OperationResult result)
    		throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException {
    	
        if (aPrioriDelta == null && projContext.getObjectCurrent() == null) {
            LOGGER.trace("Nothing to process in inbound, both a priori delta and current account were null.");
            return;
        }

        PrismObject<ShadowType> accountCurrent = projContext.getObjectCurrent();
        PrismObject<ShadowType> accountNew = projContext.getObjectNew();
        if (hasAnyStrongMapping(accountDefinition) && !projContext.isFullShadow() && !projContext.isThombstone()) {
        	LOGGER.trace("There are strong inbound mapping, but the shadow hasn't be fully loaded yet. Trying to load full shadow now.");
			accountCurrent = loadProjection(context, projContext, task, result, accountCurrent);
			if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
				return;
			}
		}
        
        for (QName accountAttributeName : accountDefinition.getNamesOfAttributesWithInboundExpressions()) {
            final PropertyDelta<?> attributeAPrioriDelta;
            if (aPrioriDelta != null) {
                attributeAPrioriDelta = aPrioriDelta.findPropertyDelta(new ItemPath(SchemaConstants.C_ATTRIBUTES), accountAttributeName);
                if (attributeAPrioriDelta == null && !projContext.isFullShadow() && !LensUtil.hasDependentContext(context, projContext)) {
					LOGGER.trace("Skipping inbound for {} in {}: Not a full shadow and account a priori delta exists, but doesn't have change for processed property.",
							accountAttributeName, projContext.getResourceShadowDiscriminator());
					continue;
                }
            } else {
            	attributeAPrioriDelta = null;
			}

            RefinedAttributeDefinition attrDef = accountDefinition.findAttributeDefinition(accountAttributeName);
            
            if (attrDef.isIgnored(LayerType.MODEL)) {
            	LOGGER.trace("Skipping inbound for attribute {} in {} because the attribute is ignored",
                		PrettyPrinter.prettyPrint(accountAttributeName), projContext.getResourceShadowDiscriminator());
            	continue;
            }
            
            List<MappingType> inboundMappingTypes = attrDef.getInboundMappingTypes();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Processing inbound for {} in {}; ({} mappings)", PrettyPrinter.prettyPrint(accountAttributeName),
						projContext.getResourceShadowDiscriminator(), inboundMappingTypes.size());
			}

            if (!inboundMappingTypes.isEmpty()) {
            	
            	PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
            	if (limitations != null) {
            		PropertyAccessType access = limitations.getAccess();
            		if (access != null) {
            			if (access.isRead() == null || !access.isRead()) {
            				LOGGER.warn("Inbound mapping for non-readable attribute {} in {}, skipping", 
            						accountAttributeName, projContext.getHumanReadableName());
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
					//
					// TODO what if there is a priori delta for a given attribute (e.g. ADD one) and
					// we want to reconcile also the existing attribute value? This probably would not work.
					if (inboundMappingType.getStrength() == MappingStrengthType.STRONG) {
						LOGGER.trace("There is an inbound mapping with strength == STRONG, trying to load full account now.");
						if (!projContext.isFullShadow() && !projContext.isDelete()) {
							accountCurrent = loadProjection(context, projContext, task, result, accountCurrent);
							if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
								return;
							}
						}
					}

					if (attributeAPrioriDelta == null && !projContext.isFullShadow() && !LensUtil.hasDependentContext(context, projContext)) {
						LOGGER.trace("Skipping inbound for {} in {}: Not a full shadow and account a priori delta exists, but doesn't have change for processed property.",
								accountAttributeName, projContext.getResourceShadowDiscriminator());
						continue;
					}

					PrismObject<F> focus;
	            	if (context.getFocusContext().getObjectCurrent() != null) {
	            		focus = context.getFocusContext().getObjectCurrent();
	            	} else {
	            		focus = context.getFocusContext().getObjectNew();
	            	}

	                ItemDelta focusItemDelta = null;
	                if (attributeAPrioriDelta != null) {
	                    LOGGER.trace("Processing inbound from a priori delta: {}", aPrioriDelta);
	                    focusItemDelta = evaluateInboundMapping(context, inboundMappingType, accountAttributeName, null,
								attributeAPrioriDelta, focus, accountNew, projContext.getResource(), task, result);
	                } else if (accountCurrent != null) {
	                	if (!projContext.isFullShadow()) {
	                		LOGGER.warn("Attempted to execute inbound expression on account shadow {} WITHOUT full account. Trying to load the account now.", projContext.getOid());      // todo change to trace level eventually
							accountCurrent = loadProjection(context, projContext, task, result, accountCurrent);
							if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
								return;
							}
                            if (!projContext.isFullShadow()) {
                            	if (projContext.getResourceShadowDiscriminator().getOrder() > 0) {
                            		// higher-order context. It is OK not to load this
                            		LOGGER.trace("Skipped load of higher-order account with shadow OID {} skipping inbound processing on it", projContext.getOid());
                            		return;
                            	}
								// TODO: is it good to mark as broken? what is
								// the resorce is down?? if there is no
								// assignment and the account was added directly
								// it can cause that the account will be
								// unlinked from the user FIXME
                                LOGGER.warn("Couldn't load account with shadow OID {}, setting context as broken and skipping inbound processing on it", projContext.getOid());
                                projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
                                return;
                            }
                        }
	                    PrismProperty<?> oldAccountProperty = accountCurrent.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, accountAttributeName));
	                    LOGGER.trace("Processing inbound from account sync absolute state (currentAccount): {}", oldAccountProperty);
	                    focusItemDelta = evaluateInboundMapping(context, inboundMappingType, accountAttributeName, oldAccountProperty, null, 
	                    		focus, accountNew, projContext.getResource(), task, result);
	                }
	
	                if (focusItemDelta != null && !focusItemDelta.isEmpty()) {
	                	if (LOGGER.isTraceEnabled()) {
	                		LOGGER.trace("Created delta (from inbound expression for {} on {})\n{}", accountAttributeName, projContext.getResource(), focusItemDelta.debugDump(1));
	                	}
	                    context.getFocusContext().swallowToProjectionWaveSecondaryDelta(focusItemDelta);
	                    context.recomputeFocus();
	                } else {
	                    LOGGER.trace("Created delta (from inbound expression for {} on {}) was null or empty.", accountAttributeName, projContext.getResource());
	                }
	            }
            }
        }

		if (isDeleteAccountDelta(projContext)) {
			// we don't need to do inbound if account was deleted
			return;
		}
        processSpecialPropertyInbound(accountDefinition.getPasswordInbound(), SchemaConstants.PATH_PASSWORD_VALUE,
        		context.getFocusContext().getObjectNew(), projContext, accountDefinition, context, task, now, result);
        
        processSpecialPropertyInbound(accountDefinition.getActivationBidirectionalMappingType(ActivationType.F_ADMINISTRATIVE_STATUS), SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
        		context.getFocusContext().getObjectNew(), projContext, accountDefinition, context, task, now, result);        
        processSpecialPropertyInbound(accountDefinition.getActivationBidirectionalMappingType(ActivationType.F_VALID_FROM), SchemaConstants.PATH_ACTIVATION_VALID_FROM,
        		context.getFocusContext().getObjectNew(), projContext, accountDefinition, context, task, now, result);
        processSpecialPropertyInbound(accountDefinition.getActivationBidirectionalMappingType(ActivationType.F_VALID_TO), SchemaConstants.PATH_ACTIVATION_VALID_TO,
        		context.getFocusContext().getObjectNew(), projContext, accountDefinition, context, task, now, result);
    }

	private <F extends FocusType> PrismObject<ShadowType> loadProjection(LensContext<F> context,
			LensProjectionContext projContext, Task task, OperationResult result, PrismObject<ShadowType> accountCurrent)
			throws SchemaException {
		try {
			contextLoader.loadFullShadow(context, projContext, task, result);
			accountCurrent = projContext.getObjectCurrent();
		} catch (ObjectNotFoundException |SecurityViolationException |CommunicationException |ConfigurationException e) {
			LOGGER.warn("Couldn't load account with shadow OID {} because of {}, setting context as broken and skipping inbound processing on it", projContext.getOid(), e.getMessage());
			projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
		}
		return accountCurrent;
	}

	private boolean hasAnyStrongMapping(RefinedObjectClassDefinition objectDefinition) {
    	
    	for (QName attributeName : objectDefinition.getNamesOfAttributesWithInboundExpressions()) {
    		RefinedAttributeDefinition<?> attributeDefinition = objectDefinition.findAttributeDefinition(attributeName);
    		for (MappingType inboundMapping : attributeDefinition.getInboundMappingTypes()){
    			if (inboundMapping.getStrength() == MappingStrengthType.STRONG) {
    				return true;
    			}
    		}
    	}
    	
    	return false;
    }

    /**
	 * A priori delta is a delta that was executed in a previous "step". That means it is either delta from a previous
	 * wave or a sync delta (in wave 0).
	 */
	private <F extends ObjectType> ObjectDelta<ShadowType> getAPrioriDelta(LensContext<F> context, 
			LensProjectionContext accountContext) throws SchemaException {
		int wave = context.getProjectionWave();
		if (wave == 0) {
			return accountContext.getSyncDelta();
		}
		if (wave == accountContext.getWave() + 1) {
			// If this resource was processed in a previous wave ....
			// Normally, we take executed delta. However, there are situations (like preview changes - i.e. projector without execution),
			// when there is no executed delta. In that case we take standard primary + secondary delta.
			// TODO is this really correct? Think if the following can happen:
			// - NOT previewing
			// - no executed deltas but
			// - existing primary/secondary delta.
			List<LensObjectDeltaOperation<ShadowType>> executed = accountContext.getExecutedDeltas();
			if (executed != null && !executed.isEmpty()) {
				return executed.get(executed.size()-1).getObjectDelta();
			} else {
				return accountContext.getDelta();
			}
		}
		return null;
	}

	private <F extends ObjectType> boolean checkWeakSkip(Mapping<?,?> inbound, PrismObject<F> newUser) throws SchemaException {
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
    
    private <A, F extends FocusType, V extends PrismValue,D extends ItemDefinition> ItemDelta<V,D> evaluateInboundMapping(final LensContext<F> context, 
    		MappingType inboundMappingType, 
    		QName accountAttributeName, PrismProperty<A> oldAccountProperty, PropertyDelta<A> attributeAPrioriDelta,
            PrismObject<F> focusNew, PrismObject<ShadowType> account, ResourceType resource, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, ConfigurationException {

    	if (oldAccountProperty != null && oldAccountProperty.hasRaw()) {
        	throw new SystemException("Property "+oldAccountProperty+" has raw parsing state, such property cannot be used in inbound expressions");
        }
    	
    	Mapping.Builder<V,D> builder = mappingFactory.createMappingBuilder(inboundMappingType,
    			"inbound expression for "+accountAttributeName+" in "+resource);
    	
    	if (!builder.isApplicableToChannel(context.getChannel())) {
    		return null;
    	}
    	
    	Source<PrismPropertyValue<A>,PrismPropertyDefinition<A>> defaultSource = new Source<>(oldAccountProperty, attributeAPrioriDelta, null, ExpressionConstants.VAR_INPUT);
    	defaultSource.recompute();
		Mapping<V,D> mapping = builder.defaultSource(defaultSource)
				.targetContext(LensUtil.getFocusDefinition(context))
				.addVariableDefinition(ExpressionConstants.VAR_USER, focusNew)
    			.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusNew)
    			.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, account)
				.addVariableDefinition(ExpressionConstants.VAR_SHADOW, account)
				.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource)
				.stringPolicyResolver(createStringPolicyResolver(context, task, result))
				.originType(OriginType.INBOUND)
				.originObject(resource)
				.build();
    	
    	if (checkWeakSkip(mapping, focusNew)) {
            LOGGER.trace("Skipping because of mapping is weak and focus property has already a value");
            return null;
        }
        
        ItemPath targetFocusItemPath = mapping.getOutputPath();
        if (ItemPath.isNullOrEmpty(targetFocusItemPath)) {
        	throw new ConfigurationException("Empty target path in "+mapping.getContextDescription());
        }
        Item targetFocusItem = null;
        if (focusNew != null) {
        	targetFocusItem = focusNew.findItem(targetFocusItemPath);
        }
        PrismObjectDefinition<F> focusDefinition = context.getFocusContext().getObjectDefinition();
        ItemDefinition targetItemDef = focusDefinition.findItemDefinition(targetFocusItemPath);
        if (targetItemDef == null) {
        	throw new SchemaException("No definition for focus property "+targetFocusItemPath+", cannot process inbound expression in "+resource);
        }
        final ItemDelta outputFocusItemDelta = targetItemDef.createEmptyDelta(targetFocusItemPath);
    	
        mappingEvaluator.evaluateMapping(mapping, context, task, result);
        
    	PrismValueDeltaSetTriple<V> triple = mapping.getOutputTriple();
    	// Meaning of the resulting triple:
    	//   values in PLUS set will be added     (valuesToAdd in delta)
    	//   values in MINUS set will be removed  (valuesToDelete in delta)
    	//   values in ZERO set will be compared with existing values in user property
    	//                  the differences will be added to delta
    	
    	if (LOGGER.isTraceEnabled()) {
    		LOGGER.trace("Inbound mapping for {} returned triple:\n{}", accountAttributeName, triple == null ? "null" : triple.debugDump());
    	}
        
    	if (triple != null) {
    		
	        if (triple.hasPlusSet()) {

				boolean alreadyReplaced = false;

				for (V value : triple.getPlusSet()) {

	                if (targetFocusItem != null && targetFocusItem.hasRealValue(value)) {
	                    continue;
	                }
	
	                //if property is not multi value replace existing attribute
	                if (targetFocusItem != null && !targetFocusItem.getDefinition().isMultiValue() && !targetFocusItem.isEmpty()) {
	                    Collection<V> replace = new ArrayList<V>();
	                    replace.add((V) value.clone());
	                    outputFocusItemDelta.setValuesToReplace(replace);

						if (alreadyReplaced) {
							LOGGER.warn("Multiple values for a single-valued property {}; duplicate value = {}", targetFocusItem, value);
						} else {
							alreadyReplaced = true;
						}
	                } else {
	                    outputFocusItemDelta.addValueToAdd(value.clone());
	                }
	            }
	        }

	        if (triple.hasMinusSet()) {
	            LOGGER.trace("Checking account sync property delta values to delete");
	            for (V value : triple.getMinusSet()) {

	                if (targetFocusItem == null || targetFocusItem.hasRealValue(value)) {
	                	if (!outputFocusItemDelta.isReplace()) {
	                		// This is not needed if we are going to replace. In fact it might cause an error.
	                		outputFocusItemDelta.addValueToDelete(value);
	                	}
	                }
	            }
	        }
	        
        	Item shouldBeItem = targetItemDef.instantiate();
	    	shouldBeItem.addAll(PrismValue.cloneCollection(triple.getZeroSet()));
	    	shouldBeItem.addAll(PrismValue.cloneCollection(triple.getPlusSet()));
	        if (targetFocusItem != null) {
	            ItemDelta diffDelta = targetFocusItem.diff(shouldBeItem);
	            if (LOGGER.isTraceEnabled()) {
	        		LOGGER.trace("Comparing focus item:\n{}\nto should be item:\n{}\ndiff:\n{} ",
							DebugUtil.debugDump(targetFocusItem, 1),
							DebugUtil.debugDump(shouldBeItem, 1),
							DebugUtil.debugDump(diffDelta, 1));
	        	}
	            if (diffDelta != null) {
	            	if (mapping.isTolerant() == Boolean.TRUE) {			// this is probably not correct, as the default for inbounds should be TRUE
	            		if (diffDelta.isReplace()) {
	            			if (diffDelta.getValuesToReplace().isEmpty()) {
	            				diffDelta.resetValuesToReplace();
		            			if (LOGGER.isTraceEnabled()) {
			            			LOGGER.trace("Removing empty replace part of the diff delta because mapping is tolerant:\n{}", diffDelta.debugDump());
			            		}
	            			} else {
	            				if (LOGGER.isTraceEnabled()) {
			            			LOGGER.trace("Making sure that the replace part of the diff contains old values delta because mapping is tolerant:\n{}", diffDelta.debugDump());
			            		}
	            				for (Object shouldBeValueObj: shouldBeItem.getValues()) {
	            					PrismValue shouldBeValue = (PrismValue)shouldBeValueObj;
	            					if (!PrismValue.containsRealValue(diffDelta.getValuesToReplace(), shouldBeValue)) {
	            						diffDelta.addValueToReplace(shouldBeValue.clone());
	            					}
	            				}
	            			}
	            		} else {
		            		diffDelta.resetValuesToDelete();
		            		if (LOGGER.isTraceEnabled()) {
		            			LOGGER.trace("Removing delete part of the diff delta because mapping is tolerant:\n{}", diffDelta.debugDump());
		            		}
	            		}
	            	}
	            	diffDelta.setElementName(ItemPath.getName(targetFocusItemPath.last()));
	            	diffDelta.setParentPath(targetFocusItemPath.allExceptLast());
	            	outputFocusItemDelta.merge(diffDelta);
	            }
	        } else {
	        	if (LOGGER.isTraceEnabled()) {
	        		LOGGER.trace("Adding user property because inbound say so (account doesn't contain that value):\n{}",
                		shouldBeItem.getValues());
	        	}
                //if user property doesn't exist we have to add it (as delta), because inbound say so
                outputFocusItemDelta.addValuesToAdd(shouldBeItem.getClonedValues());
	        }
	        
    	} else { // triple == null
    		
    		// the mapping is not applicable. Nothing to do.
    		
    	}

        // if no changes were generated return null
        return outputFocusItemDelta.isEmpty() ? null : outputFocusItemDelta;
    }

	private <F extends ObjectType> StringPolicyResolver createStringPolicyResolver(final LensContext<F> context, final Task task, final OperationResult result) {
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
				ValuePolicyType passwordPolicy = passwordPolicyProcessor.determinePasswordPolicy(context.getFocusContext(), task, result);
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
	 * @throws ObjectNotFoundException 
	 * @throws ExpressionEvaluationException 
     */
    private <F extends FocusType> void processSpecialPropertyInbound(ResourceBidirectionalMappingType biMappingType, ItemPath sourcePath,
            PrismObject<F> newUser, LensProjectionContext accContext, 
            RefinedObjectClassDefinition accountDefinition, LensContext<F> context,
            Task task, XMLGregorianCalendar now, OperationResult opResult) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	if (biMappingType == null) {
    		return;
    	}
    	processSpecialPropertyInbound(biMappingType.getInbound(), sourcePath, newUser, accContext, accountDefinition, context, task, now, opResult);
    }

//    private void processSpecialPropertyInbound(MappingType inboundMappingType, ItemPath sourcePath,
//            PrismObject<UserType> newUser, LensProjectionContext<ShadowType> accContext, 
//            RefinedObjectClassDefinition accountDefinition, LensContext<UserType,ShadowType> context, 
//            OperationResult opResult) throws SchemaException {
//    	if (inboundMappingType == null) {
//    		return;
//    	}
//    	Collection<MappingType> inboundMappingTypes = new ArrayList<MappingType>(1);
//    	inboundMappingTypes.add(inboundMappingType);
//    	processSpecialPropertyInbound(inboundMappingTypes, sourcePath, newUser, accContext, accountDefinition, context, opResult);
//    }

    /**
     * Processing for special (fixed-schema) properties such as credentials and activation. 
     * @throws ObjectNotFoundException 
     * @throws ExpressionEvaluationException 
     */
    private <F extends FocusType> void processSpecialPropertyInbound(Collection<MappingType> inboundMappingTypes, final ItemPath sourcePath,
            final PrismObject<F> newUser, final LensProjectionContext accContext,
            RefinedObjectClassDefinition accountDefinition, final LensContext<F> context,
            Task task, XMLGregorianCalendar now, OperationResult opResult) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

        if (inboundMappingTypes == null || inboundMappingTypes.isEmpty() || newUser == null || !accContext.isFullShadow()) {
            return;
        }
        
        ObjectDelta<F> userPrimaryDelta = context.getFocusContext().getPrimaryDelta();
        PropertyDelta primaryPropDelta = null;
        if (userPrimaryDelta != null) {
        	primaryPropDelta = userPrimaryDelta.findPropertyDelta(sourcePath);
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
        
        MappingInitializer initializer = new MappingInitializer() {
			@Override
			public Mapping.Builder initialize(Mapping.Builder builder) throws SchemaException {
				if (accContext.getObjectNew() == null) {
					accContext.recompute();
					if (accContext.getObjectNew() == null) {
						// Still null? something must be really wrong here.
						String message = "Recomputing account " + accContext.getResourceShadowDiscriminator()
								+ " results in null new account. Something must be really broken.";
						LOGGER.error(message);
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Account context:\n{}", accContext.debugDump());
						}
						throw new SystemException(message);
					}
				}

				ObjectDelta<ShadowType> aPrioriShadowDelta = getAPrioriDelta(context, accContext);
				ItemDelta<PrismPropertyValue<?>,PrismPropertyDefinition<?>> specialAttributeDelta = null;
				if (aPrioriShadowDelta != null){
					specialAttributeDelta = aPrioriShadowDelta.findItemDelta(sourcePath);
				}
				ItemDeltaItem<PrismPropertyValue<?>,PrismPropertyDefinition<?>> sourceIdi = accContext.getObjectDeltaObject().findIdi(sourcePath);
				if (specialAttributeDelta == null){
					specialAttributeDelta = sourceIdi.getDelta();
				}
				Source<PrismPropertyValue<?>,PrismPropertyDefinition<?>> source = new Source<>(sourceIdi.getItemOld(), specialAttributeDelta,
						sourceIdi.getItemOld(), ExpressionConstants.VAR_INPUT);
				builder = builder.defaultSource(source)
						.addVariableDefinition(ExpressionConstants.VAR_USER, newUser)
						.addVariableDefinition(ExpressionConstants.VAR_FOCUS, newUser);

				PrismObject<ShadowType> accountNew = accContext.getObjectNew();
				builder = builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, accountNew)
						.addVariableDefinition(ExpressionConstants.VAR_SHADOW, accountNew)
						.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, accContext.getResource())
						.stringPolicyResolver(createStringPolicyResolver(context, task, opResult))
						.originType(OriginType.INBOUND)
						.originObject(accContext.getResource());
				return builder;
			}
        };
        
        MappingOutputProcessor<PrismValue> processor = new MappingOutputProcessor<PrismValue>() {
			@Override
			public void process(ItemPath mappingOutputPath, PrismValueDeltaSetTriple<PrismValue> outputTriple)
					throws ExpressionEvaluationException, SchemaException {
		        if (outputTriple == null){
		        	LOGGER.trace("Mapping for property {} evaluated to null. Skipping inboud processing for that property.", sourcePath);
		        	return;
		        }
		        
		        ObjectDelta<F> userSecondaryDelta = context.getFocusContext().getProjectionWaveSecondaryDelta();
		        if (userSecondaryDelta != null) {
			        PropertyDelta<?> delta = userSecondaryDelta.findPropertyDelta(sourcePath);
			        if (delta != null) {
			            //remove delta if exists, it will be handled by inbound
			            userSecondaryDelta.getModifications().remove(delta);
			        }
		        }
		        
		        PrismObjectDefinition<F> focusDefinition = context.getFocusContext().getObjectDefinition();
		        PrismProperty result = focusDefinition.findPropertyDefinition(sourcePath).instantiate();
		    	result.addAll(PrismValue.cloneCollection(outputTriple.getNonNegativeValues()));
		        
		    	PrismProperty targetPropertyNew = newUser.findOrCreateProperty(sourcePath);
		    	PropertyDelta<?> delta;
		    	if (ProtectedStringType.COMPLEX_TYPE.equals(targetPropertyNew.getDefinition().getTypeName())) {
		    		// We have to compare this in a special way. The cipherdata may be different due to a different
		    		// IV, but the value may still be the same
		    		ProtectedStringType resultValue = (ProtectedStringType) result.getRealValue();
		    		ProtectedStringType targetPropertyNewValue = (ProtectedStringType) targetPropertyNew.getRealValue();
		    		try {
						if (protector.compare(resultValue, targetPropertyNewValue)) {
							delta = null;
						} else {
							delta = targetPropertyNew.diff(result);
						}
					} catch (EncryptionException e) {
						throw new SystemException(e.getMessage(), e);
					}
		    	} else {
		    		delta = targetPropertyNew.diff(result);
		    	}
		    	if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("targetPropertyNew:\n{}\ndelta:\n{}", targetPropertyNew.debugDump(1), DebugUtil.debugDump(delta, 1));
				}
		        if (delta != null && !delta.isEmpty()) {
		        	delta.setParentPath(sourcePath.allExceptLast());
		        	if (!context.getFocusContext().alreadyHasDelta(delta)){
		        		context.getFocusContext().swallowToProjectionWaveSecondaryDelta(delta);
		        	}
		        }

			}
		};
        
        MappingEvaluatorParams<PrismValue, ItemDefinition, F, F> params = new MappingEvaluatorParams<>();
        params.setMappingTypes(inboundMappingTypes);
        params.setMappingDesc("inbound mapping for " + sourcePath + " in " + accContext.getResource());
        params.setNow(now);
        params.setInitializer(initializer);
		params.setProcessor(processor);
        params.setAPrioriTargetObject(newUser);
        params.setAPrioriTargetDelta(userPrimaryDelta);
        params.setTargetContext(context.getFocusContext());
        params.setDefaultTargetItemPath(sourcePath);
        params.setEvaluateCurrent(true);
        params.setContext(context);
        params.setHasFullTargetObject(true);
		mappingEvaluator.evaluateMappingSetProjection(params, task, opResult);
        
//        MutableBoolean strongMappingWasUsed = new MutableBoolean();
//        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> outputTriple = mappingEvaluatorHelper.evaluateMappingSetProjection(
//                inboundMappingTypes, "inbound mapping for " + sourcePath + " in " + accContext.getResource(), now, initializer, targetPropertyNew, primaryPropDelta, newUser, true, strongMappingWasUsed, context, accContext, task, opResult);
		
    }
    
//    private Collection<Mapping> getMappingApplicableToChannel(
//			Collection<MappingType> inboundMappingTypes, String description, String channelUri) {
//    	Collection<Mapping> inboundMappings = new ArrayList<Mapping>();
//		for (MappingType inboundMappingType : inboundMappingTypes){
//			Mapping<PrismPropertyValue<?>,PrismPropertyDefinition<?>> mapping = mappingFactory.createMapping(inboundMappingType,
//	        		description);
//
//			if (mapping.isApplicableToChannel(channelUri)){
//				inboundMappings.add(mapping);
//			}
//		}
//
//		return inboundMappings;
//	}
}
