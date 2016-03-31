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

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDtoProvider;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * @author mederly
 */
public abstract class PageProcessInstances extends PageAdminWorkItems {

    public static final String ID_STOP = "stop";
    public static final String ID_BACK = "back";
    public static final String ID_PROCESS_INSTANCES_TABLE = "processInstancesTable";

    boolean requestedBy;        // true if we want to show process instances requested BY a user
    boolean requestedFor;       // true if we want to show instances requested FOR a user

    private static final Trace LOGGER = TraceManager.getTrace(PageProcessInstances.class);
    private static final String DOT_CLASS = PageProcessInstances.class.getName() + ".";
    private static final String OPERATION_STOP_PROCESS_INSTANCES = DOT_CLASS + "stopProcessInstances";

    public PageProcessInstances(boolean requestedBy, boolean requestedFor) {
        this.requestedBy = requestedBy;
        this.requestedFor = requestedFor;
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

		ISortableDataProvider<ProcessInstanceDto, String> provider = new ProcessInstanceDtoProvider(PageProcessInstances.this, requestedBy, requestedFor);
		WorkflowRequestsPanel panel = new WorkflowRequestsPanel(ID_PROCESS_INSTANCES_TABLE, provider,
				UserProfileStorage.TableId.PAGE_WORKFLOW_REQUESTS, (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_WORKFLOW_REQUESTS));
		panel.setOutputMarkupId(true);
		mainForm.add(panel);

        initItemButtons(mainForm);
    }

    private void initItemButtons(Form mainForm) {

        AjaxButton stop = new AjaxButton(ID_STOP, createStringResource("pageProcessInstances.button.stop")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                stopProcessInstancesPerformed(target);
            }

            @Override
            public boolean isVisible() {
                return !requestedFor;
            }
        };
        mainForm.add(stop);

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("pageProcessInstances.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                goBack(PageDashboard.class);
            }
        };
        mainForm.add(back);

    }

    private TablePanel getTable() {
        return (TablePanel) get("mainForm:processInstancesTable");
    }

    private boolean isSomeItemSelected(List<ProcessInstanceDto> instances, AjaxRequestTarget target) {
        if (!instances.isEmpty()) {
            return true;
        }

        warn(getString("pageProcessInstances.message.noItemSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    private void itemDetailsPerformed(AjaxRequestTarget target, String taskOid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
        setResponsePage(new PageTaskEdit(parameters));
    }

    private void stopProcessInstancesPerformed(AjaxRequestTarget target) {

    	MidPointPrincipal user = SecurityUtils.getPrincipalUser();

        List<ProcessInstanceDto> processInstanceDtoList = WebComponentUtil.getSelectedData(getTable());

        if (!isSomeItemSelected(processInstanceDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_STOP_PROCESS_INSTANCES);

        WorkflowService workflowService = getWorkflowService();
        for (ProcessInstanceDto processInstanceDto : processInstanceDtoList) {
            try {
//                workflowService.stopProcessInstance(processInstanceDto.getInstanceId(),
//                        WebComponentUtil.getOrigStringFromPoly(user.getName()), result);
            } catch (Exception ex) {    // todo
                result.createSubresult("stopProcessInstance").recordPartialError("Couldn't stop process instance " + processInstanceDto.getName(), ex);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "Selected process instance(s) have been successfully stopped and/or deleted.");
        }

        showResult(result);

        ProcessInstanceDtoProvider provider = (ProcessInstanceDtoProvider) getTable().getDataTable().getDataProvider();
        provider.clearCache();

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTable());

        setReinitializePreviousPages(true);
    }

}
