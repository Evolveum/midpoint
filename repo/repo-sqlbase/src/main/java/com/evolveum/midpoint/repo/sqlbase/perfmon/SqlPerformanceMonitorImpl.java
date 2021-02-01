/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.perfmon;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.evolveum.midpoint.repo.api.perf.OperationRecord;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitorImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryStatisticsClassificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryStatisticsCollectionStyleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryStatisticsReportingConfigurationType;

public class SqlPerformanceMonitorImpl implements PerformanceMonitor {

    private static final Trace LOGGER = TraceManager.getTrace(SqlPerformanceMonitorImpl.class);

    public static final int LEVEL_NONE = 0;
    public static final int LEVEL_GLOBAL_STATISTICS = 2;
    public static final int LEVEL_LOCAL_STATISTICS = 4;
    public static final int LEVEL_DETAILS = 10;

    private final int initialLevel;
    private final String statisticsFile;

    private int level;
    private boolean perObjectType = false;

    private final AtomicLong currentHandle = new AtomicLong();

    /**
     * Operations that were started but not finished yet. Indexed by handle.
     * It is used at levels > NONE (0).
     */
    private final Map<Long, OperationRecord> outstandingOperations = new ConcurrentHashMap<>();

    /**
     * Operations that were completed. Used for archival and detailed analysis purpose (presumably only for tests).
     * It is used at levels >= DETAILS (10).
     */
    private final List<OperationRecord> finishedOperations = Collections.synchronizedList(new ArrayList<>());

    /**
     * Aggregated operations performance information local to the thread.
     * It is used at levels >= STATISTICS (1).
     */
    private final ThreadLocal<PerformanceInformationImpl> threadLocalPerformanceInformation = new ThreadLocal<>();

    /**
     * Aggregated operations performance information common to all threads.
     * It is used at levels >= STATISTICS (1).
     */
    private final PerformanceInformationImpl globalPerformanceInformation = new PerformanceInformationImpl();

    public SqlPerformanceMonitorImpl(int initialLevel, String statisticsFile) {
        this.initialLevel = initialLevel;
        this.statisticsFile = statisticsFile;
        level = initialLevel;
        outstandingOperations.clear();
        finishedOperations.clear();
        globalPerformanceInformation.clear();
        // at least for this thread; other threads have to do their own homework
        threadLocalPerformanceInformation.remove();

        // fixme put to better place
        OperationsPerformanceMonitorImpl.INSTANCE.initialize();
        LOGGER.info("SQL Performance Monitor initialized (level = {})", level);
    }

    @Override
    public void clearGlobalPerformanceInformation() {
        globalPerformanceInformation.clear();
    }

    @Override
    public PerformanceInformationImpl getGlobalPerformanceInformation() {
        return globalPerformanceInformation;
    }

    @Override
    public void startThreadLocalPerformanceInformationCollection() {
        threadLocalPerformanceInformation.set(new PerformanceInformationImpl());
    }

    @Override
    public PerformanceInformationImpl getThreadLocalPerformanceInformation() {
        return threadLocalPerformanceInformation.get();
    }

    @Override
    public void stopThreadLocalPerformanceInformationCollection() {
        threadLocalPerformanceInformation.remove();
    }

    public void shutdown() {
        LOGGER.info("SQL Performance Monitor shutting down.");
        synchronized (finishedOperations) {
            if (!finishedOperations.isEmpty()) {
                LOGGER.info("Statistics:\n{}", OutputFormatter.getFormattedStatistics(finishedOperations, outstandingOperations));
                if (statisticsFile != null) {
                    OutputFormatter.writeStatisticsToFile(statisticsFile, finishedOperations, outstandingOperations);
                }
            }
        }
        if (level >= LEVEL_GLOBAL_STATISTICS) {
            LOGGER.info("Global performance information:\n{}", globalPerformanceInformation.debugDump());
        }
        OperationsPerformanceMonitorImpl.INSTANCE.shutdown();
    }

