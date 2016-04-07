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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.WorkItemsTablePanel;
import com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalHistoryPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author mederly
 */
public class WorkItemPanel extends BasePanel<WorkItemDto> {

    private static final Trace LOGGER = TraceManager.getTrace(WorkItemPanel.class);

    private static final String ID_REQUESTED_BY = "requestedBy";
    private static final String ID_REQUESTED_BY_FULL_NAME = "requestedByFullName";
    private static final String ID_REQUESTED_ON = "requestedOn";
    private static final String ID_WORK_ITEM_CREATED_ON = "workItemCreatedOn";
    private static final String ID_ASSIGNEE = "assignee";
    private static final String ID_CANDIDATES = "candidates";
    private static final String ID_DELTAS_TO_BE_APPROVED = "deltasToBeApproved";
    private static final String ID_HISTORY = "history";
    private static final String ID_RELATED_WORK_ITEMS_CONTAINER = "relatedWorkItemsContainer";
    private static final String ID_RELATED_WORK_ITEMS = "relatedWorkItems";
    private static final String ID_RELATED_REQUESTS_CONTAINER = "relatedRequestsContainer";
    private static final String ID_RELATED_REQUESTS = "relatedRequests";
    private static final String ID_APPROVER_COMMENT = "approverComment";

    public WorkItemPanel(String id, IModel<WorkItemDto> model, PageBase pageBase) {
        super(id, model);
        initLayout(pageBase);
    }

    protected void initLayout(PageBase pageBase) {
        add(new Label(ID_REQUESTED_BY, new PropertyModel(getModel(), WorkItemDto.F_REQUESTER_NAME)));
        add(new Label(ID_REQUESTED_BY_FULL_NAME, new PropertyModel(getModel(), WorkItemDto.F_REQUESTER_FULL_NAME)));
        add(new Label(ID_REQUESTED_ON, new PropertyModel(getModel(), WorkItemDto.F_PROCESS_STARTED)));
        add(new Label(ID_WORK_ITEM_CREATED_ON, new PropertyModel(getModel(), WorkItemDto.F_CREATED)));
        add(new Label(ID_ASSIGNEE, new PropertyModel(getModel(), WorkItemDto.F_ASSIGNEE)));
        add(new Label(ID_CANDIDATES, new PropertyModel(getModel(), WorkItemDto.F_CANDIDATES)));
        add(new ItemApprovalHistoryPanel(ID_HISTORY, new PropertyModel<WfContextType>(getModel(), WorkItemDto.F_WORKFLOW_CONTEXT),
				UserProfileStorage.TableId.PAGE_WORK_ITEM_HISTORY_PANEL, (int) pageBase.getItemsPerPage(UserProfileStorage.TableId.PAGE_WORK_ITEM_HISTORY_PANEL)));
        add(new ScenePanel(ID_DELTAS_TO_BE_APPROVED, new PropertyModel<SceneDto>(getModel(), WorkItemDto.F_DELTAS)));

		final IModel<List<WorkItemDto>> relatedWorkItemsModel = new PropertyModel<>(getModel(), WorkItemDto.F_OTHER_WORK_ITEMS);
		WebMarkupContainer relatedWorkItemsContainer = new WebMarkupContainer(ID_RELATED_WORK_ITEMS_CONTAINER);
		relatedWorkItemsContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !relatedWorkItemsModel.getObject().isEmpty();
			}
		});
		add(relatedWorkItemsContainer);
		final ISortableDataProvider<WorkItemDto, String> relatedWorkItemsProvider = new ListDataProvider<>(this, relatedWorkItemsModel);
		relatedWorkItemsContainer.add(new WorkItemsTablePanel(ID_RELATED_WORK_ITEMS, relatedWorkItemsProvider, null, 10, WorkItemsTablePanel.View.ITEMS_FOR_PROCESS));

		final IModel<List<ProcessInstanceDto>> relatedWorkflowRequestsModel = new PropertyModel<>(getModel(), WorkItemDto.F_RELATED_WORKFLOW_REQUESTS);
		WebMarkupContainer relatedWorkflowRequestsContainer = new WebMarkupContainer(ID_RELATED_REQUESTS_CONTAINER);
		relatedWorkflowRequestsContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !relatedWorkflowRequestsModel.getObject().isEmpty();
			}
		});
		add(relatedWorkflowRequestsContainer);
		final ISortableDataProvider<ProcessInstanceDto, String> relatedWorkflowRequestsProvider = new ListDataProvider<>(this, relatedWorkflowRequestsModel);
		relatedWorkflowRequestsContainer.add(
				new WorkflowRequestsPanel(ID_RELATED_REQUESTS, relatedWorkflowRequestsProvider, null, 10,
						WorkflowRequestsPanel.View.TASKS_FOR_PROCESS, new PropertyModel<String>(getModel(), WorkItemDto.F_PROCESS_INSTANCE_ID)));
		
        add(new TextArea<>(ID_APPROVER_COMMENT, new PropertyModel<String>(getModel(), WorkItemDto.F_APPROVER_COMMENT)));
    }

}
