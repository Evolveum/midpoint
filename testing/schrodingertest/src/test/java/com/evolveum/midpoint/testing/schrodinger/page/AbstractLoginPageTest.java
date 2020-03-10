/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.report.AuditRecordTable;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.report.AuditLogViewerPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static com.codeborne.selenide.Selenide.*;

/**
 * @author skublik
 */

public class AbstractLoginPageTest extends AbstractSchrodingerTest {

    private static final File ENABLED_USER = new File("src/test/resources/configuration/objects/users/enabled-user.xml");
    private static final File DISABLED_USER = new File("src/test/resources/configuration/objects/users/disabled-user.xml");
    private static final File ENABLED_USER_WITHOUT_AUTHORIZATIONS = new File("src/test/resources/configuration/objects/users/enabled-user-without-authorizations.xml");

    @BeforeClass
    @Override
    public void beforeClass() throws IOException{
        super.beforeClass();
        importObject(ENABLED_USER, true);
        importObject(DISABLED_USER, true);
        importObject(ENABLED_USER_WITHOUT_AUTHORIZATIONS, true);
    }

    @Test
    public void test001loginLockoutUser() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        open("/login");
        for (int i = 0; i < 4; i++) {
            unsuccessfulLogin("enabled_user", "bad_password");
        }
        unsuccessfulLogin("enabled_user", "5ecr3t");
    }

    @Test
    public void test002loginDisabledUser() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        unsuccessfulLogin("disabled_user", "5ecr3t");
    }

    @Test
    public void test003loginEnabledUserWithoutAuthorizationsUser() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        unsuccessfulLogin("enabled_user_without_authorizations", "5ecr3t");
    }

    protected void unsuccessfulLogin(String username, String password){
        FormLoginPage login = midPoint.formLogin();
        login.login(username, password);

        FeedbackBox feedback = login.feedback();
        Assert.assertTrue(feedback.isError("0"));
    }

    @Test
    public void test010auditingSuccessfulLogin() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        login.login("administrator", "5ecr3t");
        auditingSuccesfulLogin("administrator");
    }

    protected void auditingSuccesfulLogin(String username) {
        AuditLogViewerPage auditLogViewer = basicPage.auditLogViewer();
        AuditRecordTable auditRecordsTable = auditLogViewer.table();
        auditRecordsTable.checkInitiator(1, username);
        auditRecordsTable.checkEventType(1, "Create session");
        auditRecordsTable.checkOutcome(1, "Success");
    }

    @Test
    public void test011auditingFailLogin() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        login.login("bad_administrator", "5ecr3t");
        login.login("administrator", "5ecr3t");

        AuditLogViewerPage auditLogViewer = basicPage.auditLogViewer();
        AuditRecordTable auditRecordsTable = auditLogViewer.table();
        auditRecordsTable.checkInitiator(2, "");
        auditRecordsTable.checkEventType(2, "Create session");
        auditRecordsTable.checkOutcome(2, "Fatal Error");
    }

    @Test
    public void test012auditingSuccessfulLogout() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        login.login("administrator", "5ecr3t");
        basicPage.loggedUser().logout();
        login.login("administrator", "5ecr3t");

        AuditLogViewerPage auditLogViewer = basicPage.auditLogViewer();
        AuditRecordTable auditRecordsTable = auditLogViewer.table();
        auditRecordsTable.checkInitiator(2, "administrator");
        auditRecordsTable.checkEventType(2, "Terminate session");
        auditRecordsTable.checkOutcome(2, "Success");
    }
}
