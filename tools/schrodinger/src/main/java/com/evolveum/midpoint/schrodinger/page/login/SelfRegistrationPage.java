/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.login;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SelfRegistrationPage extends LoginPage {

    public SelfRegistrationPage setGivenName(String value) {
        $(By.name("contentArea:staticForm:firstName:input")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(value);
        return  this;
    }

    public SelfRegistrationPage setFamilyName(String value) {
        $(By.name("contentArea:staticForm:lastName:input")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(value);
        return  this;
    }

    public SelfRegistrationPage setEmail(String value) {
        $(By.name("contentArea:staticForm:email:input")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(value);
        return  this;
    }

    public SelfRegistrationPage setPassword(String value) {
        $(Schrodinger.byDataId("password1")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(value);
        $(Schrodinger.byDataId("password2")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue(value);
        return  this;
    }

    public SelfRegistrationPage submit() {
        $(Schrodinger.byDataId("text")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).setValue("text");
        $(Schrodinger.byDataId("submitRegistration")).click();
        return this;
    }

    protected static String getBasePath() {
        return "/registration";
    }
}
