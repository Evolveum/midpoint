/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricAggregationFunctionType.DOMAIN_TOTAL_VALUE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricAggregationFunctionType.SELECTION_TOTAL_VALUE;

/**
 * Computes metrics aggregation for the whole {@link SimulationResultType}.
 *
 * Separated from {@link SimulationResultManagerImpl} for understandability.
 *
 * @see ObjectMetricsComputation
 */
class AggregatedMetricsComputation {

    private static final Trace LOGGER = TraceManager.getTrace(AggregatedMetricsComputation.class);

    private static final int SCALE = 10;

    private final String oid;
    private final SimulationResultManagerImpl simulationResultManager;
    private final Task task;

    @NotNull private final Collection<TagType> allEventTags;

    /** Summarized (counted) event tag occurrences. */
    private final Map<String, DefaultEventTagAggregation> eventTagAggregations = new HashMap<>();

    /** Summarized object metrics + aggregated metrics. */
    private final Map<String, MetricAggregation> metricAggregations = new HashMap<>();

    private AggregatedMetricsComputation(String oid, SimulationResultManagerImpl simulationResultManager, Task task) {
        this.oid = oid;
        this.simulationResultManager = simulationResultManager;
        this.allEventTags = simulationResultManager.getAllEventTags();
        this.task = task;
    }

    static Collection<?> computeAll(
            String oid, SimulationResultManagerImpl simulationResultManager, Task task, OperationResult result)
            throws CommonException {
        return new AggregatedMetricsComputation(oid, simulationResultManager, task)
                .computeAll(result);
    }

    /** TODO implement via iterative search */
    private Collection<AggregatedSimulationMetricValueType> computeAll(OperationResult result)
            throws CommonException {
        initializeMetricComputations();
        for (ProcessedObjectImpl<?> processedObject : simulationResultManager.getStoredProcessedObjects(oid, result)) {
            processObject(processedObject, result);
        }
        return createBeans();
    }

    private void initializeMetricComputations() throws ConfigurationException {
        List<GlobalSimulationMetricDefinitionType> definitions = simulationResultManager.getMetricDefinitions();
        LOGGER.trace("Processing {} global metric definitions", definitions.size());
        for (GlobalSimulationMetricDefinitionType definition : definitions) {
            OriginalSimulationMetricComputationType objectValueComputation = definition.getObjectValue();
            AggregatedSimulationMetricComputationType aggregationComputation = definition.getAggregation();
            if (objectValueComputation == null && aggregationComputation == null) {
                continue;
            }
            String identifier = definition.getIdentifier();
            if (objectValueComputation != null) {
                if (aggregationComputation != null) {
                    throw new ConfigurationException(
                            "Both 'objectValue' and 'aggregation' definition present for metric '" + identifier + "'");
                }
                aggregationComputation = new AggregatedSimulationMetricComputationType()
                        .aggregationFunction(DOMAIN_TOTAL_VALUE)
                        .source(new AbstractSimulationMetricReferenceType()
                                .identifier(identifier));
            }
            var previous = metricAggregations.put(
                    identifier,
                    new MetricAggregation(identifier, aggregationComputation, definition.getDomain()));
            configCheck(previous == null, "Multiple definitions for metric '%s'", identifier);
        }
        LOGGER.trace("Pre-processed {} metrics", metricAggregations.size());
        LOGGER.trace("Processing {} event tags", allEventTags.size());
        for (TagType eventTag : allEventTags) {
            eventTagAggregations.put(eventTag.getOid(), new DefaultEventTagAggregation(eventTag));
        }
        LOGGER.trace("Pre-processed {} event tags", eventTagAggregations.size());
    }

    private void processObject(ProcessedObjectImpl<?> processedObject, OperationResult result) throws CommonException {
        LOGGER.trace("Summarizing {}", processedObject);
        for (DefaultEventTagAggregation tagAggregation : eventTagAggregations.values()) {
            tagAggregation.addObject(processedObject, result);
        }
        for (MetricAggregation metricAggregation : metricAggregations.values()) {
            metricAggregation.addObject(processedObject, result);
        }
    }

    private List<AggregatedSimulationMetricValueType> createBeans() {
        List<AggregatedSimulationMetricValueType> aggregatedMetricBeans = new ArrayList<>();
        eventTagAggregations.forEach(
                (tagOid, aggregation) ->
                        aggregatedMetricBeans.addAll(aggregation.getMetricBeans()));
        metricAggregations.forEach(
                (identifier, aggregation) ->
                        aggregatedMetricBeans.addAll(aggregation.getMetricBeans()));
        LOGGER.trace("Aggregated simulation metrics:\n{}", DebugUtil.debugDumpLazily(aggregatedMetricBeans, 1));
        return aggregatedMetricBeans;
    }

