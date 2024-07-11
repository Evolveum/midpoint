/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class FullInboundsPreparation<F extends FocusType> {


//    void processAssociations(OperationResult result)
//            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
//            ConfigurationException, ObjectNotFoundException, StopProcessingProjectionException {
//        // FIXME fix this hacking aimed at providing "object new" version
//        PrismObject<ShadowType> shadow;
//        if (ObjectDelta.isAdd(source.aPrioriDelta)) {
//            shadow = source.aPrioriDelta.getObjectToAdd();
//        } else {
//            var resourceObjectNew = source.sourceData.getShadowIfPresent(); // when doing associations, we are certainly at a shadow
//            if (resourceObjectNew == null) {
//                return;
//            }
//            if (ObjectDelta.isModify(source.aPrioriDelta)) {
//                shadow = resourceObjectNew.clone();
//                source.aPrioriDelta.applyTo(shadow);
//            } else {
//                shadow = resourceObjectNew;
//            }
//        }
//
//        // TODO make sure we don't process associations from associated objects
//        for (var assocDef : source.sourceData.getAssociationDefinitions()) {
//            for (var inboundDef : assocDef.getRelevantInboundDefinitions()) {
//                var assocName = assocDef.getItemName();
//                var assocValues = ShadowUtil.getAssociationValues(shadow, assocName);
//                LOGGER.trace("Processing association '{}' ({} values) using {}", assocName, assocValues.size(), inboundDef);
//                new AssociationProcessing(assocValues, assocDef, inboundDef)
//                        .process(result);
//            }
//        }
//    }
//
//    /**
//     * Complex processing of an association (represented by a reference attribute)
//     * by given {@link ResourceObjectInboundDefinition}.
//     */
//    private class AssociationProcessing {
//
//        @NotNull private final Collection<? extends ShadowAssociationValue> associationValues;
//        @NotNull private final ShadowAssociationDefinition associationDefinition;
//        @NotNull private final ResourceObjectInboundDefinition inboundDefinition;
//
//        /** IDs of (existing) assignments that were seen by this processing. Other assignments in the range will be removed. */
//        @NotNull private final Set<Long> assignmentsIdsSeen = new HashSet<>();
//
//        AssociationProcessing(
//                @NotNull Collection<? extends ShadowAssociationValue> associationValues,
//                @NotNull ShadowAssociationDefinition associationDefinition,
//                @NotNull ResourceObjectInboundDefinition inboundDefinition) {
//            this.associationValues = associationValues;
//            this.associationDefinition = associationDefinition;
//            this.inboundDefinition = inboundDefinition;
//        }
//
//        void process(OperationResult result)
//                throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
//                ConfigurationException, StopProcessingProjectionException, ObjectNotFoundException {
//
//            LOGGER.trace("Processing {} individual values of the association '{}'",
//                    associationValues.size(), associationDefinition.getItemName());
//
//            values: for (var associationValue : associationValues) {
//
//                // FIXME EXTRA HACK: we check if the association value was not removed by a-priori delta
//                //  (normal delta application does not work here)
//                if (source.aPrioriDelta != null) {
//                    var associationDelta = source.aPrioriDelta.<ShadowAssociationValueType>findContainerDelta(
//                            associationDefinition.getStandardPath());
//                    if (associationDelta != null) {
//                        for (var deletedValue : emptyIfNull(associationDelta.getValuesToDelete())) {
//                            if (associationValue.matches((ShadowAssociationValue) deletedValue)) {
//                                LOGGER.trace("Ignoring association value that was already deleted: {}", associationValue);
//                                continue values;
//                            }
//                        }
//                    }
//                }
//
//                LOGGER.trace("Processing association value: {}", associationValue);
//                new ValueProcessing(associationValue)
//                        .process(result);
//            }
//
//            // Deleting unseen assignments from the range
//            var assignmentsIdsInRange = getAssignmentsIdsInRange();
//            var assignmentsIdsToDelete = Sets.difference(assignmentsIdsInRange, assignmentsIdsSeen);
//            LOGGER.trace("Assignments in range: {}; seen: {}; to delete: {}",
//                    assignmentsIdsInRange, assignmentsIdsSeen, assignmentsIdsToDelete);
//            context.assignmentsProcessingContext.addAssignmentsToKeep(assignmentsIdsSeen);
//            context.assignmentsProcessingContext.addAssignmentsToDelete(assignmentsIdsToDelete);
//        }
//
//        // TODO consider provenance metadata here as well
//        private @NotNull Set<Long> getAssignmentsIdsInRange() {
//            return getCandidateAssignments().stream()
//                    .map(AssignmentType::getId)
//                    .collect(Collectors.toSet());
//        }
//
//        private @NotNull Collection<AssignmentType> getCandidateAssignments() {
//            var targetPcv = target.targetPcv;
//            if (targetPcv == null) {
//                return List.of();
//            }
//            var assignments = targetPcv.asContainerable().getAssignment();
//            var focusSpecification = inboundDefinition.getFocusSpecification();
//            var assignmentSubtype = focusSpecification.getAssignmentSubtype();
//            var assignmentTargetTypeName = focusSpecification.getAssignmentTargetTypeName();
//            return assignments.stream()
//                    .filter(a -> assignmentSubtype == null
//                            || a.getSubtype().contains(assignmentSubtype))
//                    .filter(a -> assignmentTargetTypeName == null
//                            || a.getTargetRef() != null && QNameUtil.match(a.getTargetRef().getType(), assignmentTargetTypeName))
//                    .toList();
//        }
//
//        /**
//         * Complex processing of a embedded object (later: any embedded value):
//         *
//         * 1. transforming to object for correlation ("pre-focus")
//         * 2. determining the target PCV + action (synchronizing or not)
//         * 3. collecting the mappings
//         */
//        private class ValueProcessing {
//
//            @NotNull private final ShadowAssociationValue associationValue;
//            @NotNull private final ResourceType resource = projectionContext.getResourceRequired();
//
//            ValueProcessing(@NotNull ShadowAssociationValue associationValue) {
//                this.associationValue = associationValue;
//            }
//
//            void process(OperationResult parentResult)
//                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
//                    ConfigurationException, ObjectNotFoundException, StopProcessingProjectionException {
//
//                OperationResult result = parentResult.subresult(OP_PROCESS_ASSOCIATION_VALUE)
//                        .addArbitraryObjectAsParam("associationValue", associationValue)
//                        .build();
//                try {
//
//                    var correlationResult = executeCorrelation(result);
//                    var synchronizationReaction = determineReaction(correlationResult);
//                    executeReaction(correlationResult, synchronizationReaction, result);
//
//                    registerAssignmentsSeen(correlationResult);
//
//                } catch (Throwable t) {
//                    result.recordException(t);
//                    throw t;
//                } finally {
//                    result.close();
//                }
//            }
//
//            private @NotNull SimplifiedCorrelationResult executeCorrelation(OperationResult result)
//                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
//                    SecurityViolationException, ObjectNotFoundException {
//
//                LOGGER.trace("Executing correlation for assignments");
//
//                var candidateAssignments = getCandidateAssignments();
//                if (candidateAssignments.isEmpty()) {
//                    LOGGER.trace("No candidate assignments found, the correlation is trivial: no owner");
//                    return SimplifiedCorrelationResult.noOwner();
//                }
//
//                var assignmentForCorrelation = computeAssignmentForCorrelation(result);
//
//                var correlationDefinitionBean = mergeCorrelationDefinition(inboundDefinition, null, resource);
//
//                var correlationResult = beans.correlationServiceImpl.correlateLimited(
//                        CorrelatorContextCreator.createRootContext(
//                                correlationDefinitionBean,
//                                CorrelatorDiscriminator.forSynchronization(),
//                                null,
//                                context.getSystemConfigurationBean()),
//                        new CorrelationContext.AssociationValue(
//                                associationValue,
//                                assignmentForCorrelation,
//                                candidateAssignments,
//                                context.getSystemConfigurationBean(),
//                                context.env.task),
//                        result);
//
//                LOGGER.trace("Correlation result:\n{}", correlationResult.debugDumpLazily(1));
//                return correlationResult;
//            }
//
//            private AssignmentType computeAssignmentForCorrelation(OperationResult result)
//                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
//                    ConfigurationException, ObjectNotFoundException {
//                var targetAssignment = instantiateTargetAssignment();
//                PreMappingsEvaluation.computePreFocusForAssociationValue(
//                        associationValue,
//                        associationValue.hasAssociationObject() ?
//                                associationValue.getAssociationObject().getObjectDefinition() :
//                                source.sourceData.getShadowObjectDefinition(),
//                        inboundDefinition,
//                        projectionContext.getResourceRequired(),
//                        targetAssignment,
//                        context.env.task,
//                        result);
//                LOGGER.trace("Target (for correlation):\n{}", targetAssignment.debugDumpLazily(1));
//                return targetAssignment;
//            }
//
//            private AssignmentType instantiateTargetAssignment() {
//                // FIXME temporary
//                var assignment = new AssignmentType();
//                var subtype = inboundDefinition.getFocusSpecification().getAssignmentSubtype();
//                if (subtype != null) {
//                    assignment.subtype(subtype);
//                }
//                return assignment;
//            }
//
//            private void registerAssignmentsSeen(SimplifiedCorrelationResult correlationResult) {
//                var owner = correlationResult.getOwner();
//                if (owner != null) {
//                    assignmentsIdsSeen.add(owner.asPrismContainerValue().getId());
//                }
//                // This is to be discussed. We probably should avoid deleting assignments that were matched with 100%
//                // confidence, even if there are multiple ones. Should we do the same also for assignment matched with
//                // less certainty? Currently, we do so.
//                emptyIfNull(correlationResult.getUncertainOwners()).forEach(
//                        a -> assignmentsIdsSeen.add(a.getValue().asPrismContainerValue().getId()));
//            }
//
//            // FIXME temporary
//            private ItemSynchronizationReactionDefinition determineReaction(SimplifiedCorrelationResult correlationResult) {
//                var synchronizationState = ItemSynchronizationState.fromCorrelationResult(correlationResult);
//                ItemSynchronizationSituationType situation = synchronizationState.situation();
//                for (var abstractReaction : inboundDefinition.getSynchronizationReactions()) {
//                    var reaction = (ItemSynchronizationReactionDefinition) abstractReaction;
//                    if (reaction.matchesSituation(situation)) {
//                        // TODO evaluate other aspects, like condition etc
//                        LOGGER.trace("Determined synchronization reaction: {}", reaction);
//                        return reaction;
//                    }
//                }
//                LOGGER.trace("No synchronization reaction matches");
//                return null;
//            }
//
//            private void executeReaction(
//                    @NotNull SimplifiedCorrelationResult correlationResult,
//                    @Nullable ItemSynchronizationReactionDefinition synchronizationReaction,
//                    @NotNull OperationResult result)
//                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
//                    CommunicationException, StopProcessingProjectionException, ObjectNotFoundException {
//                if (synchronizationReaction == null) {
//                    return;
//                }
//                for (var action : synchronizationReaction.getActions()) {
//                    // TODO implement using action factory, like the regular ones are
//                    var beanClass = action.getClass();
//                    if (AddFocusValueItemSynchronizationActionType.class.equals(beanClass)) {
//                        executeAdd(result);
//                    } else if (DeleteFocusValueItemSynchronizationActionType.class.equals(beanClass)) {
//                        executeDelete();
//                    } else if (SynchronizeItemSynchronizationActionType.class.equals(beanClass)) {
//                        executeSynchronize(correlationResult, result);
//                    } else {
//                        throw new UnsupportedOperationException("Action " + action + " is not supported here");
//                    }
//                }
//            }
//
//            private void executeAdd(@NotNull OperationResult result)
//                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
//                    CommunicationException, StopProcessingProjectionException, ObjectNotFoundException {
//                var focusContext = lensContext.getFocusContextRequired();
//                long id = focusContext.getTemporaryContainerId(FocusType.F_ASSIGNMENT);
//                LOGGER.trace("Going to ADD a new assignment ({}) for association: {}", id, associationDefinition);
//                context.assignmentsProcessingContext.addAssignmentToAdd(
//                        instantiateTargetAssignment().id(id));
//                collectChildMappings(id, result);
//            }
//
//            private void executeSynchronize(@NotNull SimplifiedCorrelationResult correlationResult, @NotNull OperationResult result)
//                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
//                    ConfigurationException, StopProcessingProjectionException, ObjectNotFoundException {
//                var owner = stateNonNull(
//                        correlationResult.getOwner(),
//                        "Cannot invoke SYNCHRONIZE action without the owner");
//                var ownerId = stateNonNull(
//                        owner.asPrismContainerValue().getId(),
//                        "Cannot invoke SYNCHRONIZE action on an owner without PCV ID: %s", owner);
//                LOGGER.trace("Going to SYNCHRONIZE existing assignment ({}) for association: {}", ownerId, associationDefinition);
//                collectChildMappings(ownerId, result);
//            }
//
//            private void collectChildMappings(long id, @NotNull OperationResult result)
//                    throws ConfigurationException, SchemaException, ObjectNotFoundException, SecurityViolationException,
//                    CommunicationException, ExpressionEvaluationException, StopProcessingProjectionException {
//                MappingSource childSource = new FullSource(
//                        InboundSourceData.forAssociationValue(
//                                associationValue,
//                                associationDefinition.hasAssociationObject() ?
//                                        associationDefinition.getAssociationObjectDefinition() :
//                                        source.sourceData.getShadowObjectDefinition()),
//                        inboundDefinition,
//                        projectionContext,
//                        context
//                );
//                child(childSource, FocusType.F_ASSIGNMENT.append(id))
//                        .prepareOrEvaluate(result);
//            }
//
//            private void executeDelete() {
//                throw new UnsupportedOperationException("Sorry, 'delete' action is not supported yet");
//            }
//        }
//    }
}
