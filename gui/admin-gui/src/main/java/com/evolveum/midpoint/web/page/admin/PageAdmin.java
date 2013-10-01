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

import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.menu.top2.MenuBarItem;
import com.evolveum.midpoint.web.component.menu.top2.MenuItem;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageOrgStruct;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItems;
import com.evolveum.midpoint.web.util.WebMiscUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageAdmin extends PageBase {

    @Override
    protected List<MenuBarItem> createMenuItems() {
        List<MenuBarItem> items = new ArrayList<MenuBarItem>();

        // todo fix with visible behaviour [lazyman]
        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                AuthorizationConstants.AUTZ_UI_HOME_ALL_URL)) {
            items.add(createHomeItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_URL,
                AuthorizationConstants.AUTZ_UI_USERS_ALL_URL)) {
            items.add(createUsersItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_URL,
                AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL)) {
            items.add(createRolesItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL)) {
            items.add(createResourcesItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL)) {
            items.add(createWorkItemsItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL)) {
            items.add(createServerTasksItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_REPORTS_URL)) {
            items.add(createReportsItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL)) {
            items.add(createConfigurationItems());
        }

        return items;
    }

    private MenuBarItem createWorkItemsItems() {
        MenuBarItem workItems = new MenuBarItem(createStringResource("PageAdmin.menu.top.workItems"), null);
        workItems.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.workItems.list"), PageWorkItems.class));

        return workItems;
    }

    private MenuBarItem createServerTasksItems() {
        MenuBarItem serverTasks = new MenuBarItem(createStringResource("PageAdmin.menu.top.serverTasks"), null);
        serverTasks.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.list"), PageTasks.class));
        serverTasks.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.new"), PageTaskAdd.class));

        return serverTasks;
    }

    private MenuBarItem createResourcesItems() {
        MenuBarItem resources = new MenuBarItem(createStringResource("PageAdmin.menu.top.resources"), null);
        resources.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.resources.list"), PageResources.class));
        resources.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.resources.new"), PageDashboard.class));

        return resources;
    }

    private MenuBarItem createReportsItems() {
        MenuBarItem reports = new MenuBarItem(createStringResource("PageAdmin.menu.top.reports"), null);
        reports.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.reports.list"), PageDashboard.class));
        reports.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.reports.created"), PageDashboard.class));

        return reports;
    }

    private MenuBarItem createConfigurationItems() {
        MenuBarItem configuration = new MenuBarItem(createStringResource("PageAdmin.menu.top.configuration"), null);
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.bulkActions"), PageDashboard.class));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.importObject"), PageImportObject.class));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.repositoryObjects"), PageDebugList.class));
        configuration.addMenuItem(new MenuItem(null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.configuration"), true, null, null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.basic"), PageDashboard.class));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.security"), PageDashboard.class));
        configuration.addMenuItem(new MenuItem(null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.development"), true, null, null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.shadowsDetails"), PageDashboard.class));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.expressionEvaluator"), PageDashboard.class));
        configuration.addMenuItem(new MenuItem(null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.about"), PageDashboard.class));

        return configuration;
    }

    private MenuBarItem createHomeItems() {
        MenuBarItem home = new MenuBarItem(createStringResource("PageAdmin.menu.top.home"), PageDashboard.class);

        return home;
    }

    private MenuBarItem createUsersItems() {
        MenuBarItem users = new MenuBarItem(createStringResource("PageAdmin.menu.top.users"), null);
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.list"), PageUsers.class));
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.find"), PageUsers.class));
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.new"), PageUser.class));
        users.addMenuItem(new MenuItem(null));
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.org"), true, null, null));
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.org.tree"), PageOrgStruct.class));
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.org.new"), PageOrgStruct.class));

        return users;
    }

    private MenuBarItem createRolesItems() {
        MenuBarItem roles = new MenuBarItem(createStringResource("PageAdmin.menu.top.roles"), null);
        roles.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.roles.list"), PageRoles.class));
        roles.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.roles.new"), PageRole.class));

        return roles;
    }


    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        return new ArrayList<BottomMenuItem>();
    }
}
