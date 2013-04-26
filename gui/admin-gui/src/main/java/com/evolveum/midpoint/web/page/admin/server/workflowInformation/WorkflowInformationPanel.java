/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server.workflowInformation;

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wf.deltas.WfDeltasPanel;
import com.evolveum.midpoint.web.component.wf.history.WfHistoryEventDto;
import com.evolveum.midpoint.web.component.wf.history.WfHistoryPanel;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstance;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
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
                parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_ID, pid);
                parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_FINISHED, finished);
                parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_BACK, PageProcessInstance.PARAM_PROCESS_INSTANCE_BACK_ALL);
                WorkflowInformationPanel.this.setResponsePage(new PageProcessInstance(parameters));
            }
        });

        add(new WfDeltasPanel(ID_DELTAS_PANEL, getModel()));
        add(new WfHistoryPanel(ID_HISTORY_PANEL, new PropertyModel<List<WfHistoryEventDto>>(getModel(), TaskDto.F_WORKFLOW_HISTORY)));
    }
}
