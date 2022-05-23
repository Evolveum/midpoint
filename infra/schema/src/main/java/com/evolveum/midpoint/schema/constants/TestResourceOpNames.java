/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.constants;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Enumeration of standardized test connection operation names as they are presented in the {@link OperationResult}
 * returned by `testResource` and related methods.
 *
 * @author lazyman
 * @author Radovan Semancik
 */
public enum TestResourceOpNames {

    /**
     * The root of the operation result tree.
     */
    RESOURCE_TEST(TestResourceOpNames.class.getName() + ".test"),

    /**
     * Envelope operation for all connector-level tests.
     */
    CONNECTOR_TEST(TestResourceOpNames.class.getName() + ".connector"),

    /**
     * Check whether the connector can be instantiated, e.g. connector classes can be loaded.
     */
    CONNECTOR_INSTANTIATION(TestResourceOpNames.class.getName() + ".connector.instantiation"),

    /**
     * Check whether the configuration is valid e.g. well-formed XML, valid with regard to schema, etc.
     */
    CONNECTOR_INITIALIZATION(TestResourceOpNames.class.getName() + ".connector.initialization"),

    /**
     * Check whether a connection to the resource can be established.
     */
    CONNECTOR_CONNECTION(TestResourceOpNames.class.getName() + ".connector.connection"),

    /**
     * Check whether connector capabilities can be retrieved.
     */
    CONNECTOR_CAPABILITIES(TestResourceOpNames.class.getName() + ".connector.capabilities"),

    /**
     * Check whether the connector can fetch and process resource schema.
     */
    RESOURCE_SCHEMA(TestResourceOpNames.class.getName() + ".resourceSchema"),

    /**
     * Check whether the connector can be used to fetch some mandatory objects (e.g. fetch a "root" user).
     *
     * Currently not used.
     */
    RESOURCE_SANITY(TestResourceOpNames.class.getName() + ".resourceSanity"),

    /** Currently not used. */
    EXTRA_TEST(TestResourceOpNames.class.getName() + ".extraTest");

    private final String operation;

    TestResourceOpNames(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
