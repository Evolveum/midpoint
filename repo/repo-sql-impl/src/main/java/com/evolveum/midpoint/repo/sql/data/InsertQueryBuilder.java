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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * @author skublik
 */
public class InsertQueryBuilder {
	
	private StringBuilder sbQuery;
	private StringBuilder sbValues;
	private int index = 1;
	private Map<Integer, Object> parameters = new HashMap<Integer, Object>();
	private List<Integer> primaryKey = new ArrayList<Integer>();
	
	public InsertQueryBuilder(String tableName) {
		if(StringUtils.isBlank(tableName)) {
			throw new IllegalArgumentException("Name of table is empty");
		}
		sbQuery = new StringBuilder("INSERT INTO ");
    	sbQuery.append(tableName).append(" (");
    	sbValues = new StringBuilder(" VALUES (");
	}
	
	public  void addNullParameter(String nameOfparameter) {
		addParameter(nameOfparameter, null);
    }
	
	public  void addParameter(String nameOfparameter, Object value) {
		addParameter(nameOfparameter, value, false);
	}
	
	public  void addParameter(String nameOfparameter, Object value, boolean isPrimaryKey) {
    	sbQuery.append(parameters.isEmpty() ? "" : ", ").append(nameOfparameter);
    	sbValues.append(parameters.isEmpty() ? "" : ", ").append("?");
    	parameters.put(index, value);
    	if(isPrimaryKey) {
    		primaryKey.add(index);
    	}
    	index++;
    }
    
    public SingleSqlQuery build() {
    	String query = sbQuery.append(") ").append(sbValues.toString()).append(") ").toString();
    	SingleSqlQuery sqlQuery = new SingleSqlQuery(query, parameters);
    	sqlQuery.setPrimaryKeys(primaryKey);
    	return sqlQuery;
    }
}
