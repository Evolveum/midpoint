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

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshDto;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshPanel;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.*;

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

public class PageTaskEdit extends PageAdmin implements Refreshable {

	private static final int REFRESH_INTERVAL_IF_RUNNABLE = 2000;
	private static final int REFRESH_INTERVAL_IF_SUSPENDED = 60000;
	private static final int REFRESH_INTERVAL_IF_WAITING = 60000;
	private static final int REFRESH_INTERVAL_IF_CLOSED = 60000;

	public static final String DOT_CLASS = PageTaskEdit.class.getName() + ".";
	private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadTask";
	private static final String OPERATION_LOAD_NODES = DOT_CLASS + "loadNodes";
	static final String OPERATION_SAVE_TASK = DOT_CLASS + "saveTask";
	static final String OPERATION_DELETE_SYNC_TOKEN = DOT_CLASS + "deleteSyncToken";

	public static final String ID_SUMMARY_PANEL = "summaryPanel";
	public static final String ID_MAIN_PANEL = "mainPanel";

	private static final Trace LOGGER = TraceManager.getTrace(PageTaskEdit.class);

	private String taskOid;
	private IModel<TaskDto> taskDtoModel;
	private LoadableModel<ObjectWrapper<TaskType>> objectWrapperModel;
	private boolean edit = false;

	private TaskDto currentTaskDto, previousTaskDto;

	private PageTaskController controller = new PageTaskController(this);

	private TaskMainPanel mainPanel;
	private IModel<AutoRefreshDto> refreshModel;
	private IModel<Boolean> showAdvancedFeaturesModel;
	private IModel<List<NodeType>> nodeListModel;

	public PageTaskEdit(PageParameters parameters) {
		taskOid = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
		taskDtoModel = new LoadableModel<TaskDto>(false) {
			@Override
			protected TaskDto load() {
				try {
					previousTaskDto = currentTaskDto;
					final OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
					final Task operationTask = getTaskManager().createTaskInstance(OPERATION_LOAD_TASK);
					final TaskType taskType = loadTaskTypeChecked(taskOid, operationTask, result);
					currentTaskDto = prepareTaskDto(taskType, operationTask, result);
					return currentTaskDto;
				} catch (SchemaException|ObjectNotFoundException|ExpressionEvaluationException e) {
					throw new SystemException("Couldn't prepare task DTO: " + e.getMessage(), e);
				}
			}
		};
		objectWrapperModel = new LoadableModel<ObjectWrapper<TaskType>>() {
			@Override
			protected ObjectWrapper<TaskType> load() {
				final Task operationTask = getTaskManager().createTaskInstance(OPERATION_LOAD_TASK);
				return loadObjectWrapper(taskDtoModel.getObject().getTaskType().asPrismObject(), operationTask, new OperationResult("loadObjectWrapper"));
			}
		};
		showAdvancedFeaturesModel = new Model<>(false);		// todo save setting in session
		nodeListModel = new LoadableModel<List<NodeType>>(false) {
			@Override
			protected List<NodeType> load() {
				OperationResult result = new OperationResult(OPERATION_LOAD_NODES);
				Task opTask = getTaskManager().createTaskInstance(OPERATION_LOAD_NODES);
				try {
					return PrismObject.asObjectableList(
							getModelService().searchObjects(NodeType.class, null, null, opTask, result));
				} catch (Throwable t) {
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve nodes", t);
					return Collections.emptyList();
				}
			}
		};
		edit = false;
		initLayout();
	}

	@Override
	protected IModel<String> createPageTitleModel() {
		TaskDto taskDto = taskDtoModel != null ? taskDtoModel.getObject() : null;
		String suffix;
		if (taskDto != null && taskDto.isWorkflowParent()) {
			suffix = ".wfOperation";
		} else if (taskDto != null && taskDto.isWorkflowChild()) {
			suffix = ".wfRequest";
		} else {
			suffix = "";
		}
		return createStringResource("PageTaskEdit.title" + suffix);
	}

