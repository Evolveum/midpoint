/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
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
