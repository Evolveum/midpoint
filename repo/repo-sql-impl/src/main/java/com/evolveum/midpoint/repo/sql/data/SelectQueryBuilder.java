/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.repo.sql.Database;

/**
 * @author skublik
 */
public class SelectQueryBuilder {

    private final Database database;
    private final Map<Integer, Object> parameters = new HashMap<>();

    private String query;
    private Integer firstResult = null;
    private Integer maxResult = null;

    public SelectQueryBuilder(Database database, String query) {
        Objects.requireNonNull(database, "Database is null");
        if (StringUtils.isBlank(query)) {
            throw new IllegalArgumentException("Query is empty");
        }
        this.database = database;
        if (database.equals(Database.ORACLE)) {
            int indexOfWhere = query.toLowerCase().indexOf("where");
            indexOfWhere = (indexOfWhere == -1) ? query.length() : indexOfWhere;
            String partOfQuery = query.substring(0, indexOfWhere);
            partOfQuery = partOfQuery.replace("as ", "");
            partOfQuery = partOfQuery.replace("AS ", "");
            this.query = partOfQuery + query.substring(indexOfWhere);
        } else {
            this.query = query;
        }
    }

    private void addPaging(int firstResult, int maxResult) {
        Objects.requireNonNull(database, "Database is null");
        StringBuilder sb = new StringBuilder();
        String queryWithoutStringValue = query.replaceAll("\".*?\"|'.*?'|`.*`", "");
        if (Database.SQLSERVER.equals(database)) {
            if (queryWithoutStringValue.toLowerCase().contains(" offset ") || queryWithoutStringValue.contains(" fetch ")) {
                throw new IllegalArgumentException("query already contains offset or fetch");
            }
            sb.append(query)
                    .append(" OFFSET ").append(firstResult).append(" ROWS")
                    .append(" FETCH NEXT ").append(maxResult).append(" ROWS ONLY ");
        } else if (Database.ORACLE.equals(database)) {
            sb.append("SELECT * FROM" +
                    "( " +
                    "SELECT a.*, rownum r__ FROM (")
                    .append(query)
                    .append(") a WHERE rownum < ").append(firstResult + maxResult + 1)
                    .append(") WHERE r__ >= ").append(firstResult + 1).append(" ");
        } else if (Database.H2.equals(database) || Database.MARIADB.equals(database)
                || Database.MYSQL.equals(database) || Database.POSTGRESQL.equals(database)) {
            if (queryWithoutStringValue.toLowerCase().contains(" limit ") || queryWithoutStringValue.contains(" offset ")) {
                throw new IllegalArgumentException("query already contains offset or fetch");
            }
            sb.append(query)
                    .append(" LIMIT ").append(maxResult)
                    .append(" OFFSET ").append(firstResult).append(" ");
        } else {
            throw new IllegalArgumentException("Unsupported type of database: " + database);
        }
        query = sb.toString();
    }

    private void addFirstResult(int firstResult) {
        Objects.requireNonNull(database, "Database is null");

        StringBuilder sb = new StringBuilder();
        String queryWithoutStringValue = query.replaceAll("\".*?\"|'.*?'|`.*`", "");
        if (Database.H2.equals(database) || Database.MARIADB.equals(database)
                || Database.MYSQL.equals(database) || Database.POSTGRESQL.equals(database)) {
            if (queryWithoutStringValue.contains(" offset ")) {
                throw new IllegalArgumentException("query already contains offset");
            }
            sb.append(query).append(" OFFSET ").append(firstResult).append(" ");
        } else if (Database.ORACLE.equals(database)) {
            sb.append("SELECT * FROM" +
                    "( ")
                    .append(query)
                    .append(") WHERE rownum > ").append(firstResult).append(" ");
        } else if (Database.SQLSERVER.equals(database)) {
            if (queryWithoutStringValue.toLowerCase().contains(" offset ")) {
                throw new IllegalArgumentException("query already contains offset");
            }
            sb.append(query).append(" OFFSET ").append(firstResult).append(" ROWS ");
        } else {
            throw new IllegalArgumentException("Unsupported type of database: " + database);
        }
        query = sb.toString();
    }