    private static class Classifier {
        private final QName objectType;
        private final String resourceOid;
        private final ShadowKindType kind;
        private final String intent;

        private Classifier(QName objectType, String resourceOid, ShadowKindType kind, String intent) {
            this.objectType = objectType;
            this.resourceOid = resourceOid;
            this.kind = kind;
            this.intent = intent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Classifier that = (Classifier) o;
            return Objects.equals(objectType, that.objectType)
                    && Objects.equals(resourceOid, that.resourceOid)
                    && kind == that.kind
                    && Objects.equals(intent, that.intent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(objectType, resourceOid, kind, intent);
        }

        public AggregatedSimulationMetricClassifierType toBean() {
            return new AggregatedSimulationMetricClassifierType()
                    .typeName(objectType)
                    .resourceOid(resourceOid)
                    .kind(kind)
                    .intent(intent);
        }

        private static Classifier forObject(ProcessedObjectImpl<?> processedObject) {
            // TODO flexible classification
            return new Classifier(
                    processedObject.getTypeName(),
                    processedObject.getResourceOid(),
                    processedObject.getKind(),
                    processedObject.getIntent());
        }
    }

    /** Similar to {@link MetricAggregation} but let's not complicate the code by trying to generalize these. */
    private class DefaultEventTagAggregation {

        @NotNull private final TagType tag;
        @NotNull private final String tagOid;
        @NotNull private final Map<Classifier, Segment> segments = new HashMap<>();

        private DefaultEventTagAggregation(@NotNull TagType tag) {
            this.tag = tag;
            this.tagOid = tag.getOid();
        }

        @NotNull List<AggregatedSimulationMetricValueType> getMetricBeans() {
            return segments.entrySet().stream()
                    // TODO should we filter out data with domain > 0 but selected == 0 ?
                    .map(e -> e.getValue().getMetricBean(e.getKey()))
                    .collect(Collectors.toList());
        }

        public void addObject(ProcessedObjectImpl<?> processedObject, OperationResult result) throws CommonException {
            if (!processedObject.isInDomainOf(tag, task, result)) {
                return;
            }
            Classifier classifier = Classifier.forObject(processedObject);
            segments
                    .computeIfAbsent(classifier, (key) -> new Segment())
                    .addObject(processedObject);
        }

        private class Segment {

            private int selectionSize;
            private int domainSize;

            @SuppressWarnings("DuplicatedCode")
            void addObject(@NotNull ProcessedObjectImpl<?> processedObject) {
                domainSize++;
                boolean inSelection = processedObject.getEventTags().contains(tagOid);
                if (inSelection) {
                    selectionSize++;
                }
            }

            AggregatedSimulationMetricValueType getMetricBean(Classifier classifier) {
                return new AggregatedSimulationMetricValueType()
                        .ref(new AbstractSimulationMetricReferenceType()
                                .eventTagRef(tagOid, TagType.COMPLEX_TYPE))
                        .classifier(classifier.toBean())
                        .value(BigDecimal.valueOf(selectionSize))
                        .selectionSize(selectionSize)
                        .domainSize(domainSize);
            }
        }
    }

    private class MetricAggregation {

        @NotNull private final String identifier;
        @NotNull final AggregatedSimulationMetricComputationType computationDefinition;
        @Nullable final SimulationResultProcessedObjectPredicateType selectionPredicate;
        @Nullable final SimulationResultProcessedObjectPredicateType domainPredicate;
        @NotNull final SimulationMetricAggregationFunctionType function;
        @NotNull final AbstractSimulationMetricReferenceType sourceRef;

        @NotNull private final Map<Classifier, Segment> segments = new HashMap<>();

        MetricAggregation(
                @NotNull String identifier,
                @NotNull AggregatedSimulationMetricComputationType aggregationDefinition,
                @Nullable SimulationResultProcessedObjectPredicateType domainPredicate)
                throws ConfigurationException {
            this.identifier = identifier;
            this.computationDefinition = aggregationDefinition;
            this.selectionPredicate = aggregationDefinition.getSelection();
            this.domainPredicate = domainPredicate;
            this.function = Objects.requireNonNullElse(
                    aggregationDefinition.getAggregationFunction(), SELECTION_TOTAL_VALUE);
            this.sourceRef =
                    configNonNull(aggregationDefinition.getSource(), () -> "no source in the definition of '" + identifier + "'");
        }

        @NotNull List<AggregatedSimulationMetricValueType> getMetricBeans() {
            return segments.entrySet().stream()
                    .map(e -> e.getValue().getMetricBean(e.getKey()))
                    .collect(Collectors.toList());
        }

