/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricReferenceType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Parsed form of {@link SimulationMetricReferenceType}, suitable e.g. as a map key.
 *
 * Primitive implementation, for now.
 */
public class SimulationMetricReference implements Serializable {

    private final String metricIdentifier;
    private final String eventMarkOid;

    private SimulationMetricReference(String metricIdentifier, String eventMarkOid) {
        argCheck(
                metricIdentifier != null || eventMarkOid != null,
                "Either metric identifier or event mark OID must be specified");
        this.metricIdentifier = metricIdentifier;
        this.eventMarkOid = eventMarkOid;
    }

    static SimulationMetricReference fromBean(@NotNull SimulationMetricReferenceType bean) {
        return new SimulationMetricReference(bean.getIdentifier(), getOid(bean.getEventMarkRef()));
    }

    static SimulationMetricReference forMark(@NotNull String markOid) {
        return new SimulationMetricReference(null, markOid);
    }

    static SimulationMetricReference forMetricId(@NotNull String identifier) {
        return new SimulationMetricReference(identifier, null);
    }

    public boolean isMark() {
        return eventMarkOid != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimulationMetricReference that = (SimulationMetricReference) o;
        return Objects.equals(metricIdentifier, that.metricIdentifier)
                && Objects.equals(eventMarkOid, that.eventMarkOid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricIdentifier, eventMarkOid);
    }

    @Override
    public String toString() {
        return isMark() ? eventMarkOid : metricIdentifier;
    }

    String getMarkOid() {
        return eventMarkOid;
    }

    boolean isCustomMetric() {
        return metricIdentifier != null;
    }

    String getMetricIdentifier() {
        return metricIdentifier;
    }

    @SuppressWarnings("WeakerAccess")
    public ObjectReferenceType getMarkRef() {
        return eventMarkOid != null ? ObjectTypeUtil.createObjectRef(eventMarkOid, ObjectTypes.MARK) : null;
    }
}
