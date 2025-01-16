/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.api.authorization;

import static com.evolveum.midpoint.security.api.AuthorizationConstants.*;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.security.api.RestAuthorizationAction;

import java.util.Arrays;

/**
 * @author lazyman
 */
public enum EndPointsUrlMapping {

    USER_DETAILS("/admin/user/**",
            new AuthorizationActionValue(AUTZ_UI_USER_DETAILS_URL,
                    "PageAdminUsers.authUri.userDetails.label", "PageAdminUsers.authUri.userDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_USERS_ALL_URL,
                    "PageAdminUsers.authUri.usersAll.label", "PageAdminUsers.authUri.usersAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminUsers.authUri.usersAll.label", "PageAdminUsers.authUri.guiAll.description")),

    TASK_DETAILS("/admin/task/**",
            new AuthorizationActionValue(AUTZ_UI_TASK_DETAIL_URL,
                    "PageAdminTasks.authUri.taskDetails.label", "PageAdminTasks.authUri.taskDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_TASKS_ALL_URL,
                    "PageAdminTasks.authUri.tasksAll.label", "PageAdminTasks.authUri.tasksAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminTasks.authUri.tasksAll.label", "PageAdminTasks.authUri.guiAll.description")),
    ROLE_DETAILS("/admin/role/**",
            new AuthorizationActionValue(AUTZ_UI_ROLE_DETAILS_URL,
                    "PageAdminRoles.authUri.roleDetails.label", "PageAdminRoles.authUri.roleDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_ROLES_ALL_URL,
                    "PageAdminRoles.authUri.rolesAll.label", "PageAdminRoles.authUri.rolesAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminRoles.authUri.rolesAll.label", "PageAdminRoles.authUri.guiAll.description")),


    SCHEMA_DETAILS("/admin/schema/**",
            new AuthorizationActionValue(AUTZ_UI_SCHEMA_DETAILS_URL,
                    "PageAdminSchemas.authUri.schemaDetails.label", "PageAdminSchemas.authUri.schemaDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_SCHEMAS_ALL_URL,
                    "PageAdminSchemas.authUri.schemasAll.label", "PageAdminSchemas.authUri.schemasAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminSchemas.authUri.guiAll.label", "PageAdminSchemas.authUri.guiAll.description")),


    //TODO authz
    ROLE_MINING_DETAILS("/admin/roleAnalysis/**",
            new AuthorizationActionValue(AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                    "PageRoleAnalysis.auth.roleAnalysisAll.label", "PageRoleAnalysis.authUri.roleAnalysisAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminCluster.authUri.rolesAll.label", "PageAdminCluster.authUri.guiAll.description")),
    //TODO authz
    ROLE_MINING_SESSION_DETAILS("/admin/roleAnalysisSession/**",
            new AuthorizationActionValue(AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                    "PageAdminSession.authUri.roleDetails.label", "PageAdminSession.authUri.roleDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_ROLE_ANALYSIS_SESSION_URL,
                    "PageAdminSession.authUri.rolesAll.label", "PageAdminSession.authUri.rolesAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminCluster.authUri.rolesAll.label", "PageAdminCluster.authUri.guiAll.description")),
    //TODO authz
    ROLE_MINING_CLUSTER_DETAILS("/admin/roleAnalysisCluster/**",
            new AuthorizationActionValue(AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                    "PageAdminCluster.authUri.roleDetails.label", "PageAdminCluster.authUri.roleDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_ROLE_ANALYSIS_CLUSTER_URL,
                    "PageAdminCluster.authUri.rolesAll.label", "PageAdminCluster.authUri.rolesAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminCluster.authUri.rolesAll.label", "PageAdminCluster.authUri.guiAll.description")),

    ROLE_ANALYSIS_OUTLIER_DETAILS("/admin/roleAnalysisOutlier/**",
            new AuthorizationActionValue(AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                    "PageAdminSession.authUri.roleDetails.label", "PageAdminSession.authUri.roleDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_ROLE_ANALYSIS_SESSION_URL,
                    "PageAdminSession.authUri.rolesAll.label", "PageAdminSession.authUri.rolesAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminCluster.authUri.rolesAll.label", "PageAdminCluster.authUri.guiAll.description")),

