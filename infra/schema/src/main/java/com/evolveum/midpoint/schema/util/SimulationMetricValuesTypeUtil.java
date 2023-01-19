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

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.prism.Referencable.getOid;

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

    public static boolean matchesEventTagOid(@NotNull SimulationMetricValuesType value, @NotNull String tagOid) {
        SimulationMetricReferenceType ref = value.getRef();
        return ref != null && tagOid.equals(getOid(ref.getEventTagRef()));
    }

    public static BigDecimal getValue(SimulationMetricValuesType mv) {
        return MiscUtil.extractSingletonRequired(
                        findOrComputePartitions(mv, Set.of()))
                .getValue();
    }

    public static @NotNull List<SimulationMetricPartitionType> findOrComputePartitions(
            @NotNull SimulationMetricValuesType mv, @NotNull Set<QName> dimensions) {
        List<SimulationMetricPartitionType> partitions = selectPartitions(mv, dimensions);
        if (!partitions.isEmpty()) {
            return partitions;
        } else {
            return SimulationMetricComputer.computePartitions(mv, dimensions);
        }
    }

    public static List<SimulationMetricPartitionType> selectPartitions(
            @NotNull SimulationMetricValuesType mv,
            @NotNull Set<QName> dimensions) {
        return mv.getPartition().stream()
                .filter(p -> SimulationMetricPartitionTypeUtil.matches(p, dimensions))
                .collect(Collectors.toList());
    }
}
