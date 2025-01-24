/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class RepoQueryDto implements Serializable {

    public static final String F_OBJECT_TYPE = "objectType";
    public static final String F_MIDPOINT_QUERY = "midPointQuery";
    public static final String F_HIBERNATE_QUERY = "hibernateQuery";
    public static final String F_HIBERNATE_PARAMETERS = "hibernateParameters";
    public static final String F_QUERY_RESULT_TEXT = "queryResultText";
    public static final String F_QUERY_RESULT_OBJECT = "queryResultObject";
    public static final String F_DISTINCT = "distinct";
    public static final String F_SCRIPT_ENABLED = "scriptEnabled";
    public static final String F_MIDPOINT_QUERY_SCRIPT = "midPointQueryScript";

    private static final String EMPTY_RESULT = null;

    private QName objectType;
    private String midPointQuery = "";
    private String hibernateQuery = "";
    private String hibernateParameters = "";
    private String queryResultText = EMPTY_RESULT;
    private Object queryResultObject = null;
    private boolean distinct;
    private boolean scriptEnabled;
    private int positionCursor = -1;

    private ExpressionType midPointQueryScript = new ExpressionType();

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

    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public ExpressionType getMidPointQueryScript() {
        return midPointQueryScript;
    }

    public void setMidPointQueryScript(ExpressionType midPointQueryScript) {
        this.midPointQueryScript = midPointQueryScript;
    }

    public boolean isScriptEnabled() {
        return scriptEnabled;
    }

    public void setScriptEnabled(boolean scriptEnabled) {
        this.scriptEnabled = scriptEnabled;
    }

    public int getPositionCursor() {
        return positionCursor;
    }

    public void setPositionCursor(int positionCursor) {
        this.positionCursor = positionCursor;
    }
}
