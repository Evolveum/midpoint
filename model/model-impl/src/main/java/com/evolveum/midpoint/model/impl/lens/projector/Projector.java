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
import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectorRunTraceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.ProjectionCredentialsProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentHolderProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentProcessor;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;

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
    @Autowired private AssignmentHolderProcessor focusProcessor;
    @Autowired private AssignmentProcessor assignmentProcessor;
    @Autowired private ProjectionValuesProcessor projectionValuesProcessor;
    @Autowired private ReconciliationProcessor reconciliationProcessor;
    @Autowired private ProjectionCredentialsProcessor projectionCredentialsProcessor;
    @Autowired private ActivationProcessor activationProcessor;
    @Autowired private DependencyProcessor dependencyProcessor;
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
        projectInternal(context, activityDescription, true, false, task, parentResult);
    }

    /**
     * Resumes projection at current projection wave.
     */
    public <F extends ObjectType> void resume(LensContext<F> context, String activityDescription,
                                               Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {

        if (context.getProjectionWave() != context.getExecutionWave()) {
            throw new IllegalStateException("Projector.resume called in illegal wave state: execution wave = " + context.getExecutionWave() +
                    ", projection wave = " + context.getProjectionWave());
        }
        if (!context.isFresh()) {
            throw new IllegalStateException("Projector.resume called on non-fresh context");
        }

        projectInternal(context, activityDescription, false, false, task, parentResult);
    }

    /**
     * Executes projector from current execution wave to the last computed wave.
     * Useful for change preview.
     */
    public <F extends ObjectType> void projectAllWaves(LensContext<F> context, String activityDescription,
            Task task, OperationResult parentResult)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, PreconditionViolationException {
        projectInternal(context, activityDescription, true, true, task, parentResult);
    }

    private <F extends ObjectType> void projectInternal(LensContext<F> context, String activityDescription,
            boolean fromStart, boolean allWaves, Task task, OperationResult parentResult)
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

//        setupCounters(now, context.getPrismContext(), task, parentResult);

        if (consistencyChecks) context.checkConsistence();

        if (fromStart) {
            context.normalize();
            context.resetProjectionWave();
        }

        OperationResult result = parentResult.subresult(Projector.class.getName() + ".project")
                .addQualifier(context.getOperationQualifier())
                .addParam("fromStart", fromStart)
                .addContext("projectionWave", context.getProjectionWave())
                .addContext("executionWave", context.getExecutionWave())
                .build();
        ProjectorRunTraceType trace;
        if (result.isTraced()) {
            trace = new ProjectorRunTraceType();
            trace.setInputLensContext(context.toLensContextType(getExportType(trace, result)));
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
                medic.partialExecute("load",
                        (result1) -> {
                            contextLoader.load(context, activityDescription, task, result1);
                            // Set the "fresh" mark now so following consistency check will be stricter
                            context.setFresh(true);
                            if (consistencyChecks) context.checkConsistence();
                        },
                        partialProcessingOptions::getLoad,
                        Projector.class, context, result);
            }
            // For now let's pretend to do just one wave. The maxWaves number will be corrected in the
            // first wave when dependencies are sorted out for the first time.
            int maxWaves = context.getExecutionWave() + 1;

            // Start the waves ....
            LOGGER.trace("WAVE: Starting the waves.");

            boolean firstWave = true;

            while ((allWaves && context.getProjectionWave() < maxWaves) ||
                    (!allWaves && context.getProjectionWave() <= context.getExecutionWave())) {

                boolean inFirstWave = firstWave;
                firstWave = false;                    // in order to not forget to reset it ;)

                context.checkAbortRequested();

                LOGGER.trace("WAVE {} (maxWaves={}, executionWave={})",
                        context.getProjectionWave(), maxWaves, context.getExecutionWave());

                //just make sure everything is loaded and set as needed
                dependencyProcessor.preprocessDependencies(context);

                // Process the focus-related aspects of the context. That means inbound, focus activation,
                // object template and assignments.

                medic.partialExecute("focus",
                        (result1) -> {
                            focusProcessor.processFocus(context, activityDescription, now, task, result1);
                            context.recomputeFocus();
                            if (consistencyChecks) context.checkConsistence();
                        },
                        partialProcessingOptions::getFocus,
                        Projector.class, context, result);

                medic.traceContext(LOGGER, activityDescription, "focus processing", false, context, false);
                LensUtil.checkContextSanity(context, "focus processing", result);

                if (partialProcessingOptions.getProjection() != PartialProcessingTypeType.SKIP) {
                    // Process activation of all resources, regardless of the waves. This is needed to properly
                    // sort projections to waves as deprovisioning will reverse the dependencies. And we know whether
                    // a projection is provisioned or deprovisioned only after the activation is processed.
                    if (fromStart && inFirstWave) {
                        OperationResult activationResult = result.subresult(Projector.class.getName() + ".activation")
                                .setMinor()
                                .build();
                        try {
                            LOGGER.trace("Processing activation for all contexts");
                            for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
                                if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN
                                        || projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.IGNORE) {
                                    continue;
                                }
                                activationProcessor.processActivation(context, projectionContext, now, task, activationResult);
                                projectionContext.recompute();
                            }
                            assignmentProcessor.removeIgnoredContexts(
                                    context);        // TODO move implementation of this method elsewhere; but it has to be invoked here, as activationProcessor sets the IGNORE flag
                        } catch (Throwable t) {
                            activationResult.recordFatalError(t);
                            throw t;
                        } finally {
                            activationResult.computeStatusIfUnknown();
                        }
                    }
                    medic.traceContext(LOGGER, activityDescription, "projection activation of all resources", true, context,
                            true);
                    if (consistencyChecks) {
                        context.checkConsistence();
                    }

                    dependencyProcessor.sortProjectionsToWaves(context);
                    maxWaves = dependencyProcessor.computeMaxWaves(context);
                    LOGGER.trace("Continuing wave {}, maxWaves={}", context.getProjectionWave(), maxWaves);

                    for (LensProjectionContext projectionContext : context.getProjectionContexts()) {

                        medic.partialExecute("projection",
                                (result1) -> projectProjection(context, projectionContext,
                                        partialProcessingOptions, now, activityDescription, task, result1),
                                partialProcessingOptions::getProjection,
                                Projector.class, context, projectionContext, result);
                        // TODO: make this condition more complex in the future. We may want the ability
                        // to select only some projections to process

                    }

                    // if there exists some conflicting projection contexts, add them to the context so they will be recomputed in the next wave..
                    addConflictingContexts(context);
                }

                if (consistencyChecks) context.checkConsistence();

                context.incrementProjectionWave();
            }

            LOGGER.trace("WAVE: Stopping the waves. There was {} waves", context.getProjectionWave());

            // We can do this only when computation of all the waves is finished. Before that we do not know
            // activation of every account and therefore cannot decide what is OK and what is not
            dependencyProcessor.checkDependenciesFinal(context, result);

            if (consistencyChecks) context.checkConsistence();

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
                trace.setOutputLensContext(context.toLensContextType(getExportType(trace, result)));
            }
            if (context.getInspector() != null) {
                context.getInspector().projectorFinish(context);
            }
            context.reportProgress(new ProgressInformation(PROJECTOR, result));
        }

    }
