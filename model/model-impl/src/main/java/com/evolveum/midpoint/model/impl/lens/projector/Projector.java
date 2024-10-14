/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.PROJECTOR;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;
import static com.evolveum.midpoint.model.impl.lens.LensUtil.getExportType;

import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.LensContext.AuthorizationState;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.ProjectionCredentialsProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentHolderProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ObjectTemplateProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectorRunTraceType;

/**
 * Projector recomputes the context. It takes the context with a few basic data as input. It uses all the policies
 * and mappings to derive all the other data. E.g. a context with only a user (primary) delta. It applies user template,
 * outbound mappings and the inbound mappings and then inbound and outbound mappings of other accounts and so on until
 * all the data are computed. The output is the original context with all the computed delta.
 *
 * Primary deltas are in the input, secondary deltas are computed in projector. Projector "projects" primary deltas to
 * the secondary deltas of user and accounts.
 *
 * Projector does NOT execute the deltas. It only recomputes the context. It may read a lot of objects (user, accounts, policies).
 * But it does not change any of them.
 *
 * @author Radovan Semancik
 *
 */
@Component
public class Projector {

    private static final String OPERATION_PROJECT_PROJECTION = Projector.class.getName() + ".projectProjection";

    @Autowired private ContextLoader contextLoader;
    @Autowired private AssignmentHolderProcessor assignmentHolderProcessor;
    @Autowired private ProjectionValuesProcessor projectionValuesProcessor;
    @Autowired private ReconciliationProcessor reconciliationProcessor;
    @Autowired private ProjectionCredentialsProcessor projectionCredentialsProcessor;
    @Autowired private ActivationProcessor activationProcessor;
    @Autowired private PolicyRuleProcessor policyRuleProcessor;
    @Autowired private DependencyProcessor dependencyProcessor;
    @Autowired private ObjectTemplateProcessor objectTemplateProcessor;
    @Autowired private Clock clock;
    @Autowired private ClockworkMedic medic;

    private static final Trace LOGGER = TraceManager.getTrace(Projector.class);