    private void addMaxResult(int maxResult) {
        Objects.requireNonNull(database, "Database is null");
        StringBuilder sb = new StringBuilder();
        String queryWithoutStringValue = query.replaceAll("\".*?\"|'.*?'|`.*`", "");
        if (Database.H2.equals(database) || Database.MARIADB.equals(database)
                || Database.MYSQL.equals(database) || Database.POSTGRESQL.equals(database)) {
            if (queryWithoutStringValue.toLowerCase().contains(" limit ")) {
                throw new IllegalArgumentException("query already contains limit");
            }
            sb.append(query).append(" LIMIT ").append(maxResult).append(" ");
        } else if (Database.SQLSERVER.equals(database)) {
            if (queryWithoutStringValue.toLowerCase().contains(" fetch ")) {
                throw new IllegalArgumentException("query already contains fetch");
            }
            sb.append(query)
                    .append(" OFFSET 0 ROWS")           // looks like FETCH NEXT does not work without OFFSET clause
                    .append(" FETCH NEXT ").append(maxResult).append(" ROWS ONLY ");
        } else if (Database.ORACLE.equals(database)) {
            sb.append("SELECT * FROM" +
                    "( ")
                    .append(query)
                    .append(") WHERE rownum <= ").append(maxResult).append(" ");
        } else {
            throw new IllegalArgumentException("Unsupported type of database: " + database);
        }
        query = sb.toString();
    }

    public void addParameter(String name, Object value) {
        String workQuery = query.replaceAll("\".*?\"|'.*?'|`.*`", "");
        addParameter(workQuery, name, value);
    }

    public void addParameter(int index, Object value) {
        parameters.put(index, value);
    }

    private void addParameter(String workQuery, String name, Object value) {
        name = ":" + name;
        if (!(workQuery.contains(name))) {
            throw new IllegalArgumentException("query don't contains value name " + name);
        }
        while (workQuery.contains(name)) {
            String partQuery = workQuery.substring(0, workQuery.indexOf(name));
            int index = StringUtils.countMatches(partQuery, "?") + StringUtils.countMatches(partQuery, ":") + 1;

            if (value != null && value.getClass().isArray()) {
                value = Collections.singletonList(value);
            }

            if (value instanceof List) {
                StringBuilder queryPartForArray = new StringBuilder();
                for (Object singleValue : (List) value) {
                    queryPartForArray.append((queryPartForArray.length() == 0) ? "?" : ", ?");
                    parameters.put(index, singleValue);
                    index++;
                }
                query = query.replaceFirst(name, queryPartForArray.toString());
            } else {
                parameters.put(index, value);
                query = query.replaceFirst(name, "?");
            }
            workQuery = (workQuery.length() <= (workQuery.indexOf(name) + name.length())) ? "" : workQuery.substring(workQuery.indexOf(name) + name.length());
        }
    }

    public void addParameters(Map<String, Object> parameters) {
        for (String name : parameters.keySet()) {
            String workQuery = query.replaceAll("\".*?\"|'.*?'|`.*`", "");
            addParameter(workQuery, name, parameters.get(name));
        }
    }

    @Override
    public String toString() {
        return "Database: " + database
                + ", Query: " + query
                + ", Parameters: " + parameters;
    }

    public SingleSqlQuery build() {
        if (firstResult != null && maxResult != null) {
            addPaging(firstResult, maxResult);
        } else if (firstResult != null || maxResult != null) {
            if (firstResult != null) {
                addFirstResult(firstResult);
            } else {
                addMaxResult(maxResult);
            }
        }
        return new SingleSqlQuery(query, parameters);
    }

    public void setFirstResult(Integer firstResult) {
        this.firstResult = firstResult;
    }

    public void setMaxResult(Integer maxResult) {
        this.maxResult = maxResult;
    }
}
