/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.login.LoginPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;
/**
 * Created by honchar
 */
public class UserMenuPanel<BP extends BasicPage> extends Component<BP> {

    public UserMenuPanel(BP parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public LoginPage clickLogout() {
        getParentElement()
                .$(Schrodinger.byDataId("logoutForm"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(Schrodinger.byElementAttributeValue("input", "type", "submit"))
                .click();
        $(Schrodinger.byElementAttributeValue("input", "name", "username")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new LoginPage();
    }
}
