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

import com.evolveum.midpoint.model.impl.lens.projector.focus.ObjectTemplateProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.ProjectionCredentialsProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentHolderProcessor;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
    @Autowired private DependencyProcessor dependencyProcessor;
    @Autowired private ObjectTemplateProcessor objectTemplateProcessor;
    @Autowired private Clock clock;
    @Autowired private ClockworkMedic medic;

    private static final Trace LOGGER = TraceManager.getTrace(Projector.class);

    /**
     * Runs one projection wave, starting at current execution wave.
     */
    public <F extends ObjectType> void project(LensContext<F> context, String activityDescription, Task task,
            OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {
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
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {
        assert context.getProjectionWave() == context.getExecutionWave();
        assert context.isFresh();
        projectInternal(context, activityDescription, false, task, parentResult);
    }

    /**
     * Executes projector from current execution wave to the last computed wave.
     * Useful for change preview.
     */
    public <F extends ObjectType> void projectAllWaves(LensContext<F> context, String activityDescription,
            Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {
        assert context.getProjectionWave() == 0;
        assert context.getExecutionWave() == 0;
        context.normalize();
        while (context.getProjectionWave() < context.computeMaxWaves()) {
            boolean fromStart = context.getProjectionWave() == 0;
            projectInternal(context, activityDescription, fromStart, task, parentResult);
        }
    }

    private <F extends ObjectType> void projectInternal(LensContext<F> context, String activityDescription,
            boolean fromStart, Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {

        context.checkAbortRequested();

        if (context.getInspector() != null) {
            context.getInspector().projectorStart(context);
        }

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
        if (result.isTraced()) {
            trace = new ProjectorRunTraceType();
            trace.setInputLensContext(context.toLensContextType(getExportType(trace, result)));
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
            }

            //consolidationProcessor.consolidateFocusPrimaryDelta(context, now, task, result);

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
                // Process activation of all resources, regardless of the waves. This is needed to properly
                // sort projections to waves as deprovisioning will reverse the dependencies. And we know whether
                // a projection is provisioned or deprovisioned only after the activation is processed.
                if (fromStart) {
                    activationProcessor.processActivationForAllResources(context, activityDescription, now, task, result);
                }

                dependencyProcessor.sortProjectionsToWaves(context, result);

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

            medic.partialExecute(Components.OBJECT_TEMPLATE_AFTER_PROJECTIONS, objectTemplateProcessor,
                    objectTemplateProcessor::processTemplateAfterProjections,
                    partialProcessingOptions::getObjectTemplateAfterAssignments,
                    Projector.class, context, now, task, result);

            context.incrementProjectionWave();

            dependencyProcessor.checkDependenciesFinal(context, result);
            context.checkConsistenceIfNeeded();

            computeResultStatus(now, result);

        } catch (SchemaException | PolicyViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException |
                ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException | PreconditionViolationException e) {
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
                trace.setOutputLensContext(context.toLensContextType(getExportType(trace, result)));
            }
            if (context.getInspector() != null) {
                context.getInspector().projectorFinish(context);
            }
            context.reportProgress(new ProgressInformation(PROJECTOR, result));
        }
    }

    private <F extends ObjectType> void projectProjection(LensContext<F> context, LensProjectionContext projectionContext,
            PartialProcessingOptionsType partialProcessingOptions,
            XMLGregorianCalendar now, String activityDescription, Task task, OperationResult parentResult)
                    throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
                    SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException {

        String projectionDesc = projectionContext.getDescription();
        parentResult.addParam(OperationResult.PARAM_PROJECTION, projectionDesc); // a bit of hack -> to have projection info also on the root "component" operation result
        parentResult.addParam("resourceName", projectionContext.getResourceName());

        if (projectionContext.getWave() != context.getProjectionWave()) {
            LOGGER.trace("Skipping projection of {} because its wave ({}) is different from current projection wave ({})",
                    projectionContext, projectionContext.getWave(), context.getProjectionWave());
            parentResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Wave of the projection context differs from current projector wave");
            return;
        }

        if (projectionContext.isCompleted()) {
            LOGGER.trace("Skipping projection of {} because it's already completed", projectionContext);
            parentResult.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Projection context is already completed");
            return;
        }

        OperationResult result = parentResult.createMinorSubresult(OPERATION_PROJECT_PROJECTION);
        result.addParam(OperationResult.PARAM_PROJECTION, projectionDesc);

        try {

            context.checkAbortRequested();

            if (!projectionContext.isCanProject()) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipping projection because of limited propagation");
                return;
            }

            if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN ||
                    projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipping projection because it is "+projectionContext.getSynchronizationPolicyDecision());
                return;
            }

            if (projectionContext.isTombstone()) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipping projection because it is a tombstone");
                return;
            }

            LOGGER.trace("WAVE {} PROJECTION {}", context.getProjectionWave(), projectionDesc);

            // Some projections may not be loaded at this point, e.g. high-order dependency projections
            contextLoader.makeSureProjectionIsLoaded(context, projectionContext, task, result);

            context.checkConsistenceIfNeeded();

            if (!dependencyProcessor.checkDependencies(context, projectionContext, result)) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipping projection because it has unsatisfied dependencies");
                return;
            }

            // TODO: decide if we need to continue

            // This is a "composite" processor. it contains several more processor invocations inside
            medic.partialExecute(Components.PROJECTION_VALUES, projectionValuesProcessor,
                    projectionValuesProcessor::process,
                    partialProcessingOptions::getProjectionValues,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            if (projectionContext.isTombstone()) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipping projection because it is a tombstone");
                return;
            }

            medic.partialExecute(Components.PROJECTION_CREDENTIALS, projectionCredentialsProcessor,
                    projectionCredentialsProcessor::processProjectionCredentials,
                    partialProcessingOptions::getProjectionCredentials,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            medic.partialExecute(Components.PROJECTION_RECONCILIATION, reconciliationProcessor,
                    reconciliationProcessor::processReconciliation,
                    partialProcessingOptions::getProjectionReconciliation,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            medic.partialExecute(Components.PROJECTION_VALUES_POST_RECON, projectionValuesProcessor,
                    projectionValuesProcessor::processPostRecon,
                    partialProcessingOptions::getProjectionValues,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            medic.partialExecute(Components.PROJECTION_LIFECYCLE, activationProcessor,
                    activationProcessor::processLifecycle,
                    partialProcessingOptions::getProjectionLifecycle,
                    Projector.class, context, projectionContext, activityDescription, now, task, result);

            result.recordSuccess();

        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | SecurityViolationException
                | PolicyViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException | RuntimeException | Error e) {

            projectionContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
            ModelImplUtils.handleConnectorErrorCriticality(projectionContext.getResource(), e, result);
        }
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
        result.cleanupResult();
        if (LOGGER.isDebugEnabled()) {
            long projectorStartTimestamp = XmlTypeConverter.toMillis(projectorStartTimestampCal);
            long projectorEndTimestamp = clock.currentTimeMillis();
            LOGGER.trace("Projector finished ({}). Etime: {} ms", result.getStatus(), (projectorEndTimestamp - projectorStartTimestamp));
        }
    }
}
