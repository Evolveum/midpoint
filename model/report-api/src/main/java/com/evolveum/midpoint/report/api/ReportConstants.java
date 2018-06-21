package com.evolveum.midpoint.report.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

public class ReportConstants {

	public static final String NS_EXTENSION = SchemaConstants.NS_REPORT + "/extension-3";

	public static final QName REPORT_PARAMS_PROPERTY_NAME = new QName(ReportConstants.NS_EXTENSION, "reportParam");
	public static final QName REPORT_OUTPUT_OID_PROPERTY_NAME = new QName(ReportConstants.NS_EXTENSION, "reportOutputOid");


}
