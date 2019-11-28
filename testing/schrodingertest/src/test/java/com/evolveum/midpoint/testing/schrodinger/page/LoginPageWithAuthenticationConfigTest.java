/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static com.codeborne.selenide.Selenide.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoginPageWithAuthenticationConfigTest extends AbstractLoginPageTest {

    private static final File FLEXIBLE_AUTHENTICATION_SECURITY_POLICY = new File("src/test/resources/configuration/objects/securitypolicies/flexible-authentication-policy.xml");
    private static final File USER_WITHOUT_SUPERUSER = new File("src/test/resources/configuration/objects/users/user-without-superuser.xml");

    @BeforeClass
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        importObject(FLEXIBLE_AUTHENTICATION_SECURITY_POLICY, true);
        importObject(USER_WITHOUT_SUPERUSER, true);
    }

    @Test
    public void failWholeAuthenticationFlow() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        open("/auth/emergency/internalLoginForm");
        open("/");
        FormLoginPage login = midPoint.formLogin();
        login.login("bad_username", "secret");

        FeedbackBox feedback = login.feedback();
        Assert.assertTrue(feedback.isError("0"));


        login.login("bad_username", "secret");
        Assert.assertTrue(feedback.isError("0"));
        Assert.assertTrue(feedback.isError("1"));
    }

    @Test
    public void loginAndLogoutForAdministrators() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        open("/auth/emergency/internalLoginForm");
        FormLoginPage login = midPoint.formLogin();
        login.login("administrator", "5ecr3t");
        basicPage.loggedUser().logout();
        screenshot("logout-admin");
        Assert.assertFalse($(".dropdown.user.user-menu").exists());
    }

    @Test
    public void negativeLoginForAdministrators() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        open("/auth/emergency/internalLoginForm");
        unsuccessfulLogin("user_without_superuser", "5ecr3t");
    }

}
