/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.TaskDtoTablePanel;
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

	public FocusTasksTabPanel(String id, Form mainForm, LoadableModel<PrismObjectWrapper<F>> focusModel,
			TaskDtoProvider taskDtoProvider) {
		super(id, mainForm, focusModel);
		this.taskDtoProvider = taskDtoProvider;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	private void initLayout() {

		Label label = new Label(ID_LABEL, new IModel<String>() {
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
		TablePanel taskTable = new TablePanel<>(ID_TASK_TABLE, taskDtoProvider, taskColumns);
		add(taskTable);

		taskTable.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return taskDtoProvider.size() > 0;
			}
		});
	}

	private List<IColumn<TaskDto, String>> initTaskColumns() {
		List<IColumn<TaskDto, String>> columns = new ArrayList<>();

		columns.add(TaskDtoTablePanel.createTaskNameColumn(this, "pageAdminFocus.task.name"));
		columns.add(TaskDtoTablePanel.createTaskCategoryColumn(this, "pageAdminFocus.task.category"));
		columns.add(TaskDtoTablePanel.createTaskExecutionStatusColumn(this, "pageAdminFocus.task.execution"));
		columns.add(TaskDtoTablePanel.createTaskResultStatusColumn(this, "pageAdminFocus.task.status"));
		return columns;
	}


}
