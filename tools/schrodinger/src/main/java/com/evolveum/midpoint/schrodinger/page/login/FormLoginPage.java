/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.login;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FormLoginPage extends LoginPage {

    public FormLoginPage register() {
        // todo implement
        return this;
    }

    public FormLoginPage forgotPassword() {
        // todo implement
        return this;
    }

    public BasicPage loginWithReloadLoginPage(String username, String password) {
        return loginWithReloadLoginPage(username, password, null);
    }

    public BasicPage loginWithReloadLoginPage(String username, String password, String locale) {
        open("/login");
        Selenide.sleep(5000);

        return login(username, password, locale);
    }

    public BasicPage login(String username, String password) {
        return login(username, password, null);
    }

    public BasicPage login(String username, String password, String locale) {
        if (StringUtils.isNotEmpty(locale)){
            changeLanguage(locale);
        }
        $(By.name("username")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(username);
        $(By.name("password")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(password);
        $x("//input[@type='submit']").click();

        return new BasicPage();
    }


    public FeedbackBox<? extends FormLoginPage> feedback() {
        SelenideElement feedback = $(By.cssSelector("div.feedbackContainer")).waitUntil(Condition.appears, MidPoint.TIMEOUT_LONG_1_M);
        return new FeedbackBox<>(this, feedback);
    }
}
