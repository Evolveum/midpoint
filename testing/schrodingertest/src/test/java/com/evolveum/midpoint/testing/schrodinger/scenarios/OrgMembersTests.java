/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.org.MemberPanel;
import com.evolveum.midpoint.schrodinger.component.org.MemberTable;

import com.evolveum.midpoint.schrodinger.component.org.OrgRootTab;

import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.role.RolePage;

import com.evolveum.midpoint.schrodinger.page.service.ServicePage;

import org.testng.Assert;
import org.testng.annotations.Test;
import com.evolveum.midpoint.schrodinger.page.org.OrgPage;
import com.evolveum.midpoint.schrodinger.page.org.OrgTreePage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
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
    public void test00100createOrgWithinMenuItem(){
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

    @Test (dependsOnMethods = {"test00100createOrgWithinMenuItem"}, priority = 2)
    public void test00200assignDefaultRelationMember(){
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
        orgTreePage
                .selectTabWithRootOrg(ORG_NAME)
                    .getMemberPanel()
                        .table()
                        .assertTableContainsLinkTextPartially(USER_NAME);
    }

    @Test (priority = 3)
    public void test00300assignExistingUserAsMember(){
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

        AssignmentHolderObjectListTable<MemberPanel<OrgRootTab>, AssignmentHolderDetailsPage> membersTable =
                basicPage.orgStructure()
                    .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                        .getMemberPanel()
                            .table()
                                .clickRefreshButton()
                                .search()
                                .byName()
                                .inputValue("UniqueNameUserForMemberTest")
                                .updateSearch()
                            .and();
        Selenide.screenshot("test00300assignExistingUserAsMember_membersPanel");
        Assert.assertTrue(membersTable
                    .containsText("UniqueNameUserForMemberTest"));
    }

    @Test (priority = 4)
    public void test00400createNewUserMemberObject() {
        UserPage newUserPage = (UserPage) basicPage.orgStructure()
                .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                    .getMemberPanel()
                        .newMember()
                            .setType("User")
                            .setRelation("Member")
                            .clickOk();
        newUserPage.selectTabBasic()
                    .form()
                        .addAttributeValue("name", "NewUserAsOrgMember")
                        .and()
                    .and()
                .clickSave()
                .feedback()
                .isSuccess();
        MemberTable<MemberPanel<OrgRootTab>> memberTable = basicPage.orgStructure()
                .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                .getMemberPanel()
                .table();
        memberTable
                            .search()
                            .byName()
                            .inputValue("NewUserAsOrgMember")
                .updateSearch()
                .and()
                .assertTableObjectsCountEquals(1);
        Assert.assertTrue(memberTable.containsText("Member"));
    }

    @Test (priority = 5)
    public void test00500createNewRoleMemberObject() {
        RolePage newRolePage = (RolePage) basicPage.orgStructure()
                .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                    .getMemberPanel()
                        .newMember()
                            .setType("Role")
                            .setRelation("Manager")
                            .clickOk();
        newRolePage.selectTabBasic()
                    .form()
                        .addAttributeValue("name", "NewRoleAsOrgManager")
                        .and()
                    .and()
                .clickSave()
                .feedback()
                .isSuccess();
        MemberPanel<OrgRootTab> memberPanel = basicPage.orgStructure()
                .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                .getMemberPanel();
        MemberTable<MemberPanel<OrgRootTab>> memberTable = memberPanel
                .table();
        memberPanel.selectType("All");
        memberTable
                            .search()
                            .byName()
                            .inputValue("NewRoleAsOrgManager")
                .updateSearch()
                .and()
                .assertTableObjectsCountEquals(1);
        Assert.assertTrue(memberTable.containsText("Manager"));
    }

    @Test (priority = 6)
    public void test00600createNewOrgOwnerObject() {
        OrgPage newOrgPage = (OrgPage) basicPage.orgStructure()
                .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                    .getMemberPanel()
                        .newMember()
                            .setType("Organization")
                            .setRelation("Owner")
                            .clickOk();
        newOrgPage.selectTabBasic()
                    .form()
                        .addAttributeValue("name", "NewOrgAsOrgOwner")
                        .and()
                    .and()
                .clickSave()
                .feedback()
                .isSuccess();
        MemberPanel<OrgRootTab> memberPanel = basicPage.orgStructure()
                .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                .getMemberPanel();
        MemberTable<MemberPanel<OrgRootTab>> memberTable = memberPanel
                .table();
        memberPanel.selectType("All");
        memberTable
                            .search()
                            .byName()
                            .inputValue("NewOrgAsOrgOwner")
                .updateSearch()
                .and()
                .assertTableObjectsCountEquals(1);
        Assert.assertTrue(memberTable.containsText("Owner"));
    }

    @Test (priority = 7)
    public void test00700createNewServiceApproverObject() {
        ServicePage newServicePage = (ServicePage) basicPage.orgStructure()
                .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                    .getMemberPanel()
                        .newMember()
                            .setType("Service")
                            .setRelation("Approver")
                            .clickOk();
        newServicePage.selectTabBasic()
                    .form()
                        .addAttributeValue("name", "NewServiceAsOrgApprover")
                        .and()
                    .and()
                .clickSave()
                .feedback()
                .isSuccess();
        MemberPanel<OrgRootTab> memberPanel = basicPage.orgStructure()
                .selectTabWithRootOrg(ORG_WITH_MEMBER_NAME)
                .getMemberPanel();
        MemberTable<MemberPanel<OrgRootTab>> memberTable = memberPanel
                .table();
        memberPanel.selectType("All");
        memberTable
                            .search()
                            .byName()
                            .inputValue("NewServiceAsOrgApprover")
                .updateSearch()
                .and()
                .assertTableObjectsCountEquals(1);
        Assert.assertTrue(memberTable.containsText("Approver"));
    }
}
