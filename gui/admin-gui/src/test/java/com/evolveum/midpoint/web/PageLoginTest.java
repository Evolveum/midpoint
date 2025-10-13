/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web;

import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.gui.impl.page.login.module.PageLogin;

import org.apache.wicket.util.tester.WicketTester;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * Created by honchar
 */

//TODO what is this???
@ContextConfiguration(locations = {"classpath:ctx-admin-gui-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PageLoginTest extends AbstractInitializedGuiIntegrationTest {

    @Test
    public void test001BasicRender() {
        WicketTester tester = new WicketTester();
        PageLogin page = tester.startPage(PageLogin.class);
        tester.assertRenderedPage(PageUser.class);
    }



}
