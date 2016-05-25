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
package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.WorkItemsPanel;
import com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalHistoryPanel;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.TaskChangesPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskChangesDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * @author mederly
 */
public class WorkItemPanel extends BasePanel<WorkItemDto> {

    private static final Trace LOGGER = TraceManager.getTrace(WorkItemPanel.class);

    private static final String ID_PRIMARY_INFO_COLUMN = "primaryInfoColumn";
    private static final String ID_ADDITIONAL_INFO_COLUMN = "additionalInfoColumn";

    private static final String ID_REQUESTED_BY = "requestedBy";
    private static final String ID_REQUESTED_BY_FULL_NAME = "requestedByFullName";
    private static final String ID_REQUESTED_ON = "requestedOn";
    private static final String ID_WORK_ITEM_CREATED_ON = "workItemCreatedOn";
    private static final String ID_ASSIGNEE = "assignee";
    private static final String ID_CANDIDATES = "candidates";
    private static final String ID_DELTAS_TO_BE_APPROVED = "deltasToBeApproved";
    private static final String ID_HISTORY_CONTAINER = "historyContainer";
    private static final String ID_HISTORY = "history";
    private static final String ID_HISTORY_HELP = "approvalHistoryHelp";
    private static final String ID_RELATED_WORK_ITEMS_CONTAINER = "relatedWorkItemsContainer";
    private static final String ID_RELATED_WORK_ITEMS = "relatedWorkItems";
	private static final String ID_RELATED_WORK_ITEMS_HELP = "otherWorkItemsHelp";
    private static final String ID_RELATED_REQUESTS_CONTAINER = "relatedRequestsContainer";
    private static final String ID_RELATED_REQUESTS = "relatedRequests";
    private static final String ID_RELATED_REQUESTS_HELP = "relatedRequestsHelp";
    private static final String ID_APPROVER_COMMENT = "approverComment";
	private static final String ID_SHOW_REQUEST = "showRequest";
	private static final String ID_SHOW_REQUEST_HELP = "showRequestHelp";

	public WorkItemPanel(String id, IModel<WorkItemDto> model, PageBase pageBase) {
        super(id, model);
        initLayout(pageBase);
    }

