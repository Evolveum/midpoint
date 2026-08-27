/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.focus.DeltaSetTripleIvwoMap;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequestsMap;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlation.SimplifiedCorrelationResult;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.DefaultSingleShadowInboundsProcessingContextImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessing;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.InboundMappingContextSpecification;
import com.evolveum.midpoint.model.impl.sync.PreMappingsEvaluator;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.Nullable;

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
    public ComplexItemEvaluationResult<PrismContainerValue<C>> evaluate(
            ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, SubscriptionComplianceException {

        checkEvaluatorProfile(context);

        var defaultSource = stateNonNull(context.getDefaultSource(), "No default source");
        var refAttrDefinition =
                castSafely(
                        stateNonNull(defaultSource.getDefinition(), "No complex attribute definition"),
                        ShadowReferenceAttributeDefinition.class);

        var inputTriple = defaultSource.getDeltaSetTriple();

        // Currently we take only non-negative values
        Collection<? extends PrismValue> inputValues = inputTriple != null ? inputTriple.getNonNegativeValues() : List.of();

        // Actually, this should be called only once; at least for mappings
        return new Evaluation(inputValues, refAttrDefinition, context)
                .process(result);
    }

    private class Evaluation {

        @NotNull private final Collection<? extends PrismValue> inputValues;
        @NotNull private final ComplexItemEvaluationResult<PrismContainerValue<C>> evaluatorResult =
                new ComplexItemEvaluationResult<>();
        @NotNull private final ShadowReferenceAttributeDefinition inputAttrDefinition;
        @NotNull private final ExpressionEvaluationContext context;

        @NotNull private final LensProjectionContext projectionContext =
                (LensProjectionContext) ModelExpressionThreadLocalHolder.getProjectionContextRequired();
        @NotNull private final ResourceType resource = projectionContext.getResourceRequired();

        @NotNull private final ItemPath focusItemPath;
        @NotNull private final PrismContainerDefinition<C> focusItemDefinition;
        @NotNull private final Collection<C> existingFocusValues;

        Evaluation(
                @NotNull Collection<? extends PrismValue> inputValues,
                @NotNull ShadowReferenceAttributeDefinition inputAttrDefinition,
                @NotNull ExpressionEvaluationContext context)
                throws ConfigurationException {
            this.inputValues = inputValues;
            this.inputAttrDefinition = inputAttrDefinition;
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
                    "'%s' does not exist in %s", focusItemPath, objectDefinition);
            configCheck( // TODO ref
                    itemDef instanceof PrismContainerDefinition<?>,
                    "'%s' is not a container in %s (it's %s)",
                    focusItemPath, objectDefinition, itemDef.getClass().getSimpleName());
            //noinspection unchecked
            return (PrismContainerDefinition<C>) itemDef;
        }

        ComplexItemEvaluationResult<PrismContainerValue<C>> process(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {

            LOGGER.trace("Processing {} individual values of the complex attribute '{}'",
                    inputValues.size(), inputAttrDefinition.getItemName());

            for (var inputValue : inputValues) {
                var refAttrValue = (ShadowReferenceAttributeValue) inputValue;
                LOGGER.trace("Processing complex attribute value: {}", refAttrValue);
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
         * Complex processing of an embedded object (later: any embedded value):
         *
         * 1. transforming to object for correlation ("pre-focus")
         * 2. determining the target PCV + action (synchronizing or not)
         * 3. collecting the mappings
         */
        private class ValueProcessing {

            /** The "reference value" wrapping the complex attribute value. */
            @NotNull private final ShadowReferenceAttributeValue inputAttrValue;

            /** Unwrapped complex attribute value. */
            @NotNull private final AbstractShadow inputValueAsShadow;

            /** Definition of the complex attribute type, if available. If not, we do our best to copy the values as-is. */
            @Nullable private final ResourceObjectTypeDefinition complexAttrTypeDef;

            ValueProcessing(@NotNull ShadowReferenceAttributeValue inputAttrValue) {
                this.inputAttrValue = inputAttrValue;
                this.inputValueAsShadow = inputAttrValue.getShadowRequired();
                this.complexAttrTypeDef = inputValueAsShadow.getObjectDefinition().getTypeDefinition();
            }

            void process(OperationResult parentResult)
                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {

                OperationResult result = parentResult.subresult(OP_PROCESS_COMPLEX_ATTRIBUTE_VALUE)
                        .addArbitraryObjectAsParam("value", inputAttrValue)
                        .build();
                try {

                    var targetValueForCorrelation = computeValueForCorrelation(result);
                    var correlationResult = executeCorrelation(targetValueForCorrelation);
                    executeReaction(correlationResult, result);

                    registerValuesSeen(correlationResult);

                } catch (Throwable t) {
                    result.recordException(t);
                    throw t;
                } finally {
                    result.close();
                }
            }

            private @NotNull SimplifiedCorrelationResult executeCorrelation(C valueForCorrelation) {

                LOGGER.trace("Executing correlation");

                if (existingFocusValues.isEmpty()) {
                    LOGGER.trace("No candidate values found, the correlation is trivial: no owner");
                    return SimplifiedCorrelationResult.noOwner();
                }

                var naturalKey = focusItemDefinition.getNaturalKeyInstance();
                if (naturalKey == null) {
                    LOGGER.trace("No natural key, no owner");
                    return SimplifiedCorrelationResult.noOwner();
                }

                var matching = existingFocusValues.stream()
                        .filter(v -> naturalKey.valuesMatch(v.asPrismContainerValue(), valueForCorrelation.asPrismContainerValue()))
                        .toList();

                if (matching.isEmpty()) {
                    return SimplifiedCorrelationResult.noOwner();
                } else if (matching.size() == 1) {
                    var match = matching.get(0);
                    LOGGER.trace("Correlation found a single match: {}", match);
                    return SimplifiedCorrelationResult.existingOwner(matching.get(0));
                } else {
                    // TODO implement more seriously
                    throw new IllegalStateException("Multiple matching values found for correlation, cannot decide: " + matching);
                }
            }

            private C computeValueForCorrelation(OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
                var targetValue = instantiateTargetValue();
                if (complexAttrTypeDef == null) {
                    LOGGER.trace(
                            "No explicit type definition for {}. We do our best to copy the values as-is.", inputValueAsShadow);
                    copyValuesFromInputShadow(targetValue);
                } else {
                    PreMappingsEvaluator.computePreFocus(
                            inputValueAsShadow.getBean(),
                            complexAttrTypeDef,
                            determineInboundProcessingDefinition(complexAttrTypeDef),
                            resource,
                            targetValue,
                            context.getTask(),
                            result);
                }
                LOGGER.trace("Target (for correlation):\n{}", targetValue.debugDumpLazily(1));
                return targetValue;
            }

            /**
             * Result of fake "as-is" mapping execution in {@link #copyValuesFromInputShadow(Containerable)}.
             */
            private record AsIsMappingResult(
                    DeltaSetTripleIvwoMap tripleMap,
                    PathKeyedMap<ItemDefinition<?>> itemDefinitionsMap,
                    MappingEvaluationRequestsMap evaluationRequestsMap) {

                private AsIsMappingResult() {
                    this(new DeltaSetTripleIvwoMap(), new PathKeyedMap<>(), new MappingEvaluationRequestsMap());
                }
            }

            /**
             * Copies simple attributes from the {@link #inputValueAsShadow} to the target (focus) structured value
             * in an "as-is" fashion. For example, copying values from SCIM email to {@link EmailAddressType}:
             * both have `type`, `primary` and `value` properties, although from different namespaces.
             *
             * This is the only option if there is no explicit complex attribute type definition.
             *
             * Notes:
             *
             * - Converts individual values to the target type, if necessary, using {@link JavaTypeConverter}.
             * - Ignores attributes that do not have a corresponding property definition in the target.
             * - Fails if no corresponding items are found.
             *
             * Limitation / future work:
             *
             * - We deal only with simple attributes for now. Not doing recursive copying of complex attributes,
             * ignoring references.
             * - Should we fail if the items are there but without values?
             */
            private AsIsMappingResult copyValuesFromInputShadow(C targetValue) throws SchemaException {
                var mappingResult = new AsIsMappingResult();
                for (ShadowSimpleAttribute<?> simpleAttribute : inputValueAsShadow.getSimpleAttributes()) {
                    LOGGER.trace("Copying simple attribute to target: {}", simpleAttribute);
                    var name = simpleAttribute.getElementName();
                    var targetPropertyDef =
                            focusItemDefinition.findPropertyDefinition(ItemName.from("", name.getLocalPart()));
                    if (targetPropertyDef != null) {
                        ItemName targetPropertyName = targetPropertyDef.getItemName();
                        @SuppressWarnings("unchecked")
                        PrismProperty<Object> targetProperty =
                                targetValue.asPrismContainerValue().findOrCreateProperty(targetPropertyName);
                        targetProperty.clear();
                        for (Object realValue : simpleAttribute.getRealValues()) {
                            var targetPropertyRealValue = JavaTypeConverter.convert(targetPropertyDef.getTypeClass(), realValue);
                            targetProperty.addRealValue(targetPropertyRealValue);
                        }

                        mappingResult.tripleMap.put(targetPropertyName, createDeltaSetTriple(targetProperty));
                        mappingResult.itemDefinitionsMap.put(targetPropertyName, targetPropertyDef);
                        // HACK! This information seems to be used for determining IvwoConsolidator#deleteExistingValues.
                        // We hope it will not cause any problems.
                        mappingResult.evaluationRequestsMap.put(targetPropertyName, List.of());
                    } else {
                        LOGGER.trace("Target does not have a property definition for '{}', skipping", name);
                    }
                }
                if (targetValue.asPrismContainerValue().hasNoItems()) {
                    throw new SchemaException(
                            "Couldn't convert source complex value to the target type: no corresponding items found");
                }
                return mappingResult;
            }

            /**
             * We need to create fake {@link DeltaSetTriple} for the newly computed target property.
             * We simulate strong mappings here. No origin.
             */
            private static DeltaSetTriple<ItemValueWithOrigin<?, ?>> createDeltaSetTriple(PrismProperty<Object> targetProperty) {

                // This is a delta set triple with plain PrismValues, without any origin information.
                // Used to create "producer" below.
                PrismValueDeltaSetTriple<PrismValue> deltaSetTriplePlain =
                        PrismContext.get().deltaFactory().createPrismValueDeltaSetTriple();
                for (PrismValue targetPropertyValue : targetProperty.getValues()) {
                    deltaSetTriplePlain.addToZeroSet(targetPropertyValue);
                }

                // This is a substition for implicit "as is" mapping after evaluation.
                var producer = new PrismValueDeltaSetTripleProducer<>() {
                    @Override
                    public String toHumanReadableDescription() {
                        return "";
                    }

                    @Override
                    public String debugDump(int indent) {
                        return "";
                    }

                    @Override
                    public QName getTargetItemName() {
                        return targetProperty.getElementName();
                    }

                    @Override
                    public PrismValueDeltaSetTriple<PrismValue> getOutputTriple() {
                        return deltaSetTriplePlain;
                    }

                    @Override
                    public @NotNull MappingStrengthType getStrength() {
                        return MappingStrengthType.STRONG;
                    }

                    @Override
                    public PrismValueDeltaSetTripleProducer<PrismValue, ItemDefinition<?>> clone() {
                        try {
                            //noinspection unchecked
                            return (PrismValueDeltaSetTripleProducer<PrismValue, ItemDefinition<?>>) super.clone();
                        } catch (CloneNotSupportedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public boolean isExclusive() {
                        return false;
                    }

                    @Override
                    public boolean isAuthoritative() {
                        return false;
                    }

                    @Override
                    public boolean isSourceless() {
                        return false;
                    }

                    @Override
                    public String getIdentifier() {
                        return "";
                    }

                    @Override
                    public boolean isPushChanges() {
                        return false;
                    }

                    @Override
                    public boolean isEnabled() {
                        return true;
                    }

                    @Override
                    public @Nullable ItemDefinition<?> getTargetItemDefinition() {
                        return targetProperty.getDefinition();
                    }
                };

                DeltaSetTriple<ItemValueWithOrigin<?, ?>> deltaSetTripleWithOrigins =
                        PrismContext.get().deltaFactory().createDeltaSetTriple();
                for (PrismValue targetPropertyValue : targetProperty.getValues()) {
                    deltaSetTripleWithOrigins.addToZeroSet(
                            new ItemValueWithOrigin<>(targetPropertyValue, producer, null));
                }
                return deltaSetTripleWithOrigins;
            }

            /**
             * If the correlation definition is not set, we determine it from the business key.
             */
            private ResourceObjectInboundProcessingDefinition determineInboundProcessingDefinition(
                    ResourceObjectTypeDefinition typeDef) {
                if (typeDef.getCorrelation() != null) {
                    return typeDef;
                }
                var businessKeyItems = focusItemDefinition.getNaturalKeyConstituents();
                if (businessKeyItems == null || businessKeyItems.isEmpty()) {
                    return typeDef; // no business key, no correlation
                }
                LOGGER.trace("Creating correlation based on the business key: {}", businessKeyItems);
                return ResourceObjectInboundProcessingDefinition.withCorrelationDefinition(typeDef, businessKeyItems);
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

            private void executeReaction(
                    @NotNull SimplifiedCorrelationResult correlationResult,
                    @NotNull OperationResult result)
                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
                    CommunicationException, ObjectNotFoundException, SubscriptionComplianceException {
                var situation = correlationResult.getSituation();
                if (situation == CorrelationSituationType.NO_OWNER) {
                    executeAdd(result);
                } else if (situation == CorrelationSituationType.EXISTING_OWNER) {
                    registerValuesSeen(correlationResult);
                    executeSynchronize(correlationResult, result);
                } else {
                    // nothing reasonable can be done here
                }
            }

            private void executeAdd(@NotNull OperationResult result)
                    throws ConfigurationException, SchemaException, ExpressionEvaluationException, SecurityViolationException,
                    CommunicationException, ObjectNotFoundException, SubscriptionComplianceException {
                var targetValue = instantiateTargetValue();
                if (complexAttrTypeDef == null) {
                    copyValuesFromInputShadow(targetValue);
                } else {
                    SingleShadowInboundsProcessing.evaluate(
                            createShadowProcessingContext(targetValue, result),
                            result);
                }
                LOGGER.trace("Going to ADD a new value for target: {}:\n{}",
                        inputAttrDefinition, targetValue.debugDumpLazily(1));
                setValueMetadata(targetValue.asPrismContainerValue(), result);
                //noinspection unchecked
                evaluatorResult.addToPlusSet(targetValue.asPrismContainerValue());
            }

            private void setValueMetadata(PrismContainerValue<?> pcv, OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                    ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
                var metadataComputer = context.getValueMetadataComputer();
                if (metadataComputer != null) {
                    pcv.setValueMetadata(
                            metadataComputer.compute(List.of(inputAttrValue), result));
                }
            }

            private void executeSynchronize(@NotNull SimplifiedCorrelationResult correlationResult, @NotNull OperationResult result)
                    throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
                //noinspection unchecked
                var targetValue = Objects.requireNonNull((C) correlationResult.getOwner());
                var targetValuePath = focusItemPath.append(Objects.requireNonNull(targetValue.asPrismContainerValue().getId()));
                if (complexAttrTypeDef == null) {
                    C targetValueClone = CloneUtil.cloneCloneable(targetValue); // clone is discarded, mappingResult is important
                    var mappingResult = copyValuesFromInputShadow(targetValueClone);
                    evaluatorResult.mergeIntoInnerTriples(targetValuePath, mappingResult.tripleMap());
                    evaluatorResult.mergeIntoInnerItemDefinitionsMap(targetValuePath, mappingResult.itemDefinitionsMap());
                    evaluatorResult.mergeIntoInnerMappingEvaluationRequestsMap(
                            targetValuePath, mappingResult.evaluationRequestsMap());
                } else {
                    var innerProcessing = SingleShadowInboundsProcessing.evaluateToTripleMap(
                            createShadowProcessingContext(targetValue, result),
                            result);
                    evaluatorResult.mergeIntoInnerTriples(targetValuePath, innerProcessing.getOutputTripleMap());
                    evaluatorResult.mergeIntoInnerItemDefinitionsMap(targetValuePath, innerProcessing.getItemDefinitionMap());
                    evaluatorResult.mergeIntoInnerMappingEvaluationRequestsMap(
                            targetValuePath, innerProcessing.getEvaluationRequestsMap());
                }
            }

            private @NotNull DefaultSingleShadowInboundsProcessingContextImpl<C> createShadowProcessingContext(
                    C targetValue, @NotNull OperationResult result)
                    throws SchemaException {
                return new DefaultSingleShadowInboundsProcessingContextImpl<>(
                        inputValueAsShadow,
                        resource,
                        createMappingContextSpecification(),
                        targetValue,
                        ModelBeans.get().systemObjectCache.getSystemConfigurationBean(result),
                        context.getTask(),
                        inputValueAsShadow.getObjectDefinition(),
                        inputValueAsShadow.getObjectDefinition(),
                        false);
            }

            private @NotNull InboundMappingContextSpecification createMappingContextSpecification() {
                return new InboundMappingContextSpecification(
                        projectionContext.getKey().getTypeIdentification(),
                        null,
                        projectionContext.getTag());
            }
        }
    }

    @Override
    public String shortDebugDump() {
        return "complexAttributeSynchronization";
    }
}
