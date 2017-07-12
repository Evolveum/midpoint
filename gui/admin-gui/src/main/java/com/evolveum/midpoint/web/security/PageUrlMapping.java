/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.application.AuthorizationActionValue;

import static com.evolveum.midpoint.security.api.AuthorizationConstants.*;

/**
 * @author lazyman
 */
public enum PageUrlMapping {

    ADMIN_USER_DETAILS("/admin/user/**", new DisplayableValue[]{
            new AuthorizationActionValue(AUTZ_UI_USER_DETAILS_URL,
                    "PageAdminUsers.authUri.userDetails.label", "PageAdminUsers.authUri.userDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_USERS_ALL_URL,
                    "PageAdminUsers.authUri.usersAll.label", "PageAdminUsers.authUri.usersAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminUsers.authUri.usersAll.label", "PageAdminUsers.authUri.guiAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_DEPRECATED_URL,
                    "PageAdminUsers.authUri.usersAll.label", "PageAdminUsers.authUri.guiAll.description")
    }),
    TASK_DETAILS("/admin/task/**", new DisplayableValue[]{
            new AuthorizationActionValue(AUTZ_UI_TASK_DETAIL_URL,
                    "PageAdminTasks.authUri.taskDetails.label", "PageAdminTasks.authUri.taskDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_TASKS_ALL_URL,
                    "PageAdminTasks.authUri.tasksAll.label", "PageAdminTasks.authUri.tasksAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminTasks.authUri.tasksAll.label", "PageAdminTasks.authUri.guiAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_DEPRECATED_URL,
                    "PageAdminTasks.authUri.tasksAll.label", "PageAdminTasks.authUri.guiAll.description")
    }),
    ROLE_DETAILS("/admin/role/**", new DisplayableValue[]{
            new AuthorizationActionValue(AUTZ_UI_ROLE_DETAILS_URL,
                    "PageAdminRoles.authUri.roleDetails.label", "PageAdminRoles.authUri.roleDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_ROLES_ALL_URL,
                    "PageAdminRoles.authUri.rolesAll.label", "PageAdminRoles.authUri.rolesAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminRoles.authUri.rolesAll.label", "PageAdminRoles.authUri.guiAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_DEPRECATED_URL,
                    "PageAdminRoles.authUri.rolesAll.label", "PageAdminRoles.authUri.guiAll.description")
    }),
    RESOURCE_DETAILS("/admin/resource/**", new DisplayableValue[]{
            new AuthorizationActionValue(AUTZ_UI_RESOURCE_DETAILS_URL,
                    "PageAdminResources.authUri.resourceDetails.label", "PageAdminResources.authUri.resourceDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_RESOURCES_ALL_URL,
                    "PageAdminResources.authUri.resourcesAll.label", "PageAdminResources.authUri.resourcesAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminRoles.authUri.rolesAll.label", "PageAdminRoles.authUri.guiAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_DEPRECATED_URL,
                    "PageAdminRoles.authUri.rolesAll.label", "PageAdminRoles.authUri.guiAll.description")
    });

    private String url;

    private DisplayableValue[] action;

    private PageUrlMapping(String url, DisplayableValue[] action) {
        this.url = url;
        this.action = action;
    }

    public DisplayableValue[] getAction() {
        return action;
    }

    public String getUrl() {
        return url;
    }
}
