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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
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
public class PageProcessInstance extends PageAdminWorkItems {
	private static final long serialVersionUID = -5933030498922903813L;

	private static final Trace LOGGER = TraceManager.getTrace(PageProcessInstance.class);
	private static final String DOT_CLASS = PageTaskAdd.class.getName() + ".";
    public static final String PARAM_PROCESS_INSTANCE_ID = "processInstanceId";
    public static final String PARAM_PROCESS_INSTANCE_FINISHED = "processInstanceFinished";     // boolean value
    private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadProcessInstance";

    private static final String ID_PROCESS_INSTANCE_PANEL = "processInstancePanel";

    private IModel<ProcessInstanceDto> model;

    private PageParameters parameters;

    public PageProcessInstance() {
        this(new PageParameters(), null);
    }

    public PageProcessInstance(final PageParameters parameters, PageBase previousPage) {

        this.parameters = parameters;
        setPreviousPage(previousPage);

        model = new LoadableModel<ProcessInstanceDto>(false) {

			@Override
			protected ProcessInstanceDto load() {
				return loadProcessInstance();
			}
		};

        String detailsPageClassName = getWorkflowManager().getProcessInstanceDetailsPanelName(model.getObject().getProcessInstance());
        initLayout(detailsPageClassName);
	}

    private ProcessInstanceDto loadProcessInstance() {
		OperationResult result = new OperationResult(OPERATION_LOAD_TASK);

		try {
            StringValue pid = parameters.get(PARAM_PROCESS_INSTANCE_ID);
            boolean finished = parameters.get(PARAM_PROCESS_INSTANCE_FINISHED).toBoolean();
            WfProcessInstanceType processInstance;
            try {
                processInstance = getWorkflowManager().getProcessInstanceById(pid.toString(), finished, true, result);
            } catch (ObjectNotFoundException e) {
                if (finished == false) {
                    // maybe the process instance has finished in the meanwhile...
                    processInstance = getWorkflowManager().getProcessInstanceById(pid.toString(), true, true, result);
                } else {
                    throw e;
                }
            }
            return new ProcessInstanceDto(processInstance);
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get process instance information.", ex);
            showResult(result);
            getSession().error(getString("pageProcessInstance.message.cantGetDetails"));

            if (!result.isSuccess()) {
                showResultInSession(result);
            }
            throw getRestartResponseException(PageProcessInstancesAll.class);
        }
	}

    private void initLayout(String detailsPanelClassName) {
		Form mainForm = new Form("mainForm");
		add(mainForm);

        ProcessInstancePanel processInstancePanel = new ProcessInstancePanel(ID_PROCESS_INSTANCE_PANEL, model, detailsPanelClassName);
        mainForm.add(processInstancePanel);

		initButtons(mainForm);
	}


	private void initButtons(final Form mainForm) {
		AjaxLinkButton backButton = new AjaxLinkButton("backButton",
				createStringResource("pageProcessInstance.button.back")) {

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
