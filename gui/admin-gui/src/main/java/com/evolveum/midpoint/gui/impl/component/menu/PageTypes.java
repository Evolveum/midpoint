/*
 * Copyright (c) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.menu;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.application.PageApplication;
import com.evolveum.midpoint.gui.impl.page.admin.application.PageApplications;
import com.evolveum.midpoint.gui.impl.page.admin.archetype.PageArchetype;
import com.evolveum.midpoint.gui.impl.page.admin.messagetemplate.PageMessageTemplate;
import com.evolveum.midpoint.gui.impl.page.admin.messagetemplate.PageMessageTemplates;
import com.evolveum.midpoint.gui.impl.page.admin.objectcollection.PageObjectCollection;
import com.evolveum.midpoint.gui.impl.page.admin.objecttemplate.PageObjectTemplate;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrg;
import com.evolveum.midpoint.gui.impl.page.admin.policy.PagePolicies;
import com.evolveum.midpoint.gui.impl.page.admin.policy.PagePolicy;
import com.evolveum.midpoint.gui.impl.page.admin.report.PageReport;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysis;
import com.evolveum.midpoint.gui.impl.page.admin.schema.PageSchema;
import com.evolveum.midpoint.gui.impl.page.admin.schema.PageSchemas;
import com.evolveum.midpoint.gui.impl.page.admin.service.PageService;
import com.evolveum.midpoint.gui.impl.page.admin.mark.PageMark;
import com.evolveum.midpoint.gui.impl.page.admin.mark.PageMarks;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResult;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.page.PageSimulationResults;
import com.evolveum.midpoint.gui.impl.page.admin.task.PageTask;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.page.admin.archetype.PageArchetypes;
import com.evolveum.midpoint.web.page.admin.cases.PageCases;
import com.evolveum.midpoint.web.page.admin.certification.PageCertDecisions;
import com.evolveum.midpoint.web.page.admin.objectCollection.PageObjectCollections;
import com.evolveum.midpoint.web.page.admin.objectTemplate.PageObjectTemplates;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgs;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//TODO remove in future
public enum PageTypes {

    USER("users", GuiStyleConstants.CLASS_OBJECT_USER_ICON, PageUsers.class, PageUser.class, UserType.COMPLEX_TYPE),
    ROLE("roles", GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, PageRoles.class, PageRole.class, RoleType.COMPLEX_TYPE),
    SERVICE("services", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON, PageServices.class, PageService.class, ServiceType.COMPLEX_TYPE),
    POLICY("policies", GuiStyleConstants.CLASS_OBJECT_POLICY_ICON, PagePolicies.class, PagePolicy.class, PolicyType.COMPLEX_TYPE),
    ORG("orgs", GuiStyleConstants.CLASS_OBJECT_ORG_ICON, PageOrgs.class, PageOrg.class, OrgType.COMPLEX_TYPE),
    TASK("tasks", GuiStyleConstants.CLASS_OBJECT_TASK_ICON, PageTasks.class, PageTask.class, TaskType.COMPLEX_TYPE),
    RESOURCE("resources", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON, PageResources.class, PageResource.class, ResourceType.COMPLEX_TYPE),
    CASE("cases", GuiStyleConstants.EVO_CASE_OBJECT_ICON, PageCases.class, null, CaseType.COMPLEX_TYPE),
    ARCHETYPE("archetypes", GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON, PageArchetypes.class, PageArchetype.class, ArchetypeType.COMPLEX_TYPE),
    MESSAGE_TEMPLATES("messageTemplates", GuiStyleConstants.EVO_MESSAGE_TEMPLATE_TYPE_ICON, PageMessageTemplates.class, PageMessageTemplate.class, MessageTemplateType.COMPLEX_TYPE),
    OBJECT_COLLECTION("objectCollections", GuiStyleConstants.CLASS_OBJECT_COLLECTION_ICON, PageObjectCollections.class, PageObjectCollection.class, ObjectCollectionType.COMPLEX_TYPE),
    OBJECT_TEMPLATE("objectTemplates", GuiStyleConstants.CLASS_OBJECT_TEMPLATE_ICON, PageObjectTemplates.class, PageObjectTemplate.class, ObjectTemplateType.COMPLEX_TYPE),
    REPORT("reports", GuiStyleConstants.CLASS_REPORT_ICON, PageReports.class, PageReport.class, ReportType.COMPLEX_TYPE),
    SIMULATION_RESULT("simulationResults", GuiStyleConstants.CLASS_SIMULATION_RESULT, PageSimulationResults.class, PageSimulationResult.class, SimulationResultType.COMPLEX_TYPE),
    MARK("marks", GuiStyleConstants.CLASS_MARK, PageMarks.class, PageMark.class, MarkType.COMPLEX_TYPE),
    SCHEMA("schemas", GuiStyleConstants.CLASS_ICON_RESOURCE_SCHEMA, PageSchemas.class, PageSchema.class, SchemaType.COMPLEX_TYPE),
    ACCESS_CERT_ITEM_TYPE("certItems", GuiStyleConstants.CLASS_OBJECT_CERT_CAMPAIGN_ICON, PageCertDecisions.class, null, AccessCertificationWorkItemType.COMPLEX_TYPE),
    ANALYTICS("analytics", GuiStyleConstants.CLASS_SIMULATION_RESULT, PageRoleAnalysis.class, PageRoleAnalysis.class, RoleAnalysisSessionType.COMPLEX_TYPE),
    APPLICATION("applications", GuiStyleConstants.CLASS_OBJECT_APPLICATION_ICON, PageApplications.class, PageApplication.class, ApplicationType.COMPLEX_TYPE);


    private String identifier;
    private String icon;
    private Class<? extends PageBase> listClass;
    private Class<? extends PageBase> detailsPage;
    private QName typeName;

    PageTypes(String identifier, String icon, Class<? extends PageBase> listClass, Class<? extends PageBase> detailsPage, QName typeName) {
        this.identifier = identifier;
        this.icon = icon;
        this.listClass = listClass;
        this.detailsPage = detailsPage;
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

    public QName getTypeName() {
        return typeName;
    }

    public static PageTypes getPageTypesByType(QName type) {
        for (PageTypes pageType : values()) {
            if (QNameUtil.match(pageType.typeName, type)) {
                return pageType;
            }
        }
        return null;
    }
}
