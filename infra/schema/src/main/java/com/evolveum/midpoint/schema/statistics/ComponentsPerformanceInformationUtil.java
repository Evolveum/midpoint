/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ComponentsPerformanceInformationType;

public class ComponentsPerformanceInformationUtil {

    public static ComponentsPerformanceInformationType computeBasic(@NotNull OperationsPerformanceInformationType operationsInfo) {
        return new ComponentsPerformanceComputer(BasicComponentStructure.componentDescriptions())
                .compute(operationsInfo);
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
}
