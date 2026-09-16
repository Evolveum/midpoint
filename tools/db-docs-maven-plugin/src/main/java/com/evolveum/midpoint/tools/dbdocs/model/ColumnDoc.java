/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

import java.util.List;

/**
 * Documentation model for a database column.
 */
public final class ColumnDoc {

    private final String name;
    private final String type;
    private final DocMetadata metadata;
    private final boolean required;
    private final String defaultValue;
    private final boolean primaryKey;
    private final List<String> constraints;

    public ColumnDoc(
            String name,
            String type,
            DocMetadata metadata,
            boolean required,
            String defaultValue,
            boolean primaryKey,
            List<String> constraints) {
        this.name = name;
        this.type = type;
        this.metadata = metadata != null ? metadata : DocMetadata.EMPTY;
        this.required = required;
        this.defaultValue = defaultValue;
        this.primaryKey = primaryKey;
        this.constraints = List.copyOf(constraints);
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public DocMetadata metadata() {
        return metadata;
    }

    public boolean required() {
        return required;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public boolean primaryKey() {
        return primaryKey;
    }

    public List<String> constraints() {
        return constraints;
    }
}
