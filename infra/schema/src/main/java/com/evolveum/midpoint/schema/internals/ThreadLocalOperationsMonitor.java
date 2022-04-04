/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.internals;

import com.evolveum.midpoint.schema.result.OperationMonitoringConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MonitoredOperationStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MonitoredOperationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationMonitoringLevelType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MonitoredOperationsStatisticsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationMonitoringLevelType.FULL;

/**
 * Monitors operations for the current thread.
 *
 * Information on these operations are then stored e.g. to operation result and available via tracing.
 */
public class ThreadLocalOperationsMonitor {

    private static final Trace LOGGER = TraceManager.getTrace(ThreadLocalOperationsMonitor.class);

    /** Holds the monitor for the current thread. */
    @NotNull private static final ThreadLocal<ThreadLocalOperationsMonitor> THREAD_LOCAL =
            ThreadLocal.withInitial(ThreadLocalOperationsMonitor::new);

    /** What operations should be monitored in this thread, and to what level? */
    @NotNull private OperationMonitoringConfiguration configuration = OperationMonitoringConfiguration.empty();

    /**
     * What operations are in progress in this thread?
     *
     * This is used in cases when managing via recordStart-recordEnd method pair.
     */
    @NotNull private final Map<MonitoredOperationType, NestableOperationExecution> pendingOperations = new HashMap<>();

    /**
     * Operations that were executed in this thread.
     */
    @NotNull private final ExecutedOperations executedOperations = new ExecutedOperations();

    /** Records the start of an operation - if applicable. In this case the we are responsible for checking the applicability. */
    public static void recordStart(@NotNull MonitoredOperationType operation) {
        get().recordStartInternal(operation);
    }

    /** Records the end of an operation - if applicable. In this case the we are responsible for checking the applicability. */
    public static void recordEnd(@NotNull MonitoredOperationType operation) {
        get().recordEndInternal(operation);
    }

    public static @Nullable OperationExecution recordStartEmbedded(MonitoredOperationType operation) {
        return get().recordStartEmbeddedInternal(operation);
    }

    public static void recordEndEmbedded(OperationExecution execution) {
        if (execution != null) {
            execution.recordEndEmbeddedInternal();
        }
    }

    public static @NotNull ThreadLocalOperationsMonitor get() {
        return THREAD_LOCAL.get();
    }

    private void recordStartInternal(@NotNull MonitoredOperationType operation) {
        OperationMonitoringLevelType level = configuration.get(operation);
        if (level == null) {
            return;
        }
        NestableOperationExecution execution = pendingOperations.get(operation);
        if (execution != null) {
            execution.depth++;
        } else {
            NestableOperationExecution newExecution = new NestableOperationExecution();
            if (shouldWatchTime(level)) {
                newExecution.firstStart = System.nanoTime();
            }
            pendingOperations.put(operation, newExecution);
        }
    }

    private void recordEndInternal(@NotNull MonitoredOperationType operation) {
        OperationMonitoringLevelType level = configuration.get(operation);
        if (level == null) {
            return;
        }
        NestableOperationExecution execution = pendingOperations.get(operation);
        if (execution == null) {
            LOGGER.warn("No execution for {}, the operation execution will not be recorded", operation);
            return;
        }
        if (--execution.depth > 0) {
            return;
        }
        executedOperations.record(operation,
                determineDuration(execution.firstStart, level));
        pendingOperations.remove(operation);
    }

    private long determineDuration(long start, OperationMonitoringLevelType level) {
        if (shouldWatchTime(level)) {
            return System.nanoTime() - start;
        } else {
            return 0;
        }
    }

    private OperationExecution recordStartEmbeddedInternal(MonitoredOperationType operation) {
        OperationMonitoringLevelType level = configuration.get(operation);
        if (level == null) {
            return null;
        }
        OperationExecution newExecution = new OperationExecution(operation, level);
        if (shouldWatchTime(level)) {
            newExecution.start = System.nanoTime();
        }
        return newExecution;
    }

    public @NotNull OperationMonitoringConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(@NotNull OperationMonitoringConfiguration configuration) {
        this.configuration = configuration;
        checkPendingOperationsEmpty();
    }

