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

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
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

        List<IColumn<TaskType>> columns = initTaskColumns();
        mainForm.add(new TablePanel<TaskType>("taskTable", new ObjectDataProvider(TaskType.class), columns));

        columns = initNodeColumns();
        TablePanel nodeTable = new TablePanel<TaskType>("nodeTable", new ObjectDataProvider(NodeType.class), columns);
        nodeTable.setShowPaging(false);
        mainForm.add(nodeTable);

        initTaskButtons(mainForm);
        initSchedulerButtons(mainForm);
        initNodeButtons(mainForm);
    }

    private List<IColumn<TaskType>> initNodeColumns() {
        List<IColumn<TaskType>> columns = new ArrayList<IColumn<TaskType>>();

        IColumn column = new CheckBoxHeaderColumn<TaskType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<TaskType>>(createStringResource("pageTasks.node.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> rowModel) {
                TaskType role = rowModel.getObject().getValue();
                taskDetailsPerformed(target, role.getOid());
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("pageTasks.node.running"), "value.running"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.nodeIdentifier"), "value.nodeIdentifier"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.hostname"), "value.hostname")); //todo add jmx port
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.lastCheckInTime"), "value.lastCheckInTime")); //todo i18n, what is this, it's not date...
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.clustered"), "value.clustered"));
//        columns.add(new PropertyColumn(createStringResource("pageTasks.node.statusMessage"), "value.statusMessage")); //todo uncomment later


        return columns;
    }

    private List<IColumn<TaskType>> initTaskColumns() {
        List<IColumn<TaskType>> columns = new ArrayList<IColumn<TaskType>>();

        IColumn column = new CheckBoxHeaderColumn<TaskType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<TaskType>>(createStringResource("pageTasks.task.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<TaskType>> rowModel) {
                TaskType role = rowModel.getObject().getValue();
                taskDetailsPerformed(target, role.getOid());
            }
        };
        columns.add(column);

        //todo
        columns.add(new PropertyColumn(createStringResource("pageTasks.task.handler"), "handlerUri", "value.handlerUri"));
//        columns.add(new PropertyColumn(createStringResource("pageTasks.task.objectRef"), "value.objectRef"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.task.execution"), "value.executionStatus"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.task.exclusivity"), "value.exclusivityStatus"));//todo delete columns
//        columns.add(new PropertyColumn(createStringResource("pageTasks.task.threadAlive"), "value.exclusivity"));
//        columns.add(new PropertyColumn(createStringResource("pageTasks.task.currentRunTime"), "value.exclusivity"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.task.scheduledToRunAgain"), "value.nextRunStartTime"));
        return columns;
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

    private List<SelectableBean<TaskType>> getSelectedTasks() {
        TablePanel panel = (TablePanel) get("mainForm:taskTable");
        DataTable table = panel.getDataTable();
        ObjectDataProvider<TaskType> provider = (ObjectDataProvider<TaskType>) table.getDataProvider();

        List<SelectableBean<TaskType>> selected = new ArrayList<SelectableBean<TaskType>>();
        for (SelectableBean<TaskType> row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row);
            }
        }

        return selected;
    }

    private List<SelectableBean<NodeType>> getSeletedNodes() {
        TablePanel panel = (TablePanel) get("mainForm:nodeTable");
        DataTable table = panel.getDataTable();
        ObjectDataProvider<NodeType> provider = (ObjectDataProvider<NodeType>) table.getDataProvider();

        List<SelectableBean<NodeType>> selected = new ArrayList<SelectableBean<NodeType>>();
        for (SelectableBean<NodeType> row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row);
            }
        }

        return selected;
    }

    private List<String> createCategoryList() {
        List<String> categories = new ArrayList<String>();
        //todo implement

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
//        getSeletedNodes();

        //todo implement
    }

    private void suspendTaskPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void resumeTaskPerformed(AjaxRequestTarget target) {
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
