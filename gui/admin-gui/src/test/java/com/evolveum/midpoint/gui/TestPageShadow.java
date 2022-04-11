/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.page.admin.resource.PageShadow;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageShadow extends AbstractInitializedGuiIntegrationTest {

    private static final String FORM_SAVE = "detailsView:mainForm:buttons:buttons:2";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, executeOptions().overwrite(), initTask, initResult);
    }

    @Test
    public void test000testPageAccount() throws Exception {
        dummyResourceCtl.addAccount("test");

        PrismObject<ShadowType> accountMancomb = findAccountByUsername("test", dummyResourceCtl.getResource());
        renderPage(accountMancomb.getOid());
        tester.debugComponentTrees("buttons");
        tester.assertComponent(FORM_SAVE, AjaxCompositedIconSubmitButton.class);
    }

    //TODO: enable after reviewed a know why shoud throw an error
    @Test (expectedExceptions = AssertionError.class, enabled = false)
    public void test001testPageAccountWithProtectedUser() throws Exception {
        dummyResourceCtl.addAccount("admin");

        PrismObject<ShadowType> accountMancomb = findAccountByUsername("admin", dummyResourceCtl.getResource());
        renderPage(accountMancomb.getOid());
        tester.assertComponent(FORM_SAVE, AjaxCompositedIconSubmitButton.class);
    }

    private PageShadow renderPage(String userOid) {
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, userOid);
        return renderPageWithParams(params);
    }

    private PageShadow renderPageWithParams(PageParameters params) {
        logger.info("render page account");
        if(params == null) {
            params = new PageParameters();
        }
        PageShadow pageAccount = tester.startPage(PageShadow.class, params);

        tester.assertRenderedPage(PageShadow.class);

        return pageAccount;
    }

}
