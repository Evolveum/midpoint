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

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.menu.top.MenuBarItem;
import com.evolveum.midpoint.web.component.menu.top.MenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenuBar;
import com.evolveum.midpoint.web.component.menu.top.UserMenuPanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.*;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceEdit;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.*;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstancesAll;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstancesRequestedBy;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstancesRequestedFor;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItems;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItemsClaimable;
import com.evolveum.midpoint.web.util.WebMiscUtil;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageAdmin extends PageBase {

    public PageAdmin() {
        this(null);
    }

    public PageAdmin(PageParameters parameters){
        super(parameters);

        TopMenuBar menuBar = getTopMenuBar();
        menuBar.addOrReplace(new UserMenuPanel(TopMenuBar.ID_RIGHT_PANEL));
    }

    @Override
    protected List<MenuBarItem> createMenuItems() {
        //todo enable, disabled descriptor loader until finished [lazyman]
//        return DescriptorLoader.getMenuBarItems();

        List<MenuBarItem> items = new ArrayList<MenuBarItem>();

        // todo fix with visible behaviour [lazyman]
        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                AuthorizationConstants.AUTZ_UI_HOME_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URI)) {
            items.add(createHomeItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_URL,
                AuthorizationConstants.AUTZ_UI_USERS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URI)) {
            items.add(createUsersItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_URL,
                AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URI)) {
            items.add(createRolesItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URI)) {
            items.add(createResourcesItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URI)) {
            if (getWorkflowManager().isEnabled()) {
                items.add(createWorkItemsItems());
            }
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URI)) {
            items.add(createServerTasksItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_REPORTS_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_URI)) {
            items.add(createReportsItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URI)) {
            items.add(createConfigurationItems());
        }

        return items;
    }

    private MenuBarItem createWorkItemsItems() {
        MenuBarItem workItems = new MenuBarItem(createStringResource("PageAdmin.menu.top.workItems"), null);
        workItems.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.workItems.list"),
                PageWorkItems.class));
        workItems.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listClaimable"),
                PageWorkItemsClaimable.class));
        workItems.addMenuItem(new MenuItem(null));
        workItems.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesAll"),
                PageProcessInstancesAll.class));
        workItems.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesRequestedBy"),
                PageProcessInstancesRequestedBy.class));
        workItems.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesRequestedFor"),
                PageProcessInstancesRequestedFor.class));

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
        //todo delete this [lazyman]
        resources.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.resources.new"), PageResourceEdit.class));
//        resources.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.resources.wizard"), PageResourceWizard.class));


//        items.add(new BottomMenuItem(createStringResource("pageAdminResources.detailsResource"), PageResource.class,
//                new PageVisibleDisabledBehaviour(this, PageResource.class)));
//        items.add(new BottomMenuItem(createResourceWizardLabel(), PageResourceEdit.class,
//                createWizardVisibleBehaviour()));
//        items.add(new BottomMenuItem(createStringResource("pageAdminResources.importResource"),
//                PageResourceImport.class, new PageVisibleDisabledBehaviour(this, PageResourceImport.class)));
//        items.add(new BottomMenuItem(createStringResource("pageAdminResources.contentAccounts"),
//                PageContentAccounts.class, new PageVisibleDisabledBehaviour(this, PageContentAccounts.class)));
//        items.add(new BottomMenuItem(createStringResource("pageAdminResources.accountDetails"), PageAccount.class,
//                new PageVisibleDisabledBehaviour(this, PageAccount.class)));

        return resources;
    }

    private MenuBarItem createReportsItems() {
        MenuBarItem reports = new MenuBarItem(createStringResource("PageAdmin.menu.top.reports"), null);
        reports.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.reports.list"), PageReports.class));
        reports.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.reports.created"), PageCreatedReports.class));

        return reports;
    }

    private MenuBarItem createConfigurationItems() {
        MenuBarItem configuration = new MenuBarItem(createStringResource("PageAdmin.menu.top.configuration"), null);
//        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.bulkActions"), PageBulkAction.class));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.importObject"), PageImportObject.class));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.repositoryObjects"), PageDebugList.class));
        configuration.addMenuItem(new MenuItem(null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.configuration"), true, null, null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.basic"), PageSystemConfiguration.class));
//        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.security"), PageDashboard.class));
        configuration.addMenuItem(new MenuItem(null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.development"), true, null, null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.shadowsDetails"), PageAccounts.class));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.internals"), PageInternals.class));
//        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.expressionEvaluator"), PageDashboard.class));
        configuration.addMenuItem(new MenuItem(null));
        configuration.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.configuration.about"), PageAbout.class));

        return configuration;
    }

    private MenuBarItem createHomeItems() {
        MenuBarItem home = new MenuBarItem(createStringResource("PageAdmin.menu.top.home"), PageDashboard.class);

        return home;
    }

    private MenuBarItem createUsersItems() {
        MenuBarItem users = new MenuBarItem(createStringResource("PageAdmin.menu.top.users"), null);
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.list"), PageUsers.class));
//        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.find"), PageFindUsers.class));
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.new"), PageUser.class));
        users.addMenuItem(new MenuItem(null));
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.org"), true, null, null));
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.org.tree"), PageOrgTree.class));
        users.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.users.org.new"), PageOrgUnit.class));

        return users;
    }

    private MenuBarItem createRolesItems() {
        MenuBarItem roles = new MenuBarItem(createStringResource("PageAdmin.menu.top.roles"), null);
        roles.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.roles.list"), PageRoles.class));
        roles.addMenuItem(new MenuItem(createStringResource("PageAdmin.menu.top.roles.new"), PageRole.class));

        return roles;
    }
}
