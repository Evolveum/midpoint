/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger.page;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.page.LoginPage;
import org.openqa.selenium.By;
import org.testng.annotations.Test;
import com.evolveum.midpoint.testing.schrodinger.TestBase;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoginPageTest extends TestBase {

    @Test
    public void changeLanguage() {
        basicPage.loggedUser().logout();
        LoginPage login = midPoint.login();

        login.changeLanguage("de");

        $(By.cssSelector(".btn.btn-primary")).shouldHave(Condition.value("Anmelden"));
    }
}
