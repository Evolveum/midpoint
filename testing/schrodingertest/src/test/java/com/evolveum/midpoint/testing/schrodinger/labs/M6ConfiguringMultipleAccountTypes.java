/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Utils;
import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class M6ConfiguringMultipleAccountTypes extends AbstractLabTest {

    private static final Logger LOG = LoggerFactory.getLogger(M6ConfiguringMultipleAccountTypes.class);
    protected static final String LAB_OBJECTS_DIRECTORY = LAB_DIRECTORY + "M6/";

    private static final File CSV_1_RESOURCE_FILE_6_1 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-6-1.xml");
    private static final File CSV_2_RESOURCE_FILE_6_1 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-2-canteen-6-1.xml");
    private static final File CSV_3_RESOURCE_FILE_6_1 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap-6-1.xml");
    private static final File CSV1_TESTER_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-csv1-tester.xml");
    private static final File CSV3_ADMIN_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-csv3-admin.xml");
    private static final String CSV1_TESTER_ROLE_NAME = "CSV-1 Tester";
    private static final String CSV3_ADMIN_ROLE_NAME = "CSV-3 Admin";
    private static final File NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE = new File(LAB_OBJECTS_DIRECTORY + "valuepolicies/numeric-pin-first-nonzero-policy.xml");
    private static final File SECRET_I_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-i.xml");
    private static final File SECRET_II_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-ii.xml");
    private static final File INCOGNITO_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-incognito.xml");
    private static final File INTERNAL_EMPLOYEE_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-internal-employee.xml");
    private static final File KIRK_USER_TIBERIUS_FILE = new File(LAB_OBJECTS_DIRECTORY + "users/kirk-tiberius-user.xml");

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        csv1TargetFile = new File(getTestTargetDir(), CSV_1_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_1_SOURCE_FILE, csv1TargetFile);
        csv2TargetFile = new File(getTestTargetDir(), CSV_2_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_2_SOURCE_FILE, csv2TargetFile);
        csv3TargetFile = new File(getTestTargetDir(), CSV_3_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_3_SOURCE_FILE, csv3TargetFile);
    }

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(KIRK_USER_TIBERIUS_FILE);
    }

    @Test(groups={"M6"})
    public void mod06test01UsingAccountIntentsForProvisioning() {
        importObject(NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE, true);

        importObject(CSV_1_RESOURCE_FILE_6_1, true);
        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        importObject(CSV_2_RESOURCE_FILE_6_1, true);
        changeResourceAttribute(CSV_2_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv2TargetFile.getAbsolutePath(), true);

        importObject(CSV_3_RESOURCE_FILE_6_1, true);
        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);

        addObjectFromFile(SECRET_I_ROLE_FILE);
        addObjectFromFile(SECRET_II_ROLE_FILE);
        addObjectFromFile(CSV1_TESTER_ROLE_FILE);
        addObjectFromFile(CSV3_ADMIN_ROLE_FILE);
        addObjectFromFile(INCOGNITO_ROLE_FILE);
        addObjectFromFile(INTERNAL_EMPLOYEE_ROLE_FILE);

        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);

        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Secret Projects I", "Secret Projects II", "Incognito");
        Utils.addAsignments(showUser("kirk").selectTabAssignments(), CSV1_TESTER_ROLE_NAME, CSV3_ADMIN_ROLE_NAME);

        AbstractTableWithPrismView<ProjectionsTab<UserPage>> table = showUser("kirk").selectTabProjections()
                .table();
        Selenide.screenshot("kirk_user_projections");
        table.search()
                .referencePanelByItemName("Resource")
                    .inputRefOid("10000000-9999-9999-0000-a000ff000002")
                    .updateSearch()
                .and()
             .assertTableContainsText("jkirk");
        table.assertTableContainsText("_kirk");

       table.search()
                .referencePanelByItemName("Resource")
                    .inputRefOid("10000000-9999-9999-0000-a000ff000003")
                    .updateSearch()
                .and()
            .assertTableContainsText("jkirk");

        table.search()
                .referencePanelByItemName("Resource")
                    .inputRefOid("10000000-9999-9999-0000-a000ff000004")
                    .updateSearch()
                .and()
            .assertTableContainsText("cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com");
        table.assertTableContainsText("cn=Jim Tiberius Kirk,ou=_Administrators_,ou=ExAmPLE,dc=example,dc=com");

        assertShadowExists(CSV_1_RESOURCE_NAME, "Name", "jkirk", "default", true);
        assertShadowExists(CSV_1_RESOURCE_NAME, "Name", "_kirk", "test", true);
        assertShadowExists(CSV_2_RESOURCE_NAME, "Name", "jkirk", "default", true);
        assertShadowExists(CSV_3_RESOURCE_NAME, "Name",
                "cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com", "default", true);
        assertShadowExists(CSV_3_RESOURCE_NAME, "Name",
                "cn=Jim Tiberius Kirk,ou=_Administrators_,ou=ExAmPLE,dc=example,dc=com", "admin", true);
    }

}
