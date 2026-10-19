/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertEquals;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_OID;

import org.apache.wicket.model.Model;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.column.ConfigurableExpressionColumn;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimpleExpressionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Checks that expression columns in GUI tables bind the "input" variable the same way
 * as report export does, see docs for collection reports: when the column has no path,
 * "input" is the same as "object". Issue 12807.
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestConfigurableExpressionColumn extends AbstractInitializedGuiIntegrationTest {

    private PrismObject<UserType> userJack;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, initResult);
    }

    @Test
    public void test100InputIsObjectForColumnWithoutPath() {
        PageBase page = (PageBase) renderPage(PageUser.class);

        GuiObjectColumnType columnConfig = new GuiObjectColumnType()
                .name("test");
        ExpressionType expression = SimpleExpressionUtil.melExpression("input.name.orig", IntegrationTestTools.trustedForTests());

        ConfigurableExpressionColumn<SelectableBean<UserType>, UserType> column =
                new ConfigurableExpressionColumn<>(Model.of("test"), null, columnConfig, expression, page);

        String value = column.getDataModel(Model.of(new SelectableBeanImpl<>(Model.of(userJack.asObjectable())))).getObject();

        assertEquals(value, "jack");
    }

}
