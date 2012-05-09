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
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

public class RolesPopup extends Panel {

    public RolesPopup(String id, PageBase page) {
        super(id);

        initLayout(page);
    }

    private void initLayout(PageBase page) {
        Form rolesForm = new Form("rolesForm");
        add(rolesForm);

        List<IColumn<RoleType>> columns = new ArrayList<IColumn<RoleType>>();

        IColumn column = new CheckBoxHeaderColumn<RoleType>();
        columns.add(column);

        columns.add(new PropertyColumn(new StringResourceModel("rolesPopup.name", this, null), "value.name"));
        columns.add(new PropertyColumn(new StringResourceModel("rolesPopup.description", this, null), "value.description"));

        TablePanel table = new TablePanel<RoleType>("table", new ObjectDataProvider(page, RoleType.class), columns);
        table.setOutputMarkupId(true);
        rolesForm.add(table);
        initButtons(rolesForm);
    }

    private void initButtons(Form form) {
        AjaxLinkButton addButton = new AjaxLinkButton("add",
                new StringResourceModel("rolesPopup.button.add", this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        form.add(addButton);
    }

    protected void addPerformed(AjaxRequestTarget target) {

    }
}
