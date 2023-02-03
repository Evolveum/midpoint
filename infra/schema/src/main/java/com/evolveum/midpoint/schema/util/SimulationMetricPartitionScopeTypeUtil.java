/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.schema.util.SimulationMetricPartitionTypeUtil.ALL_DIMENSIONS;

import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionScopeType;

import org.jetbrains.annotations.Nullable;

/**
 * Util for {@link SimulationMetricPartitionScopeType}.
 */
@SuppressWarnings("WeakerAccess")
public class SimulationMetricPartitionScopeTypeUtil {

    public static Set<QName> getDimensions(@Nullable SimulationMetricPartitionScopeType scope) {
        if (scope == null) {
            return ALL_DIMENSIONS;
        }
        Set<QName> dimensions = new HashSet<>(scope.getNullDimensions());
        if (scope.getTypeName() != null) {
            dimensions.add(SimulationMetricPartitionScopeType.F_TYPE_NAME);
        }
        if (scope.getStructuralArchetypeOid() != null) {
            dimensions.add(SimulationMetricPartitionScopeType.F_STRUCTURAL_ARCHETYPE_OID);
        }
        if (scope.getResourceOid() != null) {
            dimensions.add(SimulationMetricPartitionScopeType.F_RESOURCE_OID);
        }
        if (scope.getKind() != null) {
            dimensions.add(SimulationMetricPartitionScopeType.F_KIND);
        }
        if (scope.getIntent() != null) {
            dimensions.add(SimulationMetricPartitionScopeType.F_INTENT);
        }
        return dimensions;
    }
}
