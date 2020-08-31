/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;

import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author skublik
 */

public class M11SystemConfiguration extends AbstractLabTest {

    private static final Logger LOG = LoggerFactory.getLogger(M11SystemConfiguration.class);

    private static final File SYSTEM_CONFIGURATION_FILE_11_1 = new File(LAB_OBJECTS_DIRECTORY + "systemConfiguration/system-configuration-11-1.xml");
    private static final File SYSTEM_CONFIGURATION_FILE_11_2 = new File(LAB_OBJECTS_DIRECTORY + "systemConfiguration/system-configuration-11-2.xml");
    private static final File SYSTEM_CONFIGURATION_FILE_11_3 = new File(LAB_OBJECTS_DIRECTORY + "systemConfiguration/system-configuration-11-3.xml");
    private static final File OBJECT_COLLECTION_ACTIVE_EMP_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectCollections/objectCollection-active-employees.xml");
    private static final File OBJECT_COLLECTION_INACTIVE_EMP_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectCollections/objectCollection-inactive-employees.xml");
    private static final File OBJECT_COLLECTION_FORMER_EMP_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectCollections/objectCollection-former-employees.xml");

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

    @Test(groups={"M11"}, dependsOnGroups={"M10"})
    public void mod11test01ConfiguringNotifications() throws IOException {
        showTask("HR Synchronization").clickResume();

        notificationFile = new File(getTestTargetDir(), NOTIFICATION_FILE_NAME);
        notificationFile.createNewFile();

        importObject(SYSTEM_CONFIGURATION_FILE_11_1, true);

        basicPage.notifications()
                .setRedirectToFile(notificationFile.getAbsolutePath())
                .and()
            .save()
                .feedback()
                    .isSuccess();

        FileUtils.copyFile(HR_SOURCE_FILE_11_1, hrTargetFile);
        Selenide.sleep(MidPoint.TIMEOUT_LONG_1_M);

        String notification = readBodyOfLastNotification();

        String startOfNotification = "Notification about user-related operation (status: SUCCESS)\n"
                + "\n"
                + "User: Chuck Norris (X000997, oid ";

        String endOfNotification = "The user record was created with the following data:\n"
                + " - Name: X000997\n"
                + " - Full name: Chuck Norris\n"
                + " - Given name: Chuck\n"
                + " - Family name: Norris\n"
                + " - Title: Application Developer\n"
                + " - Email: chuck.norris@example.com\n"
                + " - Employee Number: 000997\n"
                + " - Cost Center: 0211\n"
                + " - Organizational Unit: Java Development\n"
                + " - Extension:\n"
                + "    - Organizational Path: 0200:0210:0211\n"
                + "    - Is Manager: false\n"
                + "    - Employee Status: A\n"
                + " - Credentials:\n"
                + "    - Password:\n"
                + "       - Value: (protected string)\n"
                + " - Activation:\n"
                + "    - Administrative status: ENABLED\n"
                + "    - Valid from: Jul 15, 2010, 8:20:00 AM\n"
                + " - Assignment #1:\n"
                + "    - Target: Employee (archetype) [default]\n"
                + " - Assignment #2:\n"
                + "    - Target: ACTIVE (org) [default]\n"
                + " - Assignment #3:\n"
                + "    - Target: 0211 (org) [default]\n"
                + " - Assignment #4:\n"
                + "    - Target: Internal Employee (role) [default]\n"
                + "\n"
                + "Requester: midPoint Administrator (administrator)\n"
                + "Channel: http://midpoint.evolveum.com/xml/ns/public/common/channels-3#liveSync\n"
                + "\n";

        Assertions.assertThat(notification).startsWith(startOfNotification);
        Assertions.assertThat(notification).endsWith(endOfNotification);
    }

    @Test(dependsOnMethods = {"mod11test01ConfiguringNotifications"},groups={"M11"}, dependsOnGroups={"M10"})
    public void mod11test02ConfiguringDeploymentInformation() {
        importObject(SYSTEM_CONFIGURATION_FILE_11_2, true);

        Assert.assertTrue($(Schrodinger.byDataId("header", "mainHeader"))
                .getCssValue("background-color").equals("rgba(48, 174, 48, 1)"));
        Assert.assertTrue($(Schrodinger.byDataId("span", "pageTitle")).getText().startsWith("DEV:"));

        basicPage.deploymentInformation()
                .form()
                    .addAttributeValue("headerColor", "lightblue")
                    .and()
                .and()
            .save()
                .feedback()
                    .isSuccess();

        Assert.assertTrue($(Schrodinger.byDataId("header", "mainHeader"))
                .getCssValue("background-color").equals("rgba(173, 216, 230, 1)"));

        basicPage.deploymentInformation()
                .form()
                    .addAttributeValue("headerColor", "#30ae30")
                    .and()
                .and()
            .save()
                .feedback()
                    .isSuccess();
        Assert.assertTrue($(Schrodinger.byDataId("header", "mainHeader"))
        .getCssValue("background-color").equals("rgba(48, 174, 48, 1)"));
    }

    @Test(dependsOnMethods = {"mod11test02ConfiguringDeploymentInformation"},groups={"M11"}, dependsOnGroups={"M10"})
    public void mod11test03ConfiguringObjectCollectionsAndViews() {
        importObject(OBJECT_COLLECTION_ACTIVE_EMP_FILE, true);
        importObject(OBJECT_COLLECTION_INACTIVE_EMP_FILE, true);
        importObject(OBJECT_COLLECTION_FORMER_EMP_FILE, true);
        importObject(SYSTEM_CONFIGURATION_FILE_11_3, true);

        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        login.login(getUsername(), getPassword());

        basicPage.listUsers("Employees");
        basicPage.listUsers("Externals");
        basicPage.listUsers("Inactive Employees");
        basicPage.listUsers("Former Employees");
        basicPage.listUsers("Active Employees");

    }

    protected String readBodyOfLastNotification() throws IOException {
        String separator = "============================================";
        byte[] encoded = Files.readAllBytes(Paths.get(notificationFile.getAbsolutePath()));
        String notifications = new String(encoded, Charset.defaultCharset());
        if (!notifications.contains(separator)) {
            return "";
        }
        String notification = notifications.substring(notifications.lastIndexOf(separator) + separator.length(), notifications.length()-1);
        String bodyTag = "body='";
        if (!notifications.contains(bodyTag)) {
            return "";
        }
        String body = notification.substring(notification.indexOf(bodyTag) + bodyTag.length(), notification.lastIndexOf("'"));
        return body;
    }
}
