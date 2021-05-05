/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.Format.RAW;
import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.SortBy.TIME;

import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.tools.testng.TestMonitor;
import com.evolveum.midpoint.tools.testng.TestReportSection;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;

public class TestReportUtil {

    /**
     * Adds global performance information as a report section to the {@link TestMonitor}.
     * Does not clear the data, do not forget to call clearing in the right life-cycle method.
     */
    public static void reportGlobalPerfData(TestMonitor testMonitor) {
        OperationsPerformanceInformationType performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());

        OperationsPerformanceInformationPrinter printer = new OperationsPerformanceInformationPrinter(performanceInformation,
                new AbstractStatisticsPrinter.Options(RAW, TIME), null, null, false);

        printer.prepare();
        Data data = printer.getData();
        Formatting formatting = printer.getFormatting();

        TestReportSection section = testMonitor.addReportSection("globalPerformanceInformation")
                .withColumns(formatting.getColumnLabels().toArray(new String[0]));
        data.getRawDataStream().forEach(section::addRow);
    }
}
