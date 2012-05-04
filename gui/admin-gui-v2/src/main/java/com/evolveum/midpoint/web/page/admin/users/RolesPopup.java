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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;

public class RolesPopup extends Panel {

	
	
	public RolesPopup(String id, final ModalWindow window) {
		super(id);
		createRolesTable();
	}

	public void createRolesTable(){
    	Form rolesForm = new Form("rolesForm");
    	add(rolesForm);
    	
        List<IColumn<RoleType>> columns = new ArrayList<IColumn<RoleType>>();

        IColumn column = new CheckBoxHeaderColumn<RoleType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<RoleType>>(new StringResourceModel("rolesPopup.name", this, null), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                RoleType role = rowModel.getObject().getValue();
                roleDetailsPerformed(target, role.getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(new StringResourceModel("rolesPopup.description", this, null), "value.description");
        columns.add(column);

        TablePanel table = new TablePanel<RoleType>("table", new ObjectDataProvider((PageBase)getPage(), RoleType.class), columns);
        table.setOutputMarkupId(true);
        rolesForm.add(table);
    }
    
    private void roleDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        //parameters.add(PageRole.PARAM_ROLE_ID, oid);
        //setResponsePage(PageRole.class, parameters);
    }
}
