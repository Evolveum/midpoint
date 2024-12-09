/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.CLOCKWORK;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.EXITING;
import static com.evolveum.midpoint.model.api.context.ModelState.FINAL;
import static com.evolveum.midpoint.model.impl.lens.LensUtil.getExportTypeTraceOrReduced;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AssociationSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.FocusConstraintsChecker;
import com.evolveum.midpoint.model.impl.simulation.FullOperationSimulationDataImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.cache.CacheType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultBuilder;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The "clockwork" that drives the change processing. The main entry is {@link #run(LensContext, Task, OperationResult)} method.
 *
 * As a special responsibility, this class ensures the conflict resolution with the help of {@link ClockworkConflictResolver}.
 *
 * @author semancik
 */
@Component
public class Clockwork {

    private static final Trace LOGGER = TraceManager.getTrace(Clockwork.class);

    private static final String OP_RUN = Clockwork.class.getName() + ".run";
    private static final String OP_RUN_WITH_CONFLICT_DETECTION = Clockwork.class.getName() + ".runWithConflictDetection";
    private static final String OP_WRITE_SIMULATION_DATA = Clockwork.class.getName() + ".writeSimulationData";

    @Autowired private ProvisioningService provisioningService;
    @Autowired private EventDispatcher eventDispatcher;
    @Autowired private Tracer tracer;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private OperationExecutionRecorderForClockwork operationExecutionRecorder;
    @Autowired private ClockworkConflictResolver clockworkConflictResolver;
    @Autowired private ModelBeans beans;

    public <F extends ObjectType> HookOperationMode run(LensContext<F> context, Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResultBuilder builder = parentResult.subresult(OP_RUN);
        boolean tracingRequested = startTracingIfRequested(context, task, builder, parentResult);
        OperationResult result = builder.build();

        // There are some parts of processing (e.g. notifications deep in provisioning module) that have no access
        // to context.channel value, only to task.channel. So we have to get the two into sync. To return to the original
        // state, we restore task.channel afterwards.
        String originalTaskChannel = task.getChannel();
        task.setChannel(context.getChannel());

        ClockworkRunTraceType trace = null;
        try {
            trace = recordTraceAtStart(context, result);

            ClockworkConflictResolver.Context conflictResolutionContext = new ClockworkConflictResolver.Context();

            HookOperationMode mode = runWithConflictDetection(context, conflictResolutionContext, task, result);

            return clockworkConflictResolver.resolveFocusConflictIfPresent(context, conflictResolutionContext, mode, task, result);

        } catch (ClockworkAbortedException e) {
            // Actually, this is not a problem. We simply record the exception into the operation result and that's all.
            result.recordException(e);
            return HookOperationMode.FOREGROUND;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            task.setChannel(originalTaskChannel);
            result.close();

            recordTraceAtEnd(context, trace, result);

            if (tracingRequested) {
                tracer.storeTrace(task, result, parentResult);
            }
        }
    }

    /**
     * Runs the clockwork with the aim of detecting modify-modify conflicts on the focus object.
     * It reports such states via the conflictResolutionContext parameter.
     */
    <F extends ObjectType> HookOperationMode runWithConflictDetection(LensContext<F> context,
            ClockworkConflictResolver.Context conflictResolutionContext, Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.createSubresult(OP_RUN_WITH_CONFLICT_DETECTION);
        try {
            context.setStartedIfNotYet();
            context.updateSystemConfiguration(result);

            LOGGER.trace("Running clockwork for context {}", context);
            context.checkConsistenceIfNeeded();

            context.resetClickCounter();

            context.reportProgress(new ProgressInformation(CLOCKWORK, ENTERING));
            clockworkConflictResolver.createConflictWatcherOnStart(context);
            enterCaches();

            executeInitialChecks(context);

            try {
                while (context.getState() != FINAL) {

                    context.increaseClickCounter();
                    HookOperationMode mode = click(context, task, result);

                    if (mode == HookOperationMode.BACKGROUND) {
                        result.setInProgress();
                        return mode;
                    } else if (mode == HookOperationMode.ERROR) {
                        return mode;
                    }

                    if (ModelExecuteOptions.isFirstClickOnly(context.getOptions())) {
                        // Assuming that the first click was in the INITIAL state.
                        LOGGER.trace("Initial state only processing requested, exiting the clockwork");
                        return mode;
                    }
                }

                // One last click in FINAL state (unless limited to INITIAL only)
                HookOperationMode mode = click(context, task, result);
                if (mode == HookOperationMode.FOREGROUND) {
                    // We must check inside here - before watchers are unregistered
                    clockworkConflictResolver.detectFocusConflicts(context, conflictResolutionContext, result);
                }
                return mode;

            } catch (ConflictDetectedException e) {
                LOGGER.debug("Clockwork conflict detected", e);
                conflictResolutionContext.recordConflictException();
                return HookOperationMode.FOREGROUND;
            }
        } finally {
            operationExecutionRecorder.recordOperationExecutions(context, task, result);
            clockworkConflictResolver.unregisterConflictWatcher(context);
            exitCaches();
            context.reportProgress(new ProgressInformation(CLOCKWORK, EXITING));

            result.close();
            writeFullSimulationData(context, task, result, parentResult);
        }
    }

    private void writeFullSimulationData(
            LensContext<?> context, Task task, OperationResult resultToRecord, OperationResult parentResult) {
        assert resultToRecord.isClosed();
        SimulationTransaction transactionContext = task.getSimulationTransaction();
        if (!task.isExecutionFullyPersistent() && transactionContext != null) {
            OperationResult result = parentResult.createMinorSubresult(OP_WRITE_SIMULATION_DATA);
            try {
                transactionContext.writeSimulationData(
                        FullOperationSimulationDataImpl.with(context, resultToRecord),
                        task, result);
            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                result.close();
            }
        }
    }

    private void enterCaches() {
        FocusConstraintsChecker.enterCache(
                cacheConfigurationManager.getConfiguration(CacheType.LOCAL_FOCUS_CONSTRAINT_CHECKER_CACHE));
        enterAssociationSearchExpressionEvaluatorCache();
        //enterDefaultSearchExpressionEvaluatorCache();
        provisioningService.enterConstraintsCheckerCache();
    }

    private void exitCaches() {
        FocusConstraintsChecker.exitCache();
        //exitDefaultSearchExpressionEvaluatorCache();
        exitAssociationSearchExpressionEvaluatorCache();
        provisioningService.exitConstraintsCheckerCache();
    }

    private <F extends ObjectType> void executeInitialChecks(LensContext<F> context) throws PolicyViolationException {
        if (context.hasFocusOfType(AssignmentHolderType.class)) {
            checkArchetypeRefDelta(context);
        }
    }

    /**
     * Checking primary delta w.r.t. `archetypeRef` modifications. Their modification is forbidden. And when adding an
     * object, they must be consistent with the assignments.
     *
     * Note that this check is currently limited:
     *
     * 1. we do not support assignment target references without explicit OIDs;
     * 2. we ignore things like real assignment evaluation (e.g. conditions).
     *
     * This check was originally present in AssignmentHolderProcessor. But it refers to focus primary delta only;
     * so it's sufficient to execute it on clockwork entry (unless primary delta is manipulated e.g. in a scripting hook).
     */
    private <F extends ObjectType> void checkArchetypeRefDelta(LensContext<F> context) throws PolicyViolationException {
        ObjectDelta<F> primaryDelta = context.getFocusContext().getPrimaryDelta();
        if (primaryDelta != null) {
            if (primaryDelta.isAdd()) {
                checkArchetypeRefsOnObjectAdd(primaryDelta.getObjectToAdd().asObjectable());
            } else if (primaryDelta.isModify()) {
                checkArchetypeRefsOnObjectModify(primaryDelta);
            }
        }
    }

    /**
     * We allow explicit archetypeRef on objects being added. But only if there is also the corresponding archetype assignment.
     */
    private void checkArchetypeRefsOnObjectAdd(ObjectType objectRaw)
            throws PolicyViolationException {
        stateCheck(objectRaw instanceof AssignmentHolderType,
                "Adding archetypeRef to object other than assignment holder? %s", objectRaw);
        AssignmentHolderType object = (AssignmentHolderType) objectRaw;

        Set<String> oidsFromArchetypeRefs = object.getArchetypeRef().stream()
                .map(ObjectReferenceType::getOid)
                .collect(Collectors.toSet());
        Set<String> oidsFromAssignments = LensUtil.determineExplicitArchetypeOidsFromAssignments(object);
        Set<String> dangling = Sets.difference(oidsFromArchetypeRefs, oidsFromAssignments);
        if (!dangling.isEmpty()) {
            throw new PolicyViolationException("Attempt to add archetypeRef without a matching assignment");
        }
    }

    /**
     * We do not allow modifying archetypeRef directly.
     */
    private <F extends ObjectType> void checkArchetypeRefsOnObjectModify(ObjectDelta<F> primaryDelta)
            throws PolicyViolationException {
        ReferenceDelta archetypeRefDelta = primaryDelta.findReferenceModification(AssignmentHolderType.F_ARCHETYPE_REF);
        if (archetypeRefDelta != null) {
            throw new PolicyViolationException("Attempt to modify archetypeRef directly");
        }
    }

    /**
     * Sets the tracing profile in the operation result, effectively requesting tracing for this operation (if needed).
     * Returns `true` if the profile was set here, so the caller can write the trace down.
     *
     * Note that we request the trace even if a tracing from the upper layers is already in place. For example, if there is
     * clockwork-inside-clockwork processing, it is convenient to have two traces, even if the second one is contained (also)
     * within the first one.
     */
    private <F extends ObjectType> boolean startTracingIfRequested(
            LensContext<F> context, Task task, OperationResultBuilder builder, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        TracingProfileType tracingProfile = ModelExecuteOptions.getTracingProfile(context.getOptions());
        if (tracingProfile != null) {
            securityEnforcer.authorize(ModelAuthorizationAction.RECORD_TRACE.getUrl(), task, parentResult);
            builder.tracingProfile(
                    tracer.compileProfile(tracingProfile, parentResult));
            return true;
        } else if (task.getTracingRequestedFor().contains(TracingRootType.CLOCKWORK_RUN)) {
            builder.tracingProfile(
                    tracer.compileProfile(
                            task.getTracingProfile(), parentResult));
            return true;
        } else {
            return false;
        }
    }

    private ClockworkRunTraceType recordTraceAtStart(LensContext<?> context, OperationResult result) throws SchemaException {
        if (result.isTracingAny(ClockworkRunTraceType.class)) {
            ClockworkRunTraceType trace = new ClockworkRunTraceType();
            trace.setInputLensContextText(context.debugDump());
            trace.setInputLensContext(context.toBean(getExportTypeTraceOrReduced(trace, result)));
            result.addTrace(trace);
            return trace;
        } else {
            return null;
        }
    }

    private void recordTraceAtEnd(LensContext<?> context, ClockworkRunTraceType trace, OperationResult result)
            throws SchemaException {
        if (trace != null) {
            trace.setOutputLensContextText(context.debugDump());
            trace.setOutputLensContext(context.toBean(getExportTypeTraceOrReduced(trace, result)));
            if (context.getFocusContext() != null) { // todo reconsider this
                PrismObject<?> objectAny = context.getFocusContext().getObjectAny();
                if (objectAny != null) {
                    trace.setFocusName(PolyString.getOrig(objectAny.getName()));
                }
            }
        }
    }

    private void enterAssociationSearchExpressionEvaluatorCache() {
        AssociationSearchExpressionEvaluatorCache cache = AssociationSearchExpressionEvaluatorCache.enterCache(
                cacheConfigurationManager.getConfiguration(CacheType.LOCAL_ASSOCIATION_TARGET_SEARCH_EVALUATOR_CACHE));
        if (cache.getClientContextInformation() == null) {
            AssociationSearchExpressionCacheInvalidator invalidator = new AssociationSearchExpressionCacheInvalidator(cache);
            cache.setClientContextInformation(invalidator);
            eventDispatcher.registerListener((ResourceObjectChangeListener) invalidator);
            eventDispatcher.registerListener((ResourceOperationListener) invalidator);
        }
    }

    private void exitAssociationSearchExpressionEvaluatorCache() {
        AssociationSearchExpressionEvaluatorCache cache = AssociationSearchExpressionEvaluatorCache.exitCache();
        if (cache == null) {
            LOGGER.error("exitAssociationSearchExpressionEvaluatorCache: cache instance was not found for the current thread");
            return;
        }
        if (cache.getEntryCount() <= 0) {
            Object invalidator = cache.getClientContextInformation();
            if (!(invalidator instanceof AssociationSearchExpressionCacheInvalidator)) {
                LOGGER.error("exitAssociationSearchExpressionEvaluatorCache: expected {}, got {} instead",
                        AssociationSearchExpressionCacheInvalidator.class, invalidator);
                return;
            }
            eventDispatcher.unregisterListener((ResourceObjectChangeListener) invalidator);
            eventDispatcher.unregisterListener((ResourceOperationListener) invalidator);
        }
    }

    @SuppressWarnings("unused")
    private void enterDefaultSearchExpressionEvaluatorCache() {
        //DefaultSearchExpressionEvaluatorCache.enterCache(cacheConfigurationManager.getConfiguration(CacheType.LOCAL_DEFAULT_SEARCH_EVALUATOR_CACHE));
    }

    @SuppressWarnings("unused")
    private void exitDefaultSearchExpressionEvaluatorCache() {
        //DefaultSearchExpressionEvaluatorCache.exitCache();
    }

    @VisibleForTesting
    public <F extends ObjectType> @NotNull HookOperationMode click(
            @NotNull LensContext<F> context,
            @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConflictDetectedException, ConfigurationException, ObjectNotFoundException, PolicyViolationException,
            ObjectAlreadyExistsException {
        return new ClockworkClick<>(context, beans, task)
                .click(result);
    }
}
