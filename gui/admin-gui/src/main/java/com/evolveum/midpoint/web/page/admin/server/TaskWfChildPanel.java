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
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.WorkItemsTablePanel;
import com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalHistoryPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.WorkflowRequestsPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public class TaskWfChildPanel extends Panel {

	private static final long serialVersionUID = 1L;

	private static final String ID_DELTAS_TO_BE_APPROVED = "deltasToBeApproved";
	private static final String ID_HISTORY = "history";
	private static final String ID_CURRENT_WORK_ITEMS = "currentWorkItems";
	private static final String ID_RELATED_REQUESTS_CONTAINER = "relatedRequestsContainer";
	private static final String ID_RELATED_REQUESTS = "relatedRequests";
	private static final String ID_SHOW_PARENT = "showParent";

	private static final Trace LOGGER = TraceManager.getTrace(TaskApprovalsTabPanel.class);

	public TaskWfChildPanel(String id, IModel<TaskDto> taskDtoModel, PageBase pageBase) {
		super(id);
		initLayout(taskDtoModel, pageBase);
		setOutputMarkupId(true);
	}

	private void initLayout(final IModel<TaskDto> taskDtoModel, PageBase pageBase) {

		final ScenePanel deltasToBeApproved = new ScenePanel(ID_DELTAS_TO_BE_APPROVED, new PropertyModel(taskDtoModel, TaskDto.F_WORKFLOW_DELTA_IN));
		deltasToBeApproved.setOutputMarkupId(true);
		add(deltasToBeApproved);

		final ItemApprovalHistoryPanel history = new ItemApprovalHistoryPanel(ID_HISTORY,
				new PropertyModel<WfContextType>(taskDtoModel, TaskDto.F_WORKFLOW_CONTEXT),
				UserProfileStorage.TableId.PAGE_TASK_HISTORY_PANEL,
				(int) pageBase.getItemsPerPage(UserProfileStorage.TableId.PAGE_TASK_HISTORY_PANEL));
		history.setOutputMarkupId(true);
		add(history);

		ISortableDataProvider<WorkItemDto, String> provider = new ListDataProvider(this, new PropertyModel<List<WorkItemDto>>(taskDtoModel, TaskDto.F_WORK_ITEMS));
		final WorkItemsTablePanel workItemsPanel = new WorkItemsTablePanel(ID_CURRENT_WORK_ITEMS, provider,
				UserProfileStorage.TableId.PAGE_TASK_CURRENT_WORK_ITEMS_PANEL,
				(int) pageBase.getItemsPerPage(UserProfileStorage.TableId.PAGE_TASK_CURRENT_WORK_ITEMS_PANEL),
				WorkItemsTablePanel.View.ITEMS_FOR_PROCESS);
		workItemsPanel.setOutputMarkupId(true);
		add(workItemsPanel);

		final PropertyModel<List<ProcessInstanceDto>> relatedRequestsModel = new PropertyModel<>(taskDtoModel, TaskDto.F_WORKFLOW_REQUESTS);

		WebMarkupContainer relatedRequestsContainer = new WebMarkupContainer(ID_RELATED_REQUESTS_CONTAINER);
		relatedRequestsContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(relatedRequestsModel.getObject());
			}
		});
		relatedRequestsContainer.setOutputMarkupId(true);
		add(relatedRequestsContainer);
		ISortableDataProvider<ProcessInstanceDto, String> requestsProvider = new ListDataProvider(this, relatedRequestsModel);
		relatedRequestsContainer.add(new WorkflowRequestsPanel(ID_RELATED_REQUESTS, requestsProvider, null, 10,
				WorkflowRequestsPanel.View.TASKS_FOR_PROCESS, null));

		add(new AjaxFallbackLink(ID_SHOW_PARENT) {
			public void onClick(AjaxRequestTarget target) {
				String oid = taskDtoModel.getObject().getParentTaskOid();
				if (oid != null) {
					PageParameters parameters = new PageParameters();
					parameters.add(OnePageParameterEncoder.PARAMETER, oid);
					setResponsePage(new PageTaskEdit(parameters));
				}
			}
		});
	}

	public Collection<Component> getComponentsToUpdate() {
		return Arrays.asList(
				get(ID_DELTAS_TO_BE_APPROVED),
				get(ID_HISTORY),
				get(ID_CURRENT_WORK_ITEMS),
				get(ID_RELATED_REQUESTS_CONTAINER)
		);
		// exclude 'show parent' link
	}
}
