/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType.*;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.construction.ConstructionTargetKey;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedAssignedResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedConstructionPack;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.projector.ComplexConstructionConsumer;
import com.evolveum.midpoint.model.impl.lens.projector.ConstructionProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation.APrioriDeltaProvider;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation.ItemDefinitionProvider;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.AssignedFocusMappingEvaluationRequest;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.FixedTargetSpecification;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ConstructionTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Assignment processor is recomputing user assignments. It recomputes all the assignments whether they are direct
 * or indirect (roles).
 *
 * Processor does not do the complete recompute. Only the account "existence" is recomputed. I.e. the processor determines
 * what accounts should be added, deleted or kept as they are. The result is marked in account context SynchronizationPolicyDecision.
 * This step does not create any deltas. It recomputes the attributes to delta set triples but does not "refine" them to deltas yet.
 * It cannot create deltas as other mapping may interfere, e.g. outbound mappings. These need to be computed before we can
 * create the final deltas (because there may be mapping exclusions, interference of weak mappings, etc.)
 *
 * The result of assignment processor are intermediary data in the context such as LensContext.evaluatedAssignmentTriple and
 * LensProjectionContext.accountConstructionDeltaSetTriple.
 *
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = AssignmentHolderType.class)
public class AssignmentProcessor implements ProjectorProcessor {

