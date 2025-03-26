/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconCssStyle;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskInformationUtil;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
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
    public static final String OPERATION_DELETE_ACTIVITY_STATE_AND_WORKERS = DOT_CLASS + "deleteActivityStateAndWorkers";
    public static final String OPERATION_DELETE_WORK_STATE = DOT_CLASS + "deleteWorkState";
    public static final String OPERATION_DELETE_ALL_CLOSED_TASKS = DOT_CLASS + "deleteAllClosedTasks";
    public static final String OPERATION_SCHEDULE_TASKS = DOT_CLASS + "scheduleTasks";
    private static final String OPERATION_SYNCHRONIZE_TASKS = DOT_CLASS + "synchronizeTasks";

    public static final long WAIT_FOR_TASK_STOP = 2000L;

    public TaskTablePanel(String id) {
        super(id, TaskType.class);
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
        synchronize.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
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

    protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
        if (collectionView == null) {
            collectionView = getObjectCollectionView();
        }
        try {
            List<ObjectReferenceType> referenceList = new ArrayList<>();
            if (getNewObjectReferencesList(collectionView, relation) != null) {
                referenceList.addAll(getNewObjectReferencesList(collectionView, relation));
            }
            TaskOperationUtils.addArchetypeReferencesList(referenceList);
            DetailsPageUtil.initNewObjectWithReference(getPageBase(),
                    relation != null && CollectionUtils.isNotEmpty(relation.getObjectTypes()) ?
                            relation.getObjectTypes().get(0) : WebComponentUtil.classToQName(getPrismContext(), getType()),
                    referenceList);
        } catch (SchemaException ex) {
            getPageBase().getFeedbackMessages().error(TaskTablePanel.this, ex.getUserFriendlyMessage());
            target.add(getPageBase().getFeedbackPanel());
        }
    }

    @Override
    protected boolean isCollectionViewWithoutMorePossibleNewType(CompiledObjectCollectionView collectionView) {
        if (isViewForObjectCollectionType(collectionView, "00000000-0000-0000-0002-000000000007", ObjectCollectionType.COMPLEX_TYPE)
                || isViewForObjectCollectionType(collectionView, SystemObjectsType.ARCHETYPE_UTILITY_TASK.value(), ArchetypeType.COMPLEX_TYPE)
                || isViewForObjectCollectionType(collectionView, SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value(), ArchetypeType.COMPLEX_TYPE)
                || isViewForObjectCollectionType(collectionView, "00000000-0000-0000-0002-000000000008", ObjectCollectionType.COMPLEX_TYPE)) {
            return false;
        }
        return true;
    }

    @Override
    protected @NotNull List<CompiledObjectCollectionView> getNewObjectInfluencesList() {
        //HACK TODO clenup and think about generic mechanism for this
        CompiledObjectCollectionView objectCollectionView = getObjectCollectionView();

        if (isViewForObjectCollectionType(objectCollectionView, "00000000-0000-0000-0002-000000000007", ObjectCollectionType.COMPLEX_TYPE)) {
            return getNewTaskInfluencesList(TaskOperationUtils.getReportArchetypesList());
        }

        if (isViewForObjectCollectionType(objectCollectionView, "00000000-0000-0000-0002-000000000008", ObjectCollectionType.COMPLEX_TYPE)) {
            return getNewTaskInfluencesList(TaskOperationUtils.getCertificationArchetypesList());
        }

        if (isViewForObjectCollectionType(objectCollectionView, SystemObjectsType.ARCHETYPE_UTILITY_TASK.value(), ArchetypeType.COMPLEX_TYPE)) {
            return getNewTaskInfluencesList(TaskOperationUtils.getUtilityArchetypesList());
        }

        if (isViewForObjectCollectionType(objectCollectionView, SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value(), ArchetypeType.COMPLEX_TYPE)) {
            return getNewTaskInfluencesList(TaskOperationUtils.getSystemArchetypesList());
        }
        return super.getNewObjectInfluencesList();
    }

    protected List<CompiledObjectCollectionView> getAllApplicableArchetypeViews() {
        return TaskOperationUtils.getAllApplicableArchetypeForNewTask(getPageBase());
    }

    private List<CompiledObjectCollectionView> getNewTaskInfluencesList(List<String> oids) {
        List<CompiledObjectCollectionView> compiledObjectCollectionViews = getPageBase().getCompiledGuiProfile().findAllApplicableArchetypeViews(TaskType.COMPLEX_TYPE, OperationTypeType.ADD);
        List<CompiledObjectCollectionView> filteredObjectCollectionViews = new ArrayList<>();
        for (CompiledObjectCollectionView compiledObjectCollectionView : compiledObjectCollectionViews) {
            for (String oid : oids) {
                if (isViewForObjectCollectionType(compiledObjectCollectionView, oid, ArchetypeType.COMPLEX_TYPE)) {
                    filteredObjectCollectionViews.add(compiledObjectCollectionView);
                }
            }
        }
        return filteredObjectCollectionViews;
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
            result.recordFatalError(
                    createStringResource("pageTasks.message.synchronizeTasksPerformed.fatalError").getString(), e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTable(target);
        target.add(getTable());
        clearCache();
    }

    private List<IColumn<SelectableBean<TaskType>, String>> initTaskColumns() {
        List<IColumn<SelectableBean<TaskType>, String>> columns = new ArrayList<>();
        columns.addAll(initCustomTaskColumns());

        return columns;
    }

    protected List<IColumn<SelectableBean<TaskType>, String>> initCustomTaskColumns() {
        List<IColumn<SelectableBean<TaskType>, String>> columns = new ArrayList<>();

        columns.add(createTaskExecutionStateColumn());
        columns.add(createStatusColumn());

        return columns;
    }

    private LoadableDetachableModel<TaskExecutionProgress> createTaskExecutionProgressModel(SelectableBean<TaskType> bean) {
        return new LoadableDetachableModel<>() {

            @Override
            protected TaskExecutionProgress load() {
                return createTaskExecutionProgress(bean);
            }
        };
    }

    private AbstractExportableColumn<SelectableBean<TaskType>, String> createStatusColumn() {
        return new AbstractExportableColumn<>(
                createStringResource("pageTasks.task.status"),
                TaskType.F_RESULT_STATUS.getLocalPart()) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                return createTaskExecutionProgressModel(rowModel.getObject());
            }

            @Override
            protected Component createDisplayComponent(String componentId, IModel<?> dataModel) {
                return new TaskProgressPanel(componentId, (IModel<TaskExecutionProgress>) dataModel);
            }
        };
    }

    private TaskExecutionProgress createTaskExecutionProgress(SelectableBean<TaskType> bean) {
        TaskInformation info = getAttachedTaskInformation(bean);

        return TaskExecutionProgress.fromTaskInformation(info, getPageBase());
    }

    private AbstractExportableColumn<SelectableBean<TaskType>, String> createTaskExecutionStateColumn() {
        return new AbstractExportableColumn<>(
                createStringResource("pageTasks.task.execution")) {

            @Override
            public IModel<TaskExecutionProgress> getDataModel(IModel<SelectableBean<TaskType>> rowModel) {
                return createTaskExecutionProgressModel(rowModel.getObject());
            }

            @Override
            protected Component createDisplayComponent(String componentId, IModel<?> dataModel) {
                return new TaskExecutionPanel(componentId, (IModel<TaskExecutionProgress>) dataModel);
            }
        };
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

        ButtonInlineMenuItem buttonInlineMenuItem = new ButtonInlineMenuItem(createStringResource(buttonNameKey)) {
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
                String normalizedSizeIcon = icon + " fa-fw";
                CompositedIconBuilder builder = getDefaultCompositedIconBuilder(normalizedSizeIcon);
                if (GuiStyleConstants.CLASS_SUSPEND_MENU_ITEM.equals(icon) || GuiStyleConstants.CLASS_START_MENU_ITEM.equals(icon)) {
                    builder.appendLayerIcon(GuiStyleConstants.CLASS_OBJECT_TASK_ICON + " fa-fw", CompositedIconCssStyle.BOTTOM_RIGHT_STYLE);
                }
                return builder;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource(confirmationMessageKey).getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

        };
        buttonInlineMenuItem.setVisibilityChecker((rowModel, header) -> checkVisibility(rowModel, header, visibilityHandler));
        return buttonInlineMenuItem;
    }

    private boolean checkVisibility(IModel<?> rowModel, boolean header, SerializableFunction<TaskType, Boolean> visibilityHandler) {
        if (header) {
            return true;
        }
        TaskType taskType = null;
        if (isTaskModel(rowModel)) {
            SelectableBean<TaskType> modelObject = (SelectableBean<TaskType>) rowModel.getObject();
            if (modelObject == null) {
                return true;
            }
            taskType = modelObject.getValue();
        }

        boolean visible = visibilityHandler.apply(taskType);
        return visible;
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
                this::deleteActivityStateAndWorkers,
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
                OPERATION_DELETE_ALL_CLOSED_TASKS,  //this is more hack than a perfect solution
                (task) -> false,
                true);
    }

    private InlineMenuItem createTaskInlineMenuItem(String menuNameKey,
            SerializableBiConsumer<AjaxRequestTarget, IModel<SelectableBean<TaskType>>> action,
            String confirmationMessageKey,
            SerializableFunction<TaskType, Boolean> visibilityHandler,
            boolean header) {
        InlineMenuItem menuItem = new InlineMenuItem(createStringResource(menuNameKey)) {
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
                if (OPERATION_DELETE_ALL_CLOSED_TASKS.equals(confirmationMessageKey)) {
                    return createStringResource("pageTasks.message.deleteAllClosedTasksConfirm");
                }
                String actionName = createStringResource(confirmationMessageKey).getString();
                return TaskTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return header;
            }

        };

        menuItem.setVisibilityChecker((rowModel, menuHeader) -> isMenuVisible(rowModel, menuHeader, visibilityHandler));
        return menuItem;
    }

    private boolean isMenuVisible(IModel<?> rowModel, boolean header, SerializableFunction<TaskType, Boolean> visibilityHandler) {
        if (header) {
            return true;
        }

        if (!isTaskModel(rowModel)) {
            return true;
        }

        TaskType modelObject = getTask((IModel<SelectableBean<TaskType>>) rowModel, header);
        if (modelObject == null) {
            return true;
        }
        return visibilityHandler.apply(modelObject);

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

    private void deleteActivityStateAndWorkers(AjaxRequestTarget target, @NotNull IModel<SelectableBean<TaskType>> task) {
        Task opTask = createSimpleTask(OPERATION_DELETE_ACTIVITY_STATE_AND_WORKERS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().deleteActivityStateAndWorkers(task.getObject().getValue().getOid(), true, WAIT_FOR_TASK_STOP, opTask, result);
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
            getTaskService().deleteActivityStateAndWorkers(task.getOid(), false, WAIT_FOR_TASK_STOP, opTask, result);
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
        getPageBase().taskAwareExecutor(target, OPERATION_DELETE_ALL_CLOSED_TASKS)
                .runVoid((task, result) -> {
                    var activityDefinition =
                            ActivityDefinitionBuilder.create(new CleanupWorkDefinitionType()
                                            .policies(new CleanupPoliciesType()
                                                    .closedTasks(new CleanupPolicyType()
                                                            .maxAge(XmlTypeConverter.createDuration(0)))))
                                    .build();
                    getPageBase().getModelInteractionService().submit(
                            activityDefinition,
                            ActivitySubmissionOptions.create()
                                    .withTaskTemplate(new TaskType()
                                            .name("Closed tasks cleanup")),
                            task, result);
                });
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

        // TODO What if the task has delegated distributed activity?
        return task != null && ActivityStateUtil.hasLocalDistributedActivity(task);
    }

    // must be static, otherwise JVM crashes (probably because of some wicket serialization issues)
    @SuppressWarnings("unchecked")
    private static boolean isManageableTreeRoot(IModel<?> rowModel, boolean isHeader) {
        if (!isTaskModel(rowModel)) {
            return false;
        }
        TaskType task = getTask((IModel<SelectableBean<TaskType>>) rowModel, isHeader);
        return task != null && ActivityStateUtil.isManageableTreeRoot(task);
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

    /** Creates {@link TaskInformationUtil} based on current table row and (in subclasses) the whole activity tree overview. */
    @NotNull
    protected TaskInformation getAttachedTaskInformation(SelectableBean<TaskType> selectableTaskBean) {
        return TaskInformationUtil.getOrCreateInfo(selectableTaskBean, null);
    }
}
