/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
     * Envelope operation for all connector tests.
     */
    CONNECTOR_TEST(ConnectorTestOperation.class.getName() + ".connector"),

    /**
     * Check whether the connector can be initialized.
     * E.g. connector classes can be loaded, it can process configuration, etc.
     */
    CONNECTOR_INITIALIZATION(ConnectorTestOperation.class.getName() + ".connector.initialization"),

    /**
     * Check whether the configuration is valid e.g. well-formed XML, valid with regard to schema, etc.
     */
    CONNECTOR_CONFIGURATION(ConnectorTestOperation.class.getName() + ".connector.configuration"),

    /**
     * Check whether a connection to the resource can be established.
     */
    CONNECTOR_CONNECTION(ConnectorTestOperation.class.getName() + ".connector.connection"),

    /**
     * Check whether a connection to the resource can be established.
     */
    CONNECTOR_CAPABILITIES(ConnectorTestOperation.class.getName() + ".connector.capabilities"),

    /**
     * Check whether the connector can fetch and process resource schema.
     */
    RESOURCE_SCHEMA(ConnectorTestOperation.class.getName() + ".resourceSchema"),

    /**
     * Check whether the connector can be used to fetch some mandatory objects (e.g. fetch a "root" user).
     */
    RESOURCE_SANITY(ConnectorTestOperation.class.getName() + ".resourceSanity"),

    EXTRA_TEST(ConnectorTestOperation.class.getName() + ".extraTest");

    private String operation;

    ConnectorTestOperation(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
