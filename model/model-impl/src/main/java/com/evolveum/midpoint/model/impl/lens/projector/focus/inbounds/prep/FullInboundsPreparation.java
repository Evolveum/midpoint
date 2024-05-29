/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import static com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil.mergeCorrelationDefinition;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.StopProcessingProjectionException;

import com.evolveum.midpoint.model.impl.sync.ItemSynchronizationState;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition.ItemSynchronizationReactionDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.util.ShadowUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlation.SimplifiedCorrelationResult;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlation.CorrelatorContextCreator;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensObjectDeltaOperation;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequests;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluatorParams;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingInitializer;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingOutputProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingTimeEval;
import com.evolveum.midpoint.model.impl.sync.PreMappingsEvaluation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class FullInboundsPreparation<F extends FocusType> extends InboundsPreparation<F> {

    private static final Trace LOGGER = TraceManager.getTrace(FullInboundsPreparation.class);

    private static final String OP_PROCESS_ASSOCIATED_OBJECT = FullInboundsPreparation.class.getName() + ".processAssociatedObject";

    @NotNull private final LensProjectionContext projectionContext;
    @NotNull private final LensContext<F> lensContext;
    @NotNull private final ModelBeans beans = ModelBeans.get();

    private FullInboundsPreparation(
            @NotNull LensProjectionContext projectionContext,
            @NotNull LensContext<F> lensContext,
            @NotNull MappingEvaluationRequests evaluationRequestsBeingCollected,
            @NotNull MSource source,
            @NotNull FullTarget<F> target,
            @NotNull Context context) {
        super(evaluationRequestsBeingCollected, source, target, context);
        this.projectionContext = projectionContext;
        this.lensContext = lensContext;
    }

    /** Main constructor; to be used from the outside. */
    public FullInboundsPreparation(
            @NotNull LensProjectionContext projectionContext,
            @NotNull LensContext<F> lensContext,
            @NotNull MappingEvaluationRequests evaluationRequestsBeingCollected,
            @NotNull PathKeyedMap<ItemDefinition<?>> itemDefinitionMap,
            @NotNull FullContext context,
            @Nullable PrismObject<F> focus,
            @NotNull PrismObjectDefinition<F> focusDefinition) throws SchemaException, ConfigurationException {
        this(projectionContext,
                lensContext,
                evaluationRequestsBeingCollected,
                new FullSource(
                        projectionContext.getObjectCurrent(),
                        getAPrioriDelta(projectionContext),
                        projectionContext.getCompositeObjectDefinition(),
                        projectionContext.getCompositeObjectDefinition(),
                        projectionContext,
                        context,
                        null),
                new FullTarget<>(lensContext, focus, focusDefinition, itemDefinitionMap, ItemPath.EMPTY_PATH),
                context);
    }

    /** Recursive invocation with a sub-source in the same projection context. */
    private FullInboundsPreparation<F> child(MSource source, ItemPath targetPathPrefix) {
        return new FullInboundsPreparation<>(
                projectionContext,
                lensContext,
                evaluationRequestsBeingCollected,
                source,
                ((FullTarget<F>) target).withTargetPathPrefix(targetPathPrefix),
                context);
    }

    /**
     * Computes a priori delta for given projection context.
     *
     * TODO revise this method
     */
    private static ObjectDelta<ShadowType> getAPrioriDelta(@NotNull LensProjectionContext projectionContext) {
        int wave = projectionContext.getLensContext().getProjectionWave();
        if (wave == 0) {
            return projectionContext.getSyncDelta();
        }
        if (wave == projectionContext.getWave() + 1) {
            // If this resource was processed in a previous wave ....
            // Normally, we take executed delta. However, there are situations (like preview changes - i.e. projector without execution),
            // when there is no executed delta. In that case we take standard primary + secondary delta.
            // TODO is this really correct? Think if the following can happen:
            // - NOT previewing
            // - no executed deltas but
            // - existing primary/secondary delta.
            List<LensObjectDeltaOperation<ShadowType>> executed = projectionContext.getExecutedDeltas();
            if (!executed.isEmpty()) {
                // TODO why the last one?
                return executed.get(executed.size() - 1).getObjectDelta();
            } else {
                return projectionContext.getSummaryDelta(); // TODO check this
            }
        }
        return null;
    }

    /**
     * Processing for special (fixed-schema) properties such as credentials and activation.
     *
     * The code is rather strange. TODO revisit and clean up
     *
     * Also it is not clear why these mappings are not collected to the map for later execution,
     * just like regular mappings are.
     *
     * [EP:M:IM] DONE 4/4
     */
    private void evaluateSpecialInbounds(
            List<MappingType> inboundMappingBeans,
            ItemPath sourcePath, ItemPath targetPath, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        if (inboundMappingBeans == null || inboundMappingBeans.isEmpty()) {
            return;
        }

        LOGGER.trace("Collecting {} inbounds for special property {}", inboundMappingBeans.size(), sourcePath);

        F focus = target.getTargetRealValue();
//        if (focus == null) {
//            LOGGER.trace("No current/new focus, skipping.");
//            return;
//        }
        if (!projectionContext.isFullShadow()) {
            // TODO - is this ok?
            LOGGER.trace("Full shadow not loaded, skipping.");
            return;
        }

        ObjectDelta<F> userPrimaryDelta = lensContext.getFocusContext().getPrimaryDelta();
        if (userPrimaryDelta != null) {
            PropertyDelta<?> primaryPropDelta = userPrimaryDelta.findPropertyDelta(targetPath);
            if (primaryPropDelta != null && primaryPropDelta.isReplace()) {
                LOGGER.trace("Primary delta of 'replace' overrides any inbounds, skipping. Delta: {}", primaryPropDelta);
                return;
            }
        }

        MappingInitializer<PrismValue, ItemDefinition<?>> initializer =
                (builder) -> {
                    if (projectionContext.getObjectNew() == null) {
                        String message = "Recomputing account " + projectionContext.getKey()
                                + " results in null new account. Something must be really broken.";
                        LOGGER.error(message);
                        LOGGER.trace("Account context:\n{}", projectionContext.debugDumpLazily());
                        throw new SystemException(message);
                    }

                    ItemDelta<PrismPropertyValue<?>, PrismPropertyDefinition<?>> specialAttributeDelta;
                    if (source.aPrioriDelta != null) {
                        specialAttributeDelta = source.aPrioriDelta.findItemDelta(sourcePath);
                    } else {
                        specialAttributeDelta = null;
                    }
                    ItemDeltaItem<PrismPropertyValue<?>, PrismPropertyDefinition<?>> sourceIdi =
                            projectionContext.getObjectDeltaObject().findIdi(sourcePath);
                    if (specialAttributeDelta == null) {
                        specialAttributeDelta = sourceIdi.getDelta();
                    }
                    Source<PrismPropertyValue<?>, PrismPropertyDefinition<?>> source = new Source<>(
                            sourceIdi.getItemOld(), specialAttributeDelta, sourceIdi.getItemOld(),
                            ExpressionConstants.VAR_INPUT_QNAME,
                            sourceIdi.getDefinition());
                    builder.defaultSource(source)
                            .addVariableDefinition(ExpressionConstants.VAR_USER, focus, UserType.class)
                            .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focus, FocusType.class)
                            .addAliasRegistration(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);

                    PrismObject<ShadowType> accountNew = this.source.getResourceObjectNew();
                    builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, accountNew, ShadowType.class)
                            .addVariableDefinition(ExpressionConstants.VAR_SHADOW, accountNew, ShadowType.class)
                            .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, accountNew, ShadowType.class)
                            .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                            .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
                            .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, this.projectionContext.getResource(), ResourceType.class)
                            .valuePolicySupplier(context.createValuePolicySupplier())
                            .mappingKind(MappingKindType.INBOUND)
                            .implicitSourcePath(sourcePath)
                            .implicitTargetPath(targetPath)
                            .originType(OriginType.INBOUND)
                            .originObject(this.projectionContext.getResource());

                    return builder;
                };

        MappingOutputProcessor<PrismValue> processor =
                (mappingOutputPath, outputStruct) -> {
                    PrismValueDeltaSetTriple<PrismValue> outputTriple = outputStruct.getOutputTriple();
                    if (outputTriple == null) {
                        LOGGER.trace("Mapping for property {} evaluated to null. Skipping inbound processing for that property.", sourcePath);
                        return false;
                    }

                    PrismObjectDefinition<F> focusDefinition = lensContext.getFocusContext().getObjectDefinition();
                    PrismProperty mResult = focusDefinition.findPropertyDefinition(targetPath).instantiate();
                    //noinspection unchecked
                    mResult.addAll(PrismValueCollectionsUtil.cloneCollection(outputTriple.getNonNegativeValues()));

                    PrismProperty targetPropertyNew = focus.asPrismObject().findOrCreateProperty(targetPath);
                    PropertyDelta<?> delta;
                    if (ProtectedStringType.COMPLEX_TYPE.equals(targetPropertyNew.getDefinition().getTypeName())) {
                        // We have to compare this in a special way. The cipherdata may be different due to a different
                        // IV, but the value may still be the same
                        ProtectedStringType resultValue = (ProtectedStringType) mResult.getRealValue();
                        ProtectedStringType targetPropertyNewValue = (ProtectedStringType) targetPropertyNew.getRealValue();
                        try {
                            if (beans.protector.compareCleartext(resultValue, targetPropertyNewValue)) {
                                delta = null;
                            } else {
                                //noinspection unchecked
                                delta = targetPropertyNew.diff(mResult);
                            }
                        } catch (EncryptionException e) {
                            throw new SystemException(e.getMessage(), e);
                        }
                    } else {
                        //noinspection unchecked
                        delta = targetPropertyNew.diff(mResult);
                    }
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("targetPropertyNew:\n{}\ndelta:\n{}", targetPropertyNew.debugDump(1), DebugUtil.debugDump(delta, 1));
                    }
                    if (delta != null && !delta.isEmpty()) {
                        delta.setParentPath(targetPath.allExceptLast());
                        lensContext.getFocusContext().swallowToSecondaryDelta(delta);
                    }
                    return false;
                };

        // [EP:M:IM] DONE, see above
        OriginProvider<MappingType> originProvider =
                item -> ConfigurationItemOrigin.inResourceOrAncestor(projectionContext.getResourceRequired());

        MappingEvaluatorParams<PrismValue, ItemDefinition<?>, F, F> params = new MappingEvaluatorParams<>();
        params.setMappingConfigItems( // [EP:M:IM] DONE, see above
                ConfigurationItem.ofList(inboundMappingBeans, originProvider, MappingConfigItem.class));
        params.setMappingDesc("inbound mapping for " + sourcePath + " in " + projectionContext.getResource());
        params.setNow(context.env.now);
        params.setInitializer(initializer);
        params.setProcessor(processor);
        //noinspection unchecked
        params.setAPrioriTargetObject((PrismObject<F>) focus.asPrismObject());
        params.setAPrioriTargetDelta(userPrimaryDelta);
        params.setTargetContext(lensContext.getFocusContext());
        params.setDefaultTargetItemPath(targetPath);
        params.setEvaluateCurrent(MappingTimeEval.CURRENT);
        params.setContext(lensContext);
        params.setHasFullTargetObject(true);
        beans.projectionMappingSetEvaluator.evaluateMappingsToTriples(params, context.env.task, result);
    }

    @Override
    void evaluateSpecialInbounds(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        // TODO convert to mapping creation requests
        evaluateSpecialInbounds( // [EP:M:IM] DONE, obviously belonging to the resource
                source.inboundDefinition.getPasswordInbound(),
                SchemaConstants.PATH_PASSWORD_VALUE, SchemaConstants.PATH_PASSWORD_VALUE,
                result);
        evaluateSpecialInbounds( // [EP:M:IM] DONE, obviously belonging to the resource
                getActivationInbound(ActivationType.F_ADMINISTRATIVE_STATUS),
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                result);
        evaluateSpecialInbounds( // [EP:M:IM] DONE, obviously belonging to the resource
                getActivationInbound(ActivationType.F_VALID_FROM),
                SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                result);
        evaluateSpecialInbounds( // [EP:M:IM] DONE, obviously belonging to the resource
                getActivationInbound(ActivationType.F_VALID_TO),
                SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO,
                result);
    }

    private List<MappingType> getActivationInbound(ItemName itemName) {
        ResourceBidirectionalMappingType biDirMapping =
                source.inboundDefinition.getActivationBidirectionalMappingType(itemName);
        return biDirMapping != null ? biDirMapping.getInbound() : Collections.emptyList();
    }

    @Override
    void executeComplexProcessing(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, StopProcessingProjectionException {
        var shadow = source.getResourceObjectNew();
        if (shadow == null) {
            return;
        }

        // TODO implement also for attributes

        for (var association : ShadowUtil.getAssociations(shadow)) {
            var associationDefinition = association.getDefinition();
            var relevantInboundDefinitions = associationDefinition.getRelevantInboundDefinitions();
            if (relevantInboundDefinitions.isEmpty()) {
                continue;
            }
            for (var associationValue : association.getAssociationValues()) {
                for (var inboundProcessingDefinition : relevantInboundDefinitions) {
                    LOGGER.trace("Processing association value: {} ({})", associationValue, relevantInboundDefinitions);
                    var processing = new ValueProcessing(
                            associationValue, associationDefinition, inboundProcessingDefinition,
                            projectionContext.getResourceRequired());
                    processing.process(result);
                }
            }
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
        @NotNull private final ShadowReferenceAttributeDefinition associationDefinition;
        @NotNull private final ResourceObjectInboundDefinition inboundDefinition;
        @Deprecated // provide more abstract characterization (~ "assigned")
        @NotNull private final ItemPath focusItemPath;
        @NotNull private final ResourceType resource;

        ValueProcessing(
                @NotNull ShadowAssociationValue associationValue,
                @NotNull ShadowReferenceAttributeDefinition associationDefinition,
                @NotNull ResourceObjectInboundDefinition inboundDefinition,
                @NotNull ResourceType resource) throws ConfigurationException {
            this.associationValue = associationValue;
            this.associationDefinition = associationDefinition;
            this.inboundDefinition = inboundDefinition;
            this.focusItemPath = configNonNull(
                    inboundDefinition.getFocusSpecification().getFocusItemPath(),
                    "No focus item path in %s", inboundDefinition);
            this.resource = resource;
        }

        void process(OperationResult parentResult)
                throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                ConfigurationException, ObjectNotFoundException, StopProcessingProjectionException {

            OperationResult result = parentResult.subresult(OP_PROCESS_ASSOCIATED_OBJECT)
                    .addArbitraryObjectAsParam("associationValue", associationValue)
                    .addArbitraryObjectAsParam("associatedObjectDefinition", associationDefinition)
                    .build();
            try {

                var objectForCorrelation = computeObjectForCorrelation(result);
                var correlationResult = executeCorrelation(objectForCorrelation, result);
                var synchronizationReaction = determineReaction(correlationResult);
                executeReaction(correlationResult, synchronizationReaction, result);

            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                result.close();
            }
        }

        private Containerable computeObjectForCorrelation(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                ConfigurationException, ObjectNotFoundException {
            var target = instantiateTargetObject();
            PreMappingsEvaluation.computePreFocusForAssociationValue(
                    associationValue,
                    inboundDefinition,
                    projectionContext.getResourceRequired(),
                    target,
                    context.env.task,
                    result);
            LOGGER.trace("Target (for correlation):\n{}", target.debugDumpLazily(1));
            return target;
        }

        private @NotNull SimplifiedCorrelationResult executeCorrelation(
                Containerable objectForCorrelation, OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {

            var candidateObjects = getCandidateObjects();
            if (candidateObjects.isEmpty()) {
                LOGGER.trace("No candidate objects, the correlation is trivial: no owner");
                return SimplifiedCorrelationResult.noOwner();
            }

            var correlationDefinitionBean = mergeCorrelationDefinition(inboundDefinition, null, resource);

            var correlationResult = beans.correlationServiceImpl.correlateLimited(
                    CorrelatorContextCreator.createRootContext(
                            correlationDefinitionBean,
                            CorrelatorDiscriminator.forSynchronization(),
                            null,
                            context.getSystemConfigurationBean()),
                    new CorrelationContext.Shadow(
                            associationValue.getShadowBean(),
                            projectionContext.getResourceRequired(),
                            associationValue.getAssociatedObjectDefinition(),
                            objectForCorrelation,
                            candidateObjects,
                            context.getSystemConfigurationBean(),
                            context.env.task),
                    result);
            LOGGER.trace("Correlation result:\n{}", correlationResult.debugDumpLazily(1));
            return correlationResult;
        }

        private @NotNull Collection<? extends Containerable> getCandidateObjects() throws ConfigurationException {

            var assignmentSubtype = inboundDefinition.getFocusSpecification().getAssignmentSubtype();
            LOGGER.trace("Getting candidate objects for {} (assignment subtype: {})", focusItemPath, assignmentSubtype);

            var targetItem = target.targetPcv.findItem(focusItemPath);
            if (targetItem == null) {
                return List.of();
            }
            if (assignmentSubtype == null) {
                return targetItem.getRealValues(Containerable.class);
            }
            if (!AssignmentHolderType.F_ASSIGNMENT.equivalent(focusItemPath)) {
                throw new ConfigurationException(
                        "Specifying assignment subtype but not referencing assignments? in " + associationDefinition);
            }
            return targetItem.getRealValues(AssignmentType.class).stream()
                    .filter(a -> a.getSubtype().contains(assignmentSubtype))
                    .toList();
        }

        private AssignmentType instantiateTargetObject() {
            // FIXME temporary
            var assignment = new AssignmentType();
            var subtype = inboundDefinition.getFocusSpecification().getAssignmentSubtype();
            if (subtype != null) {
                assignment.subtype(subtype);
            }
            return assignment;
        }

        // FIXME temporary
        private ItemSynchronizationReactionDefinition determineReaction(SimplifiedCorrelationResult correlationResult) {
            var synchronizationState = ItemSynchronizationState.fromCorrelationResult(correlationResult);
            ItemSynchronizationSituationType situation = synchronizationState.situation();
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

        private void executeReaction(
                @NotNull SimplifiedCorrelationResult correlationResult,
                @Nullable ItemSynchronizationReactionDefinition synchronizationReaction,
                @NotNull OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
                CommunicationException, StopProcessingProjectionException, ObjectNotFoundException {
            if (synchronizationReaction == null) {
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
                    executeSynchronize(correlationResult, result);
                } else {
                    throw new UnsupportedOperationException("Action " + action + " is not supported here");
                }
            }
        }

        private void executeAdd(@NotNull OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
                CommunicationException, StopProcessingProjectionException, ObjectNotFoundException {
            var focusContext = lensContext.getFocusContextRequired();
            long id = focusContext.getTemporaryContainerId(focusItemPath);
            focusContext.swallowToSecondaryDelta(
                    PrismContext.get().deltaFor(FocusType.class)
                            .item(focusItemPath)
                            .add(instantiateTargetObject().id(id))
                            .asItemDelta());
            LOGGER.trace("Going to ADD a new focus-side value ({}/{}) for associated object: {}",
                    focusItemPath, id, associationDefinition);
            collectChildMappings(id, result);
        }

        private void executeSynchronize(@NotNull SimplifiedCorrelationResult correlationResult, @NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                ConfigurationException, StopProcessingProjectionException, ObjectNotFoundException {
            var owner = stateNonNull(
                    correlationResult.getOwner(),
                    "Cannot invoke SYNCHRONIZE action without the owner");
            var ownerId = stateNonNull(
                    owner.asPrismContainerValue().getId(),
                    "Cannot invoke SYNCHRONIZE action on an owner without PCV ID: %s", owner);
            LOGGER.trace("Going to SYNCHRONIZE existing focus-side value ({}/{}) for associated object: {}",
                    focusItemPath, ownerId, associationDefinition);
            collectChildMappings(ownerId, result);
        }

        private void collectChildMappings(long id, @NotNull OperationResult result)
                throws ConfigurationException, SchemaException, ObjectNotFoundException, SecurityViolationException,
                CommunicationException, ExpressionEvaluationException, StopProcessingProjectionException {
            MSource childSource = new FullSource(
                    associationValue.getShadow().getPrismObject(),
                    null, // TODO
                    associationValue.getAssociatedObjectDefinition(),
                    inboundDefinition,
                    projectionContext,
                    context,
                    associationDefinition);
            child(childSource, focusItemPath.append(id))
                    .collectOrEvaluate(result);
        }

        private void executeDelete() {
            throw new UnsupportedOperationException("Sorry, 'delete' action is not supported yet");
        }
    }
}
