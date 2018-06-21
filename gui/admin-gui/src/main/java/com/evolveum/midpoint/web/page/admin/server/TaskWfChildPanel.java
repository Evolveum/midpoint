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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.SwitchableApprovalProcessPreviewsPanel;
import com.evolveum.midpoint.web.component.wf.WorkItemsPanel;
import com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalHistoryPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskChangesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.EvaluatedTriggerGroupListPanel;
import com.evolveum.midpoint.web.page.admin.workflow.ProcessInstancesPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public class TaskWfChildPanel extends Panel {

	private static final long serialVersionUID = 1L;

	private static final String ID_CHANGES = "changes";
	private static final String ID_TRIGGERS_CONTAINER = "triggersContainer";
	private static final String ID_TRIGGERS = "triggers";
	private static final String ID_HISTORY = "history";
	private static final String ID_HISTORY_HELP = "approvalHistoryHelp";
	private static final String ID_CURRENT_WORK_ITEMS_CONTAINER = "currentWorkItemsContainer";
	private static final String ID_CURRENT_WORK_ITEMS = "currentWorkItems";
	private static final String ID_CURRENT_WORK_ITEMS_HELP = "currentWorkItemsHelp";
	private static final String ID_RELATED_REQUESTS_CONTAINER = "relatedRequestsContainer";
	private static final String ID_RELATED_REQUESTS = "relatedRequests";
	private static final String ID_RELATED_REQUESTS_HELP = "relatedRequestsHelp";
	private static final String ID_SHOW_PARENT = "showParent";
	private static final String ID_SHOW_PARENT_HELP = "showParentHelp";
	private static final String ID_PREVIEWS_PANEL = "previewsPanel";

	private static final Trace LOGGER = TraceManager.getTrace(TaskApprovalsTabPanel.class);

	private PropertyModel<TaskChangesDto> changesModel;
	private PageTaskEdit parentPage;

	public TaskWfChildPanel(String id, IModel<TaskDto> taskDtoModel, PageTaskEdit parentPage) {
		super(id);
		this.parentPage = parentPage;
		initLayout(taskDtoModel);
		setOutputMarkupId(true);
	}

	private void initLayout(final IModel<TaskDto> taskDtoModel) {

		changesModel = new PropertyModel<>(taskDtoModel, TaskDto.F_CHANGE_BEING_APPROVED);
		TaskChangesPanel changesPanel = new TaskChangesPanel(ID_CHANGES, changesModel);
		changesPanel.setOutputMarkupId(true);
		add(changesPanel);

		WebMarkupContainer triggersContainer = new WebMarkupContainer(ID_TRIGGERS_CONTAINER);
		PropertyModel<List<EvaluatedTriggerGroupDto>> triggersModel = new PropertyModel<>(taskDtoModel, TaskDto.F_TRIGGERS);
		WebMarkupContainer triggers = new EvaluatedTriggerGroupListPanel(ID_TRIGGERS, triggersModel);
		triggersContainer.add(triggers);
		triggersContainer.add(new VisibleBehaviour(() -> !EvaluatedTriggerGroupDto.isEmpty(triggersModel.getObject())));
		add(triggersContainer);

		final ItemApprovalHistoryPanel history = new ItemApprovalHistoryPanel(ID_HISTORY,
				new PropertyModel<>(taskDtoModel, TaskDto.F_WORKFLOW_CONTEXT),
				UserProfileStorage.TableId.PAGE_TASK_HISTORY_PANEL,
				(int) parentPage.getItemsPerPage(UserProfileStorage.TableId.PAGE_TASK_HISTORY_PANEL));
		history.setOutputMarkupId(true);
		add(history);
		add(WebComponentUtil.createHelp(ID_HISTORY_HELP));

		WebMarkupContainer workItemsContainer = new WebMarkupContainer(ID_CURRENT_WORK_ITEMS_CONTAINER);
		final ISortableDataProvider<WorkItemDto, String> provider = new ListDataProvider(this, new PropertyModel<List<WorkItemDto>>(taskDtoModel, TaskDto.F_WORK_ITEMS));
		final WorkItemsPanel workItemsPanel = new WorkItemsPanel(ID_CURRENT_WORK_ITEMS, provider,
				UserProfileStorage.TableId.PAGE_TASK_CURRENT_WORK_ITEMS_PANEL,
				(int) parentPage.getItemsPerPage(UserProfileStorage.TableId.PAGE_TASK_CURRENT_WORK_ITEMS_PANEL),
				WorkItemsPanel.View.ITEMS_FOR_PROCESS);
		workItemsPanel.setOutputMarkupId(true);
		workItemsContainer.add(workItemsPanel);
		workItemsContainer.setOutputMarkupId(true);
		workItemsContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return provider.size() > 0;
			}
		});
		workItemsContainer.add(WebComponentUtil.createHelp(ID_CURRENT_WORK_ITEMS_HELP));
		add(workItemsContainer);

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
		relatedRequestsContainer.add(new ProcessInstancesPanel(ID_RELATED_REQUESTS, requestsProvider, null, 10,
				ProcessInstancesPanel.View.TASKS_FOR_PROCESS, null));
		relatedRequestsContainer.add(WebComponentUtil.createHelp(ID_RELATED_REQUESTS_HELP));

		IModel<String> taskOidModel = new PropertyModel<>(taskDtoModel, TaskDto.F_OID);
		IModel<Boolean> showNextStagesModel = new PropertyModel<>(taskDtoModel, TaskDto.F_IN_STAGE_BEFORE_LAST_ONE);
		add(new SwitchableApprovalProcessPreviewsPanel(ID_PREVIEWS_PANEL, taskOidModel, showNextStagesModel, parentPage));
		add(new AjaxFallbackLink(ID_SHOW_PARENT) {
			public void onClick(AjaxRequestTarget target) {
				String oid = taskDtoModel.getObject().getParentTaskOid();
				if (oid != null) {
					PageParameters parameters = new PageParameters();
					parameters.add(OnePageParameterEncoder.PARAMETER, oid);
					((PageBase) getPage()).navigateToNext(PageTaskEdit.class, parameters);
				}
			}
		});
		add(WebComponentUtil.createHelp(ID_SHOW_PARENT_HELP));
	}

	public Collection<Component> getComponentsToUpdate() {

		TaskDto curr = parentPage.getCurrentTaskDto();
		TaskDto prev = parentPage.getPreviousTaskDto();
		boolean changesChanged = prev == null || prev.getChangesBeingApproved() == null || !prev.getChangesBeingApproved().equals(curr.getChangesBeingApproved());

		List<Component> rv = new ArrayList<>();
		if (changesChanged) {
			rv.add(get(ID_CHANGES));
		} else {
			curr.getChangesBeingApproved().applyFoldingFrom(prev.getChangesBeingApproved());
		}
		rv.add(get(ID_HISTORY));
		rv.add(get(ID_CURRENT_WORK_ITEMS_CONTAINER));
		rv.add(get(ID_RELATED_REQUESTS_CONTAINER));
		return rv; // we exclude 'show parent' link
	}
}
