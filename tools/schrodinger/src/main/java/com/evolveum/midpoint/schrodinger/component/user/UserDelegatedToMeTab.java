/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.DelegationDetailsPanel;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserDelegatedToMeTab extends Component<UserPage> {

    public UserDelegatedToMeTab(UserPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public DelegationDetailsPanel<UserDelegatedToMeTab> getDelegationDetailsPanel(String delegatedFromUser) {
        return new DelegationDetailsPanel<>(this,
                $(By.linkText(delegatedFromUser))
                        .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }
}
