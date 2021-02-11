package com.evolveum.midpoint.gui.impl.component.menu;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.cases.PageCases;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgs;
import com.evolveum.midpoint.web.page.admin.resources.PageResource;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTask;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;

public enum PageTypes {
    USER("users", GuiStyleConstants.CLASS_OBJECT_USER_ICON, PageUsers.class, PageUser.class),
    ROLE("roles", GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, PageRoles.class, PageRole.class),
    SERVICE("services", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON, PageServices.class, PageService.class),
    ORG("orgs", GuiStyleConstants.CLASS_OBJECT_ORG_ICON, PageOrgs.class, PageOrgUnit.class),
    TASK("tasks", GuiStyleConstants.CLASS_OBJECT_TASK_ICON, PageTasks.class, PageTask.class),
    RESOURCE("resources", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON, PageResources.class, PageResourceWizard.class),
    CASE("cases", GuiStyleConstants.EVO_CASE_OBJECT_ICON, PageCases.class, null);

    private String identifier;
    private String icon;
    private Class<? extends PageBase> listClass;
    private Class<? extends PageAdmin> detailsPage;

    PageTypes(String identifier, String icon, Class<? extends PageBase> listClass, Class<? extends PageAdmin> detailsPage) {
        this.identifier = identifier;
        this.icon = icon;
        this.listClass = listClass;
        this.detailsPage = detailsPage;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getIcon() {
        return icon;
    }

    public Class<? extends PageBase> getListClass() {
        return listClass;
    }

    public Class<? extends PageAdmin> getDetailsPage() {
        return detailsPage;
    }
}
