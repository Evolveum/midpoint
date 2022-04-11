/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationMonitoringLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationMonitoringType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MonitoredOperationType;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationMonitoringLevelType.COUNT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationMonitoringLevelType.FULL;

import static java.util.Collections.unmodifiableMap;

public class OperationMonitoringConfiguration implements Serializable {

    /**
     * What operations should be monitored, and to what level?
     */
    @NotNull private final Map<MonitoredOperationType, OperationMonitoringLevelType> levelsMap;

    private OperationMonitoringConfiguration(@NotNull Map<MonitoredOperationType, OperationMonitoringLevelType> levelsMap) {
        this.levelsMap = levelsMap;
    }

    public static @NotNull OperationMonitoringConfiguration create(List<OperationMonitoringType> monitoringList) {
        Map<MonitoredOperationType, OperationMonitoringLevelType> levelsMap = new HashMap<>();
        for (OperationMonitoringType monitoring : monitoringList) {
            OperationMonitoringLevelType configuredLevel = MoreObjects.firstNonNull(monitoring.getLevel(), FULL);
            List<MonitoredOperationType> operationList = monitoring.getOperation().isEmpty() ?
                    List.of(MonitoredOperationType.values()) : monitoring.getOperation();
            for (MonitoredOperationType operation : operationList) {
                levelsMap.compute(operation,
                        (ignored, oldLevel) -> max(oldLevel, configuredLevel));
            }
        }
        return new OperationMonitoringConfiguration(
                unmodifiableMap(levelsMap));
    }

    private static OperationMonitoringLevelType max(OperationMonitoringLevelType level1, OperationMonitoringLevelType level2) {
        if (level1 == FULL || level2 == FULL) {
            return FULL;
        } else if (level1 == COUNT || level2 == COUNT) {
            return COUNT;
        } else {
            return null;
        }
    }

    public static OperationMonitoringConfiguration empty() {
        return new OperationMonitoringConfiguration(Map.of());
    }

    public @Nullable OperationMonitoringLevelType get(MonitoredOperationType operation) {
        return levelsMap.get(operation);
    }
}
