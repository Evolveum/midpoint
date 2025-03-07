/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.internals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismMonitor;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.MonitoredOperationType.*;

/**
 * Simple monitoring object. It records the count of expensive operations
 * in the system. It is used in the tests to make sure such operations are not
 * executed more frequently than expected. It may also have some run-time value.
 *
 * @author Radovan Semancik
 *
 */
public class InternalMonitor implements PrismMonitor, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(InternalMonitor.class);

    private static final String CLONE_START_TIMESTAMP_KEY = InternalMonitor.class.getName()+".cloneStartTimestamp";

    private static final Map<InternalCounters, AtomicLong> COUNTER_MAP = new HashMap<>();
    private static final Map<InternalOperationClasses,Boolean> TRACE_CLASS_MAP = new HashMap<>();
    private static final Map<InternalCounters,Boolean> TRACE_COUNTER_MAP = new HashMap<>();

    private static CachingStatistics resourceCacheStats = new CachingStatistics();
    private static CachingStatistics connectorCacheStats = new CachingStatistics();

    private static boolean cloneTimingEnabled = false;
    private static long prismObjectCloneDurationNanos = 0;

    private static InternalInspector inspector;

    public static long getCount(InternalCounters counter) {
        var count = COUNTER_MAP.get(counter);
        return count != null ? count.get() : 0;
    }

    public static void recordCount(InternalCounters counter) {
        long count = recordCountInternal(counter);
        if (isTrace(counter)) {
            traceOperation(counter, counter.getOperationClass(), count);
        }
    }

    private static synchronized long recordCountInternal(InternalCounters counter) {
        return COUNTER_MAP
                .computeIfAbsent(counter, k -> new AtomicLong())
                .incrementAndGet();
    }

    public static boolean isTrace(InternalOperationClasses operationClass) {
        return Boolean.TRUE.equals(TRACE_CLASS_MAP.get(operationClass));
    }

    private static boolean isTrace(InternalCounters counter) {
        Boolean counterTrace = TRACE_COUNTER_MAP.get(counter);
        if (counterTrace != null) {
            return counterTrace;
        }
        InternalOperationClasses operationClass = counter.getOperationClass();
        if (operationClass == null) {
            return false;
        }
        return isTrace(operationClass);
    }

    public static void setTrace(InternalOperationClasses operationClass, boolean val) {
        TRACE_CLASS_MAP.put(operationClass, val);
    }

    public static void setTrace(InternalCounters counter, boolean val) {
        TRACE_COUNTER_MAP.put(counter, val);
    }

    private static void traceOperation(InternalCounters counter, InternalOperationClasses operationClass, long count) {
        traceOperation(counter.getKey() + "["+ operationClass.getKey() +"]", null, count, false);
    }

    private static void traceOperation(String opName, Supplier<String> paramsSupplier, long count, boolean traceAndDebug) {
        String params = paramsSupplier != null ? paramsSupplier.get() : "";
        // TODO consider if the outputting params for operations other than clone() is adequate
        LOGGER.info("MONITOR {}({}) ({})", opName, params, count);
        if (LOGGER.isDebugEnabled()) {
            StackTraceElement[] fullStack = Thread.currentThread().getStackTrace();
            String immediateClass = null;
            String immediateMethod = null;
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement stackElement: fullStack) {
                if (stackElement.getClassName().equals(InternalMonitor.class.getName()) ||
                        stackElement.getClassName().equals(Thread.class.getName())) {
                    // skip our own calls
                    continue;
                }
                if (immediateClass == null) {
                    immediateClass = stackElement.getClassName();
                    immediateMethod = stackElement.getMethodName();
                }
                sb.append(stackElement);
                sb.append("\n");
            }
            if (traceAndDebug) {
                LOGGER.debug("MONITOR {}({}) ({}): {} {}", opName, params, count, immediateClass, immediateMethod);
            }
            LOGGER.trace("MONITOR {}({}) ({}):\n{}", opName, params, count, sb);
        }
    }


    public static CachingStatistics getResourceCacheStats() {
        return resourceCacheStats;
    }

    public static CachingStatistics getConnectorCacheStats() {
        return connectorCacheStats;
    }

    public static void recordConnectorOperation(String name) {
        long count = recordCountInternal(InternalCounters.CONNECTOR_OPERATION_COUNT);
        if (isTrace(InternalCounters.CONNECTOR_OPERATION_COUNT)) {
            traceOperation("connectorOperation", () -> name, count, true);
        }
    }

    public static void recordConnectorModification(String name) {
        long count = recordCountInternal(InternalCounters.CONNECTOR_MODIFICATION_COUNT);
        if (isTrace(InternalCounters.CONNECTOR_MODIFICATION_COUNT)) {
            traceOperation("connectorModification", () -> name, count, true);
        }
    }

    public static <O extends ObjectType> void recordRepositoryRead(Class<O> type, String oid) {
        long count = recordCountInternal(InternalCounters.REPOSITORY_READ_COUNT);
        if (isTrace(InternalCounters.REPOSITORY_READ_COUNT)) {
            traceOperation("repositoryRead", () -> type.getSimpleName() + ", " + oid , count, false);
        }
        if (inspector != null) {
            inspector.inspectRepositoryRead(type, oid);
        }
    }

    public synchronized <O extends Objectable> void recordPrismObjectCompareCount(PrismObject<O> thisObject, Object thatObject) {
        recordCount(InternalCounters.PRISM_OBJECT_COMPARE_COUNT);
    }

    public static boolean isCloneTimingEnabled() {
        return cloneTimingEnabled;
    }

    public static void setCloneTimingEnabled(boolean cloneTimingEnabled) {
        InternalMonitor.cloneTimingEnabled = cloneTimingEnabled;
    }

    public static long getPrismObjectCloneDurationMillis() {
        return prismObjectCloneDurationNanos;
    }

    public static void setPrismObjectCloneDurationMillis(long prismObjectCloneDurationNanos) {
        InternalMonitor.prismObjectCloneDurationNanos = prismObjectCloneDurationNanos;
    }

    @Override
    public <O extends Objectable> void beforeObjectClone(@NotNull PrismObject<O> orig) {
        ThreadLocalOperationsMonitor.recordStart(CLONE);
        if (!cloneTimingEnabled) {
            return;
        }
        LOGGER.trace("MONITOR prism object clone start: {}", orig);
        if (!orig.isImmutable()) {
            orig.setUserData(CLONE_START_TIMESTAMP_KEY, System.nanoTime());
        }
    }

    @Override
    public synchronized <O extends Objectable> void afterObjectClone(@NotNull PrismObject<O> orig,
            @Nullable PrismObject<O> clone) {
        ThreadLocalOperationsMonitor.recordEnd(CLONE);
        long count = recordCountInternal(InternalCounters.PRISM_OBJECT_CLONE_COUNT);
        if (cloneTimingEnabled) {
            Object cloneStartObject = orig.getUserData(CLONE_START_TIMESTAMP_KEY);
            if (cloneStartObject instanceof Long aLong) {
                long cloneDurationNanos = System.nanoTime() - aLong;
                prismObjectCloneDurationNanos += cloneDurationNanos;
                LOGGER.debug("MONITOR prism object clone end: {} (duration {} ns)", orig, cloneDurationNanos);
            } else {
                LOGGER.debug("MONITOR prism object clone end: {}", orig);
            }
        }
        if (isTrace(InternalCounters.PRISM_OBJECT_CLONE_COUNT)) {
            traceOperation("prism object clone", orig::toString, count, false); // Consider setting traceAndDebug as necessary
        }
    }

    @Override
    public void beforeObjectSerialization(@NotNull PrismObject<?> item) {
        ThreadLocalOperationsMonitor.recordStart(OBJECT_SERIALIZATION);
    }

    @Override
    public void afterObjectSerialization(@NotNull PrismObject<?> item) {
        ThreadLocalOperationsMonitor.recordEnd(OBJECT_SERIALIZATION);
    }

    @Override
    public void beforeObjectParsing() {
        ThreadLocalOperationsMonitor.recordStart(OBJECT_PARSING);
    }

    @Override
    public void afterObjectParsing(@Nullable PrismObject<?> object) {
        ThreadLocalOperationsMonitor.recordEnd(OBJECT_PARSING);
    }

    public static <F extends AssignmentHolderType> void recordRoleEvaluation(F target, boolean fullEvaluation) {
        long count = recordCountInternal(InternalCounters.ROLE_EVALUATION_COUNT);
        if (isTrace(InternalCounters.ROLE_EVALUATION_COUNT)) {
            traceOperation("roleEvaluation", target::toString, count, true);
        }
        if (inspector != null) {
            inspector.inspectRoleEvaluation(target, fullEvaluation);
        }
    }

    public static <F extends AssignmentHolderType> void recordRoleEvaluationSkip(F target, boolean fullEvaluation) {
        long count = recordCountInternal(InternalCounters.ROLE_EVALUATION_SKIP_COUNT);
        if (isTrace(InternalCounters.ROLE_EVALUATION_SKIP_COUNT)) {
            traceOperation("roleEvaluationSkip", target::toString, count, true);
        }
    }

    public static InternalInspector getInspector() {
        return inspector;
    }

    public static void setInspector(InternalInspector inspector) {
        InternalMonitor.inspector = inspector;
    }

    public static void reset() {
        LOGGER.info("MONITOR reset");
        COUNTER_MAP.clear();
        TRACE_CLASS_MAP.clear();
        resourceCacheStats = new CachingStatistics();
        connectorCacheStats = new CachingStatistics();
        inspector = null;
    }

    @Override
    public String debugDump(int indent) {
        return debugDumpStatic(indent);
    }

    public static String debugDumpStatic(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilder(InternalMonitor.class, indent);
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "counters", indent + 1);
        for (InternalCounters counter: InternalCounters.values()) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, counter.getLabel(), getCount(counter), indent + 2);
        }
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "traces", indent + 1);
        for (InternalOperationClasses trace: InternalOperationClasses.values()) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, trace.getLabel(), isTrace(trace), indent + 2);
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "cloneTimingEnabled", cloneTimingEnabled, indent + 1);
        if (cloneTimingEnabled) {
            DebugUtil.debugDumpWithLabelLn(sb, "prismObjectCloneDuration", (prismObjectCloneDurationNanos/1000000)+" ms (" + prismObjectCloneDurationNanos + " ns)", indent + 1);
        }
        DebugUtil.debugDumpWithLabelLn(sb, "resourceCacheStats", resourceCacheStats, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "connectorCacheStats", connectorCacheStats, indent + 1);
        return sb.toString();
    }
}
