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
		this.query = query;
	}
	
	public SelectQueryBuilder(String query) {
		if(StringUtils.isBlank(query)) {
			throw new IllegalArgumentException("Query is empty");
		}
		this.query = query;
	}
	
	public void addPaging(String firstResultName, String maxResultName) {
		Validate.notNull(database, "Database is null");
		if(StringUtils.isBlank(firstResultName)) {
			throw new IllegalArgumentException("Name of firstResult value is empty");
		}
		if(StringUtils.isBlank(maxResultName)) {
			throw new IllegalArgumentException("Name of maxResult value is empty");
		}
		StringBuilder sb = new StringBuilder(query);
		String queryWithoutStringValue = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
		if(Database.SQLSERVER.equals(database) || Database.ORACLE.equals(database)) {
			if(queryWithoutStringValue.toLowerCase().contains(" offset ")
					|| queryWithoutStringValue.contains(" fetch ")) {
				throw new IllegalArgumentException("query already contains offset or fetch");
			}
			sb.append(" OFFSET :").append(firstResultName).append(" ROWS FETCH NEXT :")
			.append(maxResultName).append(" ROWS ONLY ");
		} else if(Database.H2.equals(database) || Database.MARIADB.equals(database)
				|| Database.MYSQL.equals(database) || Database.POSTGRESQL.equals(database)) {
			if(queryWithoutStringValue.toLowerCase().contains(" limit ")
					|| queryWithoutStringValue.contains(" offset ")) {
				throw new IllegalArgumentException("query already contains offset or fetch");
			}
			sb.append(" LIMIT :").append(maxResultName).append(" OFFSET :")
			.append(firstResultName).append(" ");
		} else {
			throw new IllegalArgumentException("Unsoported type of database: " + database);
		}
		query = sb.toString();
	}
	
	public void addFirstResult(String firstResultName) {
		Validate.notNull(database, "Database is null");
		if(StringUtils.isBlank(firstResultName)) {
			throw new IllegalArgumentException("Name of firstResult value is empty");
		}
		StringBuilder sb = new StringBuilder(query);
		String queryWithoutStringValue = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
		if(Database.SQLSERVER.equals(database) || Database.ORACLE.equals(database)) {
			if(queryWithoutStringValue.toLowerCase().contains(" offset ")) {
				throw new IllegalArgumentException("query already contains offset");
			}
			sb.append(" OFFSET :").append(firstResultName).append(" ROWS ");
		} else if(Database.H2.equals(database) || Database.MARIADB.equals(database)
				|| Database.MYSQL.equals(database) || Database.POSTGRESQL.equals(database)) {
			if(queryWithoutStringValue.contains(" offset ")) {
				throw new IllegalArgumentException("query already contains offset");
			}
			sb.append(" OFFSET :").append(firstResultName).append(" ");
		} else {
			throw new IllegalArgumentException("Unsoported type of database: " + database);
		}
		query = sb.toString();
	}
	
	public void addMaxResult(String maxResultName) {
		Validate.notNull(database, "Database is null");
		if(StringUtils.isBlank(maxResultName)) {
			throw new IllegalArgumentException("Name of maxResult value is empty");
		}
		StringBuilder sb = new StringBuilder(query);
		String queryWithoutStringValue = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
		if(Database.SQLSERVER.equals(database) || Database.ORACLE.equals(database)) {
			if(queryWithoutStringValue.toLowerCase().contains(" fetch ")) {
				throw new IllegalArgumentException("query already contains fetch");
			}
			sb.append(" FETCH NEXT :").append(maxResultName).append(" ROWS ONLY ");
		} else if(Database.H2.equals(database) || Database.MARIADB.equals(database)
				|| Database.MYSQL.equals(database) || Database.POSTGRESQL.equals(database)) {
			if(queryWithoutStringValue.toLowerCase().contains(" limit ")) {
				throw new IllegalArgumentException("query already contains limit");
			}
			sb.append(" LIMIT :").append(maxResultName).append(" ");
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
			int index = StringUtils.countMatches(partQuery, ":") + 1;
			
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
		String workQuery = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
		for (String name : parameters.keySet()) {
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
