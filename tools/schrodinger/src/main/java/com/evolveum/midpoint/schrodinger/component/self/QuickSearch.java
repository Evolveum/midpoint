/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.self;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/10/2018.
 */
public class QuickSearch<T> extends Component<T> {
    public QuickSearch(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public QuickSearch<T> inputValue(String name) {
        $(Schrodinger.byElementAttributeValue("input", "name", "searchInput")).setValue(name);

        return this;
    }

    //TODO rethink
    public Table clickSearch() {
        $(Schrodinger.byElementAttributeValue("button", "data-s-id", "searchButton"))
                .click();

        return new Table("null", null);
    }

    public QuickSearchDropDown<QuickSearch<T>> clickSearchFor() {
        $(Schrodinger.bySelfOrDescendantElementAttributeValue("button", "data-toggle", "dropdown", "class", "sr-only"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        SelenideElement dropDown = $(Schrodinger.byElementAttributeValue("ul", "role", "menu"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new QuickSearchDropDown<>(this, dropDown);
    }
}
