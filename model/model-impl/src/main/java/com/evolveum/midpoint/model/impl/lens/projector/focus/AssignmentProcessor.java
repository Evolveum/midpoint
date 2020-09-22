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

import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedAssignedResourceObjectConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedConstructionPack;
import com.evolveum.midpoint.model.impl.lens.projector.ComplexConstructionConsumer;
import com.evolveum.midpoint.model.impl.lens.projector.ConstructionProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
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
    @Autowired private ProvisioningService provisioningService;
    @Autowired private ConstructionProcessor constructionProcessor;
    @Autowired private PolicyRuleProcessor policyRuleProcessor;
    @Autowired private ContextLoader contextLoader;
    @Autowired private ModelBeans beans;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentProcessor.class);

    private static final String OP_EVALUATE_FOCUS_MAPPINGS = AssignmentProcessor.class.getName() + ".evaluateFocusMappings";
    private static final String OP_PROCESS_PROJECTIONS = AssignmentProcessor.class.getName() + ".processProjections";
    private static final String OP_DISTRIBUTE_PROJECTIONS = AssignmentProcessor.class.getName() + ".distributeProjections";

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
            result.recordEnd();
            result.setStatus(finalStatus);
            result.setMessage(message);
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
        if (focusContext.isDelete()) {
            processFocusDelete(context);
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
    }

    private <F extends AssignmentHolderType> boolean processPruning(LensContext<F> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, OperationResult result) throws SchemaException {
        PruningOperation pruningOperation = new PruningOperation<>(context, evaluatedAssignmentTriple, beans);
        return pruningOperation.execute(result);
    }

    private <AH extends AssignmentHolderType> void processProjections(LensContext<AH> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException, PolicyViolationException {

        OperationResult result = parentResult.createMinorSubresult(OP_PROCESS_PROJECTIONS);
        try {
            LOGGER.trace("Projection processing start, evaluatedAssignmentTriple:\n{}",
                    evaluatedAssignmentTriple.debugDumpLazily(1));

            ObjectDeltaObject<AH> focusOdoAbsolute = context.getFocusOdoAbsolute();

            // Evaluate the constructions in assignments now. These were not evaluated in the first pass of AssignmentEvaluator
            // because there may be interaction from focusMappings of some roles to outbound mappings of other roles.
            // Now we have complete focus with all the focusMappings so we can evaluate the constructions
            evaluateConstructions(context, evaluatedAssignmentTriple, focusOdoAbsolute, task, result);

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

            MappingSetEvaluation.TripleCustomizer<PrismValue, ItemDefinition> customizer = (triple, abstractRequest) -> {
                if (triple == null) {
                    return null;
                }
                DeltaSetTriple<ItemValueWithOrigin<PrismValue, ItemDefinition>> rv = prismContext.deltaFactory().createDeltaSetTriple();
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
                            rv.addAllToPlusSet(triple.getNonNegativeValues());
                            break;
                        case MINUS:
                            rv.addAllToMinusSet(triple.getNonPositiveValues());
                            break;
                        case ZERO:
                            rv = triple;
                            break;
                    }
                }
                return rv;
            };

            MappingSetEvaluation.EvaluatedMappingConsumer<PrismValue, ItemDefinition> mappingConsumer = (mapping, abstractRequest) -> {
                AssignedFocusMappingEvaluationRequest request = (AssignedFocusMappingEvaluationRequest) abstractRequest;
                request.getEvaluatedAssignment().addFocusMapping(mapping);
            };

            TargetObjectSpecification<AH> targetSpecification = new FixedTargetSpecification<>(focusOdoRelative.getNewObject(), true);

            MappingEvaluationEnvironment env = new MappingEvaluationEnvironment(
                    "focus mappings in assignments of " + focusContext.getHumanReadableName(),
                    now, task);

            MappingSetEvaluation<AH, AH> mappingSetEvaluation = new MappingSetEvaluationBuilder<AH, AH>()
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

            PathKeyedMap<DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> focusOutputTripleMap =
                    mappingSetEvaluation.getOutputTripleMap();

            logOutputTripleMap(focusOutputTripleMap);

            DeltaSetTripleMapConsolidation<AH> consolidation = new DeltaSetTripleMapConsolidation<>(focusOutputTripleMap,
                    focusOdoRelative.getNewObject(), focusOdoRelative.getObjectDelta(), context::primaryFocusItemDeltaExists,
                    null, null,
                    focusContext.getObjectDefinition(), env, beans, context, result);
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
            Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> focusOutputTripleMap) {
        if (LOGGER.isTraceEnabled()) {
            for (Entry<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> entry : focusOutputTripleMap
                    .entrySet()) {
                LOGGER.trace("Resulting output triple for {}:\n{}", entry.getKey(), entry.getValue().debugDump(1));
            }
        }
    }

    private <AH extends AssignmentHolderType> void distributeConstructions(LensContext<AH> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ComplexConstructionConsumer<ResourceShadowDiscriminator, EvaluatedAssignedResourceObjectConstructionImpl<AH>> consumer =
                new ComplexConstructionConsumer<ResourceShadowDiscriminator, EvaluatedAssignedResourceObjectConstructionImpl<AH>>() {

            private boolean processOnlyExistingProjContexts;

            @Override
            public boolean before(ResourceShadowDiscriminator rsd) {
                if (rsd.getResourceOid() == null) {
                    throw new IllegalStateException("Resource OID null in ResourceAccountType during assignment processing");
                }
                if (rsd.getIntent() == null) {
                    throw new IllegalStateException(
                            "Account type is null in ResourceAccountType during assignment processing");
                }

                processOnlyExistingProjContexts = false;
                if (ModelExecuteOptions.isLimitPropagation(context.getOptions())) {
                    if (context.getTriggeredResourceOid() != null
                            && !rsd.getResourceOid().equals(context.getTriggeredResourceOid())) {
                        LOGGER.trace(
                                "Skipping processing construction for shadow identified by {} because of limitation to propagate changes only for resource {}",
                                rsd, context.getTriggeredResourceOid());
                        return false;
                    }

                    if (context.getChannel() != null && SchemaConstants.CHANNEL_DISCOVERY.equals(QNameUtil.uriToQName(context.getChannel()))) {
                        LOGGER.trace(
                                "Processing of shadow identified by {} will be skipped because of limitation for discovery channel.", context.getChannel());    // TODO is this message OK? [med]
                        processOnlyExistingProjContexts = true;
                    }
                }

                return true;
            }

            @Override
            public void onAssigned(ResourceShadowDiscriminator rsd, String desc) throws SchemaException {
                LensProjectionContext projectionContext = LensUtil.getOrCreateProjectionContext(context, rsd);
                projectionContext.setAssigned(true);
                projectionContext.setAssignedOldIfUnknown(false);
                projectionContext.setLegalOldIfUnknown(false);
                if (projectionContext.getAssignmentPolicyEnforcementType() != AssignmentPolicyEnforcementType.NONE) {
                    LOGGER.trace("Projection {} legal: assigned (valid)", desc);
                    projectionContext.setLegal(true);
                } else {
                    LOGGER.trace("Projection {} skip: assigned (valid), NONE enforcement", desc);
                }
            }

            @Override
            public void onUnchangedValid(ResourceShadowDiscriminator key, String desc) throws SchemaException {
                LensProjectionContext projectionContext = context.findProjectionContext(key);
                if (projectionContext == null) {
                    if (processOnlyExistingProjContexts) {
                        LOGGER.trace("Projection {} skip: unchanged (valid), processOnlyExistingProjCxts", desc);
                        return;
                    }
                    // The projection should exist before the change but it does not
                    // This happens during reconciliation if there is an inconsistency.
                    // Pretend that the assignment was just added. That should do.
                    projectionContext = LensUtil.getOrCreateProjectionContext(context, key);
                }
                LOGGER.trace("Projection {} legal: unchanged (valid)", desc);
                projectionContext.setAssigned(true);
                projectionContext.setAssignedOldIfUnknown(true);
                if (projectionContext.getAssignmentPolicyEnforcementType() == AssignmentPolicyEnforcementType.NONE) {
                    projectionContext.setLegalOld(null);
                    projectionContext.setLegal(null);
                } else {
                    projectionContext.setLegalOldIfUnknown(true);
                    projectionContext.setLegal(true);
                }
            }

            @Override
            public void onUnchangedInvalid(ResourceShadowDiscriminator rsd, String desc) throws SchemaException {
                LensProjectionContext projectionContext = context.findProjectionContext(rsd);
                if (projectionContext == null) {
                    if (processOnlyExistingProjContexts) {
                        LOGGER.trace("Projection {} skip: unchanged (invalid), processOnlyExistingProjContexts", desc);
                    } else {
                        LOGGER.trace("Projection {} skip: unchanged (invalid) and does not exist in current lens context", desc);
                    }
                    return;
                }
                LOGGER.trace("Projection {} illegal: unchanged (invalid)", desc);
                projectionContext.setLegal(false);
                projectionContext.setLegalOldIfUnknown(false);
                projectionContext.setAssigned(false);
                projectionContext.setAssignedOldIfUnknown(false);
                if (projectionContext.getAssignmentPolicyEnforcementType() == AssignmentPolicyEnforcementType.NONE
                        || projectionContext.getAssignmentPolicyEnforcementType()
                        == AssignmentPolicyEnforcementType.POSITIVE) {
                    projectionContext.setLegalOld(null);
                    projectionContext.setLegal(null);
                } else {
                    projectionContext.setLegalOldIfUnknown(false);
                    projectionContext.setLegal(false);
                }
            }

            @Override
            public void onUnassigned(ResourceShadowDiscriminator rsd, String desc) throws SchemaException {
                if (accountExists(context, rsd)) {
                    LensProjectionContext projectionContext = context.findProjectionContext(rsd);
                    if (projectionContext == null) {
                        if (processOnlyExistingProjContexts) {
                            LOGGER.trace("Projection {} skip: unassigned, processOnlyExistingProjCxts", desc);
                            return;
                        }
                        projectionContext = LensUtil.getOrCreateProjectionContext(context, rsd);
                    }
                    projectionContext.setAssigned(false);
                    projectionContext.setAssignedOldIfUnknown(true);
                    projectionContext.setLegalOldIfUnknown(true);

                    AssignmentPolicyEnforcementType assignmentPolicyEnforcement = projectionContext
                            .getAssignmentPolicyEnforcementType();
                    // TODO: check for MARK and LEGALIZE enforcement policies ....add delete laso for relative enforcemenet
                    if (assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.FULL
                            || assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.RELATIVE) {
                        LOGGER.trace("Projection {} illegal: unassigned", desc);
                        projectionContext.setLegal(false);
                    } else if (assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.POSITIVE) {
                        LOGGER.trace("Projection {} legal: unassigned, but allowed by policy ({})", desc,
                                assignmentPolicyEnforcement);
                        projectionContext.setLegal(true);
                    } else {
                        LOGGER.trace("Projection {} legal: unassigned, policy decision postponed ({})", desc,
                                assignmentPolicyEnforcement);
                        projectionContext.setLegal(null);
                    }
                } else {

                    LOGGER.trace("Projection {} nothing: unassigned (valid->invalid) but not there", desc);
                    // We have to delete something that is not there. Nothing to do.
                }
            }

            @Override
            public void after(ResourceShadowDiscriminator rsd, String desc,
                    DeltaMapTriple<ResourceShadowDiscriminator, EvaluatedConstructionPack<EvaluatedAssignedResourceObjectConstructionImpl<AH>>> constructionMapTriple) {
                DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<AH>> projectionEvaluatedConstructionDeltaSetTriple =
                        prismContext.deltaFactory().createDeltaSetTriple(
                                getConstructions(constructionMapTriple.getZeroMap().get(rsd), true),
                                getConstructions(constructionMapTriple.getPlusMap().get(rsd), true),
                                getConstructions(constructionMapTriple.getMinusMap().get(rsd), false));
                LensProjectionContext projectionContext = context.findProjectionContext(rsd);
                if (projectionContext != null) {
                    // This can be null in a exotic case if we delete already deleted account
                    LOGGER.trace("Construction delta set triple for {}:\n{}", rsd,
                            projectionEvaluatedConstructionDeltaSetTriple.debugDumpLazily(1));
                    projectionContext.setEvaluatedConstructionDeltaSetTriple(projectionEvaluatedConstructionDeltaSetTriple);
                    if (isForceRecon(constructionMapTriple.getZeroMap().get(rsd)) || isForceRecon(
                            constructionMapTriple.getPlusMap().get(rsd)) || isForceRecon(
                            constructionMapTriple.getMinusMap().get(rsd))) {
                        projectionContext.setDoReconciliation(true);
                    }
                }
            }

        };

        OperationResult result = parentResult.createMinorSubresult(OP_DISTRIBUTE_PROJECTIONS);
        try {
            constructionProcessor.processConstructions(evaluatedAssignmentTriple,
                    EvaluatedAssignmentImpl::getConstructionTriple,
                    construction -> getConstructionMapKey(context, construction, task, result),
                    consumer);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // @pre: construction was already evaluated and is not ignored (i.e. has resource)
    private <AH extends AssignmentHolderType> ResourceShadowDiscriminator getConstructionMapKey(LensContext<AH> context,
            EvaluatedAssignedResourceObjectConstructionImpl<AH> evaluatedConstruction, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        String resourceOid = evaluatedConstruction.getConstruction().getResourceOid();
        String intent = evaluatedConstruction.getIntent();
        ShadowKindType kind = evaluatedConstruction.getKind();
        String tag = evaluatedConstruction.getTag();
        ResourceType resource = LensUtil.getResourceReadOnly(context, resourceOid, provisioningService, task, result);
        intent = LensUtil.refineProjectionIntent(kind, intent, resource, prismContext);
        return new ResourceShadowDiscriminator(resourceOid, kind, intent, tag, false);
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

    @Deprecated
    private <AH extends AssignmentHolderType> AssignmentHolderType determineSource(LensFocusContext<AH> focusContext) {
        // The existing algorithm was quite obscure. Let's do it in a simple way.
        return focusContext.getObjectNew().asObjectable();

//        ObjectDelta<AH> delta = focusContext.getWaveDelta(focusContext.getLensContext().getExecutionWave());
//        if (delta != null && !delta.isEmpty()) {
//            return focusContext.getObjectNew().asObjectable();
//        }
//
//        if (focusContext.getObjectCurrent() != null) {
//            return focusContext.getObjectCurrent().asObjectable();
//        }
//
//        return focusContext.getObjectNew().asObjectable();
    }

    private <AH extends AssignmentHolderType> void evaluateConstructions(LensContext<AH> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple,
            ObjectDeltaObject<AH> focusOdoAbsolute, Task task, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, SecurityViolationException, ConfigurationException, CommunicationException {
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
        Iterator<EvaluatedAssignmentImpl<F>> iterator = evaluatedAssignments.iterator();
        while (iterator.hasNext()) {
            EvaluatedAssignmentImpl<F> evaluatedAssignment = iterator.next();
            try {
                evaluatedAssignment.evaluateConstructions(focusOdoAbsolute, context::rememberResource, task, result);
            } catch (ObjectNotFoundException ex) {
                LOGGER.trace("Processing of assignment resulted in error {}: {}", ex,
                        SchemaDebugUtil.prettyPrint(evaluatedAssignment.getAssignmentType()));
                iterator.remove(); // TODO this is cruel! Review this. MID-6401
                if (!ModelExecuteOptions.isForce(context.getOptions())) {
                    ModelImplUtils.recordFatalError(result, ex);
                }
            } catch (SchemaException ex) {
                LOGGER.trace("Processing of assignment resulted in error {}: {}", ex,
                        SchemaDebugUtil.prettyPrint(evaluatedAssignment.getAssignmentType()));
                ModelImplUtils.recordFatalError(result, ex);
                String resourceOid = FocusTypeUtil.determineConstructionResource(evaluatedAssignment.getAssignmentType());
                if (resourceOid == null) {
                    // This is a role assignment or something like that. Just throw the original exception for now.
                    throw ex;
                }
                ResourceShadowDiscriminator rad = new ResourceShadowDiscriminator(resourceOid,
                        FocusTypeUtil.determineConstructionKind(evaluatedAssignment.getAssignmentType()),
                        FocusTypeUtil.determineConstructionIntent(evaluatedAssignment.getAssignmentType()),
                        null, false);
                LensProjectionContext accCtx = context.findProjectionContext(rad);
                if (accCtx != null) {
                    accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
                }
                iterator.remove(); // TODO this is cruel! Review this. MID-6401
            }
        }
    }

    /**
     * Simply mark all projections as illegal - except those that are being unlinked
     */
    private <F extends AssignmentHolderType> void processFocusDelete(LensContext<F> context) {
        for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK) {
                // We do not want to affect unlinked projections
                continue;
            }
            projectionContext.setLegal(false);
            projectionContext.setLegalOldIfUnknown(true);
        }
    }

    private <AH extends AssignmentHolderType> Collection<EvaluatedAssignedResourceObjectConstructionImpl<AH>> getConstructions(
            EvaluatedConstructionPack<EvaluatedAssignedResourceObjectConstructionImpl<AH>> accountEvaluatedConstructionPack, boolean validOnly) {
        if (accountEvaluatedConstructionPack == null) {
            return Collections.emptySet();
        }
        if (validOnly && !accountEvaluatedConstructionPack.hasValidAssignment()) {
            return Collections.emptySet();
        }
        return accountEvaluatedConstructionPack.getEvaluatedConstructions();
    }

    private boolean isForceRecon(EvaluatedConstructionPack accountEvaluatedConstructionPack) {
        return accountEvaluatedConstructionPack != null && accountEvaluatedConstructionPack.isForceRecon();
    }

    /**
     * Set 'legal' flag for the accounts that does not have it already
     */
    private <F extends AssignmentHolderType> void finishLegalDecisions(LensContext<F> context) throws PolicyViolationException, SchemaException {
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

                AssignmentPolicyEnforcementType enforcementType = projectionContext.getAssignmentPolicyEnforcementType();

                if (enforcementType == AssignmentPolicyEnforcementType.FULL) {
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

                } else if (enforcementType == AssignmentPolicyEnforcementType.NONE && !projectionContext.isTombstone()) {
                    if (projectionContext.isAdd()) {
                        LOGGER.trace("Projection {} legal: added in NONE policy", desc);
                        projectionContext.setLegal(true);
                        projectionContext.setLegalOldIfUnknown(false);
                    } else {
                        if (projectionContext.isExists()) {
                            LOGGER.trace("Projection {} legal: exists in NONE policy", desc);
                        } else {
                            LOGGER.trace("Projection {} illegal: does not exists in NONE policy", desc);
                        }
                        // Everything that exists was legal and is legal. Nothing really changes.
                        projectionContext.setLegal(projectionContext.isExists());
                        projectionContext.setLegalOldIfUnknown(projectionContext.isExists());
                    }

                } else if (enforcementType == AssignmentPolicyEnforcementType.POSITIVE && !projectionContext.isTombstone()) {
                    // Everything that is not yet dead is legal in POSITIVE enforcement mode
                    LOGGER.trace("Projection {} legal: not dead in POSITIVE policy", desc);
                    projectionContext.setLegal(true);
                    projectionContext.setLegalOldIfUnknown(true);

                } else if (enforcementType == AssignmentPolicyEnforcementType.RELATIVE && !projectionContext.isTombstone() &&
                        projectionContext.isLegal() == null && projectionContext.isLegalOld() == null) {
                    // RELATIVE mode and nothing has changed. Maintain status quo. Pretend that it is legal.
                    LOGGER.trace("Projection {} legal: no change in RELATIVE policy", desc);
                    projectionContext.setLegal(true);
                    projectionContext.setLegalOldIfUnknown(true);
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Finishing legal decision for {}, thombstone {}, enforcement mode {}, legalize {}: {} -> {}",
                        projectionContext.toHumanReadableString(), projectionContext.isTombstone(),
                        projectionContext.getAssignmentPolicyEnforcementType(),
                        projectionContext.isLegalize(), projectionContext.isLegalOld(), projectionContext.isLegal());
            }

            propagateLegalDecisionToHigherOrders(context, projectionContext);
        }
    }

    private <F extends ObjectType> void propagateLegalDecisionToHigherOrders(
            LensContext<F> context, LensProjectionContext refProjCtx) {
        ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
        if (refDiscr == null) {
            return;
        }
        for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
            ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
            if (aDiscr != null && refDiscr.equivalent(aDiscr) && (refDiscr.getOrder() < aDiscr.getOrder())) {
                aProjCtx.setLegal(refProjCtx.isLegal());
                aProjCtx.setLegalOldIfUnknown(refProjCtx.isLegalOld());
                aProjCtx.setExists(refProjCtx.isExists());
            }
        }
    }

    private <F extends AssignmentHolderType> void createAssignmentDelta(LensContext<F> context, LensProjectionContext accountContext) throws SchemaException{
        Class<F> focusClass = context.getFocusClass();
        ContainerDelta<AssignmentType> assignmentDelta = prismContext.deltaFactory().container()
                .createDelta(AssignmentHolderType.F_ASSIGNMENT, focusClass);
        AssignmentType assignment = new AssignmentType();
        ConstructionType constructionType = new ConstructionType();
        constructionType.setResourceRef(ObjectTypeUtil.createObjectRef(accountContext.getResource(), prismContext));
        assignment.setConstruction(constructionType);
        //noinspection unchecked
        assignmentDelta.addValueToAdd(assignment.asPrismContainerValue());
        PrismContainerDefinition<AssignmentType> containerDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass).findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
        assignmentDelta.applyDefinition(containerDefinition);
        context.getFocusContext().swallowToSecondaryDelta(assignmentDelta);

    }

    @ProcessorMethod
    <F extends ObjectType> void processOrgAssignments(LensContext<F> context,
            XMLGregorianCalendar now, Task task,
            OperationResult result) throws SchemaException, PolicyViolationException {
        LensFocusContext<F> focusContext = context.getFocusContext();

        Collection<PrismReferenceValue> shouldBeParentOrgRefs = new ArrayList<>();

        DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null) {
            return; // could be if "assignments" step is skipped
        }
        for (EvaluatedAssignmentImpl<?> evalAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
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
                                        throw new TunnelException(new PolicyViolationException("Attempt to add parentOrgRef "+val.getOid()+", but it is not allowed by assignments"));
                                    }
                                    break;
                                case MINUS:
                                    if (PrismValueCollectionsUtil.containsRealValue(shouldBeParentOrgRefs, val)) {
                                        throw new TunnelException(new PolicyViolationException("Attempt to delete parentOrgRef "+val.getOid()+", but it is mandated by assignments"));
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
                        throw new PolicyViolationException("Two different tenants ("+tenantOid+", "+assignmentTenantOid+") applicable to "+context.getFocusContext().getHumanReadableName());
                    }
                }
            }

        }

        addTenantRefDelta(context, objectNew, tenantOid);
    }

    private <F extends ObjectType> void addTenantRefDelta(LensContext<F> context, PrismObject<F> objectNew, String tenantOid) throws SchemaException {
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
                ReferenceDelta tenantRefDelta = prismContext.deltaFactory().reference().createModificationReplace(ObjectType.F_TENANT_REF, focusContext.getObjectDefinition(), (PrismReferenceValue)null);
                focusContext.swallowToSecondaryDelta(tenantRefDelta);
            } else {
                if (!tenantOid.equals(currentTenantRef.getOid())) {
                    LOGGER.trace("Changing tenantRef to {}", tenantOid);
                    ReferenceDelta tenantRefDelta = prismContext.deltaFactory().reference().createModificationReplace(ObjectType.F_TENANT_REF, focusContext.getObjectDefinition(), tenantOid);
                    focusContext.swallowToSecondaryDelta(tenantRefDelta);
                }
            }
        }
    }

    @ProcessorMethod
    <F extends ObjectType> void checkForAssignmentConflicts(LensContext<F> context,
            XMLGregorianCalendar now, Task task, OperationResult result) throws PolicyViolationException, SchemaException {
        for(LensProjectionContext projectionContext: context.getProjectionContexts()) {
            if (AssignmentPolicyEnforcementType.NONE == projectionContext.getAssignmentPolicyEnforcementType()) {
                continue;
            }
            if (projectionContext.isTombstone()) {
                continue;
            }
            if (Boolean.TRUE.equals(projectionContext.isAssigned())) {
                ObjectDelta<ShadowType> projectionPrimaryDelta = projectionContext.getPrimaryDelta();
                if (projectionPrimaryDelta != null) {
                    if (projectionPrimaryDelta.isDelete()) {
                        throw new PolicyViolationException("Attempt to delete "+projectionContext.getHumanReadableName()+" while " +
                                "it is assigned violates an assignment policy");
                    }
                }
            }
        }
    }


    public void processAssignmentsAccountValues(LensProjectionContext accountContext, OperationResult result) throws SchemaException,
        ObjectNotFoundException, ExpressionEvaluationException {

        // TODO: reevaluate constructions
        // This should re-evaluate all the constructions. They are evaluated already, evaluated in the assignment step before.
        // But if there is any iteration counter that it will not be taken into account

    }

    private String dumpAccountMap(Map<ResourceShadowDiscriminator, EvaluatedConstructionPack> accountMap) {
        StringBuilder sb = new StringBuilder();
        Set<Entry<ResourceShadowDiscriminator, EvaluatedConstructionPack>> entrySet = accountMap.entrySet();
        Iterator<Entry<ResourceShadowDiscriminator, EvaluatedConstructionPack>> i = entrySet.iterator();
        while (i.hasNext()) {
            Entry<ResourceShadowDiscriminator, EvaluatedConstructionPack> entry = i.next();
            sb.append(entry.getKey()).append(": ");
            sb.append(entry.getValue());
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private <F extends ObjectType> boolean accountExists(LensContext<F> context, ResourceShadowDiscriminator rat) {
        LensProjectionContext accountSyncContext = context.findProjectionContext(rat);
        if (accountSyncContext == null) {
            return false;
        }
        if (accountSyncContext.getObjectCurrent() == null) {
            return false;
        }
        return true;
    }

    @ProcessorMethod
    <F extends ObjectType> void processMembershipAndDelegatedRefs(LensContext<F> context,
            XMLGregorianCalendar now,
            Task task,
            OperationResult result)
            throws SchemaException, ConfigurationException {
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
        for (EvaluatedAssignmentImpl<?> evalAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
            if (evalAssignment.isValid()) {
                addReferences(shouldBeRoleRefs, evalAssignment.getMembershipRefVals());
                addReferences(shouldBeDelegatedRefs, evalAssignment.getDelegationRefVals());
                addReferences(shouldBeArchetypeRefs, evalAssignment.getArchetypeRefVals());
            }
        }

        if (shouldBeArchetypeRefs.size() > 1) {
            throw new ConfigurationException("Only single archetype supported. Attempting to add " + shouldBeArchetypeRefs.size() + ": " + shouldBeArchetypeRefs);
        }

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
                .source(determineSource(context.getFocusContext()))
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
