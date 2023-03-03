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

import com.evolveum.midpoint.schema.simulation.SimulationMetricReference;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricValuesType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.or0;

/**
 * Utilities for {@link SimulationResultType}.
 */
@SuppressWarnings("WeakerAccess")
public class SimulationResultTypeUtil {

    public static @Nullable SimulationMetricValuesType getMetricValuesBeanByMarkOid(
            @NotNull SimulationResultType simulationResult, String oid) {
        return getMetricValuesBean(simulationResult, SimulationMetricReference.forMark(oid));
    }

    public static @Nullable SimulationMetricValuesType getMetricValuesBean(
            @NotNull SimulationResultType simulationResult, @NotNull SimulationMetricReference ref) {
        return simulationResult.getMetric().stream()
                .filter(mv -> ref.matches(mv.getRef()))
                .findFirst().orElse(null);
    }

    public static BigDecimal getSummarizedMetricValue(
            @NotNull SimulationResultType simulationResult, @NotNull SimulationMetricReference ref) {
        return getValue(
                getMetricValuesBean(simulationResult, ref));
    }

    public static int getObjectsAdded(@NotNull SimulationResultType simulationResult) {
        return getSummarizedMetricValue(simulationResult, SimulationMetricReference.BuiltIn.ADDED)
                .intValue();
    }

    public static int getObjectsModified(@NotNull SimulationResultType simulationResult) {
        return getSummarizedMetricValue(simulationResult, SimulationMetricReference.BuiltIn.MODIFIED)
                .intValue();
    }

    public static int getObjectsDeleted(@NotNull SimulationResultType simulationResult) {
        return getSummarizedMetricValue(simulationResult, SimulationMetricReference.BuiltIn.DELETED)
                .intValue();
    }

    public static int getObjectsProcessed(@NotNull SimulationResultType simulationResult) {
        // Any of the built-in would be OK.
        SimulationMetricValuesType mv = getMetricValuesBean(simulationResult, SimulationMetricReference.BuiltIn.ADDED);
        if (mv == null) {
            return 0; // strange but maybe possible
        } else {
            return or0(collapsePartitions(mv).getDomainSize());
        }
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
