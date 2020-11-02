/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.self;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public class RequestRoleTab extends Component<RequestRolePage> {

    public RequestRoleTab(RequestRolePage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public RequestRoleItemsPanel getItemsPanel() {
        SelenideElement itemsElement = $(Schrodinger.byDataId("div", "shoppingCartItemsPanel"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new RequestRoleItemsPanel(this, itemsElement);
    }

    public RequestRoleTab setRequestingForUser(String... userNames) {
        return this;
    }

    public RequestRoleTab setRelation(String relationValue) {
        return this;
    }

    public RequestRoleTab addAll() {
        return this;
    }

    public ShoppingCartPage goToShoppingCart() {
        return new ShoppingCartPage();
    }
}
