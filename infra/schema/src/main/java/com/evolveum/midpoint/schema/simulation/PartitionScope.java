/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.simulation;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.SimulationMetricPartitionScopeTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionScopeType;

import com.google.common.collect.Sets;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionScopeType.*;

/**
 * Parsed form of {@link SimulationMetricPartitionScopeType}.
 *
 * TODO reconcile these two; what to do with null dimensions?
 */
public class PartitionScope {

    private final QName objectType;
    private final String structuralArchetypeOid;
    private final String resourceOid;
    private final ShadowKindType kind;
    private final String intent;
    private final Set<QName> allDimensions;

    public PartitionScope(
            QName objectType,
            String structuralArchetypeOid,
            String resourceOid,
            ShadowKindType kind,
            String intent,
            Set<QName> allDimensions) {
        this.objectType = objectType;
        this.structuralArchetypeOid = structuralArchetypeOid;
        this.resourceOid = resourceOid;
        this.kind = kind;
        this.intent = intent;
        this.allDimensions = new HashSet<>(allDimensions);
    }

    public static PartitionScope fromBean(SimulationMetricPartitionScopeType scope, Set<QName> availableDimensions) {
        if (scope == null) {
            return new PartitionScope(
                    null, null, null, null, null, availableDimensions);
        } else {
            return new PartitionScope(
                    ifAvailable(scope.getTypeName(), availableDimensions, F_TYPE_NAME),
                    ifAvailable(scope.getStructuralArchetypeOid(), availableDimensions, F_STRUCTURAL_ARCHETYPE_OID),
                    ifAvailable(scope.getResourceOid(), availableDimensions, F_RESOURCE_OID),
                    ifAvailable(scope.getKind(), availableDimensions, F_KIND),
                    ifAvailable(scope.getIntent(), availableDimensions, F_INTENT),
                    Sets.intersection(
                            SimulationMetricPartitionScopeTypeUtil.getDimensions(scope),
                            availableDimensions));
        }
    }

    private static <T> T ifAvailable(T value, Set<QName> availableDimensions, ItemName dimensionName) {
        if (value != null && availableDimensions.contains(dimensionName)) {
            return value;
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PartitionScope that = (PartitionScope) o;
        return Objects.equals(objectType, that.objectType)
                && Objects.equals(structuralArchetypeOid, that.structuralArchetypeOid)
                && Objects.equals(resourceOid, that.resourceOid)
                && kind == that.kind
                && Objects.equals(intent, that.intent)
                && Objects.equals(allDimensions, that.allDimensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectType, structuralArchetypeOid, resourceOid, kind, intent);
    }

    public SimulationMetricPartitionScopeType toBean() {
        SimulationMetricPartitionScopeType bean = new SimulationMetricPartitionScopeType()
                .typeName(objectType)
                .structuralArchetypeOid(structuralArchetypeOid)
                .resourceOid(resourceOid)
                .kind(kind)
                .intent(intent);
        List<QName> nullDimensions = bean.getNullDimensions();
        if (objectType == null && allDimensions.contains(F_TYPE_NAME)) {
            nullDimensions.add(F_TYPE_NAME);
        }
        if (structuralArchetypeOid == null && allDimensions.contains(F_STRUCTURAL_ARCHETYPE_OID)) {
            nullDimensions.add(F_STRUCTURAL_ARCHETYPE_OID);
        }
        if (resourceOid == null && allDimensions.contains(F_RESOURCE_OID)) {
            nullDimensions.add(F_RESOURCE_OID);
        }
        if (kind == null && allDimensions.contains(F_KIND)) {
            nullDimensions.add(F_KIND);
        }
        if (intent == null && allDimensions.contains(F_INTENT)) {
            nullDimensions.add(F_INTENT);
        }
        return bean;
    }
}
