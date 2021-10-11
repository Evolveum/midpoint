/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
