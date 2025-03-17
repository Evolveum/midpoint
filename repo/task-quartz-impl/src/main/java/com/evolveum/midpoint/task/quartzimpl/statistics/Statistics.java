/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.statistics;

import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.reporting.ConnIdOperation;
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.schema.cache.CachePerformanceCollector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceInformation;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.Objects;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;

import static java.util.Collections.emptySet;

/**
 *  Code to manage operational statistics, including structured progress.
 *  Originally it was a part of the TaskQuartzImpl but it is cleaner to keep it separate.
 *
 *  It is used for
 *
 *  1) running background tasks (RunningTask) - both heavyweight and lightweight
 *  2) transient tasks e.g. those invoked from GUI
 *
 *  (The structured progress is used only for heavyweight running tasks.)
 */
public class Statistics {

    private static final Trace LOGGER = TraceManager.getTrace(Statistics.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    private volatile EnvironmentalPerformanceInformation environmentalPerformanceInformation = new EnvironmentalPerformanceInformation();

    /**
     * Most current version of repository and audit performance information. Original (live) form of this information is accessible only
     * from the task thread itself. So we have to refresh this item periodically from the task thread.
     *
     * DO NOT modify the content of this structure from multiple threads. The task thread should only replace the whole structure,
     * while other threads should only read it.
     */
    private volatile RepositoryPerformanceInformationType repositoryPerformanceInformation;
    private volatile RepositoryPerformanceInformationType initialRepositoryPerformanceInformation;

    /**
     * Most current version of cache performance information. Original (live) form of this information is accessible only
     * from the task thread itself. So we have to refresh this item periodically from the task thread.
     *
     * DO NOT modify the content of this structure from multiple threads. The task thread should only replace the whole structure,
     * while other threads should only read it.
     */
    private volatile CachesPerformanceInformationType cachesPerformanceInformation;
    private volatile CachesPerformanceInformationType initialCachesPerformanceInformation;

    /**
     * Most current version of operations performance information. Original (live) form of this information is accessible only
     * from the task thread itself. So we have to refresh this item periodically from the task thread.
     *
     * DO NOT modify the content of this structure from multiple threads. The task thread should only replace the whole structure,
     * while other threads should only read it.
     */
    private volatile OperationsPerformanceInformationType operationsPerformanceInformation;
    private volatile OperationsPerformanceInformationType initialOperationsPerformanceInformation;

    private EnvironmentalPerformanceInformation getEnvironmentalPerformanceInformation() {
        return environmentalPerformanceInformation;
    }

    private volatile String cachingConfigurationDump;

    private EnvironmentalPerformanceInformationType getAggregateEnvironmentalPerformanceInformation(Collection<Statistics> children) {
        if (environmentalPerformanceInformation == null) {
            return null;
        }
        EnvironmentalPerformanceInformationType rv = new EnvironmentalPerformanceInformationType();
        EnvironmentalPerformanceInformation.addTo(rv, environmentalPerformanceInformation.getValueCopy());
        for (Statistics child : children) {
            EnvironmentalPerformanceInformation info = child.getEnvironmentalPerformanceInformation();
            if (info != null) {
                EnvironmentalPerformanceInformation.addTo(rv, info.getValueCopy());
            }
        }
        return rv;
    }

    private RepositoryPerformanceInformationType getAggregateRepositoryPerformanceInformation(Collection<Statistics> children) {
        if (repositoryPerformanceInformation == null) {
            return null;
        }
        RepositoryPerformanceInformationType rv = repositoryPerformanceInformation.clone();
        RepositoryPerformanceInformationUtil.addTo(rv, initialRepositoryPerformanceInformation);
        for (Statistics child : children) {
            RepositoryPerformanceInformationUtil.addTo(rv, child.getAggregateRepositoryPerformanceInformation(emptySet()));
        }
        return rv;
    }

    private CachesPerformanceInformationType getAggregateCachesPerformanceInformation(Collection<Statistics> children) {
        if (cachesPerformanceInformation == null) {
            return null;
        }
        CachesPerformanceInformationType rv = cachesPerformanceInformation.clone();
        CachePerformanceInformationUtil.addTo(rv, initialCachesPerformanceInformation);
        for (Statistics child : children) {
            CachePerformanceInformationUtil.addTo(rv, child.getAggregateCachesPerformanceInformation(emptySet()));
        }
        return rv;
    }

    private OperationsPerformanceInformationType getAggregateOperationsPerformanceInformation(Collection<Statistics> children) {
        if (operationsPerformanceInformation == null) {
            return null;
        }
        OperationsPerformanceInformationType rv = operationsPerformanceInformation.clone();
        OperationsPerformanceInformationUtil.addTo(rv, initialOperationsPerformanceInformation);
        for (Statistics child : children) {
            OperationsPerformanceInformationUtil.addTo(rv, child.getAggregateOperationsPerformanceInformation(emptySet()));
        }
        return rv;
    }

    /**
     * Gets aggregated operation statistics from this object and provided child objects.
     *
     * We assume that the children have compatible part numbers.
     */
    public OperationStatsType getAggregatedOperationStats(Collection<Statistics> children) {
        EnvironmentalPerformanceInformationType env = getAggregateEnvironmentalPerformanceInformation(children);
        RepositoryPerformanceInformationType repo = getAggregateRepositoryPerformanceInformation(children);
        CachesPerformanceInformationType caches = getAggregateCachesPerformanceInformation(children);
        OperationsPerformanceInformationType methods = getAggregateOperationsPerformanceInformation(children);
        // This is not fetched from children (present on coordinator task only).
        // It looks like that children are always LATs, and LATs do not have bucket management information.
        String cachingConfiguration = getAggregateCachingConfiguration(children);
        if (env == null && repo == null && caches == null && methods == null && cachingConfiguration == null) {
            return null;
        }
        OperationStatsType rv = new OperationStatsType();
        rv.setEnvironmentalPerformanceInformation(env);
        rv.setRepositoryPerformanceInformation(repo);
        rv.setCachesPerformanceInformation(caches);
        rv.setOperationsPerformanceInformation(methods);
        rv.setCachingConfiguration(cachingConfiguration);
        rv.setTimestamp(createXMLGregorianCalendar(new Date()));
        return rv;
    }

    private String getAggregateCachingConfiguration(Collection<Statistics> children) {
        if (children.isEmpty()) {
            return cachingConfigurationDump;
        } else {
            return cachingConfigurationDump + "\n\nFirst child:\n\n" + children.iterator().next().cachingConfigurationDump;
        }
    }

    public void recordState(String message) {
        LOGGER.trace("{}", message);
        PERFORMANCE_ADVISOR.debug("{}", message);
        environmentalPerformanceInformation.recordState(message);
    }

    public void recordProvisioningOperation(@NotNull ConnIdOperation operation) {
        environmentalPerformanceInformation.recordProvisioningOperation(operation);
    }

    public void recordNotificationOperation(String transportName, boolean success, long duration) {
        environmentalPerformanceInformation.recordNotificationOperation(transportName, success, duration);
    }

    public void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName,
            long duration) {
        environmentalPerformanceInformation.recordMappingOperation(objectOid, objectName, objectTypeName, mappingName, duration);
    }

