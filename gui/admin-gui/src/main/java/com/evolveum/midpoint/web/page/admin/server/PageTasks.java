/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyEnumValuesModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.input.StringChoiceRenderer;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshDto;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshPanel;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.page.admin.server.dto.*;
import com.evolveum.midpoint.web.session.TasksStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/tasks", matchUrlForSecurity = "/admin/tasks")
        },
        action = {
        @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_URL,
                label = "PageTasks.auth.tasks.label",
                description = "PageTasks.auth.tasks.description")})
public class PageTasks extends PageAdminTasks implements Refreshable {

    private static final Trace LOGGER = TraceManager.getTrace(PageTasks.class);
    private static final String DOT_CLASS = PageTasks.class.getName() + ".";
    private static final String OPERATION_DELETE_NODES = DOT_CLASS + "deleteNodes";
    private static final String OPERATION_START_SCHEDULERS = DOT_CLASS + "startSchedulers";
    private static final String OPERATION_STOP_SCHEDULERS_AND_TASKS = DOT_CLASS + "stopSchedulersAndTasks";
    private static final String OPERATION_STOP_SCHEDULERS = DOT_CLASS + "stopSchedulers";
    private static final String OPERATION_DEACTIVATE_SERVICE_THREADS = DOT_CLASS + "deactivateServiceThreads";
    private static final String OPERATION_REACTIVATE_SERVICE_THREADS = DOT_CLASS + "reactivateServiceThreads";
    private static final String OPERATION_SYNCHRONIZE_TASKS = DOT_CLASS + "synchronizeTasks";
    private static final String OPERATION_SYNCHRONIZE_WORKFLOW_REQUESTS = DOT_CLASS + "synchronizeWorkflowRequests";

    public static final long WAIT_FOR_TASK_STOP = 2000L;

    private static final String ID_REFRESH_PANEL = "refreshPanel";
    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_TASK_TABLE_PANEL = "taskTablePanel";
    private static final String ID_NODE_TABLE = "nodeTable";
    public static final String ID_SYNCHRONIZE_WORKFLOW_REQUESTS = "synchronizeWorkflowRequests";

    public static final String SELECTED_CATEGORY = "category";
    private static final int REFRESH_INTERVAL = 60000;                // don't set too low to prevent refreshing open inline menus (TODO skip refresh if a menu is open)

    private String searchText = "";

    private IModel<AutoRefreshDto> refreshModel;
    private AutoRefreshPanel refreshPanel;

    public PageTasks() {
        this("", null);
    }

    public PageTasks(String searchText) {
        this(searchText, null);
    }

    public PageTasks(PageParameters parameters) {
        this("", parameters);
    }

    // TODO clean the mess with constructors
    public PageTasks(String searchText, PageParameters parameters) {
        if (parameters != null) {
            getPageParameters().overwriteWith(parameters);
        } else {
            //reset task search storage. we want to use searchText instead
            TasksStorage storage = getSessionStorage().getTasks();
            storage.setTasksSearch(null);
        }

        this.searchText = searchText;
        refreshModel = new Model<>(new AutoRefreshDto(REFRESH_INTERVAL));

        initLayout();

        refreshPanel.startRefreshing(this, null);
    }

    private void initLayout() {

        refreshPanel = new AutoRefreshPanel(ID_REFRESH_PANEL, refreshModel, this, false);
        add(refreshPanel);

        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        TaskDtoTablePanel taskTable = new TaskDtoTablePanel(ID_TASK_TABLE_PANEL, searchText);
        taskTable.setOutputMarkupId(true);
        mainForm.add(taskTable);

        List<IColumn<NodeDto, String>> nodeColumns = initNodeColumns();
        BoxedTablePanel<NodeDto> nodeTable = new BoxedTablePanel<>(ID_NODE_TABLE, new NodeDtoProvider(PageTasks.this) {

            private static final long serialVersionUID = 1L;

            @Override
            public NodeDto createNodeDto(PrismObject<NodeType> node) {
                NodeDto dto = super.createNodeDto(node);
                addInlineMenuToNodeRow(dto);

                return dto;
            }
        }, nodeColumns,
                UserProfileStorage.TableId.PAGE_TASKS_NODES_PANEL,
                (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_TASKS_NODES_PANEL));
        nodeTable.setOutputMarkupId(true);
        nodeTable.setShowPaging(false);
        mainForm.add(nodeTable);

        initDiagnosticButtons();
    }

    @Override
    public void refresh(AjaxRequestTarget target) {
        refreshTasks(target);
    }

    @Override
    public Component getRefreshingBehaviorParent() {
        return refreshPanel;
    }

