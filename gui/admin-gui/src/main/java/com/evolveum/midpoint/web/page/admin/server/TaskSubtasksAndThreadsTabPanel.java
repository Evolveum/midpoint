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
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.WorkerThreadDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.subtasks.SubtasksPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.gui.api.page.PageBase.createStringResourceStatic;

/**
 * @author semancik
 */
public class TaskSubtasksAndThreadsTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_WORKER_THREADS = "workerThreads";
	private static final String ID_THREADS_CONFIGURATION_PANEL = "threadsConfigurationPanel";

	private static final String ID_WORKER_THREADS_TABLE = "workerThreadsTable";
	private static final String ID_WORKER_THREADS_TABLE_LABEL = "workerThreadsTableLabel";

	private static final String ID_SUBTASKS_LABEL = "subtasksLabel";
	private static final String ID_SUBTASKS_PANEL = "subtasksPanel";

	private static final Trace LOGGER = TraceManager.getTrace(TaskSubtasksAndThreadsTabPanel.class);

	private PageTaskEdit parentPage;

	public TaskSubtasksAndThreadsTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			IModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
		super(id, mainForm, taskWrapperModel, parentPage);
		this.parentPage = parentPage;
		initLayout(taskDtoModel);
		setOutputMarkupId(true);
	}

	private void initLayout(final IModel<TaskDto> taskDtoModel) {

		WebMarkupContainer threadsConfigurationPanel = new WebMarkupContainer(ID_THREADS_CONFIGURATION_PANEL);
		add(threadsConfigurationPanel);

		threadsConfigurationPanel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return taskDtoModel.getObject().configuresWorkerThreads();
			}
		});

		final TextField<Integer> workerThreads = new TextField<>(ID_WORKER_THREADS, new PropertyModel<Integer>(taskDtoModel, TaskDto.F_WORKER_THREADS));
		workerThreads.setOutputMarkupId(true);
		workerThreads.add(new VisibleEnableBehaviour(){
			@Override
			public boolean isEnabled() {
				return parentPage.isEdit();
			}
		});
		threadsConfigurationPanel.add(workerThreads);

		VisibleEnableBehaviour hiddenWhenEditingOrNoSubtasks = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !parentPage.isEdit() && !taskDtoModel.getObject().getSubtasks().isEmpty();
			}
		};

		Label subtasksLabel = new Label(ID_SUBTASKS_LABEL, new ResourceModel("pageTaskEdit.subtasksLabel"));
		subtasksLabel.add(hiddenWhenEditingOrNoSubtasks);
		add(subtasksLabel);
		SubtasksPanel subtasksPanel = new SubtasksPanel(ID_SUBTASKS_PANEL, new PropertyModel<>(taskDtoModel, TaskDto.F_SUBTASKS), parentPage.getWorkflowManager().isEnabled());
		subtasksPanel.add(hiddenWhenEditingOrNoSubtasks);
		add(subtasksPanel);

		VisibleEnableBehaviour hiddenWhenNoSubtasks = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				TaskDto taskDto = taskDtoModel.getObject();
				return taskDto != null && !taskDto.getTransientSubtasks().isEmpty();
			}
		};

		Label workerThreadsTableLabel = new Label(ID_WORKER_THREADS_TABLE_LABEL, new ResourceModel("TaskStatePanel.workerThreads"));
		workerThreadsTableLabel.add(hiddenWhenNoSubtasks);
		add(workerThreadsTableLabel);
		List<IColumn<WorkerThreadDto, String>> columns = new ArrayList<>();
		columns.add(new PropertyColumn(createStringResourceStatic(this, "TaskStatePanel.subtaskName"), WorkerThreadDto.F_NAME));
		columns.add(new EnumPropertyColumn<>(createStringResourceStatic(this, "TaskStatePanel.subtaskState"), WorkerThreadDto.F_EXECUTION_STATUS));
		columns.add(new PropertyColumn(createStringResourceStatic(this, "TaskStatePanel.subtaskObjectsProcessed"), WorkerThreadDto.F_PROGRESS));
		ISortableDataProvider<WorkerThreadDto, String> threadsProvider = new ListDataProvider<>(this,
				new AbstractReadOnlyModel<List<WorkerThreadDto>>() {
					@Override
					public List<WorkerThreadDto> getObject() {
						List<WorkerThreadDto> rv = new ArrayList<>();
						TaskDto taskDto = taskDtoModel.getObject();
						if (taskDto != null) {
							for (TaskDto subtaskDto : taskDto.getTransientSubtasks()) {
								rv.add(new WorkerThreadDto(subtaskDto));
							}
						}
						return rv;
					}
				});
		TablePanel<WorkerThreadDto> workerThreadsTablePanel = new TablePanel<>(ID_WORKER_THREADS_TABLE, threadsProvider , columns);
		workerThreadsTablePanel.add(hiddenWhenNoSubtasks);
		add(workerThreadsTablePanel);

	}

	@Override
	public Collection<Component> getComponentsToUpdate() {
		return Collections.<Component>singleton(this);
	}

}
