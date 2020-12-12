/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Selenide;

import com.codeborne.selenide.ex.ElementNotFound;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.configuration.ObjectPolicyTab;
import com.evolveum.midpoint.schrodinger.component.org.OrgRootTab;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceAccountsTab;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class M10ObjectTemplate extends AbstractLabTest{

    private static final File OBJECT_TEMPLATE_USER_SIMPLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectTemplate/object-template-example-user-simple.xml");
    private static final File OBJECT_TEMPLATE_USER_FILE_10_3 = new File(LAB_OBJECTS_DIRECTORY + "objectTemplate/object-template-example-user-10-3.xml");
    private static final File LOOKUP_EMP_STATUS_FILE = new File(LAB_OBJECTS_DIRECTORY + "lookupTables/lookup-emp-status.xml");
    private static final File CSV_3_RESOURCE_FILE_10_4 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap-10-4.xml");
    private static final File SYSTEM_CONFIGURATION_FILE_11_1 = new File(LAB_OBJECTS_DIRECTORY + "systemConfiguration/system-configuration-11-1.xml");

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
    }

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(ARCHETYPE_ORG_FUNCTIONAL_FILE, ARCHETYPE_ORG_COMPANY_FILE, ARCHETYPE_ORG_GROUP_FILE,
                ARCHETYPE_ORG_GROUP_LIST_FILE, KIRK_USER_TIBERIUS_FILE);
    }

    @Test
    public void mod10test01SimpleObjectTemplate() throws IOException {
        importObject(ORG_EXAMPLE_FILE, true);
        importObject(NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE, true);
        csv1TargetFile = new File(getTestTargetDir(), CSV_1_FILE_SOURCE_NAME);

        importObject(CSV_1_RESOURCE_FILE, true);
        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        hrTargetFile = new File(getTestTargetDir(), HR_FILE_SOURCE_NAME);
        FileUtils.copyFile(HR_SOURCE_FILE, hrTargetFile);
        importObject(HR_NO_EXTENSION_RESOURCE_FILE, true);
        changeResourceAttribute(HR_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, hrTargetFile.getAbsolutePath(), true);

        addObjectFromFile(HR_SYNCHRONIZATION_TASK_FILE);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);

        addObjectFromFile(OBJECT_TEMPLATE_USER_SIMPLE_FILE);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);

        basicPage.listResources()
                .table()
                    .clickByName(HR_RESOURCE_NAME)
                        .clickAccountsTab()
                            .clickSearchInResource()
                                .table()
                                .selectCheckboxByName("001212")
                                .clickImport();
        ((PrismFormWithActionButtons<ObjectPolicyTab>)basicPage.objectPolicy()
                .clickAddObjectPolicy()
                    .selectOption("type", "User")
                    .editRefValue("objectTemplateRef")
                        .table()
                            .clickByName("ExAmPLE User Template"))
                    .clickDone()
                    .and()
                .clickSave()
                    .feedback()
                        .isSuccess();

        showUser("X001212")
                .checkReconcile()
                .clickSave()
                    .feedback()
                        .isSuccess();

        Assert.assertTrue(showUser("X001212")
                .selectTabBasic()
                    .form()
                        .compareInputAttributeValue("fullName", "John Smith"));


        FileUtils.copyFile(HR_SOURCE_FILE_10_1, hrTargetFile);
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        Assert.assertTrue(showUser("X000998")
                .selectTabBasic()
                .form()
                .compareInputAttributeValue("fullName", "David Lister"));

        TaskPage task = basicPage.newTask();
        task.setHandlerUriForNewTask("Recompute task");
        task.selectTabBasic()
                .form()
                    .addAttributeValue("name", "User Recomputation Task")
                    .selectOption("recurrence","Single")
                    .selectOption("objectType","User")
                    .and()
                .and()
            .clickSaveAndRun()
                .feedback()
                    .isInfo();

        Assert.assertTrue(showUser("kirk")
                .selectTabBasic()
                .form()
                .compareInputAttributeValue("fullName", "Jim Tiberius Kirk"));
    }

    @Test(dependsOnMethods = {"mod10test01SimpleObjectTemplate"})
    public void mod10test02AutomaticAssignments() throws IOException {
        addObjectFromFile(OBJECT_TEMPLATE_USER_FILE);
        importObject(ORG_EXAMPLE_FILE, true);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);

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

        AssignmentsTab<UserPage> tab = accountTab.table()
                .clickOnOwnerByName("X001212")
                .selectTabAssignments();
        Selenide.screenshot("M10_assignmentsTab");
        Assert.assertTrue(tab.containsAssignmentsWithRelation("Member", "Human Resources",
                "Active Employees", "Internal Employee"));
        Assert.assertTrue(tab.containsAssignmentsWithRelation("Manager", "Human Resources"));

        FileUtils.copyFile(HR_SOURCE_FILE_10_2_PART1, hrTargetFile);
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        Assert.assertTrue(showUser("X000999")
            .selectTabAssignments()
                .containsAssignmentsWithRelation("Member", "Java Development",
                "Active Employees", "Internal Employee"));

        showTask("User Recomputation Task").clickRunNow();
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        Assert.assertTrue(showUser("X000998")
                .selectTabAssignments()
                .containsAssignmentsWithRelation("Member", "Java Development",
                        "Active Employees", "Internal Employee"));

        FileUtils.copyFile(HR_SOURCE_FILE_10_2_PART2, hrTargetFile);
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        UserPage user = showUser("X000998");
        Assert.assertTrue(user.selectTabBasic()
                .form()
                    .compareSelectAttributeValue("administrativeStatus", "Disabled"));
        Assert.assertTrue(user.selectTabAssignments()
                .containsAssignmentsWithRelation("Member", "Inactive Employees", "Internal Employee"));

        FileUtils.copyFile(HR_SOURCE_FILE_10_2_PART3, hrTargetFile);
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        user = showUser("X000998");
        Assert.assertTrue(user.selectTabBasic()
                .form()
                .compareSelectAttributeValue("administrativeStatus", "Disabled"));
        Assert.assertTrue(user.selectTabAssignments()
                .containsAssignmentsWithRelation("Member", "Former Employees"));

        FileUtils.copyFile(HR_SOURCE_FILE_10_2_PART1, hrTargetFile);
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        user = showUser("X000998");
        Assert.assertTrue(user.selectTabBasic()
                .form()
                .compareSelectAttributeValue("administrativeStatus", "Enabled"));
        Assert.assertTrue(showUser("X000998")
                .selectTabAssignments()
                .containsAssignmentsWithRelation("Member", "Java Development",
                        "Active Employees", "Internal Employee"));
    }

    @Test(dependsOnMethods = {"mod10test02AutomaticAssignments"})
    public void mod10test03LookupTablesAndAttributeOverrides() {

        PrismForm<AssignmentHolderBasicTab<UserPage>> form = showUser("kirk")
                .selectTabBasic()
                    .form();

        form.showEmptyAttributes("Properties");
        form.addAttributeValue("empStatus", "O");
        form.addAttributeValue("familyName", "kirk2");
        boolean existFeedback = false;
        try { existFeedback = form.and().and().feedback().isError(); } catch (ElementNotFound e) { }
        Assert.assertFalse(existFeedback);
        Assert.assertTrue(form.propertyWithTitleTextExists("telephoneNumber",
                "Primary telephone number of the user, org. unit, etc."));
        Assert.assertTrue(form.propertyWithTitleTextExists("telephoneNumber",
                "Mobile Telephone Number"));
        Assert.assertTrue(form.isPropertyEnabled("honorificSuffix"));

        addObjectFromFile(LOOKUP_EMP_STATUS_FILE);
        addObjectFromFile(OBJECT_TEMPLATE_USER_FILE_10_3);
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);

        form = showUser("kirk")
                .selectTabBasic()
                .form();

        form.showEmptyAttributes("Properties");
        form.addAttributeValue("empStatus", "O");
        form.addAttributeValue("familyName", "kirk2");
        Assert.assertTrue(form.and().and().feedback().isError());
        Assert.assertTrue(form.propertyWithTitleTextExists("telephoneNumber",
                "Primary telephone number of the user, org. unit, etc."));
        Assert.assertTrue(form.propertyWithTitleTextExists("telephoneNumber",
                "Mobile Telephone Number"));
        Assert.assertFalse(form.isPropertyEnabled("honorificSuffix"));
    }

    @Test(dependsOnMethods = {"mod10test03LookupTablesAndAttributeOverrides"})
    public void mod10test04FinishingManagerMapping() {
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        showTask("User Recomputation Task").clickRunNow();
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        OrgRootTab rootTab = basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure");
        Assert.assertTrue(rootTab.getOrgHierarchyPanel()
                .showTreeNodeDropDownMenu("Technology Division")
                    .expandAll()
                .selectOrgInTree("IT Administration Department")
                .and()
            .getManagerPanel()
                .containsManager("John Wicks"));

        rootTab.getMemberPanel()
                .selectType("User")
                .table()
                    .search()
                        .resetBasicSearch()
                    .and()
                .clickByName("X000158");
        Assert.assertTrue(new UserPage().selectTabProjections()
                .table()
                    .clickByName("cn=Alice Black,ou=0212,ou=0200,ou=ExAmPLE,dc=example,dc=com")
                        .compareInputAttributeValue("manager", "X000390"));
        Assert.assertTrue(showUser("X000390").selectTabProjections()
                .table()
                    .clickByName("cn=John Wicks,ou=0212,ou=0200,ou=ExAmPLE,dc=example,dc=com")
                        .compareInputAttributeValue("manager", "X000035"));
        Assert.assertTrue(showUser("X000035").selectTabProjections()
                .table()
                    .clickByName("cn=James Bradley,ou=0200,ou=ExAmPLE,dc=example,dc=com")
                        .showEmptyAttributes("Attributes")
                        .compareInputAttributeValue("manager", ""));

        Assert.assertTrue(showUser("kirk")
                .selectTabAssignments()
                    .containsAssignmentsWithRelation("Member", "Warp Speed Research"));
        Assert.assertTrue(new UserPage().selectTabProjections()
                .table()
                    .clickByName("cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com")
                        .showEmptyAttributes("Attributes")
                        .compareInputAttributeValue("manager", ""));

        showUser("picard")
                .selectTabAssignments()
                    .clickAddAssignemnt("New Organization type assignment with manager relation")
                        .selectType("Org")
                            .table()
                                .search()
                                    .byName()
                                        .inputValue("0919")
                                        .updateSearch()
                                    .and()
                                .selectCheckboxByName("0919")
                                .and()
                            .clickAdd()
                            .and()
                        .clickSave()
                            .feedback()
                                .isSuccess();

        showUser("kirk").checkReconcile()
                .clickSave()
                    .feedback()
                        .isSuccess();

        Assert.assertTrue(showUser("kirk").selectTabProjections()
                .table()
                    .clickByName("cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com")
                        .compareInputAttributeValue("manager", "picard"));

        showUser("picard").selectTabAssignments()
                .table()
                    .selectCheckboxByName("Warp Speed Research")
                    .removeByName("Warp Speed Research")
                    .and()
                .and()
            .clickSave()
                .feedback()
                    .isSuccess();

        Assert.assertTrue(showUser("kirk").selectTabProjections()
                .table()
                    .clickByName("cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com")
                        .compareInputAttributeValue("manager", "picard"));

        addObjectFromFile(CSV_3_RESOURCE_FILE_10_4);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);

        showUser("kirk").checkReconcile()
                .clickSave()
                    .feedback()
                        .isSuccess();

        Assert.assertTrue(showUser("kirk").selectTabProjections()
                .table()
                    .clickByName("cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com")
                        .showEmptyAttributes("Attributes")
                        .compareInputAttributeValue("manager", ""));
    }

    @Test(dependsOnMethods = {"mod10test03LookupTablesAndAttributeOverrides"})
    public void mod11test01ConfiguringNotifications() throws IOException {
//        showTask("HR Synchronization").clickResume();

        notificationFile = new File(getTestTargetDir(), NOTIFICATION_FILE_NAME);
        notificationFile.createNewFile();

        addObjectFromFile(SYSTEM_CONFIGURATION_FILE_11_1);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);

        basicPage.notifications()
                .setRedirectToFile(notificationFile.getAbsolutePath())
                .and()
                .clickSave()
                .feedback()
                .isSuccess();

        FileUtils.copyFile(HR_SOURCE_FILE_11_1, hrTargetFile);
        Selenide.sleep(MidPoint.TIMEOUT_LONG_1_M);

        String notification = readBodyOfLastNotification();

        String startOfNotification = "Notification about user-related operation (status: SUCCESS)\n"
                + "\n"
                + "User: Chuck Norris (X000997, oid ";

        String endOfNotification = "The user record was created with the following data:\n"
                + " - Name: X000997\n"
                + " - Full name: Chuck Norris\n"
                + " - Given name: Chuck\n"
                + " - Family name: Norris\n"
                + " - Title: Application Developer\n"
                + " - Email: chuck.norris@example.com\n"
                + " - Employee Number: 000997\n"
                + " - Cost Center: 0211\n"
                + " - Organizational Unit: Java Development\n"
                + " - Extension:\n"
                + "    - Organizational Path: 0200:0210:0211\n"
                + "    - Is Manager: false\n"
                + "    - Employee Status: A\n"
                + " - Credentials:\n"
                + "    - Password:\n"
                + "       - Value: (protected string)\n"
                + " - Activation:\n"
                + "    - Administrative status: ENABLED\n"
                + "    - Valid from: Jul 15, 2010, 8:20:00 AM\n"
                + " - Assignment #1:\n"
                + "    - Target: Employee (archetype) [default]\n"
                + " - Assignment #2:\n"
                + "    - Target: ACTIVE (org) [default]\n"
                + " - Assignment #3:\n"
                + "    - Target: 0211 (org) [default]\n"
                + " - Assignment #4:\n"
                + "    - Target: Internal Employee (role) [default]\n"
                + "\n"
                + "Requester: midPoint Administrator (administrator)\n"
                + "Channel: http://midpoint.evolveum.com/xml/ns/public/common/channels-3#liveSync\n"
                + "\n";

        Assertions.assertThat(notification).startsWith(startOfNotification);
        Assertions.assertThat(notification).endsWith(endOfNotification);
    }

    protected String readBodyOfLastNotification() throws IOException {
        String separator = "============================================";
        byte[] encoded = Files.readAllBytes(Paths.get(notificationFile.getAbsolutePath()));
        String notifications = new String(encoded, Charset.defaultCharset());
        if (!notifications.contains(separator)) {
            return "";
        }
        String notification = notifications.substring(notifications.lastIndexOf(separator) + separator.length(), notifications.length()-1);
        String bodyTag = "body='";
        if (!notifications.contains(bodyTag)) {
            return "";
        }
        String body = notification.substring(notification.indexOf(bodyTag) + bodyTag.length(), notification.lastIndexOf("'"));
        return body;
    }

}
