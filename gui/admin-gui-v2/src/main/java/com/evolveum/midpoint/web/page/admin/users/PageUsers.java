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

import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * @author lazyman
 */
public class PageUsers extends PageAdminUsers {

    public PageUsers() {
        initLayout();
    }

    private void initLayout() {
        ObjectDataProvider provider = new ObjectDataProvider(UserType.class);
        final DataView<UserType> pageable = new DataView<UserType>("pageable", provider) {

            @Override
            protected void populateItem(Item<UserType> item) {
                UserType user = item.getModelObject();

                item.add(new Label("name", user.getName()));
                item.add(new Label("givenName", user.getGivenName()));
                item.add(new Label("familyName", user.getFamilyName()));
                item.add(new Label("fullName", user.getFullName()));

                List<String> emails = user.getEmailAddress();
                String email = "";
                if (emails != null && !emails.isEmpty()) {
                    email = emails.get(0);
                }
                item.add(new Label("email", email));
            }
        };
        add(pageable);

        pageable.setItemsPerPage(10);
        add(new NavigatorPanel("navigatorTop", pageable));
        add(new NavigatorPanel("navigatorBottom", pageable));
    }

    public void userDetailsPerformed(AjaxRequestTarget target, IModel<String> userIdModel) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageUser.PARAM_USER_ID, userIdModel.getObject());
        setResponsePage(PageUser.class, parameters);
    }
}
