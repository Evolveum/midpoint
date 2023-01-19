/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.simulation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionScopeType;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Objects;

/**
 * Parsed form of {@link SimulationMetricPartitionScopeType}.
 *
 * TODO reconcile these two; what to do with null dimensions?
 */
public class PartitionScope {

    private final QName objectType;
    private final String resourceOid;
    private final ShadowKindType kind;
    private final String intent;

    public PartitionScope(QName objectType, String resourceOid, ShadowKindType kind, String intent) {
        this.objectType = objectType;
        this.resourceOid = resourceOid;
        this.kind = kind;
        this.intent = intent;
    }

    public static PartitionScope fromBean(SimulationMetricPartitionScopeType scope) {
        if (scope == null) {
            return new PartitionScope(null, null, null, null);
        } else {
            return new PartitionScope(
                    scope.getTypeName(),
                    scope.getResourceOid(),
                    scope.getKind(),
                    scope.getIntent());
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
                && Objects.equals(resourceOid, that.resourceOid)
                && kind == that.kind
                && Objects.equals(intent, that.intent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectType, resourceOid, kind, intent);
    }

    public SimulationMetricPartitionScopeType toBean() {
        // FIXME fill nullDimensions correctly
        SimulationMetricPartitionScopeType bean = new SimulationMetricPartitionScopeType()
                .typeName(objectType)
                .resourceOid(resourceOid)
                .kind(kind)
                .intent(intent);
        List<QName> nullDimensions = bean.getNullDimensions();
        if (objectType == null) {
            nullDimensions.add(SimulationMetricPartitionScopeType.F_TYPE_NAME);
        }
        if (resourceOid == null) {
            nullDimensions.add(SimulationMetricPartitionScopeType.F_RESOURCE_OID);
        }
        if (kind == null) {
            nullDimensions.add(SimulationMetricPartitionScopeType.F_KIND);
        }
        if (intent == null) {
            nullDimensions.add(SimulationMetricPartitionScopeType.F_INTENT);
        }
        return bean;
    }
}
