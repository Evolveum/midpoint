/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public static SqlTableMetadata create(Connection conn, String tableName) {
        try {
            SqlTableMetadata tableMetadata = new SqlTableMetadata();
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, null, null);
            while (rs.next()) {
                // constants from metaData.getColumns Javadoc
                if (tableName.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                    tableMetadata.add(ColumnMetadata.named(rs.getString("COLUMN_NAME"))
                            .ofType(rs.getInt("DATA_TYPE"))
                            .withSize(rs.getInt("COLUMN_SIZE")));
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