    @Autowired private ReferenceResolver referenceResolver;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private PrismContext prismContext;
    @Autowired private ConstructionProcessor constructionProcessor;
    @Autowired private PolicyRuleProcessor policyRuleProcessor;
    @Autowired private ModelBeans beans;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentProcessor.class);

    private static final String OP_PROCESS_ASSIGNMENTS = AssignmentProcessor.class.getName() + ".processAssignments";
    private static final String OP_EVALUATE_FOCUS_MAPPINGS = AssignmentProcessor.class.getName() + ".evaluateFocusMappings";
    private static final String OP_PROCESS_PROJECTIONS = AssignmentProcessor.class.getName() + ".processProjections";
    private static final String OP_DISTRIBUTE_CONSTRUCTIONS = AssignmentProcessor.class.getName() + ".distributeConstructions";

    /**
     * Processing all the assignments.
     */
    @ProcessorMethod
    public <O extends ObjectType, AH extends AssignmentHolderType> void processAssignments(
            LensContext<O> context, XMLGregorianCalendar now,
            Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        assert context.hasFocusOfType(AssignmentHolderType.class);

        OperationResult result = parentResult.createSubresult(OP_PROCESS_ASSIGNMENTS);
        try {
            try {
                //noinspection unchecked
                processAssignmentsInternal((LensContext<AH>) context, now, task, result);
            } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | PolicyViolationException |
                    CommunicationException | ConfigurationException | SecurityViolationException | RuntimeException | Error e) {
                result.recordException(e);
                throw e;
            }

            OperationResultStatus finalStatus = OperationResultStatus.SUCCESS;
            String message = null;
            int errors = 0;
            for (OperationResult subresult : result.getSubresults()) {
                if (subresult.isError()) {
                    errors++;
                    if (message == null) {
                        message = subresult.getMessage();
                    } else {
                        message = errors + " errors";
                    }
                    finalStatus = OperationResultStatus.PARTIAL_ERROR;
                }
            }
            result.setStatus(finalStatus);
            result.setMessage(message);
            result.recordEnd();
            result.cleanup();
        } catch (Throwable t) { // shouldn't occur -- just in case
            result.recordException(t);
            throw t;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <AH extends AssignmentHolderType> void processAssignmentsInternal(
            LensContext<AH> context, XMLGregorianCalendar now, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        LensFocusContext<AH> focusContext = context.getFocusContext();

        if (focusContext.isDeleted()) {
            LOGGER.trace("Focus is gone, therefore we cannot compute assignments. "
                    + "We just mark all projections as illegal, to ensure they will get removed.");
            markProjectionsAsIllegal(context);
            return;
        }

        checkAssignmentDeltaSanity(context);

        // ASSIGNMENT EVALUATION

        AssignmentTripleEvaluator<AH> assignmentTripleEvaluator = createAssignmentTripleEvaluator(context, now, task, result);

        // Normal processing. The enforcement policy requires that assigned accounts should be added, so we need to figure out
        // which assignments were added. Do a complete recompute for all the enforcement modes. We can do that because this does
        // not create deltas, it just creates the triples. So we can decide what to do later when we convert triples to deltas.

        // Evaluates all assignments and sorts them to triple: added, removed and untouched assignments.
        // This is where most of the assignment-level action happens.
        DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple = assignmentTripleEvaluator.processAllAssignments();
        if (assignmentTripleEvaluator.isMemberOfInvocationResultChanged(evaluatedAssignmentTriple)) {
            LOGGER.debug("Re-evaluating assignments because isMemberOf invocation result has changed");
            assignmentTripleEvaluator.reset(false);
            evaluatedAssignmentTriple = assignmentTripleEvaluator.processAllAssignments();
        }
        context.setEvaluatedAssignmentTriple((DeltaSetTriple) evaluatedAssignmentTriple);

        LOGGER.trace("evaluatedAssignmentTriple:\n{}", evaluatedAssignmentTriple.debugDumpLazily());

        // PROCESSING POLICIES

        policyRuleProcessor.evaluateAssignmentPolicyRules(focusContext, task, result);

        if (ModelExecuteOptions.isIgnoreAssignmentPruning(context.getOptions())) {
            LOGGER.debug("Assignment pruning is ignored because of the model execute option");
        } else {
            boolean needToReevaluateAssignments = processPruning(context, evaluatedAssignmentTriple, result);
            if (needToReevaluateAssignments) {
                LOGGER.debug("Re-evaluating assignments because exclusion pruning rule was triggered");

                assignmentTripleEvaluator.reset(true);
                evaluatedAssignmentTriple = assignmentTripleEvaluator.processAllAssignments();
                context.setEvaluatedAssignmentTriple((DeltaSetTriple) evaluatedAssignmentTriple);

                // TODO implement isMemberOf invocation result change check here! MID-5784
                //  Actually, we should factor out the relevant code to avoid code duplication.

                LOGGER.trace("re-evaluatedAssignmentTriple:\n{}", evaluatedAssignmentTriple.debugDumpLazily());

                policyRuleProcessor.evaluateAssignmentPolicyRules(focusContext, task, result);
            }
        }

        policyRuleProcessor.recordAssignmentPolicyRules(focusContext, task, result);

        // PROCESSING FOCUS

        evaluateFocusMappings(context, now, focusContext, evaluatedAssignmentTriple, task, result);

        if (context.getPartialProcessingOptions().getProjection() != PartialProcessingTypeType.SKIP) {
            if (FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
                processProjections(context, evaluatedAssignmentTriple, task, result);
            } else {
                LOGGER.trace("Skipping processing projections. Not a focus.");
            }
        }

        if (focusContext.isDelete()) {
            LOGGER.trace("Focus is going to be deleted. If some of the projections remained legal (e.g. because the of the"
                    + " assignment enforcement mode) we will mark them as illegal now.");
            markProjectionsAsIllegal(context);
        }
    }

    private <F extends AssignmentHolderType> boolean processPruning(LensContext<F> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, OperationResult result) throws SchemaException {
        return new PruningOperation<>(context, evaluatedAssignmentTriple, beans)
                .execute(result);
    }

    private <AH extends AssignmentHolderType> void processProjections(
            LensContext<AH> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException, PolicyViolationException {

        OperationResult result = parentResult.createMinorSubresult(OP_PROCESS_PROJECTIONS);
        try {
            LOGGER.trace("Projection processing start, evaluatedAssignmentTriple:\n{}",
                    evaluatedAssignmentTriple.debugDumpLazily(1));

            // Evaluate the constructions in assignments now. These were not evaluated in the first pass of AssignmentEvaluator
            // because there may be interaction from focusMappings of some roles to outbound mappings of other roles.
            // Now we have all the focusMappings evaluated so we can evaluate the constructions.
            evaluateConstructions(context, evaluatedAssignmentTriple, task, result);

            // Distributes constructions into appropriate projection contexts,
            // setting relevant properties in these contexts.
            distributeConstructions(context, evaluatedAssignmentTriple, task, result);

            LOGGER.trace("Projection processing done");

            context.removeIgnoredContexts();
            finishLegalDecisions(context);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <AH extends AssignmentHolderType> void evaluateFocusMappings(
            LensContext<AH> context, XMLGregorianCalendar now,
            LensFocusContext<AH> focusContext,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple, Task task,
            OperationResult parentResult) throws SchemaException, ExpressionEvaluationException,
            ConfigurationException, SecurityViolationException, ObjectNotFoundException, CommunicationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE_FOCUS_MAPPINGS)
                .setMinor()
                .build();

        try {
            LOGGER.trace("Starting evaluation of assignment-held mappings");

            ObjectDeltaObject<AH> focusOdoRelative = focusContext.getObjectDeltaObjectRelative();

            List<AssignedFocusMappingEvaluationRequest> allRequests = new ArrayList<>();
            for (EvaluatedAssignmentImpl<AH> evaluatedAssignment : evaluatedAssignmentTriple.getAllValues()) {
                allRequests.addAll(evaluatedAssignment.getFocusMappingEvaluationRequests());
            }

            FocalMappingSetEvaluation.TripleCustomizer<?, ?> customizer = (triple, abstractRequest) -> {
                if (triple == null) {
                    return null;
                }
                AssignedFocusMappingEvaluationRequest request = (AssignedFocusMappingEvaluationRequest) abstractRequest;
                //noinspection unchecked
                EvaluatedAssignmentImpl<AH> evaluatedAssignment = (EvaluatedAssignmentImpl<AH>) request.getEvaluatedAssignment();
                PlusMinusZero relativeMode = request.getRelativeMode();
                Set<PlusMinusZero> presence = new HashSet<>();
                PlusMinusZero resultingMode = null;
                if (evaluatedAssignmentTriple.presentInPlusSet(evaluatedAssignment)) {
                    resultingMode = PlusMinusZero.compute(PlusMinusZero.PLUS, relativeMode);
                    presence.add(PlusMinusZero.PLUS);
                }
                if (evaluatedAssignmentTriple.presentInMinusSet(evaluatedAssignment)) {
                    resultingMode = PlusMinusZero.compute(PlusMinusZero.MINUS, relativeMode);
                    presence.add(PlusMinusZero.MINUS);
                }
                if (evaluatedAssignmentTriple.presentInZeroSet(evaluatedAssignment)) {
                    resultingMode = PlusMinusZero.compute(PlusMinusZero.ZERO, relativeMode);
                    presence.add(PlusMinusZero.ZERO);
                }
                LOGGER.trace("triple customizer: presence = {}, relativeMode = {}, resultingMode = {}", presence, relativeMode,
                        resultingMode);

                if (presence.isEmpty()) {
                    throw new IllegalStateException("Evaluated assignment is not present in any of plus/minus/zero sets "
                            + "of the triple. Assignment = " + evaluatedAssignment + ", triple = " + triple);
                } else if (presence.size() > 1) {
                    // TODO think about this
                    throw new IllegalStateException("Evaluated assignment is present in more than one plus/minus/zero sets "
                            + "of the triple: " + presence + ". Assignment = " + evaluatedAssignment + ", triple = " + triple);
                }
                DeltaSetTriple<ItemValueWithOrigin<PrismValue, ItemDefinition<?>>> rv =
                        prismContext.deltaFactory().createDeltaSetTriple();
                if (resultingMode != null) {
                    switch (resultingMode) {
                        case PLUS -> rv.addAllToPlusSet(triple.getNonNegativeValues()); // MID-6403
                        case MINUS -> rv.addAllToMinusSet(triple.getNonPositiveValues()); // MID-6403
                        case ZERO -> rv = triple;
                    }
                }
                return rv;
            };

            FocalMappingSetEvaluation.EvaluatedMappingConsumer mappingConsumer = (mapping, abstractRequest) -> {
                AssignedFocusMappingEvaluationRequest request = (AssignedFocusMappingEvaluationRequest) abstractRequest;
                request.getEvaluatedAssignment().addFocusMapping(mapping);
            };

            var targetSpecification = new FixedTargetSpecification<>(focusOdoRelative.getNewObject(), true);

            MappingEvaluationEnvironment env = new MappingEvaluationEnvironment(
                    "focus mappings in assignments of " + focusContext.getHumanReadableName(),
                    now, task);

            FocalMappingSetEvaluation<AH, AH> mappingSetEvaluation = new FocalMappingSetEvaluationBuilder<AH, AH>()
                    .context(context)
                    .evaluationRequests(allRequests)
                    .phase(null)
                    .focusOdo(focusOdoRelative)
                    .targetSpecification(targetSpecification)
                    .tripleCustomizer(customizer)
                    .mappingConsumer(mappingConsumer)
                    .iteration(focusContext.getIteration())
                    .iterationToken(focusContext.getIterationToken())
                    .beans(beans)
                    .env(env)
                    .result(result)
                    .build();
            mappingSetEvaluation.evaluateMappingsToTriples();

            DeltaSetTripleIvwoMap focusOutputTripleMap = mappingSetEvaluation.getOutputTripleMap();

            logOutputTripleMap(focusOutputTripleMap);

            DeltaSetTripleMapConsolidation<AH> consolidation = new DeltaSetTripleMapConsolidation<>(
                    focusOutputTripleMap,
                    ObjectTypeUtil.getValue(focusOdoRelative.getNewObject()),
                    APrioriDeltaProvider.forDelta(focusOdoRelative.getObjectDelta()),
                    context::primaryFocusItemDeltaExists,
                    null,
                    null,
                    ItemDefinitionProvider.forObjectDefinition(focusContext.getObjectDefinition()),
                    env,
                    context,
                    result);
            consolidation.computeItemDeltas();
            Collection<ItemDelta<?, ?>> focusDeltas = consolidation.getItemDeltas();

            LOGGER.trace("Computed focus deltas: {}", focusDeltas);
            focusContext.swallowToSecondaryDelta(focusDeltas);
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void logOutputTripleMap(DeltaSetTripleIvwoMap focusOutputTripleMap) {
        if (LOGGER.isTraceEnabled()) {
            for (var entry : focusOutputTripleMap.entrySet()) {
                LOGGER.trace("Resulting output triple for {}:\n{}", entry.getKey(), entry.getValue().debugDump(1));
            }
        }
    }

    private <AH extends AssignmentHolderType> void distributeConstructions(
            LensContext<AH> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            Task ignored,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ComplexConstructionConsumer<ConstructionTargetKey, EvaluatedAssignedResourceObjectConstructionImpl<AH>> consumer =
                new ComplexConstructionConsumer<>() {

                    private boolean processOnlyExistingProjContexts;

                    @Override
                    public boolean before(@NotNull ConstructionTargetKey key) {
                        processOnlyExistingProjContexts = false;
                        if (ModelExecuteOptions.isLimitPropagation(context.getOptions())) {
                            String triggeringResource = context.getTriggeringResourceOid();
                            if (triggeringResource != null
                                    && !key.getResourceOid().equals(triggeringResource)) {
                                LOGGER.trace(
                                        "Skipping processing construction for shadow identified by {} because of limitation "
                                                + "to propagate changes only for resource {}", key, triggeringResource);
                                return false;
                            }
                            if (context.isDiscoveryChannel()) {
                                LOGGER.trace(
                                        "Processing of constructions of {} will be limited to existing projections because "
                                                + "we are in the discovery mode and 'limit propagation' option is on", key);
                                processOnlyExistingProjContexts = true;
                            }
                        }

                        return true;
                    }

                    @Override
                    public void onAssigned(@NotNull ConstructionTargetKey key, String desc)
                            throws SchemaException, ConfigurationException {
                        LensProjectionContext projectionContext =
                                LensContext.getOrCreateProjectionContext(context, key, false).context;
                        projectionContext.setAssigned(true);
                        projectionContext.setAssignedOldIfUnknown(false);
                        projectionContext.setLegalOldIfUnknown(false);
                        if (projectionContext.getAssignmentPolicyEnforcementMode() != NONE) {
                            LOGGER.trace("Projection {} legal: assigned (valid)", desc);
                            projectionContext.setLegal(true);
                        } else {
                            LOGGER.trace("Projection {} skip: assigned (valid), NONE enforcement", desc);
                        }
                    }

                    @Override
                    public void onUnchangedValid(@NotNull ConstructionTargetKey key, String desc)
                            throws SchemaException, ConfigurationException {
                        LensProjectionContext projectionContext = context.findFirstProjectionContext(key, false);
                        if (projectionContext == null) {
                            if (processOnlyExistingProjContexts) {
                                LOGGER.trace("Projection {} skip: unchanged (valid), processOnlyExistingProjContexts", desc);
                                return;
                            }
                            // The projection should exist before the change but it does not
                            // This happens during reconciliation if there is an inconsistency.
                            // Pretend that the assignment was just added. That should do.
                            projectionContext =
                                    LensContext.getOrCreateProjectionContext(context, key, false).context;
                        }
                        LOGGER.trace("Projection {} legal: unchanged (valid)", desc);
                        projectionContext.setAssigned(true);
                        projectionContext.setAssignedOldIfUnknown(true);
                        if (projectionContext.getAssignmentPolicyEnforcementMode() == NONE) {
                            projectionContext.setLegalOld(null);
                            projectionContext.setLegal(null);
                        } else {
                            projectionContext.setLegalOldIfUnknown(true);
                            projectionContext.setLegal(true);
                        }
                    }

                    @Override
                    public void onUnchangedInvalid(@NotNull ConstructionTargetKey key, String desc)
                            throws SchemaException, ConfigurationException {
                        LensProjectionContext projectionContext = context.findFirstProjectionContext(key, true);
                        if (projectionContext == null) {
                            if (processOnlyExistingProjContexts) {
                                LOGGER.trace("Projection {} skip: unchanged (invalid), processOnlyExistingProjContexts", desc);
                            } else {
                                LOGGER.trace("Projection {} skip: unchanged (invalid) and does not exist in current lens context",
                                        desc);
                            }
                            return;
                        }
                        LOGGER.trace("Projection {} illegal: unchanged (invalid)", desc);
                        projectionContext.setLegal(false);
                        projectionContext.setLegalOldIfUnknown(false);
                        projectionContext.setAssigned(false);
                        projectionContext.setAssignedOldIfUnknown(false);
                        if (projectionContext.getAssignmentPolicyEnforcementMode() == NONE
                                || projectionContext.getAssignmentPolicyEnforcementMode() == POSITIVE) {
                            projectionContext.setLegalOld(null);
                            projectionContext.setLegal(null);
                        } else {
                            projectionContext.setLegalOldIfUnknown(false);
                            projectionContext.setLegal(false);
                        }
                    }

                    @Override
                    public void onUnassigned(@NotNull ConstructionTargetKey key, String desc)
                            throws SchemaException, ConfigurationException {
                        // Note we look only at wave >= current wave here
                        LensProjectionContext projectionContext = context.findFirstProjectionContext(key, true);
                        if (projectionContext != null && projectionContext.getObjectCurrent() != null) {
                            projectionContext.setAssigned(false);
                            projectionContext.setAssignedOldIfUnknown(true);
                            projectionContext.setLegalOldIfUnknown(true);

                            AssignmentPolicyEnforcementType enforcement = projectionContext.getAssignmentPolicyEnforcementMode();
                            // TODO: check for MARK and LEGALIZE enforcement policies ....add delete also for relative enforcement
                            if (enforcement == FULL || enforcement == RELATIVE) {
                                LOGGER.trace("Projection {} illegal: unassigned", desc);
                                projectionContext.setLegal(false);
                            } else if (enforcement == POSITIVE) {
                                LOGGER.trace("Projection {} legal: unassigned, but allowed by policy ({})", desc, enforcement);
                                projectionContext.setLegal(true);
                            } else {
                                LOGGER.trace("Projection {} legal: unassigned, policy decision postponed ({})", desc, enforcement);
                                projectionContext.setLegal(null);
                            }
                        } else {
                            LOGGER.trace("Projection {} nothing: unassigned (valid->invalid) but not there", desc);
                            // We have to delete something that is not there. Nothing to do.
                        }
                    }

                    @Override
                    public void after(
                            @NotNull ConstructionTargetKey key,
                            String desc,
                            DeltaMapTriple<ConstructionTargetKey, EvaluatedConstructionPack<EvaluatedAssignedResourceObjectConstructionImpl<AH>>> constructionMapTriple) {
                        DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<AH>> evaluatedConstructionDeltaSetTriple =
                                prismContext.deltaFactory().createDeltaSetTriple(
                                        getConstructions(constructionMapTriple.getZeroMap().get(key), true),
                                        getConstructions(constructionMapTriple.getPlusMap().get(key), true),
                                        getConstructions(constructionMapTriple.getMinusMap().get(key), false));
                        LensProjectionContext projectionContext = context.findFirstProjectionContext(key, true);
                        if (projectionContext != null) {
                            // This can be null in an exotic case if we delete already deleted account
                            LOGGER.trace("Construction delta set triple for {}:\n{}", key,
                                    evaluatedConstructionDeltaSetTriple.debugDumpLazily(1));
                            projectionContext.setEvaluatedAssignedConstructionDeltaSetTriple(evaluatedConstructionDeltaSetTriple);
                            if (isForceRecon(constructionMapTriple.getZeroMap().get(key)) ||
                                    isForceRecon(constructionMapTriple.getPlusMap().get(key)) ||
                                    isForceRecon(constructionMapTriple.getMinusMap().get(key))) {
                                projectionContext.setDoReconciliation(true);
                            }
                        }
                    }
                };

        OperationResult result = parentResult.createMinorSubresult(OP_DISTRIBUTE_CONSTRUCTIONS);
        try {
            LOGGER.trace("Starting construction distribution");
            constructionProcessor.distributeConstructions(
                    evaluatedAssignmentTriple,
                    EvaluatedAssignmentImpl::getConstructionTriple,
                    EvaluatedResourceObjectConstructionImpl::getTargetKey,
                    consumer);
            LOGGER.trace("Finished construction distribution");
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Checks if we do not try to modify assignment.targetRef or assignment.construction.kind or intent.
     */
    private <F extends AssignmentHolderType> void checkAssignmentDeltaSanity(LensContext<F> context) throws SchemaException {
        ObjectDelta<F> focusDelta = context.getFocusContext().getCurrentDelta();
        if (focusDelta == null || !focusDelta.isModify()) {
            return;
        }

        for (@SuppressWarnings("rawtypes") ItemDelta itemDelta : focusDelta.getModifications()) {
            ItemPath itemPath = itemDelta.getPath().namedSegmentsOnly();
            if (SchemaConstants.PATH_ASSIGNMENT_TARGET_REF.isSubPathOrEquivalent(itemPath)) {
                throw new SchemaException("It is not allowed to change targetRef in an assignment. Offending path: " + itemPath);
            }
            if (SchemaConstants.PATH_ASSIGNMENT_CONSTRUCTION_KIND.isSubPathOrEquivalent(itemPath)) {
                throw new SchemaException("It is not allowed to change construction.kind in an assignment. Offending path: " + itemPath);
            }
            if (SchemaConstants.PATH_ASSIGNMENT_CONSTRUCTION_INTENT.isSubPathOrEquivalent(itemPath)) {
                throw new SchemaException("It is not allowed to change construction.intent in an assignment. Offending path: " + itemPath);
            }
            // TODO some mechanism to detect changing kind/intent by add/delete/replace whole ConstructionType (should be implemented in the caller)
        }
    }

    private <AH extends AssignmentHolderType> void evaluateConstructions(LensContext<AH> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, ConfigurationException,
            CommunicationException {
        ObjectDeltaObject<AH> focusOdoAbsolute = context.getFocusOdoAbsolute();
        evaluateConstructions(context, evaluatedAssignmentTriple.getZeroSet(), focusOdoAbsolute, task, result);
        evaluateConstructions(context, evaluatedAssignmentTriple.getPlusSet(), focusOdoAbsolute, task, result);
        evaluateConstructions(context, evaluatedAssignmentTriple.getMinusSet(), focusOdoAbsolute, task, result);
    }

    private <F extends AssignmentHolderType> void evaluateConstructions(LensContext<F> context,
            Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignments,
            ObjectDeltaObject<F> focusOdoAbsolute, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, ConfigurationException,
            CommunicationException {
        if (evaluatedAssignments == null) {
            return;
        }
        for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignments) {
            try {
                evaluatedAssignment.evaluateConstructions(focusOdoAbsolute, context::rememberResource, task, result);
            } catch (ObjectNotFoundException ex) {
                LOGGER.trace("Processing of assignment resulted in error {}: {}", ex,
                        SchemaDebugUtil.prettyPrint(evaluatedAssignment.getAssignment()));
                if (!ModelExecuteOptions.isForce(context.getOptions())) {
                    ModelImplUtils.recordFatalError(result, ex);
                }
            } catch (SchemaException | ConfigurationException ex) {
                // TODO what about other kinds of exceptions? Shouldn't they be treated also like this one?
                AssignmentType assignment = evaluatedAssignment.getAssignment();
                LOGGER.trace("Processing of assignment resulted in error {}: {}", ex, SchemaDebugUtil.prettyPrint(assignment));
                ModelImplUtils.recordFatalError(result, ex);
                ConstructionType construction = assignment != null ? assignment.getConstruction() : null;
                String resourceOid = ConstructionTypeUtil.getResourceOid(construction);
                if (resourceOid != null) {
                    context.markMatchingProjectionsBroken(construction, resourceOid);
                } else {
                    // This is a role assignment or something like that. Or we cannot get resource OID.
                    // Just throw the original exception for now.
                    throw ex;
                }
            }
        }
    }

    /**
     * Simply mark all projections as illegal - except those that are being unlinked
     */
    private <F extends AssignmentHolderType> void markProjectionsAsIllegal(LensContext<F> context) {
        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
            if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK) {
                // We do not want to affect unlinked projections
                continue;
            }
            projectionContext.setLegal(false);
            projectionContext.setLegalOldIfUnknown(true);
        }
    }

    @NotNull
    private <AH extends AssignmentHolderType> Collection<EvaluatedAssignedResourceObjectConstructionImpl<AH>> getConstructions(
            EvaluatedConstructionPack<EvaluatedAssignedResourceObjectConstructionImpl<AH>> evaluatedConstructionPack,
            boolean validOnly) {
        if (evaluatedConstructionPack == null) {
            return Collections.emptySet();
        }
        if (validOnly && !evaluatedConstructionPack.hasValidAssignment()) {
            return Collections.emptySet();
        }
        return evaluatedConstructionPack.getEvaluatedConstructions();
    }

    private boolean isForceRecon(EvaluatedConstructionPack<?> accountEvaluatedConstructionPack) {
        return accountEvaluatedConstructionPack != null && accountEvaluatedConstructionPack.isForceRecon();
    }

    /**
     * Set 'legal' flag for the accounts that does not have it already
     */
    private <F extends AssignmentHolderType> void finishLegalDecisions(LensContext<F> context)
            throws PolicyViolationException, SchemaException, ConfigurationException {
        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {

            String desc = projectionContext.toHumanReadableString();

            if (projectionContext.isLegal() != null) {
                // already have decision
                LOGGER.trace("Projection {} legal={} (predetermined)", desc, projectionContext.isLegal());
                propagateLegalDecisionToHigherOrders(context, projectionContext);
                continue;
            }

            boolean legalized;
            if (projectionContext.isLegalize()) {
                legalized = legalize(context, projectionContext);
            } else {
                legalized = false;
            }

            if (legalized) {

                LOGGER.trace("Projection {} legalized", desc);
                projectionContext.setAssigned(true);
                projectionContext.setAssignedOldIfUnknown(false);
                projectionContext.setLegal(true);
                projectionContext.setLegalOldIfUnknown(false);

            } else {

                AssignmentPolicyEnforcementType enforcementType = projectionContext.getAssignmentPolicyEnforcementMode();

                if (enforcementType == FULL) {
                    LOGGER.trace("Projection {} illegal: no assignment in FULL enforcement", desc);
                    // What is not explicitly allowed is illegal in FULL enforcement mode
                    projectionContext.setLegal(false);
                    // We need to set the old value for legal to false. There was no assignment delta for it.
                    // If it were then the code could not get here.
                    projectionContext.setLegalOldIfUnknown(false);
                    if (projectionContext.isAdd()) {
                        throw new PolicyViolationException("Attempt to add projection " + projectionContext.toHumanReadableString()
                                + " while the synchronization enforcement policy is FULL and the projection is not assigned");
                    }

                } else if (enforcementType == NONE && !projectionContext.isGone()) {
                    if (projectionContext.isAdd()) {
                        LOGGER.trace("Projection {} legal: added in NONE policy", desc);
                        projectionContext.setLegal(true);
                        projectionContext.setLegalOldIfUnknown(false);
                    } else {
                        if (projectionContext.isExists()) {
                            LOGGER.trace("Projection {} legal: exists in NONE policy", desc);
                        } else {
                            LOGGER.trace("Projection {} illegal: does not exist in NONE policy", desc);
                        }
                        // Everything that exists was legal and is legal. Nothing really changes.
                        projectionContext.setLegal(projectionContext.isExists());
                        projectionContext.setLegalOldIfUnknown(projectionContext.isExists());
                    }

                } else if (enforcementType == POSITIVE && !projectionContext.isGone()) {
                    // Everything that is not yet dead is legal in POSITIVE enforcement mode
                    LOGGER.trace("Projection {} legal: not dead in POSITIVE policy", desc);
                    projectionContext.setLegal(true);
                    projectionContext.setLegalOldIfUnknown(true);

                } else if (enforcementType == RELATIVE && !projectionContext.isGone() &&
                        projectionContext.isLegal() == null && projectionContext.isLegalOld() == null) {
                    // RELATIVE mode and nothing has changed. Maintain status quo. Pretend that it is legal.
                    LOGGER.trace("Projection {} legal: no change in RELATIVE policy", desc);
                    projectionContext.setLegal(true);
                    projectionContext.setLegalOldIfUnknown(true);
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Finishing legal decision for {}, gone {}, enforcement mode {}, legalize {} (real: {}): {} -> {}",
                        projectionContext.toHumanReadableString(), projectionContext.isGone(),
                        projectionContext.getAssignmentPolicyEnforcementMode(),
                        projectionContext.isLegalize(), legalized,
                        projectionContext.isLegalOld(), projectionContext.isLegal());
            }

            propagateLegalDecisionToHigherOrders(context, projectionContext);
        }
    }

    private <F extends ObjectType> void propagateLegalDecisionToHigherOrders(
            LensContext<F> context, LensProjectionContext refProjCtx) {
        ProjectionContextKey key = refProjCtx.getKey();
        for (LensProjectionContext aProjCtx : context.getProjectionContexts()) {
            ProjectionContextKey aKey = aProjCtx.getKey();
            if (key.equivalent(aKey) && key.getOrder() < aKey.getOrder()) {
                aProjCtx.setLegal(refProjCtx.isLegal());
                aProjCtx.setLegalOldIfUnknown(refProjCtx.isLegalOld());
                aProjCtx.setExists(refProjCtx.isExists());
            }
        }
    }

    private boolean legalize(LensContext<? extends AssignmentHolderType> context, LensProjectionContext projCtx)
            throws SchemaException {

        LensFocusContext<? extends AssignmentHolderType> focusContext = context.getFocusContextRequired();
        ProjectionContextKey projKey = projCtx.getKey();

        if (!projKey.isClassified()) {
            LOGGER.warn("Cannot legalize unclassified projection: {} - no assignment will be created in {}",
                    projCtx.getHumanReadableName(), focusContext.getObjectAny());
            return false;
        }

        AssignmentType assignment = new AssignmentType()
                .construction(new ConstructionType()
                        .resourceRef(projCtx.getResourceOidRequired(), ResourceType.COMPLEX_TYPE)
                        .kind(projKey.getKind())
                        .intent(projKey.getIntent()));

        focusContext.swallowToSecondaryDelta(
                prismContext.deltaFor(context.getFocusClass())
                        .item(AssignmentHolderType.F_ASSIGNMENT)
                        .add(assignment)
                        .asItemDelta());

        return true;
    }

    @ProcessorMethod
    <F extends ObjectType> void processOrgAssignments(
            LensContext<F> context,
            XMLGregorianCalendar ignored1,
            Task ignored2,
            OperationResult ignored3) throws SchemaException, PolicyViolationException {
        LensFocusContext<F> focusContext = context.getFocusContext();

        Collection<PrismReferenceValue> shouldBeParentOrgRefs = new ArrayList<>();

        DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null) {
            return; // could be if "assignments" step is skipped
        }
        for (EvaluatedAssignmentImpl<?> evalAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) { // MID-6403
            if (evalAssignment.isValid()) {
                addReferences(shouldBeParentOrgRefs, evalAssignment.getOrgRefVals());
            }
        }
        setReferences(focusContext, ObjectType.F_PARENT_ORG_REF, shouldBeParentOrgRefs);

        ObjectDelta<F> focusPrimaryDelta = focusContext.getPrimaryDelta();
        if (focusPrimaryDelta != null) {
            ReferenceDelta parentOrgRefDelta = focusPrimaryDelta.findReferenceModification(ObjectType.F_PARENT_ORG_REF);
            if (parentOrgRefDelta != null) {
                List<PrismReferenceValue> parentOrgRefCurrentValues = null;
                PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
                if (objectCurrent != null) {
                    PrismReference parentOrgRefCurrent = objectCurrent.findReference(ObjectType.F_PARENT_ORG_REF);
                    if (parentOrgRefCurrent != null) {
                        parentOrgRefCurrentValues = parentOrgRefCurrent.getValues();
                    }
                }
                try {

                    parentOrgRefDelta.validateValues(
                            (plusMinusZero, val) -> {
                                switch (plusMinusZero) {
                                    case PLUS, ZERO -> {
                                        if (!PrismValueCollectionsUtil.containsRealValue(shouldBeParentOrgRefs, val)) {
                                            throw new TunnelException(
                                                    new PolicyViolationException(
                                                            "Attempt to add parentOrgRef " + val.getOid()
                                                                    + ", but it is not allowed by assignments"));
                                        }
                                    }
                                    case MINUS -> {
                                        if (PrismValueCollectionsUtil.containsRealValue(shouldBeParentOrgRefs, val)) {
                                            throw new TunnelException(
                                                    new PolicyViolationException(
                                                            "Attempt to delete parentOrgRef " + val.getOid()
                                                                    + ", but it is mandated by assignments"));
                                        }
                                    }
                                }
                            }, parentOrgRefCurrentValues);

                } catch (TunnelException e) {
                    throw (PolicyViolationException) e.getCause();
                }
            }
        }

        computeTenantRef(context);
    }

    private <F extends ObjectType> void computeTenantRef(LensContext<F> context)
            throws PolicyViolationException, SchemaException {
        String tenantOid = null;
        LensFocusContext<F> focusContext = context.getFocusContext();
        PrismObject<F> objectNew = focusContext.getObjectNew();
        if (objectNew == null) {
            return;
        }

        if (objectNew.canRepresent(OrgType.class) && BooleanUtils.isTrue(((OrgType) objectNew.asObjectable()).isTenant())) {
            // Special "zero" case. Tenant org has itself as a tenant.
            tenantOid = objectNew.getOid();

        } else {

            for (EvaluatedAssignmentImpl<?> evalAssignment : context.getNonNegativeEvaluatedAssignments()) {
                if (!evalAssignment.isValid()) {
                    continue;
                }
                String assignmentTenantOid = evalAssignment.getTenantOid();
                if (assignmentTenantOid == null) {
                    continue;
                }
                if (tenantOid == null) {
                    tenantOid = assignmentTenantOid;
                } else {
                    if (!assignmentTenantOid.equals(tenantOid)) {
                        throw new PolicyViolationException(
                                "Two different tenants (" + tenantOid + ", " + assignmentTenantOid + ") applicable to "
                                        + context.getFocusContext().getHumanReadableName());
                    }
                }
            }

        }

        addTenantRefDelta(context, objectNew, tenantOid);
    }

    private <F extends ObjectType> void addTenantRefDelta(
            LensContext<F> context, PrismObject<F> objectNew, String tenantOid) throws SchemaException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        ObjectReferenceType currentTenantRef = objectNew.asObjectable().getTenantRef();
        if (currentTenantRef == null) {
            if (tenantOid == null) {
                // nothing to do here
            } else {
                LOGGER.trace("Setting tenantRef to {}", tenantOid);
                ReferenceDelta tenantRefDelta = prismContext.deltaFactory().reference()
                        .createModificationReplace(ObjectType.F_TENANT_REF, focusContext.getObjectDefinition(), tenantOid);
                focusContext.swallowToSecondaryDelta(tenantRefDelta);
            }
        } else {
            if (tenantOid == null) {
                LOGGER.trace("Clearing tenantRef");
                ReferenceDelta tenantRefDelta = prismContext.deltaFactory().reference()
                        .createModificationReplace(
                                ObjectType.F_TENANT_REF, focusContext.getObjectDefinition(), (PrismReferenceValue) null);
                focusContext.swallowToSecondaryDelta(tenantRefDelta);
            } else {
                if (!tenantOid.equals(currentTenantRef.getOid())) {
                    LOGGER.trace("Changing tenantRef to {}", tenantOid);
                    ReferenceDelta tenantRefDelta = prismContext.deltaFactory().reference()
                            .createModificationReplace(
                                    ObjectType.F_TENANT_REF, focusContext.getObjectDefinition(), tenantOid);
                    focusContext.swallowToSecondaryDelta(tenantRefDelta);
                }
            }
        }
    }

    @ProcessorMethod
    <F extends ObjectType> void checkForAssignmentConflicts(
            LensContext<F> context, XMLGregorianCalendar ignored1, Task ignored2, OperationResult ignored3)
            throws PolicyViolationException, SchemaException, ConfigurationException {
        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
            if (NONE == projectionContext.getAssignmentPolicyEnforcementMode()) {
                continue;
            }
            if (projectionContext.isGone()) {
                continue;
            }
            if (Boolean.TRUE.equals(projectionContext.isAssigned())) {
                ObjectDelta<ShadowType> projectionPrimaryDelta = projectionContext.getPrimaryDelta();
                if (ObjectDelta.isDelete(projectionPrimaryDelta)) {
                    throw new PolicyViolationException(
                            "Attempt to delete " + projectionContext.getHumanReadableName() + " while "
                                    + "it is assigned violates an assignment policy");
                }
            }
        }
    }

    public void processAssignmentsAccountValues(LensProjectionContext ignored1, OperationResult ignored2) {

        // TODO: reevaluate constructions
        // This should re-evaluate all the constructions. They are evaluated already, evaluated in the assignment step before.
        // But if there is any iteration counter that it will not be taken into account

    }

    @ProcessorMethod
    <F extends ObjectType> void processMembershipAndDelegatedRefs(
            LensContext<F> context,
            XMLGregorianCalendar ignored1,
            Task ignored2,
            OperationResult ignored3)
            throws SchemaException {
        assert context.hasFocusOfType(AssignmentHolderType.class);
        LensFocusContext<F> focusContext = context.getFocusContext();

        Collection<PrismReferenceValue> shouldBeRoleRefs = new ArrayList<>();
        Collection<PrismReferenceValue> shouldBeDelegatedRefs = new ArrayList<>();
        Collection<PrismReferenceValue> shouldBeArchetypeRefs = new ArrayList<>();

        DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null) {
            return; // could be if the "assignments" step is skipped
        }
        // Similar code is in AssignmentEvaluator.isMemberOfInvocationResultChanged -- check that if changing the business logic
        for (EvaluatedAssignmentImpl<?> evalAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) { // MID-6403
            if (evalAssignment.isValid()) {
                LOGGER.trace("Adding references from: {}", evalAssignment);
                addRoleReferences(shouldBeRoleRefs, evalAssignment, focusContext);
                addReferences(shouldBeDelegatedRefs, evalAssignment.getDelegationRefVals());
            } else {
                LOGGER.trace("Skipping because invalid (except for potential archetypes): {}", evalAssignment);
            }
            addReferences(shouldBeArchetypeRefs, evalAssignment.getArchetypeRefVals());
        }

        //TODO check for structural archetypes?
//        if (shouldBeArchetypeRefs.size() > 1) {
//            throw new ConfigurationException("Only single archetype supported. Attempting to add " + shouldBeArchetypeRefs.size() + ": " + shouldBeArchetypeRefs);
//        }

        setReferences(focusContext, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF, shouldBeRoleRefs);
        setReferences(focusContext, AssignmentHolderType.F_DELEGATED_REF, shouldBeDelegatedRefs);
        setReferences(focusContext, AssignmentHolderType.F_ARCHETYPE_REF, shouldBeArchetypeRefs);
    }

    private void addRoleReferences(
            Collection<PrismReferenceValue> shouldBeRoleRefs, EvaluatedAssignmentImpl<?> evaluatedAssignment,
            LensFocusContext<?> focusContext) throws SchemaException {
        Collection<PrismReferenceValue> membershipRefVals = evaluatedAssignment.getMembershipRefVals();
        // If sysconfig enables accesses value metadata, we will add them.
        if (focusContext.getLensContext().areAccessesMetadataEnabled()) {
            // Refs can be shared also for archetype refs and we don't want metadata there (MID-8664).
            membershipRefVals = copySimpleReferenceValuesQuickly(membershipRefVals);
            for (PrismReferenceValue roleRef : membershipRefVals) {
                addAssignmentPathValueMetadataValues(roleRef, evaluatedAssignment, focusContext);
            }
        }

        addReferences(shouldBeRoleRefs, membershipRefVals);
    }

    /**
     * Quick and dirty fix for the issue of cloning the whole content of references, including the embedded object.
     * To be resolved more intelligently in the future.
     */
    private static @NotNull Collection<PrismReferenceValue> copySimpleReferenceValuesQuickly(
            @NotNull Collection<PrismReferenceValue> originals) {
        List<PrismReferenceValue> copied = new ArrayList<>(originals.size());
        for (PrismReferenceValue original : originals) {
            var copy = new PrismReferenceValueImpl();
            copy.setOid(original.getOid());
            PolyString targetName = original.getTargetName();
            if (targetName != null) {
                // In general, this is not strictly necessary (maybe for debugging); but for create-on-demand scenarios
                // it must be here to avoid "empty references" consistency check message.
                copy.setTargetName(targetName.copy());
            }
            copy.setTargetType(original.getTargetType());
            copy.setRelation(original.getRelation());
            if (original.hasValueMetadata()) { // getValueMetadata() creates an empty metadata object
                copy.setValueMetadata(original.getValueMetadata().clone());
            }
            copied.add(copy);
        }
        return copied;
    }

    private void addAssignmentPathValueMetadataValues(
            PrismReferenceValue roleRef, EvaluatedAssignment evaluatedAssignment, LensFocusContext<?> focusContext)
            throws SchemaException {
        List<EvaluatedAssignmentTarget> evaluatedAssignmentTargets =
                findEvaluatedAssignmentTargets(roleRef, evaluatedAssignment);
        if (evaluatedAssignmentTargets.isEmpty()) {
            // Cases like Approver relations, for which EvaluatedAssignment has empty roles DeltaSetTriple.
            // Sometimes we have target available (e.g. Approver with filter), sometimes not (when targetRef has OID).
            PrismObject<?> assignmentTarget = evaluatedAssignment.getTarget();
            ObjectReferenceType assignmentTargetRef = evaluatedAssignment.getAssignment().getTargetRef();
            String assignmentTargetOid = assignmentTarget != null ? assignmentTarget.getOid() : assignmentTargetRef.getOid();
            addAssignmentPathValueMetadataValue(roleRef, evaluatedAssignment,
                    new AssignmentPathType()
                            .segment(new AssignmentPathSegmentType()
                                    .assignmentId(evaluatedAssignment.getAssignmentId())
                                    .segmentOrder(1)
                                    .isAssignment(true)
                                    .matchingOrder(true)
                                    .sourceRef(focusContext.getOid(),
                                            focusContext.getObjectDefinition().getTypeName(),
                                            SchemaConstants.ORG_DEFAULT)
                                    .targetRef(assignmentTargetOid, roleRef.getTargetType(), roleRef.getRelation())));
        } else {
            for (EvaluatedAssignmentTarget evaluatedAssignmentTarget : evaluatedAssignmentTargets) {
                AssignmentPathType assignmentPath =
                        evaluatedAssignmentTarget.getAssignmentPath().toAssignmentPathBean(false);
                // There can be some value metadata already created by previous assignment evaluation,
                // but we will add new metadata container for each assignment path without touching any existing ones.
                addAssignmentPathValueMetadataValue(roleRef, evaluatedAssignment, assignmentPath);
            }
        }
    }

    private void addAssignmentPathValueMetadataValue(
            PrismReferenceValue roleRef, EvaluatedAssignment evaluatedAssignment, AssignmentPathType assignmentPath)
            throws SchemaException {
        //noinspection unchecked
        roleRef.getValueMetadataAsContainer().add(new ValueMetadataType()
                .provenance(new ProvenanceMetadataType()
                        .assignmentPath(assignmentPathToMetadata(assignmentPath)))
                .storage(new StorageMetadataType()
                        .createTimestamp(determineAssignmentSinceTimestamp(evaluatedAssignment)))
                .asPrismContainerValue());
    }

    /**
     * Technically, storage/createTimestamp should be "now", but in this case we use it as "assigned since" date as well.
     * Normally, it is virtually the same date, but if metadata are created later, we want to "reconstruct" the date.
     * This also solves the problem for existing deployments.
     */
    private static XMLGregorianCalendar determineAssignmentSinceTimestamp(EvaluatedAssignment evalAssignment) {
        return Objects.requireNonNullElseGet(
                ValueMetadataTypeUtil.getCreateTimestamp(evalAssignment.getAssignment()),
                () -> MiscUtil.asXMLGregorianCalendar(System.currentTimeMillis()));
    }

    private AssignmentPathMetadataType assignmentPathToMetadata(AssignmentPathType assignmentPath) {
        AssignmentPathMetadataType metadata = new AssignmentPathMetadataType();
        metadata.sourceRef(assignmentPath.getSegment().get(0).getSourceRef());
        for (AssignmentPathSegmentType segment : assignmentPath.getSegment()) {
            boolean isAssignment = BooleanUtils.isTrue(segment.isIsAssignment());
            metadata.beginSegment()
                    .segmentOrder(segment.getSegmentOrder())
                    .targetRef(segment.getTargetRef())
                    .matchingOrder(segment.isMatchingOrder())
                    .assignmentId(isAssignment ? segment.getAssignmentId() : null)
                    .inducementId(isAssignment ? null : segment.getAssignmentId());
        }
        return metadata;
    }

    private @NotNull List<EvaluatedAssignmentTarget> findEvaluatedAssignmentTargets(
            PrismReferenceValue roleRef, EvaluatedAssignment evaluatedAssignment) {
        List<EvaluatedAssignmentTarget> result = new ArrayList<>();
        for (EvaluatedAssignmentTarget eat : evaluatedAssignment.getRoles().getNonNegativeValues()) {
            ObjectReferenceType evaluatedAssignmentTargetRef = eat.getAssignment().getTargetRef();
            if (MiscUtil.equals(evaluatedAssignmentTargetRef.getOid(), roleRef.getOid())
                    && prismContext.relationsEquivalent(evaluatedAssignmentTargetRef.getRelation(), roleRef.getRelation())) {
                result.add(eat);
            }
        }
        return result;
    }

    private <F extends ObjectType> void setReferences(LensFocusContext<F> focusContext, QName name,
            Collection<PrismReferenceValue> targetState) throws SchemaException {

        ItemName itemName = ItemName.fromQName(name);
        PrismObject<F> existingFocus = focusContext.getObjectCurrent();
        if (existingFocus == null) {
            if (targetState.isEmpty()) {
                return;
            }
        } else {
            PrismReference existingState = existingFocus.findReference(itemName);
            if (existingState == null || existingState.isEmpty()) {
                if (targetState.isEmpty()) {
                    return;
                }
            } else {
                List<PrismReferenceValue> existingValues = existingState.getValues();
                adoptExistingStorageCreateTimestampValueMetadata(targetState, existingValues);
                // We don't use QNameUtil.match here, because we want to ensure we store qualified values there
                // (and newValues are all qualified).
                EqualsChecker<PrismReferenceValue> comparator =
                        (a, b) -> Objects.equals(a.getOid(), b.getOid())
                                && Objects.equals(a.getRelation(), b.getRelation())
                                && a.getValueMetadata().equals(b.getValueMetadata(), EquivalenceStrategy.REAL_VALUE);
                if (MiscUtil.unorderedCollectionEquals(targetState, existingValues, comparator)) {
                    return;
                }
            }
        }

        PrismReferenceDefinition itemDef = focusContext.getObjectDefinition()
                .findItemDefinition(itemName, PrismReferenceDefinition.class);
        ReferenceDelta itemDelta = prismContext.deltaFactory().reference().create(itemName, itemDef);
        itemDelta.setValuesToReplace(targetState);
        focusContext.swallowToSecondaryDelta(itemDelta);
    }

    /** If we find the same assignment path value metadata, we want to preserve original creation date. */
    private void adoptExistingStorageCreateTimestampValueMetadata(
            Collection<PrismReferenceValue> targetState, List<PrismReferenceValue> existingValues) {
        for (PrismReferenceValue ref : targetState) {
            PrismReferenceValue oldRefMatch = MiscUtil.find(existingValues, ref,
                    (a, b) -> Objects.equals(a.getOid(), b.getOid())
                            && Objects.equals(a.getRelation(), b.getRelation()));
            if (oldRefMatch != null) {
                adoptExistingStorageCreateTimestampValueMetadata(ref, oldRefMatch);
            }
        }
    }

    private void adoptExistingStorageCreateTimestampValueMetadata(PrismReferenceValue ref, PrismReferenceValue originalRef) {
        List<PrismContainerValue<Containerable>> originalMetadataValues = originalRef.getValueMetadata().getValues();
        if (originalMetadataValues.isEmpty()) {
            return;
        }

        for (PrismContainerValue<Containerable> metadataValue : ref.getValueMetadata().getValues()) {
            ValueMetadataType metadata = Objects.requireNonNull(metadataValue.getRealValue());
            ProvenanceMetadataType provenance = metadata.getProvenance();
            if (provenance == null) {
                continue; // not assignment path metadata, we leave it as is
            }

            PrismContainerValue<Containerable> originalMetadataValue = originalMetadataValues.stream()
                    // We must not ignore "operational" elements. But we must ignore missing ValueMetadataType PCV IDs,
                    // as they are present in repo, but missing in newly-created values.
                    .filter(m -> provenance.asPrismContainerValue()
                            .equals(((ValueMetadataType) m.asContainerable()).getProvenance().asPrismContainerValue(),
                                    EquivalenceStrategy.DATA_ALLOWING_MISSING_IDS))
                    .findFirst()
                    .orElse(null);
            if (originalMetadataValue == null) {
                continue; // no action needed
            }
            StorageMetadataType originalStorage = Objects.requireNonNull(
                            (ValueMetadataType) (originalMetadataValue.getRealValue()))
                    .getStorage();
            if (originalStorage == null || originalStorage.getCreateTimestamp() == null) {
                continue; // strange, but nothing to do about it
            }

            // This preserves the original assignment path creation time.
            // Otherwise, each recompute would bump the time which is not what we want to show.
            metadata.getStorage().setCreateTimestamp(originalStorage.getCreateTimestamp());
        }
    }

    /**
     * This adds `newReferences` to the `accumulatedReferences` collection.
     * If any of the new references is in the accumulator collection already, any value metadata from it
     * is added to the existing reference (merged).
     */
    private void addReferences(
            Collection<PrismReferenceValue> accumulatedReferences, Collection<PrismReferenceValue> newReferences)
            throws SchemaException {
        newRefsLoop:
        for (PrismReferenceValue reference : newReferences) {
            for (PrismReferenceValue exVal : accumulatedReferences) {
                if (MiscUtil.equals(exVal.getOid(), reference.getOid())
                        && prismContext.relationsEquivalent(exVal.getRelation(), reference.getRelation())) {
                    ValueMetadata existingRefMetadata = exVal.getValueMetadata();
                    for (PrismContainerValue<Containerable> metadataValue : reference.getValueMetadata().getValues()) {
                        existingRefMetadata.add(metadataValue.clone());
                    }
                    continue newRefsLoop;
                }
            }

            // clone without full object instead of calling canonicalize()
            PrismReferenceValue ref = reference.cloneComplex(CloneStrategy.REUSE);
            if (ref.getRelation() == null || QNameUtil.isUnqualified(ref.getRelation())) {
                ref.setRelation(relationRegistry.normalizeRelation(ref.getRelation()));
            }
            accumulatedReferences.add(ref);
        }
    }

    @NotNull
    private <AH extends AssignmentHolderType> AssignmentTripleEvaluator<AH> createAssignmentTripleEvaluator(
            LensContext<AH> context, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {

        assert context.hasFocusContext();
        return new AssignmentTripleEvaluator.Builder<AH>()
                .context(context)
                .assignmentEvaluator(createAssignmentEvaluator(context, now))
                .source(context.getFocusContext().getObjectNewOrCurrentRequired().asObjectable())
                .beans(beans)
                .now(now)
                .task(task)
                .result(result)
                .build();
    }

    private <F extends AssignmentHolderType> AssignmentEvaluator<F> createAssignmentEvaluator(LensContext<F> context,
            XMLGregorianCalendar now) {
        return new AssignmentEvaluator.Builder<F>()
                .referenceResolver(referenceResolver)
                .focusOdoAbsolute(context.getFocusContext().getObjectDeltaObjectAbsolute())
                .focusOdoRelative(context.getFocusContext().getObjectDeltaObjectRelative())
                .lensContext(context)
                .now(now)
                .systemConfiguration(context.getSystemConfiguration())
                .build();
    }
}
