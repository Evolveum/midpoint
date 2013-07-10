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
package com.evolveum.midpoint.model.importer;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 *
 */
public class ImportConstants {
	
	/**
	 * Prefix for all import-related URIs.
	 */
	public static final String IMPORT_URI_PREFIX = "http://midpoint.evolveum.com/model/import";
    public static final String IMPORT_URI_TASK_PREFIX = "http://midpoint.evolveum.com/xml/ns/public/model/import";
	
	/**
	 * Schema URI for import extensions.
	 */
	public static final String IMPORT_EXTENSION_SCHEMA = IMPORT_URI_PREFIX + "/extension-2";

	/**
	 * Extension property that specifies object class to import.
	 */
	public static final QName OBJECTCLASS_PROPERTY_NAME = new QName(ImportConstants.IMPORT_EXTENSION_SCHEMA, "objectclass");

	public static final QName KIND_PROPERTY_NAME = new QName(ImportConstants.IMPORT_EXTENSION_SCHEMA, "kind");
	
	public static final QName INTENT_PROPERTY_NAME = new QName(ImportConstants.IMPORT_EXTENSION_SCHEMA, "intent");
	
	public static final QName FILENAME_PROPERTY_NAME = new QName(ImportConstants.IMPORT_EXTENSION_SCHEMA, "filename");

    public static final QName ENDPOINT_PROPERTY_NAME = new QName(ImportConstants.IMPORT_EXTENSION_SCHEMA, "endpoint");

    public static final QName ROUTE_PROPERTY_NAME = new QName(ImportConstants.IMPORT_EXTENSION_SCHEMA, "route");

	
}
