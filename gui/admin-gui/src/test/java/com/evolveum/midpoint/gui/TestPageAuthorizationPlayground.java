/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.page.admin.configuration.PageAuthorizationPlayground;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageAuthorizationPlayground extends AbstractInitializedGuiIntegrationTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        PrismObject<SystemConfigurationType> systemConfig = parseObject(SYSTEM_CONFIGURATION_FILE);

        logger.info("adding system config page");
        addObject(systemConfig, executeOptions().overwrite(), initTask, initResult);
    }

    @Test
    public void test100ExecuteSamples() throws CommonException {
        login(userAdministrator);

        var page = renderPage(PageAuthorizationPlayground.class);
        System.out.println("Page = " + page);
    }
}
