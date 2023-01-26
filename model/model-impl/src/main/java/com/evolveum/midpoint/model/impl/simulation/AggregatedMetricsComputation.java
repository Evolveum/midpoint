/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeUtil.ALL_DIMENSIONS;
import static com.evolveum.midpoint.schema.util.SimulationMetricReferenceTypeUtil.forEventTagOid;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricAggregationFunctionType.*;

import java.math.BigDecimal;
import java.util.*;

import com.evolveum.midpoint.model.common.TagManager;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.schema.util.SimulationMetricPartitionDimensionsTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationMetricReferenceTypeUtil;

import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.simulation.SimulationMetricPartitions;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Computes metrics aggregation for the whole {@link SimulationResultType}.
 *
 * Separated from {@link SimulationResultManagerImpl} for understandability.
 *
 * TODO will be reworked as we plan to compute the aggregated metrics on-the-fly during bucket processing
 *
 * @see ObjectMetricsComputation
 */
class AggregatedMetricsComputation {

    private static final Trace LOGGER = TraceManager.getTrace(AggregatedMetricsComputation.class);

    private final SimulationResultManagerImpl simulationResultManager = ModelBeans.get().simulationResultManager;
    private final TagManager tagManager = ModelBeans.get().tagManager;

    /** Key: Tag OID, Value: Object where the data are aggregated. */
    private final Map<String, DefaultEventTagAggregation> eventTagAggregations = new HashMap<>();

    /** Key: Metric ID (original or aggregated), Value: Object where data are aggregated. */
    private final Map<String, MetricAggregation> metricAggregations = new HashMap<>();

    private AggregatedMetricsComputation() {
    }

    static AggregatedMetricsComputation create(@NotNull OperationResult result) {
        try {
            var computation = new AggregatedMetricsComputation();
            computation.initialize(result);
            return computation;
        } catch (ConfigurationException e) {
            // TODO reconsider the error handling
            throw new SystemException("Couldn't initialize part metrics: " + e.getMessage(), e);
        }
    }

    private void initialize(@NotNull OperationResult result) throws ConfigurationException {
        Collection<SimulationMetricDefinitionType> definitions = simulationResultManager.getMetricDefinitions();
        LOGGER.trace("Processing {} global metric definitions", definitions.size());
        for (SimulationMetricDefinitionType definition : definitions) {
            SimulationMetricComputationType objectValueComputation = definition.getComputation();
            SimulationMetricAggregationType aggregationComputation = definition.getAggregation();
            if (objectValueComputation == null && aggregationComputation == null) {
                continue;
            }
            String identifier = definition.getIdentifier();
            if (aggregationComputation == null) {
                aggregationComputation = new SimulationMetricAggregationType()
                        .aggregationFunction(DOMAIN_TOTAL_VALUE)
                        .source(new SimulationMetricReferenceType()
                                .identifier(identifier));
            }
            var previous = metricAggregations.put(
                    identifier,
                    new MetricAggregation(identifier, aggregationComputation, definition.getDomain()));
            configCheck(previous == null, "Multiple definitions for metric '%s'", identifier);
        }
        LOGGER.trace("Pre-processed {} metrics", metricAggregations.size());

        Collection<TagType> allEventTags = tagManager.getAllEventTags(result);
        LOGGER.trace("Processing {} event tags", allEventTags.size());
        for (TagType eventTag : allEventTags) {
            eventTagAggregations.put(eventTag.getOid(), new DefaultEventTagAggregation(eventTag));
        }
        LOGGER.trace("Pre-processed {} event tags", eventTagAggregations.size());
    }

    void addProcessedObject(ProcessedObjectImpl<?> processedObject, Task task, OperationResult result) throws CommonException {
        LOGGER.trace("Summarizing {}", processedObject);
        for (DefaultEventTagAggregation tagAggregation : eventTagAggregations.values()) {
            tagAggregation.addObject(processedObject, task, result);
        }
        for (MetricAggregation metricAggregation : metricAggregations.values()) {
            metricAggregation.addObject(processedObject, task, result);
        }
    }

