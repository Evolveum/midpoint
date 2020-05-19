/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertEquals;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfiguration;
import com.evolveum.midpoint.web.page.admin.home.PageDashboardInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageSystemConfiguration extends AbstractInitializedGuiIntegrationTest {

    private static final String MAIN_FORM = "mainPanel:mainForm";
    private static final String FORM_INPUT_DESCRIPTION = "tabPanel:panel:basicSystemConfiguration:values:0:value:valueForm:valueContainer:input:propertiesLabel:properties:1:property:values:0:value:valueForm:valueContainer:input:input";
    private static final String FORM_SAVE = "save";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, ModelExecuteOptions.createOverwrite(), initTask, initResult);
    }

    @Test
    public void test000testPageSystemConfiguration() {
        renderPage();
    }

    @Test
    public void test001testModifySystemConfig() throws Exception {
        renderPage();

        tester.clickLink(MAIN_FORM + ":tabPanel:panel:basicSystemConfiguration:values:0:value:valueForm:valueContainer:input:propertiesLabel:showEmptyButton");

        FormTester formTester = tester.newFormTester(MAIN_FORM, false);
        String des = "new description";
        formTester.setValue(FORM_INPUT_DESCRIPTION, des);

        formTester.submit(FORM_SAVE);

        Thread.sleep(5000);

        tester.assertRenderedPage(PageDashboardInfo.class);

        PrismObject<SystemConfigurationType> sysConf = getObject(SystemConfigurationType.class, "00000000-0000-0000-0000-000000000001");
        assertEquals(des, sysConf.getRealValue().getDescription());
    }

    private PageSystemConfiguration renderPage() {
        logger.info("render page system configuration");
        PageParameters params = new PageParameters();
        PageSystemConfiguration pageAccount = tester.startPage(PageSystemConfiguration.class, params);

        tester.assertRenderedPage(PageSystemConfiguration.class);

        return pageAccount;
    }

}
