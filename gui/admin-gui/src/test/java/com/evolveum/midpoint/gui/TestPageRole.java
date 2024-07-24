/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertNotNull;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_OID;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_USERNAME;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.util.tester.FormTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author Hiroyuki Wada
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageRole extends AbstractInitializedGuiIntegrationTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, executeOptions().overwrite(), initTask, initResult);
    }

    @Test
    public void test001testPageRole() {
        renderPage(PageRole.class);
    }

    @Test
    public void test002testAddNewRole() throws Exception {
        renderPage(PageRole.class);
        choiceArchetype(2);

        FormTester formTester = tester.newFormTester(MAIN_FORM, false);
        formTester.setValue(PATH_FORM_NAME, "newRole");
        formTester.submit(FORM_SAVE);

        Thread.sleep(5000);

        PrismObject<RoleType> newRole = findObjectByName(RoleType.class, "newRole");
        assertNotNull(newRole, "New role not created.");
        logger.info("created role: {}", newRole.debugDump());
    }

    /**
     * MID-6092
     */
    @Test
    public void test003testMembers() throws Exception {
        // GIVEN
        PrismObject<RoleType> role1 = createObject(RoleType.class, "Role0001");
        String role1Oid = addObject(role1);
        Task task = createTask("assign");
        // Assign Role0001 with orgRef P0001
//        assignParametricRole(USER_JACK_OID, role1Oid, ORG_SAVE_ELAINE_OID, null, task, task.getResult()); //TODO uncomment after fixing search for members without org/project
        assignRole(USER_JACK_OID, role1Oid);
        assignRole(USER_ADMINISTRATOR_OID, role1Oid);

        String panel = "detailsView:mainForm:mainPanel";
        String tableBox = panel + ":form:memberContainer:memberTable:itemsTable";
        String memberTable = tableBox + ":tableContainer:table";

        // WHEN
        // Open Role0001 page
        renderPage(PageRole.class, role1Oid);
        // Show Members tab
        clickOnDetailsMenu(9, PageRole.class);

        // THEN
        tester.assertComponent(panel, AbstractRoleMemberPanel.class);
        tester.debugComponentTrees(":rows:.*:cells:3:cell:link:title");
        // It should show all members who are assigned Role0001
        tester.assertLabel(memberTable + ":body:rows:1:cells:3:cell:link:label", USER_ADMINISTRATOR_USERNAME);
        tester.assertLabel(memberTable + ":body:rows:2:cells:3:cell:link:label", USER_JACK_USERNAME);
        tester.assertNotExists(memberTable + ":body:rows:3:cells:3:cell:link:label");
    }

}
