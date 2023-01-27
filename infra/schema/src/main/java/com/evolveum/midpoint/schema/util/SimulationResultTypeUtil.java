/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricValuesType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import static com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil.*;

/**
 * Utilities for {@link SimulationResultType}.
 */
@SuppressWarnings("WeakerAccess")
public class SimulationResultTypeUtil {

    public static SimulationMetricValuesType getAggregatedMetricValuesByIdentifier(
            @NotNull SimulationResultType simulationResult, String metricId) {
        return simulationResult.getMetric().stream()
                .filter(mv -> SimulationMetricValuesTypeUtil.matchesMetricIdentifier(mv, metricId))
                .findFirst().orElse(null);
    }

    public static SimulationMetricValuesType getAggregatedMetricValuesByEventTagOid(
            @NotNull SimulationResultType simulationResult, String tagOid) {
        return simulationResult.getMetric().stream()
                .filter(mv -> SimulationMetricValuesTypeUtil.matchesEventTagOid(mv, tagOid))
                .findFirst().orElse(null);
    }

    public static BigDecimal getSummarizedMetricValueByIdentifier(
            @NotNull SimulationResultType simulationResult, @NotNull String metricId) {
        return getValue(
                getAggregatedMetricValuesByIdentifier(simulationResult, metricId));
    }

    public static BigDecimal getSummarizedMetricValueByEventTag(
            @NotNull SimulationResultType simulationResult, @NotNull String tagOid) {
        return getValue(
                getAggregatedMetricValuesByEventTagOid(simulationResult, tagOid));
    }

    public static SimulationResultType collapseDimensions(SimulationResultType originalResult) {
        SimulationResultType collapsedResult = originalResult.clone();
        List<SimulationMetricValuesType> collapsedMetrics = collapsedResult.getMetric();
        collapsedMetrics.clear();
        for (SimulationMetricValuesType originalMetric : originalResult.getMetric()) {
            collapsedMetrics.add(rescale(originalMetric, Set.of()));
        }
        return collapsedResult;
    }
}
