/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.common.monitor;

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
public class InternalMonitor {
	
	private static final Trace LOGGER = TraceManager.getTrace(InternalMonitor.class);
	
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
	
	private static long shadowFetchOperationCount = 0;
	private static boolean traceShadowFetchOperation = false;
	
	private static long shadowChangeOpeartionCount = 0;
	/**
	 * All provisioning operations that reach out to the resources.
	 */
	private static long provisioningAllExtOperationCount = 0;
	
	public static long getResourceSchemaParseCount() {
		return resourceSchemaParseCount;
	}
	
	public synchronized static void recordResourceSchemaParse() {
		resourceSchemaParseCount++;
		if (traceShadowFetchOperation) {
			traceOperation("resource schema parse", resourceSchemaParseCount);
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
			traceOperation("resource schema fetch", resourceSchemaFetchCount);
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
			traceOperation("shadow fetch", shadowFetchOperationCount);
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
			traceOperation("connector "+name, shadowFetchOperationCount);
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

	private static void traceOperation(String opName, long counter) {
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
			LOGGER.debug("MONITOR {} ({}): {} {}", new Object[]{opName, counter, immediateClass, immediateMethod});
			LOGGER.trace("MONITOR {} ({}):\n{}", new Object[]{opName, counter, sb});
		}
	}
}
