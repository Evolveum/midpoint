/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
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
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.stream.Collectors;

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
                        description = "PageTasks.auth.tasks.description")})
public class TaskTablePanel extends MainObjectListPanel<TaskType> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskTablePanel.class);

    private static final String DOT_CLASS = TaskTablePanel.class.getName() + ".";
    public static final String OPERATION_SUSPEND_TASKS = DOT_CLASS + "suspendTasks";
    public static final String OPERATION_SUSPEND_TASK = DOT_CLASS + "suspendTask";
    public static final String OPERATION_RESUME_TASKS = DOT_CLASS + "resumeTasks";
    public static final String OPERATION_RESUME_TASK = DOT_CLASS + "resumeTask";
    public static final String OPERATION_DELETE_TASKS = DOT_CLASS + "deleteTasks";
    public static final String OPERATION_RECONCILE_WORKERS = DOT_CLASS + "reconcileWorkers";
    public static final String OPERATION_DELETE_WORKERS_AND_WORK_STATE = DOT_CLASS + "deleteWorkersAndWorkState";
    public static final String OPERATION_DELETE_WORK_STATE = DOT_CLASS + "deleteWorkState";
    public static final String OPERATION_DELETE_ALL_CLOSED_TASKS = DOT_CLASS + "deleteAllClosedTasks";
    public static final String OPERATION_SCHEDULE_TASKS = DOT_CLASS + "scheduleTasks";
    private static final String OPERATION_SYNCHRONIZE_TASKS = DOT_CLASS + "synchronizeTasks";

    public static final long WAIT_FOR_TASK_STOP = 2000L;

    public TaskTablePanel(String id, UserProfileStorage.TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, TaskType.class, tableId, options);
    }


    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, TaskType object) {
        taskDetailsPerformed(target, object.getOid());
    }

    @Override
    protected boolean isObjectDetailsEnabled(IModel<SelectableBean<TaskType>> rowModel) {
        return rowModel.getObject().getValue().getOid() != null;
    }

    @Override
    protected List<IColumn<SelectableBean<TaskType>, String>> createColumns() {
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

    private void navigateToNext(Class<? extends WebPage> page, PageParameters parameters) {
        getPageBase().navigateToNext(page, parameters);
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
        } catch (RuntimeException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.synchronizeTasksPerformed.fatalError").getString(),
                    e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTable(TaskType.class, target);
        target.add(getTable());
    }

    private void taskDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        navigateToNext(PageTask.class, parameters);
    }

    private List<IColumn<SelectableBean<TaskType>, String>> initTaskColumns() {
        List<IColumn<SelectableBean<TaskType>, String>> columns = new ArrayList<>();

        if (!isCollectionViewPanel()){
            columns.add(createTaskCategoryColumn());
        }
        columns.addAll(initCustomTaskColumns());

        return columns;
    }

    private IColumn<SelectableBean<TaskType>, String> createTaskCategoryColumn() {
        return new AbstractExportableColumn<SelectableBean<TaskType>, String>(createStringResource("pageTasks.task.category"), TaskType.F_CATEGORY.getLocalPart()) {

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

        columns.add(createProgressColumn("pageTasks.task.progress"));
        columns.add(createErrorsColumn("pageTasks.task.errors"));

        columns.add(new IconColumn<SelectableBean<TaskType>>(createStringResource("pageTasks.task.status"), TaskType.F_RESULT_STATUS.getLocalPart()) {

            @Override
            protected DisplayType getIconDisplayType(final IModel<SelectableBean<TaskType>> rowModel) {
                String icon = "";
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getValue().getResultStatus() != null) {
                    icon = OperationResultStatusPresentationProperties
                            .parseOperationalResultStatus(rowModel.getObject().getValue().getResultStatus()).getIcon()
                            + " fa-lg";
                } else {
                    icon = OperationResultStatusPresentationProperties.UNKNOWN.getIcon() + " fa-lg";
                }

                String title = "";
                TaskType dto = rowModel.getObject().getValue();

                if (dto != null && dto.getResultStatus() != null) {
                    title = createStringResource(dto.getResultStatus()).getString();
                } else {
                    title = createStringResource(OperationResultStatusType.UNKNOWN).getString();
                }
                return WebComponentUtil.createDisplayType(icon, "", title);
            }
        });

        return columns;

    }

    private EnumPropertyColumn<SelectableBean<TaskType>> createTaskExecutionStatusColumn() {
        return new EnumPropertyColumn<SelectableBean<TaskType>>(createStringResource("pageTasks.task.execution"), TaskType.F_EXECUTION_STATUS.getLocalPart(), SelectableBeanImpl.F_VALUE + ".executionStatus") {

            @Override
            protected String translate(Enum en) {
                return TaskTablePanel.this.createStringResource(en).getString();
            }
        };
    }

    private AbstractExportableColumn<SelectableBean<TaskType>, String> createProgressColumn(String titleKey) {
        return new AbstractExportableColumn<SelectableBean<TaskType>, String>(createStringResource(titleKey)) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> cellItem, String componentId, final IModel<SelectableBean<TaskType>> rowModel) {
                if (!TaskTypeUtil.isPartitionedMaster(rowModel.getObject().getValue())) {
                    cellItem.add(new Label(componentId,
                            (IModel<Object>) () -> getProgressDescription(rowModel.getObject())));
                } else {
                    cellItem.add(new LinkPanel(componentId, createStringResource("PageTasks.show.child.progress")) {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            PageParameters pageParams = new PageParameters();
                            pageParams.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getValue().getOid());
                            navigateToNext(PageTask.class, pageParams);
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

    private AbstractColumn<SelectableBean<TaskType>, String> createErrorsColumn(String titleKey) {
        return new AbstractColumn<SelectableBean<TaskType>, String>(createStringResource(titleKey)) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<TaskType>>> cellItem, String componentId, IModel<SelectableBean<TaskType>> rowModel) {
                TaskType task = rowModel.getObject().getValue();
                cellItem.add(new Label(componentId, new Model<>(TaskTypeUtil.getObjectsProcessedFailures(task, getPrismContext()))));

            }
        };
    }

    private String getProgressDescription(SelectableBean<TaskType> task) {
        Long stalledSince = getStalledSince(task.getValue());
        if (stalledSince != null) {
            return getString("pageTasks.stalledSince", new Date(stalledSince).toLocaleString(), getRealProgressDescription(task));
        } else {
            return getRealProgressDescription(task);
        }
    }

    private String getRealProgressDescription(SelectableBean<TaskType> task) {
        if (TaskTypeUtil.isWorkStateHolder(task.getValue())) {
            return getBucketedTaskProgressDescription(task.getValue());
        } else {
            return getPlainTaskProgressDescription(task.getValue());
        }
    }

    private String getBucketedTaskProgressDescription(TaskType taskType) {
        int completeBuckets = getCompleteBuckets(taskType);
        Integer expectedBuckets = getExpectedBuckets(taskType);
        if (expectedBuckets == null) {
            return String.valueOf(completeBuckets);
        } else {
            return (completeBuckets*100/expectedBuckets) + "%";
        }
    }

    private Integer getExpectedBuckets(TaskType taskType) {
        return taskType.getWorkState() != null ? taskType.getWorkState().getNumberOfBuckets() : null;
    }

    private Integer getCompleteBuckets(TaskType taskType) {
        return TaskWorkStateTypeUtil.getCompleteBucketsNumber(taskType);
    }


    private String getPlainTaskProgressDescription(TaskType taskType) {
        Long currentProgress = taskType.getProgress();
        if (currentProgress == null && taskType.getExpectedTotal() == null) {
            return "";      // the task handler probably does not report progress at all
        } else {
            StringBuilder sb = new StringBuilder();
            if (currentProgress != null){
                sb.append(currentProgress);
            } else {
                sb.append("0");
            }
            if (taskType.getExpectedTotal() != null) {
                sb.append("/").append(taskType.getExpectedTotal());
            }
            return sb.toString();
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
        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.suspendTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                            suspendTasksPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_SUSPEND_MENU_ITEM;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.suspendAction").getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public IModel<Boolean> getVisible() {
                IModel<SelectableBean<TaskType>> rowModel = ((ColumnMenuAction) getAction()).getRowModel();
                if (rowModel == null){
                    return Model.of(Boolean.TRUE);
                }
                SelectableBean<TaskType> rowModelObj = rowModel.getObject();
                boolean visible = WebComponentUtil.canSuspendTask(rowModelObj.getValue(), TaskTablePanel.this.getPageBase());
                return Model.of(visible);
            }
        });
        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.resumeTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                            resumeTasksPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_RESUME_MENU_ITEM;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.resumeAction").getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public IModel<Boolean> getVisible() {
                IModel<SelectableBean<TaskType>> rowModel = ((ColumnMenuAction) getAction()).getRowModel();
                if (rowModel == null){
                    return Model.of(Boolean.TRUE);
                }
                SelectableBean<TaskType> rowModelObj = rowModel.getObject();
                boolean visible = WebComponentUtil.canResumeTask(rowModelObj.getValue(), TaskTablePanel.this.getPageBase());
                return Model.of(visible);
            }

        });
        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.scheduleTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                            scheduleTasksPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_START_MENU_ITEM;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.runNowAction").getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public IModel<Boolean> getVisible() {
                IModel<SelectableBean<TaskType>> rowModel = ((ColumnMenuAction) getAction()).getRowModel();
                if (rowModel == null){
                    return Model.of(Boolean.TRUE);
                }
                SelectableBean<TaskType> rowModelObj = rowModel.getObject();
                return Model.of(WebComponentUtil.canRunNowTask(rowModelObj.getValue(), TaskTablePanel.this.getPageBase()));
            }

        });
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteTaskConfirmedPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteAction").getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

        });

        InlineMenuItem reconcileWorkers = new InlineMenuItem(createStringResource("pageTasks.button.reconcileWorkers")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        reconcileWorkersConfirmedPerformed(target, getRowModel());
                    }
                };
            }
            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.reconcileWorkersAction").getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
        reconcileWorkers.setVisibilityChecker(TaskTablePanel::isCoordinator);
        items.add(reconcileWorkers);

        InlineMenuItem suspendRootOnly = new InlineMenuItem(createStringResource("pageTasks.button.suspendRootOnly")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        suspendRootOnly(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.suspendAction").getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
        suspendRootOnly.setVisibilityChecker(TaskTablePanel::isManageableTreeRoot);
        items.add(suspendRootOnly);

        InlineMenuItem resumeRootOnly = new InlineMenuItem(createStringResource("pageTasks.button.resumeRootOnly")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        resumeRootOnly(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.resumeAction").getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
        resumeRootOnly.setVisibilityChecker(TaskTablePanel::isManageableTreeRoot);
        items.add(resumeRootOnly);

        InlineMenuItem deleteWorkStateAndWorkers = new InlineMenuItem(createStringResource("pageTasks.button.deleteWorkersAndWorkState")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteWorkersAndWorkState(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteWorkersAndWorkState").getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
        deleteWorkStateAndWorkers.setVisibilityChecker(TaskTablePanel::isManageableTreeRoot);
        items.add(deleteWorkStateAndWorkers);

        InlineMenuItem deleteWorkState = new InlineMenuItem(createStringResource("pageTasks.button.deleteWorkState")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                       deleteWorkState(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteWorkState").getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public IModel<Boolean> getVisible() {
                IModel<SelectableBean<TaskType>> rowModel = ((ColumnMenuAction) getAction()).getRowModel();
                if (rowModel == null){
                    return Model.of(Boolean.TRUE);
                }
                SelectableBean<TaskType> rowModelObj = rowModel.getObject();
                return Model.of(WebComponentUtil.canSuspendTask(rowModelObj.getValue(), TaskTablePanel.this.getPageBase()));
            }

        };
        items.add(deleteWorkState);

        items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteAllClosedTasks")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<TaskType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteAllClosedTasksConfirmedPerformed(target);
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                return createStringResource("pageTasks.message.deleteAllClosedTasksConfirm");
            }

            @Override
            public IModel<Boolean> getVisible() {
                IModel<SelectableBean<TaskType>> rowModel = ((ColumnMenuAction) getAction()).getRowModel();
                if (rowModel == null) {
                    return Model.of(Boolean.TRUE);
                }
                return Model.of(Boolean.FALSE);
            }
        });

        return items;
    }

    //region Task-level actions
    //TODO unify with TaskOperationUtils
    private void suspendTasksPerformed(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> selectedTask) {
        List<TaskType> selectedTasks = getSelectedTasks(target, selectedTask);
        if (selectedTasks == null) {
            return;
        }
        Task opTask = createSimpleTask(OPERATION_SUSPEND_TASKS);
        OperationResult result = opTask.getResult();
        try {
            List<TaskType> plainTasks = selectedTasks.stream().filter(dto -> !isManageableTreeRoot(dto)).collect(Collectors.toList());
            List<TaskType> trees = selectedTasks.stream().filter(dto -> isManageableTreeRoot(dto)).collect(Collectors.toList());
            boolean suspendedPlain = suspendPlainTasks(plainTasks, result, opTask);
            boolean suspendedTrees = suspendTrees(trees, result, opTask);
            result.computeStatus();
            if (result.isSuccess()) {
                if (suspendedPlain && suspendedTrees) {
                    result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("pageTasks.message.suspendTasksPerformed.success").getString());
                } else {
                    result.recordWarning( createStringResource("pageTasks.message.suspendTasksPerformed.warning").getString());
                }
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.suspendTasksPerformed.fatalError").getString(), e);
        }
        showResult(result);

        //refresh feedback and table
        refreshTable(TaskType.class, target);
    }

    private void resumeTasksPerformed(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> selectedTask) {
        List<TaskType> selectedTasks = getSelectedTasks(target, selectedTask);
        if (selectedTasks == null) {
            return;
        }
        Task opTask = createSimpleTask(OPERATION_RESUME_TASKS);
        OperationResult result = opTask.getResult();
        try {
            List<TaskType> plainTasks = selectedTasks.stream().filter(dto -> !isManageableTreeRoot(dto)).collect(Collectors.toList());
            List<TaskType> trees = selectedTasks.stream().filter(dto -> isManageableTreeRoot(dto)).collect(Collectors.toList());
            getTaskService().resumeTasks(getOids(plainTasks), opTask, result);
            for (TaskType tree : trees) {
                getTaskService().resumeTaskTree(tree.getOid(), opTask, result);
            }
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("pageTasks.message.resumeTasksPerformed.success").getString());
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.resumeTasksPerformed.fatalError").getString(), e);
        }
        showResult(result);

        //refresh feedback and table
        refreshTable(TaskType.class, target);
    }

    private boolean suspendPlainTasks(List<TaskType> plainTasks, OperationResult result, Task opTask)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        if (!plainTasks.isEmpty()) {
            return getTaskService().suspendTasks(getOids(plainTasks), PageTasks.WAIT_FOR_TASK_STOP, opTask, result);
        } else {
            return true;
        }
    }

    private boolean suspendTrees(List<TaskType> roots, OperationResult result, Task opTask)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        boolean suspended = true;
        if (!roots.isEmpty()) {
            for (TaskType root : roots) {
                boolean s = getTaskService().suspendTaskTree(root.getOid(), PageTasks.WAIT_FOR_TASK_STOP, opTask, result);
                suspended = suspended && s;
            }
        }
        return suspended;
    }

    private List<TaskType> getSelectedTasks(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> selectedTask) {
        List<TaskType> selectedTasks = new ArrayList<>();
        if (selectedTask != null) {
            selectedTasks.add(selectedTask.getObject().getValue());
        } else {
            selectedTasks = getSelectedObjects();
        }

        if (selectedTasks.isEmpty()) {
            getSession().warn("PagetTasks.nothing.selected");
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
            getTaskService().scheduleTasksNow(getOids(selectedTasks), opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("pageTasks.message.scheduleTasksPerformed.success").getString());
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.scheduleTasksPerformed.fatalError").getString(), e);
        }
        showResult(result);

        //refresh feedback and table
        refreshTable(TaskType.class, target);

    }

    private void deleteTaskConfirmedPerformed(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> task) {
        List<TaskType> selectedTasks = getSelectedTasks(target, task);
        if (selectedTasks == null) {
            return;
        }

        Task opTask = createSimpleTask(OPERATION_DELETE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().suspendAndDeleteTasks(getOids(selectedTasks), WAIT_FOR_TASK_STOP, true, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        createStringResource("pageTasks.message.deleteTaskConfirmedPerformed.success").getString());
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.deleteTaskConfirmedPerformed.fatalError").getString(),
                    e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTable(TaskType.class, target);
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
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | SecurityViolationException
                | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(
                    createStringResource("pageTasks.message.reconcileWorkersConfirmedPerformed.fatalError").getString(), e);
        }
        showResult(result);

        refreshTable(TaskType.class, target);
    }

    private void suspendRootOnly(AjaxRequestTarget target, @NotNull IModel<SelectableBean<TaskType>> task) {
        Task opTask = createSimpleTask(OPERATION_SUSPEND_TASK);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().suspendTasks(Collections.singleton(task.getObject().getValue().getOid()), WAIT_FOR_TASK_STOP, opTask, result);
            // TODO check whether the suspension was complete
            result.computeStatus();
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.suspendRootOnly.fatalError").getString(), e);
        }
        showResult(result);

        refreshTable(TaskType.class, target);
    }

    private void resumeRootOnly(AjaxRequestTarget target, @NotNull IModel<SelectableBean<TaskType>> task) {
        Task opTask = createSimpleTask(OPERATION_RESUME_TASK);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().resumeTasks(Collections.singleton(task.getObject().getValue().getOid()), opTask, result);
            result.computeStatus();
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.resumeRootOnly.fatalError").getString(), e);
        }
        showResult(result);

        refreshTable(TaskType.class, target);
    }

    private void deleteWorkersAndWorkState(AjaxRequestTarget target, @NotNull IModel<SelectableBean<TaskType>> task) {
        Task opTask = createSimpleTask(OPERATION_DELETE_WORKERS_AND_WORK_STATE);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().deleteWorkersAndWorkState(task.getObject().getValue().getOid(), true, WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.deleteWorkersAndWorkState.fatalError").getString(),
                    e);
        }
        showResult(result);

        refreshTable(TaskType.class, target);
    }

    private void deleteWorkState(AjaxRequestTarget target, @NotNull IModel<SelectableBean<TaskType>> task) {
        Task opTask = createSimpleTask(OPERATION_DELETE_WORK_STATE);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().deleteWorkersAndWorkState(task.getObject().getValue().getOid(), false, WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.deleteWorkState.fatalError").getString(),
                    e);
        }
        showResult(result);

        refreshTable(TaskType.class, target);
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

        getTaskManager().switchToBackground(task, launchResult);
        launchResult.setBackgroundTaskOid(task.getOid());

        showResult(launchResult);
        target.add(getFeedbackPanel());
    }

    private IModel<String> getTaskConfirmationMessageModel(ColumnMenuAction action, String actionName) {
        if (action.getRowModel() == null) {
            return createStringResource("pageTasks.message.confirmationMessageForMultipleTaskObject", actionName, getSelectedObjects().size());
//                    WebComponentUtil.getSelectedData(()).size());
        } else {
            String objectName = ((SelectableBean<TaskType>) (action.getRowModel().getObject())).getValue().getName().getOrig();
            return createStringResource("pageTasks.message.confirmationMessageForSingleTaskObject", actionName, objectName);
        }

    }

    // must be static, otherwise JVM crashes (probably because of some wicket serialization issues)
    private static boolean isCoordinator(IModel<?> rowModel, boolean isHeader) {
        SelectableBean<TaskType> dto = getDto(rowModel, isHeader);
        return dto != null && TaskTypeUtil.isCoordinator(dto.getValue());
    }


    // must be static, otherwise JVM crashes (probably because of some wicket serialization issues)
    private static boolean isManageableTreeRoot(IModel<?> rowModel, boolean isHeader) {
        SelectableBean<TaskType> dto = getDto(rowModel, isHeader);
        return dto != null && isManageableTreeRoot(dto.getValue());
    }

    private static boolean isManageableTreeRoot(TaskType taskType) {
        return TaskTypeUtil.isCoordinator(taskType) || TaskTypeUtil.isPartitionedMaster(taskType);
    }

    private static SelectableBean<TaskType> getDto(IModel<?> rowModel, boolean isHeader) {
        if (rowModel != null && !isHeader) {
            Object object = rowModel.getObject();
            if (object instanceof SelectableBean) {
                return (SelectableBean<TaskType>) object;
            }
        }
        return null;
    }

    public static List<String> getOids(List<TaskType> taskDtoList) {
        List<String> retval = new ArrayList<>();
        for (TaskType taskDto : taskDtoList) {
            retval.add(taskDto.getOid());
        }
        return retval;
    }
}
