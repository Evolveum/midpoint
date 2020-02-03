/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.task;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.component.task.TasksPageTable;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListTasksPage extends AssignmentHolderObjectListPage<TasksPageTable> {

    public TasksPageTable table() {
        SelenideElement box = $(Schrodinger.byDataId("div", "table"));

        return new TasksPageTable(this, box);
    }

}
