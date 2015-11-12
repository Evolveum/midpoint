package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;

/**
 * Created by honchar
 * this class is created to link Import resource definition
 * menu item to a separate class (to fix menu item enabling issue)
 *
 */
@PageDescriptor(url = "/admin/config/import", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL,
                label = "PageImportObject.auth.configImport.label", description = "PageImportObject.auth.configImport.description")})
public class PageImportResource extends PageImportObject {
    public PageImportResource(){

    }
}
