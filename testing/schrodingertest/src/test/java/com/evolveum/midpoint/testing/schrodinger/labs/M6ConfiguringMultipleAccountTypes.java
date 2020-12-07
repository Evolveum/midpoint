/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Utils;
import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
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

    private static final File CSV_1_RESOURCE_FILE_6_1 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-6-1.xml");
    private static final File CSV_3_RESOURCE_FILE_6_1 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap-6-1.xml");
    private static final File CSV1_TESTER_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-csv1-tester.xml");
    private static final File CSV3_ADMIN_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-csv3-admin.xml");
    private static final String CSV1_TESTER_ROLE_NAME = "CSV-1 Tester";
    private static final String CSV3_ADMIN_ROLE_NAME = "CSV-3 Admin";

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
        return Arrays.asList(KIRK_USER_FILE);
    }

    @Test(groups={"M6"})
    public void mod06test01UsingAccountIntentsForProvisioning() {
        importObject(CSV_1_RESOURCE_FILE, true);
        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);
        importObject(CSV_2_RESOURCE_FILE, true);
        changeResourceAttribute(CSV_2_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv2TargetFile.getAbsolutePath(), true);
        importObject(CSV_3_RESOURCE_FILE, true);
        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);

        importObject(CSV_1_RESOURCE_FILE_6_1, true);
        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        importObject(CSV_3_RESOURCE_FILE_6_1, true);
        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);

        addObjectFromFile(CSV1_TESTER_ROLE_FILE);
        addObjectFromFile(CSV3_ADMIN_ROLE_FILE);

        Utils.addAsignments(showUser("kirk").selectTabAssignments(), CSV1_TESTER_ROLE_NAME, CSV3_ADMIN_ROLE_NAME);

        AbstractTableWithPrismView<ProjectionsTab<UserPage>> table = showUser("kirk").selectTabProjections()
                .table();
        Assert.assertTrue(table.search()
                .referencePanelByItemName("Resource")
                    .inputRefOid("10000000-9999-9999-0000-a000ff000002")
                    .updateSearch()
                .and()
             .containsText("jkirk"));
        Assert.assertTrue(table.containsText("_kirk"));

        Assert.assertTrue(table.search()
                .referencePanelByItemName("Resource")
                    .inputRefOid("10000000-9999-9999-0000-a000ff000003")
                    .updateSearch()
                .and()
            .containsText("jkirk"));

        Assert.assertTrue(table.search()
                .referencePanelByItemName("Resource")
                    .inputRefOid("10000000-9999-9999-0000-a000ff000004")
                    .updateSearch()
                .and()
            .containsText("cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com"));
        Assert.assertTrue(table.containsText("cn=Jim Tiberius Kirk,ou=_Administrators_,ou=ExAmPLE,dc=example,dc=com"));

        Assert.assertTrue(existShadow(CSV_1_RESOURCE_NAME, "Name", "jkirk", "default", true));
        Assert.assertTrue(existShadow(CSV_1_RESOURCE_NAME, "Name", "_kirk", "test", true));
        Assert.assertTrue(existShadow(CSV_2_RESOURCE_NAME, "Name", "jkirk", "default", true));
        Assert.assertTrue(existShadow(CSV_3_RESOURCE_NAME, "Name",
                "cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com", "default", true));
        Assert.assertTrue(existShadow(CSV_3_RESOURCE_NAME, "Name",
                "cn=Jim Tiberius Kirk,ou=_Administrators_,ou=ExAmPLE,dc=example,dc=com", "admin", true));
    }

}
