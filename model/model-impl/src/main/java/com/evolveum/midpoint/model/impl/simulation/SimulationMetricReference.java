/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

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
    private final String eventTagOid;

    private SimulationMetricReference(String metricIdentifier, String eventTagOid) {
        argCheck(
                metricIdentifier != null || eventTagOid != null,
                "Either metric identifier or event tag OID must be specified");
        this.metricIdentifier = metricIdentifier;
        this.eventTagOid = eventTagOid;
    }

    public static SimulationMetricReference fromBean(@NotNull SimulationMetricReferenceType bean) {
        return new SimulationMetricReference(bean.getIdentifier(), getOid(bean.getEventTagRef()));
    }

    public static SimulationMetricReference forTag(@NotNull String tagOid) {
        return new SimulationMetricReference(null, tagOid);
    }

    public static SimulationMetricReference forMetricId(@NotNull String identifier) {
        return new SimulationMetricReference(identifier, null);
    }

    public boolean isTag() {
        return eventTagOid != null;
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
                && Objects.equals(eventTagOid, that.eventTagOid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricIdentifier, eventTagOid);
    }

    @Override
    public String toString() {
        return "SimulationMetricReference{" +
                "metricIdentifier='" + metricIdentifier + '\'' +
                ", eventTagOid='" + eventTagOid + '\'' +
                '}';
    }

    public String getTagOid() {
        return eventTagOid;
    }

    public boolean isCustomMetric() {
        return metricIdentifier != null;
    }

    public String getMetricIdentifier() {
        return metricIdentifier;
    }
}
