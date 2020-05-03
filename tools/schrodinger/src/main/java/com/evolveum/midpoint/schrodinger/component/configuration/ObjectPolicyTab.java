/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetProjectionModal;
import com.evolveum.midpoint.schrodinger.component.user.ProjectionsDropDown;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author skublik
 */

public class ObjectPolicyTab extends Component<SystemPage> {

    public ObjectPolicyTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public AbstractTableWithPrismView<ObjectPolicyTab> table() {

        SelenideElement tableBox = $(Schrodinger.byDataId("div", "itemsTable"));

        return new AbstractTableWithPrismView<ObjectPolicyTab>(this, tableBox) {
            @Override
            public PrismFormWithActionButtons<AbstractTableWithPrismView<ObjectPolicyTab>> clickByName(String name) {

                $(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                SelenideElement prismElement = $(Schrodinger.byDataId("div", "itemDetails"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

                return new PrismFormWithActionButtons<>(this, prismElement);
            }

            @Override
            public AbstractTableWithPrismView<ObjectPolicyTab> selectCheckboxByName(String name) {

                $(Schrodinger.byFollowingSiblingEnclosedValue("td", "class", "check", "data-s-id", "2", name))
                        .$(By.tagName("input"))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                return this;
            }

            @Override
            public AbstractTableWithPrismView<ObjectPolicyTab> removeByName(String name) {
                //TODO implement
                return this;
            }
        };
    }

    public PrismFormWithActionButtons<ObjectPolicyTab> clickAddObjectPolicy() {
        SelenideElement plusButton = $(Schrodinger.byElementAttributeValue("i", "class", "fa fa-plus "));
        plusButton.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        plusButton.waitWhile(Condition.visible, MidPoint.TIMEOUT_LONG_1_M);
        SelenideElement prismElement = $(Schrodinger.byDataId("div", "itemDetails"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new PrismFormWithActionButtons(this, prismElement);
    }

}
