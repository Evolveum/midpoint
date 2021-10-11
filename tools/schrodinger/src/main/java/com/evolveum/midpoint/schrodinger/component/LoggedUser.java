/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoggedUser {

    public FormLoginPage logout() {
        SelenideElement userMenu =  $(".dropdown.user.user-menu");

        userMenu.$(By.cssSelector(".dropdown-toggle")).click();
        userMenu.$(By.cssSelector(".user-footer"))
                .$(Schrodinger.byElementAttributeValue("input", "type", "submit")).click();

        //todo implement

        return new FormLoginPage();
    }

    public FormLoginPage logoutIfUserIsLogin() {
        if($(".dropdown.user.user-menu").exists()) {
            SelenideElement userMenu = $(".dropdown.user.user-menu");

            userMenu.$(By.cssSelector(".dropdown-toggle")).click();
            userMenu.$(By.cssSelector(".user-footer"))
                    .$(Schrodinger.byElementAttributeValue("input", "type", "submit")).click();
        }
        return new FormLoginPage();
    }
}
