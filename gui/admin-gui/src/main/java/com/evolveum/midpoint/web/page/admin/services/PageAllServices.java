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
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * @author katkav
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/services")
        },
        action = {
                @AuthorizationAction(
                        actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                        label = "PageAdminServices.auth.servicesAll.label",
                        description = "PageAdminServices.auth.servicesAll.description"),
                @AuthorizationAction(
                        actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_URL,
                        label = "PageServices.auth.services.label",
                        description = "PageServices.auth.services.description"),
                @AuthorizationAction(
                        actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_VIEW_URL,
                        label = "PageServices.auth.services.view.label",
                        description = "PageServices.auth.services.view.description") })
@CollectionInstance(identifier = "allServices", applicableForType = ServiceType.class,
        display = @PanelDisplay(
                label = "PageAdmin.menu.top.services.list",
                singularLabel = "ObjectType.service",
                icon = GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON))
@SuppressWarnings("unused")
public class PageAllServices extends PageServices {

    @Serial private static final long serialVersionUID = 1L;

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.TABLE_SERVICES;
    }
}