    public long registerOperationStart(String kind, Class<?> objectType) {
        if (level > LEVEL_NONE) {
            long handle = currentHandle.getAndIncrement();
            if (outstandingOperations.containsKey(handle)) {
                OperationRecord unfinishedOperation = outstandingOperations.get(handle);
                LOGGER.error("Unfinished operation -- should never occur: {}", unfinishedOperation);
                throw new IllegalStateException("Unfinished operation -- should never occur: " + unfinishedOperation);
            }
            outstandingOperations.put(handle, new OperationRecord(kind, objectType, handle));
            return handle;
        } else {
            return -1L;
        }
    }

    public void registerOperationFinish(long opHandle, int attempt) {
        if (level > LEVEL_NONE) {
            OperationRecord operation = outstandingOperations.get(opHandle);
            if (isOperationHandleOk(operation, opHandle)) {
                registerOperationFinishInternal(operation, attempt);
            }
        }
    }

    private boolean isOperationHandleOk(OperationRecord operation, long opHandle) {
        if (operation == null) {
            LOGGER.warn("Tried to record attempt/finish event for unregistered operation: handle = {}, ignoring the request.", opHandle);
            return false;
        } else if (operation.getHandle() != opHandle) {
            throw new IllegalStateException(
                    "Tried to record attempt/finish event with unexpected operation handle: handle = " + opHandle
                            + ", stored outstanding operation for this thread = " + operation);
        } else {
            return true;
        }
    }

    private void registerOperationFinishInternal(OperationRecord operation, int attempt) {
        operation.setTotalTime(System.currentTimeMillis() - operation.getStartTime());
        operation.setAttempts(attempt);
        outstandingOperations.remove(operation.getHandle());
        if (level >= LEVEL_DETAILS) {
            finishedOperations.add(operation);
        }
        if (level >= LEVEL_GLOBAL_STATISTICS) {
            globalPerformanceInformation.register(operation, perObjectType);
        }
        if (level >= LEVEL_LOCAL_STATISTICS) {
            PerformanceInformationImpl localInformation = threadLocalPerformanceInformation.get();
            if (localInformation != null) {
                localInformation.register(operation, perObjectType);
            }
        }
    }

    public void registerOperationNewAttempt(long opHandle, int attempt) {
        if (level > LEVEL_NONE) {
            OperationRecord operation = outstandingOperations.get(opHandle);
            if (isOperationHandleOk(operation, opHandle)) {
                operation.setWastedTime(System.currentTimeMillis() - operation.getStartTime());
                operation.setAttempts(attempt);
            }
        }
    }

    // to be used in tests
    @SuppressWarnings("unused")     // maybe in future
    public List<OperationRecord> getFinishedOperations(String kind) {
        List<OperationRecord> matching = new ArrayList<>();
        synchronized (finishedOperations) {
            for (OperationRecord record : finishedOperations) {
                if (Objects.equals(kind, record.getKind())) {
                    matching.add(record);
                }
            }
        }
        return matching;
    }

    // to be used in tests
    public int getFinishedOperationsCount(String kind) {
        int rv = 0;
        synchronized (finishedOperations) {
            for (OperationRecord record : finishedOperations) {
                if (Objects.equals(kind, record.getKind())) {
                    rv++;
                }
            }
        }
        return rv;
    }

    public void setConfiguration(RepositoryStatisticsReportingConfigurationType configuration) {
        int newLevel;
        RepositoryStatisticsClassificationType classification = configuration != null ? configuration.getClassification() : null;
        RepositoryStatisticsCollectionStyleType collection = configuration != null ? configuration.getCollection() : null;
        if (collection == null) {
            newLevel = initialLevel;
        } else {
            switch (collection) {
                case NONE:
                    newLevel = LEVEL_NONE;
                    break;
                case GLOBALLY:
                    newLevel = LEVEL_GLOBAL_STATISTICS;
                    break;
                case GLOBALLY_AND_LOCALLY:
                    newLevel = LEVEL_LOCAL_STATISTICS;
                    break;
                case OPERATIONS:
                    newLevel = LEVEL_DETAILS;
                    break;
                default:
                    throw new IllegalArgumentException("collection: " + collection);
            }
        }
        if (newLevel != level) {
            LOGGER.info("Changing collection level from {} to {} (configured as {})", level, newLevel, collection);
            level = newLevel;
        }
        perObjectType = classification == RepositoryStatisticsClassificationType.PER_OPERATION_AND_OBJECT_TYPE;
    }
}
