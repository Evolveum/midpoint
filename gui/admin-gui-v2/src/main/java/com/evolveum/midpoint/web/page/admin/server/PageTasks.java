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

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.admin.roles.PageAdminRoles;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageTasks extends PageAdminTasks {

    public PageTasks() {
        initLayout();
    }

    private void initLayout() {
        List<IColumn<TaskType>> columns = new ArrayList<IColumn<TaskType>>();

        IColumn column = new CheckBoxColumn<TaskType>() {

            @Override
            public void onUpdateHeader(AjaxRequestTarget target) {
                //todo implement
            }

            @Override
            public void onUpdateRow(AjaxRequestTarget target, IModel<Selectable<TaskType>> rowModel) {
                //todo implement
            }
        };
        columns.add(column);

        column = new LinkColumn<Selectable<TaskType>>(createStringResource("pageTasks.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<Selectable<TaskType>> rowModel) {
                TaskType role = rowModel.getObject().getValue();
                taskDetailsPerformed(target, role.getOid());
            }
        };
        columns.add(column);

        //todo
        columns.add(new PropertyColumn(createStringResource("pageTasks.handler"), "handlerUri", "value.handlerUri"));
//        columns.add(new PropertyColumn(createStringResource("pageTasks.objectRef"), "value.objectRef"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.execution"), "value.executionStatus"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.exclusivity"), "value.exclusivityStatus"));
//        columns.add(new PropertyColumn(createStringResource("pageTasks.threadAlive"), "value.exclusivity"));
//        columns.add(new PropertyColumn(createStringResource("pageTasks.currentRunTime"), "value.exclusivity"));
        columns.add(new PropertyColumn(createStringResource("pageTasks.scheduledToRunAgain"), "value.nextRunStartTime"));


        OptionPanel option = new OptionPanel("option", new Model<String>("main title"));
        add(option);

        OptionItem item = new OptionItem("item", new Model<String>("item title"));
        option.getBodyContainer().add(item);

        OptionContent content = new OptionContent("optionContent");
        add(content);
        content.getBodyContainer().add(new TablePanel<TaskType>("table", TaskType.class, columns));
    }

    public void taskDetailsPerformed(AjaxRequestTarget target, String oid) {

    }
}
