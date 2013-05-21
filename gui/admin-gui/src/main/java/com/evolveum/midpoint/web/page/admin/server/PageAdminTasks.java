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

package com.evolveum.midpoint.web.page.admin.server;


import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.users.PageUser;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Marker page class for {@link com.evolveum.midpoint.web.component.menu.top.TopMenu}
 *
 * @author lazyman
 */
public class PageAdminTasks extends PageAdmin {

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        List<BottomMenuItem> items = new ArrayList<BottomMenuItem>();

        items.add(new BottomMenuItem(createStringResource("pageAdminTasks.listTasks"), PageTasks.class));
        items.add(new BottomMenuItem(createStringResource("pageAdminTasks.newTask"), PageTaskAdd.class, new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !isEditingTask();
            }

            @Override
            public boolean isEnabled() {
                return !(getPage() instanceof PageTaskAdd);
            }
        }));
        items.add(new BottomMenuItem(createStringResource("pageAdminTasks.editTask"), PageTaskEdit.class, new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isEditingTask();
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        }));

        return items;
    }

    private boolean isEditingTask() {
        StringValue taskOid = getPageParameters().get(PageTaskEdit.PARAM_TASK_EDIT_ID);
        return taskOid != null && StringUtils.isNotEmpty(taskOid.toString());
    }
}
