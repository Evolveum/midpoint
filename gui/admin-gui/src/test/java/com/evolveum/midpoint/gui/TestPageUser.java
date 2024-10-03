/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertNotNull;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_OID;

import static org.testng.Assert.assertTrue;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.util.tester.FormTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.AllAssignmentsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.component.*;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.gui.impl.page.admin.user.component.DelegatedToMePanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.component.UserDelegationsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.component.UserPersonasPanel;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author katka
 * @author skublik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageUser extends AbstractInitializedGuiIntegrationTest {

    private static final String MAIN_PANEL = "detailsView:mainForm:mainPanel";

    private static final String TAB_MAIN = "mainPanel:mainForm:tabPanel:panel:main";
    private static final String TAB_ACTIVATION = "mainPanel:mainForm:tabPanel:panel:activation";
    private static final String TAB_PASSWORD = "mainPanel:mainForm:tabPanel:panel:password";

    private static final String PATH_FORM_SHIP = "mainPanel:properties:container:1:values:0:value:valueForm:valueContainer:"
            + "input:propertiesLabel:properties:123:property:values:0:value:valueForm:valueContainer:input:input";
    private static final String PATH_FORM_SHIP_OLD = "tabPanel:panel:main:values:0:value:valueForm:valueContainer:input:"
            + "propertiesLabel:properties:142:property:values:0:value:valueForm:valueContainer:input:input";
    private static final String PATH_FORM_ADMINISTRATIVE_STATUS = "tabPanel:panel:activation:values:0:value:valueForm:"
            + "valueContainer:input:propertiesLabel:properties:0:property:values:0:value:valueForm:valueContainer:input:input";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, executeOptions().overwrite(), initTask, initResult);
    }

    @Test
    public void test001testPageUser() {
        renderPage(PageUser.class);
    }

    @Test
    public void test002testBasicTab() {
        renderPage(PageUser.class);
        choiceArchetype(1);

        tester.assertComponent(MAIN_PANEL, AssignmentHolderBasicPanel.class);
    }

    @Test(enabled = false)
    public void test003testAddDelta() throws Exception {
        renderPage(PageUser.class);

        FormTester formTester = tester.newFormTester(MAIN_FORM, false);
        formTester.setValue(PATH_FORM_NAME, "newUser");
        formTester.setValue(PATH_FORM_SHIP, "ship");

        formTester.submit(FORM_SAVE);

        Thread.sleep(5000);

        PrismObject<UserType> newUser = findObjectByName(UserType.class, "newUser");
        assertNotNull(newUser, "New user not created.");
        logger.info("created user: {}", newUser.debugDump());

    }

    @Test
    public void test004renderAssignmentsTab() {
        renderPage(PageUser.class, USER_ADMINISTRATOR_OID);

        clickOnDetailsMenu(3);
        String submenu = "detailsView:mainForm:navigation:menu:3:navLinkStyle:subNavigation:menu:0:navLinkStyle:navItemLink";
        tester.clickLink(submenu);
        tester.assertComponent(MAIN_PANEL, AllAssignmentsPanel.class);
    }

    @Test
    public void test011renderProjectionsTab() {
        renderPage(PageUser.class, USER_JACK_OID);

        clickOnDetailsMenu(1);
        tester.assertComponent(MAIN_PANEL, FocusProjectionsPanel.class);
    }

    @Test
    public void test012renderActivationTab() {
        renderPage(PageUser.class, USER_ADMINISTRATOR_OID);

        clickOnDetailsMenu(4);
        tester.assertComponent(MAIN_PANEL, FocusActivationPanel.class);
    }

    @Test
    public void test013renderCredentialsTab() {
        renderPage(PageUser.class, USER_ADMINISTRATOR_OID);

        clickOnDetailsMenu(5);
        tester.assertComponent(MAIN_PANEL, FocusPasswordPanel.class);
    }

    @Test
    public void test014renderHistoryTab() {
        renderPage(PageUser.class, USER_ADMINISTRATOR_OID);

        clickOnDetailsMenu(6);
        tester.assertComponent(MAIN_PANEL, FocusHistoryPanel.class);
    }

    @Test
    public void test015renderCasesTab() {
        renderPage(PageUser.class, USER_ADMINISTRATOR_OID);

        clickOnDetailsMenu(7);
        tester.assertComponent(MAIN_PANEL, FocusCasesPanel.class);
    }

    @Test
    public void test016renderPersonasTab() {
        renderPage(PageUser.class, USER_ADMINISTRATOR_OID);

        clickOnDetailsMenu(8);
        tester.assertComponent(MAIN_PANEL, UserPersonasPanel.class);
    }

    @Test
    public void test017renderDelegationsTab() {
        renderPage(PageUser.class, USER_ADMINISTRATOR_OID);

        clickOnDetailsMenu(9);
        tester.assertComponent(MAIN_PANEL, UserDelegationsPanel.class);
    }

    @Test
    public void test018renderDelegatedToMeTab() {
        renderPage(PageUser.class, USER_ADMINISTRATOR_OID);

        clickOnDetailsMenu(10);
        tester.assertComponent(MAIN_PANEL, DelegatedToMePanel.class);
    }

    @Test
    public void test019renderTriggersTab() {
        renderPage(PageUser.class, USER_ADMINISTRATOR_OID);

        clickOnDetailsMenu(11);
        tester.assertComponent(MAIN_PANEL, FocusTriggersPanel.class);
    }

    private void clickOnDetailsMenu(int order) {
        clickOnDetailsMenu(order, PageUser.class);
    }
}
