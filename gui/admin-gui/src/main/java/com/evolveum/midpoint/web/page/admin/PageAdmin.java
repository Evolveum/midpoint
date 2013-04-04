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
