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

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.SynchronizationIntent;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectLifecycleDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationValidityCapabilityType;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    private static final QName ASSIGNED_PROPERTY_NAME = new QName(SchemaConstants.NS_C, "assigned");
	private static final QName FOCUS_EXISTS_PROPERTY_NAME = new QName(SchemaConstants.NS_C, "focusExists");

	@Autowired private ContextLoader contextLoader;
    @Autowired private PrismContext prismContext;
    @Autowired private MappingEvaluator mappingEvaluator;
    @Autowired private MidpointFunctions midpointFunctions;
	
	private PrismObjectDefinition<UserType> userDefinition;
	private PrismContainerDefinition<ActivationType> activationDefinition;

    public <O extends ObjectType, F extends FocusType> void processActivation(LensContext<O> context,
    		LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

    	LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext != null && !FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for focal object.
    		return;
    	}

    	processActivationFocal((LensContext<F>)context, projectionContext, now, task, result);
    }

    private <F extends FocusType> void processActivationFocal(LensContext<F> context,
    		LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		processActivationMetadata(context, projectionContext, now, result);
    		return;
    	}
    	try {
    		
	    	processActivationUserCurrent(context, projectionContext, now, task, result);
	    	processActivationMetadata(context, projectionContext, now, result);
	    	processActivationUserFuture(context, projectionContext, now, task, result);
	    	
    	} catch (ObjectNotFoundException e) {
    		if (projectionContext.isThombstone()) {
    			// This is not critical. The projection is marked as thombstone and we can go on with processing
    			// No extra action is needed.
    		} else {
    			throw e;
    		}
    	}
    }

    public <F extends FocusType> void processActivationUserCurrent(LensContext<F> context, LensProjectionContext projCtx,
    		XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

    	String projCtxDesc = projCtx.toHumanReadableString();
    	SynchronizationPolicyDecision decision = projCtx.getSynchronizationPolicyDecision();
    	SynchronizationIntent synchronizationIntent = projCtx.getSynchronizationIntent();

    	if (decision == SynchronizationPolicyDecision.BROKEN) {
    		LOGGER.trace("Broken projection {}, skipping further activation processing", projCtxDesc);
    		return;
    	}
    	if (decision != null) {
    		throw new IllegalStateException("Decision "+decision+" already present for projection "+projCtxDesc);
    	}

    	if (synchronizationIntent == SynchronizationIntent.UNLINK) {
    		projCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.UNLINK);
    		LOGGER.trace("Evaluated decision for {} to {} because of unlink synchronization intent, skipping further activation processing", projCtxDesc, SynchronizationPolicyDecision.UNLINK);
    		return;
    	}

    	if (projCtx.isThombstone()) {
    		if (shouldKeepThombstone(projCtx)) {
	    		// Let's keep thombstones linked until they expire. So we do not have shadows without owners.
	    		// This is also needed for async delete operations.
	    		projCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.KEEP);
	    		LOGGER.trace("Evaluated decision for {} to {} because it is thombstone, skipping further activation processing", projCtxDesc, SynchronizationPolicyDecision.KEEP);
    		} else {
    			projCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.UNLINK);
    			LOGGER.trace("Evaluated decision for {} to {} because it is thombstone, skipping further activation processing", projCtxDesc, SynchronizationPolicyDecision.UNLINK);
    		}
    		return;
    	}

    	if (synchronizationIntent == SynchronizationIntent.DELETE || projCtx.isDelete()) {
    		// TODO: is this OK?
    		projCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.DELETE);
    		LOGGER.trace("Evaluated decision for {} to {}, skipping further activation processing", projCtxDesc, SynchronizationPolicyDecision.DELETE);
    		return;
    	}
    	
    	boolean shadowShouldExist = evaluateExistenceMapping(context, projCtx, now, true, task, result);

    	LOGGER.trace("Evaluated intended existence of projection {} to {}", projCtxDesc, shadowShouldExist);

    	// Let's reconcile the existence intent (shadowShouldExist) and the synchronization intent in the context

    	LensProjectionContext lowerOrderContext = LensUtil.findLowerOrderContext(context, projCtx);

    	if (synchronizationIntent == null || synchronizationIntent == SynchronizationIntent.SYNCHRONIZE) {
	    	if (shadowShouldExist) {
	    		projCtx.setActive(true);
	    		if (projCtx.isExists()) {
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
	    		if (projCtx.isExists()) {
	    			decision = SynchronizationPolicyDecision.DELETE;
	    		} else {
	    			// we should delete the entire context, but then we will lost track of what
	    			// happened. So just ignore it.
	    			decision = SynchronizationPolicyDecision.IGNORE;
	    			// if there are any triggers then move them to focus. We may still need them.
	    			LensUtil.moveTriggers(projCtx, context.getFocusContext());
	    		}
	    	}
	
    	} else if (synchronizationIntent == SynchronizationIntent.ADD) {
    		if (shadowShouldExist) {
    			projCtx.setActive(true);
    			if (projCtx.isExists()) {
    				// Attempt to add something that is already there, but should be OK
	    			decision = SynchronizationPolicyDecision.KEEP;
	    		} else {
	    			decision = SynchronizationPolicyDecision.ADD;
	    		}
    		} else {
    			throw new PolicyViolationException("Request to add projection "+projCtxDesc+" but the activation policy decided that it should not exist");
    		}

    	} else if (synchronizationIntent == SynchronizationIntent.KEEP) {
	    	if (shadowShouldExist) {
	    		projCtx.setActive(true);
	    		if (projCtx.isExists()) {
	    			decision = SynchronizationPolicyDecision.KEEP;
	    		} else {
	    			decision = SynchronizationPolicyDecision.ADD;
	    		}
	    	} else {
	    		throw new PolicyViolationException("Request to keep projection "+projCtxDesc+" but the activation policy decided that it should not exist");
	    	}

    	} else {
    		throw new IllegalStateException("Unknown sync intent "+synchronizationIntent);
    	}

    	LOGGER.trace("Evaluated decision for projection {} to {}", projCtxDesc, decision);

    	projCtx.setSynchronizationPolicyDecision(decision);

        PrismObject<F> focusNew = context.getFocusContext().getObjectNew();
        if (focusNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("focusNew is null, skipping activation processing of {}", projCtxDesc);
            return;
        }

    	if (decision == SynchronizationPolicyDecision.UNLINK || decision == SynchronizationPolicyDecision.DELETE) {
    		LOGGER.trace("Decision is {}, skipping activation properties processing for {}", decision, projCtxDesc);
    		return;
    	}

    	ResourceObjectTypeDefinitionType resourceAccountDefType = projCtx.getResourceObjectTypeDefinitionType();
        if (resourceAccountDefType == null) {
            LOGGER.trace("No refined object definition, therefore also no activation outbound definition, skipping activation processing for account " + projCtxDesc);
            return;
        }
        ResourceActivationDefinitionType activationType = resourceAccountDefType.getActivation();
        if (activationType == null) {
            LOGGER.trace("No activation definition in projection {}, skipping activation properties processing", projCtxDesc);
            return;
        }

        ActivationCapabilityType capActivation = ResourceTypeUtil.getEffectiveCapability(projCtx.getResource(), ActivationCapabilityType.class);
        if (capActivation == null) {
        	LOGGER.trace("Skipping activation status and validity processing because {} has no activation capability", projCtx.getResource());
        	return;
        }

        ActivationStatusCapabilityType capStatus = CapabilityUtil.getEffectiveActivationStatus(capActivation);
        ActivationValidityCapabilityType capValidFrom = CapabilityUtil.getEffectiveActivationValidFrom(capActivation);
        ActivationValidityCapabilityType capValidTo = CapabilityUtil.getEffectiveActivationValidTo(capActivation);
        ActivationLockoutStatusCapabilityType capLockoutStatus = CapabilityUtil.getEffectiveActivationLockoutStatus(capActivation);

        if (capStatus != null) {
	    	evaluateActivationMapping(context, projCtx,
	    			activationType.getAdministrativeStatus(),
	    			SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
	    			capActivation, now, true, ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart(), task, result);
        } else {
        	LOGGER.trace("Skipping activation administrative status processing because {} does not have activation administrative status capability", projCtx.getResource());
        }

        ResourceBidirectionalMappingType validFromMappingType = activationType.getValidFrom();
        if (validFromMappingType == null || validFromMappingType.getOutbound() == null) {
        	LOGGER.trace("Skipping activation validFrom processing because {} does not have appropriate outbound mapping", projCtx.getResource());
        } else if (capValidFrom == null && !ExpressionUtil.hasExplicitTarget(validFromMappingType.getOutbound())) {
        	LOGGER.trace("Skipping activation validFrom processing because {} does not have activation validFrom capability nor outbound mapping with explicit target", projCtx.getResource());
        } else {
	    	evaluateActivationMapping(context, projCtx, activationType.getValidFrom(),
	    			SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
	    			null, now, true, ActivationType.F_VALID_FROM.getLocalPart(), task, result);
        }

        ResourceBidirectionalMappingType validToMappingType = activationType.getValidTo();
        if (validToMappingType == null || validToMappingType.getOutbound() == null) {
        	LOGGER.trace("Skipping activation validTo processing because {} does not have appropriate outbound mapping", projCtx.getResource());
        } else if (capValidTo == null && !ExpressionUtil.hasExplicitTarget(validToMappingType.getOutbound())) {
        	LOGGER.trace("Skipping activation validTo processing because {} does not have activation validTo capability nor outbound mapping with explicit target", projCtx.getResource());
        } else {
	    	evaluateActivationMapping(context, projCtx, activationType.getValidTo(),
	    			SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO,
	    			null, now, true, ActivationType.F_VALID_TO.getLocalPart(), task, result);
	    }

        if (capLockoutStatus != null) {
	    	evaluateActivationMapping(context, projCtx,
	    			activationType.getLockoutStatus(),
	    			SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
	    			capActivation, now, true, ActivationType.F_LOCKOUT_STATUS.getLocalPart(), task, result);
        } else {
        	LOGGER.trace("Skipping activation lockout status processing because {} does not have activation lockout status capability", projCtx.getResource());
        }

    }

    private boolean shouldKeepThombstone(LensProjectionContext projCtx) {
    	PrismObject<ShadowType> objectCurrent = projCtx.getObjectCurrent();
    	if (objectCurrent != null) {
    		ShadowType objectCurrentType = objectCurrent.asObjectable();
    		if (!objectCurrentType.getPendingOperation().isEmpty()) {
    			return true;
    		}
    	}
    	// TODO: thombstone expiration
    	return false;
	}

	public <F extends FocusType> void processActivationMetadata(LensContext<F> context, LensProjectionContext accCtx,
    		XMLGregorianCalendar now, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
    	ObjectDelta<ShadowType> projDelta = accCtx.getDelta();
    	if (projDelta == null) {
    		return;
    	}

    	PropertyDelta<ActivationStatusType> statusDelta = projDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);

    	if (statusDelta != null && !statusDelta.isDelete()) {

            // we have to determine if the status really changed
            PrismObject<ShadowType> oldShadow = accCtx.getObjectOld();
            ActivationStatusType statusOld = null;
            if (oldShadow != null && oldShadow.asObjectable().getActivation() != null) {
                statusOld = oldShadow.asObjectable().getActivation().getAdministrativeStatus();
            }

            PrismProperty<ActivationStatusType> statusPropNew = (PrismProperty<ActivationStatusType>) statusDelta.getItemNewMatchingPath(null);
            ActivationStatusType statusNew = statusPropNew.getRealValue();

            if (statusNew == statusOld) {
                LOGGER.trace("Administrative status not changed ({}), timestamp and/or reason will not be recorded", statusNew);
            } else {
                // timestamps
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
                        disableReasonDelta.setValueToReplace(new PrismPropertyValue<>(disableReason, OriginType.OUTBOUND, null));
                        accCtx.swallowToSecondaryDelta(disableReasonDelta);
                    }
                }
            }
    	}

    }

    public <F extends FocusType> void processActivationUserFuture(LensContext<F> context, LensProjectionContext accCtx,
    		XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
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

        ActivationStatusCapabilityType capStatus = CapabilityUtil.getEffectiveActivationStatus(capActivation);
        ActivationValidityCapabilityType capValidFrom = CapabilityUtil.getEffectiveActivationValidFrom(capActivation);
        ActivationValidityCapabilityType capValidTo = CapabilityUtil.getEffectiveActivationValidTo(capActivation);

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
    		final LensProjectionContext projCtx, final XMLGregorianCalendar now, final boolean current,
            Task task, final OperationResult result)
    				throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
    	final String projCtxDesc = projCtx.toHumanReadableString();

    	final Boolean legal = projCtx.isLegal();
    	if (legal == null) {
    		throw new IllegalStateException("Null 'legal' for "+projCtxDesc);
    	}

    	LOGGER.trace("Evaluating intended existence of projection {}; legal={}", projCtxDesc, legal);

    	ResourceObjectTypeDefinitionType resourceAccountDefType = projCtx.getResourceObjectTypeDefinitionType();
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

        MappingEvaluatorParams<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>, ShadowType, F> params = new MappingEvaluatorParams<>();
        params.setMappingTypes(outbound);
        params.setMappingDesc("outbound existence mapping in projection " + projCtxDesc);
        params.setNow(now);
        params.setAPrioriTargetObject(projCtx.getObjectOld());
        params.setEvaluateCurrent(current);
        params.setTargetContext(projCtx);
        params.setFixTarget(true);
        params.setContext(context);

        params.setInitializer(builder -> {
			// Source: legal
	        ItemDeltaItem<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> legalSourceIdi = getLegalIdi(projCtx);
	        Source<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> legalSource
	        	= new Source<>(legalSourceIdi, ExpressionConstants.VAR_LEGAL);
			builder.defaultSource(legalSource);

            // Source: assigned
            ItemDeltaItem<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> assignedIdi = getAssignedIdi(projCtx);
            Source<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> assignedSource = new Source<>(assignedIdi, ExpressionConstants.VAR_ASSIGNED);
			builder.addSource(assignedSource);

            // Source: focusExists
	        ItemDeltaItem<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> focusExistsSourceIdi = getFocusExistsIdi(context.getFocusContext());
	        Source<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> focusExistsSource
	        	= new Source<>(focusExistsSourceIdi, ExpressionConstants.VAR_FOCUS_EXISTS);
			builder.addSource(focusExistsSource);

			// Variable: focus
			builder.addVariableDefinition(ExpressionConstants.VAR_FOCUS, context.getFocusContext().getObjectDeltaObject());

	        // Variable: user (for convenience, same as "focus")
			builder.addVariableDefinition(ExpressionConstants.VAR_USER, context.getFocusContext().getObjectDeltaObject());

			// Variable: shadow
			builder.addVariableDefinition(ExpressionConstants.VAR_SHADOW, projCtx.getObjectDeltaObject());

			// Variable: resource
			builder.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projCtx.getResource());

			builder.originType(OriginType.OUTBOUND);
			builder.originObject(projCtx.getResource());
			return builder;
        });

        final MutableBoolean output = new MutableBoolean(false);
		params.setProcessor((mappingOutputPath,outputStruct) -> {
			PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = outputStruct.getOutputTriple();
			if (outputTriple == null) {
				// The "default existence mapping"
				output.setValue(legal);
				return false;
			}

			Collection<PrismPropertyValue<Boolean>> nonNegativeValues = outputTriple.getNonNegativeValues();

			// MID-3507: this is probably the bug. The processor is executed after every mapping.
			// The processing will die on the error if one mapping returns a value and the other mapping returns null
			// (e.g. because the condition is false). This should be fixed.
	        if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
	        	throw new ExpressionEvaluationException("Activation existence expression resulted in null or empty value for projection " + projCtxDesc);
	        }
	        if (nonNegativeValues.size() > 1) {
	        	throw new ExpressionEvaluationException("Activation existence expression resulted in too many values ("+nonNegativeValues.size()+") for projection " + projCtxDesc);
	        }
	
	        output.setValue(nonNegativeValues.iterator().next().getValue());

	        return false;
		});

        PrismPropertyDefinitionImpl<Boolean> shadowExistsDef = new PrismPropertyDefinitionImpl<>(
				SHADOW_EXISTS_PROPERTY_NAME,
				DOMUtil.XSD_BOOLEAN, prismContext);
        shadowExistsDef.setMinOccurs(1);
        shadowExistsDef.setMaxOccurs(1);
        params.setTargetItemDefinition(shadowExistsDef);
		mappingEvaluator.evaluateMappingSetProjection(params, task, result);

		return (boolean) output.getValue();

    }

    private <T, F extends FocusType> void evaluateActivationMapping(final LensContext<F> context,
			final LensProjectionContext projCtx, ResourceBidirectionalMappingType bidirectionalMappingType,
			final ItemPath focusPropertyPath, final ItemPath projectionPropertyPath,
   			final ActivationCapabilityType capActivation, XMLGregorianCalendar now, final boolean current,
   			String desc, final Task task, final OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

    	MappingInitializer<PrismPropertyValue<T>,PrismPropertyDefinition<T>> initializer =
			builder -> {
				// Source: administrativeStatus, validFrom or validTo
		        ItemDeltaItem<PrismPropertyValue<T>,PrismPropertyDefinition<T>> sourceIdi = context.getFocusContext().getObjectDeltaObject().findIdi(focusPropertyPath);

		        if (capActivation != null && focusPropertyPath.equals(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
			        ActivationValidityCapabilityType capValidFrom = CapabilityUtil.getEffectiveActivationValidFrom(capActivation);
			        ActivationValidityCapabilityType capValidTo = CapabilityUtil.getEffectiveActivationValidTo(capActivation);

			        // Source: computedShadowStatus
			        ItemDeltaItem<PrismPropertyValue<ActivationStatusType>,PrismPropertyDefinition<ActivationStatusType>> computedIdi;
			        if (capValidFrom != null && capValidTo != null) {
			        	// "Native" validFrom and validTo, directly use administrativeStatus
			        	computedIdi = context.getFocusContext().getObjectDeltaObject().findIdi(focusPropertyPath);
			
			        } else {
			        	// Simulate validFrom and validTo using effectiveStatus
			        	computedIdi = context.getFocusContext().getObjectDeltaObject().findIdi(SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS);
			
			        }

			        Source<PrismPropertyValue<ActivationStatusType>,PrismPropertyDefinition<ActivationStatusType>> computedSource = new Source<>(computedIdi, ExpressionConstants.VAR_INPUT);

			        builder.defaultSource(computedSource);

			        Source<PrismPropertyValue<T>,PrismPropertyDefinition<T>> source = new Source<>(sourceIdi, ExpressionConstants.VAR_ADMINISTRATIVE_STATUS);
					builder.addSource(source);

		        } else {
		        	Source<PrismPropertyValue<T>,PrismPropertyDefinition<T>> source = new Source<>(sourceIdi, ExpressionConstants.VAR_INPUT);
					builder.defaultSource(source);
		        }

				// Source: legal
		        ItemDeltaItem<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> legalIdi = getLegalIdi(projCtx);
		        Source<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> legalSource = new Source<>(legalIdi, ExpressionConstants.VAR_LEGAL);
				builder.addSource(legalSource);

                // Source: assigned
                ItemDeltaItem<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> assignedIdi = getAssignedIdi(projCtx);
                Source<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> assignedSource = new Source<>(assignedIdi, ExpressionConstants.VAR_ASSIGNED);
				builder.addSource(assignedSource);

                // Source: focusExists
		        ItemDeltaItem<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> focusExistsSourceIdi = getFocusExistsIdi(context.getFocusContext());
		        Source<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> focusExistsSource
		        	= new Source<>(focusExistsSourceIdi, ExpressionConstants.VAR_FOCUS_EXISTS);
		        builder.addSource(focusExistsSource);

				return builder;
			};

		evaluateOutboundMapping(context, projCtx, bidirectionalMappingType, focusPropertyPath, projectionPropertyPath, initializer,
				now, current, desc + " outbound activation mapping", task, result);

    }

	private <T, F extends FocusType> void evaluateOutboundMapping(final LensContext<F> context,
			final LensProjectionContext projCtx, ResourceBidirectionalMappingType bidirectionalMappingType,
			final ItemPath focusPropertyPath, final ItemPath projectionPropertyPath,
			final MappingInitializer<PrismPropertyValue<T>,PrismPropertyDefinition<T>> initializer,
			XMLGregorianCalendar now, final boolean evaluateCurrent, String desc, final Task task, final OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

		if (bidirectionalMappingType == null) {
            LOGGER.trace("No '{}' definition in projection {}, skipping", desc, projCtx.toHumanReadableString());
            return;
        }
        List<MappingType> outboundMappingTypes = bidirectionalMappingType.getOutbound();
        if (outboundMappingTypes == null || outboundMappingTypes.isEmpty()) {
            LOGGER.trace("No outbound definition in '{}' definition in projection {}, skipping", desc, projCtx.toHumanReadableString());
            return;
        }

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
		params.setMappingTypes(outboundMappingTypes);
		params.setMappingDesc(desc + " in projection " + projCtxDesc);
		params.setNow(now);
		params.setInitializer(internalInitializer);
		params.setTargetLoader(new ProjectionMappingLoader<>(context, projCtx, contextLoader));
		params.setAPrioriTargetObject(shadowNew);
		params.setAPrioriTargetDelta(LensUtil.findAPrioriDelta(context, projCtx));
		if (context.getFocusContext() != null) {
			params.setSourceContext(context.getFocusContext().getObjectDeltaObject());
		}
		params.setTargetContext(projCtx);
		params.setDefaultTargetItemPath(projectionPropertyPath);
		params.setEvaluateCurrent(evaluateCurrent);
		params.setEvaluateWeak(true);
		params.setContext(context);
		params.setHasFullTargetObject(projCtx.hasFullShadow());
		
		Map<ItemPath, MappingOutputStruct<PrismPropertyValue<T>>> outputTripleMap = mappingEvaluator.evaluateMappingSetProjection(params, task, result);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Mapping processing output after {}:\n{}", desc, DebugUtil.debugDump(outputTripleMap, 1));
		}
		
		if (projCtx.isDoReconciliation()) {
			reconcileOutboundValue(context, projCtx, outputTripleMap, desc);			
		}

	}

	/**
	 * TODO: can we align this with ReconciliationProcessor?
	 */
	private <T, F extends FocusType> void reconcileOutboundValue(LensContext<F> context, LensProjectionContext projCtx,
			Map<ItemPath, MappingOutputStruct<PrismPropertyValue<T>>> outputTripleMap, String desc) throws SchemaException {
		
		// TODO: check for full shadow?
		
		for (Entry<ItemPath,MappingOutputStruct<PrismPropertyValue<T>>> entry: outputTripleMap.entrySet()) {
			ItemPath mappingOutputPath = entry.getKey();
			MappingOutputStruct<PrismPropertyValue<T>> mappingOutputStruct = entry.getValue();
			if (mappingOutputStruct.isWeakMappingWasUsed()) {
				// Thing to do. All deltas should already be in context
				LOGGER.trace("Skip reconciliation of {} in {} because of weak", mappingOutputPath, desc);
				continue;
			}
			if (!mappingOutputStruct.isStrongMappingWasUsed()) {
				// Normal mappings are not processed for reconciliation
				LOGGER.trace("Skip reconciliation of {} in {} because not strong", mappingOutputPath, desc);
				continue;
			}
			LOGGER.trace("reconciliation of {} for {}", mappingOutputPath, desc);
			
			PrismObjectDefinition<ShadowType> targetObjectDefinition = projCtx.getObjectDefinition();
			PrismPropertyDefinition<T> targetItemDefinition = targetObjectDefinition.findPropertyDefinition(mappingOutputPath);
			if (targetItemDefinition == null) {
				throw new SchemaException("No definition for item "+mappingOutputPath+" in "+targetObjectDefinition);
			}
			PropertyDelta<T> targetItemDelta = targetItemDefinition.createEmptyDelta(mappingOutputPath);
			
			PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple = mappingOutputStruct.getOutputTriple();
			
			PrismProperty<T> currentTargetItem = null;
			PrismObject<ShadowType> shadowCurrent = projCtx.getObjectCurrent();
			if (shadowCurrent != null) {
				currentTargetItem = shadowCurrent.findProperty(mappingOutputPath);
			}
			Collection<PrismPropertyValue<T>> hasValues = new ArrayList<>();
			if (currentTargetItem != null) {
				hasValues.addAll(currentTargetItem.getValues());
			}
			
			Collection<PrismPropertyValue<T>> shouldHaveValues = outputTriple.getNonNegativeValues();
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Reconciliation of {}:\n  hasValues:\n{}\n  shouldHaveValues\n{}", 
						mappingOutputPath, DebugUtil.debugDump(hasValues, 2), DebugUtil.debugDump(shouldHaveValues, 2));
			}
			
			for (PrismPropertyValue<T> shouldHaveValue: shouldHaveValues) {
				if (!PrismPropertyValue.containsRealValue(hasValues, shouldHaveValue)) {
					if (targetItemDefinition.isSingleValue()) {
						targetItemDelta.setValueToReplace(shouldHaveValue.clone());
					} else {
						targetItemDelta.addValueToAdd(shouldHaveValue.clone());
					}
				}
			}
			
			if (targetItemDefinition.isSingleValue()) {
				if (!targetItemDelta.isReplace() && shouldHaveValues.isEmpty()) {
					targetItemDelta.setValueToReplace();
				}
			} else {
				for (PrismPropertyValue<T> hasValue: hasValues) {
					if (!PrismPropertyValue.containsRealValue(shouldHaveValues, hasValue)) {
						targetItemDelta.addValueToDelete(hasValue.clone());
					}
				}
			}
			
			if (!targetItemDelta.isEmpty()) {
				LOGGER.trace("Reconciliation delta:\n{}", targetItemDelta.debugDumpLazily(1));
				projCtx.swallowToSecondaryDelta(targetItemDelta);
			}
		}
		
	}



	private ItemDeltaItem<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> getLegalIdi(LensProjectionContext accCtx) throws SchemaException {
		Boolean legal = accCtx.isLegal();
		Boolean legalOld = accCtx.isLegalOld();
		return createBooleanIdi(LEGAL_PROPERTY_NAME, legalOld, legal);
	}

	@NotNull
	private ItemDeltaItem<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> createBooleanIdi(
			QName propertyName, Boolean old, Boolean current) throws SchemaException {
		PrismPropertyDefinitionImpl<Boolean> definition = new PrismPropertyDefinitionImpl<>(propertyName, DOMUtil.XSD_BOOLEAN, prismContext);
		definition.setMinOccurs(1);
		definition.setMaxOccurs(1);
		PrismProperty<Boolean> property = definition.instantiate();
		property.add(new PrismPropertyValue<>(current));

		if (current == old) {
			return new ItemDeltaItem<>(property);
		} else {
			PrismProperty<Boolean> propertyOld = property.clone();
			propertyOld.setRealValue(old);
			PropertyDelta<Boolean> delta = propertyOld.createDelta();
			delta.setValuesToReplace(new PrismPropertyValue<>(current));
			return new ItemDeltaItem<>(propertyOld, delta, property);
		}
	}

	private ItemDeltaItem<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> getAssignedIdi(LensProjectionContext accCtx) throws SchemaException {
        Boolean assigned = accCtx.isAssigned();
        Boolean assignedOld = accCtx.isAssignedOld();
		return createBooleanIdi(ASSIGNED_PROPERTY_NAME, assignedOld, assigned);
	}

    private <F extends ObjectType> ItemDeltaItem<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> getFocusExistsIdi(
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

		PrismPropertyDefinitionImpl<Boolean> existsDef = new PrismPropertyDefinitionImpl<>(FOCUS_EXISTS_PROPERTY_NAME,
				DOMUtil.XSD_BOOLEAN, prismContext);
		existsDef.setMinOccurs(1);
		existsDef.setMaxOccurs(1);
		PrismProperty<Boolean> existsProp = existsDef.instantiate();

		existsProp.add(new PrismPropertyValue<>(existsNew));

		if (existsOld == existsNew) {
			return new ItemDeltaItem<>(existsProp);
		} else {
			PrismProperty<Boolean> existsPropOld = existsProp.clone();
			existsPropOld.setRealValue(existsOld);
			PropertyDelta<Boolean> existsDelta = existsPropOld.createDelta();
			existsDelta.setValuesToReplace(new PrismPropertyValue<>(existsNew));
			return new ItemDeltaItem<>(existsPropOld, existsDelta, existsProp);
		}
	}

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public <O extends ObjectType> void processLifecycle(LensContext<O> context, LensProjectionContext projCtx,
    		XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

    	LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext != null && !FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for focal object.
    		LOGGER.trace("Skipping lifecycle evaluation because focus is not FocusType");
    		return;
    	}

    	processLifecycleFocus((LensContext)context, projCtx, now, task, result);
    }

	private <F extends FocusType> void processLifecycleFocus(LensContext<F> context, LensProjectionContext projCtx,
    		XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		LOGGER.trace("Skipping lifecycle evaluation because there is no focus");
    		return;
    	}

    	ResourceObjectLifecycleDefinitionType lifecycleDef = null;
    	ResourceObjectTypeDefinitionType resourceAccountDefType = projCtx.getResourceObjectTypeDefinitionType();
        if (resourceAccountDefType != null) {
        	lifecycleDef = resourceAccountDefType.getLifecycle();
        }
        ResourceBidirectionalMappingType lifecycleStateMappingType = null;
        if (lifecycleDef != null) {
        	lifecycleStateMappingType = lifecycleDef.getLifecycleState();
        }

        if (lifecycleStateMappingType == null || lifecycleStateMappingType.getOutbound() == null) {

        	if (!projCtx.isAdd()) {
        		LOGGER.trace("Skipping lifecycle evaluation because this is not add operation (default expression)");
        		return;
        	}

        	PrismObject<F> focusNew = focusContext.getObjectNew();
        	if (focusNew == null) {
        		LOGGER.trace("Skipping lifecycle evaluation because there is no new focus (default expression)");
        		return;
        	}

        	PrismObject<ShadowType> projectionNew = projCtx.getObjectNew();
        	if (projectionNew == null) {
        		LOGGER.trace("Skipping lifecycle evaluation because there is no new projection (default expression)");
        		return;
        	}

        	String lifecycle = midpointFunctions.computeProjectionLifecycle(
        			focusNew.asObjectable(), projectionNew.asObjectable(), projCtx.getResource());

        	LOGGER.trace("Computed projection lifecycle (default expression): {}", lifecycle);

        	if (lifecycle != null) {
        		PrismPropertyDefinition<String> propDef = projCtx.getObjectDefinition().findPropertyDefinition(SchemaConstants.PATH_LIFECYCLE_STATE);
        		PropertyDelta<String> lifeCycleDelta = propDef.createEmptyDelta(SchemaConstants.PATH_LIFECYCLE_STATE);
        		PrismPropertyValue<String> pval = new PrismPropertyValue<>(lifecycle);
        		pval.setOriginType(OriginType.OUTBOUND);
				lifeCycleDelta.setValuesToReplace(pval);
				projCtx.swallowToSecondaryDelta(lifeCycleDelta);
        	}

        } else {

        	LOGGER.trace("Computing projection lifecycle (mapping): {}", lifecycleStateMappingType);
	    	evaluateActivationMapping(context, projCtx, lifecycleStateMappingType,
	    			SchemaConstants.PATH_LIFECYCLE_STATE, SchemaConstants.PATH_LIFECYCLE_STATE,
	    			null, now, true, ObjectType.F_LIFECYCLE_STATE.getLocalPart(), task, result);
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
