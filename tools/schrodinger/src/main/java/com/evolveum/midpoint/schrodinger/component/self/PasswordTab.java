/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.self;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.page.self.CredentialsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by honchar
 */
public class PasswordTab extends Component<CredentialsPage> {

    public PasswordTab(CredentialsPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ChangePasswordPanel<PasswordTab> changePasswordPanel() {
        return new ChangePasswordPanel<>(this, getParentElement()
                .$(Schrodinger.byElementAttributeValue("div", "class", "tab-content"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }
}
