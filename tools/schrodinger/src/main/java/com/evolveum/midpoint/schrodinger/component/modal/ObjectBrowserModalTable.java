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
import com.evolveum.midpoint.schrodinger.component.common.search.Search;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public class ObjectBrowserModalTable<T, M extends ModalBox<T>> extends Table<M> {

    public ObjectBrowserModalTable(M parent, SelenideElement parentElement){
        super(parent, parentElement);
    }

    public T clickByName(String name){
        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        getParent()
                .getParentElement()
                .waitUntil(Condition.disappears, MidPoint.TIMEOUT_DEFAULT_2_S);
        return getParent().getParent();
    }

    public ObjectBrowserModalTable<T, M> selectCheckboxByName(String name) {
        $(Schrodinger.byAncestorFollowingSiblingDescendantOrSelfElementEnclosedValue("input", "type", "checkbox", "data-s-id", "3", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    @Override
    public Search<ObjectBrowserModalTable<T, M>> search() {
        return (Search<ObjectBrowserModalTable<T, M>>) super.search();
    }
}
