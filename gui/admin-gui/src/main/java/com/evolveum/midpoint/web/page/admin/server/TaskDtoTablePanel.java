/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.util.SerializableSupplier;
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
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyEnumValuesModel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.input.StringChoiceRenderer;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatusFilter;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.server.dto.TasksSearchDto;
import com.evolveum.midpoint.web.session.TasksStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author mederly
 * @author skublik
 */
public class TaskDtoTablePanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(TaskDtoTablePanel.class);
    private static final String DOT_CLASS = TaskDtoTablePanel.class.getName() + ".";
    private static final String OPERATION_SUSPEND_TASKS = DOT_CLASS + "suspendTasks";
    private static final String OPERATION_SUSPEND_TASK = DOT_CLASS + "suspendTask";
    private static final String OPERATION_RESUME_TASKS = DOT_CLASS + "resumeTasks";
    private static final String OPERATION_RESUME_TASK = DOT_CLASS + "resumeTask";
    private static final String OPERATION_DELETE_TASKS = DOT_CLASS + "deleteTasks";
    private static final String OPERATION_RECONCILE_WORKERS = DOT_CLASS + "reconcileWorkers";
    private static final String OPERATION_DELETE_WORKERS_AND_WORK_STATE = DOT_CLASS + "deleteWorkersAndWorkState";
    private static final String OPERATION_DELETE_WORK_STATE = DOT_CLASS + "deleteWorkState";
    private static final String OPERATION_DELETE_ALL_CLOSED_TASKS = DOT_CLASS + "deleteAllClosedTasks";
    private static final String OPERATION_SCHEDULE_TASKS = DOT_CLASS + "scheduleTasks";
    private static final String ALL_CATEGORIES = "";

    public static final long WAIT_FOR_TASK_STOP = 2000L;

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_STATE = "state";
    private static final String ID_CATEGORY = "category";
    private static final String ID_SHOW_PROGRESS_LABEL = "showProgressLabel";
    private static final String ID_SHOW_PROGRESS = "showProgress";
    private static final String ID_SHOW_SUBTASKS_LABEL = "showSubtasksLabel";
    private static final String ID_SHOW_SUBTASKS = "showSubtasks";
    private static final String ID_TASK_TABLE = "taskTable";
    private static final String ID_SEARCH_CLEAR = "searchClear";
    private static final String ID_TABLE_HEADER = "tableHeader";
    public static final String ID_SYNCHRONIZE_WORKFLOW_REQUESTS = "synchronizeWorkflowRequests";

    public static final String SELECTED_CATEGORY = "category";

    private IModel<TasksSearchDto> searchModel;
    private String searchText = "";

    public TaskDtoTablePanel(String id) {
        this(id, "");
    }

    public TaskDtoTablePanel(String id, String searchText) {
        super(id);
        this.searchText = searchText;
        searchModel = LoadableModel.create(this::loadTasksSearchDto, false);
    }

    private TasksSearchDto loadTasksSearchDto() {
        TasksStorage storage = getTaskStorage();
        TasksSearchDto dto = storage.getTasksSearch();

        if (dto == null) {
            dto = new TasksSearchDto();
            dto.setShowSubtasks(defaultShowSubtasks());
        }

        if (getPageBase().getPageParameters() != null) {
            StringValue category = getPageBase().getPageParameters().get(SELECTED_CATEGORY);
            if (category != null && category.toString() != null && !category.toString().isEmpty()) {
                dto.setCategory(category.toString());
            }
        }

        if (dto.getStatus() == null) {
            dto.setStatus(TaskDtoExecutionStatusFilter.ALL);
        }

        return dto;
    }

    protected boolean defaultShowSubtasks() {
        return false;
    }

    protected TasksStorage getTaskStorage() {
        return getPageBase().getSessionStorage().getTasks();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();

        ISortableDataProvider provider = createDataProvider();
        BoxedTablePanel<TaskDto> taskTable = new BoxedTablePanel<TaskDto>(ID_TASK_TABLE, provider, taskColumns,
                UserProfileStorage.TableId.PAGE_TASKS_PANEL,
                (int) getPageBase().getItemsPerPage(UserProfileStorage.TableId.PAGE_TASKS_PANEL)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new SearchFragment(headerId, ID_TABLE_HEADER, TaskDtoTablePanel.this, searchModel, TaskDtoTablePanel.this);
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                CsvDownloadButtonPanel exportDataLink = new CsvDownloadButtonPanel(id) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected DataTable<?, ?> getDataTable() {
                        return getTaskTable().getDataTable();
                    }

                    @Override
                    protected String getFilename() {
                        return "TaskType_" + createStringResource("MainObjectListPanel.exportFileName").getString();
                    }

                };
                return exportDataLink;
            }
        };
        taskTable.setOutputMarkupId(true);

        TasksStorage storage = getTaskStorage();
        taskTable.setCurrentPage(storage.getPaging());

        add(taskTable);
    }

    Table getTaskTable() {
        return (Table) get(createComponentPath(ID_TASK_TABLE));
    }

    private String createObjectRef(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();
        if (task.getObjectRef() == null) {
            return "";
        }
        if (StringUtils.isNotEmpty(task.getObjectRefName())) {
            return task.getObjectRefName();
        } else {
            return task.getObjectRef().getOid();
        }
    }

    protected List<IColumn<TaskDto, String>> initCustomTaskColumns() {
        List<IColumn<TaskDto, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<TaskDto>(createStringResource("")) {
            @Override
            protected DisplayType getIconDisplayType(IModel<TaskDto> rowModel) {
                ObjectReferenceType ref = rowModel.getObject().getObjectRef();
                if (ref == null || ref.getType() == null) {
                    return WebComponentUtil.createDisplayType("");
                }
                ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(ref.getType());
                String icon = guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
                return WebComponentUtil.createDisplayType(icon);
            }

            private ObjectTypeGuiDescriptor getObjectTypeDescriptor(QName type) {
                return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(type));
            }

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId, IModel<TaskDto> rowModel) {
                super.populateItem(item, componentId, rowModel);
                ObjectReferenceType ref = rowModel.getObject().getObjectRef();
                if (ref != null && ref.getType() != null) {
                    ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(ref.getType());
                    if (guiDescriptor != null) {
                        item.add(AttributeModifier.replace("title", createStringResource(guiDescriptor.getLocalizationKey())));
                        item.add(new TooltipBehavior());
                    }
                }
            }
        });

        columns.add(new AbstractExportableColumn<TaskDto, String>(createStringResource("pageTasks.task.objectRef")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                    final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId, new IModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createObjectRef(rowModel);
                    }
                }));
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
                return Model.of(createObjectRef(rowModel));
            }

        });
        columns.add(createTaskExecutionStatusColumn(this, "pageTasks.task.execution"));
        columns.add(new PropertyColumn<>(createStringResource("pageTasks.task.executingAt"), "executingAt"));
        columns.add(createProgressColumn(getPageBase(), "pageTasks.task.progress", this::isProgressComputationEnabled));
        columns.add(new AbstractExportableColumn<TaskDto, String>(createStringResource("pageTasks.task.currentRunTime")) {

            @Override
            public void populateItem(final Item<ICellPopulator<TaskDto>> item, final String componentId,
                    final IModel<TaskDto> rowModel) {

                DateLabelComponent dateLabel = new DateLabelComponent(componentId, new IModel<Date>() {

                    @Override
                    public Date getObject() {
                        Date date = getCurrentRuntime(rowModel);
                        TaskDto task = rowModel.getObject();
                        if (task.getRawExecutionStatus() == TaskExecutionStatus.CLOSED && date != null) {
                            ((DateLabelComponent) item.get(componentId)).setBefore(createStringResource("pageTasks.task.closedAt").getString() + " ");
                        } else if (date != null) {
                            ((DateLabelComponent) item.get(componentId))
                                    .setBefore(WebComponentUtil.formatDurationWordsForLocal(date.getTime(), true, true, getPageBase()));
                        }
                        return date;
                    }
                }, WebComponentUtil.getShortDateTimeFormat(getPageBase()));
                item.add(dateLabel);
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
                TaskDto task = rowModel.getObject();
                Date date = getCurrentRuntime(rowModel);
                String displayValue = "";
                if (date != null) {
                    if (task.getRawExecutionStatus() == TaskExecutionStatus.CLOSED) {
                        displayValue =
                                createStringResource("pageTasks.task.closedAt").getString() +
                                        WebComponentUtil.getShortDateTimeFormattedValue(date, getPageBase());
                    } else {
                        displayValue = WebComponentUtil.formatDurationWordsForLocal(date.getTime(), true, true, getPageBase());
                    }
                }
                return Model.of(displayValue);
            }
        });
        columns.add(new AbstractExportableColumn<TaskDto, String>(createStringResource("pageTasks.task.scheduledToRunAgain")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                    final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId, new IModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createScheduledToRunAgain(rowModel);
                    }
                }));
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
                return Model.of(createScheduledToRunAgain(rowModel));
            }
        });

        columns.add(new IconColumn<TaskDto>(createStringResource("pageTasks.task.status")) {

            @Override
            protected DisplayType getIconDisplayType(final IModel<TaskDto> rowModel) {
                String icon = "";
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getStatus() != null) {
                    icon = OperationResultStatusPresentationProperties
                            .parseOperationalResultStatus(rowModel.getObject().getStatus().createStatusType()).getIcon()
                            + " fa-lg";
                } else {
                    icon = OperationResultStatusPresentationProperties.UNKNOWN.getIcon() + " fa-lg";
                }

                String title = "";
                TaskDto dto = rowModel.getObject();

                if (dto != null && dto.getStatus() != null) {
                    title = PageBase.createStringResourceStatic(TaskDtoTablePanel.this, dto.getStatus()).getString();
                } else {
                    title = PageBase.createStringResourceStatic(TaskDtoTablePanel.this, OperationResultStatus.UNKNOWN).getString();
                }
                return WebComponentUtil.createDisplayType(icon, "", title);
            }
        });

        return columns;
    }

    private List<IColumn<TaskDto, String>> initTaskColumns() {
        List<IColumn<TaskDto, String>> columns = new ArrayList<>();

        IColumn column = new CheckBoxHeaderColumn<TaskDto>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<TaskDto> rowModel, IModel<Boolean> selectedModel) {
                TaskDtoProvider taskTableProvider = (TaskDtoProvider) table.getDataProvider();
                List<TaskDto> objects = taskTableProvider.getAvailableData();
                if (objects == null || objects.isEmpty()) {
                    return;
                }
                objects.forEach(taskDto -> {
                    if (taskDto.getOid().equals(rowModel.getObject().getOid())) {
                        boolean selected = rowModel.getObject().isSelected();
                        taskDto.setSelected(selected);
                    }
                });
                super.onUpdateRow(target, table, rowModel, selectedModel);
            }
        };
        columns.add(column);

        column = createTaskNameColumn(this, "pageTasks.task.name");
        columns.add(column);

        columns.add(createTaskCategoryColumn(this, "pageTasks.task.category"));

        columns.addAll(initCustomTaskColumns());

        IColumn<TaskDto, String> menuColumn = new InlineMenuButtonColumn<>(createTasksInlineMenu(true, null),
                getPageBase());
        columns.add(menuColumn);

        return columns;
    }

    private String createScheduledToRunAgain(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();
        boolean runnable = task.getRawExecutionStatus() == TaskExecutionStatus.RUNNABLE;
        Long scheduledAfter = task.getScheduledToStartAgain();
        Long retryAfter = runnable ? task.getRetryAfter() : null;

        if (scheduledAfter == null) {
            if (retryAfter == null || retryAfter <= 0) {
                return "";
            }
        } else if (scheduledAfter == TaskDto.NOW) { // TODO what about retryTime?
            return getString(runnable ? "pageTasks.now" : "pageTasks.nowForNotRunningTasks");
        } else if (scheduledAfter == TaskDto.RUNS_CONTINUALLY) {    // retryTime is probably null here
            return getString("pageTasks.runsContinually");
        } else if (scheduledAfter == TaskDto.ALREADY_PASSED && retryAfter == null) {
            return getString(runnable ? "pageTasks.alreadyPassed" : "pageTasks.alreadyPassedForNotRunningTasks");
        }

        long displayTime;
        boolean displayAsRetry;
        if (retryAfter != null && retryAfter > 0 && (scheduledAfter == null || scheduledAfter < 0
                || retryAfter < scheduledAfter)) {
            displayTime = retryAfter;
            displayAsRetry = true;
        } else {
            displayTime = scheduledAfter;
            displayAsRetry = false;
        }

        String key;
        if (runnable) {
            key = displayAsRetry ? "pageTasks.retryIn" : "pageTasks.in";
        } else {
            key = "pageTasks.inForNotRunningTasks";
        }

        return PageBase.createStringResourceStatic(this, key, DurationFormatUtils.formatDurationWords(displayTime, true, true))
                .getString();
    }

    private Date getCurrentRuntime(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();

        if (task.getRawExecutionStatus() == TaskExecutionStatus.CLOSED) {

            Long time = task.getCompletionTimestamp();
            if (time == null) {
                return null;
            }
            return new Date(time);

        }
        return null;
    }

    private void addInlineMenuToTaskRow(TaskDto dto) {
        addInlineMenuToTaskDto(dto);

        List<TaskDto> list = new ArrayList<>();
        if (dto.getSubtasks() != null) {
            list.addAll(dto.getTransientSubtasks());
        }
        if (dto.getTransientSubtasks() != null) {
            list.addAll(dto.getSubtasks());
        }

        for (TaskDto task : list) {
            addInlineMenuToTaskDto(task);
        }
    }

    private void addInlineMenuToTaskDto(final TaskDto dto) {
        List<InlineMenuItem> items = dto.getMenuItems();
        if (!items.isEmpty()) {
            // menu was already added
            return;
        }

        items.addAll(createTasksInlineMenu(false, dto));
    }

    protected ISortableDataProvider createDataProvider() {
        TaskDtoProviderOptions options = TaskDtoProviderOptions.minimalOptions();
        options.setGetNextRunStartTime(true);
        options.setUseClusterInformation(true);
        options.setResolveObjectRef(true);

        TaskDtoProvider provider = new TaskDtoProvider(getPageBase(), options) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                TasksStorage storage = getTaskStorage();
                storage.setPaging(paging);
            }

            @Override
            public TaskDto createTaskDto(PrismObject<TaskType> task, boolean subtasksLoaded, Task opTask, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
                TaskDto dto = super.createTaskDto(task, subtasksLoaded, opTask, result);
                addInlineMenuToTaskRow(dto);

                return dto;
            }

            @Override
            public IModel<TaskDto> model(TaskDto object) {
                return new LoadableDetachableModel<TaskDto>(object) {

                    private static final long serialVersionUID = 1L;
                    private String oid;

                    protected void onDetach() {
                        this.oid = getObject().getOid();
                    }

                    @Override
                    protected TaskDto load() {
                        Task task = getPageBase().createSimpleTask("load task");
                        OperationResult result = task.getResult();
                        PrismObject<TaskType> taskType = WebModelServiceUtils
                                .loadObject(TaskType.class, oid, getPageBase().retrieveItemsNamed(TaskType.F_RESULT),
                                        getPageBase(), task, result);
                        if (taskType == null) {
                            return null;
                        }

                        TaskDto taskDto = null;
                        try {
                            taskDto = new TaskDto(taskType.asObjectable(), null, getModel(), getTaskService(),
                                    getModelInteractionService(), getTaskManager(), getWorkflowManager(), options,
                                    false, task, result, getPageBase());
                            taskDto.setSelected(object.isSelected());
                        } catch (SchemaException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        return taskDto;
                    }

                    ;

                };
            }
        };

        provider.setQuery(createTaskQuery());
        return provider;
    }

    private ObjectQuery createTaskQuery() {
        TasksSearchDto dto = searchModel.getObject();
        TaskDtoExecutionStatusFilter status = dto.getStatus();
        String category = dto.getCategory();
        boolean showSubtasks = dto.isShowSubtasks();

        S_AtomicFilterEntry q = getPrismContext().queryFor(TaskType.class);
        if (status != null) {
            q = status.appendFilter(q);
        }
        if (category != null && !ALL_CATEGORIES.equals(category)) {
            q = q.item(TaskType.F_CATEGORY).eq(category).and();
        }
        if (StringUtils.isNotBlank(searchText)) {
            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalizedString = normalizer.normalize(searchText);
            q = q.item(TaskType.F_NAME).containsPoly(normalizedString, normalizedString).matchingNorm().and();
            searchText = ""; // ???
        }
        if (!Boolean.TRUE.equals(showSubtasks)) {
            q = q.item(TaskType.F_PARENT).isNull().and();
        }
        return customizedObjectQuery(q).build();
    }

    protected S_AtomicFilterExit customizedObjectQuery(S_AtomicFilterEntry q) {
        return q.all();
    }

    @NotNull
    public static AbstractExportableColumn<TaskDto, String> createProgressColumn(PageBase pageBase, String titleKey,
            SerializableSupplier<Boolean> progressComputationEnabledSupplier) {
        return new AbstractExportableColumn<TaskDto, String>(pageBase.createStringResource(titleKey)) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> cellItem, String componentId, final IModel<TaskDto> rowModel) {
                cellItem.add(new Label(componentId,
                        (IModel<Object>) () -> rowModel.getObject().getProgressDescription(pageBase,
                                progressComputationEnabledSupplier.get())));
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
                return Model.of(rowModel.getObject().getProgressDescription(pageBase,
                        progressComputationEnabledSupplier.get()));
            }
        };
    }

    private IModel<String> getTaskConfirmationMessageModel(ColumnMenuAction action, String actionName) {
        if (action.getRowModel() == null) {
            return createStringResource("pageTasks.message.confirmationMessageForMultipleTaskObject", actionName,
                    WebComponentUtil.getSelectedData(getTaskTable()).size());
        } else {
            String objectName = ((TaskDto) (action.getRowModel().getObject())).getName();
            return createStringResource("pageTasks.message.confirmationMessageForSingleTaskObject", actionName, objectName);
        }

    }

    private List<InlineMenuItem> createTasksInlineMenu(boolean isHeader, TaskDto dto) {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.suspendTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            suspendTasksPerformed(target);
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            suspendTaskPerformed(target, rowDto);
                        }
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
                return TaskDtoTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });
        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.resumeTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            resumeTasksPerformed(target);
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            resumeTaskPerformed(target, rowDto);
                        }
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
                return TaskDtoTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.scheduleTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            scheduleTasksPerformed(target);
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            scheduleTaskPerformed(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.runNowAction").getString();
                return TaskDtoTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            deleteTaskConfirmedPerformed(target, null);
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            deleteTaskConfirmedPerformed(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteAction").getString();
                return TaskDtoTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        InlineMenuItem reconcileWorkers = new InlineMenuItem(createStringResource("pageTasks.button.reconcileWorkers")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            throw new UnsupportedOperationException();
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            reconcileWorkersConfirmedPerformed(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.reconcileWorkersAction").getString();
                return TaskDtoTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        };
        reconcileWorkers.setVisibilityChecker(TaskDtoTablePanel::isCoordinator);
        items.add(reconcileWorkers);

        InlineMenuItem suspendRootOnly = new InlineMenuItem(createStringResource("pageTasks.button.suspendRootOnly")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            throw new UnsupportedOperationException();
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            suspendRootOnly(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.suspendAction").getString();
                return TaskDtoTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        };
        suspendRootOnly.setVisibilityChecker(TaskDtoTablePanel::isManageableTreeRoot);
        items.add(suspendRootOnly);

        InlineMenuItem resumeRootOnly = new InlineMenuItem(createStringResource("pageTasks.button.resumeRootOnly")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            throw new UnsupportedOperationException();
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            resumeRootOnly(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.resumeAction").getString();
                return TaskDtoTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        };
        resumeRootOnly.setVisibilityChecker(TaskDtoTablePanel::isManageableTreeRoot);
        items.add(resumeRootOnly);

        InlineMenuItem deleteWorkStateAndWorkers = new InlineMenuItem(createStringResource("pageTasks.button.deleteWorkersAndWorkState")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            throw new UnsupportedOperationException();
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            deleteWorkersAndWorkState(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteWorkersAndWorkState").getString();
                return TaskDtoTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        };
        deleteWorkStateAndWorkers.setVisibilityChecker(TaskDtoTablePanel::isManageableTreeRoot);
        items.add(deleteWorkStateAndWorkers);

        InlineMenuItem deleteWorkState = new InlineMenuItem(createStringResource("pageTasks.button.deleteWorkState")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            throw new UnsupportedOperationException();
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            deleteWorkState(target, rowDto);
                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteWorkState").getString();
                return TaskDtoTablePanel.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        };
        items.add(deleteWorkState);

        if (isHeader) {
            items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteAllClosedTasks")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<TaskDto>() {
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

            });
        }
        return items;
    }

    private void refreshTable(AjaxRequestTarget target) {
        clearTableCache();
        target.add(getPageBase().getFeedbackPanel());
        target.add((Component) getTaskTable());
    }

    private void clearTableCache() {
        if (getTaskTable() != null && getTaskTable().getDataTable() != null) {
            WebComponentUtil.clearProviderCache(getTaskTable().getDataTable().getDataProvider());
        }
    }

    // must be static, otherwise JVM crashes (probably because of some wicket serialization issues)
    private static boolean isCoordinator(IModel<?> rowModel, boolean isHeader) {
        TaskDto dto = getDto(rowModel, isHeader);
        return dto != null && dto.isCoordinator();
    }

    // must be static, otherwise JVM crashes (probably because of some wicket serialization issues)
    private static boolean isManageableTreeRoot(IModel<?> rowModel, boolean isHeader) {
        TaskDto dto = getDto(rowModel, isHeader);
        return dto != null && isManageableTreeRoot(dto);
    }

    private static boolean isManageableTreeRoot(TaskDto dto) {
        return dto.isCoordinator() || dto.isPartitionedMaster();
    }

    private static TaskDto getDto(IModel<?> rowModel, boolean isHeader) {
        if (rowModel != null && !isHeader) {
            Object object = rowModel.getObject();
            if (object instanceof TaskDto) {
                return (TaskDto) object;
            }
        }
        return null;
    }

    private void reconcileWorkersConfirmedPerformed(AjaxRequestTarget target, @NotNull TaskDto task) {
        Task opTask = getPageBase().createSimpleTask(OPERATION_RECONCILE_WORKERS);
        OperationResult result = opTask.getResult();
        try {
            getPageBase().getTaskService().reconcileWorkers(task.getOid(), opTask, result);
            result.computeStatus();
            if (result.isSuccess() && result.getSubresults().size() == 1) { // brutal hack: to show statistics
                result.setMessage(result.getSubresults().get(0).getMessage());
            }
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | SecurityViolationException
                | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(
                    createStringResource("pageTasks.message.reconcileWorkersConfirmedPerformed.fatalError").getString(), e);
        }
        getPageBase().showResult(result);

        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
        provider.clearCache();

        // refresh feedback and table
        refreshTable(target);
    }

    private void suspendRootOnly(AjaxRequestTarget target, @NotNull TaskDto task) {
        Task opTask = getPageBase().createSimpleTask(OPERATION_SUSPEND_TASK);
        OperationResult result = opTask.getResult();
        try {
            getPageBase().getTaskService().suspendTasks(Collections.singleton(task.getOid()), WAIT_FOR_TASK_STOP, opTask, result);
            // TODO check whether the suspension was complete
            result.computeStatus();
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.suspendRootOnly.fatalError").getString(), e);
        }
        getPageBase().showResult(result);

        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
        provider.clearCache();
        refreshTable(target);
    }

    private void resumeRootOnly(AjaxRequestTarget target, @NotNull TaskDto task) {
        Task opTask = getPageBase().createSimpleTask(OPERATION_RESUME_TASK);
        OperationResult result = opTask.getResult();
        try {
            getPageBase().getTaskService().resumeTasks(Collections.singleton(task.getOid()), opTask, result);
            result.computeStatus();
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.resumeRootOnly.fatalError").getString(), e);
        }
        getPageBase().showResult(result);

        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
        provider.clearCache();
        refreshTable(target);
    }

    private void deleteWorkersAndWorkState(AjaxRequestTarget target, @NotNull TaskDto task) {
        Task opTask = getPageBase().createSimpleTask(OPERATION_DELETE_WORKERS_AND_WORK_STATE);
        OperationResult result = opTask.getResult();
        try {
            getPageBase().getTaskService().deleteWorkersAndWorkState(task.getOid(), true, WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.deleteWorkersAndWorkState.fatalError").getString(),
                    e);
        }
        getPageBase().showResult(result);

        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
        provider.clearCache();
        refreshTable(target);
    }

    private void deleteWorkState(AjaxRequestTarget target, @NotNull TaskDto task) {
        Task opTask = getPageBase().createSimpleTask(OPERATION_DELETE_WORK_STATE);
        OperationResult result = opTask.getResult();
        try {
            getPageBase().getTaskService().deleteWorkersAndWorkState(task.getOid(), false, WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.deleteWorkState.fatalError").getString(),
                    e);
        }
        getPageBase().showResult(result);

        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
        provider.clearCache();
        refreshTable(target);
    }

    private void deleteAllClosedTasksConfirmedPerformed(AjaxRequestTarget target) {
        OperationResult launchResult = new OperationResult(OPERATION_DELETE_ALL_CLOSED_TASKS);
        Task task = getPageBase().createSimpleTask(OPERATION_DELETE_ALL_CLOSED_TASKS);

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

        getPageBase().getTaskManager().switchToBackground(task, launchResult);
        launchResult.setBackgroundTaskOid(task.getOid());

        getPageBase().showResult(launchResult);
        target.add(getPageBase().getFeedbackPanel());
    }

    private void deleteTaskConfirmedPerformed(AjaxRequestTarget target, TaskDto task) {
        List<TaskDto> taskDtoList = new ArrayList<>();
        if (task != null) {
            taskDtoList.add(task);
        } else {
            taskDtoList.addAll(WebComponentUtil.getSelectedData(getTaskTable()));
        }
        if (!isSomeTaskSelected(taskDtoList, target)) {
            return;
        }

        Task opTask = getPageBase().createSimpleTask(OPERATION_DELETE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getPageBase().getTaskService().suspendAndDeleteTasks(TaskDto.getOids(taskDtoList), WAIT_FOR_TASK_STOP, true, opTask, result);
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
        getPageBase().showResult(result);

        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
        provider.clearCache();

        // refresh feedback and table
        refreshTable(target);
    }

    private void scheduleTasksPerformed(AjaxRequestTarget target, List<String> oids) {
        Task opTask = getPageBase().createSimpleTask(OPERATION_SCHEDULE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getPageBase().getTaskService().scheduleTasksNow(oids, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("pageTasks.message.scheduleTasksPerformed.success").getString());
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.scheduleTasksPerformed.fatalError").getString(), e);
        }
        getPageBase().showResult(result);

        //refresh feedback and table
        refreshTable(target);
    }

    private void scheduleTaskPerformed(AjaxRequestTarget target, TaskDto dto) {
        scheduleTasksPerformed(target, Arrays.asList(dto.getOid()));
    }

    private void scheduleTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskDtoList = WebComponentUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskDtoList, target)) {
            return;
        }

        scheduleTasksPerformed(target, TaskDto.getOids(taskDtoList));
    }

    private void resumeTasksPerformed(AjaxRequestTarget target, List<TaskDto> dtoList) {
        Task opTask = getPageBase().createSimpleTask(OPERATION_RESUME_TASKS);
        OperationResult result = opTask.getResult();
        try {
            List<TaskDto> plainTasks = dtoList.stream().filter(dto -> !isManageableTreeRoot(dto)).collect(Collectors.toList());
            List<TaskDto> trees = dtoList.stream().filter(dto -> isManageableTreeRoot(dto)).collect(Collectors.toList());
            getPageBase().getTaskService().resumeTasks(TaskDto.getOids(plainTasks), opTask, result);
            for (TaskDto tree : trees) {
                getPageBase().getTaskService().resumeTaskTree(tree.getOid(), opTask, result);
            }
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("pageTasks.message.resumeTasksPerformed.success").getString());
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.resumeTasksPerformed.fatalError").getString(), e);
        }
        getPageBase().showResult(result);

        //refresh feedback and table
        refreshTable(target);
    }


    private void resumeTaskPerformed(AjaxRequestTarget target, TaskDto dto) {
        resumeTasksPerformed(target, Collections.singletonList(dto));
    }

    private void resumeTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskDtoList = WebComponentUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskDtoList, target)) {
            return;
        }

        resumeTasksPerformed(target, taskDtoList);
    }

    private void suspendTaskPerformed(AjaxRequestTarget target, TaskDto dto) {
        suspendTasksPerformed(target, Collections.singletonList(dto));
    }

    //region Task-level actions
    private void suspendTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> dtoList = WebComponentUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(dtoList, target)) {
            return;
        }

        suspendTasksPerformed(target, dtoList);
    }

    private void suspendTasksPerformed(AjaxRequestTarget target, List<TaskDto> dtoList) {
        Task opTask = getPageBase().createSimpleTask(OPERATION_SUSPEND_TASKS);
        OperationResult result = opTask.getResult();
        try {
            List<TaskDto> plainTasks = dtoList.stream().filter(dto -> !isManageableTreeRoot(dto)).collect(Collectors.toList());
            List<TaskDto> trees = dtoList.stream().filter(dto -> isManageableTreeRoot(dto)).collect(Collectors.toList());
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
        getPageBase().showResult(result);

        //refresh feedback and table
        refreshTable(target);
    }

    private boolean suspendPlainTasks(List<TaskDto> plainTasks, OperationResult result, Task opTask)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        if (!plainTasks.isEmpty()) {
            return getPageBase().getTaskService().suspendTasks(TaskDto.getOids(plainTasks), WAIT_FOR_TASK_STOP, opTask, result);
        } else {
            return true;
        }
    }

    private boolean suspendTrees(List<TaskDto> roots, OperationResult result, Task opTask)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        boolean suspended = true;
        if (!roots.isEmpty()) {
            for (TaskDto root : roots) {
                boolean s = getPageBase().getTaskService().suspendTaskTree(root.getOid(), WAIT_FOR_TASK_STOP, opTask, result);
                suspended = suspended && s;
            }
        }
        return suspended;
    }

    // used in SubtasksPanel as well
    public static IColumn createTaskNameColumn(final Component component, String label) {
        LinkColumn<TaskDto> column = new LinkColumn<TaskDto>(PageBase.createStringResourceStatic(component, label), TaskDto.F_NAME,
                TaskDto.F_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<TaskDto> rowModel) {
                TaskDto task = rowModel.getObject();
                taskDetailsPerformed(target, task.getOid());
            }

            private void taskDetailsPerformed(AjaxRequestTarget target, String oid) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, oid);

                PageBase page = (PageBase) component.getPage();
                page.navigateToNext(PageTaskEdit.class, parameters);
            }

            @Override
            public boolean isEnabled(IModel<TaskDto> rowModel) {
                return super.isEnabled(rowModel) && rowModel.getObject().getOid() != null;
            }
        };
        return column;
    }

    public static AbstractColumn<TaskDto, String> createTaskCategoryColumn(final Component component, String label) {
        return new AbstractExportableColumn<TaskDto, String>(PageBase.createStringResourceStatic(component, label)) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                    final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId,
                        WebComponentUtil.createCategoryNameModel(component, new PropertyModel<>(rowModel, TaskDto.F_CATEGORY))));
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
                return WebComponentUtil.createCategoryNameModel(component, new PropertyModel<>(rowModel, TaskDto.F_CATEGORY));
            }
        };
    }

    public static EnumPropertyColumn createTaskResultStatusColumn(final Component component, String label) {
        return new EnumPropertyColumn(PageBase.createStringResourceStatic(component, label), "status") {

            @Override
            protected String translate(Enum en) {
                return PageBase.createStringResourceStatic(component, en).getString();
            }
        };
    }

    public static EnumPropertyColumn<TaskDto> createTaskExecutionStatusColumn(final Component component, String label) {
        return new EnumPropertyColumn<TaskDto>(PageBase.createStringResourceStatic(component, label), "execution") {

            @Override
            protected String translate(Enum en) {
                return PageBase.createStringResourceStatic(component, en).getString();
            }
        };
    }

    private boolean isSomeTaskSelected(List<TaskDto> tasks, AjaxRequestTarget target) {
        if (!tasks.isEmpty()) {
            return true;
        }

        warn(getString("pageTasks.message.noTaskSelected"));
        target.add(getPageBase().getFeedbackPanel());
        return false;
    }

    private void clearSearchPerformed(AjaxRequestTarget target) {
        TasksSearchDto tasksSearchDto = new TasksSearchDto();
        tasksSearchDto.setCategory(ALL_CATEGORIES);
        tasksSearchDto.setStatus(TaskDtoExecutionStatusFilter.ALL);
        searchModel.setObject(tasksSearchDto);

        Table panel = getTaskTable();
        DataTable table = panel.getDataTable();
        TaskDtoProvider provider = (TaskDtoProvider) table.getDataProvider();
        provider.setQuery(null);

        TasksStorage storage = getTaskStorage();
        storage.setTasksSearch(searchModel.getObject());
        panel.setCurrentPage(storage.getPaging());

        target.add((Component) panel);
    }

    private void searchFilterPerformed(AjaxRequestTarget target) {
        TasksSearchDto dto = searchModel.getObject();

        // ObjectQuery query = createTaskQuery(dto.getStatus(),
        // dto.getCategory(), dto.isShowSubtasks());
        ObjectQuery query = createTaskQuery();

        Table panel = getTaskTable();
        DataTable table = panel.getDataTable();
        TaskDtoProvider provider = (TaskDtoProvider) table.getDataProvider();
        provider.setQuery(query);
        table.setCurrentPage(0);

        TasksStorage storage = getTaskStorage();
        storage.setTasksSearch(dto);

        target.add(getPageBase().getFeedbackPanel());
        target.add((Component) getTaskTable());
    }

    private static class SearchFragment extends Fragment {

        TaskDtoTablePanel parent;

        public SearchFragment(String id, String markupId, MarkupContainer markupProvider, IModel<TasksSearchDto> model, TaskDtoTablePanel parent) {
            super(id, markupId, markupProvider, model);
            this.parent = parent;
            initLayout();
        }

        private void initLayout() {
            final Form searchForm = new com.evolveum.midpoint.web.component.form.Form(ID_SEARCH_FORM);
            add(searchForm);
            searchForm.setOutputMarkupId(true);

            final IModel<TasksSearchDto> searchModel = (IModel) getDefaultModel();

            DropDownChoice listSelect = new DropDownChoice<>(ID_STATE, new PropertyModel<>(searchModel, TasksSearchDto.F_STATUS),
                    new ReadOnlyEnumValuesModel<>(TaskDtoExecutionStatusFilter.class), new EnumChoiceRenderer<>(this));
            listSelect.add(createFilterAjaxBehaviour());
            listSelect.setOutputMarkupId(true);
            listSelect.setNullValid(false);
            if (listSelect.getModel().getObject() == null) {
                listSelect.getModel().setObject(TaskDtoExecutionStatusFilter.ALL);
            }
            searchForm.add(listSelect);

            DropDownChoice categorySelect = new DropDownChoice(ID_CATEGORY,
                    new PropertyModel(searchModel, TasksSearchDto.F_CATEGORY), new IModel<List<String>>() {

                        @Override
                        public List<String> getObject() {
                            return createCategoryList();
                        }
                    }, new StringChoiceRenderer.Prefixed("pageTasks.category.") {

                        @Override
                        public String getDisplayValue(String object) {
                            if (ALL_CATEGORIES.equals(object)) {
                                object = "AllCategories";
                            }
                            return getPage().getString("pageTasks.category." + object);
                        }

                    });
            categorySelect.setOutputMarkupId(true);
            categorySelect.setNullValid(false);
            categorySelect.add(createFilterAjaxBehaviour());
            if (categorySelect.getModel().getObject() == null) {
                categorySelect.getModel().setObject(ALL_CATEGORIES);
            }
            searchForm.add(categorySelect);
            WebMarkupContainer showProgressLabel = new WebMarkupContainer(ID_SHOW_PROGRESS_LABEL);
            CheckBox showProgress = new CheckBox(ID_SHOW_PROGRESS,
                    new PropertyModel<>(searchModel, TasksSearchDto.F_SHOW_PROGRESS));
            showProgress.add(createFilterAjaxBehaviour());
            showProgressLabel.add(showProgress);
            searchForm.add(showProgressLabel);
            WebMarkupContainer showSubtasksLabel = new WebMarkupContainer(ID_SHOW_SUBTASKS_LABEL);
            CheckBox showSubtasks = new CheckBox(ID_SHOW_SUBTASKS,
                    new PropertyModel<>(searchModel, TasksSearchDto.F_SHOW_SUBTASKS));
            showSubtasks.add(createFilterAjaxBehaviour());
            showSubtasksLabel.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isVisible() {
                    return parent.isVisibleShowSubtask();
                }
            });
            showSubtasksLabel.add(showSubtasks);
            searchForm.add(showSubtasksLabel);

            AjaxSubmitButton clearButton = new AjaxSubmitButton(ID_SEARCH_CLEAR) {

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                     parent.clearSearchPerformed(target);
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                    PageBase page = (PageBase) getPage();
                    target.add(page.getFeedbackPanel());
                }
            };
            searchForm.add(clearButton);
        }

        private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
            return new AjaxFormComponentUpdatingBehavior("change") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    parent.searchFilterPerformed(target);
                }
            };
        }

        private List<String> createCategoryList() {
            List<String> categories = new ArrayList<>();
            categories.add(ALL_CATEGORIES);

            PageBase page = (PageBase) getPage();
            List<String> list = page.getTaskService().getAllTaskCategories();
            if (list != null) {
                categories.addAll(list);
                Collections.sort(categories);
            }

            return categories;
        }
    }

    protected boolean isVisibleShowSubtask() {
        return true;
    }

    protected boolean isProgressComputationEnabled() {
        return searchModel.getObject().isShowProgress();
    }
}
