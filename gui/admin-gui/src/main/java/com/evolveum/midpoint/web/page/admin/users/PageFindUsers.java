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

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;

import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/users/find", action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_FIND_USERS_URL,
                label = "PageFindUsers.auth.users.label",
                description = "PageFindUsers.auth.users.description")})
public class PageFindUsers extends PageAdminUsers {

    private static final Trace LOGGER = TraceManager.getTrace(PageFindUsers.class);
    private static final String DOT_CLASS = PageFindUsers.class.getName() + ".";

    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";
    private static final String ID_CLEAR = "clear";
    private static final String ID_SEARCH = "search";

    public PageFindUsers() {
        initLayout();
    }

    private void initLayout() {
        Form form = new Form(ID_FORM);
        add(form);

//        ISortableDataProvider provider = null; //todo implement
//        List<IColumn> columns = null; //todo implement
//        TablePanel table = new TablePanel(ID_TABLE, provider, columns);
//        form.add(table);

        WebMarkupContainer table = new WebMarkupContainer(ID_TABLE);
        form.add(table);

        initButtons(form);
    }

    private void initButtons(Form form) {
        AjaxButton clear = new AjaxButton(ID_CLEAR, createStringResource("PageFindUsers.clear")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearSearch(target);
            }
        };
        form.add(clear);

        AjaxButton search = new AjaxButton(ID_SEARCH, createStringResource("PageFindUsers.search")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                search(target);
            }
        };
        form.add(search);
    }

    private void clearSearch(AjaxRequestTarget target) {
        //todo implement
    }

    private void search(AjaxRequestTarget target) {
        //todo implement
    }
}
