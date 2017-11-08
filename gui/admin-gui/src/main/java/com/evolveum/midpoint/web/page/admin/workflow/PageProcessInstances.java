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

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDtoProvider;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mederly
 */
public abstract class PageProcessInstances extends PageAdminWorkItems {

	public static final String ID_BACK = "back";
    public static final String ID_STOP = "stop";
	public static final String ID_DELETE = "delete";
    public static final String ID_PROCESS_INSTANCES_TABLE = "processInstancesTable";
	public static final String ID_MAIN_FORM = "mainForm";

	boolean requestedBy;        // true if we want to show process instances requested BY a user
    boolean requestedFor;       // true if we want to show instances requested FOR a user

    private static final Trace LOGGER = TraceManager.getTrace(PageProcessInstances.class);
    private static final String DOT_CLASS = PageProcessInstances.class.getName() + ".";
    private static final String OPERATION_STOP_PROCESS_INSTANCES = DOT_CLASS + "stopProcessInstances";
    public static final String OPERATION_STOP_PROCESS_INSTANCE = DOT_CLASS + "stopProcessInstance";
    private static final String OPERATION_DELETE_PROCESS_INSTANCES = DOT_CLASS + "deleteProcessInstances";
    private static final String OPERATION_DELETE_PROCESS_INSTANCE = DOT_CLASS + "deleteProcessInstance";

    public PageProcessInstances(boolean requestedBy, boolean requestedFor) {
        this.requestedBy = requestedBy;
        this.requestedFor = requestedFor;
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

		ISortableDataProvider<ProcessInstanceDto, String> provider = new ProcessInstanceDtoProvider(PageProcessInstances.this, requestedBy, requestedFor);
		ProcessInstancesPanel panel = new ProcessInstancesPanel(ID_PROCESS_INSTANCES_TABLE, provider,
				UserProfileStorage.TableId.PAGE_WORKFLOW_REQUESTS, (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_WORKFLOW_REQUESTS),
				ProcessInstancesPanel.View.FULL_LIST, null);
		panel.setOutputMarkupId(true);
		mainForm.add(panel);

        initItemButtons(mainForm);
    }

    private void initItemButtons(Form mainForm) {

		AjaxButton back = new AjaxButton(ID_BACK, createStringResource("pageProcessInstances.button.back")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				redirectBack();
			}
		};
		mainForm.add(back);

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

		AjaxButton delete = new AjaxButton(ID_DELETE, createStringResource("pageProcessInstances.button.delete")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteProcessInstancesPerformed(target);
			}
			@Override
			public boolean isVisible() {
				return !requestedFor;
			}
		};
		mainForm.add(delete);

    }

    private BoxedTablePanel<?> getTable() {
        return ((ProcessInstancesPanel) get(createComponentPath(ID_MAIN_FORM, ID_PROCESS_INSTANCES_TABLE))).getTablePanel();
    }

	private boolean isSomeItemSelected(List<ProcessInstanceDto> instances, boolean stoppable, AjaxRequestTarget target) {
		if (!instances.isEmpty()) {
			return true;
		}

		if (stoppable) {
			warn(getString("pageProcessInstances.message.noStoppableItemSelected"));
		} else {
			warn(getString("pageProcessInstances.message.noItemSelected"));
		}
		target.add(getFeedbackPanel());
		return false;
	}

    private void itemDetailsPerformed(AjaxRequestTarget target, String taskOid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
        navigateToNext(PageTaskEdit.class, parameters);
    }

    private void stopProcessInstancesPerformed(AjaxRequestTarget target) {

    	MidPointPrincipal user = SecurityUtils.getPrincipalUser();

        List<ProcessInstanceDto> selectedStoppableInstances = new ArrayList<>();
		for (Selectable row : WebComponentUtil.getSelectedData(getTable())) {
			ProcessInstanceDto instance = (ProcessInstanceDto) row;
			if (instance.getEndTimestamp() == null) {
				selectedStoppableInstances.add(instance);
			}
		}

		if (!isSomeItemSelected(selectedStoppableInstances, true, target)) {
			return;
		}

		Task task = createSimpleTask(OPERATION_STOP_PROCESS_INSTANCES);
        OperationResult result = task.getResult();

        WorkflowService workflowService = getWorkflowService();
        for (ProcessInstanceDto instance : selectedStoppableInstances) {
            try {
                workflowService.stopProcessInstance(instance.getProcessInstanceId(),
                        WebComponentUtil.getOrigStringFromPoly(user.getName()), task, result);
            } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException ex) {
                result.createSubresult(OPERATION_STOP_PROCESS_INSTANCE).recordPartialError("Couldn't stop process instance " + instance.getName(), ex);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "Selected process instance(s) have been successfully stopped.");
        }

        showResult(result);

        ProcessInstanceDtoProvider provider = (ProcessInstanceDtoProvider) getTable().getDataTable().getDataProvider();
        provider.clearCache();

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTable());
    }

	private void deleteProcessInstancesPerformed(AjaxRequestTarget target) {

		List<ProcessInstanceDto> processInstanceDtoList = WebComponentUtil.getSelectedData(getTable());

		if (!isSomeItemSelected(processInstanceDtoList, false, target)) {
			return;
		}

		Task opTask = createSimpleTask(OPERATION_DELETE_PROCESS_INSTANCES);
		OperationResult result = opTask.getResult();

		ModelService modelService = getModelService();
		for (ProcessInstanceDto processInstanceDto : processInstanceDtoList) {
			String taskOid = processInstanceDto.getTaskOid();
			try {
				ObjectDelta<? extends ObjectType> deleteDelta = ObjectDelta.createDeleteDelta(TaskType.class, taskOid, getPrismContext());
				modelService.executeChanges(Collections.<ObjectDelta<? extends ObjectType>>singletonList(deleteDelta), null, opTask, result);
			} catch (CommonException|RuntimeException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete task (process instance) {}", e, taskOid);
			}
		}

		if (result.isUnknown()) {
			result.recomputeStatus();
		}

		if (result.isSuccess()) {
			result.recordStatus(OperationResultStatus.SUCCESS, "Selected process instance(s) have been successfully deleted.");
		}

		showResult(result);

		ProcessInstanceDtoProvider provider = (ProcessInstanceDtoProvider) getTable().getDataTable().getDataProvider();
		provider.clearCache();

		//refresh feedback and table
		target.add(getFeedbackPanel());
		target.add(getTable());
	}

}
