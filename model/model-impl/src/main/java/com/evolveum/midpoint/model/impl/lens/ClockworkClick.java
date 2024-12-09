/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext.AuthorizationState;
import com.evolveum.midpoint.model.impl.lens.projector.Components;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClockworkClickTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;

import java.util.ArrayList;
import java.util.Collection;

import static com.evolveum.midpoint.model.impl.lens.LensUtil.getExportType;

/**
 * Represents and executes a single clockwork click.
 */
public class ClockworkClick<F extends ObjectType> {

    // For backwards compatibility we keep the class of Clockwork here (not ClockworkClick)
    private static final Trace LOGGER = TraceManager.getTrace(Clockwork.class);

    // For backwards compatibility we keep the class of Clockwork here (not ClockworkClick)
    private static final String OP_CLICK = Clockwork.class.getName() + ".click";

    @NotNull private final LensContext<F> context;
    @NotNull private final ModelBeans beans;
    @NotNull private final Task task;
    @NotNull private final XMLGregorianCalendar now;

    /** A trace for the current click, if needed. */
    @Nullable private ClockworkClickTraceType trace;

    ClockworkClick(@NotNull LensContext<F> context, @NotNull ModelBeans beans, @NotNull Task task) {
        this.context = context;
        this.beans = beans;
        this.task = task;
        this.now = beans.clock.currentTimeXMLGregorianCalendar();
    }

