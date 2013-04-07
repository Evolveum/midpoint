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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.util;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

/**
 * Constants for use in tests. DO NOT USE IN "MAIN" CODE. This is placed in "main" just for conveniece, so the
 * tests in other components can see it.
 * 
 * @author Radovan Semancik
 */
public class SchemaTestConstants {

	public static final String NS_ICFC_LDAP = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.identityconnectors.ldap/org.identityconnectors.ldap.LdapConnector";
	public static final String NS_ICFC = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-schema-2";
	public static final String NS_ICFS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-2";
	
	public static final QName ICFC_CONFIGURATION_PROPERTIES = new QName(NS_ICFC, "configurationProperties");
	public static final QName ICFC_CONFIGURATION_PROPERTIES_TYPE = new QName(NS_ICFC, "ConfigurationPropertiesType");
	public static final String ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME = "AccountObjectClass";
	public static final String ICF_GROUP_OBJECT_CLASS_LOCAL_NAME = "GroupObjectClass";

	public static final QName ICFS_UID = new QName(NS_ICFS, "uid");
	public static final ItemPath ICFS_UID_PATH = new ItemPath(ShadowType.F_ATTRIBUTES, ICFS_UID);
	public static final QName ICFS_NAME = new QName(NS_ICFS, "name");
	public static final ItemPath ICFS_NAME_PATH = new ItemPath(ShadowType.F_ATTRIBUTES, ICFS_NAME);
	
	// Extension schema loaded at runtime from the schema/src/test/resource/schema dir
	public static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
	public static final QName EXTENSION_LOCATIONS_ELEMENT = new QName(NS_EXTENSION, "locations");
	public static final QName EXTENSION_LOCATIONS_TYPE = new QName(NS_EXTENSION, "LocationsType");
	public static final QName EXTENSION_STRING_TYPE_ELEMENT = new QName(NS_EXTENSION, "stringType");
	public static final QName EXTENSION_INT_TYPE_ELEMENT = new QName(NS_EXTENSION, "intType");
	public static final QName EXTENSION_DOUBLE_TYPE_ELEMENT = new QName(NS_EXTENSION, "doubleType");
	public static final QName EXTENSION_LONG_TYPE_ELEMENT = new QName(NS_EXTENSION, "longType");
	public static final QName EXTENSION_DATE_TYPE_ELEMENT = new QName(NS_EXTENSION, "dateType");
}