    ORG_DETAILS("/admin/org/unit/**",
            new AuthorizationActionValue(AUTZ_UI_ORG_ALL_URL,
                    "PageAdminUsers.auth.orgAll.label", "PageAdminUsers.auth.orgAll.description"),
            new AuthorizationActionValue(AUTZ_UI_ORG_UNIT_URL,
                    "PageOrgUnit.auth.orgUnit.label", "PageOrgUnit.auth.orgUnit.description")),
    ORG_NEW_DETAILS("/admin/org/**",
            new AuthorizationActionValue(AUTZ_UI_ORG_ALL_URL,
                    "PageAdminUsers.auth.orgAll.label", "PageAdminUsers.auth.orgAll.description"),
            new AuthorizationActionValue(AUTZ_UI_ORG_DETAILS_URL,
                    "PageOrgUnit.auth.orgUnit.label", "PageOrgUnit.auth.orgUnit.description")),
    SERVICE_NEW_DETAILS("/admin/service/**",
            new AuthorizationActionValue(AUTZ_UI_SERVICE_DETAILS_URL,
                    "PageAdminRoles.authUri.serviceDetails.label", "PageAdminRoles.authUri.serviceDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_SERVICES_ALL_URL,
                    "PageAdminRoles.authUri.servicesAll.label", "PageAdminRoles.authUri.servicesAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminRoles.authUri.guiAll.label", "PageAdminRoles.authUri.guiAll.description")),
    POLICY_NEW_DETAILS("/admin/policy/**",
            new AuthorizationActionValue(AUTZ_UI_POLICY_DETAILS_URL,
                    "PageAdminPolicies.authUri.policyDetails.label", "PageAdminPolicies.authUri.policyDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_POLICIES_ALL_URL,
                    "PageAdminPolicies.authUri.policiesAll.label", "PageAdminPolicies.authUri.policiesAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminPolicies.authUri.guiAll.label", "PageAdminPolicies.authUri.guiAll.description")),
    RESOURCE_DETAILS("/admin/resource/**",
            new AuthorizationActionValue(AUTZ_UI_RESOURCE_DETAILS_URL,
                    "PageAdminResources.authUri.resourceDetails.label", "PageAdminResources.authUri.resourceDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_RESOURCES_ALL_URL,
                    "PageAdminResources.authUri.resourcesAll.label", "PageAdminResources.authUri.resourcesAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminRoles.authUri.rolesAll.label", "PageAdminRoles.authUri.guiAll.description")),
    WORK_ITEM_DETAILS("/admin/workItem/**",
            new AuthorizationActionValue(AUTZ_UI_WORK_ITEM_URL,
                    "PageCaseWorkItem.authUri.workItemDetails.label", "PageCaseWorkItem.authUri.workItemDetails.description"),
            new AuthorizationActionValue(AUTZ_UI_WORK_ITEMS_ALL_URL,
                    "PageCaseWorkItems.authUri.workItemsAll.label", "PageAdminResources.authUri.workItemsAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageCaseWorkItems.authUri.guiAll.label", "PageAdminRoles.authUri.guiAll.description")),
    CASE_DETAILS("/admin/case/**",
            new AuthorizationActionValue(AUTZ_UI_CASES_ALL_URL,
                    "PageAdminCases.auth.casesAll.label", "PageAdminCases.auth.casesAll.description"),
            new AuthorizationActionValue(AUTZ_UI_CASE_URL,
                    "PageCase.auth.case.label", "PageCase.auth.case.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageCaseWorkItems.authUri.guiAll.label", "PageAdminRoles.authUri.guiAll.description")),
    OBJECT_COLLECTION_DETAILS("/admin/objectCollection/**",
            new AuthorizationActionValue(AuthConstants.AUTH_CONFIGURATION_ALL,
                    AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
            new AuthorizationActionValue(AUTZ_UI_OBJECT_COLLECTIONS_ALL_URL,
                    "PageObjectCollection.auth.objectCollectionsAll.label", "PageObjectCollection.auth.objectCollectionsAll.description"),
            new AuthorizationActionValue(AUTZ_UI_OBJECT_COLLECTION_URL,
                    "PageObjectCollection.auth.objectCollection.label", "PageObjectCollection.auth.objectCollection.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageObjectCollections.authUri.guiAll.label", "PageObjectCollections.authUri.guiAll.description")),
    OBJECT_TEMPLATE_DETAILS("/admin/objectTemplate/**",
            new AuthorizationActionValue(AuthConstants.AUTH_CONFIGURATION_ALL,
                    AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
            new AuthorizationActionValue(AUTZ_UI_OBJECT_TEMPLATES_ALL_URL,
                    "PageObjectTemplate.auth.objectTemplatesAll.label", "PageObjectTemplate.auth.objectTemplateAll.description"),
            new AuthorizationActionValue(AUTZ_UI_OBJECT_TEMPLATE_URL,
                    "PageObjectTemplate.auth.objectTemplate.label", "PageObjectTemplate.auth.objectTemplate.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageObjectTemplates.authUri.guiAll.label", "PageObjectTemplates.authUri.guiAll.description")),
    ARCHETYPE_DETAILS("/admin/archetype/**",
            new AuthorizationActionValue(AuthConstants.AUTH_CONFIGURATION_ALL,
                    AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
            new AuthorizationActionValue(AUTZ_UI_ARCHETYPES_ALL_URL,
                    "PageArchetypes.auth.archetypesAll.label", "PageArchetypes.auth.archetypesAll.description"),
            new AuthorizationActionValue(AUTZ_UI_ARCHETYPE_URL,
                    "PageArchetype.auth.user.label", "PageArchetype.auth.archetype.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageCaseWorkItems.authUri.guiAll.label", "PageAdminRoles.authUri.guiAll.description")),
    REPORT_DETAILS("/admin/report/**",
            new AuthorizationActionValue(AUTZ_UI_REPORTS_ALL_URL,
                    "PageAdminCases.auth.reportsAll.label", "PageAdminCases.auth.reportsAll.description"),
            new AuthorizationActionValue(AUTZ_UI_REPORT_URL,
                    "PageReport.auth.report.label", "PageReport.auth.report.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageCaseWorkItems.authUri.guiAll.label", "PageAdminRoles.authUri.guiAll.description")),

    CAMPAIGN_DETAILS("/admin/certification/campaign/**",
            new AuthorizationActionValue(AUTZ_UI_CERTIFICATION_ALL_URL,
                    "PageAdminCertification.auth.certificationAll.label",
                    "PageAdminCertification.auth.certificationAll.description"),
            new AuthorizationActionValue(AUTZ_UI_CERTIFICATION_CAMPAIGN_URL,
                    "PageAdminCertification.auth.certificationAll.label",
                    "PageAdminCertification.auth.certificationAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminCertification.auth.certificationCampaigns.label",
                    "PageAdminCertification.auth.certificationCampaigns.description")),

    CAMPAIGNS("/admin/certification/campaigns/**",
            new AuthorizationActionValue(AUTZ_UI_CERTIFICATION_ALL_URL,
                    "PageAdminCertification.auth.certificationAll.label",
                    "PageAdminCertification.auth.certificationAll.description"),
            new AuthorizationActionValue(AUTZ_UI_CERTIFICATION_CAMPAIGNS_URL,
                    "PageAdminCertification.auth.certificationCampaigns.label",
                    "PageAdminCertification.auth.certificationCampaigns.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminCertification.auth.certificationCampaigns.label",
                    "PageAdminCertification.auth.certificationCampaigns.description")),

    //TODO better names for authorizations
    CAMPAIGN_DECISIONS_DETAILS("/admin/certification/decisionsAll/**",
            new AuthorizationActionValue(AUTZ_UI_CERTIFICATION_ALL_URL,
                    "PageAdminCertification.auth.certificationAll.label",
                    "PageAdminCertification.auth.certificationAll.description"),
            new AuthorizationActionValue(AUTZ_UI_CERTIFICATION_DECISIONS_URL,
                    "PageAdminCertification.auth.decisions.label",
                    "PageAdminCertification.auth.decisions.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageAdminCertification.auth.certificationCampaigns.label",
                    "PageAdminCertification.auth.certificationCampaigns.description")),

    SIMULATION_DETAILS("/admin/simulation/**",
            new AuthorizationActionValue(AUTZ_UI_SIMULATION_RESULT_URL,
                    "PageSimulationResults.auth.simulationResult.label", "PageSimulationResults.auth.simulationResult.description"),
            new AuthorizationActionValue(AUTZ_UI_SIMULATIONS_ALL_URL,
                    "PageSimulationResults.auth.simulationsAll.label", "PageSimulationResults.auth.simulationsAll.description"),
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "PageSimulationResults.authUri.simulationsAll.label", "PageSimulationResults.authUri.simulationsAll.description")),

    ACTUATOR("/actuator/**",
            new AuthorizationActionValue(AUTZ_ACTUATOR_ALL_URL,
                    "ActuatorEndpoint.authActuator.all.label", "ActuatorEndpoint.authActuator.all.description")),
    ACTUATOR_THREAD_DUMP("/actuator/threaddump",
            new AuthorizationActionValue(AUTZ_ACTUATOR_THREAD_DUMP_URL,
                    "ActuatorEndpoint.authActuator.threadDump.label", "ActuatorEndpoint.authActuator.threadDump.description")),
    ACTUATOR_HEAP_DUMP("/actuator/heapdump",
            new AuthorizationActionValue(AUTZ_ACTUATOR_HEAP_DUMP_URL,
                    "ActuatorEndpoint.authActuator.heapDump.label", "ActuatorEndpoint.authActuator.heapDump.description")),
    ACTUATOR_ENV("/actuator/env/**",
            new AuthorizationActionValue(AUTZ_ACTUATOR_ENV_URL,
                    "ActuatorEndpoint.authActuator.env.label", "ActuatorEndpoint.authActuator.env.description")),
    ACTUATOR_INFO("/actuator/info",
            new AuthorizationActionValue(AUTZ_ACTUATOR_INFO_URL,
                    "ActuatorEndpoint.authActuator.info.label", "ActuatorEndpoint.authActuator.info.description")),
    ACTUATOR_METRICS("/actuator/metrics/**",
            new AuthorizationActionValue(AUTZ_ACTUATOR_METRICS_URL,
                    "ActuatorEndpoint.authActuator.metrics.label", "ActuatorEndpoint.authActuator.metrics.description")),
    /**
     * This is the authorization that provides access to all the methods. However, it is possible to authorize selected
     * REST actions individually; see {@link RestAuthorizationAction} enum.
     */
    REST("/ws/**",
            new AuthorizationActionValue(AUTZ_REST_ALL_URL,
                    "RestEndpoint.authRest.all.label", "RestEndpoint.authRest.all.description")),
    REST2("/rest/**",
            new AuthorizationActionValue(AUTZ_REST_ALL_URL,
                    "RestEndpoint.authRest.all.label", "RestEndpoint.authRest.all.description")),
    REST3("/api/**",
            new AuthorizationActionValue(AUTZ_REST_ALL_URL,
                    "RestEndpoint.authRest.all.label", "RestEndpoint.authRest.all.description")),

    INSPECTOR("/inspector/**",
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "WicketDebugInfo.authUri.guiAll.label", "WicketDebugInfo.authUri.guiAll.description")),
    LIVE_SESSION("/liveSession/**",
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "WicketDebugInfo.authUri.guiAll.label", "WicketDebugInfo.authUri.guiAll.description")),
    PAGE_STORE("/pageStore/**",
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "WicketDebugInfo.authUri.guiAll.label", "WicketDebugInfo.authUri.guiAll.description")),
    WICKET_PAGE("/wicket/**",
            new AuthorizationActionValue(AUTZ_GUI_ALL_URL,
                    "WicketDebugInfo.authUri.guiAll.label", "WicketDebugInfo.authUri.guiAll.description"));

    private final String url;

    // final, but array is still mutable
    private final AuthorizationActionValue[] action;

    EndPointsUrlMapping(String url, AuthorizationActionValue... action) {
        this.url = url;
        this.action = action;
    }

    public AuthorizationActionValue[] getAction() {
        return action;
    }

    public String getUrl() {
        return url;
    }

    public static EndPointsUrlMapping findEndPointsUrlMappingByUrl(String url) {
        return Arrays.stream(values())
                .filter(e -> e.getUrl().equals(url))
                .findFirst()
                .orElse(null);
    }
}
