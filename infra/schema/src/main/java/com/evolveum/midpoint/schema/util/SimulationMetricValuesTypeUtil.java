/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeCollectionUtil.selectPartitions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.simulation.SimulationMetricComputer;
import com.evolveum.midpoint.schema.simulation.SimulationMetricReference;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricValuesType;

/**
 * Util for {@link SimulationMetricValuesType}.
 *
 * More complex computations are delegated to {@link SimulationMetricComputer}.
 */
@SuppressWarnings("WeakerAccess")
public class SimulationMetricValuesTypeUtil {

    public static boolean matches(@NotNull SimulationMetricValuesType value, @NotNull SimulationMetricReference ref) {
        return ref.matches(value.getRef());
    }

    public static @NotNull BigDecimal getValue(@Nullable SimulationMetricValuesType mv) {
        if (mv != null) {
            BigDecimal value = collapsePartitions(mv).getValue();
            return Objects.requireNonNullElse(value, BigDecimal.ZERO); // just for sure; the value should be always there
        } else {
            return BigDecimal.ZERO;
        }
    }

    static @NotNull SimulationMetricPartitionType collapsePartitions(@NotNull SimulationMetricValuesType mv) {
        return MiscUtil.extractSingletonRequired(
                findOrComputePartitions(mv, Set.of()));
    }

    public static @NotNull List<SimulationMetricPartitionType> findOrComputePartitions(
            @NotNull SimulationMetricValuesType mv, @NotNull Set<QName> dimensions) {
        List<SimulationMetricPartitionType> partitions = selectPartitions(mv.getPartition(), dimensions);
        if (!partitions.isEmpty()) {
            return partitions;
        } else {
            return SimulationMetricComputer.computePartitions(mv, dimensions);
        }
    }

    // TODO better name
    public static @NotNull SimulationMetricValuesType rescale(
            @NotNull SimulationMetricValuesType originalMetric, @NotNull Set<QName> dimensions) {
        List<SimulationMetricPartitionType> originalPartitions = selectPartitions(originalMetric.getPartition(), dimensions);
        if (!originalPartitions.isEmpty()) {
            return originalMetric;
        } else {
            SimulationMetricValuesType clonedMetric = originalMetric.clone();
            clonedMetric.getPartition().clear();
            clonedMetric.getPartition().addAll(
                    SimulationMetricComputer.computePartitions(originalMetric, dimensions));
            return clonedMetric;
        }
    }
}
