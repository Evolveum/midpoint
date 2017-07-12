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
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Temporary solution for MID-3098 (expanding both "tasks" and "certification scheduling" menu sections when opening tasks page).
 *
 * TODO show really only certification scheduling tasks (not e.g. remediation ones)
 * TODO decouple settings (e.g. selected task states) from PageTasks
 *
 * @author mederly
 */
@PageDescriptor(url = "/admin/certificationSchedulingTasks", action = {
        @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_URL,
                label = "PageTasks.auth.tasks.label",
                description = "PageTasks.auth.tasks.description")})
public class PageTasksCertScheduling extends PageTasks {

	public PageTasksCertScheduling() {
	}

	public PageTasksCertScheduling(PageParameters parameters) {
		super(parameters);
	}
}
