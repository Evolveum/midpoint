/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskChangesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.WorkflowRequestsPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author mederly
 */
public class TaskWfParentPanel extends Panel {

	private static final long serialVersionUID = 1L;

	private static final String ID_REQUESTS = "requests";
	private static final String ID_CHANGES_PREFIX = "changes";				// e.g. changes3Content
	public static final String ID_CHANGES_CONTENT_SUFFIX = "Content";
	public static final int CHANGES_NUMBER = 5;

	private static final Trace LOGGER = TraceManager.getTrace(TaskApprovalsTabPanel.class);

	public TaskWfParentPanel(String id, IModel<TaskDto> taskDtoModel, PageBase pageBase) {
		super(id);
		initLayout(taskDtoModel, pageBase);
		setOutputMarkupId(true);
	}

	private void initLayout(final IModel<TaskDto> taskDtoModel, PageBase pageBase) {
		final PropertyModel<List<ProcessInstanceDto>> requestsModel = new PropertyModel<>(taskDtoModel, TaskDto.F_WORKFLOW_REQUESTS);
		final ISortableDataProvider<ProcessInstanceDto, String> requestsProvider = new ListDataProvider<>(this, requestsModel);
		add(new WorkflowRequestsPanel(ID_REQUESTS, requestsProvider, null, 10, WorkflowRequestsPanel.View.TASKS_FOR_PROCESS, null));

		for (int i = 1; i <= CHANGES_NUMBER; i++) {
			final int index = i;
			final String changesId = ID_CHANGES_PREFIX + i;
			final String changesContentId = changesId + ID_CHANGES_CONTENT_SUFFIX;
			final WebMarkupContainer changes = new WebMarkupContainer(changesId);
			final IModel<TaskChangesDto> changesModel = new AbstractReadOnlyModel<TaskChangesDto>() {
				@Override
				public TaskChangesDto getObject() {
					return taskDtoModel.getObject().getChangesForIndex(index);
				}
			};
			final TaskChangesPanel changesPanel = new TaskChangesPanel(changesContentId, changesModel);
			changes.add(changesPanel);
			changes.add(new VisibleEnableBehaviour() {
				@Override
				public boolean isVisible() {
					return changesModel.getObject() != null;
				}
			});
			add(changes);
		}
	}

}
