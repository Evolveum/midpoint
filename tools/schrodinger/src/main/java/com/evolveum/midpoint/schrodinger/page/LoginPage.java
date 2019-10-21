/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoginPage {

    public LoginPage register() {
        // todo implement
        return this;
    }

    public LoginPage forgotPassword() {
        // todo implement
        return this;
    }

    public LoginPage changeLanguage(String countryCode) {
        Validate.notNull(countryCode, "Country code must not be null");

        SelenideElement languageDiv = $(By.cssSelector(".btn-group.bootstrap-select.select-picker-sm.pull-right"));

        languageDiv.$(By.cssSelector(".btn.dropdown-toggle.btn-default")).click();

        SelenideElement ulList = languageDiv.$(By.cssSelector(".dropdown-menu.inner"));

        String cc = countryCode.trim().toLowerCase();
        ulList.$(By.cssSelector(".glyphicon.flag-" + cc)).click();

        return this;
    }

    public BasicPage login(String username, String password) {
        return login(username, password, null);
    }

    public BasicPage login(String username, String password, String locale) {
        open("/login");
        Selenide.sleep(5000);

        if (StringUtils.isNotEmpty(locale)){
            changeLanguage(locale);
        }
        $(By.name("username")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(username);
        $(By.name("password")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(password);
        $x("//input[@type='submit']").click();

        return new BasicPage();
    }
}
