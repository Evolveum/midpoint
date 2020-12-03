/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
/**
 * Created by honchar
 */
public class ItemPathPanel<T> extends Component<T> {

    public ItemPathPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ItemPathPanel<T> setNamespaceValue(String value) {
        getParentElement().$x(".//div[@data-s-id='namespace']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(Schrodinger.byDataId("input"))
                .selectOption(value);
        return this;
    }

    public ItemPathPanel<T> setAttributeValue(String value) {
        getParentElement().$x(".//div[@data-s-id='definition']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(Schrodinger.byDataId("input"))
                .setValue(value);
        return this;
    }

    public ItemPathPanel<T> clickPlusButton() {
        getParentElement().$x(".//div[@data-s-id='plus']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public ItemPathPanel<T> clickMinusButton() {
        getParentElement().$x(".//div[@data-s-id='minus']").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

}
