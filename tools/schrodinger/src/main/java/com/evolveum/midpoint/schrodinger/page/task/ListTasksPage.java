/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.task;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.component.task.TasksPageTable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListTasksPage extends AssignmentHolderObjectListPage<TasksPageTable, TaskPage> {

    @Override
    public TasksPageTable table() {
        return new TasksPageTable(this, getTableBoxElement());
    }

    @Override
    public TaskPage getObjectDetailsPage() {
        return new TaskPage();
    }

}
