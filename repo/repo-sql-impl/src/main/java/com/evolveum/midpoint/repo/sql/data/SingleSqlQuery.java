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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * @author skublik
 */
public class SingleSqlQuery extends SqlQuery {

    private final String query;
    private final Map<Integer, Object> parameters;

    public SingleSqlQuery(String query, Map<Integer, Object> parameters) {
        if (StringUtils.isBlank(query)) {
            throw new IllegalArgumentException("Query is empty");
        }
        this.query = query;
        this.parameters = parameters;
    }

    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        return createPreparedStatement(con, null);
    }

    public PreparedStatement createPreparedStatement(Connection con, String[] keyColumn) throws SQLException {
        PreparedStatement stmt;
        if (keyColumn != null && keyColumn.length > 0) {
            stmt = con.prepareStatement(query, keyColumn);
        } else {
            stmt = con.prepareStatement(query);
        }
        addParametersToStatment(parameters, stmt);
        return stmt;
    }

    public Map<Integer, Object> getParameters() {
        return parameters;
    }

    public String getQuery() {
        return query;
    }

    public void execute(Connection connection) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = createPreparedStatement(connection);
            stmt.execute();
        } finally {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        }
    }
}
