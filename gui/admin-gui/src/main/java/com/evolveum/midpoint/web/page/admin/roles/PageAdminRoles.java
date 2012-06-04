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


import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
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
public class PageAdminRoles extends PageAdmin {

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        List<BottomMenuItem> items = new ArrayList<BottomMenuItem>();

        items.add(new BottomMenuItem("pageAdminRoles.listRoles", PageRoles.class));
        items.add(new BottomMenuItem("pageAdminRoles.newRole", PageRole.class, new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !isEditingRole();
            }

            @Override
            public boolean isEnabled() {
                return !(getPage() instanceof PageRole);
            }
        }));
        items.add(new BottomMenuItem("pageAdminRoles.editRole", PageRole.class, new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isEditingRole();
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        }));

        return items;
    }

    private boolean isEditingRole() {
        StringValue roleOid = getPageParameters().get(PageRole.PARAM_ROLE_ID);
        return roleOid != null && StringUtils.isNotEmpty(roleOid.toString());
    }
}
