/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.common.ActivationComputer;
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
import com.evolveum.midpoint.model.impl.lens.projector.util.ErrorHandlingUtil;
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
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ActivationUtil;
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
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Objects;
import java.util.Map.Entry;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_DISABLE_REASON;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

/**
 * The processor that takes care of user activation mapping to an account (outbound direction).
 *
 * Notes:
 *
 * 1. We use "old" state as the "before" state for mappings. It makes sense because we are dealing with projections,
 * which are relative to the initial focus state. (What about projections in waves greater than 0? TODO think about this.)
 *
 * 2. {@link ProcessorExecution#focusRequired()} applies only to {@link #processLifecycle(LensContext, LensProjectionContext, String,
 * XMLGregorianCalendar, Task, OperationResult)}, *not* to {@link #processProjectionsActivation(LensContext, String,
 * XMLGregorianCalendar, Task, OperationResult)}.
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
    private static final ItemName ADAPTED_ADMINISTRATIVE_STATUS_PROPERTY_NAME = new ItemName(SchemaConstants.NS_C, "adaptedAdministrativeStatus");
    private static final ItemName FOCUS_EXISTS_PROPERTY_NAME = new ItemName(SchemaConstants.NS_C, "focusExists");

    private static final String OP_ACTIVATION = Projector.class.getName() + ".activation"; // for historical reasons

    private static final String CLASS_DOT = ActivationProcessor.class.getName() + ".";
    private static final String OP_PROJECTION_ACTIVATION = CLASS_DOT + "projectionActivation";

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

    /**
     * Not a "medic-managed" entry point, hence the focusContext may or may not be present here
     * (i.e. `focusRequired` does not apply here).
     */
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
            activationResult.recordException(t);
            throw t;
        } finally {
            activationResult.close();
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
                    ErrorHandlingUtil.processProjectionNotFoundException(e, projectionContext);
                } catch (MappingLoader.NotLoadedException e) {
                    ErrorHandlingUtil.processProjectionNotLoadedException(e, projectionContext);
                }
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private <F extends FocusType> void processActivationMappingsCurrent(
            LensContext<F> context,
            LensProjectionContext projCtx,
            XMLGregorianCalendar now,
            Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException,
            CommunicationException, ConfigurationException, SecurityViolationException, MappingLoader.NotLoadedException {

        assert context.hasFocusContext();

        String projCtxDesc = projCtx.toHumanReadableString();
        SynchronizationPolicyDecision existingDecision = projCtx.getSynchronizationPolicyDecision();
        SynchronizationIntent synchronizationIntent = projCtx.getSynchronizationIntent();

        result.addContext("existingDecision", String.valueOf(existingDecision));
        result.addContext("synchronizationIntent", String.valueOf(synchronizationIntent));

        LOGGER.trace("processActivationMappingsCurrent starting for {}. Existing decision = {}, synchronization intent = {}",
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
            LOGGER.trace("Evaluated decision for {} to {}, skipping further activation processing",
                    projCtxDesc, SynchronizationPolicyDecision.DELETE);
            return;
        }

        LOGGER.trace("Evaluating intended existence of projection {} (legal={})", projCtxDesc, projCtx.isLegal());

        boolean shadowShouldExist = evaluateExistenceMapping(context, projCtx, now, MappingTimeEval.CURRENT, task, result);

        LOGGER.trace("Evaluated intended existence of projection {} to {} (legal={})",
                projCtxDesc, shadowShouldExist, projCtx.isLegal());

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
                    // We should delete the entire context, but then we will lose track of what happened. So just ignore it.
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
                throw new PolicyViolationException(
                        "Request to add projection " + projCtxDesc + " but the activation policy decided that it should not exist");
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
                throw new PolicyViolationException(
                        "Request to keep projection " + projCtxDesc + " but the activation policy decided that it should not exist");
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
            LOGGER.trace("No refined object definition, therefore also no activation outbound definition, "
                    + "skipping activation processing for account {}", projCtxDesc);
            return;
        }
        // [EP:M:OM] DONE, the bean is bound to the resource
        ResourceActivationDefinitionType activationDefinitionBean = resourceObjectDefinition.getActivationSchemaHandling();
        if (activationDefinitionBean == null) {
            LOGGER.trace("No activation definition in projection {}, skipping activation properties processing", projCtxDesc);
            return;
        }

        // FIXME what about object-class-level capabilities?
        ActivationCapabilityType capActivation =
                resourceObjectDefinition.getEnabledCapability(ActivationCapabilityType.class, projCtx.getResource());
        if (capActivation == null) {
            LOGGER.trace("Skipping activation status and validity processing because {} has no activation capability",
                    projCtx.getResource());
            return;
        }

        ActivationStatusCapabilityType capStatus = CapabilityUtil.getEnabledActivationStatus(capActivation);
        ActivationValidityCapabilityType capValidFrom = CapabilityUtil.getEnabledActivationValidFrom(capActivation);
        ActivationValidityCapabilityType capValidTo = CapabilityUtil.getEnabledActivationValidTo(capActivation);
        ActivationLockoutStatusCapabilityType capLockoutStatus = CapabilityUtil.getEnabledActivationLockoutStatus(capActivation);

        if (capStatus != null) {
            evaluateActivationMapping(context, projCtx, activationDefinitionBean,
                    activationDefinitionBean.getAdministrativeStatus(), // [EP:M:OM] DONE
                    PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                    PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                    capActivation, now, MappingTimeEval.CURRENT, ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart(), task, result);
        } else {
            LOGGER.trace("Skipping activation administrative status processing because {} "
                    + "does not have activation administrative status capability", projCtx.getResource());
        }

        ResourceBidirectionalMappingType validFromMappingType = activationDefinitionBean.getValidFrom();
        if (validFromMappingType == null || validFromMappingType.getOutbound() == null) {
            LOGGER.trace("Skipping activation validFrom processing because {} does not have appropriate outbound mapping", projCtx.getResource());
        } else if (capValidFrom == null && !ExpressionUtil.hasExplicitTarget(validFromMappingType.getOutbound())) {
            LOGGER.trace("Skipping activation validFrom processing because {} does not have activation validFrom capability nor outbound mapping with explicit target", projCtx.getResource());
        } else {
            evaluateActivationMapping(context, projCtx, activationDefinitionBean,
                    activationDefinitionBean.getValidFrom(), // [EP:M:OM] DONE
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
            evaluateActivationMapping(context, projCtx, activationDefinitionBean,
                    activationDefinitionBean.getValidTo(), // [EP:M:OM] DONE
                    SchemaConstants.PATH_ACTIVATION_VALID_TO,
                    SchemaConstants.PATH_ACTIVATION_VALID_TO,
                    null, now, MappingTimeEval.CURRENT, ActivationType.F_VALID_TO.getLocalPart(), task, result);
        }

        if (capLockoutStatus != null) {
            evaluateActivationMapping(context, projCtx, activationDefinitionBean,
                    activationDefinitionBean.getLockoutStatus(), // [EP:M:OM] DONE
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

    /**
     * Updates disable/enable timestamp and disable reason, if there is a change. Unlike before 4.8.1, we now consider
     * the change not only if the status itself changes, but also if the reason for the account being disabled changes.
     * See also MID-9220. Unfortunately, it is a kind of magic, see below and in
     * {@link #determineDisableReason(LensProjectionContext, boolean, String)}.
     *
     * Note that the old (and sometimes the new) status may be unknown, if the account is not loaded or if the status
     * is not cached.
     */
    private void processActivationMetadata(LensProjectionContext projCtx, XMLGregorianCalendar now)
            throws SchemaException {

        // We can take either current or old object. Note that if the object is loaded on demand, it is put into "current".
        ActivationStatusType oldStatus = ActivationUtil.getAdministrativeStatus(projCtx.getObjectCurrentOrOld());
        ActivationStatusType newStatus = ActivationUtil.getAdministrativeStatus(projCtx.getObjectNew());

        // The problem is that the old status may not be available because the old object is not loaded.
        // The new status may be null if there is no change.
        boolean statusChanged;
        if (oldStatus == null) {
            // We have no reliable information, so we need to have a look at the delta.
            statusChanged = projCtx.isModifiedInCurrentDelta(PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        } else {
            // If old status is known, the new status will be known as well.
            statusChanged = oldStatus != newStatus;
        }

        LOGGER.trace("processActivationMetadata starting for {}; status: {} -> {} (null can mean 'unknown'); changed: {}",
                projCtx, oldStatus, newStatus, statusChanged);

        String oldDisableReason;
        String newDisableReason;
        if (newStatus == ActivationStatusType.DISABLED) {
            oldDisableReason = ActivationUtil.getDisableReason(projCtx.getObjectOld());
            newDisableReason = determineDisableReason(projCtx, statusChanged, oldDisableReason);
            LOGGER.trace("Disable reason: {} -> {}", oldDisableReason, newDisableReason);
        } else {
            // These values are unused if the status is not DISABLED.
            oldDisableReason = null;
            newDisableReason = null;
        }

        // Disable reason of 'null' means it is either not important or it could not be determined
        var shouldUpdateDisableReason = newDisableReason != null && !Objects.equals(oldDisableReason, newDisableReason);

        if (!statusChanged && !shouldUpdateDisableReason) {
            LOGGER.trace("Administrative status nor disableReason not changed, timestamp and/or reason will not be recorded");
            return;
        }

        // set enable/disable timestamp
        projCtx.swallowToSecondaryDelta(
                LensUtil.createActivationTimestampDelta(newStatus, now, getActivationDefinition(), OriginType.OUTBOUND));

        // update disableReason if needed
        if (newStatus == ActivationStatusType.DISABLED && shouldUpdateDisableReason) {
            // The following check is here only to avoid accidental double overwriting of the reason.
            // Normally it should not occur: our code does not do this, and the client should NOT provide delta for it.
            if (!projCtx.isModifiedInCurrentDelta(PATH_ACTIVATION_DISABLE_REASON)) {
                PrismPropertyDefinition<String> disableReasonDef =
                        activationDefinition.findPropertyDefinition(ActivationType.F_DISABLE_REASON);
                PropertyDelta<String> disableReasonDelta = disableReasonDef.createEmptyDelta(PATH_ACTIVATION_DISABLE_REASON);
                disableReasonDelta.setValueToReplace(
                        prismContext.itemFactory().createPropertyValue(newDisableReason, OriginType.OUTBOUND, null));
                projCtx.swallowToSecondaryDelta(disableReasonDelta);
            }
        }
    }

    /** The reason for the account being disabled. This algorithm seems to be quite fragile; it should be reviewed. */
    private static String determineDisableReason(LensProjectionContext projCtx, boolean statusChanged, String oldDisableReason) {

        // Explicit disabling of a projection.
        //
        // Here we look at the secondary delta to make sure it does not contradict the primary one. This was the implementation
        // for years. OTOH, what if both deltas are present?
        //
        // 1. What if the account was disabled explicitly as well as because of a mapping?
        //    Then we would want to record the EXPLICIT reason. But, probably MAPPED would be OK as well.
        //    So the code seems to be OK with this respect.
        // 2. But what if the account was disabled explicitly, and the disable-instead-of-delete with delayed-delete is at play?
        //    Then we want to record the DEPROVISION reason. Hence, the code is definitely good here, as that will be the result.
        //
        // TODO What if the account is disabled for some other reason (mapped, deprovision), and in the future it's
        //   disabled also explicitly? Should we change the reason to EXPLICIT, or not? (Fortunately, it is not possible to
        //   do this from GUI: REST or Java API call would be needed.)
        if (hasAdministrativeStatusDelta(projCtx.getPrimaryDelta())
                && !hasAdministrativeStatusDelta(projCtx.getSecondaryDelta())) {
            return SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT;
        }

        Boolean legal = projCtx.isLegal();
        if (legal == null) {
            // Probably no focus here? We cannot decide on the disable reason.
            return null;
        } else if (!legal) {
            return SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION;
        }

        if (!statusChanged && SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT.equals(oldDisableReason)) {
            // Once explicit, always explicit. If we (e.g.) recompute the focus, we do not want the EXPLICIT reason
            // to disappear, even if we do not have the primary delta that caused the disablement.
            //
            // On the other hand, we do not want to keep obsolete/historic EXPLICIT value here:
            // if the account is explicitly disabled (-> EXPLICIT), then enabled by mapping, and then disabled by mapping,
            // we want to have MAPPED here. Hence the !statusChanged condition.
            return SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT;
        }

        // No other reason: it must came through a mapping.
        return SchemaConstants.MODEL_DISABLE_REASON_MAPPED;
    }

    private static boolean hasAdministrativeStatusDelta(ObjectDelta<ShadowType> delta) {
        return delta != null && delta.findPropertyDelta(PATH_ACTIVATION_ADMINISTRATIVE_STATUS) != null;
    }

    /**
     * We'll evaluate the mappings just to create the triggers.
     */
    private <F extends FocusType> void processActivationMappingsFuture(
            LensContext<F> context,
            LensProjectionContext projCtx,
            XMLGregorianCalendar now,
            Task task,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, MappingLoader.NotLoadedException {

        assert context.hasFocusContext();

        String accCtxDesc = projCtx.toHumanReadableString();
        SynchronizationPolicyDecision decision = projCtx.getSynchronizationPolicyDecision();

        LOGGER.trace("processActivationUserFuture starting for {}. Existing decision = {}", projCtx, decision);

        if (projCtx.isGone()
                || decision == SynchronizationPolicyDecision.BROKEN
                || decision == SynchronizationPolicyDecision.IGNORE
                || decision == SynchronizationPolicyDecision.UNLINK
                || decision == SynchronizationPolicyDecision.DELETE) {
            return;
        }

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
        // [EP:M:OM] DONE, as the bean is bound to the resource
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
            evaluateActivationMapping(
                    context, projCtx, activationDefinitionBean, // [EP:M:OM] DONE
                    activationDefinitionBean.getAdministrativeStatus(),
                    PATH_ACTIVATION_ADMINISTRATIVE_STATUS, PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                    capActivation, now, MappingTimeEval.FUTURE, ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart(), task, result);
        }

        if (capValidFrom != null) {
            evaluateActivationMapping(
                    context, projCtx, activationDefinitionBean,
                    activationDefinitionBean.getAdministrativeStatus(), // [EP:M:OM] DONE
                    SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                    null, now, MappingTimeEval.FUTURE, ActivationType.F_VALID_FROM.getLocalPart(), task, result);
        }

        if (capValidTo != null) {
            evaluateActivationMapping(
                    context, projCtx, activationDefinitionBean,
                    activationDefinitionBean.getAdministrativeStatus(), // [EP:M:OM] DONE
                    SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO,
                    null, now, MappingTimeEval.FUTURE, ActivationType.F_VALID_FROM.getLocalPart(), task, result);
        }
    }

    private <F extends FocusType> boolean evaluateExistenceMapping(
            LensContext<F> context, LensProjectionContext projCtx,
            XMLGregorianCalendar now, MappingTimeEval evaluationTime,
            Task task, final OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, MappingLoader.NotLoadedException {
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

        if (evaluationTime == MappingTimeEval.FUTURE) {
            createTriggerForPredefinedActivationMapping(
                    activationDefinitionBean, context, projCtx, SchemaConstants.PATH_ACTIVATION_EXISTENCE, task, now);
        } else {
            PredefinedActivationMappingEvaluator predefinedEvaluator = getPredefinedActivationMapping(
                    activationDefinitionBean, context, projCtx, task, now);
            if (predefinedEvaluator != null) {
                LOGGER.trace("Using {} as predefined activation evaluator", predefinedEvaluator.getName());
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

        MappingEvaluatorParams<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>, ShadowType, F> params =
                new MappingEvaluatorParams<>();

        // [EP:M:OM] DONE as the outbound mappings are bound to the resource in question
        OriginProvider<MappingType> originProvider =
                item -> ConfigurationItemOrigin.inResourceOrAncestor(projCtx.getResource());

        params.setMappingConfigItems(
                ConfigurationItem.ofList(outbound, originProvider, MappingConfigItem.class)); // [EP:M:OM] DONE
        params.setMappingDesc("outbound existence mapping in projection " + projCtxDesc);
        params.setNow(now);
        params.setAPrioriTargetObject(projCtx.getObjectOld());
        params.setEvaluateCurrent(evaluationTime);
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

        PrismPropertyDefinition<Boolean> shadowExistenceTargetDef =
                prismContext.definitionFactory().newPropertyDefinition(
                        SHADOW_EXISTS_PROPERTY_NAME, DOMUtil.XSD_BOOLEAN, 1, 1);
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

        for (PredefinedActivationMappingEvaluator evaluator : getConfiguredEvaluators(activationDefinitionBean, task)) {

            XMLGregorianCalendar currentNextRecomputeTime =
                    evaluator.getNextRecomputeTime(context, projCtx, path, now);
            if (currentNextRecomputeTime != null) {
                if (nextRecomputeTime == null
                        || nextRecomputeTime.compare(currentNextRecomputeTime) == DatatypeConstants.GREATER) {
                    nextRecomputeTime = currentNextRecomputeTime;
                    triggerOriginDescription = evaluator.getName();
                }
            }
        }

        if (nextRecomputeTime != null) {
            NextRecompute nextRecompute = new NextRecompute(nextRecomputeTime, triggerOriginDescription);
            nextRecompute.createTrigger(projCtx.getObjectOld(), projCtx.getObjectDefinition(), projCtx);
        }
    }

    private List<PredefinedActivationMappingEvaluator> getConfiguredEvaluators(
            ResourceActivationDefinitionType activationDefinitionBean, Task task) {

        if (activationDefinitionBean == null) {
            return List.of();
        }

        List<PredefinedActivationMappingEvaluator> evaluators = new ArrayList<>();
        for (Class<? extends PredefinedActivationMappingEvaluator> evaluatorClass : PREDEFINED_EVALUATORS) {

            try {
                PredefinedActivationMappingEvaluator evaluator = evaluatorClass
                        .getConstructor(ResourceActivationDefinitionType.class)
                        .newInstance(activationDefinitionBean);
                if (evaluator.isConfigured(task)) {
                    evaluators.add(evaluator);
                }
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw SystemException.unexpected(e, "when creating evaluator of predefined activation mappings");
            }
        }
        LOGGER.trace("Configured pre-defined evaluators: {}", evaluators);
        return evaluators;
    }

    private <F extends FocusType> PredefinedActivationMappingEvaluator getPredefinedActivationMapping(
            ResourceActivationDefinitionType activationDefinitionBean,
            LensContext<F> context, LensProjectionContext projCtx,
            Task task,
            XMLGregorianCalendar now)
            throws SchemaException, ConfigurationException {

        for (PredefinedActivationMappingEvaluator evaluator : getConfiguredEvaluators(activationDefinitionBean, task)) {
            LOGGER.trace("Determining applicability of {}", evaluator.getName());
            boolean applicable = evaluator.isApplicable(context, projCtx, now);
            LOGGER.trace("Conclusion: {} is {}", evaluator.getName(), applicable ? "applicable" : "not applicable");

            if (applicable) {
                // TODO can there be a situation where multiple predefined evaluators are applicable?
                return evaluator;
            }
        }

        LOGGER.trace("No predefined activation mapping is applicable");
        return null;
    }

    private <T, F extends FocusType> void evaluateActivationMapping(
            LensContext<F> context,
            LensProjectionContext projCtx,
            ResourceActivationDefinitionType activationDefinitionBean,
            ResourceBidirectionalMappingType bidirectionalMappingBean, // [EP:M:OM] DONE 8/8
            @Nullable ItemPath focusPropertyPath,
            ItemPath projectionPropertyPath,
            ActivationCapabilityType capActivation,
            XMLGregorianCalendar now,
            MappingTimeEval evaluationTime,
            String desc, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, MappingLoader.NotLoadedException {

        LOGGER.trace("Evaluating '{}' of projection {} ({})", projectionPropertyPath, projCtx, evaluationTime);

        if (evaluationTime == MappingTimeEval.FUTURE) {
            createTriggerForPredefinedActivationMapping(
                    activationDefinitionBean, context, projCtx, projectionPropertyPath, task, now);
        } else {
            PredefinedActivationMappingEvaluator predefinedEvaluator =
                    getPredefinedActivationMapping(activationDefinitionBean, context, projCtx, task, now);
            if (predefinedEvaluator != null && predefinedEvaluator.supportsActivationProperty(projectionPropertyPath)) {
                LOGGER.trace("Using {} as predefined activation evaluator for {}",
                        predefinedEvaluator.getName(), projectionPropertyPath);
                predefinedEvaluator.defineActivationProperty(context, projCtx, projectionPropertyPath, task, result);
                return;
            }
        }

        MappingInitializer<PrismPropertyValue<T>, PrismPropertyDefinition<T>> initializer =
                builder -> {
                    builder.mappingKind(MappingKindType.OUTBOUND);
                    builder.implicitTargetPath(projectionPropertyPath);

                    // Source: administrativeStatus, validFrom or validTo
                    LensFocusContext<F> focusContext = context.getFocusContext();

                    if (focusPropertyPath != null) {

                        ObjectDeltaObject<F> focusOdoAbsolute = focusContext.getObjectDeltaObjectAbsolute();
                        ItemDeltaItem<PrismPropertyValue<T>, PrismPropertyDefinition<T>> sourceIdi = focusOdoAbsolute.findIdi(focusPropertyPath);

                        if (capActivation != null && focusPropertyPath.equivalent(PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
                            ActivationValidityCapabilityType capValidFrom = CapabilityUtil.getEnabledActivationValidFrom(capActivation);
                            ActivationValidityCapabilityType capValidTo = CapabilityUtil.getEnabledActivationValidTo(capActivation);

                            // "Magic" computed status (tweaked admin status if validity is supported, effective status if not)
                            ItemDeltaItem<PrismPropertyValue<ActivationStatusType>, PrismPropertyDefinition<ActivationStatusType>> inputIdi;
                            if (capValidFrom != null && capValidTo != null) {
                                LOGGER.trace("Native validFrom and validTo -> using adapted administrativeStatus as the implicit input");
                                // We have to convert ARCHIVED to DISABLED, to avoid passing it forward via "asIs" mapping (MID-9026)
                                inputIdi = createIdi(
                                        ADAPTED_ADMINISTRATIVE_STATUS_PROPERTY_NAME, SchemaConstants.C_ACTIVATION_STATUS_TYPE,
                                        getAdaptedAdministrativeStatus(focusContext.getObjectOld()),
                                        getAdaptedAdministrativeStatus(focusContext.getObjectNew()));
                                // Marking the source as "administrativeStatus" is not entirely correct, because we actually use
                                // a derived value. Fortunately, the implicit source path is only for tracing purposes; moreover,
                                // this should be a temporary solution until ARCHIVED is removed in 4.9.
                                builder.implicitSourcePath(focusPropertyPath);
                            } else {
                                LOGGER.trace("No native validFrom and validTo -> using effectiveStatus as the implicit input");
                                inputIdi = focusOdoAbsolute.findIdi(SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS);
                                builder.implicitSourcePath(SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS);
                            }

                            builder.defaultSource(new Source<>(inputIdi, ExpressionConstants.VAR_INPUT_QNAME));
                            builder.additionalSource(new Source<>(sourceIdi, ExpressionConstants.VAR_ADMINISTRATIVE_STATUS_QNAME));

                        } else {
                            builder.defaultSource(new Source<>(sourceIdi, ExpressionConstants.VAR_INPUT_QNAME));
                            builder.implicitSourcePath(focusPropertyPath);
                        }
                    }

                    builder.additionalSource(new Source<>(getLegalIdi(projCtx), ExpressionConstants.VAR_LEGAL_QNAME));
                    builder.additionalSource(new Source<>(getAssignedIdi(projCtx), ExpressionConstants.VAR_ASSIGNED_QNAME));
                    builder.additionalSource(new Source<>(getFocusExistsIdi(focusContext), ExpressionConstants.VAR_FOCUS_EXISTS_QNAME));

                    return builder;
                };

        evaluateOutboundMapping( // [EP:M:OM] DONE
                context, projCtx, bidirectionalMappingBean, projectionPropertyPath, initializer,
                now, evaluationTime, desc + " outbound activation mapping", task, result);

        LOGGER.trace("Finished evaluation of '{}' of projection {} ({})", projectionPropertyPath, projCtx, evaluationTime);
    }

    private <F extends FocusType> ActivationStatusType getAdaptedAdministrativeStatus(PrismObject<F> object) {
        return ActivationComputer.archivedToDisabled(
                ActivationUtil.getAdministrativeStatus(
                        asObjectable(object)));
    }

    private <T, F extends FocusType> void evaluateOutboundMapping(
            LensContext<F> context,
            LensProjectionContext projCtx,
            ResourceBidirectionalMappingType bidirectionalMappingBean, // [EP:M:OM] DONE 1/1
            ItemPath projectionPropertyPath,
            MappingInitializer<PrismPropertyValue<T>, PrismPropertyDefinition<T>> initializer,
            XMLGregorianCalendar now,
            MappingTimeEval evaluationTime,
            String desc, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, MappingLoader.NotLoadedException {

        if (bidirectionalMappingBean == null) {
            LOGGER.trace("No '{}' definition in projection {}, skipping", desc, projCtx.toHumanReadableString());
            return;
        }
        List<MappingType> outboundMappingBeans = bidirectionalMappingBean.getOutbound();
        if (outboundMappingBeans == null || outboundMappingBeans.isEmpty()) {
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

        // [EP:M:OM] DONE as the outboundMappingBeans are shown to belong to the resource in question, see above
        OriginProvider<MappingType> originProvider =
                item -> ConfigurationItemOrigin.inResourceOrAncestor(projCtx.getResourceRequired());

        MappingEvaluatorParams<PrismPropertyValue<T>, PrismPropertyDefinition<T>, ShadowType, F> params = new MappingEvaluatorParams<>();
        params.setMappingConfigItems( // [EP:M:OM] DONE
                ConfigurationItem.ofList(outboundMappingBeans, originProvider, MappingConfigItem.class));
        params.setMappingDesc(desc + " in projection " + projCtxDesc);
        params.setNow(now);
        params.setInitializer(internalInitializer);
        params.setTargetLoader(new ProjectionMappingLoader(projCtx, contextLoader, projCtx::isActivationLoaded));
        params.setTargetValueAvailable(projCtx.isActivationLoaded());
        params.setAPrioriTargetObject(shadowNew);
        params.setAPrioriTargetDelta(LensUtil.findAPrioriDelta(context, projCtx));
        if (context.getFocusContext() != null) {
            params.setSourceContext(context.getFocusContext().getObjectDeltaObjectAbsolute());
        }
        params.setTargetContext(projCtx);
        params.setDefaultTargetItemPath(projectionPropertyPath);
        params.setEvaluateCurrent(evaluationTime);
        params.setEvaluateWeak(true);
        params.setContext(context);

        Map<UniformItemPath, MappingOutputStruct<PrismPropertyValue<T>>> outputTripleMap =
                projectionMappingSetEvaluator.evaluateMappingsToTriples(params, task, result);

        LOGGER.trace("Mapping processing output after {} ({}):\n{}", desc, evaluationTime,
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

            Collection<PrismPropertyValue<T>> shouldHaveValues =
                    outputTriple != null ? outputTriple.getNonNegativeValues() : List.of();

            LOGGER.trace("Reconciliation of {}:\n  hasValues:\n{}\n  shouldHaveValues\n{}",
                    mappingOutputPath,
                    DebugUtil.debugDumpLazily(hasValues, 2),
                    DebugUtil.debugDumpLazily(shouldHaveValues, 2));

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

    private ItemDeltaItem<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> getLegalIdi(LensProjectionContext accCtx)
            throws SchemaException {
        return createIdi(
                LEGAL_PROPERTY_NAME, DOMUtil.XSD_BOOLEAN,
                accCtx.isLegalOld(), accCtx.isLegal());
    }

    @NotNull
    private <T> ItemDeltaItem<PrismPropertyValue<T>, PrismPropertyDefinition<T>> createIdi(
            QName propertyName, QName typeName, T old, T current) throws SchemaException {
        PrismPropertyDefinition<T> definition =
                prismContext.definitionFactory().newPropertyDefinition(propertyName, typeName, 1, 1);
        PrismProperty<T> property = definition.instantiate();
        if (current != null) {
            property.add(prismContext.itemFactory().createPropertyValue(current));
        }

        if (Objects.equals(current, old)) {
            return new ItemDeltaItem<>(property);
        } else {
            PrismProperty<T> propertyOld = property.clone();
            propertyOld.setRealValue(old);
            PropertyDelta<T> delta = propertyOld.createDelta();
            if (current != null) {
                //noinspection unchecked
                delta.setValuesToReplace(prismContext.itemFactory().createPropertyValue(current));
            } else {
                //noinspection unchecked
                delta.setValuesToReplace();
            }
            return new ItemDeltaItem<>(propertyOld, delta, property, definition);
        }
    }

    private ItemDeltaItem<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> getAssignedIdi(LensProjectionContext accCtx)
            throws SchemaException {
        return createIdi(
                ASSIGNED_PROPERTY_NAME, DOMUtil.XSD_BOOLEAN,
                accCtx.isAssignedOld(), accCtx.isAssigned());
    }

    private <F extends ObjectType> ItemDeltaItem<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> getFocusExistsIdi(
            LensFocusContext<F> lensFocusContext) throws SchemaException {
        PrismPropertyDefinition<Boolean> existsDef =
                prismContext.definitionFactory().newPropertyDefinition(
                        FOCUS_EXISTS_PROPERTY_NAME, DOMUtil.XSD_BOOLEAN, 1, 1);
        PrismProperty<Boolean> existsProp = existsDef.instantiate();

        Boolean existsOld;
        Boolean existsNew;

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
            existsProp.add(prismContext.itemFactory().createPropertyValue(existsNew));
        } else {
            existsOld = null;
            existsNew = null;
        }

        if (Objects.equals(existsOld, existsNew)) {
            return new ItemDeltaItem<>(existsProp);
        } else {
            assert lensFocusContext != null && existsNew != null;

            PrismProperty<Boolean> existsPropOld = existsProp.clone();
            existsPropOld.setRealValue(existsOld);
            PropertyDelta<Boolean> existsDelta = existsPropOld.createDelta();
            //noinspection unchecked
            existsDelta.setValuesToReplace(prismContext.itemFactory().createPropertyValue(existsNew));
            return new ItemDeltaItem<>(existsPropOld, existsDelta, existsProp, existsDef);
        }
    }

    @ProcessorMethod
    <F extends FocusType> void processLifecycle(LensContext<F> context, LensProjectionContext projCtx,
            @SuppressWarnings("unused") String activityDescription, XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        ResourceBidirectionalMappingType purposeMappings = getPurposeMappings(projCtx); // [EP:M:OM] DONE
        if (purposeMappings == null) {

            if (!projCtx.isAdd()) {
                LOGGER.trace("Skipping purpose evaluation because this is not an add operation (default expression)");
                return;
            }

            PrismObject<F> focusNew = context.getFocusContext().getObjectNew();
            if (focusNew == null) {
                LOGGER.trace("Skipping purpose evaluation because there is no new focus (default expression)");
                return;
            }

            PrismObject<ShadowType> projectionNew = projCtx.getObjectNew();
            if (projectionNew == null) {
                LOGGER.trace("Skipping purpose evaluation because there is no new projection (default expression)");
                return;
            }

            ShadowPurposeType purpose = midpointFunctions.computeDefaultProjectionPurpose(
                    focusNew.asObjectable(), projectionNew.asObjectable(), projCtx.getResource());

            LOGGER.trace("Computed projection purpose (default expression): {}", purpose);

            if (purpose != null) {
                PrismPropertyDefinition<ShadowPurposeType> propDef =
                        projCtx.getObjectDefinition().findPropertyDefinition(ShadowType.F_PURPOSE);
                PropertyDelta<ShadowPurposeType> purposeDelta = propDef.createEmptyDelta(ShadowType.F_PURPOSE);
                PrismPropertyValue<ShadowPurposeType> pval = prismContext.itemFactory().createPropertyValue(purpose);
                pval.setOriginType(OriginType.OUTBOUND);
                //noinspection unchecked
                purposeDelta.setValuesToReplace(pval);
                projCtx.swallowToSecondaryDelta(purposeDelta);
            }

        } else {

            LOGGER.trace("Computing projection purpose (using mapping): {}", purposeMappings);
            try {
                evaluateActivationMapping(
                        context, projCtx, null,
                        purposeMappings, // [EP:M:OM] DONE
                        null, ShadowType.F_PURPOSE,
                        null, now, MappingTimeEval.CURRENT, ShadowType.F_PURPOSE.getLocalPart(), task, result);
            } catch (MappingLoader.NotLoadedException e) {
                ErrorHandlingUtil.processProjectionNotLoadedException(e, projCtx);
            }
        }

        context.checkConsistenceIfNeeded();
    }

    // [EP:M:OM] DONE, the bean is obtained from the resource
    @Nullable
    private ResourceBidirectionalMappingType getPurposeMappings(LensProjectionContext projCtx)
            throws SchemaException, ConfigurationException {
        var resourceObjectDefinition = projCtx.getStructuralDefinitionIfNotBroken();
        if (resourceObjectDefinition == null) {
            return null;
        }
        var lifecycleDefinitions = resourceObjectDefinition.getDefinitionBean().getLifecycle();
        if (lifecycleDefinitions == null) {
            return null;
        }
        assertNoLifecycleStateOutboundMappings(lifecycleDefinitions);
        return lifecycleDefinitions.getPurpose();
    }

    /**
     * Lifecycle state outbound mappings were there since 3.6. In 4.8, they were migrated to the "purpose" mappings.
     * We should tell the user if they are still there.
     */
    private void assertNoLifecycleStateOutboundMappings(ResourceObjectLifecycleDefinitionType lifecycleDefinitions)
            throws ConfigurationException {
        var stateMappings = lifecycleDefinitions.getLifecycleState();
        if (stateMappings == null) {
            return;
        }
        var outbound = stateMappings.getOutbound();
        if (outbound.isEmpty()) {
            return;
        }
        throw new ConfigurationException(
                "Outbound mappings for 'lifecycleState' are no longer supported. Please migrate them to 'purpose' mappings.");
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
