/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server.subtasks;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.MyWorkItemDto;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class SubtasksPanel extends SimplePanel<List<TaskDto>> {

    private static final String ID_SUBTASKS_TABLE = "subtasksTable";

    public SubtasksPanel(String id, IModel<List<TaskDto>> model, boolean workflowsEnabled) {
        super(id, model);
        initLayoutLocal(workflowsEnabled);
    }

    private void initLayoutLocal(boolean workflowsEnabled) {
        List<IColumn<TaskDto, String>> columns = new ArrayList<IColumn<TaskDto, String>>();
        columns.add(PageTasks.createTaskNameColumn(this, "SubtasksPanel.label.name"));
        columns.add(PageTasks.createTaskCategoryColumn(this, "SubtasksPanel.label.category"));
        columns.add(PageTasks.createTaskExecutionStatusColumn(this, "SubtasksPanel.label.executionState"));
        columns.add(PageTasks.createTaskResultStatusColumn(this, "SubtasksPanel.label.result"));
        columns.add(PageTasks.createTaskDetailColumn(this, "SubtasksPanel.label.detail", workflowsEnabled));

        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        add(new TablePanel<TaskDto>(ID_SUBTASKS_TABLE, provider, columns));

    }
}
