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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
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
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;

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

	private static final long REFRESH_INTERVAL = 2000L;

	private static final String DOT_CLASS = PageTask2.class.getName() + ".";
	private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadTask";
	static final String OPERATION_SAVE_TASK = DOT_CLASS + "saveTask";

	public static final String ID_SUMMARY_PANEL = "summaryPanel";
	public static final String ID_MAIN_PANEL = "mainPanel";

	private static final Trace LOGGER = TraceManager.getTrace(PageTask2.class);

	private LoadableModel<TaskDto> taskDtoModel;
	private LoadableModel<ObjectWrapper<TaskType>> objectWrapperModel;
	private boolean edit = false;

	private PageTaskController controller = new PageTaskController(this);

	private TaskMainPanel mainPanel;
	private AjaxSelfUpdatingTimerBehavior refreshingBehavior;

	public PageTask2(PageParameters parameters) {

		getPageParameters().overwriteWith(parameters);

		final OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
		final Task operationTask = getTaskManager().createTaskInstance(OPERATION_LOAD_TASK);
		final String taskOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER).toString();
		final TaskType taskType = loadTaskType(taskOid, operationTask, result);
		final TaskDto taskDto;
		try {
			taskDto = prepareTaskDto(taskType, operationTask, result);
		} catch (SchemaException|ObjectNotFoundException e) {
			throw new SystemException("Couldn't prepare task DTO: " + e.getMessage(), e);
		}
		taskDtoModel = new LoadableModel<TaskDto>() {
			@Override
			protected TaskDto load() {
				return taskDto;
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

	private TaskType loadTaskType(String taskOid, Task operationTask, OperationResult result) {
		TaskType taskType = null;

		try {
			Collection<SelectorOptions<GetOperationOptions>> options =
					GetOperationOptions.createRetrieveAttributesOptions(
							TaskType.F_SUBTASK, TaskType.F_NODE_AS_OBSERVED, TaskType.F_NEXT_RUN_START_TIMESTAMP);
			options.add(SelectorOptions.create(new ItemPath(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_WORK_ITEM), GetOperationOptions.createRetrieve()));
			taskType = getModelService().getObject(TaskType.class, taskOid, options, operationTask, result).asObjectable();
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

	private TaskDto prepareTaskDto(TaskType task, Task operationTask, OperationResult result) throws SchemaException, ObjectNotFoundException {
		TaskDto taskDto = new TaskDto(task, getModelService(), getTaskService(), getModelInteractionService(),
				getTaskManager(), TaskDtoProviderOptions.fullOptions(), operationTask, result, this);
		return taskDto;
	}


	protected void initLayout() {
		final FocusSummaryPanel<TaskType> summaryPanel = new TaskSummaryPanel(ID_SUMMARY_PANEL, objectWrapperModel);
		summaryPanel.setOutputMarkupId(true);
		add(summaryPanel);

		mainPanel = new TaskMainPanel(ID_MAIN_PANEL, objectWrapperModel, taskDtoModel, this);
		mainPanel.setOutputMarkupId(true);
		add(mainPanel);

		refreshingBehavior = new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(REFRESH_INTERVAL)) {
			@Override
			protected void onPostProcessTarget(AjaxRequestTarget target) {
				refreshModel();
				target.add(summaryPanel);
			}
		};
		mainPanel.add(refreshingBehavior);
	}

	public void refreshModel() {
		TaskDto oldTaskDto = taskDtoModel.getObject();
		if (oldTaskDto == null) {
			LOGGER.warn("Null or empty taskModel");
			return;
		}
		TaskManager taskManager = getTaskManager();
		OperationResult result = new OperationResult("refresh");
		Task operationTask = taskManager.createTaskInstance("refresh");

		try {
			LOGGER.debug("Refreshing task {}", oldTaskDto);
			TaskType taskType = loadTaskType(oldTaskDto.getOid(), operationTask, result);
			TaskDto newTaskDto = prepareTaskDto(taskType, operationTask, result);
			final ObjectWrapper<TaskType> newWrapper = loadObjectWrapper(taskType.asPrismObject(), result);
			taskDtoModel.setObject(newTaskDto);
			objectWrapperModel.setObject(newWrapper);
		} catch (ObjectNotFoundException|SchemaException|RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't refresh task {}", e, oldTaskDto);
		}
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

	public boolean isEdit() {
		return edit;
	}

	public void setEdit(boolean edit) {
		this.edit = edit;
	}

	public LoadableModel<TaskDto> getTaskDtoModel() {
		return taskDtoModel;
	}

	public TaskDto getTaskDto() {
		return taskDtoModel.getObject();
	}

	public PageTaskController getController() {
		return controller;
	}

	boolean isRunnableOrRunning() {
		TaskDtoExecutionStatus exec = getTaskDto().getExecution();
		//System.out.println(this + ": state = " + exec);
		return exec == TaskDtoExecutionStatus.RUNNABLE || exec == TaskDtoExecutionStatus.RUNNING;
	}

	boolean isRunnable() {
		TaskDtoExecutionStatus exec = getTaskDto().getExecution();
		return exec == TaskDtoExecutionStatus.RUNNABLE;
	}

	boolean isRunning() {
		TaskDtoExecutionStatus exec = getTaskDto().getExecution();
		return exec == TaskDtoExecutionStatus.RUNNING;
	}

	boolean isClosed() {
		TaskDtoExecutionStatus exec = getTaskDto().getExecution();
		return exec == TaskDtoExecutionStatus.CLOSED;
	}

	boolean isSuspended() {
		TaskDtoExecutionStatus exec = getTaskDto().getExecution();
		return exec == TaskDtoExecutionStatus.SUSPENDED;
	}

	boolean isRecurring() {
		return getTaskDto().getRecurring();
	}

	public void startRefreshing() {
		refreshingBehavior.restart(null);
	}

	public void stopRefreshing() {
		refreshingBehavior.stop(null);
	}

	public void refreshRefreshing() {		// necessary for some strange reason
		mainPanel.remove(refreshingBehavior);
		mainPanel.add(refreshingBehavior);
	}
}
