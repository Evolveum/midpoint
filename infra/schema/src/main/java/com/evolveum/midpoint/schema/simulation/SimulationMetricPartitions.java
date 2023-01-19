/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.simulation;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricAggregationFunctionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionType;

public class SimulationMetricPartitions {

    @NotNull private final Map<PartitionScope, SimulationMetricPartition> partitions = new HashMap<>();

    public List<SimulationMetricPartitionType> toPartitionBeans(@NotNull SimulationMetricAggregationFunctionType function) {
        return partitions.entrySet().stream()
                .map(e -> e.getValue().toBean(e.getKey(), function))
                .collect(Collectors.toList());
    }

    public void addObject(PartitionScope key, BigDecimal sourceMetricValue, boolean inSelection) {
        partitions
                .computeIfAbsent(key, (k) -> new SimulationMetricPartition())
                .addObject(sourceMetricValue, inSelection);
    }

    void addPartition(SimulationMetricPartitionType sourcePartitionBean) {
        PartitionScope key = PartitionScope.fromBean(sourcePartitionBean.getScope());
        partitions
                .computeIfAbsent(key, (k) -> new SimulationMetricPartition())
                .addPartition(sourcePartitionBean);
    }
}
