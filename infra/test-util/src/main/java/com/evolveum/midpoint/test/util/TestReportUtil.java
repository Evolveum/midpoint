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
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Reports performance-related statistical data collected in task or globally via reports in {@link TestMonitor}.
 *
 * Consider renaming this class.
 */
public class TestReportUtil {

    private static final String GLOBAL_PERFORMANCE_INFORMATION = "globalPerformanceInformation";
    private static final String OPERATION_PERFORMANCE = "operationPerformance";
    private static final String COMPONENT_PERFORMANCE = "componentPerformance";
    private static final String REPOSITORY_PERFORMANCE = "repositoryPerformance";
    private static final String CACHE_PERFORMANCE = "cachePerformance";
    private static final String PROVISIONING_STATISTICS = "provisioningStatistics";

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

        addPrinterDataAsNewSection(testMonitor, GLOBAL_PERFORMANCE_INFORMATION, printer);
    }

    /** Adds data from `printer` as a new report section in the {@link TestMonitor}. */
    private static void addPrinterDataAsNewSection(
            @NotNull TestMonitor testMonitor, @NotNull String sectionName, @NotNull AbstractStatisticsPrinter<?> printer) {
        printer.prepare();
        var section = testMonitor.addReportSection(sectionName)
                .withColumns(printer.getColumnLabelsAsArray());
        printer.getRawDataStream().forEach(section::addRow);
    }

    /**
     * Adds data from `printer` to report section in {@link TestMonitor} (created if not existing), discriminating by
     * the column named `descColumnName` with the value of `descColumnValue`.
     */
    @SuppressWarnings("SameParameterValue")
    private static void addPrinterDataAsNewOrExistingSection(
            @NotNull TestMonitor testMonitor, @NotNull String sectionName,
            @NotNull String descColumnName, Object descColumnValue,
            @NotNull AbstractStatisticsPrinter<?> printer) {
        printer.prepare();
        var section = testMonitor.findOrAddReportSection(sectionName)
                .withColumnsChecked(
                        prefixArray(descColumnName, String[]::new, printer.getColumnLabelsAsArray()));
        printer.getRawDataStream().forEach(
                row -> section.addRow(
                        prefixArray(descColumnValue, Object[]::new, row)));
    }

    private static <T> T[] prefixArray(@NotNull T first, IntFunction<T[]> arrayConstructor, T[] rest) {
        var result = arrayConstructor.apply(rest.length + 1);
        result[0] = first;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }

    /**
     * Adds operation performance for a given task to the {@link TestMonitor}.
     */
    public static void reportTaskOperationPerformance(
            TestMonitor testMonitor, String label, OperationStatsType stats, Integer iterations, Integer seconds) {

        var info = stats != null ? stats.getOperationsPerformanceInformation() : null;

        var printer = new OperationsPerformanceInformationPrinter(
                Objects.requireNonNullElseGet(info, OperationsPerformanceInformationType::new),
                new AbstractStatisticsPrinter.Options(RAW, TIME),
                iterations, seconds, false);

        addPrinterDataAsNewSection(testMonitor, label + ":" + OPERATION_PERFORMANCE, printer);
    }

    /**
     * Adds component performance for a given task to the {@link TestMonitor}.
     */
    public static void reportTaskComponentPerformanceAsSeparateSection(
            TestMonitor testMonitor, String label, OperationStatsType operationStats, Integer iterations) {
        addPrinterDataAsNewSection(
                testMonitor,
                label + ":" + COMPONENT_PERFORMANCE,
                getComponentsPerformanceInformationPrinter(operationStats, iterations));
    }

    /**
     * Adds component performance for a given task to the {@link TestMonitor}, into a section named `sectionName`
     * that aggregates the performance of all components for all tasks.
     */
    public static void reportTaskComponentPerformanceToSingleSection(
            TestMonitor testMonitor, String taskIdentification, OperationStatsType operationStats, Integer iterations) {
        addPrinterDataAsNewOrExistingSection(
                testMonitor, COMPONENT_PERFORMANCE, "task", taskIdentification,
                getComponentsPerformanceInformationPrinter(operationStats, iterations));
    }

    private static @NotNull ComponentsPerformanceInformationPrinter getComponentsPerformanceInformationPrinter(
            OperationStatsType operationStats, Integer iterations) {
        var operationsInfo =
                Objects.requireNonNullElseGet(
                        operationStats != null ? operationStats.getOperationsPerformanceInformation() : null,
                        OperationsPerformanceInformationType::new);

        var componentsInfo = ComponentsPerformanceInformationUtil.computeBasic(operationsInfo);

        return new ComponentsPerformanceInformationPrinter(
                componentsInfo,
                new AbstractStatisticsPrinter.Options(RAW, TIME),
                iterations);
    }

    /**
     * Adds repository performance for a given task to the {@link TestMonitor}.
     */
    public static void reportTaskRepositoryPerformance(TestMonitor testMonitor, String label,
            OperationStatsType operationStats, Integer iterations, Integer seconds) {
        RepositoryPerformanceInformationType performanceInformationFromTask =
                operationStats != null ? operationStats.getRepositoryPerformanceInformation() : null;
        RepositoryPerformanceInformationType performanceInformation = performanceInformationFromTask != null ?
                performanceInformationFromTask : new RepositoryPerformanceInformationType();

        RepositoryPerformanceInformationPrinter printer = new RepositoryPerformanceInformationPrinter(performanceInformation,
                new AbstractStatisticsPrinter.Options(RAW, NAME), iterations, seconds);

        addPrinterDataAsNewSection(testMonitor, label + ":" + REPOSITORY_PERFORMANCE, printer);
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

        addPrinterDataAsNewSection(testMonitor, label + ":" + CACHE_PERFORMANCE, printer);
    }

    /**
     * Adds provisioning operations statistics for a given task to the {@link TestMonitor}.
     */
    public static void reportTaskProvisioningStatistics(TestMonitor testMonitor, String label, OperationStatsType operationStats) {
        EnvironmentalPerformanceInformationType envPerformanceInformationFromTask =
                operationStats != null ? operationStats.getEnvironmentalPerformanceInformation() : null;
        ProvisioningStatisticsType provisioningStatisticsFromTask = envPerformanceInformationFromTask != null ?
                envPerformanceInformationFromTask.getProvisioningStatistics() : null;
        ProvisioningStatisticsType provisioningStatistics = provisioningStatisticsFromTask != null ?
                provisioningStatisticsFromTask : new ProvisioningStatisticsType();

        ProvisioningStatisticsPrinter printer = new ProvisioningStatisticsPrinter(provisioningStatistics,
                new AbstractStatisticsPrinter.Options(RAW, NAME));

        addPrinterDataAsNewSection(testMonitor, label + ":" + PROVISIONING_STATISTICS, printer);
    }
}
