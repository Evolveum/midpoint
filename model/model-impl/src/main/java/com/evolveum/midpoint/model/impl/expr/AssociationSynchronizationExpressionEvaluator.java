/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;
import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil.mergeCorrelationDefinition;
import static com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil.isMembershipSyncInboundDisabled;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.util.MiscUtil.castSafely;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.InboundMappingContextSpecification;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlation.SimplifiedCorrelationResult;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlation.CorrelatorContextCreator;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.DefaultSingleShadowInboundsProcessingContextImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessing;
import com.evolveum.midpoint.model.impl.sync.ItemSynchronizationState;
import com.evolveum.midpoint.model.impl.sync.PreMappingsEvaluator;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceObjectInboundDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition.ItemSynchronizationReactionDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Synchronizes association values by correlating and mapping them to values of respective focus item - currently
 * fixed to {@link AssignmentType}.
 */
class AssociationSynchronizationExpressionEvaluator
        extends AbstractExpressionEvaluator<
        PrismContainerValue<AssignmentType>,
        PrismContainerDefinition<AssignmentType>,
        AssociationSynchronizationExpressionEvaluatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationSynchronizationExpressionEvaluatorFactory.class);

    private static final String OP_PROCESS_ASSOCIATION_VALUE =
            AssociationSynchronizationExpressionEvaluator.class.getName() + ".processAssociationValue";

    private Evaluation lastEvaluation;

    AssociationSynchronizationExpressionEvaluator(
            QName elementName,
            AssociationSynchronizationExpressionEvaluatorType evaluatorBean,
            PrismContainerDefinition<AssignmentType> outputDefinition,
            Protector protector) {
        super(elementName, evaluatorBean, outputDefinition, protector);
    }

    @Override
    public AssociationSynchronizationResult<PrismContainerValue<AssignmentType>> evaluate(
            ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        checkEvaluatorProfile(context);

        var defaultSource = stateNonNull(context.getDefaultSource(), "No default source");
        var associationDefinition =
                castSafely(
                        stateNonNull(defaultSource.getDefinition(), "No association definition"),
                        ShadowAssociationDefinition.class);

        var inputTriple = defaultSource.getDeltaSetTriple();

        // Currently we take only non-negative values
        Collection<? extends PrismValue> inputValues = inputTriple != null ? inputTriple.getNonNegativeValues() : List.of();

        // Actually, this should be called only once; at least for mappings
        lastEvaluation = new Evaluation(inputValues, associationDefinition, context);
        return lastEvaluation.process(result);
    }

    @Override
    public boolean doesVetoTargetValueRemoval(@NotNull PrismContainerValue<AssignmentType> value, @NotNull OperationResult result) {
        return stateNonNull(lastEvaluation, "No last evaluation?")
                .doesVetoTargetValueRemoval(value, result);
    }

    class Evaluation {

        @NotNull private final Collection<? extends PrismValue> inputValues;
        @NotNull private final AssociationSynchronizationResult<PrismContainerValue<AssignmentType>> evaluatorResult =
                new AssociationSynchronizationResult<>();
        @NotNull private final ShadowAssociationDefinition associationDefinition;
        @NotNull private final ExpressionEvaluationContext context;

        @NotNull private final LensProjectionContext projectionContext =
                (LensProjectionContext) ModelExpressionThreadLocalHolder.getProjectionContextRequired();
        @NotNull private final ResourceType resource = projectionContext.getResourceRequired();

        @NotNull private final ResourceObjectInboundDefinition inboundDefinition;

        @NotNull private final Collection<AssignmentType> candidateAssignments;

        Evaluation(
                @NotNull Collection<? extends PrismValue> inputValues,
                @NotNull ShadowAssociationDefinition associationDefinition,
                @NotNull ExpressionEvaluationContext context)
                throws ConfigurationException {
            this.inputValues = inputValues;
            this.associationDefinition = associationDefinition;
            this.context = context;
            this.inboundDefinition =
                    ResourceObjectInboundDefinition.forAssociationSynchronization(
                            associationDefinition,
                            expressionEvaluatorBean,
                            context.getTargetDefinitionBean());
            this.candidateAssignments = getCandidateAssignments();
        }

        public AssociationSynchronizationResult<PrismContainerValue<AssignmentType>> process(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                ConfigurationException, ObjectNotFoundException {

            LOGGER.trace("Processing {} individual values of the association '{}'",
                    inputValues.size(), associationDefinition.getItemName());

            for (var inputValue : inputValues) {
                var associationValue = (ShadowAssociationValue) inputValue;
                LOGGER.trace("Processing association value: {}", associationValue);
                new ValueProcessing(associationValue)
                        .process(result);
            }
            return evaluatorResult;
        }

        private @NotNull Collection<AssignmentType> getCandidateAssignments() {
            var focusContext = ModelExpressionThreadLocalHolder.getLensContextRequired().getFocusContextRequired();
            var objectNew = asObjectable(focusContext.getObjectNew());
            if (!(objectNew instanceof AssignmentHolderType assignmentHolder)) {
                return List.of();
            }
            var assignments = assignmentHolder.getAssignment();
            var targetBean = context.getTargetDefinitionBean();
            if (targetBean == null) {
                return assignments;
            }
            var assignmentSubtype = targetBean.getAssignmentSubtype();
            return assignments.stream()
                    .filter(a -> assignmentSubtype == null
                            || a.getSubtype().contains(assignmentSubtype))
                    .toList();
        }

        /**
         * The mapping considers to remove the assignment that belongs to the range of this mapping.
         * Maybe it was created by this mapping before?
         *
         * We consent, unless the assignment points to a role which has a projection of relevant type (resource, kind, intent)
         * that prevents inbound membership synchronization.
         *
         * We don't support multi-tag resources here. Nor fancy assignments with filters etc.
         */
        boolean doesVetoTargetValueRemoval(PrismContainerValue<AssignmentType> value, OperationResult result) {
            LOGGER.trace("Considering whether to veto the removal of {}", value);
            try {
                var assignmentTargetRef = value.asContainerable().getTargetRef();
                if (assignmentTargetRef == null || assignmentTargetRef.getOid() == null) {
                    LOGGER.trace("-> No targetRef or OID, just remove it.");
                    return false; // We don't care about assignments without OID, why should we veto their deletion?
                }
                var type = assignmentTargetRef.getType();
                Class<? extends ObjectType> clazz;
                if (type != null) {
                    clazz = ObjectTypes.getObjectTypeClass(type);
                    if (!FocusType.class.isAssignableFrom(clazz)) {
                        LOGGER.trace("-> Not a focus, just remove it.");
                        return false; // no shadows
                    }
                } else {
                    clazz = AssignmentHolderType.class;
                }
                var target = ModelBeans.get().cacheRepositoryService.getObject(
                        clazz,
                        assignmentTargetRef.getOid(),
                        createReadOnlyCollection(),
                        result);
                List<ObjectReferenceType> linkRefs =
                        target.asObjectable() instanceof FocusType focus ? focus.getLinkRef() : List.of();
                LOGGER.trace("-> Target {} with {} shadows: {}", target, linkRefs.size(), linkRefs);
                if (linkRefs.isEmpty()) {
                    LOGGER.trace("-> No shadows, just remove it.");
                    return false;
                }
                var shadows = new ArrayList<AbstractShadow>();
                var noFetchOptions = createNoFetchCollection();
                for (var linkRef : linkRefs) {
                    shadows.add(
                            ModelBeans.get().provisioningService.getShadow(
                                    linkRef.getOid(), noFetchOptions, context.getTask(), result));
                }
                var matchingShadows = shadows.stream()
                        .filter(shadow -> associationDefinition.matches(shadow.getBean()))
                        .toList();
                LOGGER.trace("-> Matching shadows: {}", matchingShadows);
                var liveShadows = matchingShadows.stream()
                        .filter(s -> !s.isDead())
                        .toList();
                var fromLiveShadows = new HashSet<Boolean>();
                for (var liveShadow : liveShadows) {
                    fromLiveShadows.add(
                            isMembershipSyncInboundDisabled(liveShadow.getEffectiveOperationPolicyRequired()));
                }
                if (fromLiveShadows.size() == 1) {
                    boolean answer = fromLiveShadows.iterator().next();
                    LOGGER.trace("-> Answer from live shadows: {}", answer);
                    return answer;
                } else if (fromLiveShadows.size() > 1) {
                    LOGGER.trace("-> Contradicting answers from live shadows; NOT vetoing the removal");
                    return false;
                }
                // Now let's have a look at the last dead shadow
                var lastDeadShadowOptional = matchingShadows.stream()
                        .filter(s -> s.isDead() && s.getBean().getDeathTimestamp() != null)
                        .max(Comparator.comparingLong(s -> XmlTypeConverter.toMillis(s.getBean().getDeathTimestamp())));
                if (lastDeadShadowOptional.isPresent()) {
                    var lastDeadShadow = lastDeadShadowOptional.get();
                    var answer = isMembershipSyncInboundDisabled(lastDeadShadow.getEffectiveOperationPolicyRequired());
                    LOGGER.trace("-> Answer from last dead shadow: {} (from {})", answer, lastDeadShadow);
                    return answer;
                }
                LOGGER.trace("-> No shadow found, not vetoing the removal.");
                return false;
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logExceptionAsWarning(
                        LOGGER,
                        "'Object not found' detected while determining whether to remove assignment {}; so we will remove it",
                        e, value);
                return false;
            } catch (CommonException e) {
                LoggingUtils.logUnexpectedException(
                        LOGGER, "Error while determining whether to remove assignment {}; so we will remove it", e, value);
                return false;
            }
        }

        /**
         * Complex processing of a embedded object (later: any embedded value):
         *
         * 1. transforming to object for correlation ("pre-focus")
         * 2. determining the target PCV + action (synchronizing or not)
         * 3. collecting the mappings
         */
        private class ValueProcessing {

            @NotNull private final ShadowAssociationValue associationValue;

            private final ModelBeans beans = ModelBeans.get();

            ValueProcessing(@NotNull ShadowAssociationValue associationValue) {
                this.associationValue = associationValue;
            }

            void process(OperationResult parentResult)
                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ObjectNotFoundException {

                OperationResult result = parentResult.subresult(OP_PROCESS_ASSOCIATION_VALUE)
                        .addArbitraryObjectAsParam("associationValue", associationValue)
                        .build();
                try {

                    var assignmentForCorrelation = computeAssignmentForCorrelation(result);
                    var correlationResult = executeCorrelation(assignmentForCorrelation, result);
                    var synchronizationReaction = determineReaction(assignmentForCorrelation, correlationResult);
                    executeReaction(correlationResult, synchronizationReaction, result);

                    registerAssignmentsSeen(correlationResult);

                } catch (Throwable t) {
                    result.recordException(t);
                    throw t;
                } finally {
                    result.close();
                }
            }

            private @NotNull SimplifiedCorrelationResult executeCorrelation(
                    AssignmentType assignmentForCorrelation, OperationResult result)
                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                    SecurityViolationException, ObjectNotFoundException {

                LOGGER.trace("Executing correlation for assignments");

                if (candidateAssignments.isEmpty()) {
                    LOGGER.trace("No candidate assignments found, the correlation is trivial: no owner");
                    return SimplifiedCorrelationResult.noOwner();
                }

                var correlationDefinitionBean = mergeCorrelationDefinition(inboundDefinition, null, resource);
                var systemConfiguration = beans.systemObjectCache.getSystemConfigurationBean(result);
                var correlationResult = beans.correlationServiceImpl.correlateLimited(
                        CorrelatorContextCreator.createRootContext(
                                correlationDefinitionBean,
                                CorrelatorDiscriminator.forSynchronization(),
                                null,
                                systemConfiguration),
                        new CorrelationContext.AssociationValue(
                                associationValue,
                                assignmentForCorrelation,
                                candidateAssignments,
                                systemConfiguration,
                                context.getTask()),
                        result);

                LOGGER.trace("Correlation result:\n{}", correlationResult.debugDumpLazily(1));
                return correlationResult;
            }

            private AssignmentType computeAssignmentForCorrelation(OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ObjectNotFoundException {
                var targetAssignment = instantiateTargetAssignment();
                PreMappingsEvaluator.computePreFocusForAssociationValue(
                        associationValue,
                        associationValue.hasAssociationObject() ?
                                associationValue.getAssociationDataObject().getObjectDefinition() :
                                projectionContext.getCompositeObjectDefinitionRequired(),
                        inboundDefinition,
                        projectionContext.getResourceRequired(),
                        createMappingContextSpecification(),
                        targetAssignment,
                        context.getTask(),
                        result);
                LOGGER.trace("Target (for correlation):\n{}", targetAssignment.debugDumpLazily(1));
                return targetAssignment;
            }

            private AssignmentType instantiateTargetAssignment() {
                // FIXME temporary
                var assignment = new AssignmentType();
                var subtype = inboundDefinition.getFocusSpecification().getAssignmentSubtype();
                if (subtype != null) {
                    assignment.subtype(subtype);
                }
                return assignment;
            }

            /** "Assignments seen" are determined from the PLUS and ZERO sets of the resulting triple. */
            private void registerAssignmentsSeen(SimplifiedCorrelationResult correlationResult) {
                var owner = correlationResult.getOwner();
                if (owner != null) {
                    // No metadata here, as for now; these assignments might or might not be, in fact, created by this mapping
                    // see also MID-10084.
                    //noinspection unchecked
                    evaluatorResult.addToZeroSet(owner.asPrismContainerValue().clone());
                }
            }

            // FIXME temporary
            private ItemSynchronizationReactionDefinition determineReaction(
                    AssignmentType assignmentForCorrelation, SimplifiedCorrelationResult correlationResult) {
                var synchronizationState = ItemSynchronizationState.fromCorrelationResult(correlationResult);
                var situationFromCorrelation = synchronizationState.situation();
                ItemSynchronizationSituationType situation;
                if (situationFromCorrelation == ItemSynchronizationSituationType.UNMATCHED
                        && isMatchedIndirectly(assignmentForCorrelation)) {
                    situation = ItemSynchronizationSituationType.MATCHED_INDIRECTLY;
                } else {
                    situation = situationFromCorrelation;
                }

                if (isInboundMembershipSyncDisabled()) {
                    LOGGER.trace("Inbound membership synchronization is disabled, ignoring the situation: {}", situation);
                    return null;
                }

                for (var abstractReaction : inboundDefinition.getSynchronizationReactions()) {
                    var reaction = (ItemSynchronizationReactionDefinition) abstractReaction;
                    if (reaction.matchesSituation(situation)) {
                        // TODO evaluate other aspects, like condition etc
                        LOGGER.trace("Determined synchronization reaction: {}", reaction);
                        return reaction;
                    }
                }
                LOGGER.trace("No synchronization reaction matches");
                return null;
            }

            private boolean isInboundMembershipSyncDisabled() {
                if (associationDefinition.isComplex()) {
                    return false; // not supported for complex associations yet
                }
                return isMembershipSyncInboundDisabled(
                        associationValue
                                .getSingleObjectShadowRequired()
                                .getEffectiveOperationPolicyRequired());
            }

            private boolean isMatchedIndirectly(AssignmentType assignmentForCorrelation) {
                var targetRef = assignmentForCorrelation.getTargetRef();
                if (targetRef == null) {
                    LOGGER.trace("No targetRef, assignment is not matched indirectly");
                    return false;
                }
                var focusContext = ModelExpressionThreadLocalHolder.getLensContextRequired().getFocusContextRequired();
                var current = focusContext.getObjectCurrent();
                if (current == null) {
                    LOGGER.trace("No current focus, assignment is not matched indirectly");
                    return false;
                }
                List<ObjectReferenceType> roleMembershipRef =
                        current.asObjectable() instanceof AssignmentHolderType assignmentHolder ?
                                assignmentHolder.getRoleMembershipRef() : List.of();
                var matches = roleMembershipRef.stream()
                        .anyMatch(ref ->
                                targetRef.asReferenceValue().equals(ref.asReferenceValue(), EquivalenceStrategy.REAL_VALUE));
                LOGGER.trace("Assignment is matched indirectly: {}", matches);
                return matches;
            }

            private void executeReaction(
                    @NotNull SimplifiedCorrelationResult correlationResult,
                    @Nullable ItemSynchronizationReactionDefinition synchronizationReaction,
                    @NotNull OperationResult result)
                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
                    CommunicationException, ObjectNotFoundException {
                if (synchronizationReaction == null) {
                    registerAssignmentsSeen(correlationResult);
                    return;
                }
                for (var action : synchronizationReaction.getActions()) {
                    // TODO implement using action factory, like the regular ones are
                    var beanClass = action.getClass();
                    if (AddFocusValueItemSynchronizationActionType.class.equals(beanClass)) {
                        executeAdd(result);
                    } else if (DeleteFocusValueItemSynchronizationActionType.class.equals(beanClass)) {
                        executeDelete();
                    } else if (SynchronizeItemSynchronizationActionType.class.equals(beanClass)) {
                        registerAssignmentsSeen(correlationResult);
                        executeSynchronize(correlationResult, result);
                    } else {
                        throw new UnsupportedOperationException("Action " + action + " is not supported here");
                    }
                }
            }

            private void executeAdd(@NotNull OperationResult result)
                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
                    CommunicationException, ObjectNotFoundException {
                var targetAssignment = instantiateTargetAssignment();
                SingleShadowInboundsProcessing.evaluate(
                        createShadowProcessingContext(targetAssignment, result),
                        result);
                LOGGER.trace("Going to ADD a new assignment for association: {}:\n{}",
                        associationDefinition, targetAssignment.debugDumpLazily(1));
                setValueMetadata(targetAssignment.asPrismContainerValue(), result);
                //noinspection unchecked
                evaluatorResult.addToPlusSet(targetAssignment.asPrismContainerValue());
            }

            private void setValueMetadata(PrismContainerValue<?> pcv, OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                    ConfigurationException, ObjectNotFoundException {
                var metadataComputer = context.getValueMetadataComputer();
                if (metadataComputer != null) {
                    pcv.setValueMetadata(
                            metadataComputer.compute(List.of(associationValue), result));
                }
            }

            private void executeSynchronize(@NotNull SimplifiedCorrelationResult correlationResult, @NotNull OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ObjectNotFoundException {
                var targetAssignment = Objects.requireNonNull((AssignmentType) correlationResult.getOwner());
                var innerProcessing = SingleShadowInboundsProcessing.evaluateToTripleMap(
                        createShadowProcessingContext(targetAssignment, result),
                        result);
                var assignmentPath = AssignmentHolderType.F_ASSIGNMENT.append(Objects.requireNonNull(targetAssignment.getId()));
                evaluatorResult.mergeIntoOtherTriples(assignmentPath, innerProcessing.getOutputTripleMap());
                evaluatorResult.mergeIntoItemDefinitionsMap(assignmentPath, innerProcessing.getItemDefinitionMap());
                evaluatorResult.mergeIntoMappingEvaluationRequestsMap(assignmentPath, innerProcessing.getEvaluationRequestsMap());
            }

            private @NotNull DefaultSingleShadowInboundsProcessingContextImpl<AssignmentType> createShadowProcessingContext(
                    AssignmentType targetAssignment, @NotNull OperationResult result)
                    throws SchemaException, ConfigurationException {
                return new DefaultSingleShadowInboundsProcessingContextImpl<>(
                        associationValue,
                        resource,
                        createMappingContextSpecification(),
                        targetAssignment,
                        ModelBeans.get().systemObjectCache.getSystemConfigurationBean(result),
                        context.getTask(),
                        associationValue.hasAssociationObject() ?
                                associationValue.getAssociationDataObject().getObjectDefinition() :
                                projectionContext.getCompositeObjectDefinitionRequired(),
                        inboundDefinition,
                        false);
            }

            private @NotNull InboundMappingContextSpecification createMappingContextSpecification() {
                return new InboundMappingContextSpecification(
                        projectionContext.getKey().getTypeIdentification(),
                        associationDefinition.getAssociationTypeName(),
                        projectionContext.getTag());
            }

            private void executeDelete() {
                throw new UnsupportedOperationException("Sorry, 'delete' action is not supported yet");
            }
        }
    }

    @Override
    public String shortDebugDump() {
        return "associationSynchronization";
    }
}
