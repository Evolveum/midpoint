/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertNotNull;

import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrg;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgs;

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
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgTree;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageOrg extends AbstractInitializedGuiIntegrationTest {

    private static final String NEW_ORG_NAME = "A-newOrg";                  // starts with "A" to be alphabetically first
    private static final String NEW_ORG_CHILD_NAME = "newOrgChild";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, executeOptions().overwrite(), initTask, initResult);
    }

    @Test
    public void test001testPageOrg() {
        renderPage(PageOrg.class);
    }

    @Test
    public void test002testPageOrgTree() {
        renderPage(PageOrgTree.class);
    }

    @Test
    public void test003testPageOrgList() {
        renderPage(PageOrgs.class);
    }

    @Test
    public void test004testAddNewOrg() throws Exception {
        renderPage(PageOrg.class);

        FormTester formTester = tester.newFormTester(MAIN_FORM, false);
        formTester.setValue(PATH_FORM_NAME, NEW_ORG_NAME);
        formTester.submit(FORM_SAVE);

        Thread.sleep(5000);

        PrismObject<OrgType> newOrg = findObjectByName(OrgType.class, NEW_ORG_NAME);
        assertNotNull(newOrg, "New org not created.");
        logger.info("created org: {}", newOrg.debugDump());
    }

    @Test
    public void test005testCreateChild() throws Exception {
        renderPage(PageOrgTree.class);
        tester.clickLink(
                "orgPanel:tabs:panel:treePanel:treeContainer:tree:subtree:branches:1:node:content:menu:inlineMenuPanel:"
                        + "dropDownMenu:menuItem:8:menuItemBody:menuItemLink"
        );
        tester.assertRenderedPage(PageOrg.class);

        FormTester formTester = tester.newFormTester(MAIN_FORM, false);
        formTester.setValue(PATH_FORM_NAME, NEW_ORG_CHILD_NAME);
        formTester.submit(FORM_SAVE);

        Thread.sleep(5000);

        PrismObject<OrgType> newOrgChild = findObjectByName(OrgType.class, NEW_ORG_CHILD_NAME);
        PrismObject<OrgType> newOrg = findObjectByName(OrgType.class, NEW_ORG_NAME);
        assertNotNull(newOrgChild, "New org not created.");
        assertAssignedOrg(newOrgChild, newOrg.getOid());
    }

}
