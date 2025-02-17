/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.util.statistics.OperationsPerformanceInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleComponentPerformanceInformationType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ComponentsPerformanceInformationType;

import java.util.Optional;

public class ComponentsPerformanceInformationUtil {

    public static ComponentsPerformanceInformationType computeBasic(@NotNull OperationsPerformanceInformationType operationsInfo) {
        return new ComponentsPerformanceComputer(BasicComponentStructure.componentDescriptions())
                .compute(operationsInfo);
    }

    public static ComponentsPerformanceInformationType computeBasic(OperationsPerformanceInformation operationsInfo) {
        return computeBasic(
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(operationsInfo));
    }

    public static String format(@NotNull ComponentsPerformanceInformationType info) {
        return format(info, null, null);
    }

    public static String format(
            @NotNull ComponentsPerformanceInformationType info,
            AbstractStatisticsPrinter.Options options,
            Integer iterations) {
        return new ComponentsPerformanceInformationPrinter(info, options, iterations)
                .print();
    }

    public static Long getTotalTime(@NotNull ComponentsPerformanceInformationType information, @NotNull String name) {
        return findComponent(information, name)
                .map(SingleComponentPerformanceInformationType::getTotalTime)
                .orElse(null);
    }

    private static Optional<SingleComponentPerformanceInformationType> findComponent(
            @NotNull ComponentsPerformanceInformationType information, @NotNull String name) {
        return information.getComponent().stream()
                .filter(component -> name.equals(component.getName()))
                .findFirst();
    }

}
