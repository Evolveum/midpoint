/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.sync;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.importer.ImportConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * @author semancik
 *
 */
public class SynchronizationConstants {
	
	public static final String NS_SYNCHRONIZATION_PREFIX = SchemaConstants.NS_MODEL +"/synchronization";
	public static final String NS_SYNCHRONIZATION_TASK_PREFIX = NS_SYNCHRONIZATION_PREFIX + "/task";
	public static final String NS_SYNC_EXTENSION_SCHEMA = NS_SYNCHRONIZATION_PREFIX + "/extension-2";
	public static final QName FRESHENESS_INTERVAL_PROPERTY_NAME = new QName(NS_SYNC_EXTENSION_SCHEMA, "freshnessInterval");
	public static final QName DRY_RUN = new QName(NS_SYNC_EXTENSION_SCHEMA, "dryRun");
	public static final QName LAST_RECOMPUTE_TIMESTAMP_PROPERTY_NAME = new QName(NS_SYNC_EXTENSION_SCHEMA, "lastRecomputeTimestamp");

}
