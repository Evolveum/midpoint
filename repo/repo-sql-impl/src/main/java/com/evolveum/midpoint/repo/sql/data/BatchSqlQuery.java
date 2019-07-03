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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * @author skublik
 */
public class BatchSqlQuery extends SqlQuery {
	
	private String query;
	Set<SingleSqlQuery> queriesForBatch = new HashSet<SingleSqlQuery>();
	
	public void addQueryForBatch(SingleSqlQuery sqlQuery) {
		if(query == null) {
			query = sqlQuery.getQuery();
			setPrimaryKeys(sqlQuery.getPrimaryKeys());
		} else if(!query.equals(sqlQuery.getQuery())) {
			throw new IllegalArgumentException("SingleSqlQuery added to BatchQuery have different query");
		}
		queriesForBatch.add(sqlQuery);
		
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		if(StringUtils.isBlank(query)) {
			throw new IllegalArgumentException("Query is empty");
		}
		PreparedStatement stmt = con.prepareStatement(query);
		Set<SingleSqlQuery> usedSqlQuery = new HashSet<SingleSqlQuery>();
		for(SingleSqlQuery sqlQuery : queriesForBatch) {
			if(isUnique(sqlQuery, usedSqlQuery)){
				stmt.clearParameters();
				addParametersToStatment(sqlQuery.getParameters(), stmt);
				stmt.addBatch();
				usedSqlQuery.add(sqlQuery);
			}
		}
		return stmt;
	}

	private boolean isUnique(SingleSqlQuery sqlQuery, Set<SingleSqlQuery> usedSqlQueries) {
		if(getPrimaryKeys().isEmpty()) {
			return true;
		}
		for(SingleSqlQuery usedSqlQuery : usedSqlQueries) {
			boolean allIsSame = true;
			for(int primaryKey : getPrimaryKeys()) {
				if(sqlQuery.getParameters().size() < primaryKey || usedSqlQuery.getParameters().size() < primaryKey) {
					throw new IllegalArgumentException("Size of list of parameters in added query is less as index of primaryKey");
				}
				if(sqlQuery.getParameters().get(primaryKey) == null || usedSqlQuery.getParameters().get(primaryKey) == null) {
					throw new IllegalArgumentException("Value of primaryKey is null");
				}
				if(!sqlQuery.getParameters().get(primaryKey).equals(usedSqlQuery.getParameters().get(primaryKey))) {
					allIsSame = false;
				}
			}
			if(allIsSame) {
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
