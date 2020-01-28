/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import org.apache.wicket.request.mapper.parameter.PageParameters;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageAccount extends AbstractInitializedGuiIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestPageAccount.class);

    private static final String FORM_SAVE = "mainForm:save";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        LOGGER.info("adding system config page");
        addObject(systemConfig, ModelExecuteOptions.createOverwrite(), initTask, initResult);
    }

    @Test
    public void test000testPageAccount() throws Exception {
        dummyResourceCtl.addAccount("test");

        PrismObject<ShadowType> accountMancomb = findAccountByUsername("test", dummyResourceCtl.getResource());
        renderPage(accountMancomb.getOid());
        tester.assertComponent(FORM_SAVE, AjaxSubmitButton.class);
    }

    @Test (expectedExceptions = AssertionError.class)
    public void test001testPageAccountWithProtectedUser() throws Exception {
        dummyResourceCtl.addAccount("admin");

        PrismObject<ShadowType> accountMancomb = findAccountByUsername("admin", dummyResourceCtl.getResource());
        renderPage(accountMancomb.getOid());
        tester.assertComponent(FORM_SAVE, AjaxSubmitButton.class);
    }

    private PageAccount renderPage(String userOid) {
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, userOid);
        return renderPageWithParams(params);
    }

    private PageAccount renderPageWithParams(PageParameters params) {
        LOGGER.info("render page account");
        if(params == null) {
            params = new PageParameters();
        }
        PageAccount pageAccount = tester.startPage(PageAccount.class, params);

        tester.assertRenderedPage(PageAccount.class);

        return pageAccount;
    }

}
