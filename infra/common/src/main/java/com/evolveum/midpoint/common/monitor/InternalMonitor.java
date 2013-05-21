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

/**
 * Simple monitoring object. It records the count of expensive operations
 * in the system. It is used in the tests to make sure such operations are not
 * executed more frequently than expected. It may also have some run-time value.
 * 
 * @author Radovan Semancik
 *
 */
public class InternalMonitor {
	
	private static long resourceSchemaParseCount = 0;
	private static long connectorInitializationCount = 0;
	private static long connectorSchemaFetchCount = 0;
	private static CachingStatistics resourceCacheStats = new CachingStatistics();
	private static CachingStatistics connectorCacheStats = new CachingStatistics();
	
	public static long getResourceSchemaParseCount() {
		return resourceSchemaParseCount;
	}
	
	public synchronized static void recordResourceSchemaParse() {
		resourceSchemaParseCount++;
	}
	
	public static long getConnectorInitializationCount() {
		return connectorInitializationCount;
	}
	
	public synchronized static void recordConnectorInitialization() {
		connectorInitializationCount++;
	}
	
	public static long getConnectorSchemaFetchCount() {
		return connectorSchemaFetchCount;
	}
	
	public synchronized static void recordConnectorSchemaFetch() {
		connectorSchemaFetchCount++;
	}

	public static CachingStatistics getResourceCacheStats() {
		return resourceCacheStats;
	}

	public static CachingStatistics getConnectorCacheStats() {
		return connectorCacheStats;
	}

}
