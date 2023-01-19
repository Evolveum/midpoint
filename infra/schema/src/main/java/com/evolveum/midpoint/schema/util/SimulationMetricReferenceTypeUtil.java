/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.prism.Referencable.getOid;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

import org.jetbrains.annotations.NotNull;

/**
 * Util for {@link SimulationMetricReferenceType}.
 */
public class SimulationMetricReferenceTypeUtil {

    // TEMPORARY CODE, just to make prototype GUI code compile
    public static String getDisplayableIdentifier(SimulationMetricReferenceType reference) {
        String identifier = reference.getIdentifier();
        if (identifier != null) {
            return identifier;
        } else {
            return getOid(reference.getEventTagRef());
        }
    }

    public static SimulationMetricReferenceType forIdentifier(String identifier) {
        return new SimulationMetricReferenceType()
                .identifier(identifier);
    }

    public static SimulationMetricReferenceType forEventTagOid(String oid) {
        return new SimulationMetricReferenceType()
                .eventTagRef(oid, TagType.COMPLEX_TYPE);
    }

    public static boolean isMetricIdentifier(@NotNull SimulationMetricReferenceType ref, String identifier) {
        return identifier != null && identifier.equals(ref.getIdentifier());
    }

    public static boolean isEventTag(@NotNull SimulationMetricReferenceType ref, String oid) {
        return oid != null && oid.equals(getOid(ref.getEventTagRef()));
    }

    public static String describe(SimulationMetricReferenceType reference) {
        if (reference == null) {
            return "(no metric)";
        }
        String identifier = reference.getIdentifier();
        if (identifier != null) {
            return "metric '" + identifier + "'";
        } else {
            return "event tag '" + getOid(reference.getEventTagRef()) + "'";
        }
    }
}
