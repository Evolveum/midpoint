/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
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

    private static final int REFRESH_INTERVAL_IF_RUNNING = 2000;
    private static final int REFRESH_INTERVAL_IF_RUNNABLE = 60000;
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
    private LoadableModel<PrismObjectWrapper<TaskType>> objectWrapperModel;
    private boolean edit = false;

    private TaskDto currentTaskDto, previousTaskDto;

    private TaskMainPanel mainPanel;
    private IModel<AutoRefreshDto> refreshModel;
    private IModel<Boolean> showAdvancedFeaturesModel;
    private IModel<List<NodeType>> nodeListModel;

    public PageTaskEdit(PageParameters parameters) {
        taskOid = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        TaskDto taskDto = taskDtoModel != null ? taskDtoModel.getObject() : null;
        String suffix;
        if (taskDto != null && taskDto.isWorkflow()) {
            suffix = ".wfOperation";
        } else {
            suffix = "";
        }
        return createStringResource("PageTaskEdit.title" + suffix);
    }

    private TaskType loadTaskTypeChecked(String taskOid, Task operationTask, OperationResult result) {
        TaskType taskType = loadTaskType(taskOid, operationTask, result);
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
            Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                    // retrieve
                    .item(TaskType.F_SUBTASK).retrieve()
                    .item(TaskType.F_NODE_AS_OBSERVED).retrieve()
                    .item(TaskType.F_NEXT_RUN_START_TIMESTAMP).retrieve()
                    .item(TaskType.F_NEXT_RETRY_TIMESTAMP).retrieve()
                    .item(TaskType.F_RESULT).retrieve()         // todo maybe only when it is to be displayed
                    //.item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_WORK_ITEM).retrieve() // todo do this in case
                    // resolve
                    //.item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_REQUESTOR_REF).resolve()   // todo do this in case
                    .build();
            taskType = getModelService().getObject(TaskType.class, taskOid, options, operationTask, result).asObjectable();
        } catch (Exception ex) {
            result.recordFatalError(getString("PageTaskEdit.message.loadTaskType.fatalError"), ex);
        }
        return taskType;
    }

    private TaskDto prepareTaskDto(TaskType task, boolean subtasksLoaded, Task operationTask, OperationResult result) throws SchemaException {
        return new TaskDto(task, null, getModelService(), getTaskService(), getModelInteractionService(),
                getTaskManager(), getWorkflowManager(), TaskDtoProviderOptions.fullOptions(), subtasksLoaded, operationTask, result, this);
    }


    @Override
    protected void onInitialize() {
        super.onInitialize();

        taskDtoModel = new LoadableModel<TaskDto>(false) {
            @Override
            protected TaskDto load() {
                try {
                    previousTaskDto = currentTaskDto;
                    final OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
                    final Task operationTask = getTaskManager().createTaskInstance(OPERATION_LOAD_TASK);
                    final TaskType taskType = loadTaskTypeChecked(taskOid, operationTask, result);
                    currentTaskDto = prepareTaskDto(taskType, true, operationTask, result);
                    result.computeStatusIfUnknown();
                    if (!result.isSuccess()) {
                        showResult(result);
                    }
                    return currentTaskDto;
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't prepare task DTO: " + e.getMessage(), e);
                }
            }
        };
        objectWrapperModel = new LoadableModel<PrismObjectWrapper<TaskType>>() {
            @Override
            protected PrismObjectWrapper<TaskType> load() {
                final Task operationTask = getTaskManager().createTaskInstance(OPERATION_LOAD_TASK);
                return loadObjectWrapper(taskDtoModel.getObject().getTaskType().asPrismObject(), operationTask, new OperationResult("loadObjectWrapper"));
            }
        };
        showAdvancedFeaturesModel = new Model<>(false);        // todo save setting in session
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
        refreshModel = new Model<>(new AutoRefreshDto());
        refreshModel.getObject().setInterval(getRefreshInterval());

        mainPanel = new TaskMainPanel(ID_MAIN_PANEL, objectWrapperModel, taskDtoModel, showAdvancedFeaturesModel, this);
        mainPanel.setOutputMarkupId(true);
        add(mainPanel);

    }

    @Override
    public int getRefreshInterval() {
        TaskDtoExecutionStatus exec = getTaskDto().getExecution();
        if (exec == null) {
            return REFRESH_INTERVAL_IF_CLOSED;
        }
        switch (exec) {
            case RUNNING:
            case SUSPENDING: return REFRESH_INTERVAL_IF_RUNNING;
            case RUNNABLE:return REFRESH_INTERVAL_IF_RUNNABLE;
            case SUSPENDED: return REFRESH_INTERVAL_IF_SUSPENDED;
            case WAITING: return REFRESH_INTERVAL_IF_WAITING;
            case CLOSED: return REFRESH_INTERVAL_IF_CLOSED;
        }
        return REFRESH_INTERVAL_IF_RUNNABLE;
    }

    public void refresh(AjaxRequestTarget target) {
        TaskTabsVisibility tabsVisibilityOld = new TaskTabsVisibility();
        tabsVisibilityOld.computeAll(this);

        refreshTaskModels();

        TaskTabsVisibility tabsVisibilityNew = new TaskTabsVisibility();
        tabsVisibilityNew.computeAll(this);

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

        AutoRefreshDto refreshDto = refreshModel.getObject();
        refreshDto.recordRefreshed();

//        if (isEdit() || !refreshDto.isEnabled()) {
//            getRefreshPanel().stopRefreshing(this, target);
//        } else {
//        }
    }

    public TaskDto getCurrentTaskDto() {
        return currentTaskDto;
    }

    public TaskDto getPreviousTaskDto() {
        return previousTaskDto;
    }

    @Override
    public Component getRefreshingBehaviorParent() {
        return null;
    }

    private void refreshTaskModels() {
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
            TaskDto newTaskDto = prepareTaskDto(taskType, true, operationTask, result);
            final PrismObjectWrapper<TaskType> newWrapper = loadObjectWrapper(taskType.asPrismObject(), operationTask, result);
            previousTaskDto = currentTaskDto;
            currentTaskDto = newTaskDto;
            taskDtoModel.setObject(newTaskDto);
            objectWrapperModel.setObject(newWrapper);
        } catch (SchemaException|RuntimeException|Error e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't refresh task {}", e, oldTaskDto);
            result.recordFatalError(getString("PageTaskEdit.message.refreshTaskModels.fatalError", e.getMessage()), e);
        }
        result.computeStatusIfUnknown();
        if (!result.isSuccess()) {
            showResult(result);
        }
    }

    protected PrismObjectWrapper<TaskType> loadObjectWrapper(PrismObject<TaskType> object, Task task, OperationResult result) {
        PrismObjectWrapper<TaskType> wrapper;
//        ObjectWrapperFactory owf = new ObjectWrapperFactory(this);
        PrismObjectWrapperFactory<TaskType> owf = getRegistry().getObjectWrapperFactory(object.getDefinition());
        try {
            object.revive(getPrismContext());        // just to be sure (after deserialization the context is missing in this object)
//            wrapper = owf.createObjectWrapper("pageAdminFocus.focusDetails", null, object, ContainerStatus.MODIFYING, task);
            WrapperContext context = new WrapperContext(task, result);
            wrapper = owf.createObjectWrapper(object, ItemStatus.NOT_CHANGED, context);
        } catch (Exception ex) {
            result.recordFatalError(getString("PageTaskEdit.message.loadObjectWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load task", ex);
            try {
                WrapperContext context = new WrapperContext(task, result);
                wrapper = owf.createObjectWrapper(object, ItemStatus.NOT_CHANGED, context);
//                wrapper = owf.createObjectWrapper("pageAdminFocus.focusDetails", null, object, null, null, ContainerStatus.MODIFYING, task);
            } catch (SchemaException e) {
                result.recordFatalError(e.getMessage(), e);
                showResult(result, false);
                throw new RestartResponseException(PageTasks.class);
            }
        }
        showResult(result, false);

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

    public LoadableModel<PrismObjectWrapper<TaskType>> getObjectWrapperModel() {
        return objectWrapperModel;
    }

    public TaskDto getTaskDto() {
        return taskDtoModel.getObject();
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
        return isEditable(objectWrapperModel.getObject());
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

    protected boolean isEditable(ItemPath itemPath) {
        ItemDefinition<?> itemDefinition = objectWrapperModel.getObject().findItemDefinition(itemPath);
        if (itemDefinition != null) {
            return itemDefinition.canRead() && itemDefinition.canModify();
        } else {
            return true;
        }
    }

    protected boolean isReadable(ItemPath itemPath) {
        ItemDefinition<?> itemDefinition =  objectWrapperModel.getObject().findItemDefinition(itemPath);
        if (itemDefinition != null) {
            return itemDefinition.canRead();
        } else {
            return true;
        }
    }

    public boolean isExtensionReadable(ItemName name) {
        return isReadable(ItemPath.create(TaskType.F_EXTENSION, name));
    }

    boolean isReadableSomeOf(QName... names) {
        for (QName name : names) {
            if (isReadable(ItemName.fromQName(name))) {
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
            return true;            // it is only GUI thing
        }
    }
}
