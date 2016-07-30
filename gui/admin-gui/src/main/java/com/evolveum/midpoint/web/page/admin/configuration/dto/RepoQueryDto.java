/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * @author mederly
 */
public class RepoQueryDto implements Serializable {

    public static final String F_OBJECT_TYPE = "objectType";
    public static final String F_MIDPOINT_QUERY = "midPointQuery";
    public static final String F_HIBERNATE_QUERY = "hibernateQuery";
    public static final String F_HIBERNATE_PARAMETERS = "hibernateParameters";
    public static final String F_QUERY_RESULT_TEXT = "queryResultText";
    public static final String F_QUERY_RESULT_OBJECT = "queryResultObject";

	private static final String EMPTY_RESULT = null;

	private QName objectType;
    private String midPointQuery = "";
    private String hibernateQuery = "";
    private String hibernateParameters = "";
    private String queryResultText = EMPTY_RESULT;
    private Object queryResultObject = null;

	public QName getObjectType() {
		return objectType;
	}

	public void setObjectType(QName objectType) {
		this.objectType = objectType;
	}

	public String getMidPointQuery() {
		return midPointQuery;
	}

	public void setMidPointQuery(String midPointQuery) {
		this.midPointQuery = midPointQuery;
	}

	public String getHibernateQuery() {
        return hibernateQuery;
    }

    public void setHibernateQuery(String hibernateQuery) {
        this.hibernateQuery = hibernateQuery;
    }

	public String getHibernateParameters() {
		return hibernateParameters;
	}

	public void setHibernateParameters(String hibernateParameters) {
		this.hibernateParameters = hibernateParameters;
	}

	public String getQueryResultText() {
        return queryResultText;
    }

    public void setQueryResultText(String queryResultText) {
        this.queryResultText = queryResultText;
    }

	public Object getQueryResultObject() {
		return queryResultObject;
	}

	public void setQueryResultObject(Object queryResultObject) {
		this.queryResultObject = queryResultObject;
	}

	public void resetQueryResultText() {
		setQueryResultText(EMPTY_RESULT);
	}
}
