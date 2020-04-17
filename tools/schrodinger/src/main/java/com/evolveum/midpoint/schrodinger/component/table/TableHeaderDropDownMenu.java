/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.table;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.common.DropDown;

/**
 * @author skublik
 */

public class TableHeaderDropDownMenu<T> extends DropDown<T> {

    public TableHeaderDropDownMenu(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

}
