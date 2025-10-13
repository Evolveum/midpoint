/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SystemException;

public class SqlTableMetadata {

    /**
     * Maps from normalized name (lowercase) to {@link com.querydsl.sql.ColumnMetadata}.
     */
    private final Map<String, ColumnMetadata> columnMap = new LinkedHashMap<>();

    /**
     * Creates metadata for a table.
     * Implementation detail: It is possible to use {@link Connection#getMetaData()} but this
     * iterates over too many items for all visible tables and is very slow on Oracle.
     * Using {@link ResultSet#getMetaData()} is much more efficient and gives us all we need.
     */
    public static SqlTableMetadata create(Connection conn, String tableName) {
        try {
            SqlTableMetadata tableMetadata = new SqlTableMetadata();
            try (PreparedStatement stmt =
                    conn.prepareStatement("select * from " + tableName + " where 0=1")) {
                ResultSet rs = stmt.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    tableMetadata.add(ColumnMetadata.named(metaData.getColumnName(i))
                            .ofType(metaData.getColumnType(i))
                            .withSize(metaData.getColumnDisplaySize(i)));
                }
            }
            return tableMetadata;
        } catch (SQLException e) {
            throw new SystemException("Failed to obtain metadata for table " + tableName, e);
        }
    }

    public void add(ColumnMetadata columnMetadata) {
        columnMap.put(columnMetadata.getName().toLowerCase(), columnMetadata);
    }

    public ColumnMetadata get(@NotNull String columnName) {
        return columnMap.get(columnName.toLowerCase());
    }
}
