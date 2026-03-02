/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.services;

import java.io.Serial;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/integrationApplicationService", matchUrlForSecurity = "/admin/integrationApplicationService")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                label = "PageAdminServices.auth.servicesAll.label",
                description = "PageAdminServices.auth.servicesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICE_URL,
                label = "PageService.auth.role.label",
                description = "PageService.auth.role.description") })
public class PageIntegrationApplicationService extends PageApplicationService {

    @Serial private static final long serialVersionUID = 1L;
}
