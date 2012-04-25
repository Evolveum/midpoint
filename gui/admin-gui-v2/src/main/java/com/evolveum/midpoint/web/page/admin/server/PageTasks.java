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

        DropDownChoice listSelect = new DropDownChoice("type", new Model(TaskListType.ALL), createListTypeModel(),
                new EnumChoiceRenderer(PageTasks.this));
        mainForm.add(listSelect);

        List<IColumn<TaskType>> columns = initTaskColumns();
        mainForm.add(new TablePanel<TaskType>("taskTable", new ObjectDataProvider(TaskType.class), columns));

        columns = initNodeColumns();
        TablePanel nodeTable = new TablePanel<TaskType>("nodeTable", new ObjectDataProvider(NodeType.class), columns);
        nodeTable.setShowPaging(false);
        mainForm.add(nodeTable);

        initButtons(mainForm);
    }

    private IModel<List<TaskListType>> createListTypeModel() {
        return new AbstractReadOnlyModel<List<TaskListType>>() {

            @Override
            public List<TaskListType> getObject() {
                List<TaskListType> list = new ArrayList<TaskListType>();
                Collections.addAll(list, TaskListType.values());

                return list;
            }
        };
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

        columns.add(new PropertyColumn(createStringResource("pageTasks.node.hostname"), "value.hostname"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.nodeIdentifier"), "value.nodeIdentifier"));

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
        columns.add(new PropertyColumn(createStringResource("pageTasks.task.exclusivity"), "value.exclusivityStatus"));
//        columns.add(new PropertyColumn(createStringResource("pageTasks.task.threadAlive"), "value.exclusivity"));
//        columns.add(new PropertyColumn(createStringResource("pageTasks.task.currentRunTime"), "value.exclusivity"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.task.scheduledToRunAgain"), "value.nextRunStartTime"));
        return columns;
    }

    private void initButtons(Form mainForm) {
        AjaxLinkButton deactivate = new AjaxLinkButton("deactivate",
                createStringResource("pageTasks.button.deactivate")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deactivatePerformed(target);
            }
        };
        mainForm.add(deactivate);

        AjaxLinkButton deactivateAll = new AjaxLinkButton("deactivateAll",
                createStringResource("pageTasks.button.deactivateAll")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deactivateAllPerformed(target);
            }
        };
        mainForm.add(deactivateAll);

        AjaxLinkButton reactivate = new AjaxLinkButton("reactivate",
                createStringResource("pageTasks.button.reactivate")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                reactivatePerformed(target);
            }
        };
        mainForm.add(reactivate);

        AjaxLinkButton reactivateAll = new AjaxLinkButton("reactivateAll",
                createStringResource("pageTasks.button.reactivateAll")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                reactivateAllPerformed(target);
            }
        };
        mainForm.add(reactivateAll);
    }

    private void taskDetailsPerformed(AjaxRequestTarget target, String oid) {
        //todo implement
    }

    private void deactivatePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void deactivateAllPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void reactivatePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void reactivateAllPerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
