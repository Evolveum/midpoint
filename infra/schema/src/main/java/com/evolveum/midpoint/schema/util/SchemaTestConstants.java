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
package com.evolveum.midpoint.schema.util;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Constants for use in tests. DO NOT USE IN "MAIN" CODE. This is placed in "main" just for convenience, so the
 * tests in other components can see it.
 *
 * @author Radovan Semancik
 */
public class SchemaTestConstants {

	public static final String NS_ICFC_LDAP = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.identityconnectors.ldap/org.identityconnectors.ldap.LdapConnector";
	public static final String NS_ICFC = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-schema-3";
	public static final String NS_ICFS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";

	public static final QName ICFC_CONFIGURATION_PROPERTIES = new QName(NS_ICFC, "configurationProperties");
	public static final QName ICFC_CONFIGURATION_PROPERTIES_TYPE = new QName(NS_ICFC, "ConfigurationPropertiesType");
	public static final String ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME = "AccountObjectClass";
	public static final String ICF_GROUP_OBJECT_CLASS_LOCAL_NAME = "GroupObjectClass";

	public static final QName ICFS_UID = new QName(NS_ICFS, "uid");
	public static final ItemPath ICFS_UID_PATH = new ItemPath(ShadowType.F_ATTRIBUTES, ICFS_UID);
	public static final QName ICFS_NAME = new QName(NS_ICFS, "name");
	public static final ItemPath ICFS_NAME_PATH = new ItemPath(ShadowType.F_ATTRIBUTES, ICFS_NAME);

	public static final String ACCOUNT_OBJECT_CLASS_LOCAL_NAME = "AccountObjectClass";
	public static final String GROUP_OBJECT_CLASS_LOCAL_NAME = "GroupObjectClass";

	// Extension schema loaded at runtime from the schema/src/test/resource/schema dir
	public static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
	public static final QName EXTENSION_LOCATIONS_ELEMENT = new QName(NS_EXTENSION, "locations");
	public static final QName EXTENSION_LOCATIONS_TYPE = new QName(NS_EXTENSION, "LocationsType");
	public static final QName EXTENSION_STRING_TYPE_ELEMENT = new QName(NS_EXTENSION, "stringType");
	public static final QName EXTENSION_INT_TYPE_ELEMENT = new QName(NS_EXTENSION, "intType");
	public static final QName EXTENSION_INTEGER_TYPE_ELEMENT = new QName(NS_EXTENSION, "integerType");
	public static final QName EXTENSION_DECIMAL_TYPE_ELEMENT = new QName(NS_EXTENSION, "decimalType");
	public static final QName EXTENSION_DOUBLE_TYPE_ELEMENT = new QName(NS_EXTENSION, "doubleType");
	public static final QName EXTENSION_LONG_TYPE_ELEMENT = new QName(NS_EXTENSION, "longType");
	public static final QName EXTENSION_DATE_TYPE_ELEMENT = new QName(NS_EXTENSION, "dateType");
	public static final QName EXTENSION_SHIP_ELEMENT = new QName(NS_EXTENSION, "ship");
}
