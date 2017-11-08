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

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.common.filter.FilterManager;
import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;

import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensObjectDeltaOperation;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.ConsolidationProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluatorParams;
import com.evolveum.midpoint.model.impl.lens.projector.MappingInitializer;
import com.evolveum.midpoint.model.impl.lens.projector.MappingOutputProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.repo.common.expression.ValuePolicyResolver;
import com.evolveum.midpoint.repo.common.expression.VariableProducer;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionVariableDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingAndDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueFilterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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

    @Autowired private PrismContext prismContext;
    @Autowired private FilterManager<Filter> filterManager;
    @Autowired private MappingFactory mappingFactory;
    @Autowired private ContextLoader contextLoader;
    @Autowired private CredentialsProcessor credentialsProcessor;
    @Autowired private MappingEvaluator mappingEvaluator;
    @Autowired private Protector protector;
    @Autowired private ProvisioningService provisioningService;
    
    private Map<ItemDefinition, List<Mapping<?,?>>> mappingsToTarget;

    <O extends ObjectType> void processInbound(LensContext<O> context, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException, CommunicationException, SecurityViolationException {
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
			OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException, CommunicationException, SecurityViolationException {
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

                processInboundMappingsForProjection(context, projectionContext, rOcDef, aPrioriDelta, task, now, subResult);
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

    private <F extends FocusType, V extends PrismValue, D extends ItemDefinition> void processInboundMappingsForProjection(LensContext<F> context,
    		LensProjectionContext projContext,
            RefinedObjectClassDefinition projectionDefinition, ObjectDelta<ShadowType> aPrioriProjectionDelta, Task task, XMLGregorianCalendar now, OperationResult result)
    		throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException, CommunicationException, SecurityViolationException {

        if (aPrioriProjectionDelta == null && projContext.getObjectCurrent() == null) {
            LOGGER.trace("Nothing to process in inbound, both a priori delta and current account were null.");
            return;
        }

        PrismObject<ShadowType> accountCurrent = projContext.getObjectCurrent();
        if (hasAnyStrongMapping(projectionDefinition) && !projContext.isFullShadow() && !projContext.isThombstone()) {
        	LOGGER.trace("There are strong inbound mapping, but the shadow hasn't be fully loaded yet. Trying to load full shadow now.");
			accountCurrent = loadProjection(context, projContext, task, result, accountCurrent);
			if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
				return;
			}
		}
        
        mappingsToTarget = new HashMap<>();

        for (QName accountAttributeName : projectionDefinition.getNamesOfAttributesWithInboundExpressions()) {
        	boolean cont = processAttributeInbound(accountAttributeName, aPrioriProjectionDelta, projContext, projectionDefinition, context, now, task, result);
        	if (!cont) {
        		return;
        	}
        }
        
        for (QName accountAttributeName : projectionDefinition.getNamesOfAssociationsWithInboundExpressions()) {
        	boolean cont = processAssociationInbound(accountAttributeName, aPrioriProjectionDelta, projContext, projectionDefinition, context, now, task, result);
        	if (!cont) {
        		return;
        	}
        }

		if (isDeleteAccountDelta(projContext)) {
			// we don't need to do inbound if account was deleted
			return;
		}
        processSpecialPropertyInbound(projectionDefinition.getPasswordInbound(), SchemaConstants.PATH_PASSWORD_VALUE, SchemaConstants.PATH_PASSWORD_VALUE,
        		context.getFocusContext().getObjectNew(), projContext, projectionDefinition, context, now, task, result);

        processSpecialPropertyInbound(projectionDefinition.getActivationBidirectionalMappingType(ActivationType.F_ADMINISTRATIVE_STATUS), SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
        		context.getFocusContext().getObjectNew(), projContext, projectionDefinition, context, now, task, result);
        processSpecialPropertyInbound(projectionDefinition.getActivationBidirectionalMappingType(ActivationType.F_VALID_FROM), SchemaConstants.PATH_ACTIVATION_VALID_FROM,
        		context.getFocusContext().getObjectNew(), projContext, projectionDefinition, context, now, task, result);
        processSpecialPropertyInbound(projectionDefinition.getActivationBidirectionalMappingType(ActivationType.F_VALID_TO), SchemaConstants.PATH_ACTIVATION_VALID_TO,
        		context.getFocusContext().getObjectNew(), projContext, projectionDefinition, context, now, task, result);

        processAuxiliaryObjectClassInbound(aPrioriProjectionDelta, projContext, projectionDefinition, context, now, task, result);
        
	    Collection<ItemDelta<V, D>> deltas = evaluateInboundMapping(context, projContext, task, result);
	    
	    if (deltas == null) {
	    	LOGGER.trace("No focus delta poduces from inboud mappings");
	    	return;
	    }
	    
	    for (ItemDelta<V, D> focusItemDelta : deltas) {
	    	  if (focusItemDelta != null && !focusItemDelta.isEmpty()) {
	            	if (LOGGER.isTraceEnabled()) {
	            		LOGGER.trace("Created delta (from inbound expression for {} on {})\n{}", focusItemDelta.getElementName(), projContext.getResource(), focusItemDelta.debugDump(1));
	            	}
	                context.getFocusContext().swallowToProjectionWaveSecondaryDelta(focusItemDelta);
	                context.recomputeFocus();
	            } else {
	                LOGGER.trace("Created delta (from inbound expression for {} on {}) was null or empty.", focusItemDelta.getElementName(), projContext.getResource());
	            }
	    }
    }

	private <V extends PrismValue, D extends ItemDefinition, F extends FocusType> boolean processAttributeInbound(QName accountAttributeName,
			ObjectDelta<ShadowType> aPrioriProjectionDelta, final LensProjectionContext projContext,
            RefinedObjectClassDefinition projectionDefinition, final LensContext<F> context,
            XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, CommunicationException {

		PrismObject<ShadowType> projCurrent = projContext.getObjectCurrent();
        PrismObject<ShadowType> projNew = projContext.getObjectNew();

        final ItemDelta<V, D> attributeAPrioriDelta;
        if (aPrioriProjectionDelta != null) {
            attributeAPrioriDelta = aPrioriProjectionDelta.findItemDelta(new ItemPath(SchemaConstants.C_ATTRIBUTES, accountAttributeName));
            if (attributeAPrioriDelta == null && !projContext.isFullShadow() && !LensUtil.hasDependentContext(context, projContext)) {
				LOGGER.trace("Skipping inbound for {} in {}: Not a full shadow and account a priori delta exists, but doesn't have change for processed property.",
						accountAttributeName, projContext.getResourceShadowDiscriminator());
				return true;
            }
        } else {
        	attributeAPrioriDelta = null;
		}

        RefinedAttributeDefinition<?> attrDef = projectionDefinition.findAttributeDefinition(accountAttributeName);

        if (attrDef.isIgnored(LayerType.MODEL)) {
        	LOGGER.trace("Skipping inbound for attribute {} in {} because the attribute is ignored",
            		PrettyPrinter.prettyPrint(accountAttributeName), projContext.getResourceShadowDiscriminator());
        	return true;
        }

        List<MappingType> inboundMappingTypes = attrDef.getInboundMappingTypes();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Processing inbound for {} in {}; ({} mappings)", PrettyPrinter.prettyPrint(accountAttributeName),
					projContext.getResourceShadowDiscriminator(), inboundMappingTypes.size());
		}

    	PropertyLimitations limitations = attrDef.getLimitations(LayerType.MODEL);
    	if (limitations != null) {
    		PropertyAccessType access = limitations.getAccess();
    		if (access != null) {
    			if (access.isRead() == null || !access.isRead()) {
    				LOGGER.warn("Inbound mapping for non-readable attribute {} in {}, skipping",
    						accountAttributeName, projContext.getHumanReadableName());
    				return true;
    			}
    		}
    	}

        if (inboundMappingTypes.isEmpty()) {
        	return true;
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
					projCurrent = loadProjection(context, projContext, task, result, projCurrent);
					if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
						return false;
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

//            ItemDelta focusItemDelta = null;
            if (attributeAPrioriDelta != null) {
                LOGGER.trace("Processing inbound from a priori delta: {}", aPrioriProjectionDelta);
                collectMappingsForTargets(context, projContext, inboundMappingType, accountAttributeName, null, attributeAPrioriDelta, focus, null, task, result);
//                focusItemDelta = evaluateInboundMapping(context, projContext, inboundMappingType, accountAttributeName, null,
//						attributeAPrioriDelta, focus, null, task, result);

            } else if (projCurrent != null) {

            	projCurrent = loadFullShadowIfNeeded(projContext, projCurrent, context, now, task, result);
            	if (projCurrent == null) {
            		return false;
            	}

                PrismProperty<?> oldAccountProperty = projCurrent.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, accountAttributeName));
                LOGGER.trace("Processing inbound from account sync absolute state (currentAccount): {}", oldAccountProperty);
                collectMappingsForTargets(context, projContext, inboundMappingType, accountAttributeName, oldAccountProperty, null,
                		focus, null, task, result);
            }

//            if (focusItemDelta != null && !focusItemDelta.isEmpty()) {
//            	if (LOGGER.isTraceEnabled()) {
//            		LOGGER.trace("Created delta (from inbound expression for {} on {})\n{}", accountAttributeName, projContext.getResource(), focusItemDelta.debugDump(1));
//            	}
//                context.getFocusContext().swallowToProjectionWaveSecondaryDelta(focusItemDelta);
//                context.recomputeFocus();
//            } else {
//                LOGGER.trace("Created delta (from inbound expression for {} on {}) was null or empty.", accountAttributeName, projContext.getResource());
//            }
        }

        return true;
	}
	
	private <V extends PrismValue, D extends ItemDefinition, F extends FocusType> boolean processAssociationInbound(QName accountAttributeName,
			ObjectDelta<ShadowType> aPrioriProjectionDelta, final LensProjectionContext projContext,
            RefinedObjectClassDefinition projectionDefinition, final LensContext<F> context,
            XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, CommunicationException {

		PrismObject<ShadowType> projCurrent = projContext.getObjectCurrent();
        PrismObject<ShadowType> projNew = projContext.getObjectNew();

        final ItemDelta<V, D> attributeAPrioriDelta;
        if (aPrioriProjectionDelta != null) {
            attributeAPrioriDelta = aPrioriProjectionDelta.findItemDelta(new ItemPath(ShadowType.F_ASSOCIATION));
            if (attributeAPrioriDelta == null && !projContext.isFullShadow() && !LensUtil.hasDependentContext(context, projContext)) {
				LOGGER.trace("Skipping inbound for {} in {}: Not a full shadow and account a priori delta exists, but doesn't have change for processed property.",
						accountAttributeName, projContext.getResourceShadowDiscriminator());
				return true;
            }
        } else {
        	attributeAPrioriDelta = null;
		}

        RefinedAssociationDefinition associationDef = projectionDefinition.findAssociationDefinition(accountAttributeName);

        //TODO:
        if (associationDef.isIgnored(LayerType.MODEL)) {
        	LOGGER.trace("Skipping inbound for association {} in {} because the association is ignored",
            		PrettyPrinter.prettyPrint(accountAttributeName), projContext.getResourceShadowDiscriminator());
        	return true;
        }

        List<MappingType> inboundMappingTypes = associationDef.getInboundMappingTypes();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Processing inbound for {} in {}; ({} mappings)", PrettyPrinter.prettyPrint(accountAttributeName),
					projContext.getResourceShadowDiscriminator(), inboundMappingTypes.size());
		}

    	PropertyLimitations limitations = associationDef.getLimitations(LayerType.MODEL);
    	if (limitations != null) {
    		PropertyAccessType access = limitations.getAccess();
    		if (access != null) {
    			if (access.isRead() == null || !access.isRead()) {
    				LOGGER.warn("Inbound mapping for non-readable association {} in {}, skipping",
    						accountAttributeName, projContext.getHumanReadableName());
    				return true;
    			}
    		}
    	}

        if (inboundMappingTypes.isEmpty()) {
        	return true;
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
					projCurrent = loadProjection(context, projContext, task, result, projCurrent);
					if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
						return false;
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
                LOGGER.trace("Processing association inbound from a priori delta: {}", attributeAPrioriDelta);
                resolveEntitlementsIfNeeded((ContainerDelta<ShadowAssociationType>) attributeAPrioriDelta, null, projContext, task, result);
                VariableProducer<PrismContainerValue<ShadowAssociationType>> entitlementVariable = 
                		(value, variables) -> {
                			LOGGER.info("Producing value {} " + value);
                			PrismObject<ShadowType> entitlement = projContext.getEntitlementMap().get(value.findReference(ShadowAssociationType.F_SHADOW_REF).getOid());
                			LOGGER.trace("Resolved entitlement {}", entitlement);
                			variables.addVariableDefinition(ExpressionConstants.VAR_ENTITLEMENT, entitlement);
                		};
                collectMappingsForTargets(context, projContext, inboundMappingType, accountAttributeName, null,
						attributeAPrioriDelta, focus, (VariableProducer) entitlementVariable, task, result);

            } else if (projCurrent != null) {

            	projCurrent = loadFullShadowIfNeeded(projContext, projCurrent, context, now, task, result);
            	if (projCurrent == null) {
            		LOGGER.trace("Loading of full shadow failed");
            		return false;
            	}

                PrismContainer<ShadowAssociationType> oldShadowAssociation = projCurrent.findContainer(ShadowType.F_ASSOCIATION);
                
                if (oldShadowAssociation == null) {
                	LOGGER.trace("No shadow association value");
                	return true;
                }
                
                PrismContainer<ShadowAssociationType> filteredAssociations = oldShadowAssociation.getDefinition().instantiate();
                Collection<PrismContainerValue<ShadowAssociationType>> filteredAssociationValues = oldShadowAssociation.getValues().stream()
                		.filter(rVal -> accountAttributeName.equals(rVal.asContainerable().getName()))
                		.map(val -> val.clone())
                		.collect(Collectors.toCollection(ArrayList::new));
                prismContext.adopt(filteredAssociations);
                filteredAssociations.addAll(filteredAssociationValues);
                
                resolveEntitlementsIfNeeded((ContainerDelta<ShadowAssociationType>) attributeAPrioriDelta, filteredAssociations, projContext, task, result);
                
                VariableProducer<PrismContainerValue<ShadowAssociationType>> entitlementVariable = 
                		(value, variables) -> {
                			LOGGER.trace("Producing value {} " + value);
                			PrismObject<ShadowType> entitlement = projContext.getEntitlementMap().get(value.findReference(ShadowAssociationType.F_SHADOW_REF).getOid());
                			LOGGER.trace("Resolved entitlement {}", entitlement);
                			variables.addVariableDefinition(ExpressionConstants.VAR_ENTITLEMENT, entitlement);
                		};
                		
                LOGGER.trace("Processing association inbound from account sync absolute state (currentAccount): {}", filteredAssociations);
				collectMappingsForTargets(context, projContext, inboundMappingType, accountAttributeName, filteredAssociations,
						null, focus, entitlementVariable, task, result);
                
            }
        }

        return true;
	}

	private <F extends FocusType> void processAuxiliaryObjectClassInbound(
			ObjectDelta<ShadowType> aPrioriProjectionDelta, final LensProjectionContext projContext,
            RefinedObjectClassDefinition projectionDefinition, final LensContext<F> context,
            XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, CommunicationException {

        ResourceBidirectionalMappingAndDefinitionType auxiliaryObjectClassMappings = projectionDefinition.getAuxiliaryObjectClassMappings();
        if (auxiliaryObjectClassMappings == null) {
        	return;
        }
        List<MappingType> inboundMappingTypes = auxiliaryObjectClassMappings.getInbound();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Processing inbound for auxiliary object class in {}; ({} mappings)",
					projContext.getResourceShadowDiscriminator(), inboundMappingTypes.size());
		}
        if (inboundMappingTypes.isEmpty()) {
        	return;
        }

		PrismObject<ShadowType> projCurrent = projContext.getObjectCurrent();
        PrismObject<ShadowType> projNew = projContext.getObjectNew();

        final PropertyDelta<QName> attributeAPrioriDelta;
        if (aPrioriProjectionDelta != null) {
            attributeAPrioriDelta = aPrioriProjectionDelta.findPropertyDelta(ShadowType.F_AUXILIARY_OBJECT_CLASS);
            if (attributeAPrioriDelta == null && !projContext.isFullShadow() && !LensUtil.hasDependentContext(context, projContext)) {
				LOGGER.trace("Skipping inbound for auxiliary object class in {}: Not a full shadow and account a priori delta exists, but doesn't have change for processed property.",
						projContext.getResourceShadowDiscriminator());
				return;
            }
        } else {
        	attributeAPrioriDelta = null;
		}

        // Make we always have full shadow when dealing with auxiliary object classes.
        // Unlike structural object class the auxiliary object classes may have changed
        // on the resource
        projCurrent = loadFullShadowIfNeeded(projContext, projCurrent, context, now, task, result);
    	if (projCurrent == null) {
    		return;
    	}

		PrismObject<F> focus;
    	if (context.getFocusContext().getObjectCurrent() != null) {
    		focus = context.getFocusContext().getObjectCurrent();
    	} else {
    		focus = context.getFocusContext().getObjectNew();
    	}

        for (MappingType inboundMappingType : inboundMappingTypes) {

            ItemDelta focusItemDelta = null;

            PrismProperty<QName> oldAccountProperty = projCurrent.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
            LOGGER.trace("Processing inbound from account sync absolute state (currentAccount): {}", oldAccountProperty);
            collectMappingsForTargets(context, projContext, inboundMappingType, ShadowType.F_AUXILIARY_OBJECT_CLASS, oldAccountProperty, null, focus, null, task, result);
        }
	}

	private <F extends FocusType> PrismObject<ShadowType> loadFullShadowIfNeeded(LensProjectionContext projContext, PrismObject<ShadowType> projCurrent,
			LensContext<F> context, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
		if (!projContext.isFullShadow()) {
    		LOGGER.warn("Attempted to execute inbound expression on account shadow {} WITHOUT full account. Trying to load the account now.", projContext.getOid());      // todo change to trace level eventually
			projCurrent = loadProjection(context, projContext, task, result, projCurrent);
			if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
				return null;
			}
            if (!projContext.isFullShadow()) {
            	if (projContext.getResourceShadowDiscriminator().getOrder() > 0) {
            		// higher-order context. It is OK not to load this
            		LOGGER.trace("Skipped load of higher-order account with shadow OID {} skipping inbound processing on it", projContext.getOid());
            		return null;
            	}
				// TODO: is it good to mark as broken? what is
				// the resorce is down?? if there is no
				// assignment and the account was added directly
				// it can cause that the account will be
				// unlinked from the user FIXME
                LOGGER.warn("Couldn't load account with shadow OID {}, setting context as broken and skipping inbound processing on it", projContext.getOid());
                projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
                return null;
            }
        }
		return projCurrent;
	}

	private <F extends FocusType> PrismObject<ShadowType> loadProjection(LensContext<F> context,
			LensProjectionContext projContext, Task task, OperationResult result, PrismObject<ShadowType> accountCurrent)
			throws SchemaException {
		try {
			contextLoader.loadFullShadow(context, projContext, "inbound", task, result);
			accountCurrent = projContext.getObjectCurrent();
		} catch (ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
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
	
	private <F extends FocusType, V extends PrismValue, D extends ItemDefinition> void collectMappingsForTargets(final LensContext<F> context,
    		LensProjectionContext projectionCtx, MappingType inboundMappingType,
    		QName accountAttributeName, Item<V,D> oldAccountProperty, ItemDelta<V,D> attributeAPrioriDelta,
            PrismObject<F> focusNew, 
            VariableProducer<V> variableProducer, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, ConfigurationException, SecurityViolationException, CommunicationException {

    	if (oldAccountProperty != null && oldAccountProperty.hasRaw()) {
        	throw new SystemException("Property "+oldAccountProperty+" has raw parsing state, such property cannot be used in inbound expressions");
        }

    	ResourceType resource = projectionCtx.getResource();
    	Mapping.Builder<V,D> builder = mappingFactory.createMappingBuilder(inboundMappingType,
    			"inbound expression for "+accountAttributeName+" in "+resource);

    	if (!builder.isApplicableToChannel(context.getChannel())) {
    		return;
    	}
    	
    	PrismObject<ShadowType> account = projectionCtx.getObjectNew();
    	ExpressionVariables variables = new ExpressionVariables();
    	variables.addVariableDefinition(ExpressionConstants.VAR_USER, focusNew);
    	variables.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusNew);
    	variables.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, account);
    	variables.addVariableDefinition(ExpressionConstants.VAR_SHADOW, account);
    	variables.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource);
    	variables.addVariableDefinition(ExpressionConstants.VAR_OPERATION, context.getFocusContext().getOperation().getValue());
    	
    	Collection<V> originalValues = null;
    	if (!context.getFocusContext().isDelete()) {
    		
    		originalValues = ExpressionUtil.computeTargetValues(inboundMappingType.getTarget(), focusNew, variables, mappingFactory.getObjectResolver() , "resolving range", task, result);
    	}
    	Source<V,D> defaultSource = new Source<>(oldAccountProperty, attributeAPrioriDelta, null, ExpressionConstants.VAR_INPUT);
    	defaultSource.recompute();
		builder = builder.defaultSource(defaultSource)
				.targetContext(LensUtil.getFocusDefinition(context))
				.variables(variables)
				.variableResolver(variableProducer)
				.valuePolicyResolver(createStringPolicyResolver(context, task, result))
				.originType(OriginType.INBOUND)
				.originObject(resource);
		
		if (!context.getFocusContext().isDelete()){
			builder.originalTargetValues(originalValues);
		}
	
		Mapping<V, D> mapping = builder.build();
		
    	if (checkWeakSkip(mapping, focusNew)) {
            LOGGER.trace("Skipping because of mapping is weak and focus property has already a value");
            return;
        }

        ItemPath targetFocusItemPath = mapping.getOutputPath();
        if (ItemPath.isNullOrEmpty(targetFocusItemPath)) {
        	throw new ConfigurationException("Empty target path in "+mapping.getContextDescription());
        }
        boolean isAssignment = new ItemPath(FocusType.F_ASSIGNMENT).equivalent(targetFocusItemPath);
        Item targetFocusItem = null;
        if (focusNew != null) {
        	targetFocusItem = focusNew.findItem(targetFocusItemPath);
        }
        PrismObjectDefinition<F> focusDefinition = context.getFocusContext().getObjectDefinition();
        ItemDefinition targetItemDef = focusDefinition.findItemDefinition(targetFocusItemPath);
        if (targetItemDef == null) {
        	throw new SchemaException("No definition for focus property "+targetFocusItemPath+", cannot process inbound expression in "+resource);
        }
        
        List<Mapping<V, D>> existingMapping = (List) mappingsToTarget.get(targetItemDef);
        if (CollectionUtils.isEmpty(existingMapping)) {
        	mappingsToTarget.put(targetItemDef, Arrays.asList(mapping));
        } else {
        	List<Mapping<V, D>> clone = new ArrayList<>(existingMapping);
        	clone.add(mapping);
        	mappingsToTarget.replace(targetItemDef, (List) clone);
        }
	}

    private <F extends FocusType, V extends PrismValue,D extends ItemDefinition> Collection<ItemDelta<V,D>> evaluateInboundMapping(final LensContext<F> context,
    		LensProjectionContext projectionCtx, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, ConfigurationException, SecurityViolationException, CommunicationException {

    	PrismObject<F> focusNew = context.getFocusContext().getObjectCurrent();
    	if (focusNew == null) {
    		focusNew = context.getFocusContext().getObjectNew();
    	}
    	
    	Collection<ItemDelta<V, D>> outputDeltas = new ArrayList<>();
        
    	Set<Entry<D, List<Mapping<V, D>>>> mappingsToTargeSet = (Set) mappingsToTarget.entrySet();
		for (Entry<D, List<Mapping<V, D>>> mappingEntry : mappingsToTargeSet) {
			checkTolerant(mappingEntry.getValue());
			
			List<Mapping<V, D>> mappings = mappingEntry.getValue();
			Iterator<Mapping<V, D>> mappingIterator = mappings.iterator();
			DeltaSetTriple<ItemValueWithOrigin<V, D>> allTriples = new DeltaSetTriple<ItemValueWithOrigin<V, D>>();
			while (mappingIterator.hasNext()) {
				Mapping<V, D> mapping = mappingIterator.next();
				mappingEvaluator.evaluateMapping(mapping, context, projectionCtx, task, result);
				
				DeltaSetTriple<ItemValueWithOrigin<V, D>> itemValueWithOrigin = ItemValueWithOrigin.createOutputTriple(mapping);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Inbound mapping for {} returned triple:\n{}", mapping.getDefaultSource().debugDump(),
							itemValueWithOrigin == null ? "null" : itemValueWithOrigin.debugDump());
				}
				if (itemValueWithOrigin != null) {
					
					allTriples.addAllToMinusSet(itemValueWithOrigin.getMinusSet());
					allTriples.addAllToPlusSet(itemValueWithOrigin.getPlusSet());
					allTriples.addAllToZeroSet(itemValueWithOrigin.getZeroSet());
				}
				
			}
			
			DeltaSetTriple<ItemValueWithOrigin<V, D>> consolidatedTriples = consolidateTriples(allTriples);
			
			LOGGER.trace("Consolidated triples {} \nfor mapping for item {}", consolidatedTriples.debugDump(), mappingEntry.getKey());
			
			Mapping<V, D> firstMapping = mappingEntry.getValue().iterator().next();
			outputDeltas.add(
					collectOutputDelta(mappingEntry.getKey(), firstMapping.getOutputPath(), focusNew,
							consolidatedTriples,
					firstMapping.isTolerant() == Boolean.TRUE ? true : false,	
					hasRange(mappingEntry.getValue())));	
		}
    	
    	
        // if no changes were generated return null
        return outputDeltas.isEmpty() ? null : outputDeltas;
    }
    
    private <V extends PrismValue, D extends ItemDefinition> DeltaSetTriple<ItemValueWithOrigin<V, D>> consolidateTriples(DeltaSetTriple<ItemValueWithOrigin<V, D>> originTriples) {
    	// Meaning of the resulting triple:
		// values in PLUS set will be added (valuesToAdd in delta)
		// values in MINUS set will be removed (valuesToDelete in delta)
		// values in ZERO set will be compared with existing values in
		// user property
		// the differences will be added to delta

    	Collection<ItemValueWithOrigin<V, D>> consolidatedZeroSet = new HashSet<>();
    	Collection<ItemValueWithOrigin<V, D>> consolidatedPlusSet = new HashSet<>();
    	Collection<ItemValueWithOrigin<V, D>> consolidatedMinusSet = new HashSet<>();
    	
    	if (originTriples != null) {
	    	if (originTriples.hasPlusSet()) {
				Collection<ItemValueWithOrigin<V, D>> plusSet = originTriples.getPlusSet();
				LOGGER.trace("Consolidating plusSet from origin:\n {}", plusSet);
				
				
				for (ItemValueWithOrigin<V, D> plusValue : plusSet) {
					boolean consolidated = false;
					if (originTriples.hasMinusSet()) {
						for (ItemValueWithOrigin<V, D> minusValue: originTriples.getMinusSet()) {
							if (minusValue.getItemValue().equalsRealValue(plusValue.getItemValue())) {
								LOGGER.trace(
										"Removing value {} from minus set -> moved to the zero, becuase the same value present in plus and minus set at the same time",
										minusValue);
								consolidatedMinusSet.remove(minusValue);
								consolidatedPlusSet.remove(plusValue);
								consolidatedZeroSet.add(minusValue);
								consolidated = true;
							}
						}
					}
					
					if (originTriples.hasZeroSet()) {
						for (ItemValueWithOrigin<V, D> zeroValue: originTriples.getZeroSet()) {
							if (zeroValue.getItemValue().equalsRealValue(plusValue.getItemValue())) {
								LOGGER.trace(
										"Removing value {} from plus set -> moved to the zero, becuase the same value present in plus and minus set at the same time",
										zeroValue);
								consolidatedPlusSet.remove(plusValue);
								consolidated = true;
							}
						}
					}
					if (!consolidated) { //&& !PrismValue.containsRealValue(consolidatedPlusSet, plusValue)) {
						consolidatedPlusSet.add(plusValue);
					}
				}
				
								
	
			}
	    	
		
			
			if (originTriples.hasZeroSet()) {
				Collection<ItemValueWithOrigin<V, D>> zeroSet = originTriples.getZeroSet();
				LOGGER.trace("Consolidating zero set from origin:\n {}", zeroSet);
				boolean consolidated = false;
				for (ItemValueWithOrigin<V, D> zeroValue : zeroSet) {
					if (originTriples.hasMinusSet()) {
						for (ItemValueWithOrigin<V, D> minusValue : originTriples.getMinusSet()) {
							if (minusValue.getItemValue().equalsRealValue(zeroValue.getItemValue())) {
								LOGGER.trace(
										"Removing value {} from minus set -> moved to the zero, becuase the same value present in zero and minus set at the same time",
										minusValue);
								consolidatedMinusSet.remove(minusValue);
								consolidatedZeroSet.add(minusValue);
								consolidated = true;
							}
						}

					}
					
					if (originTriples.hasPlusSet()) {
//						Iterator<V> plusValuesIterator = originTriples.getPlusSet().iterator();
						for (ItemValueWithOrigin<V, D> plusValue : originTriples.getPlusSet()) {
//							V minusValue = plusValuesIterator.next();
							if (plusValue.getItemValue().equalsRealValue(zeroValue.getItemValue())) {
								LOGGER.trace(
										"Removing value {} from plus set -> moved to the zero, becuase the same value present in zero and plus set at the same time",
										plusValue);
								consolidatedPlusSet.remove(plusValue);
								consolidatedZeroSet.add(plusValue);
								consolidated = true;
							}
						}

					}
					if (!consolidated) {// && !PrismValue.containsRealValue(consolidatedZeroSet, zeroValue)) {
						consolidatedZeroSet.add(zeroValue);
					}
				}
				
				
			}
			
			
		
			if (originTriples.hasMinusSet()) {
				Collection<ItemValueWithOrigin<V, D>> minusSet = originTriples.getMinusSet();
				LOGGER.trace("Consolidating minus set from origin:\n {}", minusSet);
				boolean consolidated = false;
				for (ItemValueWithOrigin<V, D> minusValue : minusSet){
					
					if (originTriples.hasPlusSet()) {
						for (ItemValueWithOrigin<V, D> plusValue : originTriples.getPlusSet()) {
							if (plusValue.getItemValue().equalsRealValue(minusValue.getItemValue())) {
								LOGGER.trace(
										"Removing value {} from plus set -> moved to the zero, becuase the same value present in plus and minus set at the same time",
										plusValue);
								consolidatedPlusSet.remove(plusValue);
								consolidatedMinusSet.remove(minusValue);
								consolidatedZeroSet.add(minusValue);
								consolidated = true;
							}
						}
					}
					
					
					if (originTriples.hasZeroSet()) {
						for (ItemValueWithOrigin<V, D> zeroValue : originTriples.getZeroSet()) {
							if (zeroValue.getItemValue().equalsRealValue(minusValue.getItemValue())) {
								LOGGER.trace(
										"Removing value {} from minus set -> moved to the zero, becuase the same value present in plus and minus set at the same time",
										zeroValue);
								consolidatedMinusSet.remove(minusValue);
								consolidatedZeroSet.add(zeroValue);
								consolidated = true;
							}
						}
					}
					
					if (!consolidated) { // && !PrismValue.containsRealValue(consolidatedMinusSet, minusValue)) {
						consolidatedMinusSet.add(minusValue);
					}
					
				}

			}
		}
		
		DeltaSetTriple<ItemValueWithOrigin<V, D>> consolidatedTriples = new DeltaSetTriple<>();
		consolidatedTriples.addAllToMinusSet(consolidatedMinusSet);
		consolidatedTriples.addAllToPlusSet(consolidatedPlusSet);
		consolidatedTriples.addAllToZeroSet(consolidatedZeroSet);
		return consolidatedTriples;
    }
    
    
    private <V extends PrismValue, D extends ItemDefinition, F extends FocusType> ItemDelta<V, D> collectOutputDelta(ItemDefinition outputDefinition, ItemPath outputPath, PrismObject<F> focusNew, DeltaSetTriple<ItemValueWithOrigin<V, D>> consolidatedTriples, boolean tolerant, boolean hasRange) throws SchemaException {
		
//    	ItemPath outputPath = inboundMappingType.getOutputPath();
		ItemDelta outputFocusItemDelta = outputDefinition.createEmptyDelta(outputPath);
		Item targetFocusItem = null;
		if (focusNew != null) {
			targetFocusItem = focusNew.findItem(outputPath);
		}
//
		boolean isAssignment = new ItemPath(FocusType.F_ASSIGNMENT).equivalent(outputPath);
		
    	    	
		Item shouldBeItem = outputDefinition.instantiate();
		if (consolidatedTriples != null) {
			
			Collection<ItemValueWithOrigin<V, D>> shouldBeItemValues = consolidatedTriples.getNonNegativeValues();
			for (ItemValueWithOrigin<V, D> itemWithOrigin : shouldBeItemValues) {
				shouldBeItem.add(LensUtil.cloneAndApplyMetadata(itemWithOrigin.getItemValue(),
						isAssignment,
						shouldBeItemValues));
			}
			
			
			
//			shouldBeItem.addAll(LensUtil.cloneAndApplyMetadata(consolidatedTriples.getZeroSet(), isAssignment, inboundMappingType.getMappingType()));
//			shouldBeItem.addAll(LensUtil.cloneAndApplyMetadata(consolidatedTriples.getPlusSet(), isAssignment, inboundMappingType.getMappingType()));
			
			if (consolidatedTriples.hasPlusSet()) {

				boolean alreadyReplaced = false;
				for (ItemValueWithOrigin<V, D> valueWithOrigin : consolidatedTriples.getPlusSet()) {
					Mapping<V,D> originMapping = (Mapping) valueWithOrigin.getMapping();
					if (targetFocusItem == null) {
						targetFocusItem = focusNew.findItem(originMapping.getOutputPath());
					}
					V value = valueWithOrigin.getItemValue();
					if (targetFocusItem != null && targetFocusItem.hasRealValue(value)) {
						continue;
					}

					if (outputFocusItemDelta == null) {
						outputFocusItemDelta = outputDefinition.createEmptyDelta(originMapping.getOutputPath());
					}

					// if property is not multi value replace existing
					// attribute
					if (targetFocusItem != null && !targetFocusItem.getDefinition().isMultiValue()
							&& !targetFocusItem.isEmpty()) {
						Collection<V> replace = new ArrayList<V>();
						replace.add(LensUtil.cloneAndApplyMetadata(value, isAssignment, originMapping.getMappingType()));
						outputFocusItemDelta.setValuesToReplace(replace);

					
	                	if (alreadyReplaced) {
							LOGGER.warn("Multiple values for a single-valued property {}; duplicate value = {}", targetFocusItem,
									value);
						} else {
							alreadyReplaced = true;
						}
					} else {
						outputFocusItemDelta.addValueToAdd(LensUtil.cloneAndApplyMetadata(value, isAssignment, originMapping.getMappingType()));
					}
				}
			}

			if (consolidatedTriples.hasMinusSet()) {
				LOGGER.trace("Checking account sync property delta values to delete");
				for (ItemValueWithOrigin<V, D> valueWithOrigin : consolidatedTriples.getMinusSet()) {
					V value = valueWithOrigin.getItemValue();

					if (targetFocusItem == null || targetFocusItem.hasRealValue(value)) {
						if (!outputFocusItemDelta.isReplace()) {
							// This is not needed if we are going to
							// replace. In fact it might cause an error.
							outputFocusItemDelta.addValueToDelete(value);
						}
					}
				}
			}

		} else {
			// triple == null
			// the mapping is not applicable. Nothing to do.
		}		
		
		if (targetFocusItem != null) {
			ItemDelta diffDelta = targetFocusItem.diff(shouldBeItem);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Comparing focus item:\n{}\nto should be item:\n{}\ndiff:\n{} ",
						DebugUtil.debugDump(targetFocusItem, 1), DebugUtil.debugDump(shouldBeItem, 1),
						DebugUtil.debugDump(diffDelta, 1));
			}
			
			if (hasRange) {
				LOGGER.trace("Skipping merge for diff delta because mapping contains range. All plus/minus/zero set were computed during renge checking");
				LOGGER.trace("Returining delta: {}", outputFocusItemDelta.debugDump());
				return outputFocusItemDelta;
			}
			
			if (diffDelta != null) {
				// this is probably not correct, as the default for
				// inbounds should be TRUE
				if (tolerant) {
					if (diffDelta.isReplace()) {
						if (diffDelta.getValuesToReplace().isEmpty()) {
							diffDelta.resetValuesToReplace();
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Removing empty replace part of the diff delta because mapping is tolerant:\n{}",
										diffDelta.debugDump());
							}
						} else {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace(
										"Making sure that the replace part of the diff contains old values delta because mapping is tolerant:\n{}",
										diffDelta.debugDump());
							}
							for (Object shouldBeValueObj : shouldBeItem.getValues()) {
								PrismValue shouldBeValue = (PrismValue) shouldBeValueObj;
								if (!PrismValue.containsRealValue(diffDelta.getValuesToReplace(), shouldBeValue)) {
									diffDelta.addValueToReplace(shouldBeValue.clone());
								}
							}
						}	
					} else {
						diffDelta.resetValuesToDelete();
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Removing delete part of the diff delta because mapping is tolerant:\n{}",
									diffDelta.debugDump());
						}
					}
				}

				// if (!hasRange(mappingEntry.getValue())) {
				diffDelta.setElementName(ItemPath.getName(outputPath.last()));
				diffDelta.setParentPath(outputPath.allExceptLast());
				outputFocusItemDelta.merge(diffDelta);
				// }
			}

		} else {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Adding user property because inbound say so (account doesn't contain that value):\n{}",
						shouldBeItem.getValues());
			}
			// if user property doesn't exist we have to add it (as
			// delta), because inbound say so
			outputFocusItemDelta.addValuesToAdd(shouldBeItem.getClonedValues());
		}
		
		
		return outputFocusItemDelta;
    }
    
    private <V extends PrismValue, D extends ItemDefinition> boolean hasRange(List<Mapping<V, D>> mappings) {
    	for (Mapping<V, D> mapping : mappings) {
			if (mapping.hasTargetRange()) {
				return true;
			}
		}
    	return false;
    }
    
    private <V extends PrismValue, D extends ItemDefinition> void checkTolerant(List<Mapping<V, D>> mappings)  throws SchemaException {
    	int tolerantCount = 0;
    	int intolerantCount = 0;
    	for (Mapping<V, D> mapping : mappings) {
    		if (mapping.isTolerant() == Boolean.TRUE) {
    			tolerantCount++;
    		}
    		if (mapping.isTolerant() == null || mapping.isTolerant() == Boolean.FALSE) {
    			intolerantCount++;
    		}
    	}
    	
    	if (tolerantCount > 0 && intolerantCount > 0) {
    		throw new SchemaException("Incorrect configuration. There cannot be different 'tolernt' settings for the target item " + mappings.iterator().next().getOutputDefinition());
    	}
    	
    }

	private void resolveEntitlementsIfNeeded(ContainerDelta<ShadowAssociationType> attributeAPrioriDelta, PrismContainer<ShadowAssociationType> oldAccountProperty, LensProjectionContext projCtx, Task task, OperationResult result) {
		Collection<PrismContainerValue<ShadowAssociationType>> shadowAssociations = null;
		if (oldAccountProperty != null) {
			shadowAssociations = oldAccountProperty.getValues();
		} else if (attributeAPrioriDelta != null) {
			shadowAssociations = attributeAPrioriDelta.getValues(ShadowAssociationType.class);
		}
		
		if (shadowAssociations == null) {
			LOGGER.trace("No shadow associations found");
			return;
		}

		for (PrismContainerValue<ShadowAssociationType> value : shadowAssociations) {
			PrismReference shadowRef = value.findReference(ShadowAssociationType.F_SHADOW_REF);
			if (shadowRef == null) {
				continue;
			}

			if (projCtx.getEntitlementMap().containsKey(shadowRef.getOid())) {
				shadowRef.getValue().setObject(projCtx.getEntitlementMap().get(shadowRef.getOid()));
			} else {
				try {
					PrismObject<ShadowType> entitlement = provisioningService.getObject(ShadowType.class, shadowRef.getOid(),
							null, task, result);
					projCtx.getEntitlementMap().put(entitlement.getOid(), entitlement);
				} catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException
						| SecurityViolationException | ExpressionEvaluationException e) {
					LOGGER.error("failed to load entitlement.");
					// TODO: can we just ignore and continue?
					continue;
				}
			}
		}

	}
	
	private <F extends ObjectType> ValuePolicyResolver createStringPolicyResolver(final LensContext<F> context, final Task task, final OperationResult result) {
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
				if (!outputDefinition.getName().equals(PasswordType.F_VALUE)) {
					return null;
				}
				ValuePolicyType passwordPolicy = credentialsProcessor.determinePasswordPolicy(context.getFocusContext(), task, result);
				if (passwordPolicy == null) {
					return null;
				}
				return passwordPolicy;
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
     */
    private <F extends FocusType> void processSpecialPropertyInbound(ResourceBidirectionalMappingType biMappingType, ItemPath sourceTargetPath,
            PrismObject<F> newUser, LensProjectionContext projCtx,
            RefinedObjectClassDefinition rOcDef, LensContext<F> context,
            XMLGregorianCalendar now, Task task, OperationResult opResult) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	if (biMappingType == null) {
    		return;
    	}
    	processSpecialPropertyInbound(biMappingType.getInbound(), sourceTargetPath, sourceTargetPath, newUser, projCtx, rOcDef, context, now, task, opResult);
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
    private <F extends FocusType> void processSpecialPropertyInbound(Collection<MappingType> inboundMappingTypes,
    		final ItemPath sourcePath, final ItemPath targetPath,
            final PrismObject<F> newUser, final LensProjectionContext projContext,
            RefinedObjectClassDefinition projectionDefinition, final LensContext<F> context,
            XMLGregorianCalendar now, Task task, OperationResult opResult) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        if (inboundMappingTypes == null || inboundMappingTypes.isEmpty() || newUser == null || !projContext.isFullShadow()) {
            return;
        }

        ObjectDelta<F> userPrimaryDelta = context.getFocusContext().getPrimaryDelta();
        PropertyDelta primaryPropDelta = null;
        if (userPrimaryDelta != null) {
        	primaryPropDelta = userPrimaryDelta.findPropertyDelta(targetPath);
        	if (primaryPropDelta != null && primaryPropDelta.isReplace()) {
				// Replace primary delta overrides any inbound
        		return;
        	}
        }

      ObjectDelta<F> userSecondaryDelta = context.getFocusContext().getProjectionWaveSecondaryDelta();
      if (userSecondaryDelta != null) {
	        PropertyDelta<?> delta = userSecondaryDelta.findPropertyDelta(targetPath);
	        if (delta != null) {
	            //remove delta if exists, it will be handled by inbound
	            userSecondaryDelta.getModifications().remove(delta);
	        }
      }

        MappingInitializer initializer =
			(builder) -> {
				if (projContext.getObjectNew() == null) {
					projContext.recompute();
					if (projContext.getObjectNew() == null) {
						// Still null? something must be really wrong here.
						String message = "Recomputing account " + projContext.getResourceShadowDiscriminator()
								+ " results in null new account. Something must be really broken.";
						LOGGER.error(message);
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Account context:\n{}", projContext.debugDump());
						}
						throw new SystemException(message);
					}
				}

				ObjectDelta<ShadowType> aPrioriShadowDelta = getAPrioriDelta(context, projContext);
				ItemDelta<PrismPropertyValue<?>,PrismPropertyDefinition<?>> specialAttributeDelta = null;
				if (aPrioriShadowDelta != null){
					specialAttributeDelta = aPrioriShadowDelta.findItemDelta(sourcePath);
				}
				ItemDeltaItem<PrismPropertyValue<?>,PrismPropertyDefinition<?>> sourceIdi = projContext.getObjectDeltaObject().findIdi(sourcePath);
				if (specialAttributeDelta == null){
					specialAttributeDelta = sourceIdi.getDelta();
				}
				Source<PrismPropertyValue<?>,PrismPropertyDefinition<?>> source = new Source<>(sourceIdi.getItemOld(), specialAttributeDelta,
						sourceIdi.getItemOld(), ExpressionConstants.VAR_INPUT);
				builder = builder.defaultSource(source)
						.addVariableDefinition(ExpressionConstants.VAR_USER, newUser)
						.addVariableDefinition(ExpressionConstants.VAR_FOCUS, newUser);

				PrismObject<ShadowType> accountNew = projContext.getObjectNew();
				builder = builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, accountNew)
						.addVariableDefinition(ExpressionConstants.VAR_SHADOW, accountNew)
						.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projContext.getResource())
						.valuePolicyResolver(createStringPolicyResolver(context, task, opResult))
						.originType(OriginType.INBOUND)
						.originObject(projContext.getResource());

				return builder;
			};

        MappingOutputProcessor<PrismValue> processor =
			(mappingOutputPath, outputStruct) -> {
				PrismValueDeltaSetTriple<PrismValue> outputTriple = outputStruct.getOutputTriple();
		        if (outputTriple == null){
		        	LOGGER.trace("Mapping for property {} evaluated to null. Skipping inboud processing for that property.", sourcePath);
		        	return false;
		        }

		        ObjectDelta<F> userSecondaryDeltaInt = context.getFocusContext().getProjectionWaveSecondaryDelta();
		        if (userSecondaryDeltaInt != null) {
			        PropertyDelta<?> delta = userSecondaryDeltaInt.findPropertyDelta(targetPath);
			        if (delta != null) {
			            //remove delta if exists, it will be handled by inbound
			            userSecondaryDeltaInt.getModifications().remove(delta);
			        }
		        }

		        PrismObjectDefinition<F> focusDefinition = context.getFocusContext().getObjectDefinition();
		        PrismProperty result = focusDefinition.findPropertyDefinition(targetPath).instantiate();
		    	result.addAll(PrismValue.cloneCollection(outputTriple.getNonNegativeValues()));

		    	PrismProperty targetPropertyNew = newUser.findOrCreateProperty(targetPath);
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
		        	delta.setParentPath(targetPath.allExceptLast());
		        	if (!context.getFocusContext().alreadyHasDelta(delta)){
		        		context.getFocusContext().swallowToProjectionWaveSecondaryDelta(delta);
		        	}
		        }
		        return false;
			};

        MappingEvaluatorParams<PrismValue, ItemDefinition, F, F> params = new MappingEvaluatorParams<>();
        params.setMappingTypes(inboundMappingTypes);
        params.setMappingDesc("inbound mapping for " + sourcePath + " in " + projContext.getResource());
        params.setNow(now);
        params.setInitializer(initializer);
		params.setProcessor(processor);
        params.setAPrioriTargetObject(newUser);
        params.setAPrioriTargetDelta(userPrimaryDelta);
        params.setTargetContext(context.getFocusContext());
        params.setDefaultTargetItemPath(targetPath);
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
