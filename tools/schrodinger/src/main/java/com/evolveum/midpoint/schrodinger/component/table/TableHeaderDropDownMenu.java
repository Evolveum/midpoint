/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.table;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.DropDown;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author skublik
 */

public class TableHeaderDropDownMenu<T> extends DropDown<T> {

    public TableHeaderDropDownMenu(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    protected T clickByDataResourceKey(String key) {
        $(Schrodinger.byDataResourceKey(key))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this.getParent();
    }

}
