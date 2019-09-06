/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0 
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web;

import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.login.PageLogin;
import org.apache.wicket.util.tester.WicketTester;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * Created by honchar
 */
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
