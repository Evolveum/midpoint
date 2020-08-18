/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.querydsl.sql.ColumnMetadata;
import org.apache.commons.lang3.StringUtils;

/**
 * @author skublik
 */
public class InsertQueryBuilder {

    private final StringBuilder sbQuery;
    private final StringBuilder sbValues;
    private final Map<Integer, Object> parameters = new HashMap<>();
    private final List<Integer> primaryKey = new ArrayList<>();

    private int index = 1;

    public InsertQueryBuilder(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("Name of table is empty");
        }
        sbQuery = new StringBuilder("INSERT INTO ");
        sbQuery.append(tableName).append(" (");
        sbValues = new StringBuilder(" VALUES (");
    }

    public void addNullParameter(ColumnMetadata column) {
        addNullParameter(column.getName());
    }

    public void addNullParameter(String nameOfParameter) {
        addParameter(nameOfParameter, null);
    }

    public void addParameter(ColumnMetadata column, Object value) {
        addParameter(column.getName(), value);
    }

    public void addParameter(String nameOfParameter, Object value) {
        addParameter(nameOfParameter, value, false);
    }

    public void addParameter(ColumnMetadata column, Object value, boolean isPrimaryKey) {
        addParameter(column.getName(), value, isPrimaryKey);
    }

    public void addParameter(String nameOfParameter, Object value, boolean isPrimaryKey) {
        sbQuery.append(parameters.isEmpty() ? "" : ", ").append(nameOfParameter);
        sbValues.append(parameters.isEmpty() ? "" : ", ").append("?");
        parameters.put(index, value);
        if (isPrimaryKey) {
            primaryKey.add(index);
        }
        index++;
    }

    public SingleSqlQuery build() {
        String query = sbQuery.append(") ").append(sbValues.toString()).append(") ").toString();
        SingleSqlQuery sqlQuery = new SingleSqlQuery(query, parameters);
        sqlQuery.setPrimaryKeys(primaryKey);
        return sqlQuery;
    }
}
