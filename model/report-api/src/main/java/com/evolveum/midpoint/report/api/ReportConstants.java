package com.evolveum.midpoint.report.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

public class ReportConstants {

	public static final String NS_EXTENSION = SchemaConstants.NS_REPORT + "/extension-3";

	public static final ItemName REPORT_PARAMS_PROPERTY_NAME = new ItemName(ReportConstants.NS_EXTENSION, "reportParam");
	public static final ItemName REPORT_OUTPUT_OID_PROPERTY_NAME = new ItemName(ReportConstants.NS_EXTENSION, "reportOutputOid");


}