//
//    private synchronized void setupCounters(XMLGregorianCalendar now, PrismContext prismContext, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
//
//        if (task.getOid() == null) {
//            return;
//        }
//
//        TaskType taskType = task.getTaskType();
//        LOGGER.trace("Collecting counters for {}", task);
//
//        AssignmentTripleEvaluator<TaskType> ate = new AssignmentTripleEvaluator<>();
//        ate.setNow(now);
//        ate.setPrismContext(prismContext);
//        ate.setResult(result);
//        ate.setSource(taskType);
//        ate.setTask(task);
//        DeltaSetTriple<EvaluatedAssignmentImpl<TaskType>> evaluatedAssignments = ate.preProcessAssignments(task.getUpdatedOrClonedTaskObject());
//
//        Set<EvaluatedPolicyRule> evaluatedPolicyRules = new HashSet<>();
//
//        for (EvaluatedAssignmentImpl<TaskType> evaluatedAssignment : evaluatedAssignments.union()) {
//            evaluatedPolicyRules.addAll(evaluatedAssignment.getOtherTargetsPolicyRules());
//        }
//        evaluatedPolicyRules.forEach(policyRule -> counterManager.registerCounter(task, policyRule.getPolicyRule()));
//
//    }
//
    private <F extends ObjectType> void projectProjection(LensContext<F> context, LensProjectionContext projectionContext,
            PartialProcessingOptionsType partialProcessingOptions,
            XMLGregorianCalendar now, String activityDescription, Task task, OperationResult parentResult)
                    throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
                    SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException {

        String projectionDesc = getProjectionDesc(projectionContext);
        parentResult.addParam(OperationResult.PARAM_PROJECTION, projectionDesc);        // a bit of hack -> to have projection info also on the root "component" operation result
        parentResult.addParam("resourceName", projectionContext.getResourceName());

        if (projectionContext.getWave() != context.getProjectionWave()) {
            // Let's skip accounts that do not belong into this wave.
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

            if (projectionContext.isReaping()) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipping projection because it is being reaped");
                return;
            }

            LOGGER.trace("WAVE {} PROJECTION {}", context.getProjectionWave(), projectionDesc);

            // Some projections may not be loaded at this point, e.g. high-order dependency projections
            contextLoader.makeSureProjectionIsLoaded(context, projectionContext, task, result);

            if (consistencyChecks) context.checkConsistence();

            if (!dependencyProcessor.checkDependencies(context, projectionContext, result)) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipping projection because it has unsatisfied dependencies");
                return;
            }

            // TODO: decide if we need to continue

            medic.partialExecute("projectionValues",
                    (result1) -> {
                        // This is a "composite" processor. it contains several more processor invocations inside
                        projectionValuesProcessor.process(context, projectionContext, activityDescription, task, result1);
                        if (consistencyChecks) context.checkConsistence();

                        projectionContext.recompute();
                        if (consistencyChecks) context.checkConsistence();
                    },
                    partialProcessingOptions::getProjectionValues,
                    Projector.class, context, projectionContext, result);

            if (projectionContext.isTombstone()) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipping projection because it is a tombstone");
                return;
            }

            medic.partialExecute("projectionCredentials",
                    (result1) -> {
                        projectionCredentialsProcessor.processProjectionCredentials(context, projectionContext, now, task, result1);
                        if (consistencyChecks) context.checkConsistence();

                        projectionContext.recompute();
                        medic.traceContext(LOGGER, activityDescription, "projection values and credentials of "+projectionDesc, false, context, true);
                        if (consistencyChecks) context.checkConsistence();
                    },
                    partialProcessingOptions::getProjectionCredentials,
                    Projector.class, context, projectionContext, result);


            medic.partialExecute("projectionReconciliation",
                    (result1) -> {
                        reconciliationProcessor.processReconciliation(context, projectionContext, task, result1);
                        projectionContext.recompute();
                        medic.traceContext(LOGGER, activityDescription, "projection reconciliation of "+projectionDesc, false, context, false);
                        if (consistencyChecks) context.checkConsistence();
                    },
                    partialProcessingOptions::getProjectionReconciliation,
                    Projector.class, context, projectionContext, result);

            medic.partialExecute("projectionValuesPostRecon",
                    (result1) -> {
                        projectionValuesProcessor.processPostRecon(context, projectionContext, activityDescription, task, result1);
                        if (consistencyChecks) context.checkConsistence();

                        projectionContext.recompute();
                        if (consistencyChecks) context.checkConsistence();
                    },
                    partialProcessingOptions::getProjectionValues,
                    Projector.class, context, projectionContext, result);

            medic.partialExecute("projectionLifecycle",
                    (result1) -> {
                        activationProcessor.processLifecycle(context, projectionContext, now, task, result1);
                        if (consistencyChecks) context.checkConsistence();

                        projectionContext.recompute();
//                        LensUtil.traceContext(LOGGER, activityDescription, "projection lifecycle of "+projectionDesc, false, context, false);
                        if (consistencyChecks) context.checkConsistence();
                    },
                    partialProcessingOptions::getProjectionLifecycle,
                    Projector.class, context, projectionContext, result);

            result.recordSuccess();

        } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException | SecurityViolationException
                | PolicyViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException | RuntimeException | Error e) {

            projectionContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
            ModelImplUtils.handleConnectorErrorCriticality(projectionContext.getResource(), e, result);
        }


    }

    private String getProjectionDesc(LensProjectionContext projectionContext) {
        if (projectionContext.getResource() != null) {
            return projectionContext.getResource() + "("+projectionContext.getResourceShadowDiscriminator().getIntent()+")";
        } else {
            ResourceShadowDiscriminator discr = projectionContext.getResourceShadowDiscriminator();
            if (discr != null) {
                return projectionContext.getResourceShadowDiscriminator().toString();
            } else {
                return "(UNKNOWN)";
            }
        }
    }

    private <F extends ObjectType> void addConflictingContexts(LensContext<F> context) {
        List<LensProjectionContext> conflictingContexts = context.getConflictingProjectionContexts();
        if (conflictingContexts != null && !conflictingContexts.isEmpty()){
            for (LensProjectionContext conflictingContext : conflictingContexts){
                LOGGER.trace("Adding conflicting projection context {}", conflictingContext.getHumanReadableName());
                context.addProjectionContext(conflictingContext);
            }
            context.clearConflictingProjectionContexts();
        }

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
