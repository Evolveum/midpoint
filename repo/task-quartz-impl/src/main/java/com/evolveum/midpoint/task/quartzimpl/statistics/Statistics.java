/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.statistics;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceInformation;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
public class Statistics implements WorkBucketStatisticsCollector {

    private static final Trace LOGGER = TraceManager.getTrace(Statistics.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    @NotNull private final PrismContext prismContext;

    public Statistics(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    private EnvironmentalPerformanceInformation environmentalPerformanceInformation = new EnvironmentalPerformanceInformation();
    private SynchronizationInformation synchronizationInformation; // has to be explicitly enabled (by setting non-null value)
    private IterativeTaskInformation iterativeTaskInformation = new IterativeTaskInformation(PrismContext.get()); // just to have any value
    private ActionsExecutedInformation actionsExecutedInformation; // has to be explicitly enabled (by setting non-null value)

    private StructuredTaskProgress structuredProgress = null; // has to be explicitly enabled (by setting non-null value)

    /**
     * This data structure is synchronized explicitly. Because it is updated infrequently, it should be sufficient.
     */
    private WorkBucketManagementPerformanceInformationType workBucketManagementPerformanceInformation;

    private static final Object BUCKET_INFORMATION_LOCK = new Object();

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

    private SynchronizationInformation getSynchronizationInformation() {
        return synchronizationInformation;
    }

    private IterativeTaskInformation getIterativeTaskInformation() {
        return iterativeTaskInformation;
    }

    private ActionsExecutedInformation getActionsExecutedInformation() {
        return actionsExecutedInformation;
    }

    private volatile String cachingConfigurationDump;

    @NotNull
    @Deprecated
    public List<String> getLastFailures() {
        return iterativeTaskInformation.getLastFailures();
    }

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

    /** We assume that the children have compatible part numbers. */
    private IterativeTaskInformationType getAggregateIterativeTaskInformation(Collection<Statistics> children) {
        IterativeTaskInformationType sum = iterativeTaskInformation.getValueCopy();
        for (Statistics child : children) {
            IterativeTaskInformation info = child.getIterativeTaskInformation();
            if (info != null) {
                IterativeTaskInformation.addTo(sum, info.getValueCopy());
            }
        }
        return sum;
    }

    private SynchronizationInformationType getAggregateSynchronizationInformation(Collection<Statistics> children) {
        if (synchronizationInformation == null) {
            return null;
        }
        SynchronizationInformationType rv = new SynchronizationInformationType();
        SynchronizationInformation.addTo(rv, synchronizationInformation.getValueCopy());
        for (Statistics child : children) {
            SynchronizationInformation info = child.getSynchronizationInformation();
            if (info != null) {
                SynchronizationInformation.addTo(rv, info.getValueCopy());
            }
        }
        return rv;
    }

    private ActionsExecutedInformationType getAggregateActionsExecutedInformation(Collection<Statistics> children) {
        if (actionsExecutedInformation == null) {
            return null;
        }
        ActionsExecutedInformationType rv = new ActionsExecutedInformationType();
        ActionsExecutedInformation.addTo(rv, actionsExecutedInformation.getAggregatedValue());
        for (Statistics child : children) {
            ActionsExecutedInformation info = child.getActionsExecutedInformation();
            if (info != null) {
                ActionsExecutedInformation.addTo(rv, info.getAggregatedValue());
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

    private WorkBucketManagementPerformanceInformationType getWorkBucketManagementPerformanceInformation() {
        synchronized (BUCKET_INFORMATION_LOCK) {
            return workBucketManagementPerformanceInformation != null ? workBucketManagementPerformanceInformation.clone() : null;
        }
    }

    /**
     * Gets aggregated operation statistics from this object and provided child objects.
     *
     * We assume that the children have compatible part numbers.
     */
    public OperationStatsType getAggregatedOperationStats(Collection<Statistics> children) {
        EnvironmentalPerformanceInformationType env = getAggregateEnvironmentalPerformanceInformation(children);
        IterativeTaskInformationType itit = getAggregateIterativeTaskInformation(children);
        SynchronizationInformationType sit = getAggregateSynchronizationInformation(children);
        ActionsExecutedInformationType aeit = getAggregateActionsExecutedInformation(children);
        RepositoryPerformanceInformationType repo = getAggregateRepositoryPerformanceInformation(children);
        CachesPerformanceInformationType caches = getAggregateCachesPerformanceInformation(children);
        OperationsPerformanceInformationType methods = getAggregateOperationsPerformanceInformation(children);
        WorkBucketManagementPerformanceInformationType buckets = getWorkBucketManagementPerformanceInformation();   // this is not fetched from children (present on coordinator task only)
        String cachingConfiguration = getAggregateCachingConfiguration(children);
        if (env == null && itit == null && sit == null && aeit == null && repo == null && caches == null && methods == null && buckets == null && cachingConfiguration == null) {
            return null;
        }
        OperationStatsType rv = new OperationStatsType();
        rv.setEnvironmentalPerformanceInformation(env);
        rv.setIterativeTaskInformation(itit);
        rv.setSynchronizationInformation(sit);
        rv.setActionsExecutedInformation(aeit);
        rv.setRepositoryPerformanceInformation(repo);
        rv.setCachesPerformanceInformation(caches);
        rv.setOperationsPerformanceInformation(methods);
        rv.setCachingConfiguration(cachingConfiguration);
        rv.setWorkBucketManagementPerformanceInformation(buckets);
        rv.setTimestamp(createXMLGregorianCalendar(new Date()));
        return rv;
    }

    public StructuredTaskProgressType getStructuredTaskProgress() {
        return structuredProgress != null ? structuredProgress.getValueCopy() : null;
    }

    private String getAggregateCachingConfiguration(Collection<Statistics> children) {
        if (children.isEmpty()) {
            return cachingConfigurationDump;
        } else {
            return cachingConfigurationDump + "\n\nFirst child:\n\n" + children.iterator().next().cachingConfigurationDump;
        }
    }

    public void recordState(String message) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}", message);
        }
        if (PERFORMANCE_ADVISOR.isDebugEnabled()) {
            PERFORMANCE_ADVISOR.debug("{}", message);
        }
        environmentalPerformanceInformation.recordState(message);
    }

    public void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName,
            ProvisioningOperation operation, boolean success, int count, long duration) {
        environmentalPerformanceInformation
                .recordProvisioningOperation(resourceOid, resourceName, objectClassName, operation, success, count, duration);
    }

    public void recordNotificationOperation(String transportName, boolean success, long duration) {
        environmentalPerformanceInformation.recordNotificationOperation(transportName, success, duration);
    }

    public void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName,
            long duration) {
        environmentalPerformanceInformation.recordMappingOperation(objectOid, objectName, objectTypeName, mappingName, duration);
    }

