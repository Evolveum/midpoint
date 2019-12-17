package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.TaskSelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.page.admin.server.dto.*;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.lucene.queryparser.surround.query.AndQuery;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
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
public class PageTasksNew extends PageAdminObjectList implements Refreshable {

    private static final transient Trace LOGGER = TraceManager.getTrace(PageTasksNew.class);

    private static final int REFRESH_INTERVAL = 60000;                // don't set too low to prevent refreshing open inline menus (TODO skip refresh if a menu is open)


    @Override
    protected Class getType() {
        return TaskType.class;
    }

    @Override
    protected List<IColumn<TaskSelectableBean, String>> initColumns() {
        return initTaskColumns();
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        return createTasksInlineMenu(true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.TABLE_TASKS;
    }


    @Override
    protected BaseSortableDataProvider<SelectableBean> getCustomProvider() {
        TaskDtoProviderOptions options = TaskDtoProviderOptions.minimalOptions();
        options.setGetNextRunStartTime(true);
        options.setUseClusterInformation(true);
        options.setResolveObjectRef(true);
        return (BaseSortableDataProvider) new TaskSelectableBeanProvider(PageTasksNew.this, options) {

            @Override
            public TaskSelectableBean createTaskDto(PrismObject<TaskType> task, boolean subtasksLoaded, Task opTask, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
                TaskSelectableBean bean = super.createTaskDto(task, subtasksLoaded, opTask, result);
                bean.getMenuItems().addAll(createTasksInlineMenu(false));
                return bean;
            }

            @Override
            public ObjectQuery getQuery() {
                ObjectQuery q = super.getQuery();
                ObjectFilter noSubtasksFilter = getPrismContext().queryFor(TaskType.class).item(TaskType.F_PARENT).isNull().buildFilter();
                if (q == null) {
                    q = getPrismContext().queryFactory().createQuery();
                }
                q.addFilter(noSubtasksFilter);

                return q;
            }
        };
    }

    @Override
    protected boolean isNameColumnClickable(IModel rowModel) {
        return ((TaskType) ((TaskSelectableBean)rowModel.getObject()).getValue()).getOid() != null;
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {
        taskDetailsPerformed(target, object.getOid());
    }

    private void taskDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        navigateToNext(PageTaskEdit.class, parameters);
    }

    private List<IColumn<TaskSelectableBean, String>> initTaskColumns() {
        List<IColumn<TaskSelectableBean, String>> columns = new ArrayList<>();

        columns.add(createTaskCategoryColumn());
        columns.addAll(initCustomTaskColumns());

        return columns;
    }

    private IColumn<TaskSelectableBean, String> createTaskCategoryColumn() {
            return new AbstractExportableColumn<TaskSelectableBean, String>(createStringResource("pageTasks.task.category"), TaskType.F_CATEGORY.getLocalPart()) {

                @Override
                public void populateItem(Item<ICellPopulator<TaskSelectableBean>> item, String componentId,
                                         final IModel<TaskSelectableBean> rowModel) {
                    item.add(new Label(componentId,
                            WebComponentUtil.createCategoryNameModel(PageTasksNew.this, new PropertyModel<>(rowModel, TaskSelectableBean.F_VALUE + "." + TaskDto.F_CATEGORY))));
                }

                @Override
                public IModel<String> getDataModel(IModel<TaskSelectableBean> rowModel) {
                    return WebComponentUtil.createCategoryNameModel(PageTasksNew.this, new PropertyModel<>(rowModel, TaskSelectableBean.F_VALUE + "." + TaskDto.F_CATEGORY));
                }
            };

    }

    protected List<IColumn<TaskSelectableBean, String>> initCustomTaskColumns() {
        List<IColumn<TaskSelectableBean, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<TaskSelectableBean>(createStringResource("")) {
            @Override
            protected DisplayType getIconDisplayType(IModel<TaskSelectableBean> rowModel) {
                ObjectReferenceType ref = rowModel.getObject().getValue().getObjectRef();
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
            public void populateItem(Item<ICellPopulator<TaskSelectableBean>> item, String componentId, IModel<TaskSelectableBean> rowModel) {
                super.populateItem(item, componentId, rowModel);
                ObjectReferenceType ref = rowModel.getObject().getValue().getObjectRef();
                if (ref != null && ref.getType() != null) {
                    ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(ref.getType());
                    if (guiDescriptor != null) {
                        item.add(AttributeModifier.replace("title", createStringResource(guiDescriptor.getLocalizationKey())));
                        item.add(new TooltipBehavior());
                    }
                }
            }
        });

        columns.add(new AbstractExportableColumn<TaskSelectableBean, String>(createStringResource("pageTasks.task.objectRef")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskSelectableBean>> item, String componentId,
                                     final IModel<TaskSelectableBean> rowModel) {
                item.add(new Label(componentId, new IModel<String>() {

                    @Override
                    public String getObject() {
                        return createObjectRef(rowModel);
                    }
                }));
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskSelectableBean> rowModel) {
                return Model.of(createObjectRef(rowModel));
            }

        });
        columns.add(createTaskExecutionStatusColumn());
        columns.add(new PropertyColumn<>(createStringResource("pageTasks.task.executingAt"), TaskSelectableBean.F_VALUE + "." + TaskType.F_NODE_AS_OBSERVED.getLocalPart()));
        columns.add(createProgressColumn("pageTasks.task.progress", this::isProgressComputationEnabled));
        columns.add(new AbstractExportableColumn<TaskSelectableBean, String>(createStringResource("pageTasks.task.currentRunTime")) {

            @Override
            public void populateItem(final Item<ICellPopulator<TaskSelectableBean>> item, final String componentId,
                                     final IModel<TaskSelectableBean> rowModel) {

                DateLabelComponent dateLabel = new DateLabelComponent(componentId, new IModel<Date>() {

                    @Override
                    public Date getObject() {
                        Date date = getCurrentRuntime(rowModel);
                        TaskSelectableBean task = rowModel.getObject();
                        if (getRawExecutionStatus(task.getValue()) == TaskExecutionStatus.CLOSED && date != null) {
                            ((DateLabelComponent) item.get(componentId)).setBefore(createStringResource("pageTasks.task.closedAt").getString() + " ");
                        } else if (date != null) {
                            ((DateLabelComponent) item.get(componentId))
                                    .setBefore(WebComponentUtil.formatDurationWordsForLocal(date.getTime(), true, true, PageTasksNew.this));
                        }
                        return date;
                    }
                }, WebComponentUtil.getShortDateTimeFormat(PageTasksNew.this));
                item.add(dateLabel);
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskSelectableBean> rowModel) {
                TaskSelectableBean task = rowModel.getObject();
                Date date = getCurrentRuntime(rowModel);
                String displayValue = "";
                if (date != null) {
                    if (getRawExecutionStatus(task.getValue()) == TaskExecutionStatus.CLOSED) {
                        displayValue =
                                createStringResource("pageTasks.task.closedAt").getString() +
                                        WebComponentUtil.getShortDateTimeFormattedValue(date, PageTasksNew.this);
                    } else {
                        displayValue = WebComponentUtil.formatDurationWordsForLocal(date.getTime(), true, true, PageTasksNew.this);
                    }
                }
                return Model.of(displayValue);
            }
        });
        columns.add(new AbstractExportableColumn<TaskSelectableBean, String>(createStringResource("pageTasks.task.scheduledToRunAgain")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskSelectableBean>> item, String componentId,
                                     final IModel<TaskSelectableBean> rowModel) {
                item.add(new Label(componentId, new IModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createScheduledToRunAgain(rowModel);
                    }
                }));
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskSelectableBean> rowModel) {
                return Model.of(createScheduledToRunAgain(rowModel));
            }
        });

        columns.add(new IconColumn<TaskSelectableBean>(createStringResource("pageTasks.task.status")) {

            @Override
            protected DisplayType getIconDisplayType(final IModel<TaskSelectableBean> rowModel) {
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

    private Date getCurrentRuntime(IModel<TaskSelectableBean> taskModel) {
        TaskType task = taskModel.getObject().getValue();

        if (getRawExecutionStatus(task) == TaskExecutionStatus.CLOSED) {

            Long time = getCompletionTimestamp(task);
            if (time == null) {
                return null;
            }
            return new Date(time);

        }
        return null;
    }

    public Long getCompletionTimestamp(TaskType taskType) {
        return xgc2long(taskType.getCompletionTimestamp());
    }

    private String createScheduledToRunAgain(IModel<TaskSelectableBean> taskModel) {
        TaskType task = taskModel.getObject().getValue();
        boolean runnable = getRawExecutionStatus(task) == TaskExecutionStatus.RUNNABLE;
        Long scheduledAfter = getScheduledToStartAgain(taskModel.getObject());
        Long retryAfter = runnable ? getRetryAfter(task) : null;

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

    public static final long RUNS_CONTINUALLY = -1L;
    public static final long ALREADY_PASSED = -2L;
    public static final long NOW = 0L;

    public Long getScheduledToStartAgain(TaskSelectableBean taskBean) {
        long current = System.currentTimeMillis();

        if (getExecution(taskBean.getValue()) == TaskDtoExecutionStatus.RUNNING) {

            if (TaskRecurrenceType.RECURRING != taskBean.getValue().getRecurrence()) {
                return null;
            } else if (TaskBindingType.TIGHT == taskBean.getValue().getBinding()) {
                return RUNS_CONTINUALLY;             // runs continually; todo provide some information also in this case
            }
        }

        Long nextRunStartTimeLong = getNextRunStartTimeLong(taskBean.getValue());
        if (nextRunStartTimeLong == null || nextRunStartTimeLong == 0) {
            return null;
        }

        if (nextRunStartTimeLong > current + 1000) {
            return nextRunStartTimeLong - System.currentTimeMillis();
        } else if (nextRunStartTimeLong < current - 60000) {
            return ALREADY_PASSED;
        } else {
            return NOW;
        }
    }

    public Long getRetryAfter(TaskType taskType) {
        Long retryAt = getNextRetryTimeLong(taskType);
        return retryAt != null ? retryAt - System.currentTimeMillis() : null;
    }

    public Long getNextRetryTimeLong(TaskType taskType) {
        return xgc2long(taskType.getNextRetryTimestamp());
    }

    public Long getNextRunStartTimeLong(TaskType taskType) {
        return xgc2long(taskType.getNextRunStartTimestamp());
    }

    public TaskDtoExecutionStatus getExecution(TaskType taskType) {
        return TaskDtoExecutionStatus.fromTaskExecutionStatus(taskType.getExecutionStatus(), taskType.getNodeAsObserved() != null);
    }

    private String createObjectRef(IModel<TaskSelectableBean> taskModel) {
        TaskType task = taskModel.getObject().getValue();
        if (task.getObjectRef() == null) {
            return "";
        }

        return WebComponentUtil.getDisplayNameOrName(task.getObjectRef());
    }

    public EnumPropertyColumn<TaskSelectableBean> createTaskExecutionStatusColumn() {
        //TaskType.F_EXECUTION_STATUS
        return new EnumPropertyColumn<TaskSelectableBean>(createStringResource("pageTasks.task.execution"), TaskSelectableBean.F_VALUE + ".executionStatus") {

            @Override
            protected String translate(Enum en) {
                return PageTasksNew.this.createStringResource(en).getString();
            }
        };
    }

    public AbstractExportableColumn<TaskSelectableBean, String> createProgressColumn(String titleKey,
                                                                                               SerializableSupplier<Boolean> progressComputationEnabledSupplier) {
        return new AbstractExportableColumn<TaskSelectableBean, String>(createStringResource(titleKey)) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskSelectableBean>> cellItem, String componentId, final IModel<TaskSelectableBean> rowModel) {
                cellItem.add(new Label(componentId,
                        (IModel<Object>) () -> getProgressDescription(rowModel.getObject(),
                                progressComputationEnabledSupplier.get())));
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskSelectableBean> rowModel) {
                return Model.of(getProgressDescription(rowModel.getObject(),
                        progressComputationEnabledSupplier.get()));
            }
        };
    }

    public String getProgressDescription(TaskSelectableBean task, boolean alwaysCompute) {
        Long stalledSince = getStalledSince(task.getValue());
        if (stalledSince != null) {
            return getString("pageTasks.stalledSince", new Date(stalledSince).toLocaleString(), getRealProgressDescription(task, alwaysCompute));
        } else {
            return getRealProgressDescription(task, alwaysCompute);
        }
    }

    private String getRealProgressDescription(TaskSelectableBean task, boolean alwaysCompute) {
        if (isPartitionedMaster(task.getValue())) {
            return getPartitionedTaskProgressDescription(task, alwaysCompute);
        } else if (isWorkStateHolder(task)) {
            return getBucketedTaskProgressDescription(task.getValue());
        } else {
            return getPlainTaskProgressDescription(task.getValue());
        }
    }

    public boolean isPartitionedMaster(TaskType taskType) {
        return taskType.getWorkManagement() != null && taskType.getWorkManagement().getTaskKind() == TaskKindType.PARTITIONED_MASTER;
    }

    private String getPartitionedTaskProgressDescription(TaskSelectableBean selectableBean, boolean alwaysCompute) {
        if (alwaysCompute) {
            ensureSubtasksLoaded(selectableBean);
        }
        if (!selectableBean.isLoadedChildren()) {
            return "?";
        }
        int completePartitions = 0;
        List<TaskSelectableBean> subtasks = selectableBean.getChildren();
        int allPartitions = subtasks.size();
        int firstIncompleteSequentialNumber = 0;
        TaskSelectableBean firstIncomplete = null;
        for (TaskSelectableBean subtask : subtasks) {
            TaskType taskType = subtask.getValue();
            if (getRawExecutionStatus(taskType) == TaskExecutionStatus.CLOSED) {
                completePartitions++;
            } else if (getPartitionSequentialNumber(taskType) != null) {
                if (firstIncomplete == null ||
                        getPartitionSequentialNumber(taskType) < firstIncompleteSequentialNumber) {
                    firstIncompleteSequentialNumber = getPartitionSequentialNumber(taskType);
                    firstIncomplete = subtask;
                }
            }
        }
        String coarseProgress = completePartitions + "/" + allPartitions;
        if (firstIncomplete == null) {
            return coarseProgress;
        } else {
            return coarseProgress + " + " + getRealProgressDescription(firstIncomplete, alwaysCompute);
        }
    }

    //TODO why?
    public TaskExecutionStatus getRawExecutionStatus(TaskType taskType) {
        return TaskExecutionStatus.fromTaskType(taskType.getExecutionStatus());
    }

    private Integer getPartitionSequentialNumber(TaskType taskType) {
        return taskType.getWorkManagement() != null ? taskType.getWorkManagement().getPartitionSequentialNumber() : null;
    }

    public void ensureSubtasksLoaded(TaskSelectableBean pageBase) {
        ensureSubtasksLoaded(pageBase, false);
    }

    public void ensureSubtasksLoaded(TaskSelectableBean selectableBean, boolean recursive) {
        TaskType taskType = selectableBean.getValue();
        if (!selectableBean.isLoadedChildren() && taskType.getOid() != null) {

                Collection<SelectorOptions<GetOperationOptions>> getOptions = getOperationOptionsBuilder()
                        .item(TaskType.F_SUBTASK).retrieve()
                        .build();
                Task opTask = createAnonymousTask("ensureSubtasksLoaded");
                try {
                    TaskType task = getModelService()
                            .getObject(TaskType.class, taskType.getOid(), getOptions, opTask, opTask.getResult()).asObjectable();
//                    fillInChildren(task, pageBase.getModelService(), pageBase.getTaskService(),
//                            pageBase.getModelInteractionService(),
//                            pageBase.getTaskManager(), pageBase.getWorkflowManager(), TaskDtoProviderOptions.minimalOptions(),
//                            opTask,
//                            opTask.getResult(), pageBase);
                    selectableBean.setLoadedChildren(true);
                    selectableBean.setChildren(createSubtasks(taskType));
                } catch (ObjectNotFoundException t) {   // often happens when refreshing Task List page
                    LOGGER.debug("Couldn't load subtasks for task {}", taskType, t);
                    selectableBean.setLoadedChildren(false);
                } catch (Throwable t) {
                    getSession().error("Couldn't load subtasks: " + t.getMessage());            // TODO
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load subtasks for task {}", t, taskType);
                    selectableBean.setLoadedChildren(false);
                }

        }
        if (recursive) {
            for (TaskSelectableBean subtask : selectableBean.getChildren()) {
                ensureSubtasksLoaded(subtask, true);
            }
        }
    }

    private List<TaskSelectableBean> createSubtasks(TaskType taskType) {
        List<TaskSelectableBean> subtasks = new ArrayList<>();
        for (TaskType subtask : TaskTypeUtil.getResolvedSubtasks(taskType)) {
            TaskSelectableBean selectableSubTask = new TaskSelectableBean(subtask);
            subtasks.add(selectableSubTask);
        }
        return subtasks;
    }

    public boolean isWorkStateHolder(TaskSelectableBean taskType) {
        return (TaskTypeUtil.isCoordinator(taskType.getValue()) || hasBuckets(taskType.getValue())) && !isCoordinatedWorker(taskType.getValue());
    }

    public boolean isCoordinatedWorker(TaskType taskType) {
        return taskType.getWorkManagement() != null && taskType.getWorkManagement().getTaskKind() == TaskKindType.WORKER;
    }

    public boolean hasBuckets(TaskType taskType) {
        if (taskType.getWorkState() == null) {
            return false;
        }
        if (taskType.getWorkState().getNumberOfBuckets() != null && taskType.getWorkState().getNumberOfBuckets() > 1) {
            return true;
        }
        List<WorkBucketType> buckets = taskType.getWorkState().getBucket();
        if (buckets.size() > 1) {
            return true;
        } else {
            return buckets.size() == 1 && buckets.get(0).getContent() != null;
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

    public Long getStalledSince(TaskType taskType) {
        return xgc2long(taskType.getStalledSince());
    }

    private Long xgc2long(XMLGregorianCalendar gc) {
        return gc != null ? XmlTypeConverter.toMillis(gc) : null;
    }



    private List<InlineMenuItem> createTasksInlineMenu(boolean isHeader) {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.suspendTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskSelectableBean>() {
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
                return PageTasksNew.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });
        items.add(new ButtonInlineMenuItem(createStringResource("pageTasks.button.resumeTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskSelectableBean>() {
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
                return PageTasksNew.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.scheduleTask")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskSelectableBean>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                            scheduleTasksPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.runNowAction").getString();
                return PageTasksNew.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
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
//                        if (getRowModel() == null) {
//                            deleteTaskConfirmedPerformed(target, null);
//                        } else {
//                            TaskDto rowDto = getRowModel().getObject();
//                            deleteTaskConfirmedPerformed(target, rowDto);
//                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteAction").getString();
                return PageTasksNew.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
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
//                        if (getRowModel() == null) {
//                            throw new UnsupportedOperationException();
//                        } else {
//                            TaskDto rowDto = getRowModel().getObject();
//                            reconcileWorkersConfirmedPerformed(target, rowDto);
//                        }
                    }
                };
            }
            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.reconcileWorkersAction").getString();
                return PageTasksNew.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
        reconcileWorkers.setVisibilityChecker(PageTasksNew::isCoordinator);
        items.add(reconcileWorkers);

        InlineMenuItem suspendRootOnly = new InlineMenuItem(createStringResource("pageTasks.button.suspendRootOnly")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        if (getRowModel() == null) {
//                            throw new UnsupportedOperationException();
//                        } else {
//                            TaskDto rowDto = getRowModel().getObject();
//                            suspendRootOnly(target, rowDto);
//                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.suspendAction").getString();
                return PageTasksNew.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
        suspendRootOnly.setVisibilityChecker(PageTasksNew::isManageableTreeRoot);
        items.add(suspendRootOnly);

        InlineMenuItem resumeRootOnly = new InlineMenuItem(createStringResource("pageTasks.button.resumeRootOnly")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        if (getRowModel() == null) {
//                            throw new UnsupportedOperationException();
//                        } else {
//                            TaskDto rowDto = getRowModel().getObject();
//                            resumeRootOnly(target, rowDto);
//                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.resumeAction").getString();
                return PageTasksNew.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
        resumeRootOnly.setVisibilityChecker(PageTasksNew::isManageableTreeRoot);
        items.add(resumeRootOnly);

        InlineMenuItem deleteWorkStateAndWorkers = new InlineMenuItem(createStringResource("pageTasks.button.deleteWorkersAndWorkState")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        if (getRowModel() == null) {
//                            throw new UnsupportedOperationException();
//                        } else {
//                            TaskDto rowDto = getRowModel().getObject();
//                            deleteWorkersAndWorkState(target, rowDto);
//                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteWorkersAndWorkState").getString();
                return PageTasksNew.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
        deleteWorkStateAndWorkers.setVisibilityChecker(PageTasksNew::isManageableTreeRoot);
        items.add(deleteWorkStateAndWorkers);

        InlineMenuItem deleteWorkState = new InlineMenuItem(createStringResource("pageTasks.button.deleteWorkState")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<TaskDto>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
//                        if (getRowModel() == null) {
//                            throw new UnsupportedOperationException();
//                        } else {
//                            TaskDto rowDto = getRowModel().getObject();
//                            deleteWorkState(target, rowDto);
//                        }
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("pageTasks.message.deleteWorkState").getString();
                return PageTasksNew.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
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
//                            deleteAllClosedTasksConfirmedPerformed(target);
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

    //region Task-level actions
    private void suspendTasksPerformed(AjaxRequestTarget target, IModel<TaskSelectableBean> bean) {
        List<TaskType> selectedTasks = new ArrayList<>();
        if (bean != null) {
            selectedTasks.add(bean.getObject().getValue());
        } else {
            selectedTasks = getObjectListPanel().getSelectedObjects();
        }
//        List<TaskDto> dtoList = WebComponentUtil.getSelectedData(getTaskTable());
//        if (!isSomeTaskSelected(dtoList, target)) {
//            return;
//        }
        if (selectedTasks.isEmpty()) {
            getSession().warn("PagetTasks.nothing.selected");
            target.add(getFeedbackPanel());
            return;
        }

        suspendTasksPerformed(target, selectedTasks);
    }

    private void suspendTasksPerformed(AjaxRequestTarget target, List<TaskType> dtoList) {
        Task opTask = createSimpleTask(TaskDtoTablePanel.OPERATION_SUSPEND_TASKS);
        OperationResult result = opTask.getResult();
        try {
            List<TaskType> plainTasks = dtoList.stream().filter(dto -> !isManageableTreeRoot(dto)).collect(Collectors.toList());
            List<TaskType> trees = dtoList.stream().filter(dto -> isManageableTreeRoot(dto)).collect(Collectors.toList());
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
        getObjectListPanel().refreshTable(TaskType.class, target);
    }

    private void resumeTasksPerformed(AjaxRequestTarget target, IModel<TaskSelectableBean> selectedTask) {

        List<TaskType> selectedTasks = new ArrayList<>();
        if (selectedTask != null) {
            selectedTasks.add(selectedTask.getObject().getValue());
        } else {
            selectedTasks = getObjectListPanel().getSelectedObjects();
        }

        if (selectedTasks.isEmpty()) {
            getSession().warn("PagetTasks.nothing.selected");
            target.add(getFeedbackPanel());
            return;
        }

        resumeTasksPerformed(target, selectedTasks);


    }
    private void resumeTasksPerformed(AjaxRequestTarget target, List<TaskType> dtoList) {
        Task opTask = createSimpleTask(TaskDtoTablePanel.OPERATION_RESUME_TASKS);
        OperationResult result = opTask.getResult();
        try {
            List<TaskType> plainTasks = dtoList.stream().filter(dto -> !isManageableTreeRoot(dto)).collect(Collectors.toList());
            List<TaskType> trees = dtoList.stream().filter(dto -> isManageableTreeRoot(dto)).collect(Collectors.toList());
            getTaskService().resumeTasks(TaskSelectableBean.getOids(plainTasks), opTask, result);
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
        getObjectListPanel().refreshTable(TaskType.class, target);
    }

    private boolean suspendPlainTasks(List<TaskType> plainTasks, OperationResult result, Task opTask)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        if (!plainTasks.isEmpty()) {
            return getTaskService().suspendTasks(TaskSelectableBean.getOids(plainTasks), PageTasks.WAIT_FOR_TASK_STOP, opTask, result);
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

    private void scheduleTasksPerformed(AjaxRequestTarget target, IModel<TaskSelectableBean> selectedTask) {
        List<String> oids = new ArrayList<>();
        if (selectedTask != null) {
            oids.add(selectedTask.getObject().getValue().getOid());
        } else {
            oids = TaskSelectableBean.getOids(getObjectListPanel().getSelectedObjects());
        }

        if (oids.isEmpty()) {
            getSession().warn("PagetTasks.nothing.selected");
            target.add(getFeedbackPanel());
            return;
        }

        scheduleTasksPerformed(target, oids);

    }

    private void scheduleTasksPerformed(AjaxRequestTarget target, List<String> oids) {
        Task opTask = createSimpleTask(TaskDtoTablePanel.OPERATION_SCHEDULE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().scheduleTasksNow(oids, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("pageTasks.message.scheduleTasksPerformed.success").getString());
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(createStringResource("pageTasks.message.scheduleTasksPerformed.fatalError").getString(), e);
        }
        showResult(result);

        //refresh feedback and table
        getObjectListPanel().refreshTable(TaskType.class, target);
    }

    private IModel<String> getTaskConfirmationMessageModel(ColumnMenuAction action, String actionName) {
        if (action.getRowModel() == null) {
            return createStringResource("pageTasks.message.confirmationMessageForMultipleTaskObject", actionName, 1);
//                    WebComponentUtil.getSelectedData(()).size());
        } else {
            String objectName = ((TaskSelectableBean) (action.getRowModel().getObject())).getValue().getName().getOrig();
            return createStringResource("pageTasks.message.confirmationMessageForSingleTaskObject", actionName, objectName);
        }

    }

    // must be static, otherwise JVM crashes (probably because of some wicket serialization issues)
    private static boolean isCoordinator(IModel<?> rowModel, boolean isHeader) {
        TaskSelectableBean dto = getDto(rowModel, isHeader);
        return dto != null && TaskTypeUtil.isCoordinator(dto.getValue());
    }


    // must be static, otherwise JVM crashes (probably because of some wicket serialization issues)
    private static boolean isManageableTreeRoot(IModel<?> rowModel, boolean isHeader) {
        TaskSelectableBean dto = getDto(rowModel, isHeader);
        return dto != null && isManageableTreeRoot(dto.getValue());
    }

    private static boolean isManageableTreeRoot(TaskType taskType) {
        return TaskTypeUtil.isCoordinator(taskType) || TaskTypeUtil.isPartitionedMaster(taskType);
    }

    protected boolean isProgressComputationEnabled() {
        return true;//searchModel.getObject().isShowProgress();
    }

    private static TaskSelectableBean getDto(IModel<?> rowModel, boolean isHeader) {
        if (rowModel != null && !isHeader) {
            Object object = rowModel.getObject();
            if (object instanceof TaskSelectableBean) {
                return (TaskSelectableBean) object;
            }
        }
        return null;
    }

    @Override
    public void refresh(AjaxRequestTarget target) {
        //refreshTasks(target);
    }

    @Override
    public Component getRefreshingBehaviorParent() {
        return getObjectListPanel();
    }

    @Override
    public int getRefreshInterval() {
        return REFRESH_INTERVAL;
    }
}
