/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger.page;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.configuration.InfrastructureTab;
import com.evolveum.midpoint.schrodinger.component.configuration.NotificationsTab;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.login.MailNoncePage;
import com.evolveum.midpoint.schrodinger.page.login.SecurityQuestionsPage;
import com.evolveum.midpoint.schrodinger.page.login.SelfRegistrationPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selenide.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoginPageWithAuthenticationConfigTest extends AbstractLoginPageTest {

    private static final File FLEXIBLE_AUTHENTICATION_SEC_QUES_RESET_PASS_SECURITY_POLICY = new File("src/test/resources/configuration/objects/securitypolicies/flexible-authentication-policy-secururity-question-reset-pass.xml");
    private static final File FLEXIBLE_AUTHENTICATION_MAIL_NONCE_RESET_PASS_SECURITY_POLICY = new File("src/test/resources/configuration/objects/securitypolicies/flexible-authentication-policy-nonce-reset-pass.xml");
    private static final File MAIL_NONCE_VALUE_POLICY = new File("src/test/resources/configuration/objects/valuepolicies/mail-nonce.xml");
    private static final File USER_WITHOUT_SUPERUSER = new File("src/test/resources/configuration/objects/users/user-without-superuser.xml");
    private static final File SYSTEM_CONFIG_WITH_NOTIFICATION = new File("src/test/resources/configuration/objects/systemconfig/system-configuration-notification.xml");
    private static final File CREATE_NAME_OBJECT_TEMPLATE = new File("src/test/resources/configuration/objects/objecttemplate/create-name-after-self-reg.xml");
    private static final File NOTIFICATION_FILE = new File("./target/notification.txt");

    private static final String NAME_OF_ENABLED_USER = "enabled_user";
    private static final String MAIL_OF_ENABLED_USER = "enabled_user@evolveum.com";

    @BeforeClass
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        importObject(MAIL_NONCE_VALUE_POLICY, true);
        importObject(FLEXIBLE_AUTHENTICATION_MAIL_NONCE_RESET_PASS_SECURITY_POLICY, true);
        importObject(USER_WITHOUT_SUPERUSER, true);
        importObject(CREATE_NAME_OBJECT_TEMPLATE, true);
        importObject(SYSTEM_CONFIG_WITH_NOTIFICATION, true);
        basicPage.infrastructure();
        SystemPage systemPage = new SystemPage();
        PrismForm<InfrastructureTab> infrastructureForm = systemPage.infrastructureTab().form();
        infrastructureForm.showEmptyAttributes("Infrastructure");
        infrastructureForm.addAttributeValue("publicHttpUrlPattern", getConfiguration().getBaseUrl());
        File notificationFile = NOTIFICATION_FILE;
        notificationFile.createNewFile();
        NotificationsTab notificationTab = systemPage.notificationsTab();
        notificationTab.setRedirectToFile(notificationFile.getAbsolutePath());
        systemPage.save();
        Assert.assertTrue(systemPage.feedback().isSuccess());
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
    public void test020loginAndLogoutForAdministrators() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        open("/auth/emergency/internalLoginForm");
        FormLoginPage login = midPoint.formLogin();
        login.login("administrator", "5ecr3t");
        basicPage.loggedUser().logout();
        Assert.assertFalse($(".dropdown.user.user-menu").exists());
    }

    @Test
    public void test021negativeLoginForAdministrators() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        open("/auth/emergency/internalLoginForm");
        unsuccessfulLogin("user_without_superuser", "5ecr3t");
    }

    @Test
    public void test030changePassowordMailNonce() throws IOException, InterruptedException {
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        MailNoncePage mailNonce = (MailNoncePage) login.forgotPassword();
        mailNonce.setMail(MAIL_OF_ENABLED_USER);
        TimeUnit.SECONDS.sleep(2);
        String notification = readLastNotification();
        String bodyTag = "body='";
        String link = notification.substring(notification.indexOf(bodyTag) + bodyTag.length(), notification.lastIndexOf("'"));
        open(link);
        String actualUrl = basicPage.getCurrentUrl();
        Assert.assertTrue(actualUrl.endsWith("/resetPassword"));
    }

    @Test
    public void test031changePassowordSecurityQuestion() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        open("/login");
        open("/");
        login.loginWithReloadLoginPage("administrator", "5ecr3t");
        importObject(FLEXIBLE_AUTHENTICATION_SEC_QUES_RESET_PASS_SECURITY_POLICY, true);
        basicPage.loggedUser().logoutIfUserIsLogin();
        SecurityQuestionsPage securityQuestion = (SecurityQuestionsPage) login.forgotPassword();
        securityQuestion.setUsername(NAME_OF_ENABLED_USER)
            .setAnswer(0, "10")
            .submit();
        String actualUrl = basicPage.getCurrentUrl();
        Assert.assertTrue(actualUrl.endsWith("/resetPassword"));
    }

    @Test
    public void test040selfRegistration() throws IOException, InterruptedException {
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        open("/login");
        open("/");
        TimeUnit.SECONDS.sleep(2);
        SelfRegistrationPage registrationPage = login.register();
        registrationPage.setGivenName("Test").setFamilyName("User").setEmail("test.user@evolveum.com").setPassword("5ecr3t").submit();
        TimeUnit.SECONDS.sleep(4);
        String notification = readLastNotification();
        String usernameTag = "username='";
        String linkTag = "link='";
        String username = notification.substring(notification.indexOf(usernameTag) + usernameTag.length(), notification.lastIndexOf("', " + linkTag));
        String link = notification.substring(notification.indexOf(linkTag) + linkTag.length(), notification.lastIndexOf("''"));
        open(link);
        $(Schrodinger.byDataId("successPanel")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        String actualUrl = basicPage.getCurrentUrl();
        Assert.assertTrue(actualUrl.endsWith("/registration/result"));
    }

    private String readLastNotification() throws IOException {
        String separator = "============================================";
        byte[] encoded = Files.readAllBytes(Paths.get(NOTIFICATION_FILE.getAbsolutePath()));
        String notifications = new String(encoded, Charset.defaultCharset());
        return notifications.substring(notifications.lastIndexOf(separator) + separator.length(), notifications.length()-1);
    }

}
