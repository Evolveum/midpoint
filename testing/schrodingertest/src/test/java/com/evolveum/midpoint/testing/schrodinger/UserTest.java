/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.DateTimePanel;
import com.evolveum.midpoint.schrodinger.component.common.DelegationDetailsPanel;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserTest extends AbstractSchrodingerTest {

    private static final String LOCALIZATION_TEST_USER_NAME_ORIG = "localizationTestUserName";
    private static final String LOCALIZATION_TEST_USER_NAME_DE = "localizationTestUserNameDe";
    private static final String LOCALIZATION_VALUE = "de";
    private static final File DELEGATE_FROM_USER_FILE = new File("./src/test/resources/component/objects/users/delegate-from-user.xml");
    private static final File DELEGATE_TO_USER_FILE = new File("./src/test/resources/component/objects/users/delegate-to-user.xml");
    private static final File DELEGABLE_END_USER_ROLE_FILE = new File("./src/test/resources/component/objects/roles/delegable-end-user-role.xml");
    private static final File DELEGATE_END_USER_ROLE_FROM_USER_FILE = new File("./src/test/resources/component/objects/users/delegate-end-user-role-from-user.xml");
    private static final File DELEGATE_END_USER_ROLE_TO_USER_FILE = new File("./src/test/resources/component/objects/users/delegate-end-user-role-to-user.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(DELEGATE_FROM_USER_FILE, DELEGATE_TO_USER_FILE,
                DELEGABLE_END_USER_ROLE_FILE, DELEGATE_END_USER_ROLE_FROM_USER_FILE, DELEGATE_END_USER_ROLE_TO_USER_FILE);
    }

    @Test
    public void test0010createUser() {

        //@formatter:off
        UserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                    .addAttributeValue("name", "jdoe222323")
                    .addAttributeValue(UserType.F_GIVEN_NAME, "john")
                    .addAttributeValue(UserType.F_FAMILY_NAME, "doe")
                    .and()
                .and()
            .clickSave();

        ListUsersPage usersPage = basicPage.listUsers();
        PrismForm<AssignmentHolderBasicTab<UserPage>> userForm = usersPage
                .table()
                .search()
                .byName()
                .inputValue("jdoe222323")
                .updateSearch()
                .and()
                .clickByName("jdoe222323")
                .selectTabBasic()
                .form();
        userForm.assertInputAttributeValueMatches("name", "jdoe222323");
        userForm.assertInputAttributeValueMatches("givenName", "john");
        userForm.assertInputAttributeValueMatches("familyName", "doe");

    }

    @Test //covers MID-5845
    public void test0020isLocalizedPolystringValueDisplayed(){
        UserPage user = basicPage.newUser();

        Assert.assertTrue(
                user.selectTabBasic()
                        .form()
                        .addAttributeValue("name", LOCALIZATION_TEST_USER_NAME_ORIG)
                        .setPolyStringLocalizedValue(UserType.F_NAME, LOCALIZATION_VALUE, LOCALIZATION_TEST_USER_NAME_DE)
                        .and()
                        .and()
                        .clickSave()
                        .feedback()
                        .isSuccess()
        );

        basicPage.loggedUser().logout();
        FormLoginPage loginPage = midPoint.formLogin();
        loginPage.loginWithReloadLoginPage(getUsername(), getPassword(), LOCALIZATION_VALUE);

        ListUsersPage usersPage = basicPage.listUsers();
        usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(LOCALIZATION_TEST_USER_NAME_ORIG)
                        .updateSearch()
                    .and()
                    .clickByName(LOCALIZATION_TEST_USER_NAME_ORIG)
                        .selectTabBasic()
                            .form()
                                .assertInputAttributeValueMatches("name", LOCALIZATION_TEST_USER_NAME_DE);
    }

    @Test
    public void test0030createDelegationTest() {
        showUser("DelegateFromUser")
                .selectTabDelegations()
                    .clickAddDelegation()
                        .table()
                            .search()
                            .byName()
                            .inputValue("DelegateToUser")
                            .updateSearch()
                        .and()
                        .clickByName("DelegateToUser")
                    .and()
                .clickSave()
                .feedback()
                .assertSuccess();

        DelegationDetailsPanel delegationDetailsPanel = showUser("DelegateToUser")
                .selectTabDelegatedToMe()
                    .getDelegationDetailsPanel("DelegateFromUser")
                    .expandDetailsPanel("DelegateFromUser");
        delegationDetailsPanel.assertAssignmentPrivilegesNotSelected();
        delegationDetailsPanel.assertAssignmentLimitationsNotSelected();
        delegationDetailsPanel.assertApprovalWorkItemsSelected();
        delegationDetailsPanel.assertCertificationWorkItemsSelected();

        Assert.assertFalse(delegationDetailsPanel.isDescriptionEnabled(), "Description should be disabled");
        Assert.assertTrue(delegationDetailsPanel.isValidFromPanelDisabled(), "Valid from panel should be disabled");

        DelegationDetailsPanel delegationDetailsFromUser = showUser("DelegateFromUser")
                .selectTabDelegations()
                .getDelegationDetailsPanel("DelegateToUser")
                .expandDetailsPanel("DelegateToUser");

        delegationDetailsFromUser.assertAssignmentPrivilegesNotSelected();
        delegationDetailsFromUser.assertAssignmentLimitationsNotSelected();
        delegationDetailsFromUser.assertApprovalWorkItemsSelected();
        delegationDetailsFromUser.assertCertificationWorkItemsSelected();

        Assert.assertFalse(delegationDetailsFromUser.isDescriptionEnabled(), "Description should be disabled");
        Assert.assertTrue(delegationDetailsFromUser.isValidFromPanelDisabled(), "Valid from panel should be disabled");
    }

    @Test
    public void test0040delegateAssignmentPrivileges() {
        basicPage.loggedUser().logout();
        Assert.assertTrue(midPoint.formLogin().login("DelegateEndUserRoleToUser", "password")
                        .feedback()
                        .isError(),
                "User shouldn't login, doesn't has rights yet");
        midPoint.formLogin().login(username, password);

        showUser("DelegateEndUserRoleFromUser")
                .selectTabDelegations()
                    .clickAddDelegation()
                        .table()
                            .search()
                            .byName()
                            .inputValue("DelegateEndUserRoleToUser")
                            .updateSearch()
                        .and()
                        .clickByName("DelegateEndUserRoleToUser")
                            .getDelegationDetailsPanel("DelegateEndUserRoleToUser")
                            .getValidFromPanel()
                            .setDateTimeValue("11/11/2019", "10", "30", DateTimePanel.AmOrPmChoice.PM)
                            .and()
                        .and()
                    .and()
                .clickSave()
                .feedback()
                .assertSuccess();

        basicPage.loggedUser().logout();
        Assert.assertTrue(midPoint.formLogin().login("DelegateEndUserRoleToUser", "password")
                        .userMenuExists(),
                "User should be logged in, he has delegated end user role");
    }

}
