/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.task;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.table.InputTable;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NewTaskPage extends BasicPage {

    public InputTable<NewTaskPage> basicTable() {

        SelenideElement tableElement = $(Schrodinger.byPrecedingSiblingEnclosedValue("table", "class", "table table-condensed table-striped", "", "", "Basic"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new InputTable<>(this, tableElement);
    }

    public InputTable<NewTaskPage> schedulingTable() {

        SelenideElement tableElement = $(Schrodinger.byPrecedingSiblingEnclosedValue("table", "class", "table table-condensed table-striped", "", "", "Scheduling"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new InputTable<>(this, tableElement);
    }

    public ListTasksPage clickSave() {

        $(Schrodinger.byDataId("saveButton"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return new ListTasksPage();
    }

}
