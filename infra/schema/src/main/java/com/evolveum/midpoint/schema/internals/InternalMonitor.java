/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismMonitor;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Simple monitoring object. It records the count of expensive operations
 * in the system. It is used in the tests to make sure such operations are not
 * executed more frequently than expected. It may also have some run-time value.
 * 
 * @author Radovan Semancik
 *
 */
public class InternalMonitor implements PrismMonitor {
	
	private static final Trace LOGGER = TraceManager.getTrace(InternalMonitor.class);

	private static final String CLONE_START_TIMESTAMP_KEY = InternalMonitor.class.getName()+".cloneStartTimestamp";
	
	private static long resourceSchemaParseCount = 0;
	private static long resourceSchemaFetchCount = 0;
	private static boolean traceResourceSchemaOperations = false;
	
	private static long connectorInstanceInitializationCount = 0;
	private static long connectorSchemaParseCount = 0;
	private static long connectorCapabilitiesFetchCount = 0;
	private static CachingStatistics resourceCacheStats = new CachingStatistics();
	private static CachingStatistics connectorCacheStats = new CachingStatistics();
	private static long scriptCompileCount = 0;
	private static long scriptExecutionCount = 0;
	
	private static boolean traceConnectorOperation = false;
	private static long connectorOperationCount = 0;
	
	private static long connectorSimulatedPagingSearchCount = 0;
	
	private static long shadowFetchOperationCount = 0;
	private static boolean traceShadowFetchOperation = false;
	
	private static long shadowChangeOpeartionCount = 0;
	/**
	 * All provisioning operations that reach out to the resources.
	 */
	private static long provisioningAllExtOperationCount = 0;
	
	private static long prismObjectCloneCount = 0;
	private static long prismObjectCloneDurationMillis = 0;
	private static boolean tracePrismObjectClone = false;
	
	public static long getResourceSchemaParseCount() {
		return resourceSchemaParseCount;
	}
	
	public synchronized static void recordResourceSchemaParse() {
		resourceSchemaParseCount++;
		if (traceShadowFetchOperation) {
			traceOperation("resource schema parse", resourceSchemaParseCount, true);
		}
	}
	
	public static long getConnectorInstanceInitializationCount() {
		return connectorInstanceInitializationCount;
	}
	
	public synchronized static void recordConnectorInstanceInitialization() {
		connectorInstanceInitializationCount++;
	}
	
	public static long getResourceSchemaFetchCount() {
		return resourceSchemaFetchCount;
	}
	
	public synchronized static void recordResourceSchemaFetch() {
		resourceSchemaFetchCount++;
		provisioningAllExtOperationCount++;
		if (traceShadowFetchOperation) {
			traceOperation("resource schema fetch", resourceSchemaFetchCount, true);
		}
	}

	public static long getConnectorSchemaParseCount() {
		return connectorSchemaParseCount;
	}
	
	public synchronized static void recordConnectorSchemaParse() {
		connectorSchemaParseCount++;
	}

	public static long getConnectorCapabilitiesFetchCount() {
		return connectorCapabilitiesFetchCount;
	}
	
	public synchronized static void recordConnectorCapabilitiesFetchCount() {
		connectorCapabilitiesFetchCount++;
		provisioningAllExtOperationCount++;
	}

	public static CachingStatistics getResourceCacheStats() {
		return resourceCacheStats;
	}

	public static CachingStatistics getConnectorCacheStats() {
		return connectorCacheStats;
	}

	public static long getScriptCompileCount() {
		return scriptCompileCount;
	}

	public static void setScriptCompileCount(long scriptCompileCount) {
		InternalMonitor.scriptCompileCount = scriptCompileCount;
	}
	
	public static void recordScriptCompile() {
		scriptCompileCount++;
	}

	public static long getScriptExecutionCount() {
		return scriptExecutionCount;
	}

	public static void setScriptExecutionCount(long scriptExecutionCount) {
		InternalMonitor.scriptExecutionCount = scriptExecutionCount;
	}
	
	public static void recordScriptExecution() {
		scriptExecutionCount++;
	}

	public static long getShadowFetchOperationCount() {
		return shadowFetchOperationCount;
	}
	
	public static void recordShadowFetchOperation() {
		shadowFetchOperationCount++;
		provisioningAllExtOperationCount++;
		if (traceShadowFetchOperation) {
			traceOperation("shadow fetch", shadowFetchOperationCount, true);
		}
	}

	public static boolean isTraceShadowFetchOperation() {
		return traceShadowFetchOperation;
	}

