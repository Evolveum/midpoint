/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.Format.RAW;
import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.SortBy.NAME;
import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.SortBy.TIME;

import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.tools.testng.TestMonitor;
import com.evolveum.midpoint.tools.testng.TestReportSection;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.Objects;

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

        addPrinterData(testMonitor, "globalPerformanceInformation", printer);
    }

    private static void addPrinterData(TestMonitor testMonitor, String sectionName, AbstractStatisticsPrinter<?> printer) {
        printer.prepare();
        Data data = printer.getData();
        Formatting formatting = printer.getFormatting();

        TestReportSection section = testMonitor.addReportSection(sectionName)
                .withColumns(formatting.getColumnLabels().toArray(new String[0]));
        data.getRawDataStream().forEach(section::addRow);
    }

    /**
     * Adds operation performance for a given task to the {@link TestMonitor}.
     */
    public static void reportTaskOperationPerformance(TestMonitor testMonitor, String label,
            TaskType task, Integer iterations, Integer seconds) {

        var info = task.getOperationStats() != null ? task.getOperationStats().getOperationsPerformanceInformation() : null;

        var printer = new OperationsPerformanceInformationPrinter(
                Objects.requireNonNullElseGet(info, OperationsPerformanceInformationType::new),
                new AbstractStatisticsPrinter.Options(RAW, TIME),
                iterations, seconds, false);

        addPrinterData(testMonitor, label + ":operationPerformance", printer);
    }

    /**
     * Adds component performance for a given task to the {@link TestMonitor}.
     */
    public static void reportTaskComponentPerformance(
            TestMonitor testMonitor, String label, TaskType task, Integer iterations) {

        var operationsInfo =
                Objects.requireNonNullElseGet(
                        task.getOperationStats() != null ? task.getOperationStats().getOperationsPerformanceInformation() : null,
                        OperationsPerformanceInformationType::new);

        var componentsInfo = ComponentsPerformanceInformationUtil.computeBasic(operationsInfo);

        var printer = new ComponentsPerformanceInformationPrinter(
                componentsInfo,
                new AbstractStatisticsPrinter.Options(RAW, TIME),
                iterations);

        addPrinterData(testMonitor, label + ":componentPerformance", printer);
    }

    /**
     * Adds repository performance for a given task to the {@link TestMonitor}.
     */
    public static void reportTaskRepositoryPerformance(TestMonitor testMonitor, String label,
            TaskType task, Integer iterations, Integer seconds) {
        RepositoryPerformanceInformationType performanceInformationFromTask =
                task.getOperationStats() != null ? task.getOperationStats().getRepositoryPerformanceInformation() : null;
        RepositoryPerformanceInformationType performanceInformation = performanceInformationFromTask != null ?
                performanceInformationFromTask : new RepositoryPerformanceInformationType();

        RepositoryPerformanceInformationPrinter printer = new RepositoryPerformanceInformationPrinter(performanceInformation,
                new AbstractStatisticsPrinter.Options(RAW, NAME), iterations, seconds);

        addPrinterData(testMonitor, label + ":repositoryPerformance", printer);
    }

    /**
     * Adds caches performance for a given task to the {@link TestMonitor}.
     */
    public static void reportTaskCachesPerformance(TestMonitor testMonitor, String label, TaskType task) {
        CachesPerformanceInformationType performanceInformationFromTask =
                task.getOperationStats() != null ? task.getOperationStats().getCachesPerformanceInformation() : null;
        CachesPerformanceInformationType performanceInformation = performanceInformationFromTask != null ?
                performanceInformationFromTask : new CachesPerformanceInformationType();

        CachePerformanceInformationPrinter printer = new CachePerformanceInformationPrinter(performanceInformation,
                new AbstractStatisticsPrinter.Options(RAW, NAME));

        addPrinterData(testMonitor, label + ":cachePerformance", printer);
    }

    /**
     * Adds provisioning operations statistics for a given task to the {@link TestMonitor}.
     */
    public static void reportTaskProvisioningStatistics(TestMonitor testMonitor, String label, TaskType task) {
        EnvironmentalPerformanceInformationType envPerformanceInformationFromTask =
                task.getOperationStats() != null ? task.getOperationStats().getEnvironmentalPerformanceInformation() : null;
        ProvisioningStatisticsType provisioningStatisticsFromTask = envPerformanceInformationFromTask != null ?
                envPerformanceInformationFromTask.getProvisioningStatistics() : null;
        ProvisioningStatisticsType provisioningStatistics = provisioningStatisticsFromTask != null ?
                provisioningStatisticsFromTask : new ProvisioningStatisticsType();

        ProvisioningStatisticsPrinter printer = new ProvisioningStatisticsPrinter(provisioningStatistics,
                new AbstractStatisticsPrinter.Options(RAW, NAME));

        addPrinterData(testMonitor, label + ":provisioningStatistics", printer);
    }
}