    @Override
    public int getRefreshInterval() {
        return REFRESH_INTERVAL;
    }

    private List<IColumn<NodeDto, String>> initNodeColumns() {
        List<IColumn<NodeDto, String>> columns = new ArrayList<>();

        IColumn<NodeDto, String> column = new CheckBoxHeaderColumn<>();
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("pageTasks.node.name"), NodeDto.F_NAME, NodeDto.F_NAME);
        columns.add(column);

        columns.add(new EnumPropertyColumn<NodeDto>(createStringResource("pageTasks.node.executionStatus"),
                NodeDto.F_EXECUTION_STATUS) {

            @Override
            protected String translate(Enum en) {
                return createStringResource(en).getString();
            }
        });

        columns.add(new PropertyColumn<>(createStringResource("pageTasks.node.contact"), NodeDto.F_CONTACT));
        columns.add(new AbstractColumn<NodeDto, String>(createStringResource("pageTasks.node.lastCheckInTime")) {

            @Override
            public void populateItem(Item<ICellPopulator<NodeDto>> item, String componentId,
                    final IModel<NodeDto> rowModel) {
                item.add(new Label(componentId, (IModel<Object>) () -> getLastCheckInTime(rowModel)));
            }
        });
        CheckBoxColumn<NodeDto> check = new CheckBoxColumn<>(createStringResource("pageTasks.node.clustered"), NodeDto.F_CLUSTERED);
        check.setEnabled(false);
        columns.add(check);
        columns.add(new PropertyColumn<>(createStringResource("pageTasks.node.statusMessage"), NodeDto.F_STATUS_MESSAGE));

        IColumn<NodeDto, String> menuColumn = new InlineMenuButtonColumn<>(createNodesInlineMenu(), PageTasks.this);
        columns.add(menuColumn);

