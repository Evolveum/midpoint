/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_OID;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_USERNAME;
import static org.testng.Assert.assertNotNull;

/**
 * @author Hiroyuki Wada
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageRole extends AbstractInitializedGuiIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestPageOrg.class);

    private static final String MAIN_FORM = "mainPanel:mainForm";
    private static final String PATH_FORM_NAME = "tabPanel:panel:main:values:0:value:propertiesLabel:properties:0:property:values:0:valueContainer:form:input:originValueContainer:origValueWithButton:origValue:input";
    private static final String FORM_SAVE = "save";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        LOGGER.info("adding system config page");
        addObject(systemConfig, ModelExecuteOptions.createOverwrite(), initTask, initResult);
    }

    @Test
    public void test001testPageRole() {
        renderPage(PageRole.class);
    }

    @Test
    public void test002testAddNewRole() throws Exception {
        renderPage(PageRole.class);

        FormTester formTester = tester.newFormTester(MAIN_FORM, false);
        formTester.setValue(PATH_FORM_NAME, "newRole");
        formTester = formTester.submit(FORM_SAVE);

        Thread.sleep(5000);

        PrismObject<RoleType> newRole = findObjectByName(RoleType.class, "newRole");
        assertNotNull(newRole, "New role not created.");
        LOGGER.info("created role: {}", newRole.debugDump());
    }

    /**
     * MID-6092
     */
    @Test
    public void test003testMembers() throws Exception {
        // GIVEN
        PrismObject<RoleType> role1 = createObject(RoleType.class, "Role0001");
        PrismObject<RoleType> role2 = createObject(RoleType.class, "Role0002");
        String role1Oid = addObject(role1);
        String role2Oid = addObject(role2);
        Task task = createTask("assign");
        // Assign Role0001 with orgRef P0001
        assignParametricRole(USER_JACK_OID, role1Oid, ORG_SAVE_ELAINE_OID, null, task, task.getResult());
        assignRole(USER_ADMINISTRATOR_OID, role1Oid);
        // Assign Role0002 with orgRef P0001
        assignParametricRole(USER_ADMINISTRATOR_OID, role2Oid, ORG_SAVE_ELAINE_OID, null, task, task.getResult());

        String panel = "mainPanel:mainForm:tabPanel:panel";
        String memberTable = panel + ":form:memberContainer:memberTable:mainForm:table:box:tableContainer:table";

        // WHEN
        // Open Role0001 page
        renderPage(PageRole.class, role1Oid);
        // Show Members tab
        clickOnTab(8);

        // THEN
        tester.assertComponent(panel, AbstractRoleMemberPanel.class);
        tester.debugComponentTrees(":rows:.*:cells:3:cell:link:label");
        // It should show all members who are assigned Role0001
        tester.hasLabel(memberTable + ":body:rows:1:cells:3:cell:link:label", USER_ADMINISTRATOR_USERNAME);
        tester.hasLabel(memberTable + ":body:rows:2:cells:3:cell:link:label", USER_JACK_USERNAME);
        tester.assertNotExists(memberTable + ":body:rows:3:cells:3:cell:link:label");

        // WHEN
        // Choose P0001 in 'Org/Project' filter selection
        tester.clickLink(panel + ":form:project:inputContainer:choose");
        tester.clickLink("mainPopup:content:table:mainForm:table:box:tableContainer:table:body:rows:7:cells:2:cell:link");
        executeModalWindowCloseCallback("mainPopup");

        // THEN
        // It should show only one user who is assigned Role0001 with orgRef P0001
        tester.debugComponentTrees(":rows:.*:cells:3:cell:link:label");
        tester.hasLabel(memberTable + ":body:rows:3:cells:3:cell:link:label", USER_JACK_USERNAME);
        tester.assertNotExists(memberTable + ":body:rows:4:cells:3:cell:link:label");
    }

    private void clickOnTab(int order) {
        tester.assertRenderedPage(PageRole.class);
        String tabPath = "mainPanel:mainForm:tabPanel:tabs-container:tabs:" + order + ":link";
        tester.clickLink(tabPath);
    }

    private Page renderPage(Class<? extends Page> expectedRenderedPageClass) {
        return renderPage(expectedRenderedPageClass, null);
    }

    private Page renderPage(Class<? extends Page> expectedRenderedPageClass, String oid) {
        LOGGER.info("render page role");
        PageParameters params = new PageParameters();
        if (oid != null) {
            params.add(OnePageParameterEncoder.PARAMETER, oid);
        }
        Page pageRole = tester.startPage(expectedRenderedPageClass, params);

        tester.assertRenderedPage(expectedRenderedPageClass);

        return pageRole;
    }
}
