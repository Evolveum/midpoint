/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home;

import java.util.Arrays;

import com.evolveum.midpoint.prism.query.ObjectQuery;

import org.apache.wicket.Component;
import org.apache.wicket.request.component.IRequestablePage;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.box.InfoBox;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.PersonalInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.component.SystemInfoPanel;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin", matchUrlForSecurity = "/admin"),
                @Url(mountUrl = "/admin/dashboard/info", matchUrlForSecurity = "/admin/dashboard/info")
        },
        action = {
                @AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
                        label = PageAdminHome.AUTH_HOME_ALL_LABEL,
                        description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                        label = "PageDashboard.auth.dashboard.label",
                        description = "PageDashboard.auth.dashboard.description")
        })
public class PageDashboardInfo extends PageDashboard {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageDashboardInfo.class);

    private static final String ID_INFO_BOX_USERS = "infoBoxUsers";
    private static final String ID_INFO_BOX_ORGS = "infoBoxOrgs";
    private static final String ID_INFO_BOX_ROLES = "infoBoxRoles";
    private static final String ID_INFO_BOX_SERVICES = "infoBoxServices";
    private static final String ID_INFO_BOX_RESOURCES = "infoBoxResources";
    private static final String ID_INFO_BOX_TASKS = "infoBoxTasks";

    private static final String ID_PERSONAL_INFO = "personalInfo";
    private static final String ID_SYSTEM_INFO = "systemInfo";

    protected void initLayout() {
        initInfoBoxes();
        initPersonalInfo();
        initSystemInfo();
    }

    private void initInfoBoxes() {
        Task task = createSimpleTask("PageDashboard.infobox");
        OperationResult result = task.getResult();

        add(createFocusInfoBoxPanel(ID_INFO_BOX_USERS, UserType.class, "object-user-bg",
                GuiStyleConstants.CLASS_OBJECT_USER_ICON, "PageDashboard.infobox.users", PageUsers.class,
                result, task));

        add(createFocusInfoBoxPanel(ID_INFO_BOX_ORGS, OrgType.class, "object-org-bg",
                GuiStyleConstants.CLASS_OBJECT_ORG_ICON, "PageDashboard.infobox.orgs", PageOrgTree.class,
                result, task));

        add(createFocusInfoBoxPanel(ID_INFO_BOX_ROLES, RoleType.class, "object-role-bg",
                GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, "PageDashboard.infobox.roles", PageRoles.class,
                result, task));

        add(createFocusInfoBoxPanel(ID_INFO_BOX_SERVICES, ServiceType.class, "object-service-bg",
                GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON, "PageDashboard.infobox.services", PageServices.class,
                result, task));

        add(createResourceInfoBoxPanel(result, task));
        add(createTaskInfoBoxPanel(result, task));
    }

    private <F extends FocusType> InfoBox createFocusInfoBoxPanel(String id, Class<F> type, String bgColor,
            String icon, String keyPrefix, Class<? extends IRequestablePage> linkPage, OperationResult result, Task task) {
        return new InfoBox(id, getFocusInfoBoxType(type, bgColor, icon, keyPrefix, result, task, linkPage));
    }

    private Component createResourceInfoBoxPanel(OperationResult result, Task task) {
        ObjectQuery totalQuery = getPrismContext().queryFor(ResourceType.class)
                .block()
                .not()
                .item(ResourceType.F_TEMPLATE).eq(true)
                .endBlock()
                .and()
                .block()
                .not()
                .item(ResourceType.F_ABSTRACT).eq(true)
                .endBlock()
                .and()
                .block()
                .not()
                .item(ResourceType.F_ARCHETYPE_REF).ref("00000000-0000-0000-0000-000000000703")
                .endBlock()
                .build();
        ObjectQuery activeQuery = getPrismContext().queryFor(ResourceType.class)
                .item(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS)
                .eq(AvailabilityStatusType.UP)
                .build();
        activeQuery.addFilter(totalQuery.getFilter());
        return new InfoBox(ID_INFO_BOX_RESOURCES, getObjectInfoBoxTypeModel(ResourceType.class,
                totalQuery, activeQuery, "object-resource-bg", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON,
                "PageDashboard.infobox.resources", result, task, PageResources.class));
    }

    private Component createTaskInfoBoxPanel(OperationResult result, Task task) {
        ObjectQuery query = getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATE).eq(TaskExecutionStateType.RUNNABLE)
                .build();
        return new InfoBox(ID_INFO_BOX_TASKS, getObjectInfoBoxTypeModel(TaskType.class, null, query, "object-task-bg",
                GuiStyleConstants.CLASS_OBJECT_TASK_ICON, "PageDashboard.infobox.tasks", result, task,
                PageTasks.class));
    }

    private void initPersonalInfo() {
        DashboardPanel personalInfo = new DashboardPanel(ID_PERSONAL_INFO, null,
                createStringResource("PageDashboard.personalInfo"), GuiStyleConstants.CLASS_OBJECT_USER_BOX_CSS_CLASSES,
                GuiStyleConstants.CLASS_OBJECT_USER_BOX_CSS_CLASSES) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Component getMainComponent(String componentId) {
                return new PersonalInfoPanel(componentId);
            }
        };
        add(personalInfo);
    }

    private void initSystemInfo() {
        DashboardPanel systemInfo = new DashboardPanel(ID_SYSTEM_INFO, null,
                createStringResource("PageDashboard.systemInfo"),
                GuiStyleConstants.CLASS_ICON_TACHOMETER, GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_CSS_CLASSES) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Component getMainComponent(String componentId) {
                return new SystemInfoPanel(componentId);
            }
        };
        add(systemInfo);
    }
}