        return columns;
    }

    private List<InlineMenuItem> createNodesInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.startScheduler")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<NodeDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            startSchedulersPerformed(target);
                        } else {
                            NodeDto rowDto = getRowModel().getObject();
                            startSchedulersPerformed(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_START_MENU_ITEM;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.startSchedulerAction").getString();
                return PageTasks.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.stopScheduler")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<NodeDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            stopSchedulersPerformed(target);
                        } else {
                            NodeDto rowDto = getRowModel().getObject();
                            stopSchedulersPerformed(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_STOP_MENU_ITEM;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.stopSchedulerAction").getString();
                return PageTasks.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageTasks.button.stopSchedulerAndTasks")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<NodeDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        stopSchedulersAndTasksPerformed(target, getRowModel() != null ? getRowModel().getObject() : null);
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.stopSchedulerTasksAction").getString();
                return PageTasks.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteNode")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<NodeDto>() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            deleteNodesPerformed(target);
                        } else {
                            NodeDto rowDto = getRowModel().getObject();
                            deleteNodesPerformed(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteAction").getString();
                return PageTasks.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        return items;
    }

    private String getLastCheckInTime(IModel<NodeDto> nodeModel) {
        NodeDto node = nodeModel.getObject();
        Long time = node.getLastCheckInTime();
        if (time == null || time == 0) {
            return "";
        }

        return createStringResource("pageTasks.message.getLastCheckInTime", DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - time, true, true)).getString();
    }

    private void initDiagnosticButtons() {
        AjaxButton deactivate = new AjaxButton("deactivateServiceThreads",
                createStringResource("pageTasks.button.deactivateServiceThreads")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deactivateServiceThreadsPerformed(target);
            }
        };
        add(deactivate);

        AjaxButton reactivate = new AjaxButton("reactivateServiceThreads",
                createStringResource("pageTasks.button.reactivateServiceThreads")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                reactivateServiceThreadsPerformed(target);
            }
        };
        add(reactivate);

        AjaxButton synchronize = new AjaxButton("synchronizeTasks",
                createStringResource("pageTasks.button.synchronizeTasks")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                synchronizeTasksPerformed(target);
            }
        };
        add(synchronize);

        AjaxButton synchronizeWorkflowRequests = new AjaxButton(ID_SYNCHRONIZE_WORKFLOW_REQUESTS,
                createStringResource("pageTasks.button.synchronizeWorkflowRequests")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                synchronizeWorkflowRequestsPerformed(target);
            }
        };
        // TODO this functionality will be needed in the future (to synchronize execution tasks with their cases)
        //  But it is not available now, so let's hide it.
        synchronizeWorkflowRequests.setVisible(false);
        add(synchronizeWorkflowRequests);

        AjaxButton refresh = new AjaxButton("refreshTasks",
                createStringResource("pageTasks.button.refreshTasks")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                refreshTasks(target);
            }
        };

        add(refresh);
    }

    private Table getTaskTable() {
        TaskDtoTablePanel tablePanel = (TaskDtoTablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TASK_TABLE_PANEL));
        return (Table) tablePanel.getTaskTable();
    }

    private Table getNodeTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_NODE_TABLE));
    }

    private boolean isSomeNodeSelected(List<NodeDto> nodes, AjaxRequestTarget target) {
        if (!nodes.isEmpty()) {
            return true;
        }

        warn(getString("pageTasks.message.noNodeSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    // region Node-level actions
    private void stopSchedulersAndTasksPerformed(AjaxRequestTarget target, List<String> identifiers) {
        Task opTask = createSimpleTask(OPERATION_STOP_SCHEDULERS_AND_TASKS);
        OperationResult result = opTask.getResult();
        try {
            boolean suspended = getTaskService().stopSchedulersAndTasks(identifiers, WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (suspended) {
                    result.recordStatus(OperationResultStatus.SUCCESS,
                            createStringResource("pageTasks.message.stopSchedulersAndTasksPerformed.success").getString());
                } else {
                    result.recordWarning(
                            createStringResource("pageTasks.message.stopSchedulersAndTasksPerformed.warning").getString());
                }
            }
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(
                    createStringResource("pageTasks.message.stopSchedulersAndTasksPerformed.fatalError").getString(), e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTables(target);
    }

    private void stopSchedulersAndTasksPerformed(AjaxRequestTarget target, NodeDto dto) {
        List<NodeDto> nodeDtoList = new ArrayList<>();
        if (dto != null) {
            nodeDtoList.add(dto);
        } else {
            nodeDtoList.addAll(WebComponentUtil.getSelectedData(getNodeTable()));
        }
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        stopSchedulersAndTasksPerformed(target, NodeDto.getNodeIdentifiers(nodeDtoList));
    }

    private void startSchedulersPerformed(AjaxRequestTarget target, List<String> identifiers) {
        Task opTask = createSimpleTask(OPERATION_START_SCHEDULERS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().startSchedulers(identifiers, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        createStringResource("pageTasks.message.startSchedulersPerformed.success").getString());
            }
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.startSchedulersPerformed.fatalError").getString(), e);
        }

        showResult(result);

        // refresh feedback and table
        refreshTables(target);
    }

    private void startSchedulersPerformed(AjaxRequestTarget target, NodeDto dto) {
        startSchedulersPerformed(target, Collections.singletonList(dto.getNodeIdentifier()));
    }

    private void startSchedulersPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebComponentUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        startSchedulersPerformed(target, NodeDto.getNodeIdentifiers(nodeDtoList));
    }

    private void stopSchedulersPerformed(AjaxRequestTarget target, List<String> identifiers) {
        Task opTask = createSimpleTask(OPERATION_STOP_SCHEDULERS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().stopSchedulers(identifiers, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        createStringResource("pageTasks.message.stopSchedulersPerformed.success").getString());
            }
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.stopSchedulersPerformed.fatalError").getString(), e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTables(target);
    }

    private void stopSchedulersPerformed(AjaxRequestTarget target, NodeDto dto) {
        stopSchedulersPerformed(target, Collections.singletonList(dto.getNodeIdentifier()));
    }

    private void stopSchedulersPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebComponentUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        stopSchedulersPerformed(target, NodeDto.getNodeIdentifiers(nodeDtoList));
    }

    private void deleteNodesPerformed(AjaxRequestTarget target, List<NodeDto> nodes) {
        OperationResult result = new OperationResult(OPERATION_DELETE_NODES);

        Task task = createSimpleTask(OPERATION_DELETE_NODES);

        for (NodeDto nodeDto : nodes) {
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
            deltas.add(getPrismContext().deltaFactory().object().createDeleteDelta(NodeType.class, nodeDto.getOid()));
            try {
                getModelService().executeChanges(deltas, null, task, result);
            } catch (Exception e) { // until java 7 we do it in this way
                result.recordFatalError(createStringResource("pageTasks.message.deleteNodesPerformed.fatalError").getString()
                        + nodeDto.getNodeIdentifier(), e);
            }
        }

        result.computeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS,
                    createStringResource("pageTasks.message.deleteNodesPerformed.success").getString());
        }
        showResult(result);

        NodeDtoProvider provider = (NodeDtoProvider) getNodeTable().getDataTable().getDataProvider();
        provider.clearCache();

        // refresh feedback and table
        refreshTables(target);
    }

    private void deleteNodesPerformed(AjaxRequestTarget target, NodeDto dto) {
        deleteNodesPerformed(target, Collections.singletonList(dto));
    }

    private void deleteNodesPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebComponentUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        deleteNodesPerformed(target, nodeDtoList);
    }
    // endregion

    // region Diagnostics actions
    private void deactivateServiceThreadsPerformed(AjaxRequestTarget target) {
        Task opTask = createSimpleTask(OPERATION_DEACTIVATE_SERVICE_THREADS);
        OperationResult result = opTask.getResult();

        try {
            boolean stopped = getTaskService().deactivateServiceThreads(WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (stopped) {
                    result.recordStatus(OperationResultStatus.SUCCESS,
                            createStringResource("pageTasks.message.deactivateServiceThreadsPerformed.success").getString());
                } else {
                    result.recordWarning(
                            createStringResource("pageTasks.message.deactivateServiceThreadsPerformed.warning").getString());
                }
            }
        } catch (RuntimeException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(
                    createStringResource("pageTasks.message.deactivateServiceThreadsPerformed.fatalError").getString(), e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTables(target);
    }

    private void reactivateServiceThreadsPerformed(AjaxRequestTarget target) {
        Task opTask = createSimpleTask(OPERATION_REACTIVATE_SERVICE_THREADS);
        OperationResult result = opTask.getResult();

        try {
            getTaskService().reactivateServiceThreads(opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        createStringResource("pageTasks.message.reactivateServiceThreadsPerformed.success").getString());
            }
        } catch (RuntimeException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(
                    createStringResource("pageTasks.message.reactivateServiceThreadsPerformed.fatalError").getString(), e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTables(target);
    }

    private void refreshTables(AjaxRequestTarget target) {
        clearTablesCache();
        target.add(getFeedbackPanel());
        target.add((Component) getTaskTable());
        target.add((Component) getNodeTable());
    }

    private void clearTablesCache() {
        if (getTaskTable() != null && getTaskTable().getDataTable() != null) {
            WebComponentUtil.clearProviderCache(getTaskTable().getDataTable().getDataProvider());
        }
        if (getNodeTable() != null && getNodeTable().getDataTable() != null) {
            WebComponentUtil.clearProviderCache(getNodeTable().getDataTable().getDataProvider());
        }
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
        refreshTables(target);
    }

    private void synchronizeWorkflowRequestsPerformed(AjaxRequestTarget target) {
        Task opTask = createSimpleTask(OPERATION_SYNCHRONIZE_WORKFLOW_REQUESTS);
        OperationResult result = opTask.getResult();

        try {
//            getTaskService().synchronizeWorkflowRequests(opTask, result);
            result.computeStatusIfUnknown();
            if (result.isSuccess()) { // brutal hack - the subresult's message
                                        // contains statistics
                result.recordStatus(OperationResultStatus.SUCCESS, result.getLastSubresult().getMessage());
            }
        } catch (RuntimeException  e) {
            result.recordFatalError(createStringResource("pageTasks.message.synchronizeTasksPerformed.fatalError").getString(),
                    e);
        }
        showResult(result);

        // refresh feedback and table
        refreshTables(target);
    }
    // endregion

    private void refreshTasks(AjaxRequestTarget target) {
        // searchModel = new LoadableModel<TasksSearchDto>(false) {
        //
        // @Override
        // protected TasksSearchDto load() {
        // return loadTasksSearchDto();
        // }
        // };

        target.add(refreshPanel);

        // refresh feedback and table
        refreshTables(target);

        if (refreshModel.getObject().isEnabled()) {
            refreshPanel.startRefreshing(this, target);
        }
    }

    private void addInlineMenuToNodeRow(final NodeDto dto) {
        List<InlineMenuItem> items = dto.getMenuItems();
        if (!items.isEmpty()) {
            // menu already added
            return;
        }

        items.addAll(createNodesInlineMenu());
    }

    private IModel<String> getNodeConfirmationMessageModel(ColumnMenuAction action, String actionName) {
        if (action.getRowModel() == null) {
            return createStringResource("pageTasks.message.confirmationMessageForMultipleNodeObject", actionName,
                    WebComponentUtil.getSelectedData(getNodeTable()).size());
        } else {
            String objectName = ((NodeDto) (action.getRowModel().getObject())).getName();
            return createStringResource("pageTasks.message.confirmationMessageForSingleNodeObject", actionName, objectName);
        }

    }

    private boolean isNodeShowConfirmationDialog(ColumnMenuAction action) {
        return action.getRowModel() != null || WebComponentUtil.getSelectedData(getNodeTable()).size() > 0;
    }
}
