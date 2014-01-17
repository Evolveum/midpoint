/*
 * Copyright (c) 2010-2014 Evolveum
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

import static com.evolveum.midpoint.common.security.AuthorizationConstants.*;

/**
 * @author lazyman
 */
public enum PageUrlMapping {

    ADMIN_USER_DETAILS("/admin/user/**", new String[]{AUTZ_UI_USER_DETAILS_URL, AUTZ_UI_USERS_ALL_URL}),
    TASK_DETAILS("/admin/task/**", new String[]{AUTZ_UI_TASK_DETAIL_URL, AUTZ_UI_TASKS_ALL_URL}),
    ROLE_DETAILS("/admin/role/**", new String[]{AUTZ_UI_ROLE_DETAILS_URL, AUTZ_UI_ROLES_ALL_URL}),
    RESOURCE_DETAILS("/admin/resource/**", new String[]{AUTZ_UI_RESOURCE_DETAILS_URL, AUTZ_UI_RESOURCES_ALL_URL});

    private String url;

    private String[] action;

    private PageUrlMapping(String url, String[] action) {
        this.url = url;
        this.action = action;
    }

    public String[] getAction() {
        return action;
    }

    public String getUrl() {
        return url;
    }
}
