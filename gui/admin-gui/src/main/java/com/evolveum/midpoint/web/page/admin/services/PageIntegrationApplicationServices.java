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
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_INTEGRATION_APPLICATIONS_URL,
                        label = "PageAdminServices.auth.integrationApplicationServices.label",
                        description = "PageAdminServices.auth.servicesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_INTEGRATION_APPLICATIONS_URL,
                        label = "PageServices.auth.integrationApplicationServices.label",
                        description = "PageServices.auth.integrationApplicationServices.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_INTEGRATION_APPLICATIONS_VIEW_URL,
                        label = "PageServices.auth.integrationApplicationServices.view.label",
                        description = "PageServices.auth.integrationApplicationServices.view.description") })
@SuppressWarnings("unused")
public class PageIntegrationApplicationServices extends PageApplicationServices {

    @Serial private static final long serialVersionUID = 1L;

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.TABLE_SERVICES_APPLICATIONS_INTEGRATION;
    }
}