        public void addObject(ProcessedObjectImpl<?> processedObject, OperationResult result) throws CommonException {
            BigDecimal sourceMetricValue = processedObject.getMetricValue(sourceRef);
            if (sourceMetricValue == null) {
                // The processed object may be out of domain of the original metric.
                return;
            }
            boolean inDomain = domainPredicate == null || processedObject.matches(domainPredicate, task, result);
            if (!inDomain) {
                return;
            }

            Classifier classifier = Classifier.forObject(processedObject);
            segments
                    .computeIfAbsent(classifier, (key) -> new Segment())
                    .addObject(processedObject, sourceMetricValue, result);
        }

        private class Segment {

            private int selectionSize;
            private BigDecimal selectionTotalValue = BigDecimal.ZERO;
            private int domainSize;
            private BigDecimal domainTotalValue = BigDecimal.ZERO;

            private BigDecimal selectionMinValue;
            private BigDecimal selectionMaxValue;
            private BigDecimal domainMinValue;
            private BigDecimal domainMaxValue;

            BigDecimal getValue() {
                switch (function) {
                    case SELECTION_SIZE:
                        return getSelectionSizeBigDecimal();
                    case SELECTION_TOTAL_VALUE:
                        return selectionTotalValue;
                    case DOMAIN_SIZE:
                        return getDomainSizeBigDecimal();
                    case DOMAIN_TOTAL_VALUE:
                        return domainTotalValue;
                    case SELECTION_SIZE_TO_DOMAIN_SIZE:
                        return ratio(getSelectionSizeBigDecimal(), getDomainSizeBigDecimal());
                    case SELECTION_TOTAL_VALUE_TO_DOMAIN_SIZE:
                        return ratio(selectionTotalValue, getDomainSizeBigDecimal());
                    case DOMAIN_TOTAL_VALUE_TO_DOMAIN_SIZE:
                        return ratio(domainTotalValue, getDomainSizeBigDecimal());
                    case SELECTION_TOTAL_VALUE_TO_DOMAIN_TOTAL_VALUE:
                        return ratio(selectionTotalValue, domainTotalValue);
                    case SELECTION_MIN_VALUE:
                        return selectionMinValue;
                    case DOMAIN_MIN_VALUE:
                        return domainMinValue;
                    case SELECTION_MAX_VALUE:
                        return selectionMaxValue;
                    case DOMAIN_MAX_VALUE:
                        return domainMaxValue;
                    default:
                        throw new AssertionError(function);
                }
            }

            private BigDecimal ratio(BigDecimal part, BigDecimal total) {
                if (BigDecimal.ZERO.equals(total)) {
                    return null;
                } else {
                    return part.divide(total, SCALE, RoundingMode.HALF_UP);
                }
            }

            @SuppressWarnings("DuplicatedCode")
            void addObject(@NotNull ProcessedObjectImpl<?> processedObject, BigDecimal sourceMetricValue, OperationResult result)
                    throws CommonException {
                if (domainMinValue == null || sourceMetricValue.compareTo(domainMinValue) < 0) {
                    domainMinValue = sourceMetricValue;
                }
                if (domainMaxValue == null || sourceMetricValue.compareTo(domainMaxValue) > 0) {
                    domainMaxValue = sourceMetricValue;
                }
                domainSize++;
                domainTotalValue = domainTotalValue.add(sourceMetricValue);

                boolean inSelection = selectionPredicate == null || processedObject.matches(selectionPredicate, task, result);
                if (inSelection) {
                    if (selectionMinValue == null || sourceMetricValue.compareTo(selectionMinValue) < 0) {
                        selectionMinValue = sourceMetricValue;
                    }
                    if (selectionMaxValue == null || sourceMetricValue.compareTo(selectionMaxValue) > 0) {
                        selectionMaxValue = sourceMetricValue;
                    }
                    selectionSize++;
                    selectionTotalValue = selectionTotalValue.add(sourceMetricValue);
                }
            }

            private BigDecimal getSelectionSizeBigDecimal() {
                return BigDecimal.valueOf(selectionSize);
            }

            private BigDecimal getDomainSizeBigDecimal() {
                return BigDecimal.valueOf(domainSize);
            }

            AggregatedSimulationMetricValueType getMetricBean(Classifier classifier) {
                return new AggregatedSimulationMetricValueType()
                        .ref(new AbstractSimulationMetricReferenceType()
                                .identifier(identifier))
                        .classifier(classifier.toBean())
                        .value(getValue())
                        .selectionSize(selectionSize)
                        .selectionTotalValue(selectionTotalValue)
                        .domainSize(domainSize)
                        .domainTotalValue(domainTotalValue)
                        .selectionMinValue(selectionMinValue)
                        .selectionMaxValue(selectionMaxValue)
                        .domainMinValue(domainMinValue)
                        .domainMaxValue(domainMaxValue);
            }
        }
    }
}
