/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgTreePage;

import com.evolveum.midpoint.schrodinger.page.resource.AccountPage;
import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

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

public class M9OrganizationalStructure extends AbstractLabTest{


    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        csv1TargetFile = new File(getTestTargetDir(), CSV_1_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_1_SOURCE_FILE, csv1TargetFile);
    }

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(ARCHETYPE_ORG_FUNCTIONAL_FILE, ARCHETYPE_ORG_COMPANY_FILE, ARCHETYPE_ORG_GROUP_FILE,
                ARCHETYPE_ORG_GROUP_LIST_FILE, KIRK_USER_TIBERIUS_FILE, OBJECT_TEMPLATE_USER_FILE);
    }

    @Test(groups={"M9"})
    public void mod09test01ImportStaticOrgStructure() {
        importObject(NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE, true);

        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        login.login(getUsername(), getPassword());

        importObject(ORG_EXAMPLE_FILE, true);

        OrgTreePage orgTree = basicPage.orgStructure();
        Assert.assertTrue(orgTree.selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                .getOrgHierarchyPanel()
                    .showTreeNodeDropDownMenu("ExAmPLE, Inc. - Functional Structure")
                        .expandAll()
                    .expandOrg("Software Department")
                    .containsChildOrg("ExAmPLE, Inc. - Functional Structure", "Executive Division", "Sales Department",
                                "Human Resources", "Technology Division", "IT Administration Department", "Software Department", "Java Development"));
        Assert.assertTrue(orgTree.selectTabWithRootOrg("Groups")
                .getOrgHierarchyPanel()
                    .containsChildOrg("Groups", "Active Employees", "Administrators", "Contractors", "Former Employees",
                        "Inactive Employees", "Security"));

        importObject(ORG_SECRET_OPS_FILE, true);
        Assert.assertTrue(basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .containsChildOrg("Secret Operations", "Transportation and Logistics Department"));
    }

    @Test(dependsOnMethods = {"mod09test01ImportStaticOrgStructure"})
    public void mod09test02CreateStaticOrgStructure() {
        OrgPage orgPage = (OrgPage) basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .showTreeNodeDropDownMenu("Secret Operations")
                            .expandAll()
                        .selectOrgInTree("Transportation and Logistics Department")
                        .and()
                    .getMemberPanel()
                        .newMember("Create Organization type member with Member relation", "Organization");
        orgPage
                .selectTabBasic()
                    .form()
                        .addAttributeValue("Name", "0919")
                        .addAttributeValue("Display Name", "Warp Speed Research")
                        .and()
                    .and()
                .clickSave()
                    .feedback()
                        .isSuccess();

        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .showTreeNodeDropDownMenu("Secret Operations")
                            .expandAll()
                        .showTreeNodeDropDownMenu("Warp Speed Research")
                            .edit()
                                .selectTabBasic()
                                    .form()
                                        .assertInputAttributeValueMatches("Name", "0919");

        showUser("kirk").selectTabAssignments()
                .clickAddAssignemnt("New Organization type assignment with Member relation")
                    .table()
                        .paging()
                            .next()
                            .and()
                        .and()
                    .table()
                        .search()
                            .byName()
                            .inputValue("0919")
                            .updateSearch()
                        .and()
                        .rowByColumnLabel("Name", "0919")
                        .clickCheckBox()
                        .and()
                    .and()
                    .clickAdd()
                .and()
            .clickSave()
                .feedback()
                    .isSuccess();

        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .showTreeNodeDropDownMenu("Secret Operations")
                            .expandAll()
                        .selectOrgInTree("Warp Speed Research")
                        .and()
                    .getMemberPanel()
                        .selectType("User")
                        .table()
                            .assertTableContainsText("kirk");

        showUser("kirk").selectTabAssignments()
                .table()
                    .selectCheckboxByName("Warp Speed Research")
                    .removeByName("Warp Speed Research")
                    .and()
                .and()
            .clickSave()
                .feedback()
                    .isSuccess();
    }

    @Test(dependsOnMethods = {"mod09test02CreateStaticOrgStructure"})
    public void mod09test03OrganizationActingAsARole() {
        addObjectFromFile(SECRET_I_ROLE_FILE);
        addObjectFromFile(SECRET_II_ROLE_FILE);

        importObject(CSV_1_RESOURCE_FILE, true);
        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .showTreeNodeDropDownMenu("Secret Operations")
                            .expandAll()
                        .selectOrgInTree("Warp Speed Research")
                        .and()
                    .getMemberPanel()
                        .selectType("User")
                            .table()
                            .assertTableContainsText("kirk");

        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .showTreeNodeDropDownMenu("Warp Speed Research")
                            .edit()
                                .selectTabInducements()
                                    .clickAddInducement()
                                        .table()
                                            .search()
                                                .byName()
                                                    .inputValue("Secret Projects I")
                                                    .updateSearch()
                                                    .and()
                                            .selectCheckboxByName("Secret Projects I")
                                            .and()
                                        .clickAdd()
                                    .and()
                                .clickSave()
                                    .feedback()
                                        .isSuccess();

        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .selectOrgInTree("Warp Speed Research")
                        .and()
                    .getMemberPanel()
                        .table()
                                .assign()
                                    .selectType("User")
                                    .table()
                                        .search()
                                            .byName()
                                                .inputValue("kirk")
                                                .updateSearch()
                                            .and()
                                        .selectCheckboxByName("kirk")
                                        .and()
                                    .clickAdd()
                                .and()
                            .and()
                        .and()
                    .feedback()
                        .isInfo();

        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .showTreeNodeDropDownMenu("Secret Operations")
                            .expandAll()
                        .selectOrgInTree("Warp Speed Research")
                        .and()
                    .getMemberPanel()
                        .selectType("User")
                            .table()
                            .assertTableContainsText("kirk");

        AccountPage accountPage = showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Selenide.screenshot("M9_accountPage");
        Assert.assertTrue(accountPage
                        .form()
                        .compareInputAttributeValues("groups", "Internal Employees",
                                "Essential Documents", "Teleportation", "Time Travel"));

        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .showTreeNodeDropDownMenu("Secret Operations")
                            .expandAll()
                        .showTreeNodeDropDownMenu("Warp Speed Research")
                            .edit()
                                .selectTabInducements()
                                    .clickAddInducement()
                                        .selectType("Role")
                                        .table()
                                            .search()
                                                .byName()
                                                    .inputValue("Secret Projects II")
                                                    .updateSearch()
                                                .and()
                                            .selectCheckboxByName("Secret Projects II")
                                            .and()
                                        .clickAdd()
                                    .and()
                                .clickSave()
                                    .feedback()
                                        .isSuccess();

        Assert.assertTrue(
                showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk")
                        .form()
                        .compareInputAttributeValues("groups", "Internal Employees",
                                "Essential Documents", "Teleportation", "Time Travel"));

        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .selectOrgInTree("Warp Speed Research")
                        .and()
                    .getMemberPanel()
                        .table()
                            .recompute()
                                .clickYes()
                            .and()
                        .and()
                    .and()
                .feedback()
                    .isInfo();

        Assert.assertTrue(
                showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk")
                        .form()
                        .compareInputAttributeValues("groups", "Internal Employees",
                                "Essential Documents", "Teleportation", "Time Travel", "Lucky Numbers",
                                "Presidential Candidates Motivation"));

        Assert.assertTrue(showUser("kirk").selectTabAssignments()
                .selectTypeAllDirectIndirect()
                    .containsIndirectAssignments("Secret Projects II"));
    }

}
