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
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTable;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserProjectionsTab extends Component<UserPage> {
    public UserProjectionsTab(UserPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public UserProjectionsDropDown<UserProjectionsTab> clickHeaderActionDropDown() {

        $(By.tagName("thead"))
                .$(Schrodinger.byDataId("inlineMenuPanel"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();

        SelenideElement dropDownMenu = $(Schrodinger.byElementAttributeValue("ul", "class", "dropdown-menu pull-right"));

        return new UserProjectionsDropDown<>(this, dropDownMenu);
    }

    public AbstractTable<UserProjectionsTab> table() {

        SelenideElement tableBox = $(By.cssSelector(".box.projection"));

        return new AbstractTable<UserProjectionsTab>(this, tableBox) {
            @Override
            public PrismForm<AbstractTable<UserProjectionsTab>> clickByName(String name) {

                $(Schrodinger.byElementValue("span", "data-s-id", "name", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                SelenideElement prismElement = $(By.cssSelector(".container-fluid.prism-object"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

                return new PrismForm<>(this, prismElement);
            }

            @Override
            public AbstractTable<UserProjectionsTab> selectCheckboxByName(String name) {

                $(Schrodinger.byFollowingSiblingEnclosedValue("input", "type", "checkbox", "class", "check-table-label", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                return this;
            }
        };
    }
}
