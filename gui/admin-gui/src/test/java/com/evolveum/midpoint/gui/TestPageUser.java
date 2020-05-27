/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertNotNull;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_OID;

import java.io.File;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeDetailsTabPanel;
import com.evolveum.midpoint.web.component.objectdetails.ObjectHistoryTabPanel;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
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

    private static final String TAB_MAIN = "mainPanel:mainForm:tabPanel:panel:main";
    private static final String TAB_ACTIVATION = "mainPanel:mainForm:tabPanel:panel:activation";
    private static final String TAB_PASSWORD = "mainPanel:mainForm:tabPanel:panel:password";
    private static final String MAIN_FORM = "mainPanel:mainForm";

    private static final String PATH_FORM_NAME = "tabPanel:panel:main:values:0:value:propertiesLabel:properties:0:property:values:0:valueContainer:form:input:originValueContainer:origValueWithButton:origValue:input";
    private static final String PATH_FORM_ADMINISTRATIVE_STATUS = "tabPanel:panel:activation:values:0:value:propertiesLabel:properties:0:property:values:0:valueContainer:form:input:input";
    private static final String PATH_PASSWORD_NEW = "tabPanel:panel:password:values:0:value:propertiesLabel:properties:0:property:values:0:passwordPanel:inputContainer:password1";
    private static final String PATH_PASSWORD_NEW_REPEAT = "tabPanel:panel:password:values:0:value:propertiesLabel:properties:0:property:values:0:passwordPanel:inputContainer:password2";
    private static final String FORM_SAVE = "save";

    public static final File USER_EMPTY_WITH_FAKE_PROJECTION_FILE = new File(COMMON_DIR, "user-empty-with-fake-projection.xml");
    public static final String USER_EMPTY_WITH_FAKE_PROJECTION_OID = "50053534-36dc-11e6-86f7-035182a6f689";

    public static final File CONNECTOR_CSV_FILE = new File(COMMON_DIR, "connector-csv.xml");
    public static final File RESOURCE_CSV_FAKE_FILE = new File(COMMON_DIR, "resource-csv-fake.xml");
    public static final File SHADOW_RESOURCE_CSV_FAKE_FILE = new File(COMMON_DIR, "shadow-resource-csv-fake.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, executeOptions().overwrite(), initTask, initResult);
    }

    @Test
    public void test000testPageUserNew() {
        renderPage();
    }

    @Test
    public void test001testBasicTab() {
        PageUser pageUser = renderPage();

        tester.assertComponent(TAB_MAIN, PrismContainerPanel.class);

        tester.assertComponent(TAB_ACTIVATION, PrismContainerPanel.class);

        tester.assertComponent(TAB_PASSWORD, PrismContainerPanel.class);

    }

    @Test(enabled = false)
    public void test002testAddDelta() throws Exception {
        PageUser pageUser = renderPage();

//        pageUser.

        FormTester formTester = tester.newFormTester(MAIN_FORM, false);
//        formTester.setValue(PATH_FORM_NAME, "newUser");
//        String value = formTester.getTextComponentValue(PATH_FORM_NAME);
//        assertEquals(value, "newUser");
        formTester.select(PATH_FORM_ADMINISTRATIVE_STATUS, 2);//index 2 is ActivationStatusType.ENABLED
        formTester.setValue(PATH_PASSWORD_NEW, "n3wP4ss"); //TODO uncomment when save with password will be OK
        formTester.setValue(PATH_PASSWORD_NEW_REPEAT, "n3wP4ss");

        formTester = formTester.submit(FORM_SAVE);

        Thread.sleep(5000);

        PrismObject<UserType> newUser = findObjectByName(UserType.class, "newUser");
        assertNotNull(newUser, "New user not created.");
        logger.info("created user: {}", newUser.debugDump());

    }

    @Test
    public void test010renderAssignmentsTab() {
        renderPage(USER_ADMINISTRATOR_OID);

        clickOnTab(2);
        String assignmentTable = "mainPanel:mainForm:tabPanel:panel:assignmentsContainer:assignmentsPanel:assignmentsPanel:assignments";
        tester.assertComponent(assignmentTable, MultivalueContainerListPanelWithDetailsPanel.class);

        String assignmentTableDetailsLink = assignmentTable + ":items:itemsTable:box:tableContainer:table:body:rows:1:cells:3:cell:link";
        tester.clickLink(assignmentTableDetailsLink);
        String assignmentTableDetails = assignmentTable + ":details:itemsDetails:0:itemDetails";
        tester.assertComponent(assignmentTableDetails, MultivalueContainerDetailsPanel.class);
    }

    @Test
    public void test011renderProjectionsTab() {
        renderPage(USER_JACK_OID);

        clickOnTab(1);
        String projectionTable = "mainPanel:mainForm:tabPanel:panel:shadowTable";
        tester.assertComponent(projectionTable, MultivalueContainerListPanelWithDetailsPanel.class);

        String projectionTableDetailsLink = projectionTable + ":items:itemsTable:box:tableContainer:table:body:rows:1:cells:3:cell:values:0:value:link";
        tester.clickLink(projectionTableDetailsLink);
        String projectionTableDetails = projectionTable + ":details:itemsDetails:0:itemDetails";
        tester.assertComponent(projectionTableDetails, MultivalueContainerDetailsPanel.class);
    }

    @Test
    public void test012renderHistoryTab() {
        renderPage(USER_ADMINISTRATOR_OID);

        clickOnTab(3);
        String historyPanel = "mainPanel:mainForm:tabPanel:panel";
        tester.assertComponent(historyPanel, ObjectHistoryTabPanel.class);
    }

    @Test
    public void test013renderTasksTab() {
        renderPage();

        clickOnTab(3);
        String panel = "mainPanel:mainForm:tabPanel:panel";
        tester.assertComponent(panel, AssignmentHolderTypeDetailsTabPanel.class);
    }

    @Test
    public void test014renderPersonasTab() {
        renderPage();

        clickOnTab(4);
        String panel = "mainPanel:mainForm:tabPanel:panel";
        tester.assertComponent(panel, AssignmentHolderTypeDetailsTabPanel.class);
    }

    @Test
    public void test015renderDelegationsTab() {
        renderPage();

        clickOnTab(5);
        String panel = "mainPanel:mainForm:tabPanel:panel";
        tester.assertComponent(panel, AssignmentHolderTypeDetailsTabPanel.class);
    }

    @Test
    public void test016renderDelegatedToMeTab() {
        renderPage();

        clickOnTab(6);
        String panel = "mainPanel:mainForm:tabPanel:panel";
        tester.assertComponent(panel, AssignmentHolderTypeDetailsTabPanel.class);
    }

    private void clickOnTab(int order) {
        tester.assertRenderedPage(PageUser.class);
        String tabPath = "mainPanel:mainForm:tabPanel:tabs-container:tabs:" + order + ":link";
        tester.clickLink(tabPath);
    }

    private PageUser renderPage() {
        return renderPageWithParams(null);
    }

    private PageUser renderPage(String userOid) {
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, userOid);
        return renderPageWithParams(params);
    }

    private PageUser renderPageWithParams(PageParameters params) {
        logger.info("render page user");
        if (params == null) {
            params = new PageParameters();
        }
        PageUser pageUser = tester.startPage(PageUser.class, params);

        tester.assertRenderedPage(PageUser.class);

        return pageUser;
    }

}
