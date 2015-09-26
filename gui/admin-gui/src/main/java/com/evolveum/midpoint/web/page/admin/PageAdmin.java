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

import com.evolveum.midpoint.common.SystemConfigurationHolder;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.menu.SideBarMenuItem;
import com.evolveum.midpoint.web.component.menu.MainMenuItem;
import com.evolveum.midpoint.web.component.menu.MenuItem;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDecisions;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDefinitions;
import com.evolveum.midpoint.web.page.admin.configuration.*;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.reports.PageNewReport;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.workflow.*;
import com.evolveum.midpoint.web.page.self.PageSelfAssignments;
import com.evolveum.midpoint.web.page.self.PageSelfCredentials;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.web.page.self.PageSelfProfile;
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

    public PageAdmin(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected List<SideBarMenuItem> createMenuItems() {
        List<SideBarMenuItem> menus = new ArrayList<>();

        SideBarMenuItem menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.selfService"));
        menus.add(menu);
        createSelfServiceMenu(menu);

        menu = new SideBarMenuItem(createStringResource("PageAdmin.menu.mainNavigation"));
        menus.add(menu);
        List<MainMenuItem> items = menu.getItems();

        // todo fix with visible behaviour [lazyman]
        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                AuthorizationConstants.AUTZ_UI_HOME_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createHomeItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_URL,
                AuthorizationConstants.AUTZ_UI_USERS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createUsersItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ORG_STRUCT_URL,
                AuthorizationConstants.AUTZ_UI_ORG_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createOrganizationsMenu());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_URL,
                AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createRolesItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createResourcesItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            if (getWorkflowManager().isEnabled()) {
                items.add(createWorkItemsItems());
            }
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CERTIFICATION_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)
                && SystemConfigurationHolder.isExperimentalCodeEnabled()) {
            items.add(createCertificationItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createServerTasksItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_REPORTS_URL,
                AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createReportsItems());
        }

        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_URL, AuthorizationConstants.AUTZ_GUI_ALL_DEPRECATED_URL)) {
            items.add(createConfigurationItems());
        }

        return menus;
    }

    private MainMenuItem createWorkItemsItems() {
        MainMenuItem item = new MainMenuItem("fa fa-inbox",
                createStringResource("PageAdmin.menu.top.workItems"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.list"),
                PageWorkItems.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listClaimable"),
                PageWorkItemsClaimable.class);
        submenu.add(menu);

        menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesAll"),
                PageProcessInstancesAll.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesRequestedBy"),
                PageProcessInstancesRequestedBy.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.workItems.listProcessInstancesRequestedFor"),
                PageProcessInstancesRequestedFor.class);
        submenu.add(menu);

        return item;
    }

    private MainMenuItem createServerTasksItems() {
        MainMenuItem item = new MainMenuItem("fa fa-tasks",
                createStringResource("PageAdmin.menu.top.serverTasks"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.list"),
                PageTasks.class);
        submenu.add(list);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.serverTasks.new"),
                PageTaskAdd.class);
        submenu.add(n);

        return item;
    }

    private MainMenuItem createResourcesItems() {
        MainMenuItem item = new MainMenuItem("fa fa-laptop",
                createStringResource("PageAdmin.menu.top.resources"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.resources.list"),
                PageResources.class);
        submenu.add(list);
        MenuItem created = new MenuItem(createStringResource("PageAdmin.menu.top.resources.new"),
                PageResourceWizard.class);
        submenu.add(created);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.resources.import"),
                PageImportObject.class);
        submenu.add(n);

        return item;
    }

    private MainMenuItem createReportsItems() {
        MainMenuItem item = new MainMenuItem("fa fa-pie-chart",
                createStringResource("PageAdmin.menu.top.reports"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.reports.list"),
                PageReports.class);
        submenu.add(list);
        MenuItem created = new MenuItem(createStringResource("PageAdmin.menu.top.reports.created"),
                PageCreatedReports.class);
        submenu.add(created);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.reports.new"),
                PageNewReport.class);
        submenu.add(n);

        return item;
    }

    private MainMenuItem createCertificationItems() {

        MainMenuItem item = new MainMenuItem("fa fa-certificate",
                createStringResource("PageAdmin.menu.top.certification"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.definitions"),
                PageCertDefinitions.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.newDefinition"),
                PageImportObject.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.campaigns"),
                PageCertCampaigns.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.certification.decisions"),
                PageCertDecisions.class);
        submenu.add(menu);

        return item;
    }

    private MainMenuItem createConfigurationItems() {
        MainMenuItem item = new MainMenuItem("fa fa-cog",
                createStringResource("PageAdmin.menu.top.configuration"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.bulkActions"),
                PageBulkAction.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.importObject"),
                PageImportObject.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.repositoryObjects"),
                PageDebugList.class);
        submenu.add(menu);

        PageParameters params = new PageParameters();
        params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_BASIC);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.basic"),
                PageSystemConfiguration.class, params, null);
        submenu.add(menu);

        params = new PageParameters();
        params.add(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_LOGGING);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.logging"),
                PageSystemConfiguration.class, params, null);
        submenu.add(menu);


        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.shadowsDetails"),
                PageAccounts.class);
        submenu.add(menu);
        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.internals"),
                PageInternals.class);
        submenu.add(menu);


        menu = new MenuItem(createStringResource("PageAdmin.menu.top.configuration.about"),
                PageAbout.class);
        submenu.add(menu);

        return item;
    }

    private void createSelfServiceMenu(SideBarMenuItem menu) {
        MainMenuItem item = new MainMenuItem("fa fa-dashboard",
                createStringResource("PageAdmin.menu.dashboard"), PageSelfDashboard.class);
        menu.getItems().add(item);
        item = new MainMenuItem("fa fa-user",
                createStringResource("PageAdmin.menu.profile"), PageSelfProfile.class);
        menu.getItems().add(item);
        item = new MainMenuItem("fa fa-star",
                createStringResource("PageAdmin.menu.assignments"), PageSelfAssignments.class);
        menu.getItems().add(item);
        item = new MainMenuItem("fa fa-shield",
                createStringResource("PageAdmin.menu.credentials"), PageSelfCredentials.class);
        menu.getItems().add(item);
    }

    private MainMenuItem createHomeItems() {
        MainMenuItem item = new MainMenuItem("fa fa-dashboard",
                createStringResource("PageAdmin.menu.dashboard"), PageDashboard.class);

        return item;
    }

    private MainMenuItem createUsersItems() {
        MainMenuItem item = new MainMenuItem("fa fa-group",
                createStringResource("PageAdmin.menu.top.users"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.users.list"), PageUsers.class);
        submenu.add(list);
        MenuItem newUser = new MenuItem(createStringResource("PageAdmin.menu.top.users.new"), PageUser.class);
        submenu.add(newUser);
//        MenuItem search = new MenuItem(createStringResource("PageAdmin.menu.users.search"),
//        PageUsersSearch.class);
//        submenu.add(search);

        return item;
    }

    private MainMenuItem createOrganizationsMenu() {
        MainMenuItem item = new MainMenuItem("fa fa-building",
                createStringResource("PageAdmin.menu.top.users.org"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.users.org.tree"), PageOrgTree.class);
        submenu.add(list);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.users.org.new"), PageOrgUnit.class);
        submenu.add(n);

        return item;
    }

    private MainMenuItem createRolesItems() {
        MainMenuItem item = new MainMenuItem("fa fa-bookmark",
                createStringResource("PageAdmin.menu.top.roles"), null);

        List<MenuItem> submenu = item.getItems();

        MenuItem list = new MenuItem(createStringResource("PageAdmin.menu.top.roles.list"), PageRoles.class);
        submenu.add(list);
        MenuItem n = new MenuItem(createStringResource("PageAdmin.menu.top.roles.new"), PageRole.class);
        submenu.add(n);

        return item;
    }
}