    public synchronized void onSyncItemProcessingStart(@NotNull String processingIdentifier,
            @Nullable SynchronizationSituationType beforeOperation) {
        if (synchronizationInformation != null) {
            synchronizationInformation.onItemProcessingStart(processingIdentifier, beforeOperation);
        }
    }

    public synchronized void onSynchronizationStart(@Nullable String processingIdentifier,
            @Nullable String shadowOid, @Nullable SynchronizationSituationType situation) {
        if (synchronizationInformation != null) {
            synchronizationInformation.onSynchronizationStart(processingIdentifier, shadowOid, situation);
        }
    }

    public synchronized void onSynchronizationExclusion(@Nullable String processingIdentifier,
            @NotNull SynchronizationExclusionReasonType exclusionReason) {
        if (synchronizationInformation != null) {
            synchronizationInformation.onSynchronizationExclusion(processingIdentifier, exclusionReason);
        }
    }

    public synchronized void onSynchronizationSituationChange(@Nullable String processingIdentifier,
            @Nullable String shadowOid, @Nullable SynchronizationSituationType situation) {
        if (synchronizationInformation != null) {
            synchronizationInformation.onSynchronizationSituationChange(processingIdentifier, shadowOid, situation);
        }
    }

    public synchronized void onSyncItemProcessingEnd(@NotNull String processingIdentifier,
            @NotNull QualifiedItemProcessingOutcomeType outcome) {
        if (synchronizationInformation != null) {
            synchronizationInformation.onSyncItemProcessingEnd(processingIdentifier, outcome);
        }
    }

    @NotNull
    public synchronized IterativeTaskInformation.Operation recordIterativeOperationStart(IterativeOperationStartInfo operation) {
        return iterativeTaskInformation.recordOperationStart(operation);
    }

    public void setStructuredProgressPartInformation(String partUri, Integer partNumber, Integer expectedParts) {
        if (structuredProgress != null) {
            structuredProgress.setPartInformation(partUri, partNumber, expectedParts);
        }
    }

    public void incrementStructuredProgress(String partUri, QualifiedItemProcessingOutcomeType outcome) {
        if (structuredProgress != null) {
            structuredProgress.increment(partUri, outcome);
        }
    }

    public void changeStructuredProgressOnWorkBucketCompletion() {
        if (structuredProgress != null) {
            structuredProgress.changeOnWorkBucketCompletion();
        }
    }

