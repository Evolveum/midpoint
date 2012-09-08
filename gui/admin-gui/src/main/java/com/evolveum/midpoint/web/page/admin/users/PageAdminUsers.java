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

package com.evolveum.midpoint.web.page.admin.users;


import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.util.PageDisabledVisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Marker page class for {@link com.evolveum.midpoint.web.component.menu.top.TopMenu}
 *
 * @author lazyman
 */
public class PageAdminUsers extends PageAdmin {

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        List<BottomMenuItem> items = new ArrayList<BottomMenuItem>();

        items.add(new BottomMenuItem("pageAdminUsers.listUsers", PageUsers.class));
        items.add(new BottomMenuItem("pageAdminUsers.newUser", PageUser.class,
                new PageDisabledVisibleBehaviour(this, PageUser.class) {

            @Override
            public boolean isVisible() {
                return !isEditingUser();
            }
        }));
        items.add(new BottomMenuItem("pageAdminUsers.editUser", PageUser.class, new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isEditingUser();
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        }));
        items.add(new BottomMenuItem("pageAdminUsers.orgStruct", PageOrgStruct.class,
                new PageDisabledVisibleBehaviour(this, PageOrgStruct.class)));

        return items;

    }

    private boolean isEditingUser() {
        StringValue userOid = getPageParameters().get(PageUser.PARAM_USER_ID);
        return userOid != null && StringUtils.isNotEmpty(userOid.toString());
    }
}
