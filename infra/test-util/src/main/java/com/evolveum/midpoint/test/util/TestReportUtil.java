/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import java.util.Comparator;

import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationUtil;
import com.evolveum.midpoint.tools.testng.TestMonitor;
import com.evolveum.midpoint.tools.testng.TestReportSection;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleOperationPerformanceInformationType;

public class TestReportUtil {

    public static void reportPerfData(TestMonitor testMonitor) {
        OperationsPerformanceInformationType performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());
        TestReportSection section = testMonitor.addReportSection("globalPerformanceInformation")
                .withColumns("Operation", "Count", "Total time (ms)", "Min", "Max", "Avg");

        performanceInformation.getOperation().stream()
                .sorted(Comparator.comparing(SingleOperationPerformanceInformationType::getTotalTime).reversed())
                .forEach(op -> {
                    int count = zeroIfNull(op.getInvocationCount());
                    float totalTime = zeroIfNull(op.getTotalTime()) / 1000.0f;
                    section.addRow(
                            op.getName(),
                            count,
                            totalTime,
                            zeroIfNull(op.getMinTime()) / 1000.0f,
                            zeroIfNull(op.getMaxTime()) / 1000.0f,
                            avg(totalTime, count));
                });
        // TODO: How to adapt this to the code above? See OperationsPerformanceInformationPrinter for the details.
//        OperationsPerformanceInformationUtil.format(performanceInformation,
//                new AbstractStatisticsPrinter.Options(CSV, TIME), null, null));
    }

    // TODO: cleanup! taken from AbstractStatisticsPrinter
    private static int zeroIfNull(Integer n) {
        return n != null ? n : 0;
    }

    private static long zeroIfNull(Long n) {
        return n != null ? n : 0;
    }

    private static Number avg(Number total, int count) {
        return total != null && count > 0 ? total.floatValue() / count : null;
    }
}
