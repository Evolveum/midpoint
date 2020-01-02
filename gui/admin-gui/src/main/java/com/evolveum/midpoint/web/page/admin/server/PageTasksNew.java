package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.*;
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
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.TaskSelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.page.admin.server.dto.*;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Collections;
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
public class PageTasksNew extends PageAdminObjectList {

    private static final transient Trace LOGGER = TraceManager.getTrace(PageTasksNew.class);

    private static final String DOT_CLASS = PageTasksNew.class.getName() + ".";
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

    public static final long WAIT_FOR_TASK_STOP = 2000L;

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
            public TaskSelectableBean createTaskDto(PrismObject<TaskType> task, Task opTask, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
                TaskSelectableBean bean = super.createTaskDto(task, opTask, result);
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
        return ((TaskSelectableBean)rowModel.getObject()).getValue().getOid() != null;
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
            return new AbstractExportableColumn<>(createStringResource("pageTasks.task.category"), TaskType.F_CATEGORY.getLocalPart()) {

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

        columns.add(new ObjectReferenceColumn<>(createStringResource("pageTasks.task.objectRef"), TaskSelectableBean.F_VALUE+"."+TaskType.F_OBJECT_REF.getLocalPart()){
            @Override
            public IModel<ObjectReferenceType> extractDataModel(IModel<TaskSelectableBean> rowModel) {
                TaskSelectableBean bean = rowModel.getObject();
                return Model.of(bean.getValue().getObjectRef());

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
        columns.add(new AbstractExportableColumn<>(createStringResource("pageTasks.task.scheduledToRunAgain")) {

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

        columns.add(new IconColumn<>(createStringResource("pageTasks.task.status"), TaskType.F_RESULT_STATUS.getLocalPart()) {

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

    public EnumPropertyColumn<TaskSelectableBean> createTaskExecutionStatusColumn() {
        return new EnumPropertyColumn<>(createStringResource("pageTasks.task.execution"), TaskType.F_EXECUTION_STATUS.getLocalPart(), TaskSelectableBean.F_VALUE + ".executionStatus") {

            @Override
            protected String translate(Enum en) {
                return PageTasksNew.this.createStringResource(en).getString();
            }
        };
    }

    public AbstractExportableColumn<TaskSelectableBean, String> createProgressColumn(String titleKey,
                                                                                               SerializableSupplier<Boolean> progressComputationEnabledSupplier) {
        return new AbstractExportableColumn<>(createStringResource(titleKey)) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskSelectableBean>> cellItem, String componentId, final IModel<TaskSelectableBean> rowModel) {
                if (!TaskTypeUtil.isPartitionedMaster(rowModel.getObject().getValue())) {
                    cellItem.add(new Label(componentId,
                            (IModel<Object>) () -> getProgressDescription(rowModel.getObject(),
                                    progressComputationEnabledSupplier.get())));
                } else {
                    cellItem.add(new LinkPanel(componentId, createStringResource("PageTasks.show.child.progress")) {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            PageParameters pageParams = new PageParameters();
                            pageParams.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getValue().getOid());
                            navigateToNext(PageTaskEdit.class, pageParams);
                        }
                    });
                }

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
        if (TaskTypeUtil.isPartitionedMaster(task.getValue())) {
            return "N/A";//getPartitionedTaskProgressDescription(task, alwaysCompute);
        } else if (isWorkStateHolder(task)) {
            return getBucketedTaskProgressDescription(task.getValue());
        } else {
            return getPlainTaskProgressDescription(task.getValue());
        }
    }

    //TODO why?
    public TaskExecutionStatus getRawExecutionStatus(TaskType taskType) {
        return TaskExecutionStatus.fromTaskType(taskType.getExecutionStatus());
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
                return new ColumnMenuAction<TaskSelectableBean>() {
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
                return new ColumnMenuAction<TaskSelectableBean>() {
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
                return new ColumnMenuAction<TaskSelectableBean>() {
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
                return new ColumnMenuAction<TaskSelectableBean>() {
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
                return new ColumnMenuAction<TaskSelectableBean>() {
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
                return new ColumnMenuAction<TaskSelectableBean>() {
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

    //region Task-level actions
    private void suspendTasksPerformed(AjaxRequestTarget target, IModel<TaskSelectableBean> selectedTask) {
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
        getObjectListPanel().refreshTable(TaskType.class, target);
    }

    private void resumeTasksPerformed(AjaxRequestTarget target, IModel<TaskSelectableBean> selectedTask) {
        List<TaskType> selectedTasks = getSelectedTasks(target, selectedTask);
        if (selectedTasks == null) {
            return;
        }
        Task opTask = createSimpleTask(OPERATION_RESUME_TASKS);
        OperationResult result = opTask.getResult();
        try {
            List<TaskType> plainTasks = selectedTasks.stream().filter(dto -> !isManageableTreeRoot(dto)).collect(Collectors.toList());
            List<TaskType> trees = selectedTasks.stream().filter(dto -> isManageableTreeRoot(dto)).collect(Collectors.toList());
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

    private List<TaskType> getSelectedTasks(AjaxRequestTarget target, IModel<TaskSelectableBean> selectedTask) {
        List<TaskType> selectedTasks = new ArrayList<>();
        if (selectedTask != null) {
            selectedTasks.add(selectedTask.getObject().getValue());
        } else {
            selectedTasks = getObjectListPanel().getSelectedObjects();
        }

        if (selectedTasks.isEmpty()) {
            getSession().warn("PagetTasks.nothing.selected");
            target.add(getFeedbackPanel());
            return null;
        }

        return selectedTasks;

    }

    private void scheduleTasksPerformed(AjaxRequestTarget target, IModel<TaskSelectableBean> selectedTask) {

        List<TaskType> selectedTasks = getSelectedTasks(target, selectedTask);
        if (selectedTasks == null) {
            return;
        }
        Task opTask = createSimpleTask(OPERATION_SCHEDULE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().scheduleTasksNow(TaskSelectableBean.getOids(selectedTasks), opTask, result);
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

    private void deleteTaskConfirmedPerformed(AjaxRequestTarget target, IModel<TaskSelectableBean> task) {
        List<TaskType> selectedTasks = getSelectedTasks(target, task);
        if (selectedTasks == null) {
            return;
        }

        Task opTask = createSimpleTask(OPERATION_DELETE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().suspendAndDeleteTasks(TaskSelectableBean.getOids(selectedTasks), WAIT_FOR_TASK_STOP, true, opTask, result);
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

//        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
//        provider.clearCache();

        // refresh feedback and table
        getObjectListPanel().refreshTable(TaskType.class, target);
//        refreshTable(target);
    }

    private void reconcileWorkersConfirmedPerformed(AjaxRequestTarget target, @NotNull IModel<TaskSelectableBean> task) {
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

        getObjectListPanel().refreshTable(TaskType.class, target);
    }

    private void suspendRootOnly(AjaxRequestTarget target, @NotNull IModel<TaskSelectableBean> task) {
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

        getObjectListPanel().refreshTable(TaskType.class, target);
    }

    private void resumeRootOnly(AjaxRequestTarget target, @NotNull IModel<TaskSelectableBean> task) {
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

        getObjectListPanel().refreshTable(TaskType.class, target);
    }

    private void deleteWorkersAndWorkState(AjaxRequestTarget target, @NotNull IModel<TaskSelectableBean> task) {
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

        getObjectListPanel().refreshTable(TaskType.class, target);
    }

    private void deleteWorkState(AjaxRequestTarget target, @NotNull IModel<TaskSelectableBean> task) {
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

        getObjectListPanel().refreshTable(TaskType.class, target);
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

}
