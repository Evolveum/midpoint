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
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * @author skublik
 */
public class SelectQueryBuilder {
	
	private Database database;
	String query;
	Map<Integer, Object> parameters = new HashMap<Integer, Object>();
	
	public SelectQueryBuilder(Database database, String query) {
		Validate.notNull(database, "Database is null");
		if(StringUtils.isBlank(query)) {
			throw new IllegalArgumentException("Query is empty");
		}
		this.database = database;
		if(database.equals(Database.ORACLE)) {
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
	
	public SelectQueryBuilder(String query) {
		if(StringUtils.isBlank(query)) {
			throw new IllegalArgumentException("Query is empty");
		}
		this.query = query;
	}
	
	public void addPaging(int firstResult, int maxResult) {
		Validate.notNull(database, "Database is null");
		StringBuilder sb = new StringBuilder();
		String queryWithoutStringValue = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
		if(Database.SQLSERVER.equals(database)) {
			if(queryWithoutStringValue.toLowerCase().contains(" offset ")
					|| queryWithoutStringValue.contains(" fetch ")) {
				throw new IllegalArgumentException("query already contains offset or fetch");
			}
			sb.append(query).append(" OFFSET ").append(firstResult).append(" ROWS FETCH NEXT ")
			.append(maxResult).append(" ROWS ONLY ");
		} else if(Database.ORACLE.equals(database)) {
			sb.append("SELECT * FROM" + 
						"( "+ 
							"SELECT a.*, rownum r__ FROM (")
								.append(query)
							.append(") a WHERE rownum < ").append(firstResult + maxResult+1)
						.append(") WHERE r__ >= ").append(firstResult+1).append(" ");
		} else if(Database.H2.equals(database) || Database.MARIADB.equals(database)
				|| Database.MYSQL.equals(database) || Database.POSTGRESQL.equals(database)) {
			if(queryWithoutStringValue.toLowerCase().contains(" limit ")
					|| queryWithoutStringValue.contains(" offset ")) {
				throw new IllegalArgumentException("query already contains offset or fetch");
			}
			sb.append(query).append(" LIMIT ").append(maxResult).append(" OFFSET ")
			.append(firstResult).append(" ");
		} else {
			throw new IllegalArgumentException("Unsoported type of database: " + database);
		}
		query = sb.toString();
	}
	
	
	public void addParameter(String name, Object value) {
		String workQuery = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
		addParameter(workQuery, name, value);
	}
	
	public void addParameter(int index, Object value) {
		parameters.put(index, value);
	}
	
	private void addParameter(String workQuery, String name, Object value) {
		name = ":" + name;
		if(!(workQuery.contains(name))) {
			throw new IllegalArgumentException("query don't contains value name " + name);
		}
		while(workQuery.contains(name)) {
			String partQuery = workQuery.substring(0, workQuery.indexOf(name));
			int index = StringUtils.countMatches(partQuery, "?") + StringUtils.countMatches(partQuery, ":") + 1;
			
			if(value != null && value.getClass().isArray()) {
				value = Arrays.asList(value);
			}
			
			if(value instanceof List) {
				String queryPartForArray = "";
				for(Object singleValue : (List) value) {
					queryPartForArray = queryPartForArray + (queryPartForArray.isEmpty() ? "?" : ", ?");
					parameters.put(index, singleValue);
					index++;
				}
				query = query.replaceFirst(name, queryPartForArray);
			} else {
				parameters.put(index, value);
				query = query.replaceFirst(name, "?");
			}
			workQuery = (workQuery.length() <= (workQuery.indexOf(name) + name.length())) ? "" : workQuery.substring(workQuery.indexOf(name) + name.length());
		}
	}
	
	public void addParameters (Map<String, Object> parameters) {
		for (String name : parameters.keySet()) {
			String workQuery = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
			addParameter(workQuery, name, parameters.get(name));
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Database: ").append(database)
			.append(", Query: ").append(query)
			.append(", Parameters: ").append(parameters);
		return sb.toString();
	}
	
	public SingleSqlQuery build() {
		return new SingleSqlQuery(query, parameters);
	}
}