    private void resetEnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
        environmentalPerformanceInformation = new EnvironmentalPerformanceInformation(value);
    }

    public void startCollectingStatistics(@NotNull RunningTask task,
            @NotNull StatisticsCollectionStrategy strategy, SqlPerformanceMonitorsCollection sqlPerformanceMonitors) {
        OperationStatsType initialOperationStats = getOrCreateInitialOperationStats(task, strategy.isStartFromZero());
        startOrRestartCollectingRegularStatistics(initialOperationStats);
        startOrRestartCollectingThreadLocalStatistics(initialOperationStats, sqlPerformanceMonitors);
    }

    public void restartCollectingStatisticsFromStoredValues(@NotNull RunningTask task,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitors) {
        OperationStatsType newInitialValues = getOrCreateInitialOperationStats(task, false);
        startOrRestartCollectingRegularStatistics(newInitialValues);
        startOrRestartCollectingThreadLocalStatistics(newInitialValues, sqlPerformanceMonitors);
    }

    public void restartCollectingStatisticsFromZero(SqlPerformanceMonitorsCollection sqlPerformanceMonitors) {
        OperationStatsType newInitialValues = new OperationStatsType();
        startOrRestartCollectingRegularStatistics(newInitialValues);
        startOrRestartCollectingThreadLocalStatistics(newInitialValues, sqlPerformanceMonitors);
    }

    @NotNull
    private OperationStatsType getOrCreateInitialOperationStats(@NotNull RunningTask task, boolean fromZero) {
        OperationStatsType initialValue =
                fromZero ? null : task.getStoredOperationStatsOrClone();
        return initialValue != null ? initialValue : new OperationStatsType();
    }

    private void startOrRestartCollectingRegularStatistics(OperationStatsType initialOperationStats) {
        resetEnvironmentalPerformanceInformation(initialOperationStats.getEnvironmentalPerformanceInformation());
    }

    private void startOrRestartCollectingThreadLocalStatistics(OperationStatsType initialOperationStats,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitors) {
        setInitialValuesForThreadLocalStatistics(initialOperationStats);
        startOrRestartCollectingThreadLocalStatistics(sqlPerformanceMonitors);
    }

    /**
     * Cheap operation so we can (and should) invoke it frequently.
     * But ALWAYS call it from the thread that executes the task in question; otherwise we get wrong data there.
     */
    public void refreshLowLevelStatistics(TaskManagerQuartzImpl taskManager) {
        refreshRepositoryAndAuditPerformanceInformation(taskManager);
        refreshCachePerformanceInformation();
        refreshMethodsPerformanceInformation();
        refreshCacheConfigurationInformation(taskManager.getCacheConfigurationManager());
    }

    private void refreshCacheConfigurationInformation(CacheConfigurationManager cacheConfigurationManager) {
        XMLGregorianCalendar now = createXMLGregorianCalendar(System.currentTimeMillis());
        String dump = "Caching configuration for thread " + Thread.currentThread().getName() + " on " + now + ":\n\n";
        String cfg = cacheConfigurationManager.dumpThreadLocalConfiguration(false);
        dump += Objects.requireNonNullElse(cfg, "(none defined)");
        cachingConfigurationDump = dump;
    }

    public void startOrRestartCollectingThreadLocalStatistics(SqlPerformanceMonitorsCollection sqlPerformanceMonitors) {
        if (sqlPerformanceMonitors != null) {
            sqlPerformanceMonitors.startThreadLocalPerformanceInformationCollection();
            repositoryPerformanceInformation = new RepositoryPerformanceInformationType();
        }

        CachePerformanceCollector.INSTANCE.startThreadLocalPerformanceInformationCollection();
        cachesPerformanceInformation = new CachesPerformanceInformationType();

        OperationsPerformanceMonitor.INSTANCE.startThreadLocalPerformanceInformationCollection();
        operationsPerformanceInformation = new OperationsPerformanceInformationType();
    }

    private void setInitialValuesForThreadLocalStatistics(OperationStatsType operationStats) {
        initialRepositoryPerformanceInformation = operationStats != null ? operationStats.getRepositoryPerformanceInformation() : null;
        initialCachesPerformanceInformation = operationStats != null ? operationStats.getCachesPerformanceInformation() : null;
        initialOperationsPerformanceInformation = operationStats != null ? operationStats.getOperationsPerformanceInformation() : null;
    }

    private void refreshRepositoryAndAuditPerformanceInformation(TaskManagerQuartzImpl taskManager) {
        SqlPerformanceMonitorsCollection monitors = taskManager.getSqlPerformanceMonitorsCollection();
        PerformanceInformation sqlPerformanceInformation = monitors != null ? monitors.getThreadLocalPerformanceInformation() : null;
        if (sqlPerformanceInformation != null) {
            repositoryPerformanceInformation = sqlPerformanceInformation.toRepositoryPerformanceInformationType();
        } else {
            repositoryPerformanceInformation = null; // probably we are not collecting these
        }
    }

    private void refreshMethodsPerformanceInformation() {
        OperationsPerformanceInformation performanceInformation = OperationsPerformanceMonitor.INSTANCE.getThreadLocalPerformanceInformation();
        if (performanceInformation != null) {
            operationsPerformanceInformation = OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(performanceInformation);
        } else {
            operationsPerformanceInformation = null;       // probably we are not collecting these
        }
    }

    private void refreshCachePerformanceInformation() {
        Map<String, CachePerformanceCollector.CacheData> performanceMap = CachePerformanceCollector.INSTANCE
                .getThreadLocalPerformanceMap();
        if (performanceMap != null) {
            cachesPerformanceInformation = CachePerformanceInformationUtil.toCachesPerformanceInformationType(performanceMap);
        } else {
            cachesPerformanceInformation = null;
        }
    }
}
