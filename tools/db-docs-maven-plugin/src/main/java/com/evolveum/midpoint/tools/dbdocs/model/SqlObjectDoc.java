/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Generic documentation model for non-table schema objects.
 */
public final class SqlObjectDoc {

    public enum Kind {
        VIEW,
        MATERIALIZED_VIEW,
        ENUM_TYPE,
        FUNCTION,
        PROCEDURE,
        TRIGGER,
        EXTENSION,
        SCHEMA,
        DO_BLOCK,
        ALTER_TABLE
    }

    public static final String DETAIL_TABLE = "table";
    public static final String DETAIL_EXECUTES = "executes";

    private final String name;
    private final Path sourceFile;
    private final DocRegion region;
    private final Kind kind;
    private final DocMetadata metadata;
    private final Map<String, String> details;
    private final List<String> values;

    public SqlObjectDoc(
            String name,
            Path sourceFile,
            DocRegion region,
            Kind kind,
            DocMetadata metadata,
            Map<String, String> details,
            List<String> values) {
        this.name = name;
        this.sourceFile = sourceFile;
        this.region = region;
        this.kind = kind;
        this.metadata = metadata != null ? metadata : DocMetadata.EMPTY;
        this.details = Map.copyOf(details);
        this.values = List.copyOf(values);
    }

    public String name() {
        return name;
    }

    public Path sourceFile() {
        return sourceFile;
    }

    public DocRegion region() {
        return region;
    }

    public Kind kind() {
        return kind;
    }

    public DocMetadata metadata() {
        return metadata;
    }

    public String detail(String key) {
        return details.get(key);
    }

    public List<String> values() {
        return values;
    }
}
