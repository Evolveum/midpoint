/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionScopeType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionType;

/**
 * Util for {@link SimulationMetricPartitionType}.
 */
@SuppressWarnings("WeakerAccess")
public class SimulationMetricPartitionTypeUtil {

    public static final Set<QName> ALL_DIMENSIONS = Set.of(
            SimulationMetricPartitionScopeType.F_TYPE_NAME,
            SimulationMetricPartitionScopeType.F_STRUCTURAL_ARCHETYPE_OID,
            SimulationMetricPartitionScopeType.F_RESOURCE_OID,
            SimulationMetricPartitionScopeType.F_KIND,
            SimulationMetricPartitionScopeType.F_INTENT);

    public static boolean matches(@NotNull SimulationMetricPartitionType partition, @NotNull Set<QName> dimensions) {
        return getDimensions(partition).equals(dimensions);
    }

    private static Set<QName> getDimensions(@NotNull SimulationMetricPartitionType partition) {
        return SimulationMetricPartitionScopeTypeUtil.getDimensions(partition.getScope());
    }
}