	private TaskType loadTaskTypeChecked(String taskOid, Task operationTask, OperationResult result) {
		TaskType taskType = loadTaskType(taskOid, operationTask, result);

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

	public IModel<List<NodeType>> getNodeListModel() {
		return nodeListModel;
	}

	private TaskType loadTaskType(String taskOid, Task operationTask, OperationResult result) {
		TaskType taskType = null;

		try {
			Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.retrieveItemsNamed(
					TaskType.F_SUBTASK,
					TaskType.F_NODE_AS_OBSERVED,
					TaskType.F_NEXT_RUN_START_TIMESTAMP,
					TaskType.F_NEXT_RETRY_TIMESTAMP,
					new ItemPath(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_WORK_ITEM));
			options.addAll(GetOperationOptions.resolveItemsNamed(
					new ItemPath(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_REQUESTER_REF)
			));
			taskType = getModelService().getObject(TaskType.class, taskOid, options, operationTask, result).asObjectable();
			result.computeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get task.", ex);
		}
		return taskType;
	}

	private TaskDto prepareTaskDto(TaskType task, Task operationTask, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		TaskDto taskDto = new TaskDto(task, null, getModelService(), getTaskService(), getModelInteractionService(),
				getTaskManager(), getWorkflowManager(), TaskDtoProviderOptions.fullOptions(), operationTask, result, this);
		return taskDto;
	}


	protected void initLayout() {
		refreshModel = new Model(new AutoRefreshDto());
		refreshModel.getObject().setInterval(getRefreshInterval());

		IModel<PrismObject<TaskType>> prismObjectModel = new AbstractReadOnlyModel<PrismObject<TaskType>>() {
			@Override
			public PrismObject<TaskType> getObject() {
				return objectWrapperModel.getObject().getObject();
			}
		};
		final TaskSummaryPanel summaryPanel = new TaskSummaryPanel(ID_SUMMARY_PANEL, prismObjectModel, refreshModel, this);
		summaryPanel.setOutputMarkupId(true);
		add(summaryPanel);

		mainPanel = new TaskMainPanel(ID_MAIN_PANEL, objectWrapperModel, taskDtoModel, showAdvancedFeaturesModel, this);
		mainPanel.setOutputMarkupId(true);
		add(mainPanel);

		summaryPanel.getRefreshPanel().startRefreshing(this, null);
	}

	@Override
	public int getRefreshInterval() {
		TaskDtoExecutionStatus exec = getTaskDto().getExecution();
		if (exec == null) {
			return REFRESH_INTERVAL_IF_CLOSED;
		}
		switch (exec) {
			case RUNNABLE:
			case RUNNING:
			case RUNNING_OR_RUNNABLE:
			case SUSPENDING: return REFRESH_INTERVAL_IF_RUNNABLE;
			case SUSPENDED: return REFRESH_INTERVAL_IF_SUSPENDED;
			case WAITING: return REFRESH_INTERVAL_IF_WAITING;
			case CLOSED: return REFRESH_INTERVAL_IF_CLOSED;
		}
		return REFRESH_INTERVAL_IF_RUNNABLE;
	}

	public void refresh(AjaxRequestTarget target) {
		TaskTabsVisibility tabsVisibilityOld = new TaskTabsVisibility();
		tabsVisibilityOld.computeAll(this);
		TaskButtonsVisibility buttonsVisibilityOld = new TaskButtonsVisibility();
		buttonsVisibilityOld.computeAll(this);

		refreshTaskModels();

		TaskTabsVisibility tabsVisibilityNew = new TaskTabsVisibility();
		tabsVisibilityNew.computeAll(this);
		TaskButtonsVisibility buttonsVisibilityNew = new TaskButtonsVisibility();
		buttonsVisibilityNew.computeAll(this);

		if (!buttonsVisibilityNew.equals(buttonsVisibilityOld)) {
			target.add(mainPanel.getButtonPanel());
		}
		if (tabsVisibilityNew.equals(tabsVisibilityOld)) {
			// soft version
			for (Component component : mainPanel.getTabPanel()) {
				if (component instanceof TaskTabPanel) {
					for (Component c : ((TaskTabPanel) component).getComponentsToUpdate()) {
						target.add(c);
					}
				}
			}
		} else {
			// hard version
			target.add(mainPanel.getTabPanel());
		}
		target.add(getSummaryPanel());

		AutoRefreshDto refreshDto = refreshModel.getObject();
		refreshDto.recordRefreshed();

		if (isEdit() || !refreshDto.isEnabled()) {
			getRefreshPanel().stopRefreshing(this, target);
		} else {
			getRefreshPanel().startRefreshing(this, target);
		}
	}

	public TaskDto getCurrentTaskDto() {
		return currentTaskDto;
	}

	public TaskDto getPreviousTaskDto() {
		return previousTaskDto;
	}

	@Override
	public Component getRefreshingBehaviorParent() {
		return getRefreshPanel();
	}

	public void refreshTaskModels() {
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
			final ObjectWrapper<TaskType> newWrapper = loadObjectWrapper(taskType.asPrismObject(), operationTask, result);
			previousTaskDto = currentTaskDto;
			currentTaskDto = newTaskDto;
			taskDtoModel.setObject(newTaskDto);
			objectWrapperModel.setObject(newWrapper);
		} catch (ObjectNotFoundException|SchemaException|ExpressionEvaluationException|RuntimeException|Error e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't refresh task {}", e, oldTaskDto);
		}
	}

