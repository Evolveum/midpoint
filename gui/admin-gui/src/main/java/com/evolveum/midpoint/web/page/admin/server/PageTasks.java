/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.server.dto.*;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstance;
import com.evolveum.midpoint.web.session.TasksStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.*;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/tasks", action = {
        @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#tasks",
                label = "PageTasks.auth.tasks.label",
                description = "PageTasks.auth.tasks.description")})
public class PageTasks extends PageAdminTasks {

    private static final Trace LOGGER = TraceManager.getTrace(PageTasks.class);
    private static final String DOT_CLASS = PageTasks.class.getName() + ".";
    private static final String OPERATION_SUSPEND_TASKS = DOT_CLASS + "suspendTasks";
    private static final String OPERATION_RESUME_TASKS = DOT_CLASS + "resumeTasks";
    private static final String OPERATION_RESUME_TASK = DOT_CLASS + "resumeTask";
    private static final String OPERATION_DELETE_TASKS = DOT_CLASS + "deleteTasks";
    private static final String OPERATION_SCHEDULE_TASKS = DOT_CLASS + "scheduleTasks";
    private static final String OPERATION_DELETE_NODES = DOT_CLASS + "deleteNodes";
    private static final String OPERATION_START_SCHEDULERS = DOT_CLASS + "startSchedulers";
    private static final String OPERATION_STOP_SCHEDULERS_AND_TASKS = DOT_CLASS + "stopSchedulersAndTasks";
    private static final String OPERATION_STOP_SCHEDULERS = DOT_CLASS + "stopSchedulers";
    private static final String OPERATION_DEACTIVATE_SERVICE_THREADS = DOT_CLASS + "deactivateServiceThreads";
    private static final String OPERATION_REACTIVATE_SERVICE_THREADS = DOT_CLASS + "reactivateServiceThreads";
    private static final String OPERATION_SYNCHRONIZE_TASKS = DOT_CLASS + "synchronizeTasks";
    private static final String ALL_CATEGORIES = "";

    public static final long WAIT_FOR_TASK_STOP = 2000L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_STATE = "state";
    private static final String ID_CATEGORY = "category";
    private static final String ID_SHOW_SUBTASKS = "showSubtasks";
    private static final String ID_TASK_TABLE = "taskTable";
    private static final String ID_NODE_TABLE = "nodeTable";
    private static final String ID_SEARCH_CLEAR = "searchClear";

    private IModel<TasksSearchDto> searchModel;

    public PageTasks() {
        searchModel = new LoadableModel<TasksSearchDto>(false) {

            @Override
            protected TasksSearchDto load() {
                return loadTasksSearchDto();
            }
        };
        initLayout();
    }

    private TasksSearchDto loadTasksSearchDto() {
        TasksStorage storage = getSessionStorage().getTasks();
        TasksSearchDto dto = storage.getTasksSearch();

        if(dto == null){
            dto = new TasksSearchDto();
            dto.setShowSubtasks(false);
        }

        if (dto.getStatus() == null) {
            dto.setStatus(TaskDtoExecutionStatusFilter.ALL);
        }

        return dto;
    }

    private void initLayout() {
        Form searchForm = new Form(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);
        add(searchForm);
        initSearchForm(searchForm);

        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();

        TaskDtoProviderOptions options = TaskDtoProviderOptions.fullOptions();
        options.setGetTaskParent(false);
        options.setRetrieveModelContext(false);
        options.setResolveOwnerRef(false);
        TaskDtoProvider provider = new TaskDtoProvider(PageTasks.this, options);

        provider.setQuery(createTaskQuery());
        TablePanel<TaskDto> taskTable = new TablePanel<>(ID_TASK_TABLE, provider, taskColumns,
                UserProfileStorage.TableId.PAGE_TASKS_PANEL);
        taskTable.setOutputMarkupId(true);
        mainForm.add(taskTable);

        List<IColumn<NodeDto, String>> nodeColumns = initNodeColumns();
        TablePanel nodeTable = new TablePanel<>(ID_NODE_TABLE, new NodeDtoProvider(PageTasks.this), nodeColumns);
        nodeTable.setOutputMarkupId(true);
        nodeTable.setShowPaging(false);
        mainForm.add(nodeTable);

        initDiagnosticButtons();
    }

