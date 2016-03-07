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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceNewDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

/**
 * @author lazyman
 * @author mserbak
 * @author mederly
 */
@PageDescriptor(url = "/admin/workItems/processInstance", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL,
                label = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL_LABEL,
                description = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEMS_PROCESS_INSTANCE_URL,
                label = "PageProcessInstance.auth.workItemsProcessInstance.label",
                description = "PageProcessInstance.auth.workItemsProcessInstance.description")})
public class PageProcessInstance extends PageAdminWorkItems {

    private static final Trace LOGGER = TraceManager.getTrace(PageProcessInstance.class);
    private static final String DOT_CLASS = PageTaskAdd.class.getName() + ".";
    private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadProcessInstance";

    private static final String ID_PROCESS_INSTANCE_PANEL = "processInstancePanel";

    private LoadableModel<ProcessInstanceNewDto> model;

    private PageParameters parameters;

    public PageProcessInstance() {
        this(new PageParameters(), null);
    }

    public PageProcessInstance(final PageParameters parameters, PageBase previousPage) {

        this.parameters = parameters;
        setPreviousPage(previousPage);

        model = new LoadableModel<ProcessInstanceNewDto>(false) {

            @Override
            protected ProcessInstanceNewDto load() {
                return loadProcessInstance();
            }
        };

        initLayout();
    }

    private ProcessInstanceNewDto loadProcessInstance() {
        Task opTask = getTaskManager().createTaskInstance(OPERATION_LOAD_TASK);
        OperationResult result = opTask.getResult();
        try {
            StringValue taskOid = parameters.get(OnePageParameterEncoder.PARAMETER);
            TaskType task = getModelService().getObject(TaskType.class, taskOid.toString(), null, opTask, result).asObjectable();
            return new ProcessInstanceNewDto(task);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get process instance information.", ex);
            showResult(result);
            getSession().error(getString("pageProcessInstance.message.cantGetDetails"));
            showResult(result, false);
            throw getRestartResponseException(PageProcessInstancesAll.class);
        }
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        ProcessInstancePanel processInstancePanel = new ProcessInstancePanel(ID_PROCESS_INSTANCE_PANEL, model);
        mainForm.add(processInstancePanel);

        initButtons(mainForm);
    }


    private void initButtons(final Form mainForm) {
        AjaxButton backButton = new AjaxButton("backButton", createStringResource("pageProcessInstance.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                goBack(PageProcessInstancesAll.class);
            }
        };
        mainForm.add(backButton);
    }

    @Override
    public PageBase reinitialize() {
        return new PageProcessInstance(parameters, getPreviousPage());
    }
}
