/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database;

/**
 * @deprecated use Querydsl from "pure" package and remove this
 */
@Deprecated
public class BatchSqlQuery extends SqlQuery {

    private final Set<SingleSqlQuery> queriesForBatch = new HashSet<>();
    private String query;

    public BatchSqlQuery(Database database) {
        setDatabase(database);
    }

    public void addQueryForBatch(SingleSqlQuery sqlQuery) {
        if (query == null) {
            query = sqlQuery.getQuery();
            setPrimaryKeys(sqlQuery.getPrimaryKeys());
        } else if (!query.equals(sqlQuery.getQuery())) {
            throw new IllegalArgumentException("SingleSqlQuery added to BatchQuery have different query");
        }
        queriesForBatch.add(sqlQuery);
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        if (StringUtils.isBlank(query)) {
            throw new IllegalArgumentException("Query is empty");
        }
        PreparedStatement stmt = con.prepareStatement(query);
        Set<SingleSqlQuery> usedSqlQuery = new HashSet<>();
        for (SingleSqlQuery sqlQuery : queriesForBatch) {
            if (isUnique(sqlQuery, usedSqlQuery)) {
                stmt.clearParameters();
                addParametersToStatement(sqlQuery.getParameters(), stmt);
                stmt.addBatch();
                usedSqlQuery.add(sqlQuery);
            }
        }
        return stmt;
    }

    private boolean isUnique(SingleSqlQuery sqlQuery, Set<SingleSqlQuery> usedSqlQueries) {
        if (getPrimaryKeys().isEmpty()) {
            return true;
        }
        for (SingleSqlQuery usedSqlQuery : usedSqlQueries) {
            boolean allIsSame = true;
            for (int primaryKey : getPrimaryKeys()) {
                if (sqlQuery.getParameters().size() < primaryKey || usedSqlQuery.getParameters().size() < primaryKey) {
                    throw new IllegalArgumentException("Size of list of parameters in added query is less as index of primaryKey");
                }
                if (sqlQuery.getParameters().get(primaryKey) == null || usedSqlQuery.getParameters().get(primaryKey) == null) {
                    throw new IllegalArgumentException("Value of primaryKey is null");
                }
                if (!sqlQuery.getParameters().get(primaryKey).equals(usedSqlQuery.getParameters().get(primaryKey))) {
                    allIsSame = false;
                }
            }
            if (allIsSame) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = createPreparedStatement(connection);
            stmt.executeBatch();
        } finally {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        }
    }

    public boolean isEmpty() {
        return queriesForBatch.isEmpty();
    }
}
