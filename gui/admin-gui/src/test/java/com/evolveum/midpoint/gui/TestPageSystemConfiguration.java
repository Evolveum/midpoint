/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertEquals;

import com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page.*;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageSystemConfiguration extends AbstractInitializedGuiIntegrationTest {

    private static final String FORM_INPUT_DESCRIPTION = "tabPanel:panel:basicSystemConfiguration:values:0:value:valueForm:valueContainer:input:propertiesLabel:properties:1:property:values:0:value:valueForm:valueContainer:input:input";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, executeOptions().overwrite(), initTask, initResult);
    }

    @Test
    public void test001testPageSystemConfiguration() {
        renderPage(com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.PageSystemConfiguration.class);
    }

    @Test
    public void test002testPageSystemConfigurationBasic() {
        renderPage(PageSystemBasic.class);
    }

    @Test
    public void test007testPageNotificationConfiguration() {
        renderPage(PageSystemNotification.class);
    }

    @Test
    public void test008testPageLogging() {
        renderPage(PageSystemLogging.class);
    }

    @Test
    public void test009testPageProfiling() {
        renderPage(PageProfiling.class);
    }

    @Test
    public void test010testPageAdminGuiConfiguration() {
        renderPage(PageSystemAdminGui.class);
    }

    @Test
    public void test011testPageWorkflowConfiguration() {
        renderPage(PageSystemWorkflow.class);
    }

    @Test
    public void test012testPageRoleManagement() {
        renderPage(PageRoleManagement.class);
    }

    @Test
    public void test013testPageInternalsConfiguration() {
        renderPage(PageSystemInternals.class);
    }

    @Test
    public void test015testPageAccessCertification() {
        renderPage(PageAccessCertification.class);
    }

    @Test
    public void test018testModifySystemConfig() throws Exception {
        renderPage(com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.PageSystemConfiguration.class);

        tester.executeAjaxEvent("container:additionalButtons:0:additionalButton:compositedButton", "click");
        tester.assertRenderedPage(PageSystemBasic.class);

        final String mainFormPath = "detailsView:mainForm";
        final String descriptionPath = "mainPanel:properties:container:1:valuesContainer:values:0:value:valueForm:valueContainer:input:"
                + "propertiesLabel:properties:1:property:valuesContainer:values:0:value:valueForm:valueContainer:input:input";

        tester.clickLink(mainFormPath + ":mainPanel:properties:container:1:valuesContainer:values:0:value:valueForm:valueContainer:input:propertiesLabel:showEmptyButton");

        FormTester formTester = tester.newFormTester(mainFormPath, false);
        String des = "new description";
        formTester.setValue(descriptionPath, des);

        final String saveButton = "buttons:buttons:2";
        formTester.submit(saveButton);

        Thread.sleep(5000);

        tester.assertRenderedPage(com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.PageSystemConfiguration.class);

        PrismObject<SystemConfigurationType> sysConf = getObject(SystemConfigurationType.class, "00000000-0000-0000-0000-000000000001");
        assertEquals(des, sysConf.getRealValue().getDescription());
    }

}
