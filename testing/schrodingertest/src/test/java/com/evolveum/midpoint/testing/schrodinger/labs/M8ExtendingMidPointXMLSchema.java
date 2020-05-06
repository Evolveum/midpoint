/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceAccountsTab;
import com.evolveum.midpoint.schrodinger.page.resource.AccountPage;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;

import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author skublik
 */

public class M8ExtendingMidPointXMLSchema extends  AbstractLabTest {

    private static final File HR_RESOURCE_FILE_8_1 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-hr.xml");
    private static final File CSV_3_RESOURCE_FILE_8_1 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap-8-1.xml");

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
    }

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextBeforeTestClass" })
    @Override
    protected void springTestContextPrepareTestInstance() throws Exception {
        String home = System.getProperty("midpoint.home");
        File schemaDir = new File(home, "schema");

        if (!schemaDir.mkdir()) {
            if (schemaDir.exists()) {
                FileUtils.cleanDirectory(schemaDir);
            } else {
                throw new IOException("Creation of directory \"" + schemaDir.getAbsolutePath() + "\" unsuccessful");
            }
        }
        File schemaFile = new File(schemaDir, EXTENSION_SCHEMA_NAME);
        FileUtils.copyFile(EXTENSION_SCHEMA_FILE, schemaFile);

        super.springTestContextPrepareTestInstance();
    }

    @Test(groups={"M8"}, dependsOnGroups={"M7"})
    public void mod08test01ExtendingMidPointXMLSchema() {
        PrismForm<AssignmentHolderBasicTab<UserPage>> form = basicPage.newUser()
                .selectTabBasic()
                    .form();

        form.findProperty("ouNumber");
        form.findProperty("ouPath");
        form.findProperty("isManager");
        form.findProperty("empStatus");

//        showTask("HR Synchronization").clickSuspend();

        importObject(HR_RESOURCE_FILE_8_1,true);
        changeResourceAttribute(HR_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, hrTargetFile.getAbsolutePath(), true);

        importObject(CSV_3_RESOURCE_FILE_8_1,true);
        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        ResourceAccountsTab<ViewResourcePage> accountTab = basicPage.listResources()
                .table()
                    .search()
                        .byName()
                            .inputValue(HR_RESOURCE_NAME)
                            .updateSearch()
                        .and()
                    .clickByName(HR_RESOURCE_NAME)
                        .clickAccountsTab()
                        .clickSearchInResource();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        accountTab.table()
                .selectCheckboxByName("001212")
                    .clickHeaderActionDropDown()
                        .clickImport()
                    .and()
                .and()
            .feedback()
                .isSuccess();

        form = accountTab.table()
                .clickOnOwnerByName("X001212")
                .selectTabBasic()
                .form();

        Assert.assertTrue(form.compareInputAttributeValue("ouPath", "0300"));
        Assert.assertTrue(form.compareSelectAttributeValue("isManager", "True"));
        Assert.assertTrue(form.compareInputAttributeValue("empStatus", "A"));

        form.and()
                .and()
            .selectTabAssignments()
                .clickAddAssignemnt()
                    .table()
                        .search()
                            .byName()
                                .inputValue("Internal Employee")
                                .updateSearch()
                            .and()
                        .selectCheckboxByName("Internal Employee")
                        .and()
                    .clickAdd()
                .and()
            .clickSave()
                .feedback()
                    .isSuccess();

        AccountPage shadow = showShadow(CSV_1_RESOURCE_NAME, "Login", "jsmith");
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        PrismForm<AccountPage> accountForm = shadow.form();
        Selenide.sleep(1000);
        Assert.assertTrue(accountForm.compareInputAttributeValue("dep", "Human Resources"));

        showShadow(CSV_2_RESOURCE_NAME, "Login", "jsmith");
        Assert.assertTrue(accountForm.compareInputAttributeValue("department", "Human Resources"));

        Assert.assertTrue(existShadow(CSV_3_RESOURCE_NAME, "Distinguished Name", "cn=John Smith,ou=0300,ou=ExAmPLE,dc=example,dc=com"));
    }
}
