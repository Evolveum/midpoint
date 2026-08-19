/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import com.evolveum.midpoint.gui.api.component.otp.OtpListPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.application.PageApplication;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestPageApplication extends AbstractInitializedGuiIntegrationTest {

    private static final String MAIN_PANEL = "detailsView:mainForm:mainPanel";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test001RenderOtpTabForNewApplication() {
        PageParameters params = new PageParameters()
                .add(AbstractPageObjectDetails.PARAM_PANEL_ID, "otp");

        tester.startPage(PageApplication.class, params);

        tester.assertRenderedPage(PageApplication.class);
        tester.assertComponent(MAIN_PANEL, OtpListPanel.class);
    }
}
