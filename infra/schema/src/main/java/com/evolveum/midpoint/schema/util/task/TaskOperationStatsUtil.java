/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.util.stream.Stream;

import com.evolveum.midpoint.schema.statistics.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Utility methods related to task operation statistics.
 */
public class TaskOperationStatsUtil {

    /**
     * Provides aggregated operation statistics from this task and all its subtasks.
     * Works with stored operation stats, obviously. (We have no task instances here.)
     *
     * Assumes that the task has all subtasks filled-in.
     *
     * Currently does NOT support some low-level performance statistics, namely:
     *
     * . cachesPerformanceInformation,
     * . cachingConfiguration.
     */
    public static OperationStatsType getOperationStatsFromTree(TaskType root) {
        OperationStatsType aggregate = new OperationStatsType()
                .environmentalPerformanceInformation(new EnvironmentalPerformanceInformationType())
                .repositoryPerformanceInformation(new RepositoryPerformanceInformationType())
                .operationsPerformanceInformation(new OperationsPerformanceInformationType());

        Stream<TaskType> tasks = TaskTreeUtil.getAllTasksStream(root);
        tasks.forEach(task -> {
            OperationStatsType operationStatsBean = task.getOperationStats();
            if (operationStatsBean != null) {
                EnvironmentalPerformanceInformation.addTo(aggregate.getEnvironmentalPerformanceInformation(), operationStatsBean.getEnvironmentalPerformanceInformation());
                RepositoryPerformanceInformationUtil.addTo(aggregate.getRepositoryPerformanceInformation(), operationStatsBean.getRepositoryPerformanceInformation());
                OperationsPerformanceInformationUtil.addTo(aggregate.getOperationsPerformanceInformation(), operationStatsBean.getOperationsPerformanceInformation());
            }
        });
        return aggregate;
    }

    public static boolean isEmpty(EnvironmentalPerformanceInformationType info) {
        return info == null ||
                (isEmpty(info.getProvisioningStatistics())
                && isEmpty(info.getMappingsStatistics())
                && isEmpty(info.getNotificationsStatistics())
                && info.getLastMessage() == null
                && info.getLastMessageTimestamp() == null);
    }

    public static boolean isEmpty(NotificationsStatisticsType notificationsStatistics) {
        return notificationsStatistics == null || notificationsStatistics.getEntry().isEmpty();
    }

    public static boolean isEmpty(MappingsStatisticsType mappingsStatistics) {
        return mappingsStatistics == null || mappingsStatistics.getEntry().isEmpty();
    }

    public static boolean isEmpty(ProvisioningStatisticsType provisioningStatistics) {
        return provisioningStatistics == null || provisioningStatistics.getEntry().isEmpty();
    }

    /**
     * Computes a sum of two operation statistics.
     * Returns a modifiable object, independent from the source ones.
     */
    public static OperationStatsType sum(OperationStatsType a, OperationStatsType b) {
        if (a == null) {
            return CloneUtil.clone(b);
        } else {
            OperationStatsType sum = CloneUtil.clone(a);
            addTo(sum, b);
            return sum;
        }
    }

    /**
     * Adds an statistics increment into given aggregate statistic information.
     */
    private static void addTo(@NotNull OperationStatsType aggregate, @Nullable OperationStatsType increment) {
        if (increment == null) {
            return;
        }
        if (increment.getEnvironmentalPerformanceInformation() != null) {
            if (aggregate.getEnvironmentalPerformanceInformation() == null) {
                aggregate.setEnvironmentalPerformanceInformation(new EnvironmentalPerformanceInformationType());
            }
            EnvironmentalPerformanceInformation.addTo(aggregate.getEnvironmentalPerformanceInformation(), increment.getEnvironmentalPerformanceInformation());
        }
        if (increment.getRepositoryPerformanceInformation() != null) {
            if (aggregate.getRepositoryPerformanceInformation() == null) {
                aggregate.setRepositoryPerformanceInformation(new RepositoryPerformanceInformationType());
            }
            RepositoryPerformanceInformationUtil.addTo(aggregate.getRepositoryPerformanceInformation(), increment.getRepositoryPerformanceInformation());
        }
        if (increment.getCachesPerformanceInformation() != null) {
            if (aggregate.getCachesPerformanceInformation() == null) {
                aggregate.setCachesPerformanceInformation(new CachesPerformanceInformationType());
            }
            CachePerformanceInformationUtil.addTo(aggregate.getCachesPerformanceInformation(), increment.getCachesPerformanceInformation());
        }
    }

    public static String format(OperationStatsType statistics) {
        if (statistics == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        var environmentalPerformanceInformation = statistics.getEnvironmentalPerformanceInformation();
        if (environmentalPerformanceInformation != null) {
            sb.append("Environmental performance information\n\n")
                    .append(EnvironmentalPerformanceInformation.format(environmentalPerformanceInformation))
                    .append("\n");
        }
        var repositoryPerformanceInformation = statistics.getRepositoryPerformanceInformation();
        if (repositoryPerformanceInformation != null) {
            sb.append("Repository performance information\n\n")
                    .append(RepositoryPerformanceInformationUtil.format(repositoryPerformanceInformation))
                    .append("\n");
        }
        var cachesPerformanceInformation = statistics.getCachesPerformanceInformation();
        if (cachesPerformanceInformation != null) {
            sb.append("Cache performance information\n\n")
                    .append(CachePerformanceInformationUtil.format(cachesPerformanceInformation))
                    .append("\n");
        }
        var operationsPerformanceInformation = statistics.getOperationsPerformanceInformation();
        if (operationsPerformanceInformation != null) {
            sb.append("Methods performance information\n\n")
                    .append(OperationsPerformanceInformationUtil.format(operationsPerformanceInformation))
                    .append("\n");
            var componentsPerformanceInformation =
                    ComponentsPerformanceInformationUtil.computeBasic(operationsPerformanceInformation);
            sb.append("Components performance information\n\n")
                    .append(ComponentsPerformanceInformationUtil.format(componentsPerformanceInformation))
                    .append("\n");
        }
        return sb.toString();
    }
}
