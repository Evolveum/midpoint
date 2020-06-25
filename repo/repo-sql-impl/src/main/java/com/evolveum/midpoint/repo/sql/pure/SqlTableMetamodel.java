/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure;

import java.util.*;

import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;

/**
 * Describes metamodel for table columns and their relations to Prism type properties.
 * Builder can be separated, but I don't think it's necessary for our usage.
 */
public class SqlTableMetamodel<T extends RelationalPathBase<?>> {

    private final String tableName;
    private final Class<T> type;

    private Map<String, ColumnMetadata> columns = new LinkedHashMap<>();

    /**
     * Creates metamodel for the table described by designated type (Q-class).
     * Allows registration of any number of columns - typically used for static properties
     * (non-extensions).
     */
    public SqlTableMetamodel(String tableName, Class<T> type, ColumnMetadata... columns) {
        this.tableName = tableName;
        this.type = type;
        for (ColumnMetadata column : columns) {
            this.columns.put(column.getName(), column);
        }
    }

    /**
     * Registers additional column metadata - typically used for extension columns.
     */
    public SqlTableMetamodel<T> add(ColumnMetadata column) {
        columns.put(column.getName(), column);
        return this;
    }

    public Collection<String> columnNames() {
        return Collections.unmodifiableSet(columns.keySet());
    }

    public Set<Map.Entry<String, ColumnMetadata>> columnMetadataEntries() {
        return Collections.unmodifiableSet(columns.entrySet());
    }

    public ColumnMetadata getColumnMetadata(String name) {
        return columns.get(name);
    }
}
