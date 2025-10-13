/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SimulationMetricPartitionTypeCollectionUtil {

    public static List<SimulationMetricPartitionType> selectPartitions(
            @NotNull List<SimulationMetricPartitionType> allPartitions, @NotNull Set<QName> dimensions) {
        return allPartitions.stream()
                .filter(p -> SimulationMetricPartitionTypeUtil.matches(p, dimensions))
                .collect(Collectors.toList());
    }
}
