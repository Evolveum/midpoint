/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger.page;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.login.SamlSelectPage;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.evolveum.midpoint.testing.schrodinger.TestBase;

import java.io.File;
import java.io.IOException;

import static com.codeborne.selenide.Selenide.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoginPageTest extends AbstractLoginPageTest {

    @Test
    public void test020changeLanguageFormPage() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();

        login.changeLanguage("de");

        $(By.cssSelector(".btn.btn-primary")).shouldHave(Condition.value("Anmelden"));
    }

    @Test
    public void test021changeLanguageSamlSelectPage() {
        basicPage.loggedUser().logoutIfUserIsLogin();
        SamlSelectPage login = midPoint.samlSelect();
        login.goToUrl();

        login.changeLanguage("us");

        $(By.xpath("/html/body/div[2]/div/section/div[2]/div/div/div/h4"))
                .shouldHave(Condition.text("Select an Identity Provider"));
    }



}
