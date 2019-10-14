/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.util;

import java.io.File;

/**
 * Created by Kate Honchar
 */
public class ConstantsUtil {

    //left menu items keys
    public static String ADMINISTRATION_MENU_ITEMS_SECTION_KEY = "PageAdmin.menu.mainNavigation";
    public static String MENU_TOP_CASES = "PageAdmin.menu.top.cases";
    public static String MENU_ALL_CASES = "PageAdmin.menu.top.cases.listAll";


    //left menu items label texts
    public static final String MENU_MY_CASES_MENU_ITEM_LABEL_TEXT = "My cases";
    public static final String MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT = "All manual cases";
    public static final String MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT = "All requests";
    public static final String MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT = "All approvals";

    //object attribute values
    public static final String CASE_CREATION_TEST_USER_NAME = "caseCreationTestUser";
    public static final String CASE_CREATION_TEST_ROLE_NAME = "Role with admin approver";
    public static final String CASE_CREATION_TEST_CASE_NAME = "Approving and executing change of user \"caseCreationTestUser\"";

    //files to import
    public static final File ROLE_WITH_ADMIN_APPROVER_XML = new File("./src/test/resources/role-with-admin-approver.xml");

    //add new assignment popup - tabs names
    public static final String ASSIGNMENT_TYPE_SELECTOR_ROLE = "Role";
    public static final String ASSIGNMENT_TYPE_SELECTOR_ORG_TREE = "Org. tree view";
    public static final String ASSIGNMENT_TYPE_SELECTOR_ORG = "Org";
    public static final String ASSIGNMENT_TYPE_SELECTOR_SERVICE = "Service";
    public static final String ASSIGNMENT_TYPE_SELECTOR_RESOURCE = "Resource";

    //styles
    public static final String OBJECT_USER_BOX_COLOR = "object-user-box";
}
