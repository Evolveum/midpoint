/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.page.self.HomePage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by matus on 5/9/2018.
 */
public class UserAccountTests extends AccountTests {

    private static final String DISABLE_MP_USER_DEPENDENCY = "disableUser";
    private static final String ENABLE_MP_USER_DEPENDENCY = "enableUser";
    private static final String BULK_DISABLE_MP_USER_DEPENDENCY = "bulkDisableUsers";

    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY}, groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void modifyUserAttribute(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                    .table()
                        .search()
                        .byName()
                            .inputValue(TEST_USER_MIKE_NAME)
                            .updateSearch()
                    .and()
                    .clickByName(TEST_USER_MIKE_NAME)
                        .selectTabBasic()
                            .form()
                            .changeAttributeValue("Given name","Michelangelo","Michael Angelo")
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess();
    }
    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY, MODIFY_ACCOUNT_PASSWORD_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void modifyUserPassword(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                    .table()
                        .search()
                        .byName()
                            .inputValue(TEST_USER_MIKE_NAME)
                            .updateSearch()
                    .and()
                    .clickByName(TEST_USER_MIKE_NAME)
                        .selectTabBasic()
                            .form()
                            .showEmptyAttributes("Password")
                            .addProtectedAttributeValue("Value","S36re7")
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess();

    }

    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void disableUser(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                .table()
                    .search()
                    .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                        .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabBasic()
                        .form()
                        .showEmptyAttributes("Activation")
                        .selectOption("Administrative status","Disabled")
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test (dependsOnMethods = {DISABLE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void enableUser(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                .table()
                    .search()
                    .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                        .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabBasic()
                        .form()
                        .showEmptyAttributes("Activation")
                        .selectOption("Administrative status","Enabled")
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test (dependsOnMethods = {ENABLE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void bulkDisableUsers(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .selectAll()
                .and()
                    .table()
                        .disableUser()
                            .clickYes()
                        .and()
                    .feedback()
                        .isSuccess()
            ;
    }

    @Test (dependsOnMethods = {BULK_DISABLE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void bulkEnableUsers(){
        ListUsersPage usersPage = basicPage.listUsers();
            usersPage
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .selectAll()
                .and()
                    .table()
                    .enableUser()
                        .clickYes()
                    .and()
                .feedback()
                .isSuccess()
        ;
    }

    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void searchUser(){
    ListUsersPage usersPage = basicPage.listUsers();
    usersPage
                       .table()
                            .search()
                                .byName()
                                .inputValue(TEST_USER_MIKE_NAME)
                            .updateSearch()
                       .and()
                       .assertCurrentTableContains(TEST_USER_MIKE_NAME);
    }

    @Test (dependsOnMethods = {CREATE_MP_USER_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void searchUserFromHome(){
        HomePage homePage = basicPage.home();
        homePage
                    .search()
                        .clickSearchFor()
                    .clickUsers()
                    .inputValue(TEST_USER_MIKE_NAME)
                        .clickSearch()
                        .assertCurrentTableContains(TEST_USER_MIKE_NAME);

    }

    @Test (dependsOnGroups = {TEST_GROUP_BEFORE_USER_DELETION})
    public void bulkDeleteUsers(){
        ListUsersPage usersPage = basicPage.listUsers();
        usersPage
                .table()
                    .search()
                        .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .selectAll()
                .and()
                    .table()
                        .deleteUser()
                        .clickYes()
                    .and()
                .feedback()
                    .isSuccess();
    }
}
