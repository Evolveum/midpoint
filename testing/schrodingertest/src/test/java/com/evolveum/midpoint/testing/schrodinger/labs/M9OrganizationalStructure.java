/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgTreePage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author skublik
 */

public class M9OrganizationalStructure extends AbstractLabTest{

    private static final Logger LOG = LoggerFactory.getLogger(M9OrganizationalStructure.class);

    private static final File ARCHETYPE_ORG_COMPANY_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-company.xml");
    private static final File ARCHETYPE_ORG_FUNCTIONAL_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-functional.xml");
    private static final File ARCHETYPE_ORG_GROUP_LIST_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-group-list.xml");
    private static final File ARCHETYPE_ORG_GROUP_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-group.xml");
    private static final File ORG_EXAMPLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "org/org-example.xml");
    private static final File ORG_SECRET_OPS_FILE = new File(LAB_OBJECTS_DIRECTORY + "org/org-secret-ops.xml");

    @AfterClass
    @Override
    public void afterClass() {
        super.afterClass();

        midPoint.formLogin().loginWithReloadLoginPage(username, password);

        LOG.info("After: Login name " + username + " pass " + password);

        AboutPage aboutPage = basicPage.aboutPage();
        aboutPage
                .clickSwitchToFactoryDefaults()
                .clickYes();
    }

    @BeforeClass
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        csv1TargetFile = new File("/home/lskublik/Documents/Evolveum/actual/master/05-02-2020/midpoint/testing/schrodingertest/target/midpoint-home/schrodinger/labTests/csv-1.csv");
        csv2TargetFile = new File ("/home/lskublik/Documents/Evolveum/actual/master/05-02-2020/midpoint/testing/schrodingertest/target/midpoint-home/schrodinger/labTests/csv-2.csv");
        csv3TargetFile = new File ("/home/lskublik/Documents/Evolveum/actual/master/05-02-2020/midpoint/testing/schrodingertest/target/midpoint-home/schrodinger/labTests/csv-3.csv");
        hrTargetFile = new File ("/home/lskublik/Documents/Evolveum/actual/master/05-02-2020/midpoint/testing/schrodingertest/target/midpoint-home/schrodinger/labTests/source.csv");
    }

    @Test
    public void test0901ImportStaticOrgStructure() {
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

    @Test(dependsOnMethods = {"test0901ImportStaticOrgStructure"})
    public void test0902CreateStaticOrgStructure() {
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
                        .editOrg("Warp Speed Research")
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

}
