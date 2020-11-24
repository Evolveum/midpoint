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
import com.evolveum.midpoint.schrodinger.page.self.AssignmentDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public class RequestRoleItemsPanel extends Component<RequestRoleTab> {

    public RequestRoleItemsPanel(RequestRoleTab parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public RequestRoleItemsPanel addItemToCart(String itemName) {
        $(Schrodinger.byElementValue("div", "class", "inner-label", itemName))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .parent()
                .parent()
                .$(Schrodinger.byElementAttributeValue("span", "class", "shopping-cart-item-button-add"))
                .click();
        return RequestRoleItemsPanel.this;
    }

    public AssignmentDetailsPage clickPropertiesLink(String itemName) {
        $(Schrodinger.byElementValue("div", "class", "inner-label", itemName))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .parent()
                .parent()
                .$(Schrodinger.byElementAttributeValue("span", "class", "shopping-cart-item-button-details"))
                .click();
        $(byText("Assignment details")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new AssignmentDetailsPage();

    }


}
