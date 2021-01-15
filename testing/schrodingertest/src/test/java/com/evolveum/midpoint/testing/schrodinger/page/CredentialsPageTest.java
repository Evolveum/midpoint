/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar
 */
public class CredentialsPageTest extends AbstractSchrodingerTest {
    private static final File CHANGE_USER_PASSWORD_TEST_FILE = new File("./src/test/resources/component/objects/users/change-user-password-test-user.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(CHANGE_USER_PASSWORD_TEST_FILE);
    }

    @Test (priority = 1)
    public void test0010changeUserPasswordSuccessfully() {
        basicPage.loggedUser().logout();
        midPoint.formLogin()
                .loginWithReloadLoginPage("CredentialsPageTestUser", "password");
        basicPage.credentials()
                .passwordTab()
                    .changePasswordPanel()
                        .setOldPasswordValue("password")
                        .setNewPasswordValue("password1")
                        .setRepeatPasswordValue("password1")
                        .and()
                    .and()
                .save()
                .feedback()
                .assertSuccess();
        basicPage.loggedUser().logout();
        midPoint.formLogin()
                .loginWithReloadLoginPage("CredentialsPageTestUser", "password1");
        Assert.assertTrue(basicPage.userMenuExists(), "User should be logged in");
    }

    @Test(priority = 2, dependsOnMethods = {"test0010changeUserPasswordSuccessfully"})
    public void test0020changeUserPasswordWrongOldPassword() {
        basicPage.loggedUser().logout();
        midPoint.formLogin()
                .loginWithReloadLoginPage("CredentialsPageTestUser", "password1");
        Assert.assertTrue(basicPage.credentials()
                .passwordTab()
                    .changePasswordPanel()
                        .setOldPasswordValue("wrongPassword")
                        .setNewPasswordValue("passwordNew")
                        .setRepeatPasswordValue("passwordNew")
                .and()
                    .and()
                .save()
                .feedback()
                .isError());
        basicPage.loggedUser().logout();
        midPoint.formLogin()
                .loginWithReloadLoginPage("CredentialsPageTestUser", "passwordNew");
        Assert.assertFalse(basicPage.userMenuExists(), "User should not be logged in with new password");
        midPoint.formLogin()
                .loginWithReloadLoginPage("CredentialsPageTestUser", "password1");
        Assert.assertTrue(basicPage.userMenuExists(), "User should be logged in with old password");
    }

    @Test(priority = 3, dependsOnMethods = {"test0010changeUserPasswordSuccessfully"})
    public void test0030changeUserPasswordNewPasswordDoesntMatch() {
        basicPage.loggedUser().logout();
        midPoint.formLogin()
                .loginWithReloadLoginPage("CredentialsPageTestUser", "password1");
        Assert.assertTrue(basicPage.credentials()
                .passwordTab()
                    .changePasswordPanel()
                        .setOldPasswordValue("password1")
                        .setNewPasswordValue("passwordNew1")
                        .setRepeatPasswordValue("passwordNew2")
                .and()
                    .and()
                .save()
                .feedback()
                .isError());
        basicPage.loggedUser().logout();
        midPoint.formLogin()
                .loginWithReloadLoginPage("CredentialsPageTestUser", "passwordNew1");
        Assert.assertFalse(basicPage.userMenuExists(), "User should not be logged in with new password");
        midPoint.formLogin()
                .loginWithReloadLoginPage("CredentialsPageTestUser", "password1");
        Assert.assertTrue(basicPage.userMenuExists(), "User should be logged in with old password");
    }

}