	public static void setTraceShadowFetchOperation(boolean traceShadowFetchOperation) {
		LOGGER.debug("MONITOR traceShadowFetchOperation={}", traceShadowFetchOperation);
		InternalMonitor.traceShadowFetchOperation = traceShadowFetchOperation;
	}

	public static boolean isTraceResourceSchemaOperations() {
		return traceResourceSchemaOperations;
	}

	public static void setTraceResourceSchemaOperations(
			boolean traceResourceSchemaOperations) {
		LOGGER.debug("MONITOR traceResourceSchemaOperations={}", traceResourceSchemaOperations);
		InternalMonitor.traceResourceSchemaOperations = traceResourceSchemaOperations;
	}

	public static long getShadowChangeOpeartionCount() {
		return shadowChangeOpeartionCount;
	}
	
	public static void recordShadowChangeOperation() {
		shadowChangeOpeartionCount++;
		provisioningAllExtOperationCount++;
	}
	
	public static long getConnectorOperationCount() {
		return connectorOperationCount;
	}
	
	public static void recordConnectorOperation(String name) {
		connectorOperationCount++;
		if (traceConnectorOperation) {
			traceOperation("connector "+name, connectorOperationCount, true);
		}
	}
	
	public static long getConnectorSimulatedPagingSearchCount() {
		return connectorSimulatedPagingSearchCount;
	}
	
	public static void recordConnectorSimulatedPagingSearchCount() {
		connectorSimulatedPagingSearchCount++;
		if (traceConnectorOperation) {
			traceOperation("simulated paged search", connectorSimulatedPagingSearchCount, true);
		}
	}

	public static boolean isTraceConnectorOperation() {
		return traceShadowFetchOperation;
	}

	public static void setTraceConnectorOperation(boolean trace) {
		LOGGER.debug("MONITOR traceConnectorOperation={}", trace);
		InternalMonitor.traceConnectorOperation = trace;
	}
	
	public static long getProvisioningAllExtOperationCont() {
		return provisioningAllExtOperationCount;
	}
	
	public static void recordShadowOtherOperation() {
		provisioningAllExtOperationCount++;
	}
	
	public static long getPrismObjectCloneDurationMillis() {
		return prismObjectCloneDurationMillis;
	}

	public static void setPrismObjectCloneDurationMillis(long prismObjectCloneDurationMillis) {
		InternalMonitor.prismObjectCloneDurationMillis = prismObjectCloneDurationMillis;
	}

	public static boolean isTracePrismObjectClone() {
		return tracePrismObjectClone;
	}

	public static void setTracePrismObjectClone(boolean tracePrismObjectClone) {
		InternalMonitor.tracePrismObjectClone = tracePrismObjectClone;
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
		prismObjectCloneCount++;
		Object cloneStartObject = orig.getUserData(CLONE_START_TIMESTAMP_KEY);
		if (cloneStartObject != null && cloneStartObject instanceof Long) {
			long cloneDurationMillis = System.currentTimeMillis() - (Long)cloneStartObject;
			prismObjectCloneDurationMillis += cloneDurationMillis;
			LOGGER.debug("MONITOR prism object clone end: {} (duration {} ms)", orig, cloneDurationMillis);
		} else {
			LOGGER.debug("MONITOR prism object clone end: {}", orig);
		}
		if (tracePrismObjectClone) {
			traceOperation("prism object clone", prismObjectCloneCount, false);
		}
	}
	
	public static long getPrismObjectCloneCount() {
		return prismObjectCloneCount;
	}

	public static void setPrismObjectCloneCount(long prismObjectCloneCount) {
		InternalMonitor.prismObjectCloneCount = prismObjectCloneCount;
	}

	public static void reset() {
		LOGGER.info("MONITOR reset");
		resourceSchemaParseCount = 0;
		connectorInstanceInitializationCount = 0;
		resourceSchemaFetchCount = 0;
		connectorSchemaParseCount = 0;
		connectorCapabilitiesFetchCount = 0;
		resourceCacheStats = new CachingStatistics();
		connectorCacheStats = new CachingStatistics();
		scriptCompileCount = 0;
		scriptExecutionCount = 0;
		shadowFetchOperationCount = 0;
		traceShadowFetchOperation = false;
		shadowChangeOpeartionCount = 0;
		traceConnectorOperation = false;
		connectorOperationCount = 0;
	}

	private static void traceOperation(String opName, long counter, boolean traceAndDebug) {
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
			if (traceAndDebug) {
				LOGGER.debug("MONITOR {} ({}): {} {}", new Object[]{opName, counter, immediateClass, immediateMethod});
			}
			LOGGER.trace("MONITOR {} ({}):\n{}", new Object[]{opName, counter, sb});
		}
	}

}
