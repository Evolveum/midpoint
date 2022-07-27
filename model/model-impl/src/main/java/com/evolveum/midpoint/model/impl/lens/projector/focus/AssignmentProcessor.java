/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.*;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.construction.ConstructionTargetKey;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedAssignedResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedConstructionPack;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.projector.ComplexConstructionConsumer;
import com.evolveum.midpoint.model.impl.lens.projector.ConstructionProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.*;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.util.ConstructionTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType.*;

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

    @Autowired
    @Qualifier("modelObjectResolver")
    private ObjectResolver objectResolver;

    @Autowired private ReferenceResolver referenceResolver;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private PrismContext prismContext;
    @Autowired private MappingFactory mappingFactory;
    @Autowired private MappingEvaluator mappingEvaluator;
    @Autowired private ActivationComputer activationComputer;
    @Autowired private ConstructionProcessor constructionProcessor;
    @Autowired private PolicyRuleProcessor policyRuleProcessor;
    @Autowired private ContextLoader contextLoader;
    @Autowired private ModelBeans beans;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentProcessor.class);

    private static final String OP_EVALUATE_FOCUS_MAPPINGS = AssignmentProcessor.class.getName() + ".evaluateFocusMappings";
    private static final String OP_PROCESS_PROJECTIONS = AssignmentProcessor.class.getName() + ".processProjections";
    private static final String OP_DISTRIBUTE_CONSTRUCTIONS = AssignmentProcessor.class.getName() + ".distributeConstructions";

    /**
     * Processing all the assignments.
     */
    @ProcessorMethod
    public <O extends ObjectType, AH extends AssignmentHolderType> void processAssignments(LensContext<O> context, XMLGregorianCalendar now,
            Task task, OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        assert context.hasFocusOfType(AssignmentHolderType.class);

        OperationResult result = parentResult.createSubresult(AssignmentProcessor.class.getName() + ".processAssignments");
        try {
            try {
                //noinspection unchecked
                processAssignmentsInternal((LensContext<AH>) context, now, task, result);
            } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | PolicyViolationException |
                    CommunicationException | ConfigurationException | SecurityViolationException | RuntimeException | Error e) {
                result.recordFatalError(e);
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
            result.cleanupResult();
        } catch (Throwable t) { // shouldn't occur -- just in case
            result.recordFatalError(t);
            throw t;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <AH extends AssignmentHolderType> void processAssignmentsInternal(LensContext<AH> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

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
        policyRuleProcessor.addGlobalPolicyRulesToAssignments(context, evaluatedAssignmentTriple, task, result);
        context.setEvaluatedAssignmentTriple((DeltaSetTriple)evaluatedAssignmentTriple);

        LOGGER.trace("evaluatedAssignmentTriple:\n{}", evaluatedAssignmentTriple.debugDumpLazily());

        // PROCESSING POLICIES

        policyRuleProcessor.evaluateAssignmentPolicyRules(context, evaluatedAssignmentTriple, task, result);
        boolean needToReevaluateAssignments = processPruning(context, evaluatedAssignmentTriple, result);

        if (needToReevaluateAssignments) {
            LOGGER.debug("Re-evaluating assignments because exclusion pruning rule was triggered");

            assignmentTripleEvaluator.reset(true);
            evaluatedAssignmentTriple = assignmentTripleEvaluator.processAllAssignments();
            context.setEvaluatedAssignmentTriple((DeltaSetTriple)evaluatedAssignmentTriple);

            // TODO implement isMemberOf invocation result change check here! MID-5784
            //  Actually, we should factor out the relevant code to avoid code duplication.

            policyRuleProcessor.addGlobalPolicyRulesToAssignments(context, evaluatedAssignmentTriple, task, result);

            LOGGER.trace("re-evaluatedAssignmentTriple:\n{}", evaluatedAssignmentTriple.debugDumpLazily());

            policyRuleProcessor.evaluateAssignmentPolicyRules(context, evaluatedAssignmentTriple, task, result);
        }

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

    private <AH extends AssignmentHolderType> void evaluateFocusMappings(LensContext<AH> context, XMLGregorianCalendar now,
            LensFocusContext<AH> focusContext,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple, Task task,
            OperationResult parentResult) throws SchemaException, ExpressionEvaluationException, PolicyViolationException,
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
                DeltaSetTriple<ItemValueWithOrigin<PrismValue, ItemDefinition<?>>> rv = prismContext.deltaFactory().createDeltaSetTriple();
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
                if (resultingMode != null) {
                    switch (resultingMode) {
                        case PLUS:
                            rv.addAllToPlusSet(triple.getNonNegativeValues()); // MID-6403
                            break;
                        case MINUS:
                            rv.addAllToMinusSet(triple.getNonPositiveValues()); // MID-6403
                            break;
                        case ZERO:
                            rv = triple;
                            break;
                    }
                }
                return rv;
            };

            FocalMappingSetEvaluation.EvaluatedMappingConsumer mappingConsumer = (mapping, abstractRequest) -> {
                AssignedFocusMappingEvaluationRequest request = (AssignedFocusMappingEvaluationRequest) abstractRequest;
                request.getEvaluatedAssignment().addFocusMapping(mapping);
            };

            TargetObjectSpecification<AH> targetSpecification = new FixedTargetSpecification<>(focusOdoRelative.getNewObject(), true);

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

            PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> focusOutputTripleMap =
                    mappingSetEvaluation.getOutputTripleMap();

            logOutputTripleMap(focusOutputTripleMap);

            DeltaSetTripleMapConsolidation<AH> consolidation = new DeltaSetTripleMapConsolidation<>(
                    focusOutputTripleMap,
                    focusOdoRelative.getNewObject(),
                    focusOdoRelative.getObjectDelta(),
                    context::primaryFocusItemDeltaExists,
                    null,
                    null,
                    focusContext.getObjectDefinition(),
                    env,
                    beans,
                    context,
                    result);
            consolidation.computeItemDeltas();
            Collection<ItemDelta<?, ?>> focusDeltas = consolidation.getItemDeltas();

            LOGGER.trace("Computed focus deltas: {}", focusDeltas);
            focusContext.swallowToSecondaryDelta(focusDeltas);
            focusContext.recompute();
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void logOutputTripleMap(
            Map<ItemPath, DeltaSetTriple<ItemValueWithOrigin<?, ?>>> focusOutputTripleMap) {
        if (LOGGER.isTraceEnabled()) {
            for (Entry<ItemPath, DeltaSetTriple<ItemValueWithOrigin<?, ?>>> entry : focusOutputTripleMap
                    .entrySet()) {
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
                        LensProjectionContext projectionContext = LensContext.getOrCreateProjectionContext(context, key).context;
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
                        LensProjectionContext projectionContext = context.findFirstProjectionContext(key);
                        if (projectionContext == null) {
                            if (processOnlyExistingProjContexts) {
                                LOGGER.trace("Projection {} skip: unchanged (valid), processOnlyExistingProjContexts", desc);
                                return;
                            }
                            // The projection should exist before the change but it does not
                            // This happens during reconciliation if there is an inconsistency.
                            // Pretend that the assignment was just added. That should do.
                            projectionContext = LensContext.getOrCreateProjectionContext(context, key).context;
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
                        LensProjectionContext projectionContext = context.findFirstProjectionContext(key);
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
                        LensProjectionContext projectionContext = context.findFirstProjectionContext(key);
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
                        LensProjectionContext projectionContext = context.findFirstProjectionContext(key);
                        if (projectionContext != null) {
                            // This can be null in a exotic case if we delete already deleted account
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
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
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
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {

            String desc = projectionContext.toHumanReadableString();

            if (projectionContext.isLegal() != null) {
                // already have decision
                LOGGER.trace("Projection {} legal={} (predetermined)", desc, projectionContext.isLegal());
                propagateLegalDecisionToHigherOrders(context, projectionContext);
                continue;
            }

            if (projectionContext.isLegalize()){
                LOGGER.trace("Projection {} legal: legalized", desc);
                createAssignmentDelta(context, projectionContext);
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
                        throw new PolicyViolationException("Attempt to add projection "+projectionContext.toHumanReadableString()
                                +" while the synchronization enforcement policy is FULL and the projection is not assigned");
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
                LOGGER.trace("Finishing legal decision for {}, gone {}, enforcement mode {}, legalize {}: {} -> {}",
                        projectionContext.toHumanReadableString(), projectionContext.isGone(),
                        projectionContext.getAssignmentPolicyEnforcementMode(),
                        projectionContext.isLegalize(), projectionContext.isLegalOld(), projectionContext.isLegal());
            }

            propagateLegalDecisionToHigherOrders(context, projectionContext);
        }
    }

    private <F extends ObjectType> void propagateLegalDecisionToHigherOrders(
            LensContext<F> context, LensProjectionContext refProjCtx) {
        ProjectionContextKey key = refProjCtx.getKey();
        for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
            ProjectionContextKey aKey = aProjCtx.getKey();
            if (key.equivalent(aKey) && key.getOrder() < aKey.getOrder()) {
                aProjCtx.setLegal(refProjCtx.isLegal());
                aProjCtx.setLegalOldIfUnknown(refProjCtx.isLegalOld());
                aProjCtx.setExists(refProjCtx.isExists());
            }
        }
    }

    private <F extends AssignmentHolderType> void createAssignmentDelta(
            LensContext<F> context, LensProjectionContext accountContext) throws SchemaException {
        Class<F> focusClass = context.getFocusClass();
        ContainerDelta<AssignmentType> assignmentDelta = prismContext.deltaFactory().container()
                .createDelta(AssignmentHolderType.F_ASSIGNMENT, focusClass);
        AssignmentType assignment = new AssignmentType();
        ConstructionType constructionType = new ConstructionType();
        constructionType.setResourceRef(ObjectTypeUtil.createObjectRef(accountContext.getResource(), prismContext));
        assignment.setConstruction(constructionType);
        //noinspection unchecked
        assignmentDelta.addValueToAdd(assignment.asPrismContainerValue());
        PrismContainerDefinition<AssignmentType> containerDefinition =
                prismContext.getSchemaRegistry()
                        .findObjectDefinitionByCompileTimeClass(focusClass)
                        .findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
        assignmentDelta.applyDefinition(containerDefinition);
        context.getFocusContext().swallowToSecondaryDelta(assignmentDelta);

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
                        (plusMinusZero,val) -> {
                            switch (plusMinusZero) {
                                case PLUS:
                                case ZERO:
                                    if (!PrismValueCollectionsUtil.containsRealValue(shouldBeParentOrgRefs, val)) {
                                        throw new TunnelException(
                                                new PolicyViolationException(
                                                        "Attempt to add parentOrgRef " + val.getOid()
                                                                + ", but it is not allowed by assignments"));
                                    }
                                    break;
                                case MINUS:
                                    if (PrismValueCollectionsUtil.containsRealValue(shouldBeParentOrgRefs, val)) {
                                        throw new TunnelException(
                                                new PolicyViolationException(
                                                        "Attempt to delete parentOrgRef " + val.getOid()
                                                                + ", but it is mandated by assignments"));
                                    }
                                    break;
                            }
                        }, parentOrgRefCurrentValues);

                } catch (TunnelException e) {
                    throw (PolicyViolationException)e.getCause();
                }
            }
        }

        computeTenantRef(context);
    }

    private <F extends ObjectType> void computeTenantRef(LensContext<F> context) throws PolicyViolationException, SchemaException {
        String tenantOid = null;
        LensFocusContext<F> focusContext = context.getFocusContext();
        PrismObject<F> objectNew = focusContext.getObjectNew();
        if (objectNew == null) {
            return;
        }

        if (objectNew.canRepresent(OrgType.class) && BooleanUtils.isTrue(((OrgType)objectNew.asObjectable()).isTenant())) {
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
                                ObjectType.F_TENANT_REF, focusContext.getObjectDefinition(), (PrismReferenceValue)null);
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
        for(LensProjectionContext projectionContext: context.getProjectionContexts()) {
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
            return;    // could be if the "assignments" step is skipped
        }
        // Similar code is in AssignmentEvaluator.isMemberOfInvocationResultChanged -- check that if changing the business logic
        for (EvaluatedAssignmentImpl<?> evalAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) { // MID-6403
            if (evalAssignment.isValid()) {
                LOGGER.trace("Adding references from: {}", evalAssignment);
                addReferences(shouldBeRoleRefs, evalAssignment.getMembershipRefVals());
                addReferences(shouldBeDelegatedRefs, evalAssignment.getDelegationRefVals());
                addReferences(shouldBeArchetypeRefs, evalAssignment.getArchetypeRefVals());
            } else {
                LOGGER.trace("Skipping because invalid: {}", evalAssignment);
            }
        }

        //TODO check for structural archetypes?
//        if (shouldBeArchetypeRefs.size() > 1) {
//            throw new ConfigurationException("Only single archetype supported. Attempting to add " + shouldBeArchetypeRefs.size() + ": " + shouldBeArchetypeRefs);
//        }

        setReferences(focusContext, AssignmentHolderType.F_ROLE_MEMBERSHIP_REF, shouldBeRoleRefs);
        setReferences(focusContext, AssignmentHolderType.F_DELEGATED_REF, shouldBeDelegatedRefs);
        setReferences(focusContext, AssignmentHolderType.F_ARCHETYPE_REF, shouldBeArchetypeRefs);

        context.recompute(); // really needed?
    }

    private <F extends ObjectType> void setReferences(LensFocusContext<F> focusContext, QName name,
            Collection<PrismReferenceValue> targetState) throws SchemaException {

        ItemName itemName = ItemName.fromQName(name);
        PrismObject<F> focusOld = focusContext.getObjectOld();
        if (focusOld == null) {
            if (targetState.isEmpty()) {
                return;
            }
        } else {
            PrismReference existingState = focusOld.findReference(itemName);
            if (existingState == null || existingState.isEmpty()) {
                if (targetState.isEmpty()) {
                    return;
                }
            } else {
                // we don't use QNameUtil.match here, because we want to ensure we store qualified values there
                // (and newValues are all qualified)
                Comparator<PrismReferenceValue> comparator =
                        (a, b) -> 2*a.getOid().compareTo(b.getOid())
                                + (Objects.equals(a.getRelation(), b.getRelation()) ? 0 : 1);
                if (MiscUtil.unorderedCollectionCompare(targetState, existingState.getValues(), comparator)) {
                    return;
                }
            }
        }

        PrismReferenceDefinition itemDef = focusContext.getObjectDefinition().findItemDefinition(itemName, PrismReferenceDefinition.class);
        ReferenceDelta itemDelta = prismContext.deltaFactory().reference().create(itemName, itemDef);
        itemDelta.setValuesToReplace(targetState);
        focusContext.swallowToSecondaryDelta(itemDelta);
    }

    private void addReferences(Collection<PrismReferenceValue> extractedReferences, Collection<PrismReferenceValue> references) {
        for (PrismReferenceValue reference: references) {
            boolean found = false;
            for (PrismReferenceValue exVal: extractedReferences) {
                if (MiscUtil.equals(exVal.getOid(), reference.getOid())
                        && prismContext.relationsEquivalent(exVal.getRelation(), reference.getRelation())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                PrismReferenceValue ref = reference.cloneComplex(CloneStrategy.REUSE);        // clone without full object instead of calling canonicalize()
                if (ref.getRelation() == null || QNameUtil.isUnqualified(ref.getRelation())) {
                    ref.setRelation(relationRegistry.normalizeRelation(ref.getRelation()));
                }
                extractedReferences.add(ref);
            }
        }
    }

    @NotNull
    private <AH extends AssignmentHolderType> AssignmentTripleEvaluator<AH> createAssignmentTripleEvaluator(
            LensContext<AH> context, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {

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
                .channel(context.getChannel())
                .modelBeans(beans)
                .objectResolver(objectResolver)
                .systemObjectCache(systemObjectCache)
                .relationRegistry(relationRegistry)
                .prismContext(prismContext)
                .mappingFactory(mappingFactory)
                .mappingEvaluator(mappingEvaluator)
                .contextLoader(contextLoader)
                .activationComputer(activationComputer)
                .now(now)
                .systemConfiguration(context.getSystemConfiguration())
                .build();
    }
}
