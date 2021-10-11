/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * @author skublik
 */
public class InsertQueryBuilder {

    private StringBuilder sbQuery;
    private StringBuilder sbValues;
    private int index = 1;
    private Map<Integer, Object> parameters = new HashMap<Integer, Object>();
    private List<Integer> primaryKey = new ArrayList<Integer>();

    public InsertQueryBuilder(String tableName) {
        if(StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("Name of table is empty");
        }
        sbQuery = new StringBuilder("INSERT INTO ");
        sbQuery.append(tableName).append(" (");
        sbValues = new StringBuilder(" VALUES (");
    }

    public  void addNullParameter(String nameOfparameter) {
        addParameter(nameOfparameter, null);
    }

    public  void addParameter(String nameOfparameter, Object value) {
        addParameter(nameOfparameter, value, false);
    }

    public  void addParameter(String nameOfparameter, Object value, boolean isPrimaryKey) {
        sbQuery.append(parameters.isEmpty() ? "" : ", ").append(nameOfparameter);
        sbValues.append(parameters.isEmpty() ? "" : ", ").append("?");
        parameters.put(index, value);
        if(isPrimaryKey) {
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
