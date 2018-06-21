package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;

/**
 * Created by honchar
 * this class is created to link Import resource definition
 * menu item to a separate class (to fix menu item enabling issue)
 *
 */
@PageDescriptor(url = "/admin/config/importResource", action = {
        @AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL,
                label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL,
                description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL,
                label = "PageImportObject.auth.configImport.label", description = "PageImportObject.auth.configImport.description")})
public class PageImportResource extends PageImportObject {
    public PageImportResource(){

    }
}
