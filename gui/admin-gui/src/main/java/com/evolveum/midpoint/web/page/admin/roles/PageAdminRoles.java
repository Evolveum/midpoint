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

package com.evolveum.midpoint.web.page.admin.roles;


import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
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

        items.add(new BottomMenuItem(createStringResource("pageAdminRoles.listRoles"), PageRoles.class));
        items.add(new BottomMenuItem(createRoleLabel(), PageRole.class, createRoleVisibleBehaviour()));

        return items;
    }

    private IModel<String> createRoleLabel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                String key = isEditingRole() ? "pageAdminRoles.editRole" : "pageAdminRoles.newRole";
                return PageAdminRoles.this.getString(key);
            }
        };
    }

    private VisibleEnableBehaviour createRoleVisibleBehaviour() {
        return new VisibleEnableBehaviour() {

            @Override
            public boolean isEnabled() {
                return !isEditingRole() && !(getPage() instanceof PageRole);
            }
        };
    }

    private boolean isEditingRole() {
        StringValue roleOid = getPageParameters().get(PageRole.PARAM_ROLE_ID);
        return roleOid != null && StringUtils.isNotEmpty(roleOid.toString());
    }
}
