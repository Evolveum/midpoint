/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.internals;

/**
 * @author semancik
 *
 */
public enum InternalOperationClasses {
    RESOURCE_SCHEMA_OPERATIONS("resourceSchemaOperations", "resource schema operations"),

    CONNECTOR_OPERATIONS("connectorOperations", "connector operations"),

    SHADOW_FETCH_OPERATIONS("shadowFetchOperations", "shadow fetch operations"),

    REPOSITORY_OPERATIONS("repositoryOperations", "repository operations"),

    ROLE_EVALUATIONS("roleEvaluations", "role evaluations"),

    PRISM_OPERATIONS("prismOperations", "prism operations");

    // Used as localization key
    private String key;

    // Used in logfiles, etc.
    private String label;

    InternalOperationClasses(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }
}
