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

package com.evolveum.midpoint.repo.sql.util;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * @author skublik
 */
public class PreparedStatementBuilder {
	
	private Database database;
	String query;
	Map<Integer, Object> parameters = new HashMap<Integer, Object>();
	
	public PreparedStatementBuilder(Database database, String query) {
		Validate.notNull(database, "Database is null");
		if(StringUtils.isBlank(query)) {
			throw new IllegalArgumentException("Query is empty");
		}
		this.database = database;
		this.query = query;
	}
	
	public void addPaging(String firstResultName, String maxResultName) {
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
	
	public PreparedStatement build(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(query);
		for(int index : parameters.keySet()) {
			Object value = parameters.get(index);
			if(value instanceof String) {
				stmt.setString(index, (String) value);
			} else if(value instanceof Integer) {
				stmt.setInt(index, (int) value);
			} else if (value instanceof Timestamp) {
				stmt.setTimestamp(index, (Timestamp) value);
			} else {
				stmt.setObject(index, value);
			}
		}
		return stmt;
	}
	
	public void addParameter(String name, Object value) {
		String workQuery = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
		addParameter(workQuery, name, value);
	}
	
	private void addParameter(String workQuery, String name, Object value) {
		name = ":" + name;
		if(!(workQuery.contains(name))) {
			throw new IllegalArgumentException("query don't contains value name " + name);
		}
		while(workQuery.split(name).length > 1) {
			String partQuery = workQuery.split(name)[0];
			int index = StringUtils.countMatches(partQuery, ":") + 1;
			parameters.put(index, toRepoType(value));
			workQuery = workQuery.split(name)[1];
		}
		query = query.replace(name, "?");
	}
	
	public void addParameters (Map<String, Object> parameters) {
		String workQuery = query.replaceAll("\".*?\"|\'.*?\'|`.*`", "");
		for (String name : parameters.keySet()) {
			addParameter(workQuery, name, parameters.get(name));
		}
	}
	
	private Object toRepoType(Object value) {
        if (XMLGregorianCalendar.class.isAssignableFrom(value.getClass())) {
        	Date date = MiscUtil.asDate((XMLGregorianCalendar) value);
        	return new Timestamp(date.getTime());
//            return MiscUtil.asDate((XMLGregorianCalendar) value);
        } else if (value instanceof AuditEventTypeType) {
        	return ((AuditEventTypeType) value).ordinal();
//            return RAuditEventType.toRepo((AuditEventType) value);
        } else if (value instanceof AuditEventStageType) {
        	return ((AuditEventStageType) value).ordinal();
//            return RAuditEventStage.toRepo((AuditEventStage) value);
        } else if (value instanceof OperationResultStatusType) {
        	return ((OperationResultStatusType) value).ordinal();
//            return ROperationResultStatus.toRepo((OperationResultStatusType) value);
        }

        return value;
    }
	
	@Override
	public String toString() {
		return query + "\n" + parameters;
	}
}
