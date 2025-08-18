/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.model.api.correlation.SimplifiedCorrelationResult;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.DefaultSingleShadowInboundsProcessingContextImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessing;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.InboundMappingContextSpecification;
import com.evolveum.midpoint.model.impl.sync.ItemSynchronizationState;
import com.evolveum.midpoint.model.impl.sync.PreMappingsEvaluator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition.ItemSynchronizationReactionDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.util.MiscUtil.*;

/**
 * Synchronizes complex attribute values by correlating and mapping them to values of respective focus item.
 */
class ComplexAttributeSynchronizationExpressionEvaluator<C extends Containerable>
        extends AbstractExpressionEvaluator<
        PrismContainerValue<C>,
        PrismContainerDefinition<C>,
        ComplexAttributeSynchronizationExpressionEvaluatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(ComplexAttributeSynchronizationExpressionEvaluator.class);

    private static final String OP_PROCESS_COMPLEX_ATTRIBUTE_VALUE =
            ComplexAttributeSynchronizationExpressionEvaluator.class.getName() + ".processComplexAttributeValue";

    ComplexAttributeSynchronizationExpressionEvaluator(
            QName elementName,
            ComplexAttributeSynchronizationExpressionEvaluatorType evaluatorBean,
            PrismContainerDefinition<C> outputDefinition,
            Protector protector) {
        super(elementName, evaluatorBean, outputDefinition, protector);
    }

    @Override
    public AssociationSynchronizationResult<PrismContainerValue<C>> evaluate(
            ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        checkEvaluatorProfile(context);

        var defaultSource = stateNonNull(context.getDefaultSource(), "No default source");
        var refAttrDefinition =
                castSafely(
                        stateNonNull(defaultSource.getDefinition(), "No reference attribute definition"),
                        ShadowReferenceAttributeDefinition.class);

        var inputTriple = defaultSource.getDeltaSetTriple();

        // Currently we take only non-negative values
        Collection<? extends PrismValue> inputValues = inputTriple != null ? inputTriple.getNonNegativeValues() : List.of();

        // Actually, this should be called only once; at least for mappings
        return new Evaluation(inputValues, refAttrDefinition, context)
                .process(result);
    }

    class Evaluation {

        @NotNull private final Collection<? extends PrismValue> inputValues;
        @NotNull private final AssociationSynchronizationResult<PrismContainerValue<C>> evaluatorResult =
                new AssociationSynchronizationResult<>();
        @NotNull private final ShadowReferenceAttributeDefinition refAttrDefinition;
        @NotNull private final ExpressionEvaluationContext context;

        @NotNull private final LensProjectionContext projectionContext =
                (LensProjectionContext) ModelExpressionThreadLocalHolder.getProjectionContextRequired();
        @NotNull private final ResourceType resource = projectionContext.getResourceRequired();

        @NotNull private final ItemPath focusItemPath;
        @NotNull private final PrismContainerDefinition<C> focusItemDefinition;
        @NotNull private final Collection<C> existingFocusValues;

        Evaluation(
                @NotNull Collection<? extends PrismValue> inputValues,
                @NotNull ShadowReferenceAttributeDefinition refAttrDefinition,
                @NotNull ExpressionEvaluationContext context)
                throws ConfigurationException {
            this.inputValues = inputValues;
            this.refAttrDefinition = refAttrDefinition;
            this.context = context;
            this.focusItemPath = determineFocusItemPath(context.getTargetDefinitionBean());
            this.focusItemDefinition = determineFocusItemDefinition(focusItemPath);
            this.existingFocusValues = getExistingFocusValues();
        }

        private @NotNull ItemPath determineFocusItemPath(VariableBindingDefinitionType targetDefinitionBean)
                throws ConfigurationException {
            var path = targetDefinitionBean != null ? targetDefinitionBean.getPath() : null;
            if (path == null) {
                throw new ConfigurationException("There is no target path definition"); // TODO ref
            }
            return path.getItemPath();
        }

        private @NotNull PrismContainerDefinition<C> determineFocusItemDefinition(@NotNull ItemPath focusItemPath)
                throws ConfigurationException {
            var objectDefinition = projectionContext
                    .getLensContext()
                    .getFocusContextRequired()
                    .getObjectDefinition();
            var itemDef = objectDefinition.findItemDefinition(focusItemPath);
            configCheck( // TODO ref
                    itemDef instanceof PrismContainerDefinition<?>,
                    "'{}' does not exist in {}", focusItemPath, objectDefinition);
            configCheck( // TODO ref
                    itemDef instanceof PrismContainerDefinition<?>,
                    "'{}' is not a container in {} (it's {})",
                    focusItemPath, objectDefinition, itemDef.getClass().getSimpleName());
            //noinspection unchecked
            return (PrismContainerDefinition<C>) itemDef;
        }

        public AssociationSynchronizationResult<PrismContainerValue<C>> process(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                ConfigurationException, ObjectNotFoundException {

            LOGGER.trace("Processing {} individual values of the reference attribute '{}'",
                    inputValues.size(), refAttrDefinition.getItemName());

            for (var inputValue : inputValues) {
                var refAttrValue = (ShadowReferenceAttributeValue) inputValue;
                LOGGER.trace("Processing reference attribute value: {}", refAttrValue);
                new ValueProcessing(refAttrValue)
                        .process(result);
            }
            return evaluatorResult;
        }

        private @NotNull Collection<C> getExistingFocusValues() {
            var focusContext = ModelExpressionThreadLocalHolder.getLensContextRequired().getFocusContextRequired();
            var objectNew = focusContext.getObjectNew();
            if (objectNew == null) {
                return List.of();
            } else {
                var container = objectNew.findContainer(focusItemPath);
                //noinspection unchecked
                return container != null ? (Collection<C>) container.getRealValues() : List.of();
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

            @NotNull private final ShadowReferenceAttributeValue refAttrValue;
            @NotNull private final AbstractShadow embeddedShadow;

            ValueProcessing(@NotNull ShadowReferenceAttributeValue refAttrValue) {
                this.refAttrValue = refAttrValue;
                this.embeddedShadow = refAttrValue.getShadowRequired();
            }

            void process(OperationResult parentResult)
                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ObjectNotFoundException {

                OperationResult result = parentResult.subresult(OP_PROCESS_COMPLEX_ATTRIBUTE_VALUE)
                        .addArbitraryObjectAsParam("value", refAttrValue)
                        .build();
                try {

                    var targetValueForCorrelation = computeValueForCorrelation(result);
                    var correlationResult = executeCorrelation(targetValueForCorrelation, result);
                    var synchronizationReaction = determineReaction(targetValueForCorrelation, correlationResult);
                    executeReaction(correlationResult, synchronizationReaction, result);

                    registerValuesSeen(correlationResult);

                } catch (Throwable t) {
                    result.recordException(t);
                    throw t;
                } finally {
                    result.close();
                }
            }

            private @NotNull SimplifiedCorrelationResult executeCorrelation(
                    C valueForCorrelation, OperationResult result)
                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                    SecurityViolationException, ObjectNotFoundException {

                LOGGER.trace("Executing correlation");

                if (existingFocusValues.isEmpty()) {
                    LOGGER.trace("No candidate values found, the correlation is trivial: no owner");
                    return SimplifiedCorrelationResult.noOwner();
                }

                // TODO do correlation based on business key comparison

                return SimplifiedCorrelationResult.noOwner();
            }

            private C computeValueForCorrelation(OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ObjectNotFoundException {
                var typeDef = embeddedShadow.getObjectDefinition().getTypeDefinition();
                if (typeDef == null) {
                    throw new ExpressionEvaluationException("Couldn't evaluate inbound mapping for complex attribute value: "
                            + "no type definition for the embedded shadow: " + embeddedShadow);
                }
                var targetValue = instantiateTargetValue();
                PreMappingsEvaluator.computePreFocus(
                        embeddedShadow.getBean(), typeDef, resource, targetValue, context.getTask(), result);
                LOGGER.trace("Target (for correlation):\n{}", targetValue.debugDumpLazily(1));
                return targetValue;
            }

            private C instantiateTargetValue() {
                return focusItemDefinition
                        .createValue()
                        .asContainerable(focusItemDefinition.getTypeClass());
            }

            /** "Values seen" are determined from the PLUS and ZERO sets of the resulting triple. */
            private void registerValuesSeen(SimplifiedCorrelationResult correlationResult) {
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
                    C valueForCorrelation, SimplifiedCorrelationResult correlationResult) {
                var synchronizationState = ItemSynchronizationState.fromCorrelationResult(correlationResult);
                var situationFromCorrelation = synchronizationState.situation();
                // TODO
                return SynchronizationReactionDefinition.itemLevel(
                                new ItemSynchronizationReactionsType()
                                        .reaction(new ItemSynchronizationReactionType()
                                                .actions(new ItemSynchronizationActionsType()
                                                        .addFocusValue(new AddFocusValueItemSynchronizationActionType()))))
                        .get(0);
            }

            private void executeReaction(
                    @NotNull SimplifiedCorrelationResult correlationResult,
                    @Nullable ItemSynchronizationReactionDefinition synchronizationReaction,
                    @NotNull OperationResult result)
                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
                    CommunicationException, ObjectNotFoundException {
                if (synchronizationReaction == null) {
                    registerValuesSeen(correlationResult);
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
                        registerValuesSeen(correlationResult);
                        executeSynchronize(correlationResult, result);
                    } else {
                        throw new UnsupportedOperationException("Action " + action + " is not supported here");
                    }
                }
            }

            private void executeAdd(@NotNull OperationResult result)
                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
                    CommunicationException, ObjectNotFoundException {
                var targetValue = instantiateTargetValue();
                SingleShadowInboundsProcessing.evaluate(
                        createShadowProcessingContext(targetValue, result),
                        result);
                LOGGER.trace("Going to ADD a new value for target: {}:\n{}",
                        refAttrDefinition, targetValue.debugDumpLazily(1));
                setValueMetadata(targetValue.asPrismContainerValue(), result);
                //noinspection unchecked
                evaluatorResult.addToPlusSet(targetValue.asPrismContainerValue());
            }

            private void setValueMetadata(PrismContainerValue<?> pcv, OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                    ConfigurationException, ObjectNotFoundException {
                var metadataComputer = context.getValueMetadataComputer();
                if (metadataComputer != null) {
                    pcv.setValueMetadata(
                            metadataComputer.compute(List.of(refAttrValue), result));
                }
            }

            private void executeSynchronize(@NotNull SimplifiedCorrelationResult correlationResult, @NotNull OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ObjectNotFoundException {
                throw new UnsupportedOperationException("TODO");
//                //noinspection unchecked
//                var targetValue = Objects.requireNonNull((C) correlationResult.getOwner());
//                var innerProcessing = SingleShadowInboundsProcessing.evaluateToTripleMap(
//                        createShadowProcessingContext(targetValue, result),
//                        result);
//                var assignmentPath = AssignmentHolderType.F_ASSIGNMENT.append(Objects.requireNonNull(targetValue.getId()));
//                evaluatorResult.mergeIntoOtherTriples(assignmentPath, innerProcessing.getOutputTripleMap());
//                evaluatorResult.mergeIntoItemDefinitionsMap(assignmentPath, innerProcessing.getItemDefinitionMap());
//                evaluatorResult.mergeIntoMappingEvaluationRequestsMap(assignmentPath, innerProcessing.getEvaluationRequestsMap());
            }

            private @NotNull DefaultSingleShadowInboundsProcessingContextImpl<C> createShadowProcessingContext(
                    C targetValue, @NotNull OperationResult result)
                    throws SchemaException {
                return new DefaultSingleShadowInboundsProcessingContextImpl<>(
                        embeddedShadow,
                        resource,
                        createMappingContextSpecification(),
                        targetValue,
                        ModelBeans.get().systemObjectCache.getSystemConfigurationBean(result),
                        context.getTask(),
                        embeddedShadow.getObjectDefinition(),
                        embeddedShadow.getObjectDefinition(),
                        false);
            }

            private @NotNull InboundMappingContextSpecification createMappingContextSpecification() {
                return new InboundMappingContextSpecification(
                        projectionContext.getKey().getTypeIdentification(),
                        null,
                        projectionContext.getTag());
            }

            private void executeDelete() {
                throw new UnsupportedOperationException("Sorry, 'delete' action is not supported yet");
            }
        }
    }

    @Override
    public String shortDebugDump() {
        return "complexAttributeSynchronization";
    }
}