    private void initSearchForm(Form searchForm) {
        DropDownChoice listSelect = new DropDownChoice(ID_STATE,
                new PropertyModel(searchModel, TasksSearchDto.F_STATUS),
                new AbstractReadOnlyModel<List<TaskDtoExecutionStatusFilter>>() {

                    @Override
                    public List<TaskDtoExecutionStatusFilter> getObject() {
                        return createTypeList();
                    }
                },
                new EnumChoiceRenderer(PageTasks.this));
        listSelect.add(createFilterAjaxBehaviour());
        listSelect.setOutputMarkupId(true);
        listSelect.setNullValid(false);
        if (listSelect.getModel().getObject() == null) {
            listSelect.getModel().setObject(TaskDtoExecutionStatusFilter.ALL);
        }
        searchForm.add(listSelect);

        DropDownChoice categorySelect = new DropDownChoice(ID_CATEGORY,
                new PropertyModel(searchModel, TasksSearchDto.F_CATEGORY),
                new AbstractReadOnlyModel<List<String>>() {

                    @Override
                    public List<String> getObject() {
                        return createCategoryList();
                    }
                },
                new IChoiceRenderer<String>() {

                    @Override
                    public Object getDisplayValue(String object) {
                        if (ALL_CATEGORIES.equals(object)) {
                            object = "AllCategories";
                        }
                        return PageTasks.this.getString("pageTasks.category." + object);
                    }

                    @Override
                    public String getIdValue(String object, int index) {
                        return Integer.toString(index);
                    }
                }
        );
        categorySelect.setOutputMarkupId(true);
        categorySelect.setNullValid(false);
        categorySelect.add(createFilterAjaxBehaviour());
        if (categorySelect.getModel().getObject() == null) {
            categorySelect.getModel().setObject(ALL_CATEGORIES);
        }
        searchForm.add(categorySelect);

        CheckBox showSubtasks = new CheckBox(ID_SHOW_SUBTASKS,
                new PropertyModel(searchModel, TasksSearchDto.F_SHOW_SUBTASKS));
        showSubtasks.add(createFilterAjaxBehaviour());
        searchForm.add(showSubtasks);

        AjaxSubmitButton clearButton = new AjaxSubmitButton(ID_SEARCH_CLEAR) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                clearSearchPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        searchForm.add(clearButton);
    }

    private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
        return new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                searchFilterPerformed(target);
            }
        };
    }

    private List<IColumn<NodeDto, String>> initNodeColumns() {
        List<IColumn<NodeDto, String>> columns = new ArrayList<IColumn<NodeDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<NodeDto>();
        columns.add(column);

        column = new LinkColumn<NodeDto>(createStringResource("pageTasks.node.name"), "name", "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<NodeDto> rowModel) {
                NodeDto node = rowModel.getObject();
                nodeDetailsPerformed(target, node.getOid());
            }
        };
        columns.add(column);

        columns.add(new EnumPropertyColumn<NodeDto>(createStringResource("pageTasks.node.executionStatus"),
                "executionStatus") {

            @Override
            protected String translate(Enum en) {
                return createStringResource(en).getString();
            }
        });

//        CheckBoxColumn check = new CheckBoxColumn(createStringResource("pageTasks.node.running"), "running");
//        check.setEnabled(false);
//        columns.add(check);

        // currently, name == identifier, so this is redundant
