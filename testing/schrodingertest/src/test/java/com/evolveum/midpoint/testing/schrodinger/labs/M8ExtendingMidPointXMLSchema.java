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
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class M8ExtendingMidPointXMLSchema extends  AbstractLabTest {


    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();

        hrTargetFile = new File(getTestTargetDir(), HR_FILE_SOURCE_NAME);
        FileUtils.copyFile(HR_SOURCE_FILE_7_4_PART_4, hrTargetFile);

        csv3TargetFile = new File(getTestTargetDir(), CSV_3_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_3_SOURCE_FILE, csv3TargetFile);

        csv1TargetFile = new File(getTestTargetDir(), CSV_1_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_1_SOURCE_FILE, csv1TargetFile);

        csv2TargetFile = new File(getTestTargetDir(), CSV_2_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_2_SOURCE_FILE, csv2TargetFile);
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

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(INTERNAL_EMPLOYEE_ROLE_FILE);
    }

    @Test
    public void mod08test01ExtendingMidPointXMLSchema() throws IOException {
        importObject(NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE, true);

        PrismForm<AssignmentHolderBasicTab<UserPage>> form = basicPage.newUser()
                .selectTabBasic()
                    .form();

        form.findProperty("ouNumber");
        form.findProperty("ouPath");
        form.findProperty("isManager");
        form.findProperty("empStatus");

//        showTask("HR Synchronization").clickSuspend();

        importObject(HR_RESOURCE_FILE_8_1, true);
        changeResourceAttribute(HR_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, hrTargetFile.getAbsolutePath(), true);

        importObject(CSV_1_RESOURCE_FILE, true);
        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        importObject(CSV_2_RESOURCE_FILE, true);
        changeResourceAttribute(CSV_2_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv2TargetFile.getAbsolutePath(), true);

        importObject(CSV_3_RESOURCE_FILE_8_1, true);
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
                        .clickImport()
                    .and()
                .and()
            .feedback()
                .isSuccess();

        form = accountTab.table()
                .clickOnOwnerByName("X001212")
                .selectTabBasic()
                .form();

        form.assertInputAttributeValueMatches("ouPath", "0300");
        form.assertSelectAttributeValueMatches("isManager", "True");
        form.assertInputAttributeValueMatches("empStatus", "A");

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
        accountForm.assertInputAttributeValueMatches("dep", "Human Resources");

        showShadow(CSV_2_RESOURCE_NAME, "Login", "jsmith");
        accountForm.assertInputAttributeValueMatches("department", "Human Resources");

        assertShadowExists(CSV_3_RESOURCE_NAME, "Distinguished Name", "cn=John Smith,ou=0300,ou=ExAmPLE,dc=example,dc=com");
    }
}
