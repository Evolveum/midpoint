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

import com.evolveum.midpoint.web.component.data.BasicOrderByBorder;
import com.evolveum.midpoint.web.component.data.NavigatorPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author lazyman
 */
public class PageRoles extends PageAdminRoles {

    public PageRoles() {
        initLayout();
    }

    private void initLayout() {
        ObjectDataProvider provider = new ObjectDataProvider(RoleType.class);
        final DataView<RoleType> pageable = new DataView<RoleType>("pageable", provider) {

            @Override
            protected void populateItem(Item<RoleType> item) {
                final RoleType role = item.getModelObject();

                AjaxLink link = new AjaxLink("link") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        roleDetailsPerformed(target, role.getOid());
                    }
                };
                link.add(new Label("name", role.getName()));
                item.add(link);

                item.add(new Label("description", role.getDescription()));
            }
        };
        add(pageable);

        pageable.setItemsPerPage(10);
        add(new NavigatorPanel("navigatorTop", pageable));
        add(new NavigatorPanel("navigatorBottom", pageable));

        add(new BasicOrderByBorder("orderByName", "name", provider) {

            @Override
            protected void onSortChanged() {
                pageable.setCurrentPage(0);
            }
        });
    }

    public void roleDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageUser.PARAM_USER_ID, oid);
        setResponsePage(PageUser.class, parameters);
    }
}