    protected void initLayout(PageBase pageBase) {
		WebMarkupContainer additionalInfoColumn = new WebMarkupContainer(ID_ADDITIONAL_INFO_COLUMN);

		WebMarkupContainer historyContainer = new WebMarkupContainer(ID_HISTORY_CONTAINER);
        historyContainer.add(new ItemApprovalHistoryPanel(ID_HISTORY, new PropertyModel<WfContextType>(getModel(), WorkItemDto.F_WORKFLOW_CONTEXT),
				UserProfileStorage.TableId.PAGE_WORK_ITEM_HISTORY_PANEL, (int) pageBase.getItemsPerPage(UserProfileStorage.TableId.PAGE_WORK_ITEM_HISTORY_PANEL)));
		final VisibleEnableBehaviour historyContainerVisible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().hasHistory();
			}
		};
		historyContainer.add(historyContainerVisible);
		historyContainer.add(WebComponentUtil.createHelp(ID_HISTORY_HELP));
		additionalInfoColumn.add(historyContainer);

		WebMarkupContainer relatedWorkItemsContainer = new WebMarkupContainer(ID_RELATED_WORK_ITEMS_CONTAINER);
		final IModel<List<WorkItemDto>> relatedWorkItemsModel = new PropertyModel<>(getModel(), WorkItemDto.F_OTHER_WORK_ITEMS);
		final ISortableDataProvider<WorkItemDto, String> relatedWorkItemsProvider = new ListDataProvider<>(this, relatedWorkItemsModel);
		relatedWorkItemsContainer.add(new WorkItemsPanel(ID_RELATED_WORK_ITEMS, relatedWorkItemsProvider, null, 10, WorkItemsPanel.View.ITEMS_FOR_PROCESS));
		final VisibleEnableBehaviour relatedWorkItemsContainerVisible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !relatedWorkItemsModel.getObject().isEmpty();
			}
		};
		relatedWorkItemsContainer.add(relatedWorkItemsContainerVisible);
		relatedWorkItemsContainer.add(WebComponentUtil.createHelp(ID_RELATED_WORK_ITEMS_HELP));
		additionalInfoColumn.add(relatedWorkItemsContainer);

		final WebMarkupContainer relatedWorkflowRequestsContainer = new WebMarkupContainer(ID_RELATED_REQUESTS_CONTAINER);
		final IModel<List<ProcessInstanceDto>> relatedWorkflowRequestsModel = new PropertyModel<>(getModel(), WorkItemDto.F_RELATED_WORKFLOW_REQUESTS);
		final ISortableDataProvider<ProcessInstanceDto, String> relatedWorkflowRequestsProvider = new ListDataProvider<>(this, relatedWorkflowRequestsModel);
		relatedWorkflowRequestsContainer.add(
				new ProcessInstancesPanel(ID_RELATED_REQUESTS, relatedWorkflowRequestsProvider, null, 10,
						ProcessInstancesPanel.View.TASKS_FOR_PROCESS, new PropertyModel<String>(getModel(), WorkItemDto.F_PROCESS_INSTANCE_ID)));
		final VisibleEnableBehaviour relatedWorkflowRequestsContainerVisible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !relatedWorkflowRequestsModel.getObject().isEmpty();
			}
		};
		relatedWorkflowRequestsContainer.add(relatedWorkflowRequestsContainerVisible);
		relatedWorkflowRequestsContainer.add(WebComponentUtil.createHelp(ID_RELATED_REQUESTS_HELP));
		additionalInfoColumn.add(relatedWorkflowRequestsContainer);
		final VisibleEnableBehaviour additionalInfoColumnVisible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return historyContainerVisible.isVisible() || relatedWorkItemsContainerVisible.isVisible() || relatedWorkflowRequestsContainerVisible
						.isVisible();
			}
		};
		additionalInfoColumn.add(additionalInfoColumnVisible);
		add(additionalInfoColumn);

		WebMarkupContainer primaryInfoColumn = new WebMarkupContainer(ID_PRIMARY_INFO_COLUMN);

		primaryInfoColumn.add(new Label(ID_REQUESTED_BY, new PropertyModel(getModel(), WorkItemDto.F_REQUESTER_NAME)));
		primaryInfoColumn.add(new Label(ID_REQUESTED_BY_FULL_NAME, new PropertyModel(getModel(), WorkItemDto.F_REQUESTER_FULL_NAME)));
		primaryInfoColumn.add(new Label(ID_REQUESTED_ON, new PropertyModel(getModel(), WorkItemDto.F_STARTED_FORMATTED_FULL)));
		primaryInfoColumn.add(new Label(ID_WORK_ITEM_CREATED_ON, new PropertyModel(getModel(), WorkItemDto.F_CREATED_FORMATTED_FULL)));
		primaryInfoColumn.add(new Label(ID_ASSIGNEE, new PropertyModel(getModel(), WorkItemDto.F_ASSIGNEE)));
		primaryInfoColumn.add(new Label(ID_CANDIDATES, new PropertyModel(getModel(), WorkItemDto.F_CANDIDATES)));
		//primaryInfoColumn.add(new ScenePanel(ID_DELTAS_TO_BE_APPROVED, new PropertyModel<SceneDto>(getModel(), WorkItemDto.F_DELTAS)));
		primaryInfoColumn.add(new TaskChangesPanel(ID_DELTAS_TO_BE_APPROVED, new PropertyModel<TaskChangesDto>(getModel(), WorkItemDto.F_CHANGES)));
		primaryInfoColumn.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return additionalInfoColumnVisible.isVisible() ? "col-md-5" : "col-md-12";
			}
		}));
		add(primaryInfoColumn);


		add(new AjaxFallbackLink(ID_SHOW_REQUEST) {
			public void onClick(AjaxRequestTarget target) {
				String oid = WorkItemPanel.this.getModelObject().getTaskOid();
				if (oid != null) {
					PageParameters parameters = new PageParameters();
					parameters.add(OnePageParameterEncoder.PARAMETER, oid);
					setResponsePage(PageTaskEdit.class, parameters);
				}
			}
		});
		add(WebComponentUtil.createHelp(ID_SHOW_REQUEST_HELP));

        add(new TextArea<>(ID_APPROVER_COMMENT, new PropertyModel<String>(getModel(), WorkItemDto.F_APPROVER_COMMENT)));
    }

}
