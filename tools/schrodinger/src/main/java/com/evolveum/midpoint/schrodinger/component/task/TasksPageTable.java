/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.task;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.page.task.ListTasksPage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by matus on 6/25/2018.
 */
public class TasksPageTable extends AssignmentHolderObjectListTable<ListTasksPage, TaskPage> {
    public TasksPageTable(ListTasksPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    @Override
    public TaskPage clickByName(String name) {

        SelenideElement label = getParentElement().$(Schrodinger.byElementValue("span", "data-s-id", "label", name));
        label.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        label.waitWhile(Condition.exist, MidPoint.TIMEOUT_MEDIUM_6_S);

        return new TaskPage();
    }

    @Override
    protected TableHeaderDropDownMenu<TasksPageTable> clickHeaderActionDropDown() {
        return null;
    }

    @Override
    public TaskPage getObjectDetailsPage(){
        return new TaskPage();
    }

}
