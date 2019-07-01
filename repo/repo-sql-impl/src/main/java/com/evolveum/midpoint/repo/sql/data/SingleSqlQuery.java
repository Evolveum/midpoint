/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.repo.sql.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * @author skublik
 */
public class SingleSqlQuery extends SqlQuery {

	private String query;
	private Map<Integer, Object> parameters;
	
	public SingleSqlQuery(String query, Map<Integer, Object> parameters) {
		if(StringUtils.isBlank(query)) {
			throw new IllegalArgumentException("Query is empty");
		}
		this.query = query;
		this.parameters = parameters;
	}
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return createPreparedStatement(con, false);
	}
	
	public PreparedStatement createPreparedStatement(Connection con, boolean getGeneratedKey) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(query, getGeneratedKey ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
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
