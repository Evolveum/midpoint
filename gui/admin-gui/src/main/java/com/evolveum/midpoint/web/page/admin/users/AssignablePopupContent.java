/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignableDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;
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
    private boolean multiselect;

    public AssignablePopupContent(String id) {
        super(id, null);
    }

    @Override
    protected void initLayout() {
        Form rolesForm = new Form(ID_ASSIGNABLE_FORM);
        add(rolesForm);

        TablePanel table = createTable();
        rolesForm.add(table);

        AjaxLinkButton addButton = new AjaxLinkButton(ID_ADD,
                createStringResource("assignablePopupContent.button.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target, getSelectedObjects());
            }
        };
        addButton.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return multiselect;
            }
        });
        rolesForm.add(addButton);
    }

    private TablePanel createTable() {
        List<IColumn> columns = multiselect ? createMultiSelectColumns() : createSingleSelectColumns();
        TablePanel table = new TablePanel(ID_TABLE, new ObjectDataProvider(getPageBase(), type), columns);
        table.setOutputMarkupId(true);

        return table;
    }

    private List<IColumn> createSingleSelectColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new LinkColumn<SelectableBean<? extends ObjectType>>(
                createStringResource("assignablePopupContent.name"), "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<? extends ObjectType>> rowModel) {
                addPerformed(target, rowModel.getObject().getValue());
            }
        });
        columns.add(new PropertyColumn(createStringResource("assignablePopupContent.description"), "value.description"));

        return columns;
    }

    private List<IColumn> createMultiSelectColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        IColumn column = new CheckBoxHeaderColumn();
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("assignablePopupContent.name"), "value.name"));
        columns.add(new PropertyColumn(createStringResource("assignablePopupContent.description"), "value.description"));

        return columns;
    }

    private List<UserAssignableDto> getSelectedObjects() {
        List<UserAssignableDto> roles = new ArrayList<UserAssignableDto>();

        TablePanel table = (TablePanel) get(ID_ASSIGNABLE_FORM + ":" + ID_TABLE);
        ObjectDataProvider<? extends ObjectType> provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
        for (SelectableBean<? extends ObjectType> bean : provider.getAvailableData()) {
            if (!bean.isSelected()) {
                continue;
            }

            ObjectType role = bean.getValue();
            roles.add(new UserAssignableDto(role.getOid(), role.getName(), role.getDescription()));
        }

        return roles;
    }

    public void setType(Class<? extends ObjectType> type) {
        Validate.notNull(type, "Class must not be null.");

        this.type = type;
        multiselect = !ResourceType.class.isAssignableFrom(type);

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

    protected void addPerformed(AjaxRequestTarget target, ObjectType selected) {

    }

    protected void addPerformed(AjaxRequestTarget target, List<UserAssignableDto> selected) {

    }
}
