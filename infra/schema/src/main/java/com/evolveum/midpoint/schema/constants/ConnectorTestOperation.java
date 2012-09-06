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
package com.evolveum.midpoint.schema.constants;

/**
 * Enumeration of standardized test connection opration codes as they are presented in the OperationResult.
 * 
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
public enum ConnectorTestOperation {
	
	TEST_CONNECTION(ConnectorTestOperation.class.getName() + ".testConnection"),

	/**
	 * Check whether the configuration is valid e.g. well-formed XML, valid with regard to schema, etc.
	 */
	CONFIGURATION_VALIDATION(ConnectorTestOperation.class.getName() + ".configurationValidation"),

	/**
	 * Check whether the connector can be initialized.
	 * E.g. connector classes can be loaded, it can process configuration, etc.
	 */
	CONNECTOR_INITIALIZATION(ConnectorTestOperation.class.getName() + ".connectorInitialization"),

	/**
	 * Check whether a connection to the resource can be established.
	 */
	CONNECTOR_CONNECTION(ConnectorTestOperation.class.getName() + ".connectorConnection"),

	/**
	 * Check whether the connector can fetch and process resource schema.
	 */
	CONNECTOR_SCHEMA(ConnectorTestOperation.class.getName() + ".connectorSchema"),

	/**
	 * Check whether the connector can be used to fetch some mandatory objects (e.g. fetch a "root" user).
	 */
	CONNECTOR_SANITY(ConnectorTestOperation.class.getName() + ".connectorSanity"),

	EXTRA_TEST(ConnectorTestOperation.class.getName() + ".extraTest");

	private String operation;

	private ConnectorTestOperation(String operation) {
		this.operation = operation;
	}

	public String getOperation() {
		return operation;
	}
}
