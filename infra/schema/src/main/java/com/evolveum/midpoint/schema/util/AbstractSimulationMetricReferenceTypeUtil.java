/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.prism.Referencable.getOid;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSimulationMetricReferenceType;

import org.jetbrains.annotations.NotNull;

public class AbstractSimulationMetricReferenceTypeUtil {

    // TEMPORARY CODE, just to make prototype GUI code compile
    public static String getDisplayableIdentifier(AbstractSimulationMetricReferenceType reference) {
        String identifier = reference.getIdentifier();
        if (identifier != null) {
            return identifier;
        } else {
            return getOid(reference.getEventTagRef());
        }
    }

    public static boolean isMetricIdentifier(@NotNull AbstractSimulationMetricReferenceType ref, String identifier) {
        return identifier != null && identifier.equals(ref.getIdentifier());
    }

    public static boolean isEventTag(@NotNull AbstractSimulationMetricReferenceType ref, String oid) {
        return oid != null && oid.equals(getOid(ref.getEventTagRef()));
    }
}
