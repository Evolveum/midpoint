/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.server.workflowInformation;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wf.WfDeltasPanel;
import com.evolveum.midpoint.web.component.wf.WfHistoryEventDto;
import com.evolveum.midpoint.web.component.wf.WfHistoryPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstance;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * @author mederly
 */
public class WorkflowInformationPanel extends SimplePanel<TaskDto> {

    private static final String ID_DELTAS_PANEL = "deltasPanel";
    private static final String ID_HISTORY_PANEL = "historyPanel";
    private static final String ID_PAGE_PROCESS_INSTANCE_LINK = "pageProcessInstanceLink";

    public WorkflowInformationPanel(String id, IModel<TaskDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        add(new LinkPanel(ID_PAGE_PROCESS_INSTANCE_LINK, new StringResourceModel("WorkflowInformationPanel.link.processInstance", getModel())) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String pid = WorkflowInformationPanel.this.getModel().getObject().getWorkflowProcessInstanceId();
                boolean finished = WorkflowInformationPanel.this.getModel().getObject().isWorkflowProcessInstanceFinished();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, pid);
                WorkflowInformationPanel.this.setResponsePage(new PageProcessInstance(parameters, (PageBase) WorkflowInformationPanel.this.getPage()));
            }
        });

        add(new WfDeltasPanel(ID_DELTAS_PANEL, getModel()));
        add(new WfHistoryPanel(ID_HISTORY_PANEL, new PropertyModel<List<WfHistoryEventDto>>(getModel(), TaskDto.F_WORKFLOW_HISTORY)));
    }
}
