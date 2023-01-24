/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.simulation;

import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeCollectionUtil.selectPartitions;
import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeUtil.ALL_DIMENSIONS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.SimulationMetricPartitionDimensionsTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeCollectionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricValuesType;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricAggregationFunctionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionType;

import org.jetbrains.annotations.Nullable;

/**
 * Works with the metric computations at one place.
 *
 * (Called from various x-Type-Util classes.)
 */
public class SimulationMetricComputer {

    private static final int DEFAULT_SCALE = 10;

    /**
     * Computes "base + delta", according to all dimensions that are present.
     * We assume these dimensions are "compatible". (TODO)
     */
    public static @NotNull List<SimulationMetricValuesType> add(
            @NotNull List<SimulationMetricValuesType> base,
            @NotNull List<SimulationMetricValuesType> delta) {
        List<SimulationMetricValuesType> sum = new ArrayList<>();
        Set<SimulationMetricReferenceType> presentInDelta = new HashSet<>();
        for (SimulationMetricValuesType deltaMetric : delta) {
            SimulationMetricReferenceType deltaRef = deltaMetric.getRef();
            presentInDelta.add(deltaRef);
            SimulationMetricValuesType matchingInBase =
                    SimulationMetricValuesTypeCollectionUtil.findByRef(base, deltaRef);
            sum.add(
                    add(matchingInBase, deltaMetric));
        }
        for (SimulationMetricValuesType baseMetric : base) {
            if (!presentInDelta.contains(baseMetric.getRef())) {
                sum.add(baseMetric.clone());
            }
        }
        return sum;
    }

    private static @NotNull SimulationMetricValuesType add(
            @Nullable SimulationMetricValuesType base,
            @NotNull SimulationMetricValuesType delta) {
        if (base == null) {
            return delta.clone();
        }
        SimulationMetricPartitions sumPartitions = new SimulationMetricPartitions(ALL_DIMENSIONS);
        for (SimulationMetricPartitionType basePartition : base.getPartition()) {
            sumPartitions.addPartition(basePartition);
        }
        for (SimulationMetricPartitionType deltaPartition : delta.getPartition()) {
            sumPartitions.addPartition(deltaPartition);
        }
        // TODO we could check if aggregation function and source dimensions match
        SimulationMetricValuesType sum = base.clone();
        sum.getPartition().clear();
        sum.getPartition().addAll(
                sumPartitions.toPartitionBeans(delta.getAggregationFunction()));
        return sum;
    }

    public static List<SimulationMetricPartitionType> computePartitions(
            @NotNull SimulationMetricValuesType mv, @NotNull Set<QName> dimensions) {
        Set<QName> sourceDimensions = SimulationMetricPartitionDimensionsTypeUtil.getDimensions(mv.getSourceDimensions());
        Set<QName> missing = Sets.difference(dimensions, sourceDimensions);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Cannot compute partition for %s as the following dimension(s) are missing: %s; source = %s",
                            dimensions, missing, sourceDimensions));
        }
        SimulationMetricPartitions targetPartitions = new SimulationMetricPartitions(dimensions);
        for (SimulationMetricPartitionType sourcePartitionBean : selectPartitions(mv.getPartition(), sourceDimensions)) {
            targetPartitions.addPartition(sourcePartitionBean);
        }
        return targetPartitions.toPartitionBeans(mv.getAggregationFunction());
    }

    @SuppressWarnings("SameParameterValue")
    static BigDecimal computeValue(
            SimulationMetricPartitionType partition,
            SimulationMetricAggregationFunctionType function,
            ComputationParameters computationParameters) {
        switch (function) {
            case SELECTION_SIZE:
                return asBigDecimal(partition.getSelectionSize());
            case SELECTION_TOTAL_VALUE:
                return partition.getSelectionTotalValue();
            case DOMAIN_SIZE:
                return asBigDecimal(partition.getDomainSize());
            case DOMAIN_TOTAL_VALUE:
                return partition.getDomainTotalValue();
            case SELECTION_SIZE_TO_DOMAIN_SIZE:
                return ratio(
                        asBigDecimal(partition.getSelectionSize()),
                        asBigDecimal(partition.getDomainSize()),
                        computationParameters);
            case SELECTION_TOTAL_VALUE_TO_DOMAIN_SIZE:
                return ratio(
                        partition.getSelectionTotalValue(),
                        asBigDecimal(partition.getDomainSize()),
                        computationParameters);
            case DOMAIN_TOTAL_VALUE_TO_DOMAIN_SIZE:
                return ratio(
                        partition.getDomainTotalValue(),
                        asBigDecimal(partition.getDomainSize()),
                        computationParameters);
            case SELECTION_TOTAL_VALUE_TO_DOMAIN_TOTAL_VALUE:
                return ratio(
                        partition.getSelectionTotalValue(),
                        partition.getDomainTotalValue(),
                        computationParameters);
            case SELECTION_MIN_VALUE:
                return partition.getSelectionMinValue();
            case DOMAIN_MIN_VALUE:
                return partition.getDomainMinValue();
            case SELECTION_MAX_VALUE:
                return partition.getSelectionMaxValue();
            case DOMAIN_MAX_VALUE:
                return partition.getDomainMaxValue();
            default:
                throw new AssertionError(function);
        }
    }

    private static BigDecimal asBigDecimal(Integer value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private static BigDecimal ratio(BigDecimal part, BigDecimal total, ComputationParameters parameters) {
        if (part == null || total == null || BigDecimal.ZERO.equals(total)) {
            return null;
        } else {
            return part.divide(total, ComputationParameters.getScale(parameters), RoundingMode.HALF_UP);
        }
    }

    public static class ComputationParameters {
        private final int scale;

        public ComputationParameters(int scale) {
            this.scale = scale;
        }

        static int getScale(ComputationParameters parameters) {
            return parameters != null ? parameters.scale : DEFAULT_SCALE;
        }
    }
}
