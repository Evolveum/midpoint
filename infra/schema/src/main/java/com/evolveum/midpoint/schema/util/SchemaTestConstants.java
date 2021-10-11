/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
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

    public static final ItemName ICFC_CONFIGURATION_PROPERTIES = new ItemName(NS_ICFC, "configurationProperties");
    public static final ItemName ICFC_CONFIGURATION_PROPERTIES_TYPE = new ItemName(NS_ICFC, "ConfigurationPropertiesType");
    public static final String ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME = "AccountObjectClass";
    public static final String ICF_GROUP_OBJECT_CLASS_LOCAL_NAME = "GroupObjectClass";

    public static final ItemName ICFS_UID = new ItemName(NS_ICFS, "uid");
    public static final ItemPath ICFS_UID_PATH_PARTS = ItemPath.create(ShadowType.F_ATTRIBUTES, ICFS_UID);
    public static final ItemName ICFS_NAME = new ItemName(NS_ICFS, "name");
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
