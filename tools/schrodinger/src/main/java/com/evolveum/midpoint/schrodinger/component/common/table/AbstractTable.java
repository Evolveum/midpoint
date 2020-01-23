/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.common.table;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by matus on 5/9/2018.
 */
public abstract class AbstractTable<T> extends Table<T> {

    public AbstractTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public abstract PrismForm<AbstractTable<T>> clickByName(String name);

    public abstract AbstractTable<T> selectCheckboxByName(String name);

    public AbstractTable<T> selectHeaderCheckBox(){
        $(By.tagName("thead"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S)
        .$(Schrodinger.byElementAttributeValue("input", "type", "checkbox"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }
}
