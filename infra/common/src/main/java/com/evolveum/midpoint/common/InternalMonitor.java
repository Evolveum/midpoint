/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.common;

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
	
	public static long getResourceSchemaParseCount() {
		return resourceSchemaParseCount;
	}
	
	public static void recordResourceSchemaParse() {
		resourceSchemaParseCount++;
	}
	
	public static long getConnectorInitializationCount() {
		return connectorInitializationCount;
	}
	
	public static void recordConnectorInitialization() {
		connectorInitializationCount++;
	}
	
	public static long getConnectorSchemaFetchCount() {
		return connectorSchemaFetchCount;
	}
	
	public static void recordConnectorSchemaFetch() {
		connectorSchemaFetchCount++;
	}

}
