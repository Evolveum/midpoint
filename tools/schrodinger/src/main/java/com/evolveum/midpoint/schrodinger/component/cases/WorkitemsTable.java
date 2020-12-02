/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.cases;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.cases.WorkitemPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by honchar
 */
public class WorkitemsTable<T> extends TableWithPageRedirect<T> {

    public WorkitemsTable(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public WorkitemPage clickByName(String name) {
        getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new WorkitemPage();
    }

    @Override
    public WorkitemsTable<T> selectCheckboxByName(String name) {
        return null;
    }

    @Override
    protected TableHeaderDropDownMenu<WorkitemsTable> clickHeaderActionDropDown() {
        return null;

    }
}
