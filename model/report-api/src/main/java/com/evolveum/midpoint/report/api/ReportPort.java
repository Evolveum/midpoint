package com.evolveum.midpoint.report.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;


public interface ReportPort {
	String CLASS_NAME_WITH_DOT = ReportPort.class.getName() + ".";
	String PROCESS_REPORT = CLASS_NAME_WITH_DOT + "processReport";


	 public static final QName PARSE_QUERY_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "parseQueryResponse");
	 public static final QName PROCESS_REPORT_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "processReportResponse");
	 public static final QName EVALUATE_SCRIPT_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "evaluateScriptResponse");
	 public static final QName EVALUATE_AUDIT_SCRIPT_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "evaluateAuditScriptResponse");
	 public static final QName SEARCH_OBJECTS_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "searchObjectsResponse");
	 public static final QName GET_FIELD_VALUE_RESPONSE = new QName(SchemaConstants.NS_REPORT_WS, "getFieldValueResponse");
}
