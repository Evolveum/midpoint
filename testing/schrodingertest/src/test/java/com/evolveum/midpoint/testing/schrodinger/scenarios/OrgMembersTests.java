/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import static com.codeborne.selenide.Selenide.$;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgTreePage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

/**
 * Created by honchar.
 */
public class OrgMembersTests extends AbstractSchrodingerTest {

    private static final String CONFIGURATION_DIRECTORY = "./src/test/resources/configuration/";
    private static final String CONFIGURATION_OBJECTS_DIRECTORY = CONFIGURATION_DIRECTORY + "objects/";
    private static final String CONFIGURATION_USERS_DIRECTORY = CONFIGURATION_OBJECTS_DIRECTORY + "users/";
    private static final String CONFIGURATION_ORGS_DIRECTORY = CONFIGURATION_OBJECTS_DIRECTORY + "orgs/";

    private static final File USER_ORG_MEMBER_FILE = new File(CONFIGURATION_USERS_DIRECTORY + "user-org-member.xml");
    private static final File USER_NOT_ORG_MEMBER_FILE = new File(CONFIGURATION_USERS_DIRECTORY + "user-not-org-member.xml");
    private static final File ORG_WITH_MEMBER_FILE = new File(CONFIGURATION_ORGS_DIRECTORY + "org-with-member.xml");
    private static final String ORG_NAME = "TestOrgWithMembers";
    private static final String ORG_WITH_MEMBER_NAME = "Assign member test";
    private static final String USER_NAME = "OrgMembersWithDefaultRelation";

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(USER_ORG_MEMBER_FILE, USER_NOT_ORG_MEMBER_FILE, ORG_WITH_MEMBER_FILE);
    }

    @Test (priority = 1)
    public void createOrgWithinMenuItem(){
        OrgPage newOrgPage = basicPage.newOrgUnit();
        newOrgPage
                .selectTabBasic()
                    .form()
                    .addAttributeValue("name", ORG_NAME)
                    .and()
                .and()
                .clickSave()
                .feedback()
                .isSuccess();
        Assert.assertTrue(basicPage.orgStructure().doesRootOrgExists(ORG_NAME));
    }

    @Test (dependsOnMethods = {"createOrgWithinMenuItem"}, priority = 2)
    public void assignDefaultRelationMember(){
        UserPage user = basicPage.newUser();

        Assert.assertTrue(user.selectTabBasic()
                .form()
                    .addAttributeValue("name", USER_NAME)
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                .feedback()
                .isSuccess());

        OrgTreePage orgTreePage = basicPage.orgStructure();
        orgTreePage
                .selectTabWithRootOrg(ORG_NAME)
                    .getMemberPanel()
                    .assignMember()
                        .table()
                            .search()
                            .byName()
                            .inputValue(USER_NAME)
                            .updateSearch()
                        .and()
                        .selectCheckboxByName(USER_NAME)
                    .and()
                    .clickAdd();
        orgTreePage = basicPage.orgStructure();
        Assert.assertTrue(orgTreePage
                .selectTabWithRootOrg(ORG_NAME)
                    .getMemberPanel()
                        .table()
                        .containsLinksTextPartially(USER_NAME));
    }

    @Test (priority = 3)
    public void assignExistingUserAsMember(){
        basicPage.orgStructure()
                    .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                        .getMemberPanel()
                        .assignMember()
                            .table()
                                .search()   // the goal is to check search on the parent
                                .byName()
                                .inputValue("UniqueNameUserForMemberTest")
                                .updateSearch()
                            .and()
                            .selectCheckboxByName("UniqueNameUserForMemberTest")
                        .and()
                        .clickAdd();

        Assert.assertTrue(basicPage.orgStructure()
                                        .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                                            .getMemberPanel()
                                                .table()
                                                    .containsLinksTextPartially("UniqueNameUserForMemberTest"));
        //test that schrodinger looks correctly for the element inside parent element, not on the whole page
        // (both page and popup window contains tables with Name column, we need to look through Name column in the popup)
        Assert.assertNotNull(basicPage.orgStructure()
                                .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                                    .getMemberPanel()
                                    .assignMember()
                                        .table()
                                            .rowByColumnLabel("Name", "NotMemberUser"));
    }
}
