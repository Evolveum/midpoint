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

import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.NodeExecutionStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.*;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstance;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.WorkflowService;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
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
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author lazyman
 */
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
    private static final String ID_SHOW_SUBTASKS = "showSubtasks";

    public PageTasks() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        OptionPanel option = new OptionPanel("option", createStringResource("pageTasks.optionsTitle"), true);
        mainForm.add(option);

        OptionItem diagnostics = new OptionItem("diagnostics", createStringResource("pageTasks.diagnostics"));
        option.getBodyContainer().add(diagnostics);

        OptionContent content = new OptionContent("optionContent");
        mainForm.add(content);

        DropDownChoice listSelect = new DropDownChoice("state", new Model(),
                new AbstractReadOnlyModel<List<TaskDtoExecutionStatusFilter>>() {

                    @Override
                    public List<TaskDtoExecutionStatusFilter> getObject() {
                        return createTypeList();
                    }
                },
                new EnumChoiceRenderer(PageTasks.this));
        listSelect.setMarkupId("stateSelect");
        listSelect.setNullValid(false);
        if (listSelect.getModel().getObject() == null) {
            listSelect.getModel().setObject(TaskDtoExecutionStatusFilter.ALL);
        }
        content.getBodyContainer().add(listSelect);

        DropDownChoice categorySelect = new DropDownChoice("category", new Model(),
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
        categorySelect.setMarkupId("categorySelect");
        categorySelect.setNullValid(false);
        if (categorySelect.getModel().getObject() == null) {
            categorySelect.getModel().setObject(ALL_CATEGORIES);
        }
        content.getBodyContainer().add(categorySelect);

        CheckBox showSubtasks = new CheckBox(ID_SHOW_SUBTASKS, new Model<Boolean>());
        content.getBodyContainer().add(showSubtasks);

        listSelect.add(createFilterAjaxBehaviour(listSelect.getModel(), categorySelect.getModel(), showSubtasks.getModel()));
        categorySelect.add(createFilterAjaxBehaviour(listSelect.getModel(), categorySelect.getModel(), showSubtasks.getModel()));
        showSubtasks.add(createFilterAjaxBehaviour(listSelect.getModel(), categorySelect.getModel(), showSubtasks.getModel()));

        List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();

        TaskDtoProviderOptions options = TaskDtoProviderOptions.fullOptions();
        options.setGetTaskParent(false);
        options.setRetrieveModelContext(false);
        options.setResolveOwnerRef(false);
        TaskDtoProvider provider = new TaskDtoProvider(PageTasks.this, options);

        provider.setQuery(createTaskQuery(null, null, false));      // show only root tasks
        TablePanel<TaskDto> taskTable = new TablePanel<TaskDto>("taskTable", provider,
                taskColumns);
        taskTable.setOutputMarkupId(true);
        content.getBodyContainer().add(taskTable);

        List<IColumn<NodeDto, String>> nodeColumns = initNodeColumns();
        TablePanel nodeTable = new TablePanel<NodeDto>("nodeTable", new NodeDtoProvider(PageTasks.this), nodeColumns);
        nodeTable.setOutputMarkupId(true);
        nodeTable.setShowPaging(false);
        content.getBodyContainer().add(nodeTable);

        initTaskButtons(mainForm);
        initNodeButtons(mainForm);
        initDiagnosticButtons(diagnostics);
    }

    private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour(final IModel<TaskDtoExecutionStatusFilter> status,
            final IModel<String> category, final IModel<Boolean> showSubtasks) {
        return new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                searchFilterPerformed(target, status, category, showSubtasks);
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

        return columns;
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

        columns.add(createTaskResultStatusColumn(this, "pageTasks.task.status"));
        return columns;
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
                parameters.add(PageTaskEdit.PARAM_TASK_EDIT_ID, oid);
                component.setResponsePage(new PageTaskEdit(parameters, component.getPage()));
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
                        parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_ID, task.getWorkflowProcessInstanceId());
                        parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_FINISHED, task.isWorkflowProcessInstanceFinished());
                        component.setResponsePage(new PageProcessInstance(parameters, component.getPage()));
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
            builder.append(createStringResource(task.getObjectRefType().getLocalizationKey()).getString());
            builder.append(")");
        }

        return builder.toString();
    }

    private String createScheduledToRunAgain(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();
        Long time = task.getScheduledToStartAgain();
        if (time == null) {
            return "";
        } else if (time == 0) {
            return getString("pageTasks.now");
        } else if (time == -1) {
            return getString("pageTasks.runsContinually");
        } else if (time == -2) {
            return getString("pageTasks.alreadyPassed");
        }

        //todo i18n
        return new StringResourceModel("pageTasks.in", this, null, null,
                DurationFormatUtils.formatDurationWords(time, true, true)).getString();
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

    private void initTaskButtons(Form mainForm) {
        AjaxLinkButton suspend = new AjaxLinkButton("suspendTask",
                createStringResource("pageTasks.button.suspendTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                suspendTasksPerformed(target);
            }
        };
        mainForm.add(suspend);

        AjaxLinkButton resume = new AjaxLinkButton("resumeTask",
                createStringResource("pageTasks.button.resumeTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                resumeTasksPerformed(target);
            }
        };
        mainForm.add(resume);

        AjaxLinkButton delete = new AjaxLinkButton("deleteTask", ButtonType.NEGATIVE, 
                createStringResource("pageTasks.button.deleteTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteTasksPerformed(target);
            }
        };
        mainForm.add(delete);

        AjaxLinkButton schedule = new AjaxLinkButton("scheduleTask",
                createStringResource("pageTasks.button.scheduleTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                scheduleTasksPerformed(target);
            }
        };
        mainForm.add(schedule);
    }

    private void initNodeButtons(Form mainForm) {
        AjaxLinkButton pause = new AjaxLinkButton("stopScheduler",
                createStringResource("pageTasks.button.stopScheduler")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                stopSchedulersPerformed(target);
            }
        };
        mainForm.add(pause);

        AjaxLinkButton stop = new AjaxLinkButton("stopSchedulerAndTasks",
                createStringResource("pageTasks.button.stopSchedulerAndTasks")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                stopSchedulersAndTasksPerformed(target);
            }
        };
        mainForm.add(stop);

        AjaxLinkButton start = new AjaxLinkButton("startScheduler",
                createStringResource("pageTasks.button.startScheduler")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                startSchedulersPerformed(target);
            }
        };
        mainForm.add(start);

        AjaxLinkButton delete = new AjaxLinkButton("deleteNode", ButtonType.NEGATIVE, 
                createStringResource("pageTasks.button.deleteNode")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteNodesPerformed(target);
            }
        };
        mainForm.add(delete);
    }

    private void initDiagnosticButtons(OptionItem item) {
        AjaxLinkButton deactivate = new AjaxLinkButton("deactivateServiceThreads",
                createStringResource("pageTasks.button.deactivateServiceThreads")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deactivateServiceThreadsPerformed(target);
            }
        };
        item.add(deactivate);

        AjaxLinkButton reactivate = new AjaxLinkButton("reactivateServiceThreads",
                createStringResource("pageTasks.button.reactivateServiceThreads")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                reactivateServiceThreadsPerformed(target);
            }
        };
        item.add(reactivate);

        AjaxLinkButton synchronize = new AjaxLinkButton("synchronizeTasks",
                createStringResource("pageTasks.button.synchronizeTasks")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                synchronizeTasksPerformed(target);
            }
        };
        item.add(synchronize);
    }

    private TablePanel getTaskTable() {
        OptionContent content = (OptionContent) get("mainForm:optionContent");
        return (TablePanel) content.getBodyContainer().get("taskTable");
    }

    private TablePanel getNodeTable() {
        OptionContent content = (OptionContent) get("mainForm:optionContent");
        return (TablePanel) content.getBodyContainer().get("nodeTable");
    }

    private List<String> createCategoryList() {
        List<String> categories = new ArrayList<String>();
        categories.add(ALL_CATEGORIES);

        TaskManager manager = getTaskManager();

        List<String> list = manager.getAllTaskCategories();
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

    private void suspendTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskTypeList = WebMiscUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskTypeList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_SUSPEND_TASKS);
        TaskManager taskManager = getTaskManager();
        List<Task> taskList = new ArrayList<Task>();
        try {
            for (TaskDto taskDto : taskTypeList) {
                Task task = taskManager.getTask(taskDto.getOid(), result);

                if (TaskExecutionStatus.SUSPENDED.equals(task.getExecutionStatus()) || TaskExecutionStatus.CLOSED.equals(task.getExecutionStatus())) {
                    warn(getString("pageTasks.message.alreadySuspended", task.getName()));
                } else {
                    taskList.add(task);
                }
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get information on tasks to be suspended.", ex);
        }

        boolean suspended = false;
        if (!result.isError()) {
            try {
                suspended = taskManager.suspendTasks(taskList, 2000L, result);
            } catch (Exception e) {
                result.recordFatalError("Couldn't suspend tasks.", e);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            if (suspended) {
                result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully suspended.");
            } else {
                result.recordWarning("Task(s) suspension has been successfully requested; please check for its completion using task list.");
            }
        }

        if (!result.isSuccess() || !taskList.isEmpty()) {
            showResult(result);
        }

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

        boolean atLeastOneToBeResumed = false;

        OperationResult mainResult = new OperationResult(OPERATION_RESUME_TASKS);
        TaskManager taskManager = getTaskManager();
        for (TaskDto taskDto : taskDtoList) {
            OperationResult result = mainResult.createSubresult(OPERATION_RESUME_TASK);
            try {
                Task task = taskManager.getTask(taskDto.getOid(), result);
                if (!TaskExecutionStatus.SUSPENDED.equals(task.getExecutionStatus())) {
                    warn(getString("pageTasks.message.alreadyResumed", task.getName()));
                } else {
                    atLeastOneToBeResumed = true;
                    taskManager.resumeTask(task, result);
                }
                result.recordSuccessIfUnknown();
            }
            // some situations (e.g. resume task that is not suspended) are recorded in OperationResult only (i.e. no exception)
            // ordinary exceptions (ObjectNotFoundException, SchemaException) are already recorded in the result (and an exception is thrown)
            // unexpected exceptions are not recorded in OperationResult, only the exception is thrown
            catch (ObjectNotFoundException e) {
                // see above
            } catch (SchemaException e) {
                // see above
            } catch (Exception e) {
                result.recordPartialError("Couldn't resume task due to an unexpected exception.", e);
            }
        }
        if (mainResult.isUnknown()) {
            mainResult.recomputeStatus();
        }

        if (mainResult.isSuccess()) {
            mainResult.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully resumed.");
        }

        if (!mainResult.isSuccess() || atLeastOneToBeResumed) {
            showResult(mainResult);
        } // otherwise, the warning has been issued and there's no point in displaying info about success in resuming tasks

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void nodeDetailsPerformed(AjaxRequestTarget target, String oid) {

    }

    private void deleteTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskTypeList = WebMiscUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskTypeList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_TASKS);
        TaskManager taskManager = getTaskManager();
        List<Task> taskList = new ArrayList<Task>();
        try {
            for (TaskDto taskDto : taskTypeList) {
                Task task = taskManager.getTask(taskDto.getOid(), result);
                taskList.add(task);
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get information on tasks to be deleted. Please try again.", ex);
        }

        if (!result.isError()) {
            try {
                taskManager.suspendTasks(taskList, 2000L, result);
            } catch (Exception e) {
                result.recordFatalError("Couldn't suspend tasks before deletion.", e);
            }

            for (Task task : taskList) {
                try {
                    taskManager.deleteTask(task.getOid(), result);
                } catch (ObjectNotFoundException e) {
                    // already stored in operation result
                } catch (SchemaException e) {
                    // already stored in operation result
                } catch (Exception e) {
                    result.createSubresult("deleteTask").recordPartialError("Couldn't delete task " + task.getName(), e);
                }
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully deleted.");
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
        TaskManager taskManager = getTaskManager();
        for (TaskDto taskDto : taskDtoList) {
            try {
                Task task = taskManager.getTask(taskDto.getOid(), result);
                taskManager.scheduleTaskNow(task, result);
            }
            // some situations (e.g. resume task that is not suspended) are recorded in OperationResult only (i.e. no exception)
            // ordinary exceptions (ObjectNotFoundException, SchemaException) are already recorded in the result (and an exception is thrown)
            // unexpected exceptions are not recorded in OperationResult, only the exception is thrown
            catch (ObjectNotFoundException e) {
                // see above
            } catch (SchemaException e) {
                // see above
            } catch (Exception e) {
                // only the last error is recorded but we don't care much (low probability of this case)
                result.recordPartialError("Couldn't schedule task due to an unexpected exception.", e);
            }
        }
        if (result.isUnknown()) {
            result.computeStatus();
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully scheduled.");
        }

        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());

    }

    private void stopSchedulersAndTasksPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebMiscUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_STOP_SCHEDULERS_AND_TASKS);

        TaskManager taskManager = getTaskManager();
        List<String> nodeList = new ArrayList<String>();
        for (NodeDto nodeDto : nodeDtoList) {
            nodeList.add(nodeDto.getNodeIdentifier());
        }

        boolean suspended = false;
        try {
            suspended = taskManager.stopSchedulersAndTasks(nodeList, 2000L, result);
        } catch (Exception e) {
            result.recordFatalError("Couldn't stop schedulers.", e);
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        //System.out.println("Result after recompute = " + result.dump());

        if (result.isSuccess()) {
            if (suspended) {
                result.recordStatus(OperationResultStatus.SUCCESS, "Selected node scheduler(s) have been successfully stopped, including tasks that were running on them.");
            } else {
                result.recordWarning("Selected node scheduler(s) have been successfully paused; however, some of the tasks they were executing are still running on them. Please check their completion using task list.");
            }
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
        TaskManager taskManager = getTaskManager();
        for (NodeDto nodeDto : nodeDtoList) {
            try {
                if (nodeDto.getExecutionStatus() == NodeExecutionStatus.ERROR) {
                    result.createSubresult("startScheduler").recordFatalError("Couldn't start the scheduler on node " + nodeDto.getName() + ", because the node is in error state.");
                } else {
                    taskManager.startScheduler(nodeDto.getNodeIdentifier(), result);
                }
            } catch (Exception e) {
                result.recordFatalError("Couldn't start the scheduler on node " + nodeDto.getName(), e);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "Selected node scheduler(s) have been successfully started.");
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
        TaskManager taskManager = getTaskManager();
        for (NodeDto nodeDto : nodeDtoList) {
            try {
                taskManager.stopScheduler(nodeDto.getNodeIdentifier(), result);
            } catch (Exception e) {
                result.recordFatalError("Couldn't stop the scheduler on node " + nodeDto.getName(), e);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "Selected node scheduler(s) have been successfully stopped.");
        }

        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void deactivateServiceThreadsPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_DEACTIVATE_SERVICE_THREADS);

        TaskManager taskManager = getTaskManager();
//        List<NodeDto> nodeDtoList = getSelectedNodes();
//
//        if (nodeDtoList.size() != 1 || !taskManager.getNodeId().equals(nodeDtoList.get(0).getNodeIdentifier())) {
//            error("Service threads can be deactivated on local node only.");
//            target.add(getFeedbackPanel());
//            return;
//        }

        boolean stopped = false;
        try {
            stopped = taskManager.deactivateServiceThreads(2000L, result);
        } catch (Exception e) {
            result.recordFatalError("Couldn't deactivate service threads on this node.");
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            if (stopped) {
                result.recordStatus(OperationResultStatus.SUCCESS, "Service threads on local node have been successfully deactivated.");
            } else {
                result.recordWarning("Deactivation of service threads on local node have been successfully requested; however, some of the tasks are still running. Please check their completion using task list.");
            }
        }

        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void reactivateServiceThreadsPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_REACTIVATE_SERVICE_THREADS);

        TaskManager taskManager = getTaskManager();
//        List<NodeDto> nodeDtoList = getSelectedNodes();
//
//        if (nodeDtoList.size() != 1 || !taskManager.getNodeId().equals(nodeDtoList.get(0).getNodeIdentifier())) {
//            error("Service threads can be reactivated on local node only.");
//            target.add(getFeedbackPanel());
//            return;
//        }

        try {
            taskManager.reactivateServiceThreads(result);
        } catch (Exception e) {
            result.recordFatalError("Couldn't reactivate service threads on local node.");
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "Service threads on local node have been successfully reactivated.");
        }

        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void synchronizeTasksPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SYNCHRONIZE_TASKS);

        TaskManager taskManager = getTaskManager();

        try {
            taskManager.synchronizeTasks(result);
        } catch (Exception e) {
            result.recordFatalError("Couldn't synchronize tasks.");
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
            if (result.isSuccess()) {       // brutal hack - the subresult's message contains statistics
                result.recordStatus(OperationResultStatus.SUCCESS, result.getLastSubresult().getMessage());
            }
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
        for (NodeDto nodeDto : nodeDtoList) {
            try {
                getMidpointApplication().getTaskManager().deleteNode(nodeDto.getNodeIdentifier(), result);
            } catch (Exception e) {
                result.recordFatalError("Couldn't delete the node " + nodeDto.getName(), e);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "Selected node(s) have been successfully deleted.");
        }

        NodeDtoProvider provider = (NodeDtoProvider) getNodeTable().getDataTable().getDataProvider();
        provider.clearCache();

        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
        target.add(getNodeTable());
    }

    private void searchFilterPerformed(AjaxRequestTarget target, IModel<TaskDtoExecutionStatusFilter> status,
            IModel<String> category, IModel<Boolean> showSubtasks) {

        ObjectQuery query = createTaskQuery(status.getObject(), category.getObject(), showSubtasks.getObject());

        TablePanel panel = getTaskTable();
        DataTable table = panel.getDataTable();
        TaskDtoProvider provider = (TaskDtoProvider) table.getDataProvider();
        provider.setQuery(query);
        table.setCurrentPage(0);

        target.add(getFeedbackPanel());
        target.add(getTaskTable());
    }

    private ObjectQuery createTaskQuery(TaskDtoExecutionStatusFilter status, String category, Boolean showSubtasks) {
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
                filters.add(EqualsFilter.createEqual(TaskType.class, getPrismContext(), TaskType.F_CATEGORY, category));
            }
            if (!Boolean.TRUE.equals(showSubtasks)) {
                filters.add(EqualsFilter.createEqual(TaskType.class, getPrismContext(), TaskType.F_PARENT, null));
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
}
