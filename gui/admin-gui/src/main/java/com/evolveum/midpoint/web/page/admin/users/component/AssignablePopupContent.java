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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;

import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

public class AssignablePopupContent extends BasePanel {

    private static final String ID_ASSIGNABLE_FORM = "assignableForm";
    private static final String ID_TABLE = "table";
    private static final String ID_ADD = "add";

    private Class<? extends ObjectType> type = RoleType.class;

    public AssignablePopupContent(String id) {
        super(id, null);
    }

    @Override
    protected void initLayout() {
        Form assignableForm = new Form(ID_ASSIGNABLE_FORM);
        add(assignableForm);

        TablePanel table = createTable();
        assignableForm.add(table);

        AjaxLinkButton addButton = new AjaxLinkButton(ID_ADD,
                createStringResource("assignablePopupContent.button.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target, getSelectedObjects());
            }
        };
        assignableForm.add(addButton);
    }

    private TablePanel createTable() {
        List<IColumn> columns = createMultiSelectColumns();
        TablePanel table = new TablePanel(ID_TABLE, new ObjectDataProvider(getPageBase(), type), columns);
        table.setOutputMarkupId(true);

        return table;
    }

    private List<IColumn> createMultiSelectColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        IColumn column = new CheckBoxHeaderColumn();
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("assignablePopupContent.name"), "value.name"));
        if (OrgType.class.isAssignableFrom(type)) {
            columns.add(new PropertyColumn(createStringResource("assignablePopupContent.displayName"), "value.displayName.orig"));
        }
        columns.add(new PropertyColumn(createStringResource("assignablePopupContent.description"), "value.description"));

        return columns;
    }

    private List<ObjectType> getSelectedObjects() {
        List<ObjectType> selected = new ArrayList<ObjectType>();

        TablePanel table = (TablePanel) get(ID_ASSIGNABLE_FORM + ":" + ID_TABLE);
        ObjectDataProvider<? extends ObjectType> provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
        for (SelectableBean<? extends ObjectType> bean : provider.getAvailableData()) {
            if (!bean.isSelected()) {
                continue;
            }

            selected.add(bean.getValue());
        }

        return selected;
    }

    public void setType(Class<? extends ObjectType> type) {
        Validate.notNull(type, "Class must not be null.");

        this.type = type;

        TablePanel table = (TablePanel) get(ID_ASSIGNABLE_FORM + ":" + ID_TABLE);
        if (table != null) {
            ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
            provider.setType(type);

            //replace table with table with proper columns
            Form rolesForm = (Form) get(ID_ASSIGNABLE_FORM);
            rolesForm.replace(createTable());
        }
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {

    }
}
