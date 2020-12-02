/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceAccountsTab;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author skublik
 */

public class M7SynchronizationFlavours extends AbstractLabTest{

    private static final Logger LOG = LoggerFactory.getLogger(M7SynchronizationFlavours.class);

    @Test(groups={"M7"}, dependsOnGroups={"M6"})
    public void mod07test01RunningImportFromResource() throws IOException {
        hrTargetFile = new File(getTestTargetDir(), HR_FILE_SOURCE_NAME);
        FileUtils.copyFile(HR_SOURCE_FILE, hrTargetFile);

        addObjectFromFile(HR_NO_EXTENSION_RESOURCE_FILE);
        changeResourceAttribute(HR_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, hrTargetFile.getAbsolutePath(), true);

        ResourceAccountsTab<ViewResourcePage> accountTab = basicPage.listResources()
                .table()
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

        UserPage owner = accountTab.table()
                .clickOnOwnerByName("X001212");

        Assert.assertTrue(owner.selectTabBasic()
                .form()
                    .compareInputAttributeValue("name", "X001212"));

        basicPage.listResources()
                .table()
                    .clickByName(HR_RESOURCE_NAME)
                        .clickAccountsTab()
                            .importTask()
                                .clickCreateNew()
                                    .selectTabBasic()
                                        .form()
                                            .addAttributeValue("name","Initial import from HR")
                                            .and()
                                        .and()
                                    .clickSaveAndRun()
                                        .feedback()
                                            .isInfo();

        Assert.assertEquals(showTask("Initial import from HR")
                .selectTabOperationStatistics()
                    .getSuccessfullyProcessed(), Integer.valueOf(14));
        Assert.assertEquals(basicPage.listUsers(ARCHETYPE_EMPLOYEE_PLURAL_LABEL).getCountOfObjects(), 15);
    }

    @Test(dependsOnMethods = {"mod07test01RunningImportFromResource"}, groups={"M7"}, dependsOnGroups={"M6"})
    public void mod07test02RunningAccountReconciliation() {
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        createReconTask("CSV-1 Reconciliation", CSV_1_RESOURCE_NAME);
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        deselectDryRun("CSV-1 Reconciliation");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        Assert.assertTrue(containsProjection("X001212", CSV_1_RESOURCE_OID, "jsmith"));

        createReconTask("CSV-2 Reconciliation", CSV_2_RESOURCE_NAME);
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        deselectDryRun("CSV-2 Reconciliation");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        Assert.assertTrue(containsProjection("X001212", CSV_2_RESOURCE_OID, "jsmith"));

        createReconTask("CSV-3 Reconciliation", CSV_3_RESOURCE_NAME);
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        deselectDryRun("CSV-3 Reconciliation");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        Assert.assertTrue(containsProjection("X001212", CSV_3_RESOURCE_OID, "cn=John Smith,ou=ExAmPLE,dc=example,dc=com"));
    }

    @Test(dependsOnMethods = {"mod07test02RunningAccountReconciliation"}, groups={"M7"}, dependsOnGroups={"M6"})
    public void mod07test03RunningAttributeReconciliation() throws IOException {
        FileUtils.copyFile(CSV_1_SOURCE_FILE_7_3, csv1TargetFile);

        showTask("CSV-1 Reconciliation", "Reconciliation tasks").clickRunNow();

        Assert.assertTrue(
                showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk")
                        .form()
                        .compareInputAttributeValues("groups", "Internal Employees",
                                "Essential Documents"));

    }

    @Test(dependsOnMethods = {"mod07test03RunningAttributeReconciliation"}, groups={"M7"}, dependsOnGroups={"M6"})
    public void mod07test04RunningLiveSync() throws IOException {
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        TaskPage task = basicPage.newTask();
        task.setHandlerUriForNewTask("Live synchronization task");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        task.selectTabBasic()
                .form()
                    .addAttributeValue("objectclass", "AccountObjectClass")
                    .addAttributeValue(TaskType.F_NAME, "HR Synchronization")
                    .selectOption("recurrence","Recurring")
                    .selectOption("binding","Tight")
                    .editRefValue("objectRef")
                        .selectType("Resource")
                        .table()
                            .search()
                                .byName()
                                    .inputValue(HR_RESOURCE_NAME)
                                    .updateSearch()
                                .and()
                            .clickByName(HR_RESOURCE_NAME)
                    .and()
                .and()
            .selectScheduleTab()
                .form()
                    .addAttributeValue("interval", "5")
                    .and()
                .and()
            .clickSaveAndRun()
                .feedback()
                    .isInfo();

        FileUtils.copyFile(HR_SOURCE_FILE_7_4_PART_1, hrTargetFile);
        Selenide.sleep(20000);
        Assert.assertTrue(showUser("X000999")
                .selectTabBasic()
                    .form()
                        .compareInputAttributeValue("givenName", "Arnold"));
        Assert.assertTrue(showUser("X000999")
                .selectTabBasic()
                .form()
                .compareInputAttributeValue("familyName", "Rimmer"));
        Assert.assertTrue(showUser("X000999")
                .selectTabBasic()
                .form()
                .compareSelectAttributeValue("administrativeStatus", "Enabled"));

        FileUtils.copyFile(HR_SOURCE_FILE_7_4_PART_2, hrTargetFile);
        Selenide.sleep(20000);
        Assert.assertTrue(showUser("X000999")
                .selectTabBasic()
                .form()
                .compareInputAttributeValue("givenName", "Arnold J."));

        FileUtils.copyFile(HR_SOURCE_FILE_7_4_PART_3, hrTargetFile);
        Selenide.sleep(20000);
        Assert.assertTrue(showUser("X000999")
                .selectTabBasic()
                .form()
                .compareSelectAttributeValue("administrativeStatus", "Disabled"));

        FileUtils.copyFile(HR_SOURCE_FILE_7_4_PART_4, hrTargetFile);
        Selenide.sleep(20000);
        Assert.assertTrue(showUser("X000999")
                .selectTabBasic()
                .form()
                .compareSelectAttributeValue("administrativeStatus", "Enabled"));

    }

    private boolean containsProjection(String user, String resourceOid, String accountName) {
       return showUser(user).selectTabProjections()
                .table()
                    .search()
                        .referencePanelByItemName("Resource")
                            .inputRefOid(resourceOid)
                            .updateSearch()
                        .and()
                    .containsText(accountName);
    }

    private void createReconTask(String reconTaskName, String resource){
        TaskPage task = basicPage.newTask();
        task.setHandlerUriForNewTask("Reconciliation task");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        task.selectTabBasic()
                .form()
                    .addAttributeValue("objectclass", "AccountObjectClass")
                    .selectOption("dryRun", "True")
                    .addAttributeValue(TaskType.F_NAME, reconTaskName)
                    .editRefValue("objectRef")
                        .selectType("Resource")
                            .table()
                                .search()
                                    .byName()
                                        .inputValue(resource)
                                        .updateSearch()
                                    .and()
                                .clickByName(resource)
                .and()
            .and()
                .clickSaveAndRun()
                    .feedback()
                        .isInfo();
    }

    private void deselectDryRun(String taskName) {
        showTask(taskName).selectTabBasic()
                .form()
                    .selectOption("dryRun", "Undefined")
                .and()
            .and()
        .clickSaveAndRun()
            .feedback()
                .isInfo();
    }
}
