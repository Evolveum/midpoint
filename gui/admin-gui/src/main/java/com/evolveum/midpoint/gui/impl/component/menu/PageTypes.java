/*
 * Copyright (c) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.menu;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.archetype.PageArchetype;
import com.evolveum.midpoint.gui.impl.page.admin.objectcollection.PageObjectCollection;
import com.evolveum.midpoint.gui.impl.page.admin.objecttemplate.PageObjectTemplate;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrg;
import com.evolveum.midpoint.gui.impl.page.admin.report.PageReport;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.service.PageService;
import com.evolveum.midpoint.gui.impl.page.admin.task.PageTask;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.archetype.PageArchetypes;
import com.evolveum.midpoint.web.page.admin.cases.PageCases;
import com.evolveum.midpoint.web.page.admin.objectCollection.PageObjectCollections;
import com.evolveum.midpoint.web.page.admin.objectTemplate.PageObjectTemplates;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgs;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

//TODO remove in 4.5
public enum PageTypes {

    USER("users", GuiStyleConstants.CLASS_OBJECT_USER_ICON, PageUsers.class, PageUser.class, com.evolveum.midpoint.web.page.admin.users.PageUser.class, UserType.COMPLEX_TYPE),
    ROLE("roles", GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, PageRoles.class, PageRole.class, com.evolveum.midpoint.web.page.admin.roles.PageRole.class, RoleType.COMPLEX_TYPE),
    SERVICE("services", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON, PageServices.class, PageService.class, com.evolveum.midpoint.web.page.admin.services.PageService.class, ServiceType.COMPLEX_TYPE),
    ORG("orgs", GuiStyleConstants.CLASS_OBJECT_ORG_ICON, PageOrgs.class, PageOrg.class, PageOrgUnit.class, OrgType.COMPLEX_TYPE),
    TASK("tasks", GuiStyleConstants.CLASS_OBJECT_TASK_ICON, PageTasks.class, PageTask.class, com.evolveum.midpoint.web.page.admin.server.PageTask.class, TaskType.COMPLEX_TYPE),
    RESOURCE("resources", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON, PageResources.class, PageResource.class, PageResourceWizard.class, ResourceType.COMPLEX_TYPE),
    CASE("cases", GuiStyleConstants.EVO_CASE_OBJECT_ICON, PageCases.class, null, null, CaseType.COMPLEX_TYPE),
    ARCHETYPE("archetypes", GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON, PageArchetypes.class, PageArchetype.class, com.evolveum.midpoint.web.page.admin.archetype.PageArchetype.class, ArchetypeType.COMPLEX_TYPE),
    OBJECT_COLLECTION("objectCollections", GuiStyleConstants.CLASS_OBJECT_COLLECTION_ICON, PageObjectCollections.class, PageObjectCollection.class, com.evolveum.midpoint.web.page.admin.objectCollection.PageObjectCollection.class, ObjectCollectionType.COMPLEX_TYPE),
    OBJECT_TEMPLATE("objectTemplates", GuiStyleConstants.CLASS_OBJECT_TEMPLATE_ICON, PageObjectTemplates.class, PageObjectTemplate.class, com.evolveum.midpoint.web.page.admin.objectTemplate.PageObjectTemplate.class, ObjectTemplateType.COMPLEX_TYPE),
    REPORT("reports", GuiStyleConstants.CLASS_REPORT_ICON, PageReports .class, PageReport.class, com.evolveum.midpoint.web.page.admin.reports.PageReport.class, ReportType.COMPLEX_TYPE);

    private String identifier;
    private String icon;
    private Class<? extends PageBase> listClass;
    private Class<? extends PageBase> detailsPage;
    private Class<? extends PageAdmin> oldDetailsPage;
    private QName typeName;

    PageTypes(String identifier, String icon, Class<? extends PageBase> listClass, Class<? extends PageBase> detailsPage, Class<? extends PageAdmin> oldDetailsPage, QName typeName) {
        this.identifier = identifier;
        this.icon = icon;
        this.listClass = listClass;
        this.detailsPage = detailsPage;
        this.oldDetailsPage = oldDetailsPage;
        this.typeName = typeName;
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

    public Class<? extends PageBase> getDetailsPage() {
        return detailsPage;
    }

    public Class<? extends PageAdmin> getOldDetailsPage() {
        return oldDetailsPage;
    }

    public QName getTypeName() {
        return typeName;
    }
}
