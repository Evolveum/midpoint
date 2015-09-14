/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.server.currentState;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wf.WfDeltasPanel;
import com.evolveum.midpoint.web.component.wf.WfHistoryEventDto;
import com.evolveum.midpoint.web.component.wf.WfHistoryPanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstance;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * @author mederly
 */
public class TaskStatePanel extends SimplePanel<TaskDto> {

    private static final String ID_DELTAS_PANEL = "deltasPanel";
    private static final String ID_HISTORY_PANEL = "historyPanel";
    private static final String ID_PAGE_PROCESS_INSTANCE_LINK = "pageProcessInstanceLink";

    public TaskStatePanel(String id, IModel<TaskDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        SortableDataProvider<OperationResult, String> provider = new ListDataProvider<>(this,
                new PropertyModel<List<OperationResult>>(model, "opResult"));
        TablePanel result = new TablePanel<>("operationResult", provider, initResultColumns());
        result.setStyle("padding-top: 0px;");
        result.setShowPaging(false);
        result.setOutputMarkupId(true);
        result.add(hiddenWhenEditing);
        mainForm.add(result);

        add(new LinkPanel(ID_PAGE_PROCESS_INSTANCE_LINK, new StringResourceModel("WorkflowInformationPanel.link.processInstance", getModel())) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String pid = TaskStatePanel.this.getModel().getObject().getWorkflowProcessInstanceId();
                boolean finished = TaskStatePanel.this.getModel().getObject().isWorkflowProcessInstanceFinished();
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, pid);
                parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_FINISHED, finished);
                TaskStatePanel.this.setResponsePage(new PageProcessInstance(parameters, (PageBase) TaskStatePanel.this.getPage()));
            }
        });

        add(new WfDeltasPanel(ID_DELTAS_PANEL, getModel()));
        add(new WfHistoryPanel(ID_HISTORY_PANEL, new PropertyModel<List<WfHistoryEventDto>>(getModel(), TaskDto.F_WORKFLOW_HISTORY)));
    }
}
