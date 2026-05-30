/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.tools.dbdocs.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Documentation model for a database table.
 */
public final class TableDoc {

    private final String name;
    private final Path sourceFile;
    private final DocRegion region;
    private final DocMetadata metadata;
    private final List<ColumnDoc> columns;
    private final List<IndexDoc> indexes;
    private final List<ForeignKeyDoc> foreignKeys;
    private final String inheritsFrom;
    private final String partitionOf;
    private final String parseNote;

    public TableDoc(
            String name,
            Path sourceFile,
            DocRegion region,
            DocMetadata metadata,
            List<ColumnDoc> columns,
            List<IndexDoc> indexes,
            List<ForeignKeyDoc> foreignKeys,
            String inheritsFrom,
            String partitionOf,
            String parseNote) {
        this.name = name;
        this.sourceFile = sourceFile;
        this.region = region;
        this.metadata = metadata != null ? metadata : DocMetadata.EMPTY;
        this.columns = List.copyOf(columns);
        this.indexes = List.copyOf(indexes);
        this.foreignKeys = List.copyOf(foreignKeys);
        this.inheritsFrom = inheritsFrom;
        this.partitionOf = partitionOf;
        this.parseNote = parseNote;
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

    public DocMetadata metadata() {
        return metadata;
    }

    public List<ColumnDoc> columns() {
        return columns;
    }

    public List<IndexDoc> indexes() {
        return indexes;
    }

    public List<ForeignKeyDoc> foreignKeys() {
        return foreignKeys;
    }

    public String inheritsFrom() {
        return inheritsFrom;
    }

    public String partitionOf() {
        return partitionOf;
    }

    public String parseNote() {
        return parseNote;
    }
}