    public HookOperationMode click(OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException,
            ConflictDetectedException {

        // DO NOT CHECK CONSISTENCY of the context here. The context may not be fresh and consistent yet. Project will fix
        // that. Check consistency afterwards (and it is also checked inside projector several times).

        if (context.getInspector() == null) {
            context.setInspector(beans.medic.getClockworkInspector());
        }

        OperationResult result = parentResult.subresult(OP_CLICK)
                .addQualifier(context.getOperationQualifier())
                .addArbitraryObjectAsContext("context", context)
                .addArbitraryObjectAsContext("task", task)
                .build();
        createTraceIfNeeded(result);

        try {

            // We need to determine focus before auditing. Otherwise we will not know user
            // for the accounts (unless there is a specific delta for it).
            // This is ugly, but it is the easiest way now (TODO: cleanup).
            beans.contextLoader.loadFocusContext(context, task, result);

            ModelState state = context.getState();
            if (state == ModelState.INITIAL) {
                beans.medic.clockworkStart(context);
                beans.metadataManager.setRequestMetadataInContext(context, now, task);
                context.getStats().setRequestTimestamp(now);
                context.generateRequestIdentifierIfNeeded();
                // We need to do this BEFORE projection. If we would do that after projection
                // there will be secondary changes that are not part of the request.
                // As for the results: we need also the overall ("Clockwork.run") operation result.
                beans.clockworkAuditHelper.audit(context, AuditEventStage.REQUEST, task, result, parentResult);
            }

            if (state != ModelState.FINAL) {
                projectIfNeeded(result);
            }

            checkIndestructible(result);

            // The preliminary authorization is done in the projector after loading the context.
            // Here we finish it, as we have now the full information from the projector.
            if (context.getAuthorizationState() != AuthorizationState.FULL) {
                ClockworkRequestAuthorizer.authorizeContextRequest(context, true, task, result);
            }

            beans.medic.traceContext(LOGGER, "CLOCKWORK (" + state + ")", "before processing", true, context, false);
            context.checkConsistenceIfNeeded();
            context.checkEncryptedIfNeeded();

            return moveStateForward(parentResult, result, state);

        } catch (CommunicationException | ConfigurationException | ExpressionEvaluationException | ObjectNotFoundException |
                PolicyViolationException | SchemaException | SecurityViolationException | RuntimeException | Error |
                ObjectAlreadyExistsException | ConflictDetectedException e) {
            processClockworkException(e, result, parentResult);
            throw e;
        } finally {
            finishTrace(result);
            result.computeStatusIfUnknown(); // Maybe this should be "composite" instead.
            result.cleanupResultDeeply();
        }
    }

    private void createTraceIfNeeded(OperationResult result) throws SchemaException {
        if (result.isTracingAny(ClockworkClickTraceType.class)) {
            trace = new ClockworkClickTraceType();
            if (result.isTracingNormal(ClockworkClickTraceType.class)) {
                trace.setInputLensContextText(context.debugDump());
            }
            trace.setInputLensContext(context.toBean(getExportType(trace, result)));
            result.getTraces().add(trace);
        } else {
            trace = null;
        }
    }

    private void finishTrace(OperationResult result) throws SchemaException {
        if (trace != null) {
            if (result.isTracingNormal(ClockworkClickTraceType.class)) {
                trace.setOutputLensContextText(context.debugDump());
            }
            trace.setOutputLensContext(context.toBean(getExportType(trace, result)));
        }
    }

    private void projectIfNeeded(OperationResult result)
            throws SchemaException, ConfigurationException, PolicyViolationException, ExpressionEvaluationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, SecurityViolationException,
            ConflictDetectedException {
        boolean recompute;
        if (!context.isFresh()) {
            LOGGER.trace("Context is not fresh -- forcing cleanup and recomputation");
            recompute = true;
        } else if (context.getExecutionWave() > context.getProjectionWave()) { // should not occur
            LOGGER.warn("Execution wave is greater than projection wave -- forcing cleanup and recomputation");
            recompute = true;
        } else if (context.isInPrimary() && ModelExecuteOptions.getInitialPartialProcessing(context.getOptions()) != null) {
            LOGGER.trace("Initial phase was run with initialPartialProcessing option -- forcing cleanup and recomputation");
            recompute = true;
        } else {
            recompute = false;
        }

        if (recompute) {
            context.cleanup();
            LOGGER.trace("Running projector with cleaned-up context for execution wave {}", context.getExecutionWave());
            beans.projector.project(context, "PROJECTOR ("+ context.getState() +")", task, result);
        } else if (context.getExecutionWave() == context.getProjectionWave()) {
            LOGGER.trace("Resuming projector for execution wave {}", context.getExecutionWave());
            beans.projector.resume(context, "PROJECTOR ("+ context.getState() +")", task, result);
        } else {
            LOGGER.trace("Skipping projection because the context is fresh and projection for current wave has already run");
        }
    }

    private HookOperationMode moveStateForward(OperationResult parentResult,
            OperationResult result, ModelState state) throws PolicyViolationException, ObjectNotFoundException, SchemaException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ConflictDetectedException {
        switch (state) {
            case INITIAL:
                processInitialToPrimary(result);
                break;
            case PRIMARY:
                processPrimaryToSecondary();
                break;
            case SECONDARY:
                processSecondary(result, parentResult);
                if (context.getExecutionWave() > context.getMaxWave() + 1) {
                    processSecondaryToFinal(context, task, result);
                }
                break;
            case FINAL:
                HookOperationMode mode = processFinal(result, parentResult);
                beans.medic.clockworkFinish(context);
                return mode;
        }
        return beans.clockworkHookHelper.invokeHooks(context, task, result);
    }

    private void processInitialToPrimary(OperationResult result)
            throws PolicyViolationException, ConfigurationException {
        // To mimic operation of the original enforcer hook, we execute the following only in the initial state.
        beans.policyRuleProcessor.enforce(context, result);

        switchState(ModelState.PRIMARY);
    }

    private void processPrimaryToSecondary() {
        switchState(ModelState.SECONDARY);
    }

    private void processSecondary(OperationResult result, OperationResult overallResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException, ConflictDetectedException {

        context.clearLastChangeExecutionResult();

        beans.medic.partialExecute(Components.EXECUTION,
                (result1) -> beans.changeExecutor.executeChanges(context, task, result1),
                context.getPartialProcessingOptions()::getExecution,
                Clockwork.class, context, null, result);

        beans.clockworkAuditHelper.audit(context, AuditEventStage.EXECUTION, task, result, overallResult);

        context.updateAfterExecution();

        if (!context.isProjectionRecomputationRequested()) {
            context.incrementExecutionWave();
        } else {
            LOGGER.trace("Restart of the current execution wave ({}) was requested by the change executor; "
                            + "setting focus context as not fresh", context.getExecutionWave());
            LensFocusContext<?> focusContext = context.getFocusContext();
            if (focusContext != null) {
                focusContext.setFresh(false); // will run activation again (hopefully)
            }
            // Shouldn't we explicitly rot the whole context here? (maybe not)
            // BTW, what if restart is requested indefinitely?
        }

        beans.medic.traceContext(
                LOGGER, "CLOCKWORK (" + context.getState() + ")", "change execution", false,
                context, false);
    }

    private void processSecondaryToFinal(LensContext<F> context, Task task, OperationResult result) throws SchemaException {
        switchState(ModelState.FINAL);
        beans.policyRuleScriptExecutor.execute(context, task, result);
    }

    private HookOperationMode processFinal(OperationResult result, OperationResult overallResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {
        beans.clockworkAuditHelper.auditFinalExecution(context, task, result, overallResult);
        logFinalReadable(context);
        beans.migrator.executeAfterOperationMigration(context, result);

        return beans.personaProcessor.processPersonaChanges(context, task, result);
    }

    /**
     * Check for indestructibility.
     * This check just makes sure that we fail fast, before we run full clockwork.
     */
    private void checkIndestructible(OperationResult result) throws IndestructibilityViolationException {
        checkIndestructible(context.getFocusContext(), result);
        for (LensProjectionContext projCtx : context.getProjectionContexts()) {
            checkIndestructible(projCtx, result);
        }
    }

    private <O extends ObjectType> void checkIndestructible(LensElementContext<O> elementContext, OperationResult result)
            throws IndestructibilityViolationException {
        if (elementContext != null && elementContext.isDelete()) {
            PrismObject<O> contextObject = elementContext.getObjectAny();
            if (contextObject != null && Boolean.TRUE.equals(contextObject.asObjectable().isIndestructible())) {
                IndestructibilityViolationException e =
                        new IndestructibilityViolationException("Attempt to delete indestructible object " + contextObject);
                ModelImplUtils.recordFatalError(result, e);
                throw e;
            }
        }
    }

    private void processClockworkException(Throwable e, OperationResult result, OperationResult overallResult)
            throws SchemaException {
        LOGGER.trace("Processing clockwork exception {}", e.toString());
        result.recordException(e);
        beans.clockworkAuditHelper.auditEvent(context, AuditEventStage.EXECUTION, null, true, task, result, overallResult);

        reclaimSequencesIfPossible(result);
        result.recordEnd();
    }

    /**
     * An exception occurred, so it's quite possible that allocated sequence values have to be reclaimed.
     *
     * But this is safe to do only if we are sure they were not used. Currently the only way how
     * to be sure is to know that no focus/projection deltas were applied. It is an approximate
     * solution (because sequence values were maybe not used in these deltas), but we have nothing
     * better at hand now.
     */
    private void reclaimSequencesIfPossible(OperationResult result) throws SchemaException {
        if (!context.wasAnythingExecuted()) {
            LensUtil.reclaimSequences(context, beans.cacheRepositoryService, task, result);
        } else {
            LOGGER.trace("Something was executed, so we are not reclaiming sequence values");
        }
    }

    /**
     * Logs the entire operation in a human-readable fashion.
     */
    private void logFinalReadable(LensContext<F> context) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        // a priori: sync delta
        boolean hasSyncDelta = false;
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            ObjectDelta<ShadowType> syncDelta = projectionContext.getSyncDelta();
            if (syncDelta != null) {
                hasSyncDelta = true;
                break;
            }
        }

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = context.getExecutedDeltas();
        if (!hasSyncDelta && executedDeltas.isEmpty()) {
            // Not worth mentioning
            return;
        }

        StringBuilder sb = new StringBuilder();
        String channel = context.getChannel();
        if (channel != null) {
            sb.append("Channel: ").append(channel).append("\n");
        }

        if (hasSyncDelta) {
            sb.append("Triggered by synchronization delta\n");
            for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
                ObjectDelta<ShadowType> syncDelta = projectionContext.getSyncDelta();
                if (syncDelta != null) {
                    sb.append(syncDelta.debugDump(1));
                    sb.append("\n");
                }
                DebugUtil.debugDumpLabel(sb, "Situation", 1);
                sb.append(" ");
                sb.append(projectionContext.getSynchronizationSituationDetected());
                sb.append(" -> ");
                sb.append(projectionContext.getSynchronizationSituationResolved());
                sb.append("\n");
            }
        }
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            if (projectionContext.isSyncAbsoluteTrigger()) {
                sb.append("Triggered by absolute state of ").append(projectionContext.getHumanReadableName());
                sb.append(": ");
                sb.append(projectionContext.getSynchronizationSituationDetected());
                sb.append(" -> ");
                sb.append(projectionContext.getSynchronizationSituationResolved());
                sb.append("\n");
            }
        }

