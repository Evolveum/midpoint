/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionState;

import com.evolveum.midpoint.web.util.TaskOperationUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/tasks2", matchUrlForSecurity = "/admin/tasks2")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                        label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                        description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_URL,
                        label = "PageTasks.auth.tasks.label",
                        description = "PageTasks.auth.tasks.description") })
public abstract class TaskTablePanel extends MainObjectListPanel<TaskType> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskTablePanel.class);

    private static final String DOT_CLASS = TaskTablePanel.class.getName() + ".";
    public static final String OPERATION_SUSPEND_TASK = DOT_CLASS + "suspendTask";
    public static final String OPERATION_RESUME_TASK = DOT_CLASS + "resumeTask";
    public static final String OPERATION_DELETE_TASKS = DOT_CLASS + "deleteTasks";
    public static final String OPERATION_RECONCILE_WORKERS = DOT_CLASS + "reconcileWorkers";
    public static final String OPERATION_DELETE_WORKERS_AND_WORK_STATE = DOT_CLASS + "deleteWorkersAndWorkState";
    public static final String OPERATION_DELETE_WORK_STATE = DOT_CLASS + "deleteWorkState";
    public static final String OPERATION_DELETE_ALL_CLOSED_TASKS = DOT_CLASS + "deleteAllClosedTasks";
    public static final String OPERATION_SCHEDULE_TASKS = DOT_CLASS + "scheduleTasks";
    private static final String OPERATION_SYNCHRONIZE_TASKS = DOT_CLASS + "synchronizeTasks";

    public static final long WAIT_FOR_TASK_STOP = 2000L;

    public TaskTablePanel(String id, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, TaskType.class, options);
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, TaskType object) {
        taskDetailsPerformed(object.getOid());
    }

    @Override
    protected boolean isObjectDetailsEnabled(IModel<SelectableBean<TaskType>> rowModel) {
        return rowModel.getObject().getValue().getOid() != null;
    }

    @Override
    protected List<IColumn<SelectableBean<TaskType>, String>> createDefaultColumns() {
        return initTaskColumns();
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return createTasksInlineMenu();
    }

    @Override
    protected List<Component> createToolbarButtonsList(String buttonId) {
        List<Component> buttonsList = super.createToolbarButtonsList(buttonId);
        AjaxIconButton synchronizeTasks = createSynchronizeTasksButton(buttonId);
        buttonsList.add(synchronizeTasks);
        return buttonsList;
    }

    private AjaxIconButton createSynchronizeTasksButton(String buttonId) {
        AjaxIconButton synchronize = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM),
                createStringResource("pageTasks.button.synchronizeTasks")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                synchronizeTasksPerformed(target);
            }
        };
        synchronize.add(AttributeAppender.append("class", "btn btn-default btn-margin-left btn-sm"));
        return synchronize;
    }

    private Task createSimpleTask(String taskName) {
        return getPageBase().createSimpleTask(taskName);
    }

    private TaskService getTaskService() {
        return getPageBase().getTaskService();
    }

    private TaskManager getTaskManager() {
        return getPageBase().getTaskManager();
    }

    private void showResult(OperationResult result) {
        getPageBase().showResult(result);
    }

    private WebMarkupContainer getFeedbackPanel() {
        return getPageBase().getFeedbackPanel();
    }

    private void synchronizeTasksPerformed(AjaxRequestTarget target) {
        Task opTask = createSimpleTask(OPERATION_SYNCHRONIZE_TASKS);
        OperationResult result = opTask.getResult();

        try {
            getTaskService().synchronizeTasks(opTask, result);
            result.computeStatus();
            if (result.isSuccess()) { // brutal hack - the subresult's message
                // contains statistics
                result.recordStatus(OperationResultStatus.SUCCESS, result.getLastSubresult().getMessage());
            }
        } catch (Throwable e) {
            result.recordFatalError(createStringResource("pageTasks.message.synchronizeTasksPerformed.fatalError").getString(),
                    e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTable(target);
        target.add(getTable());
        clearCache();
    }

    private void taskDetailsPerformed(String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        getPageBase().navigateToNext(PageTask.class, parameters);
    }

    private List<IColumn<SelectableBean<TaskType>, String>> initTaskColumns() {
        List<IColumn<SelectableBean<TaskType>, String>> columns = new ArrayList<>();

        if (!isCollectionViewPanelForCompiledView()) {
            columns.add(createTaskCategoryColumn());
        }
        columns.addAll(initCustomTaskColumns());

        return columns;
    }

    private IColumn<SelectableBean<TaskType>, String> createTaskCategoryColumn() {
        return new AbstractExportableColumn<>(createStringResource("pageTasks.task.category"), TaskType.F_CATEGORY.getLocalPart()) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> item, String componentId,
                    final IModel<SelectableBean<TaskType>> rowModel) {
                item.add(new Label(componentId,
                        WebComponentUtil.createCategoryNameModel(TaskTablePanel.this, new PropertyModel<>(rowModel, SelectableBeanImpl.F_VALUE + "." + TaskType.F_CATEGORY.getLocalPart()))));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                return WebComponentUtil.createCategoryNameModel(TaskTablePanel.this, new PropertyModel<>(rowModel, SelectableBeanImpl.F_VALUE + "." + TaskType.F_CATEGORY.getLocalPart()));
            }
        };

    }

    protected List<IColumn<SelectableBean<TaskType>, String>> initCustomTaskColumns() {
        List<IColumn<SelectableBean<TaskType>, String>> columns = new ArrayList<>();

        columns.add(createTaskExecutionStatusColumn());

        columns.add(createProgressColumn());
        columns.add(createErrorsColumn());
        columns.add(createTaskStatusIconColumn());

        return columns;

    }

    private AbstractExportableColumn<SelectableBean<TaskType>, String> createTaskExecutionStatusColumn() {
        return new AbstractExportableColumn<>(createStringResource("pageTasks.task.execution")) {

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getValue() != null) {
                    TaskType task = rowModel.getObject().getValue();
                    TaskDtoExecutionState status = TaskDtoExecutionState.fromTaskExecutionStatus(task.getExecutionStatus(), task.getNodeAsObserved() != null);
                    if (status != null) {
                        return getPageBase().createStringResource(status);
                    }
                }
                return Model.of("");
            }
        };
    }

    private AbstractExportableColumn<SelectableBean<TaskType>, String> createProgressColumn() {
        return new AbstractExportableColumn<>(createStringResource("pageTasks.task.progress")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> cellItem, String componentId, final IModel<SelectableBean<TaskType>> rowModel) {
                if (!TaskWorkStateUtil.isPartitionedMaster(rowModel.getObject().getValue())) {
                    cellItem.add(new Label(componentId,
                            (IModel<Object>) () -> getProgressDescription(rowModel.getObject())));
                } else {
                    cellItem.add(new AjaxLinkPanel(componentId, createStringResource("PageTasks.show.child.progress")) {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            PageParameters pageParams = new PageParameters();
                            pageParams.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getValue().getOid());
                            getPageBase().navigateToNext(PageTask.class, pageParams);
                        }
                    });
                }

            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                return Model.of(getProgressDescription(rowModel.getObject()));
            }
        };
    }

    private AbstractColumn<SelectableBean<TaskType>, String> createErrorsColumn() {
        return new AbstractColumn<>(createStringResource("pageTasks.task.errors")) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> cellItem, String componentId, IModel<SelectableBean<TaskType>> rowModel) {
                TaskType task = rowModel.getObject().getValue();
                cellItem.add(new Label(componentId, new Model<>(TaskOperationStatsUtil.getItemsProcessedWithFailureFromTree(task, getPrismContext()))));
            }
        };
    }

    private IconColumn<SelectableBean<TaskType>> createTaskStatusIconColumn() {
        return new IconColumn<>(createStringResource("pageTasks.task.status"), TaskType.F_RESULT_STATUS.getLocalPart()) {

            @Override
            protected DisplayType getIconDisplayType(final IModel<SelectableBean<TaskType>> rowModel) {
                String icon;
                String title;

                TaskType task = getTask(rowModel, false);

                if (task != null && task.getResultStatus() != null) {
                    icon = OperationResultStatusPresentationProperties
                            .parseOperationalResultStatus(task.getResultStatus()).getIcon()
                            + " fa-lg";
                    title = createStringResource(task.getResultStatus()).getString();
                } else {
                    icon = OperationResultStatusPresentationProperties.UNKNOWN.getIcon() + " fa-lg";
                    title = createStringResource(OperationResultStatusType.UNKNOWN).getString();
                }

                return WebComponentUtil.createDisplayType(icon, "", title);
            }
        };
    }

    private String getProgressDescription(SelectableBean<TaskType> task) {
        Long stalledSince = getStalledSince(task.getValue());
        String realProgress = WebComponentUtil.getTaskProgressInformation(task.getValue(), false, getPageBase());
        if (stalledSince != null) {
            return getString("pageTasks.stalledSince", new Date(stalledSince).toLocaleString(), realProgress);
        } else {
            return realProgress;
        }
    }

    private Long getStalledSince(TaskType taskType) {
        return xgc2long(taskType.getStalledSince());
    }

    private Long xgc2long(XMLGregorianCalendar gc) {
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }

    private List<InlineMenuItem> createTasksInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(createTaskSuspendActionNew());
        items.add(createTaskResumeAction());
        items.add(createScheduleTaskAction());

        items.add(createDeleteTaskMenuAction());
        items.add(createReconcileWorkersMenuAction());
        items.add(createSuspendRootOnlyMenuAction());
        items.add(createResumeRootOnlyMenuAction());
        items.add(createDeleteWorkStateAndWorkersMenuAction());
        items.add(createDeleteWorkStateMenuAction());
        items.add(createDeleteAllClosedTasksMenuAction());
        return items;
    }

    private ButtonInlineMenuItem createTaskSuspendActionNew() {
        return createTaskButtonAction("pageTasks.button.suspendTask",
                this::suspendTasksPerformed,
                GuiStyleConstants.CLASS_SUSPEND_MENU_ITEM,
                "pageTasks.message.suspendAction",
                (task) -> WebComponentUtil.canSuspendTask(task, TaskTablePanel.this.getPageBase()));
    }

    private ButtonInlineMenuItem createTaskResumeAction() {
        return createTaskButtonAction("pageTasks.button.resumeTask",
                this::resumeTasksPerformed,
                GuiStyleConstants.CLASS_RESUME_MENU_ITEM,
                "pageTasks.message.resumeAction",
                task -> WebComponentUtil.canResumeTask(task, TaskTablePanel.this.getPageBase()));
    }

    private ButtonInlineMenuItem createScheduleTaskAction() {
        return createTaskButtonAction("pageTasks.button.scheduleTask",
                this::scheduleTasksPerformed,
                GuiStyleConstants.CLASS_START_MENU_ITEM,
                "pageTasks.message.runNowAction",
                task -> WebComponentUtil.canRunNowTask(task, TaskTablePanel.this.getPageBase()));
    }

    private ButtonInlineMenuItem createTaskButtonAction(String buttonNameKey,
            SerializableBiConsumer<AjaxRequestTarget, IModel<SelectableBean<TaskType>>> action,
            String icon, String confirmationMessageKey,
            SerializableFunction<TaskType, Boolean> visibilityHandler) {

        return new ButtonInlineMenuItem(createStringResource(buttonNameKey)) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        action.accept(target, getRowModel());
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(icon);
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource(confirmationMessageKey).getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public IModel<Boolean> getVisible() {

                IModel<SelectableBean<TaskType>> rowModel = ((ColumnMenuAction) getAction()).getRowModel();
                if (rowModel == null) {
                    return Model.of(Boolean.TRUE);
                }
                SelectableBean<TaskType> rowModelObj = rowModel.getObject();
                boolean visible = visibilityHandler.apply(rowModelObj.getValue());
                return Model.of(visible);
            }

        };
    }

    private InlineMenuItem createDeleteTaskMenuAction() {
        return createTaskInlineMenuItem("pageTasks.button.deleteTask",
                this::deleteTaskConfirmedPerformed,
                "pageTasks.message.deleteAction",
                (task) -> true,
                true);
    }

    private InlineMenuItem createReconcileWorkersMenuAction() {
        InlineMenuItem reconcileWorkers = createTaskInlineMenuItem("pageTasks.button.reconcileWorkers",
                this::reconcileWorkersConfirmedPerformed,
                "pageTasks.message.reconcileWorkersAction",
                (task) -> true,
                false);
        reconcileWorkers.setVisibilityChecker(TaskTablePanel::isCoordinator);
        return reconcileWorkers;
    }

    private InlineMenuItem createSuspendRootOnlyMenuAction() {
        InlineMenuItem suspendRootOnly = createTaskInlineMenuItem("pageTasks.button.suspendRootOnly",
                this::suspendRootOnly,
                "pageTasks.message.suspendAction",
                (task) -> true,
                false);
        suspendRootOnly.setVisibilityChecker(TaskTablePanel::isManageableTreeRoot);
        return suspendRootOnly;
    }

    private InlineMenuItem createResumeRootOnlyMenuAction() {
        InlineMenuItem resumeRootOnly = createTaskInlineMenuItem("pageTasks.button.resumeRootOnly",
                this::resumeRootOnly,
                "pageTasks.message.resumeAction",
                (task) -> true,
                false);
        resumeRootOnly.setVisibilityChecker(TaskTablePanel::isManageableTreeRoot);
        return resumeRootOnly;
    }

    private InlineMenuItem createDeleteWorkStateAndWorkersMenuAction() {
        InlineMenuItem deleteWorkStateAndWorkers = createTaskInlineMenuItem("pageTasks.button.deleteWorkersAndWorkState",
                this::deleteWorkersAndWorkState,
                "pageTasks.message.deleteWorkersAndWorkState",
                (task) -> true,
                false);
        deleteWorkStateAndWorkers.setVisibilityChecker(TaskTablePanel::isManageableTreeRoot);
        return deleteWorkStateAndWorkers;
    }

    private InlineMenuItem createDeleteWorkStateMenuAction() {
        return createTaskInlineMenuItem("pageTasks.button.deleteWorkState",
                this::deleteWorkStatePerformed,
                "pageTasks.message.deleteWorkState",
                (task) -> WebComponentUtil.canSuspendTask(task, TaskTablePanel.this.getPageBase()),
                true);
    }

    private InlineMenuItem createDeleteAllClosedTasksMenuAction() {
        return createTaskInlineMenuItem("pageTasks.button.deleteAllClosedTasks",
                (target, task) -> deleteAllClosedTasksConfirmedPerformed(target),
                "pageTasks.message.deleteAllClosedTasksConfirm",
                (task) -> false,
                true);
    }

    private InlineMenuItem createTaskInlineMenuItem(String menuNameKey,
            SerializableBiConsumer<AjaxRequestTarget, IModel<SelectableBean<TaskType>>> action,
            String confirmationMessageKey,
            SerializableFunction<TaskType, Boolean> visibilityHandler,
            boolean header) {
        return new InlineMenuItem(createStringResource(menuNameKey)) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        action.accept(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource(confirmationMessageKey).getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return header;
            }

            @Override
            public IModel<Boolean> getVisible() {
                IModel<SelectableBean<TaskType>> rowModel = ((ColumnMenuAction) getAction()).getRowModel();
                if (rowModel == null) {
                    return Model.of(Boolean.TRUE);
                }
                return Model.of(visibilityHandler.apply(rowModel.getObject().getValue()));
            }
        };
    }

    //region Task-level actions
    private void suspendTasksPerformed(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> selectedTask) {
        List<TaskType> selectedTasks = getSelectedTasks(target, selectedTask);
        if (selectedTasks == null) {
            return;
        }
        OperationResult result = TaskOperationUtils.suspendTasks(selectedTasks, getPageBase());
        showResult(result);

        //refresh feedback and table
        refreshTable(target);
        clearCache();
    }

    private void resumeTasksPerformed(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> selectedTask) {
        List<TaskType> selectedTasks = getSelectedTasks(target, selectedTask);
        if (selectedTasks == null) {
            return;
        }
        OperationResult result = TaskOperationUtils.resumeTasks(selectedTasks, getPageBase());
        showResult(result);

        //refresh feedback and table
        refreshTable(target);
        clearCache();

    }

    private List<TaskType> getSelectedTasks(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> selectedTask) {
        List<TaskType> selectedTasks = new ArrayList<>();
        if (selectedTask != null) {
            selectedTasks.add(selectedTask.getObject().getValue());
        } else {
            selectedTasks = getSelectedRealObjects();
        }

        if (selectedTasks.isEmpty()) {
            target.add(getFeedbackPanel());
            return null;
        }

        return selectedTasks;
    }

    private void scheduleTasksPerformed(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> selectedTask) {

        List<TaskType> selectedTasks = getSelectedTasks(target, selectedTask);
        if (selectedTasks == null) {
            return;
        }
        Task opTask = createSimpleTask(OPERATION_SCHEDULE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().scheduleTasksNow(ObjectTypeUtil.getOids(selectedTasks), opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("pageTasks.message.scheduleTasksPerformed.success").getString());
            }
        } catch (Throwable e) {
            result.recordFatalError(createStringResource("pageTasks.message.scheduleTasksPerformed.fatalError").getString(), e);
        }
        showResult(result);

        //refresh feedback and table
        refreshTable(target);
        clearCache();

    }

    private void deleteTaskConfirmedPerformed(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> task) {
        List<TaskType> selectedTasks = getSelectedTasks(target, task);
        if (selectedTasks == null) {
            return;
        }

        Task opTask = createSimpleTask(OPERATION_DELETE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().suspendAndDeleteTasks(ObjectTypeUtil.getOids(selectedTasks), WAIT_FOR_TASK_STOP, true, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        createStringResource("pageTasks.message.deleteTaskConfirmedPerformed.success").getString());
            }
        } catch (Throwable e) {
            result.recordFatalError(createStringResource("pageTasks.message.deleteTaskConfirmedPerformed.fatalError").getString(),
                    e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTable(target);
        clearCache();
    }

    private void reconcileWorkersConfirmedPerformed(AjaxRequestTarget target, @NotNull IModel<SelectableBean<TaskType>> task) {
        Task opTask = createSimpleTask(OPERATION_RECONCILE_WORKERS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().reconcileWorkers(task.getObject().getValue().getOid(), opTask, result);
            result.computeStatus();
            if (result.isSuccess() && result.getSubresults().size() == 1) { // brutal hack: to show statistics
                result.setMessage(result.getSubresults().get(0).getMessage());
            }
        } catch (Throwable e) {
            result.recordFatalError(
                    createStringResource("pageTasks.message.reconcileWorkersConfirmedPerformed.fatalError").getString(), e);
        }
        showResult(result);

        refreshTable(target);
        clearCache();
    }

    private void suspendRootOnly(AjaxRequestTarget target, @NotNull IModel<SelectableBean<TaskType>> task) {
        Task opTask = createSimpleTask(OPERATION_SUSPEND_TASK);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().suspendTasks(Collections.singleton(task.getObject().getValue().getOid()), WAIT_FOR_TASK_STOP, opTask, result);
            // TODO check whether the suspension was complete
            result.computeStatus();
        } catch (Throwable e) {
            result.recordFatalError(createStringResource("pageTasks.message.suspendRootOnly.fatalError").getString(), e);
        }
        showResult(result);

        refreshTable(target);
        clearCache();
    }

    private void resumeRootOnly(AjaxRequestTarget target, @NotNull IModel<SelectableBean<TaskType>> task) {
        Task opTask = createSimpleTask(OPERATION_RESUME_TASK);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().resumeTasks(Collections.singleton(task.getObject().getValue().getOid()), opTask, result);
            result.computeStatus();
        } catch (Throwable e) {
            result.recordFatalError(createStringResource("pageTasks.message.resumeRootOnly.fatalError").getString(), e);
        }
        showResult(result);

        refreshTable(target);
        clearCache();
    }

    private void deleteWorkersAndWorkState(AjaxRequestTarget target, @NotNull IModel<SelectableBean<TaskType>> task) {
        Task opTask = createSimpleTask(OPERATION_DELETE_WORKERS_AND_WORK_STATE);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().deleteWorkersAndWorkState(task.getObject().getValue().getOid(), true, WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
        } catch (Throwable e) {
            result.recordFatalError(createStringResource("pageTasks.message.deleteWorkersAndWorkState.fatalError").getString(),
                    e);
        }
        showResult(result);

        refreshTable(target);
        clearCache();
    }

    private void deleteWorkStatePerformed(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> task) {
        List<TaskType> selectedTasks = getSelectedTasks(target, task);
        if (selectedTasks == null) {
            return;
        }
        selectedTasks.forEach(selectedTask -> deleteWorkState(target, selectedTask));
    }

    private void deleteWorkState(AjaxRequestTarget target, @NotNull TaskType task) {
        Task opTask = createSimpleTask(OPERATION_DELETE_WORK_STATE);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().deleteWorkersAndWorkState(task.getOid(), false, WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
        } catch (Throwable e) {
            result.recordFatalError(createStringResource("pageTasks.message.deleteWorkState.fatalError").getString(),
                    e);
        }
        showResult(result);

        refreshTable(target);
        clearCache();
    }

    private void deleteAllClosedTasksConfirmedPerformed(AjaxRequestTarget target) {
        OperationResult launchResult = new OperationResult(OPERATION_DELETE_ALL_CLOSED_TASKS);
        Task task = createSimpleTask(OPERATION_DELETE_ALL_CLOSED_TASKS);

        task.setHandlerUri(ModelPublicConstants.CLEANUP_TASK_HANDLER_URI);
        task.setName("Closed tasks cleanup");

        try {
            CleanupPolicyType policy = new CleanupPolicyType();
            policy.setMaxAge(XmlTypeConverter.createDuration(0));

            CleanupPoliciesType policies = new CleanupPoliciesType(getPrismContext());
            policies.setClosedTasks(policy);
            task.setExtensionContainerValue(SchemaConstants.MODEL_EXTENSION_CLEANUP_POLICIES, policies);
        } catch (SchemaException e) {
            LOGGER.error("Error dealing with schema (task {})", task, e);
            launchResult.recordFatalError(
                    createStringResource("pageTasks.message.deleteAllClosedTasksConfirmedPerformed.fatalError").getString(), e);
            throw new IllegalStateException("Error dealing with schema", e);
        }

        task.addArchetypeInformationIfMissing(SystemObjectsType.ARCHETYPE_CLEANUP_TASK.value());
        getTaskManager().switchToBackground(task, launchResult);
        launchResult.setBackgroundTaskOid(task.getOid());

        showResult(launchResult);
        target.add(getFeedbackPanel());
    }

    private IModel<String> getTaskConfirmationMessageModel(ColumnMenuAction<SelectableBean<TaskType>> action, String actionName) {
        if (action.getRowModel() != null) {
            String objectName = WebComponentUtil.getName(getTask(action.getRowModel(), false));
            return createStringResource("pageTasks.message.confirmationMessageForSingleTaskObject", actionName, objectName);
        }

        if (CollectionUtils.isEmpty(getSelectedRealObjects())) {
            getSession().warn(getString("pageTasks.message.confirmationMessageForNoTaskObject", actionName));
            return null; //confirmation popup should not be shown
        }

        return createStringResource("pageTasks.message.confirmationMessageForMultipleTaskObject", actionName, getSelectedRealObjects().size());
    }

    // must be static, otherwise JVM crashes (probably because of some wicket serialization issues)
    @SuppressWarnings("unchecked")
    private static boolean isCoordinator(IModel<?> rowModel, boolean isHeader) {
        if (!isTaskModel(rowModel)) {
            return false;
        }
        TaskType task = getTask((IModel<SelectableBean<TaskType>>) rowModel, isHeader);
        return task != null && TaskWorkStateUtil.isCoordinator(task);
    }

    // must be static, otherwise JVM crashes (probably because of some wicket serialization issues)
    @SuppressWarnings("unchecked")
    private static boolean isManageableTreeRoot(IModel<?> rowModel, boolean isHeader) {
        if (!isTaskModel(rowModel)) {
            return false;
        }
        TaskType task = getTask((IModel<SelectableBean<TaskType>>) rowModel, isHeader);
        return task != null && TaskWorkStateUtil.isManageableTreeRoot(task);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isTaskModel(IModel<?> rowModel) {
        return rowModel != null && rowModel.getObject() instanceof SelectableBean;
    }

    private static TaskType getTask(IModel<SelectableBean<TaskType>> rowModel, boolean isHeader) {
        if (rowModel != null && !isHeader) {
            SelectableBean<TaskType> object = rowModel.getObject();
            if (object == null) {
                return null;
            }

            return object.getValue();
        }
        return null;
    }

}
