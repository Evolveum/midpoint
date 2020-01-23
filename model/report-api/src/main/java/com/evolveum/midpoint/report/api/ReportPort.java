/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

public interface ReportPort {
    String CLASS_NAME_WITH_DOT = ReportPort.class.getName() + ".";
    String PROCESS_REPORT = CLASS_NAME_WITH_DOT + "processReport";

    QName PARSE_QUERY_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "parseQueryResponse");
    QName PROCESS_REPORT_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "processReportResponse");
    QName EVALUATE_SCRIPT_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "evaluateScriptResponse");
    QName EVALUATE_AUDIT_SCRIPT_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "evaluateAuditScriptResponse");
    QName SEARCH_OBJECTS_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "searchObjectsResponse");
    QName GET_FIELD_VALUE_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "getFieldValueResponse");
}
