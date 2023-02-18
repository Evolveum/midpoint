/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.api;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

public class ReportConstants {

    public static final ItemName REPORT_PARAMS_PROPERTY_NAME = new ItemName(SchemaConstants.NS_REPORT_EXTENSION, "reportParam");
    public static final ItemName REPORT_OUTPUT_OID_PROPERTY_NAME = new ItemName(SchemaConstants.NS_REPORT_EXTENSION, "reportOutputOid");
    public static final ItemName REPORT_DATA_PROPERTY_NAME = new ItemName(SchemaConstants.NS_REPORT_EXTENSION, "reportDataParam");

}
