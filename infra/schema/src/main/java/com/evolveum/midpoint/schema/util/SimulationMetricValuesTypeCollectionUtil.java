/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricValuesType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Collectors;

public class SimulationMetricValuesTypeCollectionUtil {

    public @Nullable static SimulationMetricValuesType findByRef(
            @NotNull Collection<SimulationMetricValuesType> metrics,
            @NotNull SimulationMetricReferenceType ref) {
        var matching = metrics.stream()
                .filter(mv -> ref.equals(mv.getRef()))
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                matching,
                () -> new IllegalStateException(
                        String.format("Multiple occurrences of '%s': %s", ref, matching)));
    }
}
