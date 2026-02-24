/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.services;

import java.io.Serial;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * @author katkav
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/integrationApplicationServices")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                        label = "PageAdminServices.auth.servicesAll.label",
                        description = "PageAdminServices.auth.servicesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_URL,
                        label = "PageServices.auth.services.label",
                        description = "PageServices.auth.services.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_VIEW_URL,
                        label = "PageServices.auth.services.view.label",
                        description = "PageServices.auth.services.view.description") })
@SuppressWarnings("unused")
public class PageIntegrationServicesApplications extends PageServicesApplications {

    @Serial private static final long serialVersionUID = 1L;

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.TABLE_SERVICES_APPLICATIONS_INTEGRATION;
    }
}
