/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.admin.server.dto.*;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class PageTasks extends PageAdminTasks {

    private static final String DOT_CLASS = PageTasks.class.getSimpleName() + ".";
    private static final String OPERATION_SUSPEND_TASK = DOT_CLASS + "suspendTask";
    private static final String OPERATION_RESUME_TASK = DOT_CLASS + "resumeTask";
    private static final String OPERATION_DELETE_TASK = DOT_CLASS + "deleteTask";
    private static final String OPERATION_SCHEDULE_TASK = DOT_CLASS + "scheduleTask";
    private static final String OPERATION_DELETE_NODE = DOT_CLASS + "deleteNode";
    private static final String OPERATION_START_SCHEDULER = DOT_CLASS + "startScheduler";
    private static final String OPERATION_STOP_SCHEDULER = DOT_CLASS + "stopScheduler";
    private static final String OPERATION_PAUSE_SCHEDULER = DOT_CLASS + "pauseScheduler";

    public PageTasks() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        DropDownChoice listSelect = new DropDownChoice("state", new Model(TaskListType.ALL),
                new AbstractReadOnlyModel<List<TaskListType>>() {

                    @Override
                    public List<TaskListType> getObject() {
                        return createTypeList();
                    }
                },
                new EnumChoiceRenderer(PageTasks.this));
        mainForm.add(listSelect);

        DropDownChoice categorySelect = new DropDownChoice("category", new Model(),
                new AbstractReadOnlyModel<List<String>>() {

                    @Override
                    public List<String> getObject() {
                        return createCategoryList();
                    }
                });
        mainForm.add(categorySelect);

        List<IColumn<TaskDto>> taskColumns = initTaskColumns();
        TablePanel<TaskDto> taskTable = new TablePanel<TaskDto>("taskTable", new TaskDtoProvider(PageTasks.this),
                taskColumns);
        taskTable.setOutputMarkupId(true);
        mainForm.add(taskTable);

        List<IColumn<NodeDto>> nodeColumns = initNodeColumns();
        TablePanel nodeTable = new TablePanel<NodeDto>("nodeTable", new NodeDtoProvider(PageTasks.this), nodeColumns);
        nodeTable.setOutputMarkupId(true);
        nodeTable.setShowPaging(false);
        mainForm.add(nodeTable);

        initTaskButtons(mainForm);
        initSchedulerButtons(mainForm);
        initNodeButtons(mainForm);
    }

    private List<IColumn<NodeDto>> initNodeColumns() {
        List<IColumn<NodeDto>> columns = new ArrayList<IColumn<NodeDto>>();

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

        CheckBoxColumn check = new CheckBoxColumn(createStringResource("pageTasks.node.running"), "running");
        check.setEnabled(false);
        columns.add(check);
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.nodeIdentifier"), "nodeIdentifier"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.hostname"), "hostname"));
        columns.add(new AbstractColumn<NodeDto>(createStringResource("pageTasks.node.lastCheckInTime")) {

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
        check = new CheckBoxColumn(createStringResource("pageTasks.node.clustered"), "clustered");
        check.setEnabled(false);
        columns.add(check);
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.statusMessage"), "statusMessage"));

        return columns;
    }

    private List<IColumn<TaskDto>> initTaskColumns() {
        List<IColumn<TaskDto>> columns = new ArrayList<IColumn<TaskDto>>();

        IColumn column = new CheckBoxHeaderColumn<TaskType>();
        columns.add(column);

        column = new LinkColumn<TaskDto>(createStringResource("pageTasks.task.name"), "name", "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<TaskDto> rowModel) {
                TaskDto task = rowModel.getObject();
                taskDetailsPerformed(target, task.getOid());
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("pageTasks.task.category"), "category"));
        columns.add(new AbstractColumn<TaskDto>(createStringResource("pageTasks.task.objectRef")) {

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
        columns.add(new EnumPropertyColumn<TaskDto>(createStringResource("pageTasks.task.execution"), "execution") {

            @Override
            protected String translate(Enum en) {
                return createStringResource(en).getString();
            }
        });
        columns.add(new PropertyColumn<TaskDto>(createStringResource("pageTasks.task.executingAt"), "executingAt"));
        columns.add(new AbstractColumn<TaskDto>(createStringResource("pageTasks.task.currentRunTime")) {

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
        columns.add(new AbstractColumn<TaskDto>(createStringResource("pageTasks.task.scheduledToRunAgain")) {

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

        columns.add(new EnumPropertyColumn(createStringResource("pageTasks.task.status"), "status") {

            @Override
            protected String translate(Enum en) {
                return createStringResource(en).getString();
            }
        });

        return columns;
    }

    private String createObjectRef(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();

        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(task.getObjectRefName())) {
            builder.append(task.getObjectRefName());
        } else {
            builder.append(createStringResource("pageTasks.unknownRefName").getString());
            if (task.getObjectRef() != null) {
                builder.append("(");
                builder.append(task.getObjectRef().getOid());
                builder.append(")");
            }
        }
        if (task.getObjectRefType() != null) {
            builder.append("(");
            builder.append(createStringResource(task.getObjectRefType()).getString());
            builder.append(")");
        }

        return builder.toString();
    }

    private String createScheduledToRunAgain(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();
        //todo i18n
        Long time = task.getCurrentRuntime();
        if (time == null) {
            return "-";
        } else if (time == 0) {
            return "now";
        } else if (time == -1) {
            return "runs continually";
        }

        return "in " + DurationFormatUtils.formatDurationWords(time, true, true);
    }

    private String createCurrentRuntime(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();
        //todo i18n
        Long time = task.getScheduledToStartAgain();
        if (time == null) {
            return "-";
        }

        return DurationFormatUtils.formatDurationWords(time, true, true);
    }

    private String createLastCheckInTime(IModel<NodeDto> nodeModel) {
        NodeDto node = nodeModel.getObject();
        //todo i18n
        Long time = node.getLastCheckInTime();
        if (time == null || time == 0) {
            return "-";
        }

        return DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - time, true, true) + " ago";
    }

    private void initTaskButtons(Form mainForm) {
        AjaxLinkButton suspend = new AjaxLinkButton("suspendTask",
                createStringResource("pageTasks.button.suspendTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                suspendTaskPerformed(target);
            }
        };
        mainForm.add(suspend);

        AjaxLinkButton resume = new AjaxLinkButton("resumeTask",
                createStringResource("pageTasks.button.resumeTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                resumeTaskPerformed(target);
            }
        };
        mainForm.add(resume);

        AjaxLinkButton delete = new AjaxLinkButton("deleteTask",
                createStringResource("pageTasks.button.deleteTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteTaskPerformed(target);
            }
        };
        mainForm.add(delete);

        AjaxLinkButton schedule = new AjaxLinkButton("scheduleTask",
                createStringResource("pageTasks.button.scheduleTask")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                scheduleTaskPerformed(target);
            }
        };
        mainForm.add(schedule);
    }

    private void initNodeButtons(Form mainForm) {
        AjaxLinkButton deactivate = new AjaxLinkButton("deleteNode",
                createStringResource("pageTasks.button.deleteNode")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteNodePerformed(target);
            }
        };
        mainForm.add(deactivate);
    }

    private void initSchedulerButtons(Form mainForm) {
        AjaxLinkButton stop = new AjaxLinkButton("stopScheduler",
                createStringResource("pageTasks.button.stopScheduler")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                stopSchedulerPerformed(target);
            }
        };
        mainForm.add(stop);

        AjaxLinkButton start = new AjaxLinkButton("startScheduler",
                createStringResource("pageTasks.button.startScheduler")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                startSchedulerPerformed(target);
            }
        };
        mainForm.add(start);

        AjaxLinkButton pause = new AjaxLinkButton("pauseScheduler",
                createStringResource("pageTasks.button.pauseScheduler")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                pauseSchedulerPerformed(target);
            }
        };
        mainForm.add(pause);
    }

    private TablePanel getTaskTable() {
        return (TablePanel) get("mainForm:taskTable");
    }

    private TablePanel getNodeTable() {
        return (TablePanel) get("mainForm:nodeTable");
    }

    private List<TaskDto> getSelectedTasks() {
        DataTable table = getTaskTable().getDataTable();
        TaskDtoProvider provider = (TaskDtoProvider) table.getDataProvider();

        List<TaskDto> selected = new ArrayList<TaskDto>();
        for (TaskDto row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row);
            }
        }

        return selected;
    }

    private List<NodeDto> getSelectedNodes() {
        DataTable table = getNodeTable().getDataTable();
        NodeDtoProvider provider = (NodeDtoProvider) table.getDataProvider();

        List<NodeDto> selected = new ArrayList<NodeDto>();
        for (NodeDto row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row);
            }
        }

        return selected;
    }

    private List<String> createCategoryList() {
        List<String> categories = new ArrayList<String>();
        TaskManager manager = getTaskManager();

        List<String> list = manager.getAllTaskCategories();
        if (list != null) {
            categories.addAll(list);
            Collections.sort(list);
        }
        //todo i18n

        return categories;
    }

    private List<TaskListType> createTypeList() {
        List<TaskListType> list = new ArrayList<TaskListType>();
        //todo probably reimplement
        Collections.addAll(list, TaskListType.values());

        return list;
    }

    private void taskDetailsPerformed(AjaxRequestTarget target, String oid) {
        //useful methods :)

//        TaskManager manager = getTaskManager();
//        getSelectedTasks();
//        getSelectedNodes();

        //todo implement
    }

    private void suspendTaskPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SUSPEND_TASK);

        TaskManager taskManager = getTaskManager();
        List<TaskDto> taskTypeList = getSelectedTasks();
        List<Task> taskList = new ArrayList<Task>();
        try {
            for (TaskDto taskDto : taskTypeList) {
                Task task = taskManager.getTask(taskDto.getOid(), result);
                taskList.add(task);
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
                success("The task(s) have been successfully suspended.");
            } else {
                warn("Task(s) suspension has been successfully requested; please check for its completion using task list.");
            }
        } else {
            showResult(result);
        }

        System.out.println(result.dump());

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
    }

    /*
     * TODO: error handling is quite chaotic now... it should be standardized somehow
     */
    private void resumeTaskPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_RESUME_TASK);

        TaskManager taskManager = getTaskManager();
        List<TaskDto> taskDtoList = getSelectedTasks();
        for (TaskDto taskDto : taskDtoList) {
            try {
                Task task = taskManager.getTask(taskDto.getOid(), result);
                taskManager.resumeTask(task, result);
            }
            // some situations (e.g. resume task that is not suspended) are recorded in OperationResult only (i.e. no exception)
            // ordinary exceptions (ObjectNotFoundException, SchemaException) are already recorded in the result (and an exception is thrown)
            // unexpected exceptions are not recorded in OperationResult, only the exception is thrown
            catch (ObjectNotFoundException e) {
                // see above
            } catch (SchemaException e) {
                // see above
            }
            catch (Exception e) {
                result.recordPartialError("Couldn't resume task due to an unexpected exception.", e);
            }
        }
        if (result.isUnknown()) {
            result.recomputeStatus();
        }
        System.out.println("Operation result = " + result.dump());
        showResult(result);

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTaskTable());
    }

    private void nodeDetailsPerformed(AjaxRequestTarget target, String oid) {
        //todo implement
    }

    private void deleteTaskPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void scheduleTaskPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void stopSchedulerPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void startSchedulerPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void pauseSchedulerPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void deleteNodePerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