    /**
     * Runs one projection wave, starting at current execution wave.
     */
    public <F extends ObjectType> void project(@NotNull LensContext<F> context, @NotNull String activityDescription,
            @NotNull Task task, @NotNull OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException,
            ConflictDetectedException {
        context.setStartedIfNotYet(); // This is for cases where we start the projector "from the outside" (e.g. in tests)
        context.normalize();
        context.resetProjectionWave();
        projectInternal(context, activityDescription, true, task, parentResult);
    }

    /**
     * Resumes projection at current projection wave.
     */
    public <F extends ObjectType> void resume(LensContext<F> context, String activityDescription, Task task,
            OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException,
            ConflictDetectedException {
        assert context.getProjectionWave() == context.getExecutionWave();
        assert context.isFresh();
        projectInternal(context, activityDescription, false, task, parentResult);
    }

    private <F extends ObjectType> void projectInternal(
            @NotNull LensContext<F> context,
            @NotNull String activityDescription,
            boolean fromStart,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException,
            ConflictDetectedException {

        context.checkAbortRequested();
        context.inspectProjectorStart();

        InternalMonitor.recordCount(InternalCounters.PROJECTOR_RUN_COUNT);

        // Read the time at the beginning so all processors have the same notion of "now"
        // this provides nicer unified timestamp that can be used in equality checks in tests and also for
        // troubleshooting
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        String traceTitle = fromStart ? "projector start" : "projector resume";
        medic.traceContext(LOGGER, activityDescription, traceTitle, false, context, false);

        context.checkConsistenceIfNeeded();

        OperationResult result = parentResult.subresult(Projector.class.getName() + ".project")
                .addQualifier(context.getOperationQualifier())
                .addParam("fromStart", fromStart)
                .addContext("projectionWave", context.getProjectionWave())
                .build();
        ProjectorRunTraceType trace;
        if (result.isTracingAny(ProjectorRunTraceType.class)) {
            trace = new ProjectorRunTraceType();
            trace.setInputLensContext(context.toBean(getExportType(trace, result)));
            trace.setInputLensContextText(context.debugDump());
            result.addTrace(trace);
        } else {
            trace = null;
        }

        PartialProcessingOptionsType partialProcessingOptions = context.getPartialProcessingOptions();

        // Projector is using a set of "processors" to do parts of its work. The processors will be called in sequence
        // in the following code.

        try {

            context.reportProgress(new ProgressInformation(PROJECTOR, ENTERING));

            if (fromStart) {
                medic.partialExecute(Components.LOAD, contextLoader, contextLoader::load,
                        partialProcessingOptions::getLoad,
                        Projector.class, context, activityDescription, now, task, result);
            } else {
                LOGGER.trace("Not loading the context, as 'fromStart' is false");
            }

            if (context.getAuthorizationState() == AuthorizationState.NONE) {
                // We need the context to be fully loaded before the authorization is done, hence we do the authorization
                // after the loading. But we still evaluate it under "full information may not be available" mode,
                // as parentOrgRef, tenantRef, and roleMembershipRef values may be missing here.
                ClockworkRequestAuthorizer.authorizeContextRequest(context, false, task, result);
            }

            LOGGER.trace("WAVE {} (executionWave={})", context.getProjectionWave(), context.getExecutionWave());

            //just make sure everything is loaded and set as needed
            dependencyProcessor.preprocessDependencies(context);

            // Process the focus-related aspects of the context. That means inbound, focus activation,
            // object template and assignments.

            medic.partialExecute(Components.FOCUS, assignmentHolderProcessor,
                    assignmentHolderProcessor::processFocus,
                    partialProcessingOptions::getFocus,
                    Projector.class, context, activityDescription, now, task, result);

            if (partialProcessingOptions.getProjection() != PartialProcessingTypeType.SKIP) {
                // Process activation for all eligible projections: such that their wave is either not determined
                // or such that their wave is current. The first case is needed to properly sort projections to waves
                // as deprovisioning will reverse the dependencies. And we know whether a projection is provisioned or
                // deprovisioned only after the activation is processed.
                activationProcessor.processProjectionsActivation(context, activityDescription, now, task, result);

                dependencyProcessor.sortProjectionsToWaves(context, task, result);

                // In the future we may want the ability to select only some projections to process.
                for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
                    medic.partialExecute(Components.PROJECTION,
                            (result1) -> projectProjection(context, projectionContext,
                                    partialProcessingOptions, now, activityDescription, task, result1),
                            partialProcessingOptions::getProjection,
                            Projector.class, context, projectionContext, result);
                }

                // If there exists some conflicting projection contexts, add them to the context so they will be recomputed
                // in the next wave.
                addConflictingContexts(context);
            }

            context.checkConsistenceIfNeeded();

            medic.partialExecute(
                    Components.OBJECT_TEMPLATE_AFTER_PROJECTIONS, objectTemplateProcessor,
                    objectTemplateProcessor::processTemplateAfterProjections,
                    partialProcessingOptions::getObjectTemplateAfterAssignments,
                    Projector.class, context, now, task, result);

            context.incrementProjectionWave();

            dependencyProcessor.checkDependenciesFinal(context);
            context.checkConsistenceIfNeeded();

            computeResultStatus(now, result);

        } catch (SchemaException | PolicyViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException |
                ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException |
                ConflictDetectedException e) {
            recordFatalError(e, now, result);
            throw e;
        } catch (RuntimeException e) {
            recordFatalError(e, now, result);
            // This should not normally happen unless there is something really bad or there is a bug.
            // Make sure that it is logged.
            LOGGER.error("Runtime error in projector: {}", e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
            if (trace != null) {
                trace.setOutputLensContextText(context.debugDump());
                trace.setOutputLensContext(context.toBean(getExportType(trace, result)));
            }
            context.inspectProjectorFinish();
            context.reportProgress(new ProgressInformation(PROJECTOR, result));
        }
    }

    private <F extends ObjectType> void projectProjection(
            LensContext<F> context, LensProjectionContext projectionContext,
            PartialProcessingOptionsType partialProcessingOptions,
            XMLGregorianCalendar now, String activityDescription, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {

        String projectionDesc = projectionContext.getDescription();
        parentResult.addParam(OperationResult.PARAM_PROJECTION, projectionDesc); // a bit of hack -> to have projection info also on the root "component" operation result
        parentResult.addParam("resourceName", projectionContext.getResourceName());

        if (projectionContext.getWave() != context.getProjectionWave()) {
            recordSkipReason(parentResult,
                    "Skipping projection because the wave of the projection context (" + projectionContext.getWave() +
                            ") differs from current projector wave (" + context.getProjectionWave() + ")");
            return;
        }

        if (!projectionContext.hasResource()) {
            // There's probably no point in computing projections that have no resource
            recordSkipReason(parentResult,
                    "Skipping projection because the resource for the projection context is not known");
            return;
        }

        if (!projectionContext.isVisible()) {
            recordSkipReason(parentResult,
                    "Skipping projection because the resource or object definition is not visible in current task"
                            + " execution mode");
            return;
        }

        if (projectionContext.isCompleted()) {
            recordSkipReason(parentResult, "Skipping projection because the projection context is already completed");
            return;
        }

        OperationResult result = parentResult.createMinorSubresult(OPERATION_PROJECT_PROJECTION);
        result.addParam(OperationResult.PARAM_PROJECTION, projectionDesc);

        try {

            context.checkAbortRequested();

            if (!projectionContext.isCanProject()) {
                recordSkipReason(result, "Skipping projection because of limited propagation");
                return;
            }

            if (projectionContext.isOutboundSyncDisabled(result)) {
                recordSkipReason(result, "Skipping projection because it is has outbound sync disabled.");
                return;
            }

            // Here we skip complete processing of shadow (no outbounds)
            if (projectionContext.isMarkedReadOnly(result)) {
                recordSkipReason(result, "Skipping projection because it is marked read-only.");
                return;
            }

            if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN ||
                    projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
                recordSkipReason(result,
                        "Skipping projection because it is " + projectionContext.getSynchronizationPolicyDecision());
                return;
            }

            if (projectionContext.isGone()) {
                recordSkipReason(result, "Skipping projection because it is gone");
                return;
            }

            if (projectionContext.isReaping()) {
                recordSkipReason(result, "Skipping projection because it is being reaped");
                return;
            }

            LOGGER.trace("WAVE {} PROJECTION {}", context.getProjectionWave(), projectionDesc);

            // Some projections may not be loaded at this point, e.g. high-order dependency projections
            contextLoader.updateProjectionContext(context, projectionContext, task, result);

            context.checkConsistenceIfNeeded();

            if (!dependencyProcessor.checkDependencies(projectionContext)) {
                recordSkipReason(result, "Skipping projection because it has unsatisfied dependencies");
                return;
            }

            // TODO: decide if we need to continue

            // This is a "composite" processor. it contains several more processor invocations inside
            medic.partialExecute(
                    Components.PROJECTION_VALUES, projectionValuesProcessor,
                    projectionValuesProcessor::process,
                    partialProcessingOptions::getProjectionValues,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            if (projectionContext.isGone()) {
                recordSkipReason(result, "Skipping projection because it is gone");
                return;
            }

            medic.partialExecute(
                    Components.PROJECTION_CREDENTIALS, projectionCredentialsProcessor,
                    projectionCredentialsProcessor::processProjectionCredentials,
                    partialProcessingOptions::getProjectionCredentials,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            medic.partialExecute(
                    Components.PROJECTION_RECONCILIATION, reconciliationProcessor,
                    reconciliationProcessor::processReconciliation,
                    partialProcessingOptions::getProjectionReconciliation,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            medic.partialExecute(
                    Components.PROJECTION_VALUES_POST_RECON, projectionValuesProcessor,
                    projectionValuesProcessor::processPostRecon,
                    partialProcessingOptions::getProjectionValues,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            medic.partialExecute(
                    Components.PROJECTION_LIFECYCLE, activationProcessor,
                    activationProcessor::processLifecycle,
                    partialProcessingOptions::getProjectionLifecycle,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            medic.partialExecute(
                    Components.PROJECTION_POLICY_RULES, null,
                    policyRuleProcessor::evaluateProjectionPolicyRules,
                    partialProcessingOptions::getProjectionPolicyRules,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            result.recordSuccess();
        } catch (ConflictDetectedException e) {

            // This should not occur when projecting a projection! (The exception is related to modification of a focal object.)
            throw new SystemException("Unexpected conflict detected exception: " + e.getMessage(), e);

        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | SecurityViolationException
                | PolicyViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException | RuntimeException | Error e) {

            projectionContext.setBroken();
            ModelImplUtils.handleConnectorErrorCriticality(projectionContext.getResource(), e, result);
        }
    }

    private static void recordSkipReason(OperationResult result, String message) {
        LOGGER.trace("{}", message);
        result.recordStatus(OperationResultStatus.NOT_APPLICABLE, message);
    }

    private <F extends ObjectType> void addConflictingContexts(LensContext<F> context) {
        for (LensProjectionContext conflictingContext : context.getConflictingProjectionContexts()) {
            LOGGER.trace("Adding conflicting projection context {}", conflictingContext.getHumanReadableName());
            context.addProjectionContext(conflictingContext);
        }
        context.clearConflictingProjectionContexts();
    }

    private void recordFatalError(Exception e, XMLGregorianCalendar projectorStartTimestampCal, OperationResult result) {
        result.recordFatalError(e);
        result.cleanupResult(e);
        if (LOGGER.isDebugEnabled()) {
            long projectorStartTimestamp = XmlTypeConverter.toMillis(projectorStartTimestampCal);
            long projectorEndTimestamp = clock.currentTimeMillis();
            LOGGER.debug("Projector failed: {}. Etime: {} ms", e.getMessage(), (projectorEndTimestamp - projectorStartTimestamp));
        }
    }

    private void computeResultStatus(XMLGregorianCalendar projectorStartTimestampCal, OperationResult result) {
        boolean hasProjectionError = false;
        OperationResultStatus finalStatus = OperationResultStatus.SUCCESS;
        String message = null;
        LocalizableMessage userFriendlyMessage = null;
        for (OperationResult subresult: result.getSubresults()) {
            if (subresult.isNotApplicable() || subresult.isSuccess()) {
                continue;
            }
            if (subresult.isHandledError()) {
                if (finalStatus == OperationResultStatus.SUCCESS) {
                    finalStatus = OperationResultStatus.HANDLED_ERROR;
                }
                continue;
            }
            if (subresult.isError()) {
                message = subresult.getMessage();
                userFriendlyMessage = subresult.getUserFriendlyMessage();
                if (OPERATION_PROJECT_PROJECTION.equals(subresult.getOperation())) {
                    hasProjectionError = true;
                } else {
                    if (finalStatus != OperationResultStatus.FATAL_ERROR) {
                        finalStatus = subresult.getStatus();
                    }
                }
            }
        }
        if (finalStatus != OperationResultStatus.FATAL_ERROR && hasProjectionError) {
            finalStatus = OperationResultStatus.PARTIAL_ERROR;
        }
        result.setStatus(finalStatus);
        result.setMessage(message);
        result.setUserFriendlyMessage(userFriendlyMessage);
        result.cleanupResult();
        if (LOGGER.isDebugEnabled()) {
            long projectorStartTimestamp = XmlTypeConverter.toMillis(projectorStartTimestampCal);
            long projectorEndTimestamp = clock.currentTimeMillis();
            LOGGER.trace("Projector finished ({}). Etime: {} ms", result.getStatus(), (projectorEndTimestamp - projectorStartTimestamp));
        }
    }
}
