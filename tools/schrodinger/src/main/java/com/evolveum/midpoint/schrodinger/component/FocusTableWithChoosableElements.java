/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.common.search.Search;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTable;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/9/2018.
 */
public class FocusTableWithChoosableElements<T> extends AbstractTable<T> {
    public FocusTableWithChoosableElements(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public PrismForm<AbstractTable<T>> clickByName(String name) {
        return null;
    }

    @Override
    public AbstractTable<T> selectCheckboxByName(String name) {
        $(Schrodinger.byAncestorFollowingSiblingDescendantOrSelfElementEnclosedValue("input", "type", "checkbox", "data-s-id", "3", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }


    private String constructCheckBoxIdBasedOnRow(String row) {
        StringBuilder constructCheckboxName = new StringBuilder("table:box:tableContainer:table:body:rows:")
                .append(row).append(":cells:1:cell:check");

        return constructCheckboxName.toString();
    }

    @Override
    public Search<FocusTableWithChoosableElements<T>> search() {
        SelenideElement searchElement = getParentElement().$x(".//div[contains(@class, \"form-inline\") "
                + "and contains(@class, \"pull-right\") and contains(@class, \"search-form\")]")
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new Search<FocusTableWithChoosableElements<T>>(this, searchElement);
    }
}