    private void checkPendingOperationsEmpty() {
        if (!pendingOperations.isEmpty()) {
            LOGGER.warn("Pending monitored operation at configuration state change:\n{}", pendingOperations);
            pendingOperations.clear();
        }
    }

    public MonitoredOperationsStatisticsType getMonitoredOperationsRecord(@NotNull ExecutedOperations base) {
        ExecutedOperations difference = executedOperations.subtract(base);
        return difference.toBean();
    }

    public @NotNull ExecutedOperations getExecutedOperations() {
        return executedOperations;
    }

    private boolean shouldWatchTime(OperationMonitoringLevelType level) {
        return level == FULL;
    }

    private static class NestableOperationExecution {
        private long firstStart;
        private int depth = 1;

        @Override
        public String toString() {
            return "NestableOperationExecution{" +
                    "firstStart=" + firstStart +
                    ", depth=" + depth +
                    '}';
        }
    }

    private static class OperationStatistics implements Serializable {
        private int count;
        private long time;

        private OperationStatistics() {
        }

        private OperationStatistics(int count, long time) {
            this.count = count;
            this.time = time;
        }

        void add(long time) {
            this.count++;
            this.time += time;
        }

        public OperationStatistics copy() {
            return new OperationStatistics(count, time);
        }

        private void subtractInternal(OperationStatistics other) {
            count -= other.count;
            time -= other.time;
        }

        public boolean isEmpty() {
            return count == 0 && time == 0;
        }
    }

    public class OperationExecution {
        @NotNull private final MonitoredOperationType operation;
        @NotNull private final OperationMonitoringLevelType level;
        private long start;

        OperationExecution(@NotNull MonitoredOperationType operation, @NotNull OperationMonitoringLevelType level) {
            this.operation = operation;
            this.level = level;
        }

        private void recordEndEmbeddedInternal() {
            executedOperations.record(
                    operation,
                    determineDuration(start, level));
        }
    }

    public static class ExecutedOperations implements Serializable {

        private final Map<MonitoredOperationType, OperationStatistics> map = new HashMap<>();

        public void record(MonitoredOperationType operation, long time) {
            map.computeIfAbsent(operation, (ignored) -> new OperationStatistics())
                    .add(time);
        }

        public ExecutedOperations copy() {
            ExecutedOperations copy = new ExecutedOperations();
            map.forEach((operation, stats) ->
                    copy.map.put(operation, stats.copy()));
            return copy;
        }

        /** Returns a difference of this and specified base (a new object). */
        public @NotNull ExecutedOperations subtract(@NotNull ExecutedOperations base) {
            ExecutedOperations difference = copy();
            difference.subtractInternal(base);
            return difference;
        }

        /** Subtracts 'other' from 'this'. Modifies 'this'. */
        private void subtractInternal(@NotNull ExecutedOperations other) {
            for (Map.Entry<MonitoredOperationType, OperationStatistics> entry : other.map.entrySet()) {
                if (entry.getValue() == null || entry.getValue().count == 0) {
                    continue; // shouldn't occur
                }
                MonitoredOperationType operation = entry.getKey();
                OperationStatistics stats = map.get(operation);
                if (stats == null) {
                    LOGGER.warn("Couldn't subtract {} from {}, because {} could not be found", other, this, operation);
                    continue;
                }
                stats.subtractInternal(entry.getValue());
            }
            map.entrySet().removeIf(
                    e -> e.getValue().isEmpty());
        }

        public @Nullable MonitoredOperationsStatisticsType toBean() {
            if (map.isEmpty()) {
                return null;
            }
            MonitoredOperationsStatisticsType bean = new MonitoredOperationsStatisticsType();
            map.entrySet().forEach(e ->
                    bean.getOperation().add(createOperationBean(e)));
            return bean;
        }

        private MonitoredOperationStatisticsType createOperationBean(
                Map.Entry<MonitoredOperationType, OperationStatistics> entry) {
            MonitoredOperationStatisticsType bean = new MonitoredOperationStatisticsType();
            bean.setOperation(entry.getKey());
            bean.setCount(entry.getValue().count);
            if (entry.getValue().time > 0) {
                bean.setNanos(entry.getValue().time);
            }
            return bean;
        }
    }
}