	protected ObjectWrapper<TaskType> loadObjectWrapper(PrismObject<TaskType> object, Task task, OperationResult result) {
		ObjectWrapper<TaskType> wrapper;
		ObjectWrapperFactory owf = new ObjectWrapperFactory(this);
		try {
			object.revive(getPrismContext());		// just to be sure (after deserialization the context is missing in this object)
			wrapper = owf.createObjectWrapper("pageAdminFocus.focusDetails", null, object, ContainerStatus.MODIFYING, task);
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get user.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load user", ex);
			wrapper = owf.createObjectWrapper("pageAdminFocus.focusDetails", null, object, null, null, ContainerStatus.MODIFYING);
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

	public IModel<TaskDto> getTaskDtoModel() {
		return taskDtoModel;
	}

	public LoadableModel<ObjectWrapper<TaskType>> getObjectWrapperModel() {
		return objectWrapperModel;
	}

	public TaskDto getTaskDto() {
		return taskDtoModel.getObject();
	}

	public PageTaskController getController() {
		return controller;
	}

	public TaskSummaryPanel getSummaryPanel() {
		return (TaskSummaryPanel) get(ID_SUMMARY_PANEL);
	}

	public AutoRefreshPanel getRefreshPanel() {
		return getSummaryPanel().getRefreshPanel();
	}

	public boolean isShowAdvanced() {
		return showAdvancedFeaturesModel.getObject();
	}

	public VisibleEnableBehaviour createVisibleIfEdit(final ItemPath itemPath) {
		return new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return isEdit() && isEditable(itemPath);
			}
		};
	}

	public VisibleEnableBehaviour createEnabledIfEdit(final ItemPath itemPath) {
		return new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return isEdit() && isEditable(itemPath);
			}
		};
	}
	public VisibleEnableBehaviour createVisibleIfView(final ItemPath itemPath) {
		return new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return isReadable(itemPath) && (!isEdit() || !isEditable(itemPath));
			}
		};
	}

	public VisibleEnableBehaviour createVisibleIfAccessible(QName... names) {
		return createVisibleIfAccessible(ItemPath.asPathArray(names));
	}

	public VisibleEnableBehaviour createVisibleIfAccessible(final ItemPath... itemPaths) {
		return new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				for (ItemPath itemPath : itemPaths) {
					if (!isReadable(itemPath)) {
						return false;
					}
				}
				return true;
			}
		};
	}

	protected boolean isEditable() {
		return isEditable(objectWrapperModel.getObject().getDefinition());
	}

	private boolean isEditable(ItemDefinition<?> definition) {
		return isEditable(definition, new HashSet<>());
	}

	private boolean isEditable(ItemDefinition<?> definition, Set<ItemDefinition<?>> seen) {
		if (definition.canModify()) {
			return true;
		} else if (definition instanceof PrismContainerDefinition) {
			for (ItemDefinition<?> subdef : ((PrismContainerDefinition<?>) definition).getDefinitions()) {
				if (seen.contains(subdef)) {
					return false;
				}
				if (isEditable(subdef, seen)) {
					return true;
				}
				seen.add(subdef);
			}
		}
		return false;
	}

	protected boolean isEditable(QName name) {
		return isEditable(new ItemPath(name));
	}

	protected boolean isEditable(ItemPath itemPath) {
		ItemDefinition<?> itemDefinition = objectWrapperModel.getObject().getDefinition().findItemDefinition(itemPath);
		if (itemDefinition != null) {
			return itemDefinition.canRead() && itemDefinition.canModify();
		} else {
			return true;
		}
	}

	protected boolean isReadable(ItemPath itemPath) {
		ItemDefinition<?> itemDefinition = objectWrapperModel.getObject().getDefinition().findItemDefinition(itemPath);
		if (itemDefinition != null) {
			return itemDefinition.canRead();
		} else {
			return true;
		}
	}

	public boolean isExtensionReadable(QName name) {
		return isReadable(new ItemPath(TaskType.F_EXTENSION, name));
	}

	boolean isReadableSomeOf(QName... names) {
		for (QName name : names) {
			if (isReadable(new ItemPath(name))) {
				return true;
			}
		}
		return false;
	}

	public Form getForm() {
		return mainPanel.getMainForm();
	}

	boolean canSuspend() {
		return isAuthorized(ModelAuthorizationAction.SUSPEND_TASK);
	}

	boolean canResume() {
		return isAuthorized(ModelAuthorizationAction.RESUME_TASK);
	}

	boolean canRunNow() {
		return isAuthorized(ModelAuthorizationAction.RUN_TASK_IMMEDIATELY);
	}

	boolean canStop() {
		return isAuthorized(ModelAuthorizationAction.STOP_APPROVAL_PROCESS_INSTANCE);
	}

	private boolean isAuthorized(ModelAuthorizationAction action) {
		try {
			return isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null)
					|| isAuthorized(action.getUrl(), null, taskDtoModel.getObject().getTaskType().asPrismObject(), null, null, null);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine authorization for {}", e, action);
			return true;			// it is only GUI thing
		}
	}
}
