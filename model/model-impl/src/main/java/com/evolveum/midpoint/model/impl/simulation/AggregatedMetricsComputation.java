/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeUtil.ALL_DIMENSIONS;
import static com.evolveum.midpoint.schema.util.SimulationMetricReferenceTypeUtil.forEventMarkOid;
import static com.evolveum.midpoint.util.DebugUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricAggregationFunctionType.*;

import java.util.*;

import com.evolveum.midpoint.model.common.MarkManager;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.schema.util.SimulationMetricPartitionDimensionsTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationMetricReferenceTypeUtil;

import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.simulation.SimulationMetricPartitions;
import com.evolveum.midpoint.task.api.Task;
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
 * @see ObjectMetricsComputation
 */
class AggregatedMetricsComputation {

    private static final Trace LOGGER = TraceManager.getTrace(AggregatedMetricsComputation.class);

    private static final SimulationMetricAggregationFunctionType DEFAULT_AGGREGATION_FUNCTION = SELECTION_TOTAL_VALUE;

    private final SimulationResultManagerImpl simulationResultManager = ModelBeans.get().simulationResultManager;
    private final MarkManager markManager = ModelBeans.get().markManager;

    private final Map<SimulationMetricReference, MetricAggregation> metricAggregations = new HashMap<>();

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
            String identifier = Objects.requireNonNull(definition.getIdentifier());
            if (aggregationComputation == null) {
                aggregationComputation = new SimulationMetricAggregationType()
                        .aggregationFunction(DEFAULT_AGGREGATION_FUNCTION);
            }
            var previous = metricAggregations.put(
                    SimulationMetricReference.forMetricId(identifier),
                    MetricAggregation.forMetric(identifier, aggregationComputation));
            configCheck(previous == null, "Multiple definitions for metric '%s'", identifier);
        }
        LOGGER.trace("Pre-processed {} metrics", metricAggregations.size());

        Collection<MarkType> allEventMarks = markManager.getAllEventMarks(result);
        LOGGER.trace("Processing {} event marks", allEventMarks.size());
        for (MarkType eventMark : allEventMarks) {
            metricAggregations.put(
                    SimulationMetricReference.forMark(eventMark.getOid()),
                    MetricAggregation.forMark(eventMark));
        }
        LOGGER.trace("Pre-processed {} event marks", metricAggregations.size());
    }

    void addProcessedObject(ProcessedObjectImpl<?> processedObject, Task task, OperationResult result) throws CommonException {
        LOGGER.trace("Summarizing {}", processedObject);
        for (MetricAggregation metricAggregation : metricAggregations.values()) {
            metricAggregation.addObject(processedObject, task, result);
        }
    }

    List<SimulationMetricValuesType> toBeans() {
        List<SimulationMetricValuesType> aggregatedMetricBeans = new ArrayList<>();
        metricAggregations.forEach(
                (tagOid, aggregation) ->
                        aggregatedMetricBeans.addAll(aggregation.toBeans()));
        LOGGER.trace("Aggregated simulation metrics:\n{}", debugDumpLazily(aggregatedMetricBeans, 1));
        return aggregatedMetricBeans;
    }

    private static class MetricAggregation {

        @NotNull final SimulationMetricPartitions partitions = new SimulationMetricPartitions(ALL_DIMENSIONS);
        @NotNull private final SimulationMetricReferenceType targetRef;
        @NotNull private final SimulationMetricReferenceType sourceRef;
        @NotNull private final SimulationMetricAggregationFunctionType aggregationFunction;
        @Nullable private final SimulationObjectPredicateType selectionRestriction;
        @Nullable private final SimulationObjectPredicateType domainRestriction;

        private MetricAggregation(
                @NotNull SimulationMetricReferenceType targetRef,
                @Nullable SimulationMetricReferenceType sourceRef,
                @NotNull SimulationMetricAggregationFunctionType aggregationFunction,
                @Nullable SimulationObjectPredicateType selectionRestriction,
                @Nullable SimulationObjectPredicateType domainRestriction) {
            this.targetRef = targetRef;
            this.sourceRef = Objects.requireNonNullElse(sourceRef, targetRef);
            this.aggregationFunction = aggregationFunction;
            this.selectionRestriction = selectionRestriction;
            this.domainRestriction = domainRestriction;
        }

        static MetricAggregation forMark(MarkType eventMark) {
            return new MetricAggregation(
                    forEventMarkOid(eventMark.getOid()),
                    null,
                    SELECTION_TOTAL_VALUE,
                    null, null);
        }

        static MetricAggregation forMetric(String identifier, SimulationMetricAggregationType aggregationComputation) {
            return new MetricAggregation(
                    SimulationMetricReferenceTypeUtil.forIdentifier(identifier),
                    aggregationComputation.getSource(),
                    Objects.requireNonNullElse(aggregationComputation.getAggregationFunction(), DEFAULT_AGGREGATION_FUNCTION),
                    aggregationComputation.getSelectionRestriction(),
                    aggregationComputation.getDomainRestriction());
        }

        public void addObject(ProcessedObjectImpl<?> processedObject, Task task, OperationResult result)
                throws CommonException {
            ProcessedObjectImpl.MetricValue value = processedObject.getMetricValue(sourceRef);
            if (value == null) {
                // Not in domain of the original metric
                return;
            }
            boolean inRestrictedDomain = domainRestriction == null || processedObject.matches(domainRestriction, task, result);
            if (!inRestrictedDomain) {
                return;
            }

            boolean inOriginalSelection = value.inSelection;
            boolean inRestrictedSelection =
                    inOriginalSelection
                            && (selectionRestriction == null || processedObject.matches(selectionRestriction, task, result));
            partitions.addObject(
                    processedObject.partitionScope(),
                    value.value,
                    inRestrictedSelection);
        }

        @NotNull List<SimulationMetricValuesType> toBeans() {
            var partitionBeans = partitions.toPartitionBeans(aggregationFunction);
            if (partitionBeans.isEmpty()) {
                return List.of();
            } else {
                SimulationMetricValuesType bean = new SimulationMetricValuesType()
                        .ref(targetRef)
                        .aggregationFunction(aggregationFunction)
                        .sourceDimensions(
                                SimulationMetricPartitionDimensionsTypeUtil.toBean(ALL_DIMENSIONS));
                bean.getPartition().addAll(partitionBeans);
                return List.of(bean);
            }
        }
    }
}
