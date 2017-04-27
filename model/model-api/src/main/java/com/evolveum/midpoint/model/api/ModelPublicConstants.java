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
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * Model constants referenced from the outside.
 * (TODO reconsider with regards to SchemaConstants)
 *
 * @author mederly
 */
public class ModelPublicConstants {
	
	public static final String NS_SYNCHRONIZATION_PREFIX = SchemaConstants.NS_MODEL +"/synchronization";
	public static final String NS_SYNCHRONIZATION_TASK_PREFIX = NS_SYNCHRONIZATION_PREFIX + "/task";

	public static final String DELETE_TASK_HANDLER_URI = NS_SYNCHRONIZATION_TASK_PREFIX + "/delete/handler-3";	// TODO why "synchronization"?
	public static final String REINDEX_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/reindex/handler-3";
	public static final String AUDIT_REINDEX_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/auditReindex/handler-3";
	public static final String CLEANUP_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/cleanup/handler-3";
	public static final String SHADOW_INTEGRITY_CHECK_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/shadow-integrity-check/handler-3";
	public static final String FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI = NS_SYNCHRONIZATION_TASK_PREFIX + "/focus-validation-scanner/handler-3"; // TODO why synchronization?
	public static final String TRIGGER_SCANNER_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/trigger/scanner/handler-3";
	public static final String SHADOW_REFRESH_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/shadowRefresh/handler-3";

}
