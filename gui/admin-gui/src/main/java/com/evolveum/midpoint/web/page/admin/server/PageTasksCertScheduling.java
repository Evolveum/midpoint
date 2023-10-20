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
import com.evolveum.midpoint.web.application.Url;

import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = @Url(mountUrl = "/admin/certificationSchedulingTasks"),
        action = {
                @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                        label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                        description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_URL,
                        label = "PageTasks.auth.tasks.label",
                        description = "PageTasks.auth.tasks.description") })
public class PageTasksCertScheduling extends PageTasks {

    public static final String COLLECTION_NAME = "certification-tasks-view";

    public PageTasksCertScheduling() {
        this(new PageParameters());
    }

    public PageTasksCertScheduling(PageParameters parameters) {
        super(parameters);

        parameters.set(PageTasks.PARAMETER_OBJECT_COLLECTION_NAME, COLLECTION_NAME);
    }
}