    public void markStructuredProgressAsComplete() {
        if (structuredProgress != null) {
            structuredProgress.markAsComplete();
        }
    }

    public void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid,
            ChangeType changeType, String channel, Throwable exception) {
        if (actionsExecutedInformation != null) {
            actionsExecutedInformation
                    .recordObjectActionExecuted(objectName, objectDisplayName, objectType, objectOid, changeType, channel,
                            exception);
        }
    }

    public void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, String channel, Throwable exception) {
        recordObjectActionExecuted(object, null, null, changeType, channel, exception);
    }

    public <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass,
            String defaultOid, ChangeType changeType, String channel, Throwable exception) {
        if (actionsExecutedInformation != null) {
            String name, displayName, oid;
            PrismObjectDefinition<?> definition;
            Class<T> clazz;
            if (object != null) {
                name = PolyString.getOrig(object.getName());
                displayName = TaskOperationStatsUtil.getDisplayName(object);
                definition = object.getDefinition();
                clazz = object.getCompileTimeClass();
                oid = object.getOid();
                if (oid == null) {        // in case of ADD operation
                    oid = defaultOid;
                }
            } else {
                name = null;
                displayName = null;
                definition = null;
                clazz = objectTypeClass;
                oid = defaultOid;
            }
            if (definition == null && clazz != null) {
                definition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
            }
            QName typeQName;
            if (definition != null) {
                typeQName = definition.getTypeName();
            } else {
                typeQName = ObjectType.COMPLEX_TYPE;
            }
            actionsExecutedInformation
                    .recordObjectActionExecuted(name, displayName, typeQName, oid, changeType, channel, exception);
        }
    }

    public void markObjectActionExecutedBoundary() {
        if (actionsExecutedInformation != null) {
            actionsExecutedInformation.markObjectActionExecutedBoundary();
        }
    }

    private void resetEnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
        environmentalPerformanceInformation = new EnvironmentalPerformanceInformation(value);
    }

    public void resetSynchronizationInformation(SynchronizationInformationType value) {
        synchronizationInformation = new SynchronizationInformation(value, prismContext);
    }

    public void resetIterativeTaskInformation(IterativeTaskInformationType value) {
        iterativeTaskInformation = new IterativeTaskInformation(value, prismContext);
    }

    public void resetActionsExecutedInformation(ActionsExecutedInformationType value) {
        actionsExecutedInformation = new ActionsExecutedInformation(value);
    }

    private void resetWorkBucketManagementPerformanceInformation(WorkBucketManagementPerformanceInformationType value) {
        synchronized (BUCKET_INFORMATION_LOCK) {
            workBucketManagementPerformanceInformation = value != null ? value.clone() : new WorkBucketManagementPerformanceInformationType();
        }
    }

    public void startCollectingStatistics(@NotNull RunningTask task,
            @NotNull StatisticsCollectionStrategy strategy, SqlPerformanceMonitorsCollection sqlPerformanceMonitors) {

        OperationStatsType initialOperationStats = getOrCreateInitialOperationStats(task);
        resetEnvironmentalPerformanceInformation(initialOperationStats.getEnvironmentalPerformanceInformation());
        resetIterativeTaskInformation(initialOperationStats.getIterativeTaskInformation());
        if (strategy.isMaintainSynchronizationStatistics()) {
            resetSynchronizationInformation(initialOperationStats.getSynchronizationInformation());
        } else {
            synchronizationInformation = null;
        }
        if (strategy.isMaintainActionsExecutedStatistics()) {
            resetActionsExecutedInformation(initialOperationStats.getActionsExecutedInformation());
        } else {
            actionsExecutedInformation = null;
        }
        resetWorkBucketManagementPerformanceInformation(initialOperationStats.getWorkBucketManagementPerformanceInformation());
        setInitialValuesForLowLevelStatistics(initialOperationStats);

        if (strategy.isMaintainStructuredProgress()) {
            structuredProgress = new StructuredTaskProgress(task.getStructuredProgressOrClone(), PrismContext.get());
        } else {
            structuredProgress = null;
        }

        startCollectingThreadLocalStatistics(sqlPerformanceMonitors);
    }

    @NotNull
    private OperationStatsType getOrCreateInitialOperationStats(@NotNull RunningTask task) {
        OperationStatsType stored = task.getStoredOperationStatsOrClone();
        return stored != null ? stored : new OperationStatsType(PrismContext.get());
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

    public void startCollectingThreadLocalStatistics(SqlPerformanceMonitorsCollection sqlPerformanceMonitors) {
        if (sqlPerformanceMonitors != null) {
            sqlPerformanceMonitors.startThreadLocalPerformanceInformationCollection();
        }
        CachePerformanceCollector.INSTANCE.startThreadLocalPerformanceInformationCollection();
        OperationsPerformanceMonitor.INSTANCE.startThreadLocalPerformanceInformationCollection();
    }

    private void setInitialValuesForLowLevelStatistics(OperationStatsType operationStats) {
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

    @SuppressWarnings("Duplicates")
    @Override
    public void register(String situation, long totalTime, int conflictCount, long conflictWastedTime, int bucketWaitCount, long bucketWaitTime, int bucketsReclaimed) {
        synchronized (BUCKET_INFORMATION_LOCK) {
            WorkBucketManagementOperationPerformanceInformationType operation = null;
            for (WorkBucketManagementOperationPerformanceInformationType op : workBucketManagementPerformanceInformation.getOperation()) {
                if (op.getName().equals(situation)) {
                    operation = op;
                    break;
                }
            }
            if (operation == null) {
                operation = new WorkBucketManagementOperationPerformanceInformationType();
                operation.setName(situation);
                workBucketManagementPerformanceInformation.getOperation().add(operation);
            }
            operation.setCount(or0(operation.getCount()) + 1);
            addTime(operation, totalTime, WorkBucketManagementOperationPerformanceInformationType::getTotalTime,
                    WorkBucketManagementOperationPerformanceInformationType::getMinTime,
                    WorkBucketManagementOperationPerformanceInformationType::getMaxTime,
                    WorkBucketManagementOperationPerformanceInformationType::setTotalTime,
                    WorkBucketManagementOperationPerformanceInformationType::setMinTime,
                    WorkBucketManagementOperationPerformanceInformationType::setMaxTime);
            if (conflictCount > 0 || conflictWastedTime > 0) {
                operation.setConflictCount(or0(operation.getConflictCount()) + conflictCount);
                addTime(operation, conflictWastedTime,
                        WorkBucketManagementOperationPerformanceInformationType::getTotalWastedTime,
                        WorkBucketManagementOperationPerformanceInformationType::getMinWastedTime,
                        WorkBucketManagementOperationPerformanceInformationType::getMaxWastedTime,
                        WorkBucketManagementOperationPerformanceInformationType::setTotalWastedTime,
                        WorkBucketManagementOperationPerformanceInformationType::setMinWastedTime,
                        WorkBucketManagementOperationPerformanceInformationType::setMaxWastedTime);
            }
            if (bucketWaitCount > 0 || bucketsReclaimed > 0 || bucketWaitTime > 0) {
                operation.setBucketWaitCount(or0(operation.getBucketWaitCount()) + bucketWaitCount);
                operation.setBucketsReclaimed(or0(operation.getBucketsReclaimed()) + bucketsReclaimed);
                addTime(operation, bucketWaitTime, WorkBucketManagementOperationPerformanceInformationType::getTotalWaitTime,
                        WorkBucketManagementOperationPerformanceInformationType::getMinWaitTime,
                        WorkBucketManagementOperationPerformanceInformationType::getMaxWaitTime,
                        WorkBucketManagementOperationPerformanceInformationType::setTotalWaitTime,
                        WorkBucketManagementOperationPerformanceInformationType::setMinWaitTime,
                        WorkBucketManagementOperationPerformanceInformationType::setMaxWaitTime);
            }
        }
    }

    private void addTime(WorkBucketManagementOperationPerformanceInformationType operation,
            long time, Function<WorkBucketManagementOperationPerformanceInformationType, Long> getterTotal,
            Function<WorkBucketManagementOperationPerformanceInformationType, Long> getterMin,
            Function<WorkBucketManagementOperationPerformanceInformationType, Long> getterMax,
            BiConsumer<WorkBucketManagementOperationPerformanceInformationType, Long> setterTotal,
            BiConsumer<WorkBucketManagementOperationPerformanceInformationType, Long> setterMin,
            BiConsumer<WorkBucketManagementOperationPerformanceInformationType, Long>  setterMax) {
        setterTotal.accept(operation, or0(getterTotal.apply(operation)) + time);
        Long min = getterMin.apply(operation);
        if (min == null || time < min) {
            setterMin.accept(operation, time);
        }
        Long max = getterMax.apply(operation);
        if (max == null || time > max) {
            setterMax.accept(operation, time);
        }
    }

    private int or0(Integer n) {
        return n != null ? n : 0;
    }

    private long or0(Long n) {
        return n != null ? n : 0;
    }
}
