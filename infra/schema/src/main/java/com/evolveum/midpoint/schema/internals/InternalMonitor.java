/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema.internals;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismMonitor;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

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

	private static Map<InternalCounters,Long> counterMap = new HashMap<>();
	private static Map<InternalOperationClasses,Boolean> traceMap = new HashMap<>();

	private static CachingStatistics resourceCacheStats = new CachingStatistics();
	private static CachingStatistics connectorCacheStats = new CachingStatistics();

	private static long prismObjectCloneDurationMillis = 0;

	private static InternalInspector inspector;

	public static long getCount(InternalCounters counter) {
		Long count = counterMap.get(counter);
		if (count == null) {
			return 0;
		}
		return count;
	}

	public static void recordCount(InternalCounters counter) {
		long count = recordCountInternal(counter);
		InternalOperationClasses operationClass = counter.getOperationClass();
		if (operationClass != null && isTrace(operationClass)) {
			traceOperation(operationClass, count);
		}
	}

	private static synchronized long recordCountInternal(InternalCounters counter) {
		Long count = counterMap.get(counter);
		if (count == null) {
			count = 0L;
		}
		count++;
		counterMap.put(counter, count);
		return count;
	}

	public static boolean isTrace(InternalOperationClasses operationClass) {
		Boolean b = traceMap.get(operationClass);
		if (b == null) {
			return false;
		} else {
			return b;
		}
	}

	private static boolean isTrace(InternalCounters counter) {
		InternalOperationClasses operationClass = counter.getOperationClass();
		if (operationClass == null) {
			return false;
		}
		return isTrace(operationClass);
	}

	public static void setTrace(InternalOperationClasses operationClass, boolean val) {
		traceMap.put(operationClass, val);
	}

	private static void traceOperation(InternalOperationClasses operationClass, long counter) {
		traceOperation(operationClass.getKey(), null, counter, false);
	}

	private static void traceOperation(String opName, Supplier<String> paramsSupplier, long counter, boolean traceAndDebug) {
		LOGGER.info("MONITOR {} ({})", opName, counter);
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
				sb.append(stackElement.toString());
				sb.append("\n");
			}
			String params = "";
			if (paramsSupplier != null) {
				params = paramsSupplier.get();
			}
			if (traceAndDebug) {
				LOGGER.debug("MONITOR {}({}) ({}): {} {}", opName, params, counter, immediateClass, immediateMethod);
			}
			LOGGER.trace("MONITOR {}({}) ({}):\n{}", opName, params, counter, sb);
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
			traceOperation("connector", () -> name, count, true);
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
		recordCountInternal(InternalCounters.PRISM_OBJECT_COMPARE_COUNT);
	}

	public static long getPrismObjectCloneDurationMillis() {
		return prismObjectCloneDurationMillis;
	}

	public static void setPrismObjectCloneDurationMillis(long prismObjectCloneDurationMillis) {
		InternalMonitor.prismObjectCloneDurationMillis = prismObjectCloneDurationMillis;
	}

	@Override
	public <O extends Objectable> void beforeObjectClone(PrismObject<O> orig) {
		LOGGER.trace("MONITOR prism object clone start: {}", orig);
		if (!orig.isImmutable()) {
			orig.setUserData(CLONE_START_TIMESTAMP_KEY, System.currentTimeMillis());
		}
	}

	@Override
	public synchronized <O extends Objectable> void afterObjectClone(PrismObject<O> orig, PrismObject<O> clone) {
		long count = recordCountInternal(InternalCounters.PRISM_OBJECT_CLONE_COUNT);
		Object cloneStartObject = orig.getUserData(CLONE_START_TIMESTAMP_KEY);
		if (cloneStartObject != null && cloneStartObject instanceof Long) {
			long cloneDurationMillis = System.currentTimeMillis() - (Long)cloneStartObject;
			prismObjectCloneDurationMillis += cloneDurationMillis;
			LOGGER.debug("MONITOR prism object clone end: {} (duration {} ms)", orig, cloneDurationMillis);
		} else {
			LOGGER.debug("MONITOR prism object clone end: {}", orig);
		}
		if (isTrace(InternalCounters.PRISM_OBJECT_CLONE_COUNT)) {
			traceOperation("prism object clone", null, count, false);
		}
	}

	public static <F extends FocusType> void recordRoleEvaluation(F target, boolean fullEvaluation) {
		long count = recordCountInternal(InternalCounters.ROLE_EVALUATION_COUNT);
		if (isTrace(InternalCounters.ROLE_EVALUATION_COUNT)) {
			traceOperation("roleEvaluation", () -> target.toString() , count, true);
		}
		if (inspector != null) {
			inspector.inspectRoleEvaluation(target, fullEvaluation);
		}
	}

	public static <F extends FocusType> void recordRoleEvaluationSkip(F target, boolean fullEvaluation) {
		long count = recordCountInternal(InternalCounters.ROLE_EVALUATION_SKIP_COUNT);
		if (isTrace(InternalCounters.ROLE_EVALUATION_SKIP_COUNT)) {
			traceOperation("roleEvaluationSkip", () -> target.toString() , count, true);
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
		counterMap.clear();
		traceMap.clear();
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
		// TODO
		return sb.toString();
	}
}
