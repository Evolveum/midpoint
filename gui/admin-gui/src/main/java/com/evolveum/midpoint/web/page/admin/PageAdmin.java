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

package com.evolveum.midpoint.web.page.admin;

import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.reports.PageAdminReports;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.resources.PageAdminResources;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageAdminRoles;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageAdminTasks;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageAdminUsers;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItems;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageAdmin extends PageBase {

    @Override
    public List<TopMenuItem> getTopMenuItems() {
        List<TopMenuItem> items = new ArrayList<TopMenuItem>();
        items.add(new TopMenuItem("pageAdmin.home", "pageAdmin.home.description", PageDashboard.class));
        items.add(new TopMenuItem("pageAdmin.users", "pageAdmin.users.description",
                PageUsers.class, PageAdminUsers.class));
        items.add(new TopMenuItem("pageAdmin.roles", "pageAdmin.roles.description",
                PageRoles.class, PageAdminRoles.class));
        items.add(new TopMenuItem("pageAdmin.resources", "pageAdmin.resources.description",
                PageResources.class, PageAdminResources.class));
        //todo fix with visible behaviour [lazyman]
        if (getWorkflowService().isEnabled()) {
            items.add(new TopMenuItem("pageAdmin.workItems", "pageAdmin.workItems.description",
                    PageWorkItems.class, PageAdminWorkItems.class));
        }
        items.add(new TopMenuItem("pageAdmin.serverTasks", "pageAdmin.serverTasks.description",
                PageTasks.class, PageAdminTasks.class));
        items.add(new TopMenuItem("pageAdmin.reports", "pageAdmin.reports.description",
                PageReports.class, PageAdminReports.class));
        items.add(new TopMenuItem("pageAdmin.configuration", "pageAdmin.configuration.description",
                PageDebugList.class, PageAdminConfiguration.class));

        return items;
    }

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        return new ArrayList<BottomMenuItem>();
    }
}
