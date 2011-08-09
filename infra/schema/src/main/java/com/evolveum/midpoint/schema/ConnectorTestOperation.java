/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

/**
 * 
 * @author lazyman
 *
 */
public enum ConnectorTestOperation {

	CONFIGURATION_VALIDATION(ConnectorTestOperation.class.getName()+".configurationValidation"),
	
	CONNECTION_INITIALIZATION(ConnectorTestOperation.class.getName()+".connectionInitialization"),
	
	CONNECTOR_CONNECTION(ConnectorTestOperation.class.getName()+".connectorConnection"),
	
	CONNECTOR_SANITY(ConnectorTestOperation.class.getName()+".connectorSanity"),
	
	CONNECTOR_SCHEMA(ConnectorTestOperation.class.getName()+".connectorSchema"),
	
	EXTRA_TEST(ConnectorTestOperation.class.getName()+".extraTest");
	
	String operation;

	private ConnectorTestOperation(String operation) {
		this.operation = operation;
	}
	
	public String getOperation() {
		return operation;
	}
	
}
