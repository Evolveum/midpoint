/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author mederly
 * @author semancik
 */
public class FocusTasksTabPanel<F extends FocusType>
		extends AbstractObjectTabPanel<F> {
	private static final long serialVersionUID = 1L;

	protected static final String ID_TASK_TABLE = "taskTable";
	protected static final String ID_LABEL = "label";

	private static final Trace LOGGER = TraceManager.getTrace(FocusTasksTabPanel.class);

	private TaskDtoProvider taskDtoProvider;

	public FocusTasksTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel,
			TaskDtoProvider taskDtoProvider, PageBase page) {
		super(id, mainForm, focusModel, page);
		this.taskDtoProvider = taskDtoProvider;
		initLayout(page);
	}

	private void initLayout(final PageBase page) {

		Label label = new Label(ID_LABEL, new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				if (taskDtoProvider.size() > 0) {
					return getString("pageAdminFocus.task.descriptionHasTasks");
				} else {
					return getString("pageAdminFocus.task.descriptionNoTasks");
				}
			}
		});
		add(label);

		List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();
		TablePanel taskTable = new TablePanel<TaskDto>(ID_TASK_TABLE, taskDtoProvider, taskColumns);
		add(taskTable);

		taskTable.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return taskDtoProvider.size() > 0;
			}
		});
	}

	private List<IColumn<TaskDto, String>> initTaskColumns() {
		List<IColumn<TaskDto, String>> columns = new ArrayList<IColumn<TaskDto, String>>();

		columns.add(PageTasks.createTaskNameColumn(this, "pageAdminFocus.task.name"));
		columns.add(PageTasks.createTaskCategoryColumn(this, "pageAdminFocus.task.category"));
		columns.add(PageTasks.createTaskExecutionStatusColumn(this, "pageAdminFocus.task.execution"));
		columns.add(PageTasks.createTaskResultStatusColumn(this, "pageAdminFocus.task.status"));
		return columns;
	}


}
