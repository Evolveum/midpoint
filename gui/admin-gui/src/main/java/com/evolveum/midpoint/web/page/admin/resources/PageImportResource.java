/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;

/**
 * Created by honchar
 * this class is created to link Import resource definition
 * menu item to a separate class (to fix menu item enabling issue)
 *
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/importResource")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                label = "PageAdminResources.auth.resourcesAll.label",
                description = "PageAdminResources.auth.resourcesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL,
                label = "PageImportObject.auth.configImport.label", description = "PageImportObject.auth.configImport.description")})
public class PageImportResource extends PageImportObject {
    public PageImportResource(){

    }
}
