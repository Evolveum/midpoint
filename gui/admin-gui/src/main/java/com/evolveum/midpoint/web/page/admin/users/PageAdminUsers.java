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


import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.util.PageDisabledVisibleBehaviour;
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
public class PageAdminUsers extends PageAdmin {

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        List<BottomMenuItem> items = new ArrayList<BottomMenuItem>();

        items.add(new BottomMenuItem(createStringResource("pageAdminUsers.listUsers"), PageUsers.class));
        items.add(new BottomMenuItem(createUserLabel(), PageUser.class, createUserVisibleBehaviour()));
//        items.add(new BottomMenuItem(createStringResource("pageAdminUsers.bulkActions"), PageBulkUsers.class));

        return items;

    }

    private IModel<String> createUserLabel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                String key = isEditingUser() ? "pageAdminUsers.editUser" : "pageAdminUsers.newUser";
                return PageAdminUsers.this.getString(key);
            }
        };
    }

    private VisibleEnableBehaviour createUserVisibleBehaviour() {
        return new VisibleEnableBehaviour() {

            @Override
            public boolean isEnabled() {
                return !isEditingUser() && !(getPage() instanceof PageUser);
            }
        };
    }

    private boolean isEditingUser() {
        StringValue userOid = getPageParameters().get(PageUser.PARAM_USER_ID);
        return userOid != null && StringUtils.isNotEmpty(userOid.toString());
    }
}
