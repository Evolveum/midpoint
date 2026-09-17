/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

/**
 * Stable conndev documentation topic keys requested by the connector wizard screens. Each key is
 * the contract between the wizard and the documentation JARs on the classpath
 * ({@code META-INF/conndev-doc/docs.yaml}): a wizard screen requests its key (optionally with a
 * fallback key) and the documentation authors create pages under these keys.
 */
public final class ConnectorWizardHelpTopics {

    public static final String APPLICATION_IDENTIFICATION = "application-identification";
    public static final String DOCUMENTATION_SOURCE = "documentation-source";
    public static final String CONNECTOR_COORDINATES = "connector-coordinates";
    public static final String BASE_URL = "base-url";
    public static final String AUTHENTICATION = "authentication";
    public static final String CREDENTIALS = "credentials";
    public static final String CONNECTIVITY_ENDPOINT = "connectivity-endpoint";
    public static final String FIX_CONNECTION = "fix-connection";
    public static final String SQL_CONNECTION = "sql-connection";
    public static final String OBJECT_CLASSES = "object-classes";
    public static final String SELECT_OBJECT_CLASS = "select-object-class";
    public static final String OBJECT_CLASS_SCHEMA = "object-class-schema";
    public static final String NATIVE_SCHEMA = "native-schema";
    public static final String SEARCH_ALL = "search-all";
    public static final String SEARCH_BY_ID = "search-by-id";
    public static final String SEARCH_FILTER = "search-filter";
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String RELATIONSHIPS = "relationships";
    public static final String RELATIONSHIP_SELECTION = "relationship-selection";
    public static final String RELATIONSHIP = "relationship";
    public static final String NEXT_STEPS = "next-steps";

    /** Generic endpoint guidance, used as the fallback when a screen-specific endpoint key has no page. */
    public static final String ENDPOINT_SELECTION = "endpoint-selection";
    public static final String SEARCH_ALL_ENDPOINT = "search-all-endpoint";
    public static final String SEARCH_BY_ID_ENDPOINT = "search-by-id-endpoint";
    public static final String SEARCH_FILTER_ENDPOINT = "search-filter-endpoint";
    public static final String CREATE_ENDPOINT = "create-endpoint";
    public static final String UPDATE_ENDPOINT = "update-endpoint";
    public static final String DELETE_ENDPOINT = "delete-endpoint";

    private ConnectorWizardHelpTopics() {
    }
}
