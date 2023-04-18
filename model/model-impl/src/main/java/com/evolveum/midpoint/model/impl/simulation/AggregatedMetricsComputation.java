/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeUtil.ALL_DIMENSIONS;
import static com.evolveum.midpoint.util.DebugUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricAggregationFunctionType.*;

import java.util.*;

import com.evolveum.midpoint.model.common.MarkManager;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.schema.simulation.SimulationMetricReference;
import com.evolveum.midpoint.schema.util.SimulationMetricPartitionDimensionsTypeUtil;

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
        Collection<SimulationMetricDefinitionType> definitions = simulationResultManager.getExplicitMetricDefinitions();
        LOGGER.trace("Processing {} global explicit metric definitions", definitions.size());
        for (SimulationMetricDefinitionType definition : definitions) {
            SimulationMetricComputationType objectValueComputation = definition.getComputation();
            SimulationMetricAggregationType aggregationComputation = definition.getAggregation();
            if (objectValueComputation == null && aggregationComputation == null) {
                continue;
            }
            String identifier = Objects.requireNonNull(definition.getIdentifier());
            var previous = addMetricAggregation(
                    MetricAggregation.create(
                            SimulationMetricReference.forExplicit(identifier),
                            aggregationComputation));
            configCheck(previous == null, "Multiple definitions for metric '%s'", identifier);
        }

        Collection<MarkType> allEventMarks = markManager.getAllEventMarks(result);
        LOGGER.trace("Processing {} event marks", allEventMarks.size());
        for (MarkType eventMark : allEventMarks) {
            addMetricAggregation(
                    MetricAggregation.create(
                            SimulationMetricReference.forMark(eventMark.getOid())));
        }

        LOGGER.trace("Processing {} built-in metrics", BuiltInSimulationMetricType.values().length);
        for (BuiltInSimulationMetricType builtIn : BuiltInSimulationMetricType.values()) {
            addMetricAggregation(
                    MetricAggregation.create(
                            SimulationMetricReference.forBuiltIn(builtIn)));
        }

        LOGGER.trace("Pre-processed {} metric aggregations", metricAggregations.size());
    }

    private MetricAggregation addMetricAggregation(MetricAggregation aggregation) {
        return metricAggregations.put(aggregation.getRef(), aggregation);
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
        @NotNull private final SimulationMetricReference ref;
        @NotNull private final SimulationMetricReference sourceRef;
        @NotNull private final SimulationMetricAggregationFunctionType aggregationFunction;
        @Nullable private final SimulationObjectPredicateType selectionRestriction;
        @Nullable private final SimulationObjectPredicateType domainRestriction;

        private MetricAggregation(
                @NotNull SimulationMetricReference ref,
                @Nullable SimulationMetricReferenceType sourceRefBean,
                @Nullable SimulationMetricAggregationFunctionType aggregationFunction,
                @Nullable SimulationObjectPredicateType selectionRestriction,
                @Nullable SimulationObjectPredicateType domainRestriction) {
            this.ref = ref;
            this.sourceRef = sourceRefBean != null ? SimulationMetricReference.fromBean(sourceRefBean) : ref;
            this.aggregationFunction = Objects.requireNonNullElse(aggregationFunction, SELECTION_TOTAL_VALUE);
            this.selectionRestriction = selectionRestriction;
            this.domainRestriction = domainRestriction;
        }

        static MetricAggregation create(@NotNull SimulationMetricReference ref) {
            return create(ref, null);
        }

        static MetricAggregation create(
                @NotNull SimulationMetricReference ref, @Nullable SimulationMetricAggregationType aggregation) {
            return new MetricAggregation(
                    ref,
                    aggregation != null ? aggregation.getSource() : null,
                    aggregation != null ? aggregation.getAggregationFunction() : null,
                    aggregation != null ? aggregation.getSelectionRestriction() : null,
                    aggregation != null ? aggregation.getDomainRestriction() : null);
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

        public @NotNull SimulationMetricReference getRef() {
            return ref;
        }

        @NotNull List<SimulationMetricValuesType> toBeans() {
            var partitionBeans = partitions.toPartitionBeans(aggregationFunction);
            if (partitionBeans.isEmpty()) {
                return List.of();
            } else {
                SimulationMetricValuesType bean = new SimulationMetricValuesType()
                        .ref(ref.toBean())
                        .aggregationFunction(aggregationFunction)
                        .sourceDimensions(
                                SimulationMetricPartitionDimensionsTypeUtil.toBean(ALL_DIMENSIONS));
                bean.getPartition().addAll(partitionBeans);
                return List.of(bean);
            }
        }
    }
}
