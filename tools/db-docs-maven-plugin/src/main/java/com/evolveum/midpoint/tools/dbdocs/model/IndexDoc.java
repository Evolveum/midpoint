/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

/**
 * Documentation model for a database index.
 */
public final class IndexDoc {

    private final String name;
    private final String tableName;
    private final DocMetadata metadata;
    private final String columns;
    private final boolean unique;
    private final String method;

    public IndexDoc(
            String name, String tableName, DocMetadata metadata, String columns, boolean unique, String method) {
        this.name = name;
        this.tableName = tableName;
        this.metadata = metadata != null ? metadata : DocMetadata.EMPTY;
        this.columns = columns;
        this.unique = unique;
        this.method = method;
    }

    public String name() {
        return name;
    }

    public String tableName() {
        return tableName;
    }

    public DocMetadata metadata() {
        return metadata;
    }

    public String columns() {
        return columns;
    }

    public boolean unique() {
        return unique;
    }

    public String method() {
        return method;
    }
}
