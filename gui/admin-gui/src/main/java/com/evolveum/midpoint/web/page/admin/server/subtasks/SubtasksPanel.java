/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.server.subtasks;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * @author mederly
 */
public class SubtasksPanel extends BasePanel<List<TaskDto>> {

    private static final String ID_SUBTASKS_TABLE = "subtasksTable";
    
    private boolean workflowsEnabled;

    public SubtasksPanel(String id, IModel<List<TaskDto>> model, boolean workflowsEnabled) {
        super(id, model);
        this.workflowsEnabled = workflowsEnabled;
        
    }
    
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout();
    }

    private void initLayout() {
        List<IColumn<TaskDto, String>> columns = new ArrayList<>();
        columns.add(PageTasks.createTaskNameColumn(this, "SubtasksPanel.label.name"));
        columns.add(createTaskKindColumn());
        columns.add(PageTasks.createTaskCategoryColumn(this, "SubtasksPanel.label.category"));
        columns.add(PageTasks.createTaskExecutionStatusColumn(this, "SubtasksPanel.label.executionState"));
        columns.add(PageTasks.createProgressColumn(getPageBase(), "SubtasksPanel.label.progress"));
        columns.add(PageTasks.createTaskResultStatusColumn(this, "SubtasksPanel.label.result"));
        //columns.add(PageTasks.createTaskDetailColumn(this, "SubtasksPanel.label.detail", workflowsEnabled));

        ISortableDataProvider provider = new ListDataProvider(this, getModel(), true);
        add(new TablePanel<>(ID_SUBTASKS_TABLE, provider, columns));

    }
    
    public EnumPropertyColumn createTaskKindColumn() {
		return new EnumPropertyColumn(createStringResource("SubtasksPanel.label.kind"), TaskDto.F_TASK_KIND) {

			@Override
			protected String translate(Enum en) {
				return createStringResource(en).getString();
			}
		};
	}
    
}
