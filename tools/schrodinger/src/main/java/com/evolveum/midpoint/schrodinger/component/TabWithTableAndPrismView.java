/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;

import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

abstract public class TabWithTableAndPrismView<P> extends Component<P> {

    public TabWithTableAndPrismView(P parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public <T extends TabWithTableAndPrismView<P>> AbstractTableWithPrismView<T> table() {

        SelenideElement tableBox = $(By.cssSelector(".box.boxed-table")).waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new AbstractTableWithPrismView<T>((T) this, tableBox) {
            @Override
            public PrismFormWithActionButtons<AbstractTableWithPrismView<T>> clickByName(String name) {
                $(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                SelenideElement prismElement = $(Schrodinger.byDataId("div", getPrismViewPanelId()))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

                return new PrismFormWithActionButtons<>(this, prismElement);
            }

            @Override
            public AbstractTableWithPrismView<T> selectCheckboxByName(String name) {
                $(Schrodinger.byFollowingSiblingEnclosedValue("td", "class", "check", "data-s-id", "3", name))
                        .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                return this;
            }

            public AbstractTableWithPrismView<T> removeByName(String name) {
                $(By.cssSelector(".fa.fa-minus")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

                return this;
            }
        };
    }

    abstract protected String getPrismViewPanelId();
}
