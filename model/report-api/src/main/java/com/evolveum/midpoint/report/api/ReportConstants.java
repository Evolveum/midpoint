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

    public static final String NS_EXTENSION = SchemaConstants.NS_REPORT + "/extension-3";

    public static final ItemName REPORT_PARAMS_PROPERTY_NAME = new ItemName(ReportConstants.NS_EXTENSION, "reportParam");
    public static final ItemName REPORT_OUTPUT_OID_PROPERTY_NAME = new ItemName(ReportConstants.NS_EXTENSION, "reportOutputOid");


}
