/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;
import java.io.File;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_MIDPOINT_TEST_PREFIX;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.util.QNameUtil.qNameToUri;

public class MidPointTestConstants {

    public static final String TEST_RESOURCES_PATH = "src/test/resources";
    public static final File TEST_RESOURCES_DIR = new File(TEST_RESOURCES_PATH);
    public static final File OBJECTS_DIR = new File(TEST_RESOURCES_DIR, "objects");
    public static final String TARGET_DIR_PATH = "target";

    // copied from TestProtector - unfortunately these values are needed both in prism and in other modules
    public static final String KEYSTORE_PATH = "src/test/resources/keystore.jceks";
    public static final String KEYSTORE_PASSWORD = "changeit";

    // LDAP-related constants (is this a good place?)
    public static final ItemName QNAME_SN = new ItemName(NS_RI, "sn");
    public static final ItemPath PATH_SN = ItemPath.create(ShadowType.F_ATTRIBUTES, QNAME_SN);
    public static final ItemName QNAME_DN = new ItemName(NS_RI, "dn");
    public static final ItemPath PATH_DN = ItemPath.create(ShadowType.F_ATTRIBUTES, QNAME_DN);
    public static final ItemName QNAME_CN = new ItemName(NS_RI, "cn");
    public static final ItemPath PATH_CN = ItemPath.create(ShadowType.F_ATTRIBUTES, QNAME_CN);
    public static final ItemName QNAME_GIVEN_NAME = new ItemName(NS_RI, "givenName");
    public static final ItemPath PATH_GIVEN_NAME = ItemPath.create(ShadowType.F_ATTRIBUTES, QNAME_GIVEN_NAME);
    public static final ItemName QNAME_UID = new ItemName(NS_RI, "uid");
    public static final ItemPath PATH_UID = ItemPath.create(ShadowType.F_ATTRIBUTES, QNAME_UID);
    public static final ItemName QNAME_EMPLOYEE_NUMBER = new ItemName(NS_RI, "employeeNumber");
    public static final ItemPath PATH_EMPLOYEE_NUMBER = ItemPath.create(ShadowType.F_ATTRIBUTES, QNAME_EMPLOYEE_NUMBER);
    public static final ItemName QNAME_ENTRY_UUID = new ItemName(NS_RI, "entryUUID");
    public static final ItemPath PATH_ENTRY_UUID = ItemPath.create(ShadowType.F_ATTRIBUTES, QNAME_ENTRY_UUID);
    public static final ItemName QNAME_CAR_LICENSE = new ItemName(MidPointConstants.NS_RI, "carLicense");
    public static final ItemName QNAME_EMPLOYEE_TYPE = new ItemName(MidPointConstants.NS_RI, "employeeType");

    public static final String TEST_POLICY_SITUATION_LEGACY = qNameToUri(new QName(NS_MIDPOINT_TEST_PREFIX, "legacy"));
    public static final String TEST_POLICY_SITUATION_ILLEGAL = qNameToUri(new QName(NS_MIDPOINT_TEST_PREFIX, "illegal"));
    public static final String TEST_POLICY_SITUATION_PENDING = qNameToUri(new QName(NS_MIDPOINT_TEST_PREFIX, "pending"));
}
