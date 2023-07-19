/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationIntent;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ProjectionMappingSetEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.*;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping.DelayedDeleteEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping.DisableInsteadOfDeleteEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping.PreProvisionEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping.PredefinedActivationMappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationLockoutStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationValidityCapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Objects;
import java.util.Map.Entry;

/**
 * The processor that takes care of user activation mapping to an account (outbound direction).
 *
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = FocusType.class)
public class ActivationProcessor implements ProjectorProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ActivationProcessor.class);

    private static final ItemName SHADOW_EXISTS_PROPERTY_NAME = new ItemName(SchemaConstants.NS_C, "shadowExists");
    private static final ItemName LEGAL_PROPERTY_NAME = new ItemName(SchemaConstants.NS_C, "legal");
    private static final ItemName ASSIGNED_PROPERTY_NAME = new ItemName(SchemaConstants.NS_C, "assigned");
    private static final ItemName FOCUS_EXISTS_PROPERTY_NAME = new ItemName(SchemaConstants.NS_C, "focusExists");

    private static final String OP_ACTIVATION = Projector.class.getName() + ".activation"; // for historical reasons
    private static final String OP_PROJECTION_ACTIVATION = ActivationProcessor.class.getName() + ".projectionActivation";

    /**
     * Collection with classes of predefine mapping scenarios.
     * depends on the order
     */
    private static final Collection<Class<? extends PredefinedActivationMappingEvaluator>> PREDEFINED_EVALUATORS =
            List.of(PreProvisionEvaluator.class, DelayedDeleteEvaluator.class, DisableInsteadOfDeleteEvaluator.class);

    @Autowired private ContextLoader contextLoader;
    @Autowired private PrismContext prismContext;
    @Autowired private ProjectionMappingSetEvaluator projectionMappingSetEvaluator;
    @Autowired private MidpointFunctions midpointFunctions;
    @Autowired private ClockworkMedic medic;

    private PrismObjectDefinition<UserType> userDefinition;
    private PrismContainerDefinition<ActivationType> activationDefinition;

    // not a "medic-managed" entry point
    <F extends ObjectType> void processProjectionsActivation(LensContext<F> context, String activityDescription,
            XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        OperationResult activationResult = result.subresult(OP_ACTIVATION)
                .setMinor()
                .build();
        try {
            LOGGER.trace("Processing activation for all contexts");
            for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
                if (!projectionContext.isBroken() && !projectionContext.isIgnored()) {
                    processActivation(context, projectionContext, now, task, activationResult);
                }
            }
            context.removeIgnoredContexts();
            medic.traceContext(LOGGER, activityDescription, "projection activation of all resources", true,
                    context, true);
            context.checkConsistenceIfNeeded();
        } catch (Throwable t) {
            activationResult.recordFatalError(t);
            throw t;
        } finally {
            activationResult.computeStatusIfUnknown();
        }
    }

    private <O extends ObjectType, F extends FocusType> void processActivation(LensContext<O> context,
            LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task, OperationResult parentResult)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        OperationResult result = parentResult.subresult(OP_PROJECTION_ACTIVATION)
                .addParam("resourceOid", projectionContext.getResourceOid())
                .addParam("resourceName", projectionContext.getResourceName())
                .addParam("projection", projectionContext.getHumanReadableName())
                .build();
        try {

            // There may be contexts with unknown resources (e.g. for broken linkRefs). We want to execute the activation
            // for them - at least for now.
            if (projectionContext.hasResource() && !projectionContext.isVisible()) {
                LOGGER.trace("Projection {} is not visible for this task execution mode, skipping activation processing",
                        projectionContext.getHumanReadableName());
                result.recordNotApplicable("Not visible");
                return;
            }

            if (!projectionContext.isCurrentForProjection()) {
                LOGGER.trace("Projection {} is not current, skipping activation processing", projectionContext.getHumanReadableName());
                result.recordNotApplicable("Not current");
                return;
            }

            LensFocusContext<O> focusContext = context.getFocusContext();
            if (focusContext == null || !FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {

                processActivationMetadata(projectionContext, now);

            } else {
                try {

                    //noinspection unchecked
                    LensContext<F> contextCasted = (LensContext<F>) context;
                    processActivationMappingsCurrent(contextCasted, projectionContext, now, task, result);
                    processActivationMetadata(projectionContext, now);
                    processActivationMappingsFuture(contextCasted, projectionContext, now, task, result);

                } catch (ObjectNotFoundException e) {
                    if (projectionContext.isGone()) {
                        // This is not critical. The projection is marked as gone and we can go on with processing
                        // No extra action is needed.
                    } else {
                        throw e;
                    }
                }
            }

            projectionContext.recompute();
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <F extends FocusType> void processActivationMappingsCurrent(LensContext<F> context, LensProjectionContext projCtx,
            XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException,
            SecurityViolationException {

        String projCtxDesc = projCtx.toHumanReadableString();
        SynchronizationPolicyDecision existingDecision = projCtx.getSynchronizationPolicyDecision();
        SynchronizationIntent synchronizationIntent = projCtx.getSynchronizationIntent();

        result.addContext("existingDecision", String.valueOf(existingDecision));
        result.addContext("synchronizationIntent", String.valueOf(synchronizationIntent));

        LOGGER.trace("processActivationUserCurrent starting for {}. Existing decision = {}, synchronization intent = {}",
                projCtxDesc, existingDecision, synchronizationIntent);

        if (existingDecision == SynchronizationPolicyDecision.BROKEN) {
            LOGGER.trace("Broken projection {}, skipping further activation processing", projCtxDesc);
            return;
        }

        // Activation is computed on projector start but can be recomputed in the respective wave again.
        // So we allow existingDecision to be non-null. An alternative would be to skip all activation processing
        // in such a case. But the current approach is closer to the previous implementation and safer.

        if (synchronizationIntent == SynchronizationIntent.UNLINK) {
            setSynchronizationPolicyDecision(projCtx, SynchronizationPolicyDecision.UNLINK, result);
            LOGGER.trace("Evaluated decision for {} to {} because of unlink synchronization intent, "
                    + "skipping further activation processing", projCtxDesc, SynchronizationPolicyDecision.UNLINK);
            return;
        }

        if (projCtx.isGone()) {
            if (projCtx.isDelete() && ModelExecuteOptions.isForce(context.getOptions())) {
                setSynchronizationPolicyDecision(projCtx, SynchronizationPolicyDecision.DELETE, result);
                LOGGER.trace("Evaluated decision for 'gone' {} to {} (force), skipping further activation processing",
                        projCtxDesc, SynchronizationPolicyDecision.DELETE);
            } else {
                // Let's keep 'goners' linked until they expire. So we do not have shadows without owners.
                // This is also needed for async delete operations.
                setSynchronizationPolicyDecision(projCtx, SynchronizationPolicyDecision.KEEP, result);
                LOGGER.trace("Evaluated decision for {} to {} because it is gone, skipping further activation processing",
                        projCtxDesc, SynchronizationPolicyDecision.KEEP);
            }
            return;
        }

        if (projCtx.isReaping()) {
            // Projections being reaped (i.e. having pending DELETE actions) should be kept intact.
            // This is based on assumption that it is not possible to cancel the pending DELETE operation.
            // If it was, we could try to do that.
            setSynchronizationPolicyDecision(projCtx, SynchronizationPolicyDecision.KEEP, result);
            LOGGER.trace("Evaluated decision for {} to {} because it is reaping, skipping further activation processing",
                    projCtxDesc, SynchronizationPolicyDecision.KEEP);
            return;
        }

        if (synchronizationIntent == SynchronizationIntent.DELETE || projCtx.isDelete()) {
            // TODO: is this OK?
            setSynchronizationPolicyDecision(projCtx, SynchronizationPolicyDecision.DELETE, result);
            LOGGER.trace("Evaluated decision for {} to {}, skipping further activation processing", projCtxDesc,
                    SynchronizationPolicyDecision.DELETE);
            return;
        }

        LOGGER.trace("Evaluating intended existence of projection {} (legal={})", projCtxDesc, projCtx.isLegal());

        boolean shadowShouldExist = evaluateExistenceMapping(context, projCtx, now, MappingTimeEval.CURRENT, task, result);

        LOGGER.trace("Evaluated intended existence of projection {} to {} (legal={})", projCtxDesc, shadowShouldExist, projCtx.isLegal());

        // Let's reconcile the existence intent (shadowShouldExist) and the synchronization intent in the context

        LensProjectionContext lowerOrderContext = context.findLowerOrderContext(projCtx);

        SynchronizationPolicyDecision decision;
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
                    // we should delete the entire context, but then we will lose track of what
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
                throw new PolicyViolationException("Request to add projection " + projCtxDesc + " but the activation policy decided that it should not exist");
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
                throw new PolicyViolationException("Request to keep projection " + projCtxDesc + " but the activation policy decided that it should not exist");
            }

        } else {
            throw new IllegalStateException("Unknown sync intent " + synchronizationIntent);
        }

        LOGGER.trace("Evaluated decision for projection {} to {}", projCtxDesc, decision);

        setSynchronizationPolicyDecision(projCtx, decision, result);

        PrismObject<F> focusNew = context.getFocusContext().getObjectNew();
        if (focusNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("focusNew is null, skipping activation processing of {}", projCtxDesc);
            return;
        }

        if (decision == SynchronizationPolicyDecision.DELETE) {
            LOGGER.trace("Decision is {}, skipping activation properties processing for {}", decision, projCtxDesc);
            return;
        }

        ResourceObjectDefinition resourceObjectDefinition = projCtx.getStructuralDefinitionIfNotBroken();
        if (resourceObjectDefinition == null) {
            LOGGER.trace("No refined object definition, therefore also no activation outbound definition, skipping activation processing for account {}", projCtxDesc);
            return;
        }
        ResourceActivationDefinitionType activationDefinitionBean = resourceObjectDefinition.getActivationSchemaHandling();
        if (activationDefinitionBean == null) {
            LOGGER.trace("No activation definition in projection {}, skipping activation properties processing", projCtxDesc);
            return;
        }

        ActivationCapabilityType capActivation =
                resourceObjectDefinition.getEnabledCapability(ActivationCapabilityType.class, projCtx.getResource());
        if (capActivation == null) {
            LOGGER.trace("Skipping activation status and validity processing because {} has no activation capability", projCtx.getResource());
            return;
        }

        ActivationStatusCapabilityType capStatus = CapabilityUtil.getEnabledActivationStatus(capActivation);
        ActivationValidityCapabilityType capValidFrom = CapabilityUtil.getEnabledActivationValidFrom(capActivation);
        ActivationValidityCapabilityType capValidTo = CapabilityUtil.getEnabledActivationValidTo(capActivation);
        ActivationLockoutStatusCapabilityType capLockoutStatus = CapabilityUtil.getEnabledActivationLockoutStatus(capActivation);

        if (capStatus != null) {
            evaluateActivationMapping(context, projCtx, activationDefinitionBean,
                    activationDefinitionBean.getAdministrativeStatus(),
                    SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                    SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                    capActivation, now, MappingTimeEval.CURRENT, ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart(), task, result);
        } else {
            LOGGER.trace("Skipping activation administrative status processing because {} does not have activation administrative status capability", projCtx.getResource());
        }

        ResourceBidirectionalMappingType validFromMappingType = activationDefinitionBean.getValidFrom();
        if (validFromMappingType == null || validFromMappingType.getOutbound() == null) {
            LOGGER.trace("Skipping activation validFrom processing because {} does not have appropriate outbound mapping", projCtx.getResource());
        } else if (capValidFrom == null && !ExpressionUtil.hasExplicitTarget(validFromMappingType.getOutbound())) {
            LOGGER.trace("Skipping activation validFrom processing because {} does not have activation validFrom capability nor outbound mapping with explicit target", projCtx.getResource());
        } else {
            evaluateActivationMapping(context, projCtx,
                    activationDefinitionBean, activationDefinitionBean.getValidFrom(),
                    SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                    SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                    null, now, MappingTimeEval.CURRENT, ActivationType.F_VALID_FROM.getLocalPart(), task, result);
        }

        ResourceBidirectionalMappingType validToMappingType = activationDefinitionBean.getValidTo();
        if (validToMappingType == null || validToMappingType.getOutbound() == null) {
            LOGGER.trace("Skipping activation validTo processing because {} does not have appropriate outbound mapping", projCtx.getResource());
        } else if (capValidTo == null && !ExpressionUtil.hasExplicitTarget(validToMappingType.getOutbound())) {
            LOGGER.trace("Skipping activation validTo processing because {} does not have activation validTo capability nor outbound mapping with explicit target", projCtx.getResource());
        } else {
            evaluateActivationMapping(context, projCtx,
                    activationDefinitionBean, activationDefinitionBean.getValidTo(),
                    SchemaConstants.PATH_ACTIVATION_VALID_TO,
                    SchemaConstants.PATH_ACTIVATION_VALID_TO,
                    null, now, MappingTimeEval.CURRENT, ActivationType.F_VALID_TO.getLocalPart(), task, result);
        }

        if (capLockoutStatus != null) {
            evaluateActivationMapping(context, projCtx,
                    activationDefinitionBean,
                    activationDefinitionBean.getLockoutStatus(),
                    SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
                    capActivation, now, MappingTimeEval.CURRENT, ActivationType.F_LOCKOUT_STATUS.getLocalPart(), task, result);
        } else {
            LOGGER.trace("Skipping activation lockout status processing because {} does not have activation lockout status capability", projCtx.getResource());
        }
    }

    private void setSynchronizationPolicyDecision(
            LensProjectionContext projCtx, SynchronizationPolicyDecision decision, OperationResult result) {
        projCtx.setSynchronizationPolicyDecision(decision);
        result.addReturn("decision", String.valueOf(decision));
    }

    private void processActivationMetadata(LensProjectionContext projCtx, XMLGregorianCalendar now) throws SchemaException {
        ObjectDelta<ShadowType> projDelta = projCtx.getCurrentDelta();
        if (projDelta == null) {
            LOGGER.trace("No projection delta -> no activation metadata processing");
            return;
        }

        LOGGER.trace("processActivationMetadata starting for {}", projCtx);

        PropertyDelta<ActivationStatusType> statusDelta = projDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);

        if (statusDelta != null && !statusDelta.isDelete()) {

            // we have to determine if the status really changed
            PrismObject<ShadowType> oldShadow = projCtx.getObjectOld();
            ActivationStatusType statusOld;
            if (oldShadow != null && oldShadow.asObjectable().getActivation() != null) {
                statusOld = oldShadow.asObjectable().getActivation().getAdministrativeStatus();
            } else {
                statusOld = null;
            }

            PrismProperty<ActivationStatusType> statusPropNew = (PrismProperty<ActivationStatusType>) statusDelta.getItemNewMatchingPath(null);
            ActivationStatusType statusNew = statusPropNew.getRealValue();

            if (statusNew == statusOld) {
                LOGGER.trace("Administrative status not changed ({}), timestamp and/or reason will not be recorded", statusNew);
            } else {
                // timestamps
                PropertyDelta<XMLGregorianCalendar> timestampDelta = LensUtil.createActivationTimestampDelta(statusNew,
                        now, getActivationDefinition(), OriginType.OUTBOUND, prismContext);
                projCtx.swallowToSecondaryDelta(timestampDelta);

                // disableReason
                if (statusNew == ActivationStatusType.DISABLED) {
                    PropertyDelta<String> disableReasonDelta = projDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_DISABLE_REASON);
                    if (disableReasonDelta == null) {
                        String disableReason;
                        ObjectDelta<ShadowType> projPrimaryDelta = projCtx.getPrimaryDelta();
                        ObjectDelta<ShadowType> projSecondaryDelta = projCtx.getSecondaryDelta();
                        if (projPrimaryDelta != null
                                && projPrimaryDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS) != null
                                && (projSecondaryDelta == null || projSecondaryDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS) == null)) {
                            disableReason = SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT;
                        } else if (projCtx.isLegal()) {
                            disableReason = SchemaConstants.MODEL_DISABLE_REASON_MAPPED;
                        } else {
                            disableReason = SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION;
                        }

                        PrismPropertyDefinition<String> disableReasonDef =
                                activationDefinition.findPropertyDefinition(ActivationType.F_DISABLE_REASON);
                        disableReasonDelta = disableReasonDef.createEmptyDelta(
                                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON));
                        disableReasonDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(disableReason, OriginType.OUTBOUND, null));
                        projCtx.swallowToSecondaryDelta(disableReasonDelta);
                    }
                }
            }
        }
    }

    /**
     * We'll evaluate the mappings just to create the triggers.
     */
    private <F extends FocusType> void processActivationMappingsFuture(LensContext<F> context, LensProjectionContext projCtx,
            XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        String accCtxDesc = projCtx.toHumanReadableString();
        SynchronizationPolicyDecision decision = projCtx.getSynchronizationPolicyDecision();

        LOGGER.trace("processActivationUserFuture starting for {}. Existing decision = {}", projCtx, decision);

        if (projCtx.isGone() || decision == SynchronizationPolicyDecision.BROKEN
                || decision == SynchronizationPolicyDecision.IGNORE
                || decision == SynchronizationPolicyDecision.UNLINK || decision == SynchronizationPolicyDecision.DELETE) {
            return;
        }

        projCtx.recompute();

        evaluateExistenceMapping(context, projCtx, now, MappingTimeEval.FUTURE, task, result);

        PrismObject<F> focusNew = context.getFocusContext().getObjectNew();
        if (focusNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("focusNew is null, skipping activation processing of {}", accCtxDesc);
            return;
        }

        ResourceObjectDefinition resourceObjectDefinition = projCtx.getStructuralDefinitionIfNotBroken();
        if (resourceObjectDefinition == null) {
            return;
        }
        ResourceActivationDefinitionType activationDefinitionBean = resourceObjectDefinition.getActivationSchemaHandling();
        if (activationDefinitionBean == null) {
            return;
        }

        ActivationCapabilityType capActivation =
                resourceObjectDefinition.getEnabledCapability(ActivationCapabilityType.class, projCtx.getResource());
        if (capActivation == null) {
            return;
        }

        ActivationStatusCapabilityType capStatus = CapabilityUtil.getEnabledActivationStatus(capActivation);
        ActivationValidityCapabilityType capValidFrom = CapabilityUtil.getEnabledActivationValidFrom(capActivation);
        ActivationValidityCapabilityType capValidTo = CapabilityUtil.getEnabledActivationValidTo(capActivation);

        if (capStatus != null) {
            evaluateActivationMapping(context, projCtx,
                    activationDefinitionBean, activationDefinitionBean.getAdministrativeStatus(),
                    SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                    capActivation, now, MappingTimeEval.FUTURE, ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart(), task, result);
        }

        if (capValidFrom != null) {
            evaluateActivationMapping(context, projCtx,
                    activationDefinitionBean, activationDefinitionBean.getAdministrativeStatus(),
                    SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                    null, now, MappingTimeEval.FUTURE, ActivationType.F_VALID_FROM.getLocalPart(), task, result);
        }

        if (capValidTo != null) {
            evaluateActivationMapping(context, projCtx,
                    activationDefinitionBean, activationDefinitionBean.getAdministrativeStatus(),
                    SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO,
                    null, now, MappingTimeEval.FUTURE, ActivationType.F_VALID_FROM.getLocalPart(), task, result);
        }
    }

    private <F extends FocusType> boolean evaluateExistenceMapping(final LensContext<F> context,
            final LensProjectionContext projCtx, final XMLGregorianCalendar now, final MappingTimeEval current,
            Task task, final OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        final String projCtxDesc = projCtx.toHumanReadableString();

        final Boolean legal = projCtx.isLegal();
        if (legal == null) {
            throw new IllegalStateException("Null 'legal' for " + projCtxDesc);
        }

        ResourceObjectDefinition resourceObjectDefinition = projCtx.getStructuralDefinitionIfNotBroken();
        if (resourceObjectDefinition == null) {
            return legal;
        }
        ResourceActivationDefinitionType activationDefinitionBean = resourceObjectDefinition.getActivationSchemaHandling();
        if (activationDefinitionBean == null) {
            return legal;
        }

        if (MappingTimeEval.FUTURE.equals(current)) {
            createTriggerForPredefinedActivationMapping(
                    activationDefinitionBean, context, projCtx, SchemaConstants.PATH_ACTIVATION_EXISTENCE, task, now);
        } else {
            PredefinedActivationMappingEvaluator predefinedEvaluator = getPredefinedActivationMapping(
                    activationDefinitionBean, context, projCtx, task, now);
            if (predefinedEvaluator != null) {
                LOGGER.trace("Using {} as predefined activation evaluator", predefinedEvaluator.getClass().getSimpleName());
                return predefinedEvaluator.defineExistence(context, projCtx);
            }
        }

        ResourceBidirectionalMappingType existenceDefBean = activationDefinitionBean.getExistence();
        if (existenceDefBean == null) {
            return legal;
        }
        List<MappingType> outbound = existenceDefBean.getOutbound();
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

        ObjectDeltaObject<F> focusOdoAbsolute = context.getFocusContext().getObjectDeltaObjectAbsolute();
        params.setInitializer(builder -> {
            builder.mappingKind(MappingKindType.OUTBOUND)
                    .implicitSourcePath(LEGAL_PROPERTY_NAME)
                    .implicitTargetPath(SHADOW_EXISTS_PROPERTY_NAME);

            builder.defaultSource(new Source<>(getLegalIdi(projCtx), ExpressionConstants.VAR_LEGAL_QNAME));
            builder.additionalSource(new Source<>(getAssignedIdi(projCtx), ExpressionConstants.VAR_ASSIGNED_QNAME));
            builder.additionalSource(new Source<>(getFocusExistsIdi(context.getFocusContext()), ExpressionConstants.VAR_FOCUS_EXISTS_QNAME));

            // Variable: focus
            builder.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdoAbsolute, context.getFocusContext().getObjectDefinition());

            // Variable: user (for convenience, same as "focus"), DEPRECATED
            builder.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdoAbsolute, context.getFocusContext().getObjectDefinition());
            builder.addAliasRegistration(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);

            // Variable: projection
            // This may be tricky when creation of a new projection is considered.
            // In that case we do not have any projection object (account) yet, neither new nor old. But we already have
            // projection context. We have to pass projection definition explicitly here.
            ObjectDeltaObject<ShadowType> projectionOdo = projCtx.getObjectDeltaObject();
            builder.addVariableDefinition(ExpressionConstants.VAR_SHADOW, projectionOdo, projCtx.getObjectDefinition());
            builder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, projectionOdo, projCtx.getObjectDefinition());
            builder.addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION);

            // Variable: resource
            builder.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projCtx.getResource(), ResourceType.class);

            builder.originType(OriginType.OUTBOUND);
            builder.originObject(projCtx.getResource());
            return builder;
        });

        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> aggregatedOutputTriple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();

        params.setProcessor((mappingOutputPath, outputStruct) -> {
            // This is a very primitive implementation of output processing.
            // Maybe we should somehow use the default processing in MappingEvaluator, but it's quite complex
            // and therefore we should perhaps wait for general mapping cleanup (MID-3847).
            PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = outputStruct.getOutputTriple();
            if (outputTriple != null) {
                aggregatedOutputTriple.merge(outputTriple);
            }
            return false;
        });

        MutablePrismPropertyDefinition<Boolean> shadowExistenceTargetDef = prismContext.definitionFactory().createPropertyDefinition(SHADOW_EXISTS_PROPERTY_NAME, DOMUtil.XSD_BOOLEAN);
        shadowExistenceTargetDef.setMinOccurs(1);
        shadowExistenceTargetDef.setMaxOccurs(1);
        shadowExistenceTargetDef.freeze();
        params.setTargetItemDefinition(shadowExistenceTargetDef);
        params.setSourceContext(focusOdoAbsolute);
        projectionMappingSetEvaluator.evaluateMappingsToTriples(params, task, result);

        boolean output;
        if (aggregatedOutputTriple.isEmpty()) {
            output = legal;     // the default
        } else {
            Collection<PrismPropertyValue<Boolean>> nonNegativeValues = aggregatedOutputTriple.getNonNegativeValues();
            if (nonNegativeValues.isEmpty()) {
                throw new ExpressionEvaluationException("Activation existence expression resulted in no values for projection " + projCtxDesc);
            } else if (nonNegativeValues.size() > 1) {
                throw new ExpressionEvaluationException("Activation existence expression resulted in too many values (" + nonNegativeValues.size() + ") for projection " + projCtxDesc + ": " + nonNegativeValues);
            } else {
                PrismPropertyValue<Boolean> value = nonNegativeValues.iterator().next();
                if (value != null && value.getRealValue() != null) {
                    output = value.getRealValue();
                } else {
                    // TODO could this even occur?
                    throw new ExpressionEvaluationException("Activation existence expression resulted in null value for projection " + projCtxDesc);
                }
            }
        }

        return output;
    }

    private <F extends FocusType> void createTriggerForPredefinedActivationMapping(
            ResourceActivationDefinitionType activationDefinitionBean, LensContext<F> context,
            LensProjectionContext projCtx, ItemPath path, Task task, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {

        if (activationDefinitionBean == null) {
            return;
        }

        XMLGregorianCalendar nextRecomputeTime = null;
        String triggerOriginDescription = null;

        for (Class<? extends PredefinedActivationMappingEvaluator> evaluatorClass : PREDEFINED_EVALUATORS) {

            try {
                Constructor<? extends PredefinedActivationMappingEvaluator> constructor =
                        evaluatorClass.getConstructor(ResourceActivationDefinitionType.class);
                PredefinedActivationMappingEvaluator evaluator = constructor.newInstance(activationDefinitionBean);

                if (!evaluator.isConfigured(task)) {
                    continue;
                }

                XMLGregorianCalendar currentNextRecomputeTime =
                        evaluator.defineTimeForTriggerOfActivation(context, projCtx, path, now);
                if (currentNextRecomputeTime != null) {
                    if (nextRecomputeTime == null
                            || nextRecomputeTime.compare(currentNextRecomputeTime) == DatatypeConstants.GREATER) {
                        nextRecomputeTime = currentNextRecomputeTime;
                        triggerOriginDescription = evaluatorClass.getSimpleName();
                    }
                }
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                LOGGER.trace("Couldn't create evaluator of predefined activation mappings", e);
            }
        }

        if (nextRecomputeTime != null) {
            NextRecompute nextRecompute = new NextRecompute(nextRecomputeTime, triggerOriginDescription);
            nextRecompute.createTrigger(projCtx.getObjectOld(), projCtx.getObjectDefinition(), projCtx);
        }
    }

    private <F extends FocusType> PredefinedActivationMappingEvaluator getPredefinedActivationMapping(
            ResourceActivationDefinitionType activationDefinitionBean, final LensContext<F> context,
            final LensProjectionContext projCtx, Task task, XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {

        if (activationDefinitionBean != null) {
            for (Class<? extends PredefinedActivationMappingEvaluator> evaluatorClass : PREDEFINED_EVALUATORS) {

                try {
                    Constructor<? extends PredefinedActivationMappingEvaluator> constructor =
                            evaluatorClass.getConstructor(ResourceActivationDefinitionType.class);
                    PredefinedActivationMappingEvaluator evaluator = constructor.newInstance(activationDefinitionBean);

                    if (!evaluator.isConfigured(task)) {
                        continue;
                    }
                    if (evaluator.isApplicable(context, projCtx, now)) {
                        return evaluator;
                    }
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    LOGGER.trace("Couldn't create evaluator of predefined activation mappings", e);
                }
            }
        }

        LOGGER.trace("No predefined activation mapping is configured.");
        return null;
    }

    private <T, F extends FocusType> void evaluateActivationMapping(
            final LensContext<F> context, final LensProjectionContext projCtx,
            ResourceActivationDefinitionType activationDefinitionBean, ResourceBidirectionalMappingType bidirectionalMappingType,
            final ItemPath focusPropertyPath, final ItemPath projectionPropertyPath,
            final ActivationCapabilityType capActivation, XMLGregorianCalendar now, final MappingTimeEval current,
            String desc, final Task task, final OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        if (MappingTimeEval.FUTURE.equals(current)) {
            createTriggerForPredefinedActivationMapping(
                    activationDefinitionBean, context, projCtx, projectionPropertyPath, task, now);
        } else {
            PredefinedActivationMappingEvaluator predefinedEvaluator =
                    getPredefinedActivationMapping(activationDefinitionBean, context, projCtx, task, now);
            if (predefinedEvaluator != null && predefinedEvaluator.supportsActivation(projectionPropertyPath)) {
                LOGGER.trace("Using {} as predefined activation evaluator", predefinedEvaluator.getClass().getSimpleName());
                predefinedEvaluator.defineActivation(context, projCtx, projectionPropertyPath);
                return;
            }
        }

        MappingInitializer<PrismPropertyValue<T>, PrismPropertyDefinition<T>> initializer =
                builder -> {
                    builder.mappingKind(MappingKindType.OUTBOUND);
                    builder.implicitTargetPath(projectionPropertyPath);

                    // Source: administrativeStatus, validFrom or validTo
                    ObjectDeltaObject<F> focusOdoAbsolute = context.getFocusContext().getObjectDeltaObjectAbsolute();
                    ItemDeltaItem<PrismPropertyValue<T>, PrismPropertyDefinition<T>> sourceIdi = focusOdoAbsolute.findIdi(focusPropertyPath);

                    if (capActivation != null && focusPropertyPath.equivalent(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
                        ActivationValidityCapabilityType capValidFrom = CapabilityUtil.getEnabledActivationValidFrom(capActivation);
                        ActivationValidityCapabilityType capValidTo = CapabilityUtil.getEnabledActivationValidTo(capActivation);

                        // Source: computedShadowStatus
                        ItemPath sourcePath;
                        if (capValidFrom != null && capValidTo != null) {
                            // "Native" validFrom and validTo, directly use administrativeStatus
                            sourcePath = focusPropertyPath;
                        } else {
                            // Simulate validFrom and validTo using effectiveStatus
                            sourcePath = SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS;
                        }
                        ItemDeltaItem<PrismPropertyValue<ActivationStatusType>, PrismPropertyDefinition<ActivationStatusType>> computedIdi =
                                focusOdoAbsolute.findIdi(sourcePath); // TODO wave
                        builder.implicitSourcePath(sourcePath);

                        builder.defaultSource(new Source<>(computedIdi, ExpressionConstants.VAR_INPUT_QNAME));
                        builder.additionalSource(new Source<>(sourceIdi, ExpressionConstants.VAR_ADMINISTRATIVE_STATUS_QNAME));

                    } else {
                        builder.defaultSource(new Source<>(sourceIdi, ExpressionConstants.VAR_INPUT_QNAME));
                        builder.implicitSourcePath(focusPropertyPath);
                    }

                    builder.additionalSource(new Source<>(getLegalIdi(projCtx), ExpressionConstants.VAR_LEGAL_QNAME));
                    builder.additionalSource(new Source<>(getAssignedIdi(projCtx), ExpressionConstants.VAR_ASSIGNED_QNAME));
                    builder.additionalSource(new Source<>(getFocusExistsIdi(context.getFocusContext()), ExpressionConstants.VAR_FOCUS_EXISTS_QNAME));

                    return builder;
                };

        evaluateOutboundMapping(context, projCtx, bidirectionalMappingType, focusPropertyPath, projectionPropertyPath, initializer,
                now, current, desc + " outbound activation mapping", task, result);
    }

    private <T, F extends FocusType> void evaluateOutboundMapping(final LensContext<F> context,
            final LensProjectionContext projCtx, ResourceBidirectionalMappingType bidirectionalMappingType,
            final ItemPath focusPropertyPath, final ItemPath projectionPropertyPath,
            final MappingInitializer<PrismPropertyValue<T>, PrismPropertyDefinition<T>> initializer,
            XMLGregorianCalendar now, final MappingTimeEval evaluateCurrent, String desc, final Task task, final OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

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

        MappingInitializer<PrismPropertyValue<T>, PrismPropertyDefinition<T>> internalInitializer =
                builder -> {

                    builder.addVariableDefinitions(ModelImplUtils.getDefaultVariablesMap(context, projCtx, true));

                    builder.originType(OriginType.OUTBOUND);
                    builder.mappingKind(MappingKindType.OUTBOUND);
                    builder.originObject(projCtx.getResource());
                    builder.implicitTargetPath(projectionPropertyPath);

                    initializer.initialize(builder);

                    return builder;
                };

        MappingEvaluatorParams<PrismPropertyValue<T>, PrismPropertyDefinition<T>, ShadowType, F> params = new MappingEvaluatorParams<>();
        params.setMappingTypes(outboundMappingTypes);
        params.setMappingDesc(desc + " in projection " + projCtxDesc);
        params.setNow(now);
        params.setInitializer(internalInitializer);
        params.setTargetLoader(new ProjectionMappingLoader<>(projCtx, contextLoader));
        params.setAPrioriTargetObject(shadowNew);
        params.setAPrioriTargetDelta(LensUtil.findAPrioriDelta(context, projCtx));
        if (context.getFocusContext() != null) {
            params.setSourceContext(context.getFocusContext().getObjectDeltaObjectAbsolute());
        }
        params.setTargetContext(projCtx);
        params.setDefaultTargetItemPath(projectionPropertyPath);
        params.setEvaluateCurrent(evaluateCurrent);
        params.setEvaluateWeak(true);
        params.setContext(context);
        params.setHasFullTargetObject(projCtx.hasFullShadow());

        Map<UniformItemPath, MappingOutputStruct<PrismPropertyValue<T>>> outputTripleMap =
                projectionMappingSetEvaluator.evaluateMappingsToTriples(params, task, result);

        LOGGER.trace("Mapping processing output after {} ({}):\n{}", desc, evaluateCurrent,
                DebugUtil.debugDumpLazily(outputTripleMap, 1));

        if (projCtx.isDoReconciliation()) {
            reconcileOutboundValue(projCtx, outputTripleMap, desc);
        }
    }

    /**
     * TODO: can we align this with ReconciliationProcessor?
     */
    private <T> void reconcileOutboundValue(
            LensProjectionContext projCtx,
            Map<UniformItemPath, MappingOutputStruct<PrismPropertyValue<T>>> outputTripleMap,
            String desc) throws SchemaException {

        // TODO: check for full shadow?

        for (Entry<UniformItemPath, MappingOutputStruct<PrismPropertyValue<T>>> entry : outputTripleMap.entrySet()) {
            UniformItemPath mappingOutputPath = entry.getKey();
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
                throw new SchemaException("No definition for item " + mappingOutputPath + " in " + targetObjectDefinition);
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

            LOGGER.trace("Reconciliation of {}:\n  hasValues:\n{}\n  shouldHaveValues\n{}",
                    mappingOutputPath, DebugUtil.debugDumpLazily(hasValues, 2), DebugUtil.debugDumpLazily(shouldHaveValues, 2));

            for (PrismPropertyValue<T> shouldHaveValue : shouldHaveValues) {
                if (!PrismValueCollectionsUtil.containsRealValue(hasValues, shouldHaveValue)) {
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
                for (PrismPropertyValue<T> hasValue : hasValues) {
                    if (!PrismValueCollectionsUtil.containsRealValue(shouldHaveValues, hasValue)) {
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

    private ItemDeltaItem<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> getLegalIdi(LensProjectionContext accCtx) throws SchemaException {
        Boolean legal = accCtx.isLegal();
        Boolean legalOld = accCtx.isLegalOld();
        return createBooleanIdi(LEGAL_PROPERTY_NAME, legalOld, legal);
    }

    @NotNull
    private ItemDeltaItem<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> createBooleanIdi(
            QName propertyName, Boolean old, Boolean current) throws SchemaException {
        MutablePrismPropertyDefinition<Boolean> definition = prismContext.definitionFactory().createPropertyDefinition(propertyName, DOMUtil.XSD_BOOLEAN);
        definition.setMinOccurs(1);
        definition.setMaxOccurs(1);
        PrismProperty<Boolean> property = definition.instantiate();
        if (current != null) {
            property.add(prismContext.itemFactory().createPropertyValue(current));
        }

        if (Objects.equals(current, old)) {
            return new ItemDeltaItem<>(property);
        } else {
            PrismProperty<Boolean> propertyOld = property.clone();
            propertyOld.setRealValue(old);
            PropertyDelta<Boolean> delta = propertyOld.createDelta();
            if (current != null) {
                delta.setValuesToReplace(prismContext.itemFactory().createPropertyValue(current));
            } else {
                delta.setValuesToReplace();
            }
            return new ItemDeltaItem<>(propertyOld, delta, property, definition);
        }
    }

    private ItemDeltaItem<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> getAssignedIdi(LensProjectionContext accCtx) throws SchemaException {
        Boolean assigned = accCtx.isAssigned();
        Boolean assignedOld = accCtx.isAssignedOld();
        return createBooleanIdi(ASSIGNED_PROPERTY_NAME, assignedOld, assigned);
    }

    private <F extends ObjectType> ItemDeltaItem<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> getFocusExistsIdi(
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

        MutablePrismPropertyDefinition<Boolean> existsDef = prismContext.definitionFactory().createPropertyDefinition(FOCUS_EXISTS_PROPERTY_NAME,
                DOMUtil.XSD_BOOLEAN);
        existsDef.setMinOccurs(1);
        existsDef.setMaxOccurs(1);
        PrismProperty<Boolean> existsProp = existsDef.instantiate();

        if (existsNew != null) {
            existsProp.add(prismContext.itemFactory().createPropertyValue(existsNew));
        }

        if (Objects.equals(existsOld, existsNew)) {
            return new ItemDeltaItem<>(existsProp);
        } else {
            PrismProperty<Boolean> existsPropOld = existsProp.clone();
            existsPropOld.setRealValue(existsOld);
            PropertyDelta<Boolean> existsDelta = existsPropOld.createDelta();
            if (existsNew != null) {
                //noinspection unchecked
                existsDelta.setValuesToReplace(prismContext.itemFactory().createPropertyValue(existsNew));
            } else {
                //noinspection unchecked
                existsDelta.setValuesToReplace();
            }
            return new ItemDeltaItem<>(existsPropOld, existsDelta, existsProp, existsDef);
        }
    }

    @ProcessorMethod
    <F extends FocusType> void processLifecycle(LensContext<F> context, LensProjectionContext projCtx,
            @SuppressWarnings("unused") String activityDescription, XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        ResourceBidirectionalMappingType lifecycleStateMapping = getLifecycleStateMapping(projCtx);
        if (lifecycleStateMapping == null || lifecycleStateMapping.getOutbound() == null) {

            if (!projCtx.isAdd()) {
                LOGGER.trace("Skipping lifecycle evaluation because this is not an add operation (default expression)");
                return;
            }

            PrismObject<F> focusNew = context.getFocusContext().getObjectNew();
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
                PrismPropertyValue<String> pval = prismContext.itemFactory().createPropertyValue(lifecycle);
                pval.setOriginType(OriginType.OUTBOUND);
                //noinspection unchecked
                lifeCycleDelta.setValuesToReplace(pval);
                projCtx.swallowToSecondaryDelta(lifeCycleDelta);
            }

        } else {

            LOGGER.trace("Computing projection lifecycle (using mapping): {}", lifecycleStateMapping);
            evaluateActivationMapping(context, projCtx, null, lifecycleStateMapping,
                    SchemaConstants.PATH_LIFECYCLE_STATE, SchemaConstants.PATH_LIFECYCLE_STATE,
                    null, now, MappingTimeEval.CURRENT, ObjectType.F_LIFECYCLE_STATE.getLocalPart(), task, result);
        }

        context.checkConsistenceIfNeeded();
        projCtx.recompute();
        context.checkConsistenceIfNeeded();
    }

    @Nullable
    private ResourceBidirectionalMappingType getLifecycleStateMapping(LensProjectionContext projCtx)
            throws SchemaException, ConfigurationException {
        ResourceObjectLifecycleDefinitionType lifecycleDef;
        ResourceObjectDefinition resourceObjectDefinition = projCtx.getStructuralDefinitionIfNotBroken();
        if (resourceObjectDefinition != null) {
            lifecycleDef = resourceObjectDefinition.getDefinitionBean().getLifecycle();
        } else {
            lifecycleDef = null;
        }
        ResourceBidirectionalMappingType lifecycleStateMapping;
        if (lifecycleDef != null) {
            lifecycleStateMapping = lifecycleDef.getLifecycleState();
        } else {
            lifecycleStateMapping = null;
        }
        return lifecycleStateMapping;
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