        // focus primary
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext != null) {
            ObjectDelta<F> focusPrimaryDelta = focusContext.getPrimaryDelta();
            if (focusPrimaryDelta != null) {
                sb.append("Triggered by focus primary delta\n");
                DebugUtil.indentDebugDump(sb, 1);
                sb.append(focusPrimaryDelta);
                sb.append("\n");
            }
        }

        // projection primary
        Collection<ObjectDelta<ShadowType>> projPrimaryDeltas = new ArrayList<>();
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            ObjectDelta<ShadowType> projPrimaryDelta = projectionContext.getPrimaryDelta();
            if (projPrimaryDelta != null) {
                projPrimaryDeltas.add(projPrimaryDelta);
            }
        }
        if (!projPrimaryDeltas.isEmpty()) {
            sb.append("Triggered by projection primary delta\n");
            for (ObjectDelta<ShadowType> projDelta: projPrimaryDeltas) {
                DebugUtil.indentDebugDump(sb, 1);
                sb.append(projDelta.toString());
                sb.append("\n");
            }
        }

        if (focusContext != null) {
            sb.append("Focus: ").append(focusContext.getHumanReadableName()).append("\n");
        }
        if (!context.getProjectionContexts().isEmpty()) {
            sb.append("Projections (").append(context.getProjectionContexts().size()).append("):\n");
            for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
                DebugUtil.indentDebugDump(sb, 1);
                sb.append(projectionContext.getHumanReadableName());
                if (projectionContext.isGone()) {
                    sb.append(" GONE");
                }
                sb.append(": ");
                sb.append(projectionContext.getSynchronizationPolicyDecision());
                sb.append("\n");
            }
        }

        if (executedDeltas.isEmpty()) {
            sb.append("Executed: nothing\n");
        } else {
            sb.append("Executed:\n");
            for (ObjectDeltaOperation<? extends ObjectType> executedDelta: executedDeltas) {
                ObjectDelta<? extends ObjectType> delta = executedDelta.getObjectDelta();
                OperationResult deltaResult = executedDelta.getExecutionResult();
                DebugUtil.indentDebugDump(sb, 1);
                sb.append(delta.toString());
                sb.append(": ");
                sb.append(deltaResult != null ? deltaResult.getStatus() : null);
                sb.append("\n");
            }
        }

        LOGGER.debug("\n###[ CLOCKWORK SUMMARY ]######################################\n{}" +
                "##############################################################", sb);
    }

    private void switchState(ModelState newState) {
        beans.medic.clockworkStateSwitch(context, newState);
        context.setState(newState);
    }
}