    List<SimulationMetricValuesType> toBeans() {
        List<SimulationMetricValuesType> aggregatedMetricBeans = new ArrayList<>();
        eventTagAggregations.forEach(
                (tagOid, aggregation) ->
                        aggregatedMetricBeans.addAll(aggregation.toBeans()));
        metricAggregations.forEach(
                (identifier, aggregation) ->
                        aggregatedMetricBeans.addAll(aggregation.toBeans()));
        LOGGER.trace("Aggregated simulation metrics:\n{}", DebugUtil.debugDumpLazily(aggregatedMetricBeans, 1));
        return aggregatedMetricBeans;
    }

    private abstract static class AbstractAggregation {

        @NotNull final SimulationMetricPartitions partitions = new SimulationMetricPartitions(ALL_DIMENSIONS);
        @NotNull private final SimulationMetricAggregationFunctionType function;
        @NotNull private final SimulationMetricReferenceType ref;

        private AbstractAggregation(
                @NotNull SimulationMetricAggregationFunctionType function,
                @NotNull SimulationMetricReferenceType ref) {
            this.function = function;
            this.ref = ref;
        }

        @NotNull List<SimulationMetricValuesType> toBeans() {
            var partitionBeans = partitions.toPartitionBeans(function);
            if (partitionBeans.isEmpty()) {
                return List.of();
            } else {
                SimulationMetricValuesType bean = new SimulationMetricValuesType()
                        .ref(ref)
                        .aggregationFunction(SELECTION_SIZE)
                        .sourceDimensions(
                                SimulationMetricPartitionDimensionsTypeUtil.toBean(ALL_DIMENSIONS));
                bean.getPartition().addAll(partitionBeans);
                return List.of(bean);
            }
        }
    }

    private class DefaultEventTagAggregation extends AbstractAggregation {

        @NotNull private final TagType tag;
        @NotNull private final String tagOid;

        private DefaultEventTagAggregation(@NotNull TagType tag) {
            super(SELECTION_SIZE, forEventTagOid(tag.getOid()));
            this.tag = tag;
            this.tagOid = tag.getOid();
        }

        public void addObject(ProcessedObjectImpl<?> processedObject, Task task, OperationResult result)
                throws CommonException {
            if (!processedObject.isInDomainOf(tag, task, result)) {
                return;
            }
            boolean inSelection = processedObject.getEventTags().contains(tagOid);
            partitions.addObject(
                    processedObject.partitionScope(),
                    inSelection ? BigDecimal.ONE : BigDecimal.ZERO,
                    inSelection);
        }
    }

    private class MetricAggregation extends AbstractAggregation {

        @NotNull final SimulationMetricAggregationType computationDefinition;
        @Nullable final SimulationObjectPredicateType selectionPredicate;
        @Nullable final SimulationObjectPredicateType domainPredicate;
        @NotNull final SimulationMetricReferenceType sourceRef;

        MetricAggregation(
                @NotNull String identifier,
                @NotNull SimulationMetricAggregationType aggregationDefinition,
                @Nullable SimulationObjectPredicateType domainPredicate)
                throws ConfigurationException {
            super(Objects.requireNonNullElse(aggregationDefinition.getAggregationFunction(), SELECTION_TOTAL_VALUE),
                    SimulationMetricReferenceTypeUtil.forIdentifier(identifier));
            this.computationDefinition = aggregationDefinition;
            this.selectionPredicate = aggregationDefinition.getSelection();
            this.domainPredicate = domainPredicate;
            this.sourceRef =
                    configNonNull(aggregationDefinition.getSource(), () -> "no source in the definition of '" + identifier + "'");
        }

        public void addObject(ProcessedObjectImpl<?> processedObject, Task task, OperationResult result) throws CommonException {
            BigDecimal sourceMetricValue = processedObject.getMetricValue(sourceRef);
            if (sourceMetricValue == null) {
                // The processed object may be out of domain of the original metric.
                return;
            }
            boolean inDomain = domainPredicate == null || processedObject.matches(domainPredicate, task, result);
            if (inDomain) {
                boolean inSelection = selectionPredicate == null || processedObject.matches(selectionPredicate, task, result);
                partitions.addObject(processedObject.partitionScope(), sourceMetricValue, inSelection);
            }
        }
    }
}
