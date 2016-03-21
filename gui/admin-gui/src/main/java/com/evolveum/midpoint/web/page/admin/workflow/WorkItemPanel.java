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
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalHistoryPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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
        add(new ItemApprovalHistoryPanel(ID_HISTORY, new PropertyModel(getModel(), WorkItemDto.F_WORKFLOW_CONTEXT)));
        add(new ScenePanel(ID_DELTAS_TO_BE_APPROVED, new PropertyModel(getModel(), WorkItemDto.F_DELTAS), pageBase));
        add(new TextArea(ID_APPROVER_COMMENT, new PropertyModel(getModel(), WorkItemDto.F_APPROVER_COMMENT)));
    }

}
