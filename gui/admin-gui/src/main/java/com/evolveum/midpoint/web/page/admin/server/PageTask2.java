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

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import java.util.Collection;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/task2", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
				label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
				description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASK_URL,
				label = "PageTaskEdit.auth.task.label",
				description = "PageTaskEdit.auth.task.description")})

public class PageTask2 extends PageAdmin {

	private static final String DOT_CLASS = PageTask2.class.getName() + ".";
	private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadTask";

	private static final String ID_SUMMARY_PANEL = "summaryPanel";
	private static final String ID_MAIN_PANEL = "mainPanel";

	private static final Trace LOGGER = TraceManager.getTrace(PageTask2.class);

	private LoadableModel<TaskDto> taskDtoModel;
	private LoadableModel<ObjectWrapper<TaskType>> objectWrapperModel;
	private static boolean edit = false;

	public PageTask2(PageParameters parameters) {

		getPageParameters().overwriteWith(parameters);

		final OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
		final Task operationTask = getTaskManager().createTaskInstance(OPERATION_LOAD_TASK);
		final TaskType taskType = loadTaskType(operationTask, result);
		TaskDto taskDto = null;
		try {
			taskDto = prepareTaskDto(taskType, operationTask, result);
		} catch (SchemaException e) {
			e.printStackTrace();		// TODO
		} catch (ObjectNotFoundException e) {
			e.printStackTrace();		// TODO
		}
		final TaskDto taskDtoFinal = taskDto;
		taskDtoModel = new LoadableModel<TaskDto>() {
			@Override
			protected TaskDto load() {
				return taskDtoFinal;
			}
		};
		final ObjectWrapper<TaskType> wrapper = loadObjectWrapper(taskType.asPrismObject(), result);
		objectWrapperModel = new LoadableModel<ObjectWrapper<TaskType>>() {
			@Override
			protected ObjectWrapper<TaskType> load() {
				return wrapper;
			}
		};

		edit = false;
		initLayout();
	}

	private TaskType loadTaskType(Task operationTask, OperationResult result) {

		final StringValue taskOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
		TaskType taskType = null;

		try {
			Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.createRetrieveAttributesOptions(TaskType.F_SUBTASK, TaskType.F_NODE_AS_OBSERVED, TaskType.F_NEXT_RUN_START_TIMESTAMP);
			taskType = getModelService().getObject(TaskType.class, taskOid.toString(), options, operationTask, result).asObjectable();
			result.computeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get task.", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}

		if (taskType == null) {
			getSession().error(getString("pageTaskEdit.message.cantTaskDetails"));
			showResult(result, false);
			throw getRestartResponseException(PageTasks.class);
		}
		return taskType;
	}

	private TaskDto loadTask() {
		OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
		Task operationTask = getTaskManager().createTaskInstance(OPERATION_LOAD_TASK);

		StringValue taskOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);

		TaskDto taskDto = null;
		try {
			Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.createRetrieveAttributesOptions(TaskType.F_SUBTASK, TaskType.F_NODE_AS_OBSERVED, TaskType.F_NEXT_RUN_START_TIMESTAMP);
			TaskType loadedTask = getModelService().getObject(TaskType.class, taskOid.toString(), options, operationTask, result).asObjectable();
			taskDto = prepareTaskDto(loadedTask, operationTask, result);
			result.computeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get task.", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}

		if (taskDto == null) {
			getSession().error(getString("pageTaskEdit.message.cantTaskDetails"));
			showResult(result, false);
			throw getRestartResponseException(PageTasks.class);
		}
		return taskDto;
	}

	private TaskDto prepareTaskDto(TaskType task, Task operationTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
		TaskDto taskDto = new TaskDto(task, getModelService(), getTaskService(), getModelInteractionService(),
				getTaskManager(), TaskDtoProviderOptions.fullOptions(), operationTask, result, this);
		return taskDto;
	}


	protected void initLayout() {
		initLayoutSummaryPanel();

		Panel mainPanel = new TaskMainPanel(ID_MAIN_PANEL, objectWrapperModel, taskDtoModel, this);
		mainPanel.setOutputMarkupId(true);
		add(mainPanel);
	}

	protected void initLayoutSummaryPanel() {
		FocusSummaryPanel<TaskType> summaryPanel = new TaskSummaryPanel(ID_SUMMARY_PANEL, objectWrapperModel);
		summaryPanel.setOutputMarkupId(true);
		add(summaryPanel);
	}

	protected ObjectWrapper<TaskType> loadObjectWrapper(PrismObject<TaskType> object, OperationResult result) {

		ObjectWrapper<TaskType> wrapper;
		ObjectWrapperFactory owf = new ObjectWrapperFactory(this);
		try {
			wrapper = owf.createObjectWrapper("pageAdminFocus.focusDetails", null, object, ContainerStatus.MODIFYING);
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get user.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
			wrapper = owf.createObjectWrapper("pageAdminFocus.focusDetails", null, object, null, null, ContainerStatus.MODIFYING, false);
		}
		showResult(wrapper.getResult(), false);

		return wrapper;
	}
}
