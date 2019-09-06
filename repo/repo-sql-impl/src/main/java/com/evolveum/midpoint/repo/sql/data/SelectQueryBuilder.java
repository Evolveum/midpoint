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
	private String query;
	private Map<Integer, Object> parameters = new HashMap<Integer, Object>();
	private Integer firstResult = null;
	private Integer maxResult = null;
	
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
	
	private void addPaging(int firstResult, int maxResult) {
		Validate.notNull(database, "Database is null");
		StringBuilder sb = new StringBuilder();
		String queryWithoutStringValue = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
		if (Database.SQLSERVER.equals(database)) {
			if (queryWithoutStringValue.toLowerCase().contains(" offset ") || queryWithoutStringValue.contains(" fetch ")) {
				throw new IllegalArgumentException("query already contains offset or fetch");
			}
			sb.append(query)
					.append(" OFFSET ").append(firstResult).append(" ROWS")
					.append(" FETCH NEXT ").append(maxResult).append(" ROWS ONLY ");
		} else if (Database.ORACLE.equals(database)) {
			sb.append("SELECT * FROM" +
					"( "+ 
						"SELECT a.*, rownum r__ FROM (")
							.append(query)
						.append(") a WHERE rownum < ").append(firstResult + maxResult+1)
					.append(") WHERE r__ >= ").append(firstResult+1).append(" ");
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
		Validate.notNull(database, "Database is null");
		
		StringBuilder sb = new StringBuilder();
		String queryWithoutStringValue = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
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
		}  else {
			throw new IllegalArgumentException("Unsupported type of database: " + database);
		}
		query = sb.toString();
	}
	
	private void addMaxResult(int maxResult) {
		Validate.notNull(database, "Database is null");
		StringBuilder sb = new StringBuilder();
		String queryWithoutStringValue = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
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
