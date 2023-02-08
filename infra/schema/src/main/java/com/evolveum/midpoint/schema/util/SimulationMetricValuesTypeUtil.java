/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.simulation.SimulationMetricComputer;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricValuesType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeCollectionUtil.selectPartitions;

/**
 * Util for {@link SimulationMetricValuesType}.
 *
 * More complex computations are delegated to {@link SimulationMetricComputer}.
 */
@SuppressWarnings("WeakerAccess")
public class SimulationMetricValuesTypeUtil {

    public static boolean matchesMetricIdentifier(@NotNull SimulationMetricValuesType value, @NotNull String identifier) {
        SimulationMetricReferenceType ref = value.getRef();
        return ref != null && identifier.equals(ref.getIdentifier());
    }

    public static boolean matchesEventMarkOid(@NotNull SimulationMetricValuesType value, @NotNull String tagOid) {
        SimulationMetricReferenceType ref = value.getRef();
        return ref != null && tagOid.equals(getOid(ref.getEventMarkRef()));
    }

    public static BigDecimal getValue(@Nullable SimulationMetricValuesType mv) {
        if (mv != null) {
            return MiscUtil.extractSingletonRequired(
                            findOrComputePartitions(mv, Set.of()))
                    .getValue();
        } else {
            return BigDecimal.ZERO;
        }
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
