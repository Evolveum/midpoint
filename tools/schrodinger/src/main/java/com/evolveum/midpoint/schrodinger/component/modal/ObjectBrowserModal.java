/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.modal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar.
 */
public class ObjectBrowserModal<T> extends ModalBox<T> {
    public ObjectBrowserModal(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ObjectBrowserModal<T> selectType(String type) {
        SelenideElement typeDropDown =
                $(Schrodinger.byElementAttributeValue("select", "data-s-id", "type"));
        typeDropDown.selectOption(type);
        return this;
    }

    public ObjectBrowserModalTable<T, ObjectBrowserModal<T>> table(){
        SelenideElement box = $(Schrodinger.byElementAttributeValue("div", "class","box boxed-table"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ObjectBrowserModalTable<T, ObjectBrowserModal<T>>(this, box);
    }

    public T clickAddButton() {
        $(Schrodinger.byDataId("addButton")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        return getParent();
    }


}