//        columns.add(new PropertyColumn(createStringResource("pageTasks.node.nodeIdentifier"), "nodeIdentifier"));

        columns.add(new PropertyColumn(createStringResource("pageTasks.node.managementPort"), "managementPort"));
        columns.add(new AbstractColumn<NodeDto, String>(createStringResource("pageTasks.node.lastCheckInTime")) {

            @Override
            public void populateItem(Item<ICellPopulator<NodeDto>> item, String componentId,
                                     final IModel<NodeDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createLastCheckInTime(rowModel);
                    }
                }));
            }
        });
        CheckBoxColumn check = new CheckBoxColumn(createStringResource("pageTasks.node.clustered"), "clustered");
        check.setEnabled(false);
        columns.add(check);
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.statusMessage"), "statusMessage"));

        columns.add(new InlineMenuHeaderColumn(createNodesInlineMenu()));

        return columns;
    }

    private List<InlineMenuItem> createNodesInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.stopScheduler"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        stopSchedulersPerformed(target);
                    }
                }));
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.stopSchedulerAndTasks"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        stopSchedulersAndTasksPerformed(target);
                    }
                }));
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.startScheduler"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        startSchedulersPerformed(target);
                    }
                }));
        items.add(new InlineMenuItem());
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteNode"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteNodesPerformed(target);
                    }
                }));

        return items;
    }

    private List<IColumn<TaskDto, String>> initTaskColumns() {
        List<IColumn<TaskDto, String>> columns = new ArrayList<IColumn<TaskDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<TaskType>();
        columns.add(column);

        column = createTaskNameColumn(this, "pageTasks.task.name");
        columns.add(column);

        columns.add(createTaskCategoryColumn(this, "pageTasks.task.category"));

        columns.add(new AbstractColumn<TaskDto, String>(createStringResource("pageTasks.task.objectRef")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                                     final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createObjectRef(rowModel);
                    }
                }));
            }
        });
        columns.add(createTaskExecutionStatusColumn(this, "pageTasks.task.execution"));
        columns.add(new PropertyColumn<TaskDto, String>(createStringResource("pageTasks.task.executingAt"), "executingAt"));
        columns.add(new AbstractColumn<TaskDto, String>(createStringResource("pageTasks.task.progress")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> cellItem, String componentId, final IModel<TaskDto> rowModel) {
                cellItem.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {
                    @Override
                    public Object getObject() {
                        return createProgress(rowModel);
                    }
                }));
            }
        });
        columns.add(new AbstractColumn<TaskDto, String>(createStringResource("pageTasks.task.currentRunTime")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                                     final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createCurrentRuntime(rowModel);
                    }
                }));
            }
        });
        columns.add(new AbstractColumn<TaskDto, String>(createStringResource("pageTasks.task.scheduledToRunAgain")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                                     final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createScheduledToRunAgain(rowModel);
                    }
                }));
            }
        });

        columns.add(new IconColumn<TaskDto>(createStringResource("pageTasks.task.status")){

            @Override
            protected  IModel<String> createTitleModel(final IModel<TaskDto> rowModel){

                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        TaskDto dto = rowModel.getObject();

                        if(dto != null && dto.getStatus() != null){
                            return createStringResourceStatic(PageTasks.this, dto.getStatus()).getString();
                        } else {
                            return createStringResourceStatic(PageTasks.this, OperationResultStatus.UNKNOWN).getString();
                        }
                    }
                };
            }

            @Override
            protected IModel<String> createIconModel(final IModel<TaskDto> rowModel){
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if(rowModel != null && rowModel.getObject() != null && rowModel.getObject().getStatus() != null){
                            return OperationResultStatusIcon.parseOperationalResultStatus(rowModel.getObject().getStatus().createStatusType()).getIcon();
                        } else
                            return OperationResultStatusIcon.UNKNOWN.getIcon();
                    }
                };
            }
        });

        columns.add(new InlineMenuHeaderColumn(createTasksInlineMenu()));

        return columns;
    }

    private List<InlineMenuItem> createTasksInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.suspendTask"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        suspendTasksPerformed(target);
                    }
                }));
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.resumeTask"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        resumeTasksPerformed(target);
                    }
                }));
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.scheduleTask"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        scheduleTasksPerformed(target);
                    }
                }));
        items.add(new InlineMenuItem());
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteTask"), false,
                new HeaderMenuAction(this) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteTasksPerformed(target);
                    }
                }));

        return items;
    }


    // used in SubtasksPanel as well
    public static IColumn createTaskNameColumn(final Component component, String label) {
        return new LinkColumn<TaskDto>(createStringResourceStatic(component, label), TaskDto.F_NAME, TaskDto.F_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<TaskDto> rowModel) {
                TaskDto task = rowModel.getObject();
                taskDetailsPerformed(target, task.getOid());
            }

            private void taskDetailsPerformed(AjaxRequestTarget target, String oid) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                component.setResponsePage(new PageTaskEdit(parameters, (PageBase) component.getPage()));
            }

        };
    }

    public static AbstractColumn<TaskDto, String> createTaskCategoryColumn(final Component component, String label) {
        return new AbstractColumn<TaskDto, String>(createStringResourceStatic(component, label)) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                                     final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createStringResourceStatic(component, "pageTasks.category." + rowModel.getObject().getCategory()).getString();
                    }
                }));
            }
        };
    }

    public static EnumPropertyColumn createTaskResultStatusColumn(final Component component, String label) {
        return new EnumPropertyColumn(createStringResourceStatic(component, label), "status") {

            @Override
            protected String translate(Enum en) {
                return createStringResourceStatic(component, en).getString();
            }
        };
    }

    public static EnumPropertyColumn<TaskDto> createTaskExecutionStatusColumn(final Component component, String label) {
        return new EnumPropertyColumn<TaskDto>(createStringResourceStatic(component, label), "execution") {

            @Override
            protected String translate(Enum en) {
                return createStringResourceStatic(component, en).getString();
            }
        };
    }

    public static IColumn createTaskDetailColumn(final Component component, String label, boolean workflowsEnabled) {

        if (workflowsEnabled) {

            return new LinkColumn<TaskDto>(createStringResourceStatic(component, label), TaskDto.F_WORKFLOW_LAST_DETAILS) {

                @Override
                public void onClick(AjaxRequestTarget target, IModel<TaskDto> rowModel) {
                    TaskDto task = rowModel.getObject();
                    taskDetailsPerformed(target, task);
                }

                // todo display a message if process instance cannot be found
                private void taskDetailsPerformed(AjaxRequestTarget target, TaskDto task) {
                    if (task.getWorkflowProcessInstanceId() != null) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, task.getWorkflowProcessInstanceId());
                        parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_FINISHED, task.isWorkflowProcessInstanceFinished());
                        component.setResponsePage(new PageProcessInstance(parameters, (PageBase) component.getPage()));
                    }
                }

            };
        } else {
            return new PropertyColumn(createStringResourceStatic(component, label), TaskDto.F_WORKFLOW_LAST_DETAILS);
        }
    }

    private String createObjectRef(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();

        StringBuilder builder = new StringBuilder();
        if (task.getObjectRef() == null) {
            return "";
        }

        if (StringUtils.isNotEmpty(task.getObjectRefName())) {
            builder.append(task.getObjectRefName());
        } else {
            //builder.append(createStringResource("pageTasks.unknownRefName").getString());
            builder.append(task.getObjectRef().getOid());
        }
        if (task.getObjectRefType() != null) {
            builder.append(" (");

            ObjectTypeGuiDescriptor descr = ObjectTypeGuiDescriptor.getDescriptor(task.getObjectRefType());
            String key = descr != null ? descr.getLocalizationKey() : ObjectTypeGuiDescriptor.ERROR_LOCALIZATION_KEY;
            builder.append(createStringResource(key).getString());

            builder.append(")");
        }

        return builder.toString();
    }

    private String createScheduledToRunAgain(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();
        Long time = task.getScheduledToStartAgain();

        boolean runnable = task.getRawExecutionStatus() == TaskExecutionStatus.RUNNABLE;

        if (time == null) {
            return "";
        } else if (time == 0) {
            return getString(runnable ? "pageTasks.now" : "pageTasks.nowForNotRunningTasks");
        } else if (time == -1) {
            return getString("pageTasks.runsContinually");
        } else if (time == -2) {
            return getString(runnable ? "pageTasks.alreadyPassed" : "pageTasks.alreadyPassedForNotRunningTasks");
        }

        String key = runnable ? "pageTasks.in" : "pageTasks.inForNotRunningTasks";

        //todo i18n
        return new StringResourceModel(key, this, null, null,
                DurationFormatUtils.formatDurationWords(time, true, true)).getString();
    }

    private String createProgress(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();

        if (task.getStalledSince() != null) {
            return getString("pageTasks.stalledSince", new Date(task.getStalledSince()).toLocaleString(), task.getProgressDescription());
        } else {
            return task.getProgressDescription();
        }
    }

    private String createCurrentRuntime(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();

        if (task.getRawExecutionStatus() == TaskExecutionStatus.CLOSED) {

            //todo i18n and proper date/time formatting
            Long time = task.getCompletionTimestamp();
            if (time == null) {
                return "";
            }
            return "closed at " + new Date(time).toLocaleString();

        } else {

            Long time = task.getCurrentRuntime();
            if (time == null) {
                return "";
            }

            //todo i18n
            return DurationFormatUtils.formatDurationWords(time, true, true);
        }
    }


    private String createLastCheckInTime(IModel<NodeDto> nodeModel) {
        NodeDto node = nodeModel.getObject();
        Long time = node.getLastCheckInTime();
        if (time == null || time == 0) {
            return "";
        }

        //todo i18n
        return DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - time, true, true)
                + " ago";
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
    }

    private TablePanel getTaskTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_TASK_TABLE));
    }

    private TablePanel getNodeTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_NODE_TABLE));
    }

    private List<String> createCategoryList() {
        List<String> categories = new ArrayList<String>();
        categories.add(ALL_CATEGORIES);

        List<String> list = getTaskService().getAllTaskCategories();
        if (list != null) {
            categories.addAll(list);
            Collections.sort(categories);
        }

        return categories;
    }

    private List<TaskDtoExecutionStatusFilter> createTypeList() {
        List<TaskDtoExecutionStatusFilter> list = new ArrayList<TaskDtoExecutionStatusFilter>();

        Collections.addAll(list, TaskDtoExecutionStatusFilter.values());

        return list;
    }

    private boolean isSomeTaskSelected(List<TaskDto> tasks, AjaxRequestTarget target) {
        if (!tasks.isEmpty()) {
            return true;
        }

        warn(getString("pageTasks.message.noTaskSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    private boolean isSomeNodeSelected(List<NodeDto> nodes, AjaxRequestTarget target) {
        if (!nodes.isEmpty()) {
            return true;
        }

        warn(getString("pageTasks.message.noNodeSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    //region Task-level actions
    private void suspendTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskTypeList = WebMiscUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskTypeList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SUSPEND_TASKS);
        try {
            boolean suspended = getTaskService().suspendTasks(TaskDto.getOids(taskTypeList), WAIT_FOR_TASK_STOP, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (suspended) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully suspended.");
                } else {
                    result.recordWarning("Task(s) suspension has been successfully requested; please check for its completion using task list.");
                }
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't suspend the task(s) due to an unexpected exception", e);
        }
        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void resumeTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskDtoList = WebMiscUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_RESUME_TASKS);
        try {
            getTaskService().resumeTasks(TaskDto.getOids(taskDtoList), result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully resumed.");
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't resume the task(s) due to an unexpected exception", e);
        }
        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void deleteTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskDtoList = WebMiscUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_TASKS);
        try {
            getTaskService().suspendAndDeleteTasks(TaskDto.getOids(taskDtoList), WAIT_FOR_TASK_STOP, true, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully deleted.");
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't delete the task(s) because of an unexpected exception", e);
        }
        showResult(result);

        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
        provider.clearCache();

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void scheduleTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskDtoList = WebMiscUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SCHEDULE_TASKS);
        try {
            getTaskService().scheduleTasksNow(TaskDto.getOids(taskDtoList), result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully scheduled.");
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't schedule the task(s) due to an unexpected exception.", e);
        }
        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }
    //endregion

    //region Node-level actions
    private void nodeDetailsPerformed(AjaxRequestTarget target, String oid) {

    }

    private void stopSchedulersAndTasksPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebMiscUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_STOP_SCHEDULERS_AND_TASKS);
        try {
            boolean suspended = getTaskService().stopSchedulersAndTasks(NodeDto.getNodeIdentifiers(nodeDtoList), WAIT_FOR_TASK_STOP, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (suspended) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "Selected node scheduler(s) have been successfully stopped, including tasks that were running on them.");
                } else {
                    result.recordWarning("Selected node scheduler(s) have been successfully paused; however, some of the tasks they were executing are still running on them. Please check their completion using task list.");
                }
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't stop schedulers due to an unexpected exception.", e);
        }
        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void startSchedulersPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebMiscUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_START_SCHEDULERS);
        try {
            getTaskService().startSchedulers(NodeDto.getNodeIdentifiers(nodeDtoList), result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "Selected node scheduler(s) have been successfully started.");
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't start the scheduler(s) because of unexpected exception.", e);
        }

        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void stopSchedulersPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebMiscUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_STOP_SCHEDULERS);
        try {
            getTaskService().stopSchedulers(NodeDto.getNodeIdentifiers(nodeDtoList), result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "Selected node scheduler(s) have been successfully stopped.");
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't stop the scheduler(s) because of unexpected exception.", e);
        }
        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void deleteNodesPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebMiscUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_NODES);

        Task task = createSimpleTask(OPERATION_DELETE_NODES);

        for (NodeDto nodeDto : nodeDtoList) {
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
            deltas.add(ObjectDelta.createDeleteDelta(NodeType.class, nodeDto.getOid(), getPrismContext()));
            try {
                getModelService().executeChanges(deltas, null, task, result);
            } catch (Exception e) {     // until java 7 we do it in this way
                result.recordFatalError("Couldn't delete the node " + nodeDto.getNodeIdentifier(), e);
            }
        }

        result.computeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "Selected node(s) have been successfully deleted.");
        }
        showResult(result);

        NodeDtoProvider provider = (NodeDtoProvider) getNodeTable().getDataTable().getDataProvider();
        provider.clearCache();

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }
    //endregion

    //region Diagnostics actions
    private void deactivateServiceThreadsPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_DEACTIVATE_SERVICE_THREADS);

        try {
            boolean stopped = getTaskService().deactivateServiceThreads(WAIT_FOR_TASK_STOP, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (stopped) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "Service threads on local node have been successfully deactivated.");
                } else {
                    result.recordWarning("Deactivation of service threads on local node have been successfully requested; however, some of the tasks are still running. Please check their completion using task list.");
                }
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't deactivate service threads on this node because of an unexpected exception.", e);
        }
        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void reactivateServiceThreadsPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_REACTIVATE_SERVICE_THREADS);

        try {
            getTaskService().reactivateServiceThreads(result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "Service threads on local node have been successfully reactivated.");
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't reactivate service threads on local node because of an unexpected exception.", e);
        }
        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void synchronizeTasksPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SYNCHRONIZE_TASKS);

        try {
            getTaskService().synchronizeTasks(result);
            result.computeStatus();
            if (result.isSuccess()) {       // brutal hack - the subresult's message contains statistics
                result.recordStatus(OperationResultStatus.SUCCESS, result.getLastSubresult().getMessage());
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't synchronize tasks because of an unexpected exception.", e);
        }
        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }
    //endregion

    private void searchFilterPerformed(AjaxRequestTarget target) {
        TasksSearchDto dto = searchModel.getObject();

//        ObjectQuery query = createTaskQuery(dto.getStatus(), dto.getCategory(), dto.isShowSubtasks());
        ObjectQuery query = createTaskQuery();

        TablePanel panel = getTaskTable();
        DataTable table = panel.getDataTable();
        TaskDtoProvider provider = (TaskDtoProvider) table.getDataProvider();
        provider.setQuery(query);
        table.setCurrentPage(0);

        TasksStorage storage = getSessionStorage().getTasks();
        storage.setTasksSearch(dto);

        target.add(getFeedbackPanel());
        target.add(getTaskTable());
    }

    private ObjectQuery createTaskQuery(){
        TasksSearchDto dto = searchModel.getObject();
        TaskDtoExecutionStatusFilter status = dto.getStatus();
        String category = dto.getCategory();
        boolean showSubtasks = dto.isShowSubtasks();

        ObjectQuery query = null;
        try {
            List<ObjectFilter> filters = new ArrayList<ObjectFilter>();
            if (status != null) {
                ObjectFilter filter = status.createFilter(TaskType.class, getPrismContext());
                if (filter != null) {
                    filters.add(filter);
                }
            }
            if (category != null && !ALL_CATEGORIES.equals(category)) {
                filters.add(EqualFilter.createEqual(TaskType.F_CATEGORY, TaskType.class, getPrismContext(), null, category));
            }
            if (!Boolean.TRUE.equals(showSubtasks)) {
                filters.add(EqualFilter.createEqual(TaskType.F_PARENT, TaskType.class, getPrismContext(), null));
            }
            if (!filters.isEmpty()) {
                query = new ObjectQuery().createObjectQuery(AndFilter.createAnd(filters));
            }
        } catch (Exception ex) {
            error(getString("pageTasks.message.couldntCreateQuery") + " " + ex.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create task filter", ex);
        }
        return query;
    }

    private void clearSearchPerformed(AjaxRequestTarget target){
        searchModel.setObject(new TasksSearchDto());

        TablePanel panel = getTaskTable();
        DataTable table = panel.getDataTable();
        TaskDtoProvider provider = (TaskDtoProvider) table.getDataProvider();
        provider.setQuery(null);

        TasksStorage storage = getSessionStorage().getTasks();
        storage.setTasksSearch(searchModel.getObject());
        panel.setCurrentPage(storage.getTasksPaging());

        target.add(get(ID_SEARCH_FORM));
        target.add(panel);
    }
}
