/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.Assert;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.open;

/**
 * @author skublik
 */

public class HomePageTest extends AbstractSchrodingerTest {

    @Test
    public void test001OpenPageWithSlashOnEndOfUrl() {
        open("/self/dashboard/");
        //when request will be redirect to error page, then couldn't click on home menu button
        basicPage.home();
    }

    @Test
    public void test002OpenPageWithoutSlashOnEndOfUrl() {
        open("/self/dashboard");
        //when request will be redirect to error page, then couldn't click on home menu button
        basicPage.home();
    }
}
