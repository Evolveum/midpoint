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

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageUsers extends PageAdminUsers {

    public PageUsers() {
        initLayout();
    }

    private void initLayout() {
        List<IColumn<UserType>> columns = new ArrayList<IColumn<UserType>>();

        IColumn column = new CheckBoxColumn<UserType>() {

            @Override
            public void onUpdateHeader(AjaxRequestTarget target) {
                //todo implement
            }

            @Override
            public void onUpdateRow(AjaxRequestTarget target, IModel<Selectable<UserType>> rowModel) {
                //toto implement
            }
        };
        columns.add(column);

        column = new LinkColumn<Selectable<UserType>>(createStringResource("pageUsers.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<Selectable<UserType>> rowModel) {
                UserType user = rowModel.getObject().getValue();
                userDetailsPerformed(target, user.getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.givenName"), "givenName", "value.givenName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.familyName"), "familyName", "value.familyName");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageUsers.fullName"), "fullName", "value.fullName");
        columns.add(column);

        column = new AbstractColumn<Selectable<UserType>>(createStringResource("pageUsers.email")) {

            @Override
            public void populateItem(Item<ICellPopulator<Selectable<UserType>>> cellItem, String componentId,
                    IModel<Selectable<UserType>> rowModel) {

                List<String> emails = rowModel.getObject().getValue().getEmailAddress();
                String email = "";
                if (emails != null && !emails.isEmpty()) {
                    email = emails.get(0);
                }

                cellItem.add(new Label(componentId, new Model<String>(email)));
            }
        };
        columns.add(column);

        add(new TablePanel<UserType>("table", UserType.class, columns));
    }

    public void userDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageUser.PARAM_USER_ID, oid);
        setResponsePage(PageUser.class, parameters);
    }
}
