/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.File;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class TestConstants {

    public static final String COMMON_DIR_PATH = "src/test/resources/common";
    public static final File COMMON_DIR = new File(COMMON_DIR_PATH);

    public static final String NS_EXTENSION = SchemaConstants.NS_MIDPOINT_TEST + "/extension";
    public static final String NS_FOO = "http://www.example.com/foo";

    public static final ItemName EXTENSION_STRING_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "stringType");
    public static final ItemName EXTENSION_SINGLE_STRING_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "singleStringType");
    public static final ItemName EXTENSION_INT_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "intType");
    public static final ItemName EXTENSION_IGNORED_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "ignoredType");
    public static final ItemName EXTENSION_USER_REF_ELEMENT = new ItemName(NS_EXTENSION, "userRef");

    public static final QName USER_EXTENSION_TYPE_NAME = new QName(NS_EXTENSION, "UserExtensionType");

    public static final String USER_FILE_BASENAME = "user-jack";
    public static final String USER_JACK_POLYSTRING_FILE_BASENAME = "user-jack-polystring";
    public static final File USER_FILE = new File(COMMON_DIR, "user-jack.xml");

    public static final Long USER_ASSIGNMENT_1_ID = 111L;

    public static final String USER_ACCOUNT_REF_1_OID = "2f9b9299-6f45-498f-aaaa-000000001111";
    public static final String USER_ACCOUNT_REF_2_OID = "2f9b9299-6f45-498f-aaaa-000000002222";
    public static final String USER_ACCOUNT_REF_3_OID = "2f9b9299-6f45-498f-aaaa-000000003333";

    public static final String RESOURCE_FILE_BASENAME = "resource-opendj";
    public static final String RESOURCE_FILE_SIMPLE_BASENAME = "resource-opendj-simple";
    public static final File RESOURCE_FILE = new File(TestConstants.COMMON_DIR, "xml/ns/resource-opendj.xml");
    public static final String RESOURCE_FILE_EXPRESSION_BASENAME = "resource-expression";
    public static final String RESOURCE_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
    public static final String RESOURCE_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

    public static final File ROLE_FILE = new File(TestConstants.COMMON_DIR, "role.xml");

    public static final String SHADOW_FILE_BASENAME = "shadow-hbarbossa";
    public static final String SHADOW_FILE_BASENAME_LEGACY = "shadow-hbarbossa-legacy";
    public static final String CERTIFICATION_CASE_FILE_BASENAME = "certification-case-1";

    public static final String METAROLE_FILE_BASENAME = "metarole";
    public static final String OBJECTS_FILE_BASENAME = "objects";
    public static final String OBJECTS_WRONG_FILE_BASENAME = "objects-wrong";
}
