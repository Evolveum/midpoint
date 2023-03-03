/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.prism.Referencable.getOid;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BuiltInSimulationMetricType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;

import org.jetbrains.annotations.NotNull;

/**
 * Util for {@link SimulationMetricReferenceType}.
 */
public class SimulationMetricReferenceTypeUtil {

    // TEMPORARY CODE, just to make prototype GUI code compile
    public static String getDisplayableIdentifier(SimulationMetricReferenceType reference) {
        if (reference == null) {
            return null;
        }

        String identifier = reference.getIdentifier();
        if (identifier != null) {
            return identifier;
        } else {
            return getOid(reference.getEventMarkRef());
        }
    }

    public static SimulationMetricReferenceType forIdentifier(String identifier) {
        return new SimulationMetricReferenceType()
                .identifier(identifier);
    }

    public static SimulationMetricReferenceType forBuiltIn(BuiltInSimulationMetricType builtIn) {
        return new SimulationMetricReferenceType()
                .builtIn(builtIn);
    }

    public static SimulationMetricReferenceType forEventMarkOid(String oid) {
        return new SimulationMetricReferenceType()
                .eventMarkRef(oid, MarkType.COMPLEX_TYPE);
    }

    public static boolean isMetricIdentifier(@NotNull SimulationMetricReferenceType ref, String identifier) {
        return identifier != null && identifier.equals(ref.getIdentifier());
    }

    public static boolean isEventMark(@NotNull SimulationMetricReferenceType ref, String oid) {
        return oid != null && oid.equals(getOid(ref.getEventMarkRef()));
    }

    public static String describe(SimulationMetricReferenceType reference) {
        if (reference == null) {
            return "(no metric)";
        }
        String identifier = reference.getIdentifier();
        if (identifier != null) {
            return "metric '" + identifier + "'";
        } else {
            return "event mark '" + getOid(reference.getEventMarkRef()) + "'";
        }
    }
}
