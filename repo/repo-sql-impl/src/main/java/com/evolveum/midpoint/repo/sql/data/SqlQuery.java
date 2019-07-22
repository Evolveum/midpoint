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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventStage;
import com.evolveum.midpoint.repo.sql.data.audit.RAuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * @author skublik
 */
public abstract class SqlQuery {
	
	private List<Integer> primaryKeys = new ArrayList<Integer>();
	private Database database = Database.H2;

	public abstract PreparedStatement createPreparedStatement(Connection con) throws SQLException;
	
	public abstract void execute(Connection connection) throws SQLException;
	
	protected Object toRepoType(Object value) {
		if(value == null){
			return value;
		}
		
        if (XMLGregorianCalendar.class.isAssignableFrom(value.getClass())) {
        	Date date = MiscUtil.asDate((XMLGregorianCalendar) value);
        	return new Timestamp(date.getTime());
        	
        } else if (value instanceof Date) {
        	return new Timestamp(((Date)value).getTime());
        	
        } else if (value instanceof AuditEventType) {
        	return RAuditEventType.toRepo((AuditEventType) value).ordinal();
        	
        } else if (value instanceof AuditEventTypeType) {
        	return RAuditEventType.toRepo(
        			AuditEventType.toAuditEventType((AuditEventTypeType) value)).ordinal();
        	
        } else if (value instanceof AuditEventStage) {
        	return RAuditEventStage.toRepo((AuditEventStage) value).ordinal();
        	
        } else if (value instanceof AuditEventStageType) {
        	return RAuditEventStage.toRepo(
        			AuditEventStage.toAuditEventStage((AuditEventStageType) value)).ordinal();
        	
        } else if (value instanceof OperationResultStatusType) {
        	return ROperationResultStatus.toRepo((OperationResultStatusType) value).ordinal();
        	
        } else if (value instanceof OperationResultStatus) {
        	return ROperationResultStatus.toRepo(
        			OperationResultStatus.createStatusType((OperationResultStatus) value)).ordinal();
        	
        } else if (value.getClass().isEnum()) {
        	return ((Enum) value).ordinal();
        }

        return value;
    }
	
	protected void addParametersToStatment(Map<Integer, Object> parameters, PreparedStatement stmt) throws SQLException {
		for(int index : parameters.keySet()) {
			Object value = toRepoType(parameters.get(index));
			if (value == null) {
				stmt.setObject(index, value, Types.NULL);
			}
			if (value instanceof String) {
				stmt.setString(index, (String) value);
			} else if(value instanceof Integer) {
				stmt.setInt(index, (int) value);
			} else if (value instanceof Timestamp) {
				stmt.setTimestamp(index, (Timestamp) value);
			} else if(value instanceof Long) {
				if(database.equals(Database.ORACLE)) {
					stmt.setBigDecimal(index, BigDecimal.valueOf((long) value));
				} else {
					stmt.setLong(index, (long) value);
				}
			} else if(value instanceof byte[]) {
				stmt.setBytes(index, (byte[]) value);
			} else {
				stmt.setObject(index, value);
			}
		}
	}
	
	public void setDatabase(Database database) {
		this.database = database;
	}
	
	public void setPrimaryKeys(List<Integer> primaryKeys) {
		this.primaryKeys = primaryKeys;
	}
	
	public List<Integer> getPrimaryKeys() {
		return primaryKeys;
	}
}
