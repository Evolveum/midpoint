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
package com.evolveum.midpoint.report.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

public class ReportConstants {

	public static final String NS_EXTENSION = SchemaConstants.NS_REPORT + "/extension-" + SchemaConstants.SCHEMA_MAJOR_VERSION;

	public static final ItemName REPORT_PARAMS_PROPERTY_NAME = new ItemName(ReportConstants.NS_EXTENSION, "reportParam");
	public static final ItemName REPORT_OUTPUT_OID_PROPERTY_NAME = new ItemName(ReportConstants.NS_EXTENSION, "reportOutputOid");


}
