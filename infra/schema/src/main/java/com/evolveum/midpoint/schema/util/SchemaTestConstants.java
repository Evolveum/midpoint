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
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ShadowType;

/**
 * Constants for use in tests. DO NOT USE IN "MAIN" CODE. This is placed in "main" just for convenience, so the
 * tests in other components can see it.
 *
 * @author Radovan Semancik
 */
public class SchemaTestConstants {

	public static final String NS_ICFC_LDAP = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.identityconnectors.ldap/org.identityconnectors.ldap.LdapConnector";

	public static final ItemName ICFC_CONFIGURATION_PROPERTIES = SchemaConstants.ICF_CONFIGURATION_PROPERTIES;
	public static final ItemName ICFC_CONFIGURATION_PROPERTIES_TYPE = new ItemName(SchemaConstants.NS_ICF_CONFIGURATION, "ConfigurationPropertiesType");
	public static final String ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME = "AccountObjectClass";
	public static final String ICF_GROUP_OBJECT_CLASS_LOCAL_NAME = "GroupObjectClass";

	public static final ItemName ICFS_UID = new ItemName(SchemaConstants.NS_ICF_SCHEMA, "uid");
	public static final ItemPath ICFS_UID_PATH_PARTS = ItemPath.create(ShadowType.F_ATTRIBUTES, ICFS_UID);
	public static final ItemName ICFS_NAME = new ItemName(SchemaConstants.NS_ICF_SCHEMA, "name");
	public static final ItemPath ICFS_NAME_PATH_PARTS = ItemPath.create(ShadowType.F_ATTRIBUTES, ICFS_NAME);

	public static final String ACCOUNT_OBJECT_CLASS_LOCAL_NAME = "AccountObjectClass";
	public static final String GROUP_OBJECT_CLASS_LOCAL_NAME = "GroupObjectClass";

	// Extension schema loaded at runtime from the schema/src/test/resource/schema dir
	public static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
	public static final ItemName EXTENSION_LOCATIONS_ELEMENT = new ItemName(NS_EXTENSION, "locations");
	public static final ItemName EXTENSION_LOCATIONS_TYPE = new ItemName(NS_EXTENSION, "LocationsType");
	public static final ItemName EXTENSION_STRING_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "stringType");
	public static final ItemName EXTENSION_INT_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "intType");
	public static final ItemName EXTENSION_INTEGER_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "integerType");
	public static final ItemName EXTENSION_DECIMAL_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "decimalType");
	public static final ItemName EXTENSION_DOUBLE_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "doubleType");
	public static final ItemName EXTENSION_LONG_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "longType");
	public static final ItemName EXTENSION_DATE_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "dateType");
	public static final ItemName EXTENSION_SHIP_ELEMENT = new ItemName(NS_EXTENSION, "ship");
}
