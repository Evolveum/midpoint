/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgTreePage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author skublik
 */

public class M9OrganizationalStructure extends AbstractLabTest{

    private static final File ARCHETYPE_ORG_COMPANY_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-company.xml");
    private static final File ARCHETYPE_ORG_FUNCTIONAL_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-functional.xml");
    private static final File ARCHETYPE_ORG_GROUP_LIST_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-group-list.xml");
    private static final File ARCHETYPE_ORG_GROUP_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-group.xml");
    private static final File ORG_EXAMPLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "org/org-example.xml");
    private static final File ORG_SECRET_OPS_FILE = new File(LAB_OBJECTS_DIRECTORY + "org/org-secret-ops.xml");

    @Test(groups={"M9"}, dependsOnGroups={"M8"})
    public void mod09test01ImportStaticOrgStructure() {
        importObject(ARCHETYPE_ORG_FUNCTIONAL_FILE, true, true);
        importObject(ARCHETYPE_ORG_COMPANY_FILE, true);
        importObject(ARCHETYPE_ORG_GROUP_FILE, true);
        importObject(ARCHETYPE_ORG_GROUP_LIST_FILE, true);

        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        login.login(getUsername(), getPassword());

        importObject(ORG_EXAMPLE_FILE, true);

        OrgTreePage orgTree = basicPage.orgStructure();
        Assert.assertTrue(orgTree.selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                .getOrgHierarchyPanel()
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

    @Test(dependsOnMethods = {"mod09test01ImportStaticOrgStructure"}, groups={"M9"}, dependsOnGroups={"M8"})
    public void mod09test02CreateStaticOrgStructure() {
        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .expandOrg("Secret Operations")
                        .selectOrgInTree("Transportation and Logistics Department")
                        .and()
                    .getMemberPanel()
                        .newMember("Create Organization type member with default relation");
        new OrgPage()
                .selectTabBasic()
                    .form()
                        .addAttributeValue(OrgType.F_NAME, "0919")
                        .addAttributeValue(OrgType.F_DISPLAY_NAME, "Warp Speed Research")
                        .and()
                    .and()
                .clickSave()
                    .feedback()
                        .isSuccess();

        Assert.assertTrue(basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .expandOrg("Secret Operations")
                        .expandOrg("Transportation and Logistics Department")
                        .showTreeNodeDropDownMenu("Warp Speed Research")
                            .edit()
                                .selectTabBasic()
                                    .form()
                                        .compareInputAttributeValue("name", "0919"));

        showUser("kirk").selectTabAssignments()
                .clickAddAssignemnt("New Organization type assignment with default relation")
                    .table()
                        .paging()
                            .next()
                            .and()
                        .and()
                    .table()
                        .selectCheckboxByName("0919")
                        .and()
                    .clickAdd()
                .and()
            .clickSave()
                .feedback()
                    .isSuccess();

        Assert.assertTrue(basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .expandOrg("Secret Operations")
                        .expandOrg("Transportation and Logistics Department")
                        .selectOrgInTree("Warp Speed Research")
                        .and()
                    .getMemberPanel()
                        .selectType("User")
                        .table()
                            .containsText("kirk"));

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

    @Test(dependsOnMethods = {"mod09test02CreateStaticOrgStructure"}, groups={"M9"}, dependsOnGroups={"M8"})
    public void mod09test03OrganizationActingAsARole() {
        Assert.assertFalse(basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .expandOrg("Secret Operations")
                        .expandOrg("Transportation and Logistics Department")
                        .selectOrgInTree("Warp Speed Research")
                        .and()
                    .getMemberPanel()
                        .selectType("User")
                            .table()
                            .containsText("kirk"));

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
                            .clickHeaderActionDropDown()
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

        Assert.assertTrue(basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .expandOrg("Secret Operations")
                        .expandOrg("Transportation and Logistics Department")
                        .selectOrgInTree("Warp Speed Research")
                        .and()
                    .getMemberPanel()
                        .selectType("User")
                            .table()
                            .containsText("kirk"));

        Assert.assertTrue(
                showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk")
                        .form()
                        .compareInputAttributeValues("groups", "Internal Employees",
                                "Essential Documents", "Teleportation", "Time Travel"));

        basicPage.orgStructure()
                .selectTabWithRootOrg("ExAmPLE, Inc. - Functional Structure")
                    .getOrgHierarchyPanel()
                        .expandOrg("Secret Operations")
                        .expandOrg("Transportation and Logistics Department")
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
                            .clickHeaderActionDropDown()
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
