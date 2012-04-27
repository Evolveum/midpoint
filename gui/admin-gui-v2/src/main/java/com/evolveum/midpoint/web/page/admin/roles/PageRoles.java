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

package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.test.TestPage;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageRoles extends PageAdminRoles {

    private static final String OPERATION_DELETE_ROLES = "pageRoles.deleteRoles";

    public PageRoles() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        List<IColumn<RoleType>> columns = new ArrayList<IColumn<RoleType>>();

        IColumn column = new CheckBoxHeaderColumn<RoleType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<RoleType>>(createStringResource("pageRoles.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                RoleType role = rowModel.getObject().getValue();
                roleDetailsPerformed(target, role.getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageRoles.description"), "value.description");
        columns.add(column);

        TablePanel table = new TablePanel<RoleType>("table", new ObjectDataProvider(RoleType.class), columns);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        AjaxLinkButton delete = new AjaxLinkButton("delete", createStringResource("pageRoles.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        mainForm.add(delete);

        ///////////////////////// POPUP MODAL WINDOW //////////////////////////////////
        mainForm.add(new AjaxLinkButton("popup", new Model<String>("Open test page")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(TestPage.class);
                //popupWindow.show(target);
            }
        });
        ///////////////////////////////////////////////////////////////////////////////
    }

    private void deletePerformed(AjaxRequestTarget target) {
        TablePanel panel = (TablePanel) get("mainForm:table");
        DataTable table = panel.getDataTable();
        ObjectDataProvider<RoleType> provider = (ObjectDataProvider<RoleType>) table.getDataProvider();

        List<SelectableBean<RoleType>> rows = provider.getAvailableData();
        List<SelectableBean<RoleType>> toBeDeleted = new ArrayList<SelectableBean<RoleType>>();
        for (SelectableBean row : rows) {
            if (row.isSelected()) {
                toBeDeleted.add(row);
            }
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_ROLES);
        for (SelectableBean<RoleType> row : toBeDeleted) {
            try {
                Task task = getTaskManager().createTaskInstance(OPERATION_DELETE_ROLES);
                RoleType role = row.getValue();
                getModelService().deleteObject(RoleType.class, role.getOid(), task, result);
            } catch (Exception ex) {
                result.recordPartialError("Couldn't delete role.", ex);
            }
        }
        result.recomputeStatus("Error occurred during role deleting.");
        showResult(result);

        //todo fix message, add confirmation dialog
        target.appendJavaScript("alert('deleted " + toBeDeleted.size() + " objects.');");
        target.add(panel);
    }

    private void roleDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageRole.PARAM_ROLE_ID, oid);
        setResponsePage(PageRole.class, parameters);
    }
}
