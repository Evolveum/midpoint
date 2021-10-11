/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger.page;

import com.codeborne.selenide.Condition;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.configuration.InfrastructureTab;
import com.evolveum.midpoint.schrodinger.component.configuration.NotificationsTab;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;
import com.evolveum.midpoint.schrodinger.page.login.*;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selenide.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoginPageTest extends AbstractLoginPageTest {

    private static final File SEC_QUES_RESET_PASS_SECURITY_POLICY = new File("src/test/resources/configuration/objects/securitypolicies/policy-secururity-question-reset-pass.xml");
    private static final File MAIL_NONCE_RESET_PASS_SECURITY_POLICY = new File("src/test/resources/configuration/objects/securitypolicies/policy-nonce-reset-pass.xml");

    @Test
    public void test020selfRegistration() throws IOException, InterruptedException {
        System.setProperty("midpoint.schrodinger","true");
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        open("/login");
        open("/");
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        SelfRegistrationPage registrationPage = login.register();
        registrationPage.setGivenName("Test").setFamilyName("User").setEmail("test.user@evolveum.com").setPassword("5ecr3t").submit();
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        basicPage.feedback().isSuccess();
        String notification = readLastNotification();
        String linkTag = "link='";
        String link = notification.substring(notification.indexOf(linkTag) + linkTag.length(), notification.lastIndexOf("''"));
        open(link);
        $(Schrodinger.byDataId("successPanel")).waitUntil(Condition.visible, MidPoint.TIMEOUT_MEDIUM_6_S);
        String actualUrl = basicPage.getCurrentUrl();
        Assert.assertTrue(actualUrl.endsWith("/registration"));
    }

    @Test
    public void test030resetPassowordMailNonce() throws IOException, InterruptedException {
        basicPage.loggedUser().logoutIfUserIsLogin();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        FormLoginPage login = midPoint.formLogin();
        open("/login");
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        open("/");
        login.forgotPassword();
        $(Schrodinger.byDataId("email")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(MAIL_OF_ENABLED_USER);
        $(Schrodinger.byDataId("submitButton")).click();
        TimeUnit.SECONDS.sleep(6);
        String notification = readLastNotification();
        String bodyTag = "body='";
        String link = notification.substring(notification.indexOf(bodyTag) + bodyTag.length(), notification.lastIndexOf("'"));
        open(link);
        String actualUrl = basicPage.getCurrentUrl();
        Assert.assertTrue(actualUrl.endsWith("/resetPassword"));
    }

    @Test
    public void test031resetPassowordSecurityQuestion() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        open("/login");
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        open("/");
        login.loginWithReloadLoginPage("administrator", "5ecr3t");
        importObject(SEC_QUES_RESET_PASS_SECURITY_POLICY, true);
        basicPage.loggedUser().logoutIfUserIsLogin();
        login.forgotPassword();
        $(Schrodinger.byDataId("username")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(NAME_OF_ENABLED_USER);
        $(Schrodinger.byDataId("email")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(MAIL_OF_ENABLED_USER);
        $(Schrodinger.byDataId("submitButton")).click();
        $(Schrodinger.byDataId("answerTF")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue("10");
        $(Schrodinger.byDataId("send")).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        String actualUrl = basicPage.getCurrentUrl();
        Assert.assertTrue(actualUrl.endsWith("/resetpasswordsuccess"));
    }

    @Test
    public void test040changeLanguageFormPage() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        open("/login");
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        open("/");

        login.changeLanguage("de");

        $(By.cssSelector(".btn.btn-primary")).shouldHave(Condition.value("Anmelden"));
    }

    @Test
    public void test041changeLanguageSamlSelectPage() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        SamlSelectPage login = midPoint.samlSelect();
        login.goToUrl();

        login.changeLanguage("us");

        $(By.xpath("/html/body/div[2]/div/section/div[2]/div/div/div/h4"))
                .shouldHave(Condition.text("Select an Identity Provider"));
    }

    @Override
    protected File getSecurityPolicyMailNonceResetPass() {
        return MAIL_NONCE_RESET_PASS_SECURITY_POLICY;
    }
}
