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

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskChangesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.ProcessInstancesPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public class TaskWfParentPanel extends Panel {

	private static final long serialVersionUID = 1L;

	private static final String ID_REQUESTS = "requests";
	private static final String ID_REQUESTS_HELP = "requestsHelp";
	private static final String ID_CHANGES_CONTAINER = "changesContainer";
	private static final String ID_CHANGES_PREFIX = "changes";				// e.g. changes3Content
	public static final String ID_CHANGES_CONTENT_SUFFIX = "Content";
	public static final int CHANGES_NUMBER = 6;

	private static final Trace LOGGER = TraceManager.getTrace(TaskApprovalsTabPanel.class);

	private PageTaskEdit parentPage;
	private ProcessInstancesPanel processInstancesPanel;
	private WebMarkupContainer changesContainer;

	public TaskWfParentPanel(String id, IModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
		super(id);
		this.parentPage = parentPage;
		initLayout(taskDtoModel);
		setOutputMarkupId(true);
	}

	private void initLayout(final IModel<TaskDto> taskDtoModel) {
		final PropertyModel<List<ProcessInstanceDto>> requestsModel = new PropertyModel<>(taskDtoModel, TaskDto.F_WORKFLOW_REQUESTS);
		final ISortableDataProvider<ProcessInstanceDto, String> requestsProvider = new ListDataProvider<>(this, requestsModel);
		processInstancesPanel = new ProcessInstancesPanel(ID_REQUESTS, requestsProvider, null, 10, ProcessInstancesPanel.View.TASKS_FOR_PROCESS, null);
		processInstancesPanel.setOutputMarkupId(true);
		add(processInstancesPanel);
		add(WebComponentUtil.createHelp(ID_REQUESTS_HELP));

		changesContainer = new WebMarkupContainer(ID_CHANGES_CONTAINER);
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
			changesContainer.add(changes);
		}
		changesContainer.setOutputMarkupId(true);
		add(changesContainer);
	}

	public Collection<Component> getComponentsToUpdate() {

		TaskDto curr = parentPage.getCurrentTaskDto();
		TaskDto prev = parentPage.getPreviousTaskDto();
		List<TaskChangesDto> prevList = prev != null ? prev.getChangesCategorizationList() : null;
		List<TaskChangesDto> currList = curr.getChangesCategorizationList();
		boolean changesChanged = prev == null || !prevList.equals(currList);

		List<Component> rv = new ArrayList<>();
		if (changesChanged) {
			rv.add(changesContainer);
		} else {
			for (int i = 0; i < currList.size(); i++) {
				currList.get(i).applyFoldingFrom(prevList.get(i));
			}
		}
		rv.add(processInstancesPanel);
		return rv;
	}

}
