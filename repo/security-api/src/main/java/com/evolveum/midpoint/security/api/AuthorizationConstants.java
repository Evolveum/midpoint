/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public class AuthorizationConstants {

    public static final String NS_SECURITY_PREFIX = SchemaConstants.NS_MIDPOINT_PUBLIC_PREFIX + "security/";
    public static final String NS_AUTHORIZATION = NS_SECURITY_PREFIX + "authorization-3";
    public static final String NS_AUTHORIZATION_UI = NS_SECURITY_PREFIX + "authorization-ui-3";
    public static final String NS_AUTHORIZATION_WS = NS_SECURITY_PREFIX + "authorization-ws-3";
    public static final String NS_AUTHORIZATION_REST = NS_SECURITY_PREFIX + "authorization-rest-3";
    public static final String NS_AUTHORIZATION_BULK = NS_SECURITY_PREFIX + "authorization-bulk-3";
    public static final String NS_AUTHORIZATION_MODEL = NS_SECURITY_PREFIX + "authorization-model-3";
    public static final String NS_AUTHORIZATION_ACTUATOR = NS_SECURITY_PREFIX + "authorization-actuator-3";

    public static final QName AUTZ_ALL_QNAME = new QName(NS_AUTHORIZATION, "all");
    public static final String AUTZ_ALL_URL = QNameUtil.qNameToUri(AUTZ_ALL_QNAME);

    /**
     * Authorization to access all REST operations (web resources).
     * This does NOT grant proxy authorization. It just gives access to all the
     * REST API operations. It does not automatically allow access to the data.
     * Additional data-level authorizations must be in place for most REST operations
     * to be executed.
     */
    public static final QName AUTZ_REST_ALL_QNAME = new QName(NS_AUTHORIZATION_REST, "all");
    public static final String AUTZ_REST_ALL_URL = QNameUtil.qNameToUri(AUTZ_REST_ALL_QNAME);

    public static final QName AUTZ_BULK_ALL_QNAME = new QName(NS_AUTHORIZATION_BULK, "all");
    public static final String AUTZ_BULK_ALL_URL = QNameUtil.qNameToUri(AUTZ_BULK_ALL_QNAME);

    /**
     * Authorization for a proxy user. The proxy user may impersonate other users. Special HTTP
     * header may be used to switch the identity without additional authentication.
     */
    public static final QName AUTZ_REST_PROXY_QNAME = new QName(NS_AUTHORIZATION_REST, "proxy");
    public static final String AUTZ_REST_PROXY_URL = QNameUtil.qNameToUri(AUTZ_REST_PROXY_QNAME);

    public static final QName AUTZ_WS_ALL_QNAME = new QName(NS_AUTHORIZATION_WS, "all");
    public static final String AUTZ_WS_ALL_URL = QNameUtil.qNameToUri(AUTZ_WS_ALL_QNAME);

    //    public static final QName AUTZ_DEVEL_QNAME = new QName(NS_AUTHORIZATION, "devel");
    public static final String AUTZ_NO_ACCESS_URL = NS_AUTHORIZATION + "#noAccess";
//    public static final String AUTZ_DEVEL_URL = QNameUtil.qNameToUri(AUTZ_DEVEL_QNAME);

    public static final QName AUTZ_DENY_ALL_QNAME = new QName(NS_AUTHORIZATION, "denyAll");
    public static final String AUTZ_DENY_ALL_URL = QNameUtil.qNameToUri(AUTZ_DENY_ALL_QNAME);
    public static final String AUTZ_DENY_ALL = NS_AUTHORIZATION + "#denyAll";

    public static final QName AUTZ_GUI_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "all");
    public static final String AUTZ_GUI_ALL_URL = QNameUtil.qNameToUri(AUTZ_GUI_ALL_QNAME);
    public static final String AUTZ_GUI_ALL_LABEL = "Authorization.constants.guiAll.label";
    public static final String AUTZ_GUI_ALL_DESCRIPTION = "Authorization.constants.guiAll.description";

//    @Deprecated
//    public static final QName AUTZ_GUI_ALL_DEPRECATED_QNAME = new QName(NS_AUTHORIZATION, "guiAll");
//    @Deprecated
//    public static final String AUTZ_GUI_ALL_DEPRECATED_URL = QNameUtil.qNameToUri(AUTZ_GUI_ALL_DEPRECATED_QNAME);

    // Following constants are ugly ... but they have to be.
    // Expressions such as:
    // public static final String AUTZ_UI_TASKS_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_TASKS_ALL_QNAME);
    // are not constant enough for use in annotations (e.g. in GUI pages)

    // simulation ui
    public static final QName AUTZ_UI_SIMULATIONS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "simulationsAll");
    public static final String AUTZ_UI_SIMULATIONS_ALL_URL = NS_AUTHORIZATION_UI + "#simulationsAll";
    public static final QName AUTZ_UI_SIMULATION_RESULTS_QNAME = new QName(NS_AUTHORIZATION_UI, "simulationResults");
    public static final String AUTZ_UI_SIMULATION_RESULTS_URL = NS_AUTHORIZATION_UI + "#simulationResults";
    public static final QName AUTZ_UI_SIMULATION_RESULT_QNAME = new QName(NS_AUTHORIZATION_UI, "simulationResult");
    public static final String AUTZ_UI_SIMULATION_RESULT_URL = NS_AUTHORIZATION_UI + "#simulationResult";
    public static final QName AUTZ_UI_SIMULATION_PROCESSED_OBJECTS_QNAME = new QName(NS_AUTHORIZATION_UI, "simulationProcessedObjects");
    public static final String AUTZ_UI_SIMULATION_PROCESSED_OBJECTS_URL = NS_AUTHORIZATION_UI + "#simulationProcessedObjects";
    public static final QName AUTZ_UI_SIMULATION_PROCESSED_OBJECT_QNAME = new QName(NS_AUTHORIZATION_UI, "simulationProcessedObject");
    public static final String AUTZ_UI_SIMULATION_PROCESSED_OBJECT_URL = NS_AUTHORIZATION_UI + "#simulationProcessedObject";

    // marks
    public static final QName AUTZ_UI_MARKS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "marksAll");
    public static final String AUTZ_UI_MARKS_ALL_URL = NS_AUTHORIZATION_UI + "#marksAll";
    public static final QName AUTZ_UI_MARKS_QNAME = new QName(NS_AUTHORIZATION_UI, "marks");
    public static final String AUTZ_UI_MARKS_URL = NS_AUTHORIZATION_UI + "#marks";
    public static final QName AUTZ_UI_MARK_QNAME = new QName(NS_AUTHORIZATION_UI, "mark");
    public static final String AUTZ_UI_MARK_URL = NS_AUTHORIZATION_UI + "#mark";

    //user
    public static final QName AUTZ_UI_USERS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "usersAll");
    public static final String AUTZ_UI_USERS_ALL_URL = NS_AUTHORIZATION_UI + "#usersAll";

    public static final QName AUTZ_UI_USERS_QNAME = new QName(NS_AUTHORIZATION_UI, "users");
    public static final String AUTZ_UI_USERS_URL = NS_AUTHORIZATION_UI + "#users";

    public static final QName AUTZ_UI_ROLE_ANALYSIS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "roleAnalysis");
    public static final String AUTZ_UI_ROLE_ANALYSIS_ALL_URL = NS_AUTHORIZATION_UI + "#roleAnalysis";

    public static final QName AUTZ_UI_ROLE_ANALYSIS_CLUSTER_QNAME = new QName(NS_AUTHORIZATION_UI, "roleAnalysisCluster");
    public static final String AUTZ_UI_ROLE_ANALYSIS_CLUSTER_URL = NS_AUTHORIZATION_UI + "#roleAnalysisCluster";

    public static final QName AUTZ_UI_ROLE_ANALYSIS_SESSION_QNAME = new QName(NS_AUTHORIZATION_UI, "roleAnalysisSession");
    public static final String AUTZ_UI_ROLE_ANALYSIS_SESSION_URL = NS_AUTHORIZATION_UI + "#roleAnalysisSession";

    public static final QName AUTZ_UI_USERS_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "usersView");
    public static final String AUTZ_UI_USERS_VIEW_URL = NS_AUTHORIZATION_UI + "#usersView";

    public static final QName AUTZ_UI_FIND_USERS_QNAME = new QName(NS_AUTHORIZATION_UI, "findUsers");
    public static final String AUTZ_UI_FIND_USERS_URL = NS_AUTHORIZATION_UI + "#findUsers";

    public static final QName AUTZ_UI_USER_QNAME = new QName(NS_AUTHORIZATION_UI, "user");
    public static final String AUTZ_UI_USER_URL = NS_AUTHORIZATION_UI + "#user";

    public static final QName AUTZ_UI_USER_HISTORY_QNAME = new QName(NS_AUTHORIZATION_UI, "userHistory");
    public static final String AUTZ_UI_USER_HISTORY_URL = NS_AUTHORIZATION_UI + "#userHistory";

    public static final QName AUTZ_UI_ORG_UNIT_HISTORY_QNAME = new QName(NS_AUTHORIZATION_UI, "orgUnitHistory");
    public static final String AUTZ_UI_ORG_UNIT_HISTORY_URL = NS_AUTHORIZATION_UI + "#orgUnitHistory";

    public static final QName AUTZ_UI_ROLE_HISTORY_QNAME = new QName(NS_AUTHORIZATION_UI, "roleHistory");
    public static final String AUTZ_UI_ROLE_HISTORY_URL = NS_AUTHORIZATION_UI + "#roleHistory";

    public static final QName AUTZ_UI_SERVICE_HISTORY_QNAME = new QName(NS_AUTHORIZATION_UI, "serviceHistory");
    public static final String AUTZ_UI_SERVICE_HISTORY_URL = NS_AUTHORIZATION_UI + "#serviceHistory";

    public static final QName AUTZ_UI_POLICY_HISTORY_QNAME = new QName(NS_AUTHORIZATION_UI, "policyHistory");
    public static final String AUTZ_UI_POLICY_HISTORY_URL = NS_AUTHORIZATION_UI + "#policyHistory";

    public static final QName AUTZ_UI_USER_HISTORY_XML_REVIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "userHistoryXmlReview");
    public static final String AUTZ_UI_USER_HISTORY_XML_REVIEW_URL = NS_AUTHORIZATION_UI + "#userHistoryXmlReview";

    public static final QName AUTZ_UI_USER_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "userDetails");
    public static final String AUTZ_UI_USER_DETAILS_URL = NS_AUTHORIZATION_UI + "#userDetails";

    public static final QName AUTZ_UI_MERGE_OBJECTS_QNAME = new QName(NS_AUTHORIZATION_UI, "mergeObjects");
    public static final String AUTZ_UI_MERGE_OBJECTS_URL = NS_AUTHORIZATION_UI + "#mergeObjects";

    public static final QName AUTZ_UI_SERVICES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "servicesAll");
    public static final String AUTZ_UI_SERVICES_ALL_URL = NS_AUTHORIZATION_UI + "#servicesAll";

    public static final QName AUTZ_UI_SERVICES_QNAME = new QName(NS_AUTHORIZATION_UI, "services");
    public static final String AUTZ_UI_SERVICES_URL = NS_AUTHORIZATION_UI + "#services";

    public static final QName AUTZ_UI_SERVICE_QNAME = new QName(NS_AUTHORIZATION_UI, "service");
    public static final String AUTZ_UI_SERVICE_URL = NS_AUTHORIZATION_UI + "#service";

    public static final QName AUTZ_UI_SERVICE_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "serviceDetails");
    public static final String AUTZ_UI_SERVICE_DETAILS_URL = NS_AUTHORIZATION_UI + "#serviceDetails";

    public static final QName AUTZ_UI_POLICIES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "policiesAll");
    public static final String AUTZ_UI_POLICIES_ALL_URL = NS_AUTHORIZATION_UI + "#policiesAll";

    public static final QName AUTZ_UI_POLICIES_QNAME = new QName(NS_AUTHORIZATION_UI, "policies");
    public static final String AUTZ_UI_POLICIES_URL = NS_AUTHORIZATION_UI + "#policies";

    public static final QName AUTZ_UI_POLICY_QNAME = new QName(NS_AUTHORIZATION_UI, "policy");
    public static final String AUTZ_UI_POLICY_URL = NS_AUTHORIZATION_UI + "#policy";

    public static final QName AUTZ_UI_POLICY_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "policyDetails");
    public static final String AUTZ_UI_POLICY_DETAILS_URL = NS_AUTHORIZATION_UI + "#policyDetails";

    public static final QName AUTZ_UI_ARCHETYPES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "archetypesAll");
    public static final String AUTZ_UI_ARCHETYPES_ALL_URL = NS_AUTHORIZATION_UI + "#archetypesAll";

    public static final QName AUTZ_UI_MESSAGE_TEMPLATES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "messageTemplatesAll");
    public static final String AUTZ_UI_MESSAGE_TEMPLATES_ALL_URL = NS_AUTHORIZATION_UI + "#messageTemplatesAll";

    public static final QName AUTZ_UI_ARCHETYPES_QNAME = new QName(NS_AUTHORIZATION_UI, "archetypes");
    public static final String AUTZ_UI_ARCHETYPES_URL = NS_AUTHORIZATION_UI + "#archetypes";

    public static final QName AUTZ_UI_MESSAGE_TEMPLATES_QNAME = new QName(NS_AUTHORIZATION_UI, "messageTemplates");
    public static final String AUTZ_UI_MESSAGE_TEMPLATES_URL = NS_AUTHORIZATION_UI + "#messageTemplates";

    public static final QName AUTZ_UI_ARCHETYPE_QNAME = new QName(NS_AUTHORIZATION_UI, "archetype");
    public static final String AUTZ_UI_ARCHETYPE_URL = NS_AUTHORIZATION_UI + "#archetype";

    //application
    public static final QName AUTZ_UI_APPLICATIONS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "applicationsAll");
    public static final String AUTZ_UI_APPLICATIONS_ALL_URL = NS_AUTHORIZATION_UI + "#applicationsAll";

    public static final QName AUTZ_UI_APPLICATIONS_QNAME = new QName(NS_AUTHORIZATION_UI, "applications");
    public static final String AUTZ_UI_APPLICATIONS_URL = NS_AUTHORIZATION_UI + "#applications";

    // Application XML editor
    public static final QName AUTZ_UI_APPLICATION_QNAME = new QName(NS_AUTHORIZATION_UI, "application");
    public static final String AUTZ_UI_APPLICATION_URL = NS_AUTHORIZATION_UI + "#application";

    public static final QName AUTZ_UI_APPLICATION_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "applicationDetails");
    public static final String AUTZ_UI_APPLICATION_DETAILS_URL = NS_AUTHORIZATION_UI + "#applicationDetails";

    // Also applies to application wizard
    public static final QName AUTZ_UI_APPLICATION_EDIT_QNAME = new QName(NS_AUTHORIZATION_UI, "applicationEdit");
    public static final String AUTZ_UI_APPLICATION_EDIT_URL = NS_AUTHORIZATION_UI + "#applicationEdit";

    public static final QName AUTZ_UI_MESSAGE_TEMPLATE_QNAME = new QName(NS_AUTHORIZATION_UI, "messageTemplate");
    public static final String AUTZ_UI_MESSAGE_TEMPLATE_URL = NS_AUTHORIZATION_UI + "#messageTemplate";

    public static final QName AUTZ_UI_OBJECT_COLLECTIONS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "objectCollectionsAll");
    public static final String AUTZ_UI_OBJECT_COLLECTIONS_ALL_URL = NS_AUTHORIZATION_UI + "#objectCollectionsAll";

    public static final QName AUTZ_UI_OBJECT_COLLECTIONS_QNAME = new QName(NS_AUTHORIZATION_UI, "objectCollections");
    public static final String AUTZ_UI_OBJECT_COLLECTIONS_URL = NS_AUTHORIZATION_UI + "#objectCollections";

    public static final QName AUTZ_UI_OBJECT_COLLECTION_QNAME = new QName(NS_AUTHORIZATION_UI, "objectCollection");
    public static final String AUTZ_UI_OBJECT_COLLECTION_URL = NS_AUTHORIZATION_UI + "#objectCollection";

    public static final QName AUTZ_UI_OBJECT_TEMPLATES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "objectTemplatesAll");
    public static final String AUTZ_UI_OBJECT_TEMPLATES_ALL_URL = NS_AUTHORIZATION_UI + "#objectTemplatesAll";

    public static final QName AUTZ_UI_OBJECT_TEMPLATES_QNAME = new QName(NS_AUTHORIZATION_UI, "objectTemplates");
    public static final String AUTZ_UI_OBJECT_TEMPLATES_URL = NS_AUTHORIZATION_UI + "#objectTemplates";

    public static final QName AUTZ_UI_OBJECT_TEMPLATE_QNAME = new QName(NS_AUTHORIZATION_UI, "objectTemplate");
    public static final String AUTZ_UI_OBJECT_TEMPLATE_URL = NS_AUTHORIZATION_UI + "#objectTemplate";

    public static final QName AUTZ_UI_VALUE_POLICIES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "valuePoliciesAll");
    public static final String AUTZ_UI_VALUE_POLICIES_ALL_URL = NS_AUTHORIZATION_UI + "#valuePoliciesAll";

    public static final QName AUTZ_UI_VALUE_POLICIES_QNAME = new QName(NS_AUTHORIZATION_UI, "valuePolicies");
    public static final String AUTZ_UI_VALUE_POLICIES_URL = NS_AUTHORIZATION_UI + "#valuePolicies";

    public static final QName AUTZ_UI_VALUE_POLICY_QNAME = new QName(NS_AUTHORIZATION_UI, "valuePolicy");
    public static final String AUTZ_UI_VALUE_POLICY_URL = NS_AUTHORIZATION_UI + "#valuePolicy";

    //resources
    public static final QName AUTZ_UI_RESOURCES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "resourcesAll");
    public static final String AUTZ_UI_RESOURCES_ALL_URL = NS_AUTHORIZATION_UI + "#resourcesAll";

    public static final QName AUTZ_UI_RESOURCES_QNAME = new QName(NS_AUTHORIZATION_UI, "resources");
    public static final String AUTZ_UI_RESOURCES_URL = NS_AUTHORIZATION_UI + "#resources";

    public static final QName AUTZ_UI_CONNECTOR_HOSTS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "connectorHostsAll");
    public static final String AUTZ_UI_CONNECTOR_HOSTS_ALL_URL = NS_AUTHORIZATION_UI + "#connectorHostsAll";

    // Resource XML editor
    public static final QName AUTZ_UI_RESOURCE_QNAME = new QName(NS_AUTHORIZATION_UI, "resource");
    public static final String AUTZ_UI_RESOURCE_URL = NS_AUTHORIZATION_UI + "#resource";

    public static final QName AUTZ_UI_RESOURCE_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "resourceDetails");
    public static final String AUTZ_UI_RESOURCE_DETAILS_URL = NS_AUTHORIZATION_UI + "#resourceDetails";

    // Also applies to resource wizard
    public static final QName AUTZ_UI_RESOURCE_EDIT_QNAME = new QName(NS_AUTHORIZATION_UI, "resourceEdit");
    public static final String AUTZ_UI_RESOURCE_EDIT_URL = NS_AUTHORIZATION_UI + "#resourceEdit";

    public static final QName AUTZ_UI_RESOURCES_ACCOUNT_QNAME = new QName(NS_AUTHORIZATION_UI, "resourcesAccount");
    public static final String AUTZ_UI_RESOURCES_ACCOUNT_URL = NS_AUTHORIZATION_UI + "#resourcesAccount";

    public static final QName AUTZ_UI_RESOURCES_CONTENT_ACCOUNTS_QNAME = new QName(NS_AUTHORIZATION_UI, "resourcesContentAccount");
    public static final String AUTZ_UI_RESOURCES_CONTENT_ACCOUNTS_URL = NS_AUTHORIZATION_UI + "#resourcesContentAccount";

    public static final QName AUTZ_UI_RESOURCES_CONTENT_ENTITLEMENTS_QNAME = new QName(NS_AUTHORIZATION_UI, "resourcesContentEntitlements");
    public static final String AUTZ_UI_RESOURCES_CONTENT_ENTITLEMENTS_URL = NS_AUTHORIZATION_UI + "#resourcesContentEntitlements";

    //Configuration
    public static final QName AUTZ_UI_CONFIGURATION_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "configurationAll");
    public static final String AUTZ_UI_CONFIGURATION_ALL_URL = NS_AUTHORIZATION_UI + "#configurationAll";

    public static final QName AUTZ_UI_CONFIGURATION_ABOUT_QNAME = new QName(NS_AUTHORIZATION_UI, "configAbout");
    public static final String AUTZ_UI_CONFIGURATION_ABOUT_URL = NS_AUTHORIZATION_UI + "#configAbout";

    public static final QName AUTZ_UI_CONFIGURATION_QNAME = new QName(NS_AUTHORIZATION_UI, "configuration");
    public static final String AUTZ_UI_CONFIGURATION_URL = NS_AUTHORIZATION_UI + "#configuration";

    public static final QName AUTZ_UI_CONFIGURATION_DEBUG_QNAME = new QName(NS_AUTHORIZATION_UI, "debug");
    public static final String AUTZ_UI_CONFIGURATION_DEBUG_URL = NS_AUTHORIZATION_UI + "#debug";

    public static final QName AUTZ_UI_CONFIGURATION_DEBUGS_QNAME = new QName(NS_AUTHORIZATION_UI, "debugs");
    public static final String AUTZ_UI_CONFIGURATION_DEBUGS_URL = NS_AUTHORIZATION_UI + "#debugs";

    public static final QName AUTZ_UI_CONFIGURATION_IMPORT_QNAME = new QName(NS_AUTHORIZATION_UI, "configImport");
    public static final String AUTZ_UI_CONFIGURATION_IMPORT_URL = NS_AUTHORIZATION_UI + "#configImport";

    public static final QName AUTZ_UI_CONFIGURATION_LOGGING_QNAME = new QName(NS_AUTHORIZATION_UI, "configLogging");
    public static final String AUTZ_UI_CONFIGURATION_LOGGING_URL = NS_AUTHORIZATION_UI + "#configLogging";

    public static final QName AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_QNAME = new QName(NS_AUTHORIZATION_UI, "configSystemConfiguration");
    public static final String AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL = NS_AUTHORIZATION_UI + "#configSystemConfiguration";

    public static final QName AUTZ_UI_CONFIGURATION_INTERNALS_QNAME = new QName(NS_AUTHORIZATION_UI, "configInternals");
    public static final String AUTZ_UI_CONFIGURATION_INTERNALS_URL = NS_AUTHORIZATION_UI + "#configInternals";

    public static final QName AUTZ_UI_CONFIGURATION_REPOSITORY_QUERY = new QName(NS_AUTHORIZATION_UI, "configRepositoryQuery");
    public static final String AUTZ_UI_CONFIGURATION_REPOSITORY_QUERY_URL = NS_AUTHORIZATION_UI + "#configRepositoryQuery";

    public static final QName AUTZ_UI_CONFIGURATION_EVALUATE_MAPPING = new QName(NS_AUTHORIZATION_UI, "configEvaluateMapping");
    public static final String AUTZ_UI_CONFIGURATION_EVALUATE_MAPPING_URL = NS_AUTHORIZATION_UI + "#configEvaluateMapping";

    public static final String AUTZ_UI_CONFIGURATION_AUTHORIZATION_PLAYGROUND_URL = NS_AUTHORIZATION_UI + "#configAuthorizationPlayground";

    public static final QName AUTZ_UI_TRACE_VIEW = new QName(NS_AUTHORIZATION_UI, "traceView");
    public static final String AUTZ_UI_TRACE_VIEW_URL = NS_AUTHORIZATION_UI + "#traceView";

    //Roles
    public static final QName AUTZ_UI_ROLES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "rolesAll");
    public static final String AUTZ_UI_ROLES_ALL_URL = NS_AUTHORIZATION_UI + "#rolesAll";

    public static final QName AUTZ_UI_ROLES_QNAME = new QName(NS_AUTHORIZATION_UI, "roles");
    public static final String AUTZ_UI_ROLES_URL = NS_AUTHORIZATION_UI + "#roles";

    public static final QName AUTZ_UI_ROLE_QNAME = new QName(NS_AUTHORIZATION_UI, "role");
    public static final String AUTZ_UI_ROLE_URL = NS_AUTHORIZATION_UI + "#role";

    public static final QName AUTZ_UI_ROLE_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "roleDetails");
    public static final String AUTZ_UI_ROLE_DETAILS_URL = NS_AUTHORIZATION_UI + "#roleDetails";

    //Orgs
    public static final QName AUTZ_UI_ORG_STRUCT_QNAME = new QName(NS_AUTHORIZATION_UI, "orgStruct");
    public static final String AUTZ_UI_ORG_STRUCT_URL = NS_AUTHORIZATION_UI + "#orgStruct";

    public static final QName AUTZ_UI_ORG_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "orgAll");
    public static final String AUTZ_UI_ORG_ALL_URL = NS_AUTHORIZATION_UI + "#orgAll";

    public static final QName AUTZ_UI_ORGS_QNAME = new QName(NS_AUTHORIZATION_UI, "orgs");
    public static final String AUTZ_UI_ORGS_URL = NS_AUTHORIZATION_UI + "#orgs";

    public static final QName AUTZ_UI_ORG_TREE_QNAME = new QName(NS_AUTHORIZATION_UI, "orgTree");
    public static final String AUTZ_UI_ORG_TREE_URL = NS_AUTHORIZATION_UI + "#orgTree";

    public static final QName AUTZ_UI_ORG_UNIT_QNAME = new QName(NS_AUTHORIZATION_UI, "orgUnit");
    public static final String AUTZ_UI_ORG_UNIT_URL = NS_AUTHORIZATION_UI + "#orgUnit";

    public static final QName AUTZ_UI_ORG_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "orgDetails");
    public static final String AUTZ_UI_ORG_DETAILS_URL = NS_AUTHORIZATION_UI + "#orgDetails";

    //Approvals (workflows)
    public static final String AUTZ_UI_APPROVALS_ALL_URL = NS_AUTHORIZATION_UI + "#approvalsAll";
    public static final String AUTZ_UI_MY_WORK_ITEMS_URL = NS_AUTHORIZATION_UI + "#myWorkItems";
    public static final String AUTZ_UI_ATTORNEY_WORK_ITEMS_URL = NS_AUTHORIZATION_UI + "#attorneyWorkItems";
    public static final String AUTZ_UI_CLAIMABLE_WORK_ITEMS_URL = NS_AUTHORIZATION_UI + "#claimableWorkItems";
    public static final String AUTZ_UI_ALL_WORK_ITEMS_URL = NS_AUTHORIZATION_UI + "#allWorkItems";
    public static final String AUTZ_UI_WORK_ITEM_URL = NS_AUTHORIZATION_UI + "#workItem";
    public static final String AUTZ_UI_WORK_ITEMS_ALL_REQUESTS_URL = NS_AUTHORIZATION_UI + "#allRequests";
    public static final String AUTZ_UI_MY_REQUESTS_URL = NS_AUTHORIZATION_UI + "#myRequests";
    public static final String AUTZ_UI_REQUESTS_ABOUT_ME_URL = NS_AUTHORIZATION_UI + "#requestsAboutMe";

    //Tasks
    public static final QName AUTZ_UI_TASKS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "tasksAll");
    public static final String AUTZ_UI_TASKS_ALL_URL = NS_AUTHORIZATION_UI + "#tasksAll";

    public static final QName AUTZ_UI_TASKS_QNAME = new QName(NS_AUTHORIZATION_UI, "tasks");
    public static final String AUTZ_UI_TASKS_URL = NS_AUTHORIZATION_UI + "#tasks";

    public static final QName AUTZ_UI_TASK_QNAME = new QName(NS_AUTHORIZATION_UI, "task");
    public static final String AUTZ_UI_TASK_URL = NS_AUTHORIZATION_UI + "#task";

    public static final QName AUTZ_UI_TASK_DETAIL_QNAME = new QName(NS_AUTHORIZATION_UI, "taskDetails");
    public static final String AUTZ_UI_TASK_DETAIL_URL = NS_AUTHORIZATION_UI + "#taskDetails";

    public static final QName AUTZ_UI_TASK_ADD_QNAME = new QName(NS_AUTHORIZATION_UI, "taskAdd");
    public static final String AUTZ_UI_TASK_ADD_URL = NS_AUTHORIZATION_UI + "#taskAdd";

    public static final QName AUTZ_UI_NODES_QNAME = new QName(NS_AUTHORIZATION_UI, "nodes");
    public static final String AUTZ_UI_NODES_URL = NS_AUTHORIZATION_UI + "#nodes";

    //Reports
    public static final QName AUTZ_UI_REPORTS_QNAME = new QName(NS_AUTHORIZATION_UI, "reports");
    public static final String AUTZ_UI_REPORTS_URL = NS_AUTHORIZATION_UI + "#reports";

    public static final QName AUTZ_UI_REPORT_QNAME = new QName(NS_AUTHORIZATION_UI, "report");
    public static final String AUTZ_UI_REPORT_URL = NS_AUTHORIZATION_UI + "#report";

    public static final QName AUTZ_UI_REPORTS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "reportsAll");
    public static final String AUTZ_UI_REPORTS_ALL_URL = NS_AUTHORIZATION_UI + "#reportsAll";

    public static final QName AUTZ_UI_REPORTS_CREATED_REPORTS_QNAME = new QName(NS_AUTHORIZATION_UI, "createdReports");
    public static final String AUTZ_UI_REPORTS_CREATED_REPORTS_URL = NS_AUTHORIZATION_UI + "#createdReports";

    public static final QName AUTZ_UI_AUDIT_LOG_VIEWER_QNAME = new QName(NS_AUTHORIZATION_UI, "auditLogViewer");
    public static final String AUTZ_UI_AUDIT_LOG_VIEWER_URL = NS_AUTHORIZATION_UI + "#auditLogViewer";

    public static final QName AUTZ_UI_AUDIT_LOG_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "auditLogDetails");
    public static final String AUTZ_UI_AUDIT_LOG_DETAILS_URL = NS_AUTHORIZATION_UI + "#auditLogDetails";

    public static final QName AUTZ_UI_REPORTS_REPORT_CREATE_QNAME = new QName(NS_AUTHORIZATION_UI, "reportCreate");
    public static final String AUTZ_UI_REPORTS_REPORT_CREATE_URL = NS_AUTHORIZATION_UI + "#reportCreate";

    //Cases
    public static final QName AUTZ_UI_CASES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "casesAll");
    public static final String AUTZ_UI_CASES_ALL_URL = NS_AUTHORIZATION_UI + "#casesAll";

    public static final QName AUTZ_UI_CASES_QNAME = new QName(NS_AUTHORIZATION_UI, "cases");
    public static final String AUTZ_UI_CASES_URL = NS_AUTHORIZATION_UI + "#cases";

    public static final QName AUTZ_UI_CASE_QNAME = new QName(NS_AUTHORIZATION_UI, "case");
    public static final String AUTZ_UI_CASE_URL = NS_AUTHORIZATION_UI + "#case";

    public static final QName AUTZ_UI_CASE_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "caseDetails");
    public static final String AUTZ_UI_CASE_DETAILS_URL = NS_AUTHORIZATION_UI + "#caseDetails";

    public static final String AUTZ_UI_WORK_ITEMS_ALL_URL = NS_AUTHORIZATION_UI + "#workItemsAll";

    //Certification
    public static final String AUTZ_UI_CERTIFICATION_DEFINITIONS_URL = NS_AUTHORIZATION_UI + "#certificationDefinitions";
    public static final String AUTZ_UI_CERTIFICATION_DEFINITION_URL = NS_AUTHORIZATION_UI + "#certificationDefinition";
    public static final String AUTZ_UI_CERTIFICATION_NEW_DEFINITION_URL = NS_AUTHORIZATION_UI + "#certificationNewDefinition";
    public static final String AUTZ_UI_CERTIFICATION_CAMPAIGNS_URL = NS_AUTHORIZATION_UI + "#certificationCampaigns";
    public static final String AUTZ_UI_CERTIFICATION_CAMPAIGN_URL = NS_AUTHORIZATION_UI + "#certificationCampaign";
    public static final String AUTZ_UI_CERTIFICATION_DECISIONS_URL = NS_AUTHORIZATION_UI + "#certificationDecisions";
    public static final String AUTZ_UI_ACTIVE_CERT_CAMPAIGNS_URL = NS_AUTHORIZATION_UI + "#activeCertificationCampaigns";
    public static final String AUTZ_UI_MY_CERTIFICATION_DECISIONS_URL = NS_AUTHORIZATION_UI + "#myCertificationDecisions";
    public static final String AUTZ_UI_MY_ACTIVE_CERT_CAMPAIGNS_URL = NS_AUTHORIZATION_UI + "#myActiveCertificationCampaigns";

    public static final QName AUTZ_UI_CERTIFICATION_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "certificationAll");
    public static final String AUTZ_UI_CERTIFICATION_ALL_URL = NS_AUTHORIZATION_UI + "#certificationAll";

    //Home
    public static final QName AUTZ_UI_HOME_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "home");
    public static final String AUTZ_UI_HOME_ALL_URL = NS_AUTHORIZATION_UI + "#home";

    public static final QName AUTZ_UI_DASHBOARD_QNAME = new QName(NS_AUTHORIZATION_UI, "dashboard");
    public static final String AUTZ_UI_DASHBOARD_URL = NS_AUTHORIZATION_UI + "#dashboard";

    public static final QName AUTZ_UI_MY_PASSWORDS_QNAME = new QName(NS_AUTHORIZATION_UI, "myPasswords");
    public static final String AUTZ_UI_MY_PASSWORDS_URL = NS_AUTHORIZATION_UI + "#myPasswords";

    public static final QName AUTZ_UI_MY_QUESTIONS_QNAME = new QName(NS_AUTHORIZATION_UI, "myQuestions");
    public static final String AUTZ_UI_MY_QUESTIONS_URL = NS_AUTHORIZATION_UI + "#myQuestions";

    public static final QName AUTZ_UI_BULK_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "bulkAction");
    public static final String AUTZ_UI_BULK_ACTION_URL = NS_AUTHORIZATION_UI + "#bulkAction";

    public static final QName AUTZ_UI_CONTACTS_QNAME = new QName(NS_AUTHORIZATION_UI, "contacts");
    public static final String AUTZ_UI_CONTACTS_URL = NS_AUTHORIZATION_UI + "#contacts";

    //self
    public static final QName AUTZ_UI_SELF_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "selfAll");
    public static final String AUTZ_UI_SELF_ALL_URL = NS_AUTHORIZATION_UI + "#selfAll";

    public static final QName AUTZ_UI_SELF_ASSIGNMENTS_QNAME = new QName(NS_AUTHORIZATION_UI, "selfAssignments");
    public static final String AUTZ_UI_SELF_ASSIGNMENTS_URL = NS_AUTHORIZATION_UI + "#selfAssignments";

    public static final QName AUTZ_UI_SELF_ASSIGNMENTS_CONFLICTS_QNAME = new QName(NS_AUTHORIZATION_UI, "selfAssignmentsConflicts");
    public static final String AUTZ_UI_SELF_ASSIGNMENTS_CONFLICTS_URL = NS_AUTHORIZATION_UI + "#selfAssignmentsConflicts";

    public static final QName AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_QNAME = new QName(NS_AUTHORIZATION_UI, "selfRequestAssignments");
    public static final String AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL = NS_AUTHORIZATION_UI + "#selfRequestAssignments";

    public static final QName AUTZ_UI_SELF_CREDENTIALS_QNAME = new QName(NS_AUTHORIZATION_UI, "selfCredentials");
    public static final String AUTZ_UI_SELF_CREDENTIALS_URL = NS_AUTHORIZATION_UI + "#selfCredentials";
    public static final String AUTZ_UI_RESET_PASSWORD_URL = NS_AUTHORIZATION_UI + "#resetPassword";
    public static final String AUTZ_UI_IDENTITY_RECOVERY_URL = NS_AUTHORIZATION_UI + "#identityRecovery";

    public static final QName AUTZ_UI_SELF_CONSENTS_QNAME = new QName(NS_AUTHORIZATION_UI, "selfConsents");
    public static final String AUTZ_UI_SELF_CONSENTS_URL = NS_AUTHORIZATION_UI + "#selfConsents";

    public static final QName AUTZ_UI_SELF_PROFILE_QNAME = new QName(NS_AUTHORIZATION_UI, "selfProfile");
    public static final String AUTZ_UI_SELF_PROFILE_URL = NS_AUTHORIZATION_UI + "#selfProfile";

    public static final QName AUTZ_UI_SELF_ASSIGNMENT_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "assignmentDetails");
    public static final String AUTZ_UI_SELF_ASSIGNMENT_DETAILS_URL = NS_AUTHORIZATION_UI + "#assignmentDetails";

    public static final QName AUTZ_UI_SELF_DASHBOARD_QNAME = new QName(NS_AUTHORIZATION_UI, "selfDashboard");
    public static final String AUTZ_UI_SELF_DASHBOARD_URL = NS_AUTHORIZATION_UI + "#selfDashboard";

    public static final QName AUTZ_UI_SELF_POST_AUTHENTICATION_QNAME = new QName(NS_AUTHORIZATION_UI, "postAuthentication");
    public static final String AUTZ_UI_SELF_POST_AUTHENTICATION_URL = NS_AUTHORIZATION_UI + "#postAuthentication";

    public static final QName AUTZ_UI_SELF_REGISTRATION_FINISH_QNAME = new QName(NS_AUTHORIZATION_UI, "selfRegistFinish");
    public static final String AUTZ_UI_SELF_REGISTRATION_FINISH_URL = NS_AUTHORIZATION_UI + "#selfRegistFinish";
    public static final String AUTZ_UI_INVITATION_URL = NS_AUTHORIZATION_UI + "#invitation";

    public static final QName AUTZ_UI_PREVIEW_CHANGES_QNAME = new QName(NS_AUTHORIZATION_UI, "previewChanges");
    public static final String AUTZ_UI_PREVIEW_CHANGES_URL = NS_AUTHORIZATION_UI + "#previewChanges";

    //Schema
    public static final QName AUTZ_UI_SCHEMAS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "schemasAll");
    public static final String AUTZ_UI_SCHEMAS_ALL_URL = NS_AUTHORIZATION_UI + "#schemasAll";

    public static final QName AUTZ_UI_SCHEMAS_QNAME = new QName(NS_AUTHORIZATION_UI, "schemas");
    public static final String AUTZ_UI_SCHEMAS_URL = NS_AUTHORIZATION_UI + "#schemas";

    public static final QName AUTZ_UI_SCHEMA_QNAME = new QName(NS_AUTHORIZATION_UI, "schema");
    public static final String AUTZ_UI_SCHEMA_URL = NS_AUTHORIZATION_UI + "#schema";

    public static final QName AUTZ_UI_SCHEMA_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "schemaDetails");
    public static final String AUTZ_UI_SCHEMA_DETAILS_URL = NS_AUTHORIZATION_UI + "#schemaDetails";

    //About
//    public static final QName AUTZ_UI_ABOUT_MIDPOINT_QNAME = new QName(NS_AUTHORIZATION, "aboutMidpoint");
//    public static final String AUTZ_UI_ABOUT_MIDPOINT_URL = QNameUtil.qNameToUri(AUTZ_UI_ABOUT_MIDPOINT_QNAME);
//
//    public static final QName AUTZ_UI_ABOUT_SYSTEM_QNAME = new QName(NS_AUTHORIZATION, "abountSystem");
//    public static final String AUTZ_UI_ABOUT_SYSTEM_URL = QNameUtil.qNameToUri(AUTZ_UI_ABOUT_SYSTEM_QNAME);

    // Does not really belong here. But there is no better place now.
    public static final String ANONYMOUS_USER_PRINCIPAL = "anonymousUser";

    // Misc UI authorizations
    //
    // These are for controlling specific parts of the GUI, e.g. buttons or items in context menus.
    // These enable generic functionality that is usable both for ordinary users and administrators

    public static final QName AUTZ_UI_DELEGATE_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "delegate");
    public static final String AUTZ_UI_DELEGATE_ACTION_URL = QNameUtil.qNameToUri(AUTZ_UI_DELEGATE_ACTION_QNAME);

    // UI administrator authorizations
    //
    // Those authorizations enable advanced functionality that is supposed to be available to administrators only.
    // They enable functionality that is complex or might be dangerous and that is not supposed to be used by
    // ordinary users.

    // UI authorizations for admin (complex) assign/unassign controls
    public static final QName AUTZ_UI_ADMIN_ASSIGN_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminAssign");
    public static final String AUTZ_UI_ADMIN_ASSIGN_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ASSIGN_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_UNASSIGN_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminUnassign");
    public static final String AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_UNASSIGN_ACTION_QNAME);

    //VERY! VERY! EXPERIMENTAL CODE!
    //all ui authorizations for menu items are supposed to be reworked in the future
    // (to use adminGuiConfig instead for setting up visibility for menu items and other gui elements)
    //ui authorizations for menu items on the org members/managers panel
    //Members tab on the  AbstractRole edit page
    public static final QName AUTZ_UI_ADMIN_ASSIGN_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminAssignMember");
    public static final String AUTZ_UI_ADMIN_ASSIGN_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ASSIGN_MEMBER_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_ADD_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminAddMember");
    public static final String AUTZ_UI_ADMIN_ADD_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ADD_MEMBER_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_UNASSIGN_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminUnassignMember");
    public static final String AUTZ_UI_ADMIN_UNASSIGN_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_UNASSIGN_MEMBER_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_UNASSIGN_ALL_MEMBERS_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminUnassignAllMembers");
    public static final String AUTZ_UI_ADMIN_UNASSIGN_ALL_MEMBERS_TAB_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_UNASSIGN_ALL_MEMBERS_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminRecomputeMember");
    public static final String AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_DELETE_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminDeleteMember");
    public static final String AUTZ_UI_ADMIN_DELETE_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_DELETE_MEMBER_ACTION_QNAME);

    //ui authorizations for menu items on the Governance members tab of Role type object
    public static final QName AUTZ_UI_ADMIN_ASSIGN_GOVERNANCE_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminAssignGovernance");
    public static final String AUTZ_UI_ADMIN_ASSIGN_GOVERNANCE_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ASSIGN_GOVERNANCE_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_UNASSIGN_GOVERNANCE_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminUnassignGovernance");
    public static final String AUTZ_UI_ADMIN_UNASSIGN_GOVERNANCE_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_UNASSIGN_GOVERNANCE_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_RECOMPUTE_GOVERNANCE_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminRecomputeGovernance");
    public static final String AUTZ_UI_ADMIN_RECOMPUTE_GOVERNANCE_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_RECOMPUTE_GOVERNANCE_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_DELETE_GOVERNANCE_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminDeleteGovernance");
    public static final String AUTZ_UI_ADMIN_DELETE_GOVERNANCE_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_DELETE_GOVERNANCE_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_ADD_GOVERNANCE_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminAddGovernance");
    public static final String AUTZ_UI_ADMIN_ADD_GOVERNANCE_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ADD_GOVERNANCE_ACTION_QNAME);

    //ui authorizations for Org member panel (applied for both Managers && Members panels)
    public static final QName AUTZ_UI_ADMIN_ASSIGN_ORG_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminAssignOrgMember");
    public static final String AUTZ_UI_ADMIN_ASSIGN_ORG_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ASSIGN_ORG_MEMBER_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_UNASSIGN_ORG_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminUnassignOrgMember");
    public static final String AUTZ_UI_ADMIN_UNASSIGN_ORG_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_UNASSIGN_ORG_MEMBER_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_DELETE_ORG_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminDeleteOrgMember");
    public static final String AUTZ_UI_ADMIN_DELETE_ORG_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_DELETE_ORG_MEMBER_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_ADD_ORG_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminAddOrgMember");
    public static final String AUTZ_UI_ADMIN_ADD_ORG_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ADD_ORG_MEMBER_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_RECOMPUTE_ORG_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminRecomputeOrgMember");
    public static final String AUTZ_UI_ADMIN_RECOMPUTE_ORG_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_RECOMPUTE_ORG_MEMBER_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_ORG_MOVE_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminOrgMove");
    public static final String AUTZ_UI_ADMIN_ORG_MOVE_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ORG_MOVE_ACTION_QNAME);

    public static final QName AUTZ_UI_ADMIN_ORG_MAKE_ROOT_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminOrgMakeRoot");
    public static final String AUTZ_UI_ADMIN_ORG_MAKE_ROOT_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ORG_MAKE_ROOT_ACTION_QNAME);

    //Archetype member
    public static final QName AUTZ_UI_ADMIN_ASSIGN_ARCHETYPE_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminAssignArchetypeMember");
    public static final String AUTZ_UI_ADMIN_ASSIGN_ARCHETYPE_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ASSIGN_ARCHETYPE_MEMBER_ACTION_QNAME);

    //Archetype object list pages for different object types
    public static final QName AUTZ_UI_ROLES_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "rolesView");
    public static final String AUTZ_UI_ROLES_VIEW_URL = NS_AUTHORIZATION_UI + "#rolesView";

    public static final QName AUTZ_UI_SERVICES_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "servicesView");
    public static final String AUTZ_UI_SERVICES_VIEW_URL = NS_AUTHORIZATION_UI + "#servicesView";

    public static final QName AUTZ_UI_POLICIES_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "policiesView");
    public static final String AUTZ_UI_POLICIES_VIEW_URL = NS_AUTHORIZATION_UI + "#policiesView";

    public static final QName AUTZ_UI_APPLICATION_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "applicationsView");
    public static final String AUTZ_UI_APPLICATION_VIEW_URL = NS_AUTHORIZATION_UI + "#applicationsView";

    public static final QName AUTZ_UI_ORGS_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "orgsView");
    public static final String AUTZ_UI_ORGS_VIEW_URL = NS_AUTHORIZATION_UI + "#orgsView";

    public static final QName AUTZ_UI_ARCHETYPES_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "archetypesView");
    public static final String AUTZ_UI_ARCHETYPES_VIEW_URL = NS_AUTHORIZATION_UI + "#archetypesView";

    public static final QName AUTZ_UI_MESSAGE_TEMPLATES_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "messageTemplatesView");
    public static final String AUTZ_UI_MESSAGE_TEMPLATES_VIEW_URL = NS_AUTHORIZATION_UI + "#messageTemplatesView";

    public static final QName AUTZ_UI_CASES_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "casesView");
    public static final String AUTZ_UI_CASES_VIEW_URL = NS_AUTHORIZATION_UI + "#casesView";

    public static final QName AUTZ_UI_RESOURCES_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "resourcesView");
    public static final String AUTZ_UI_RESOURCES_VIEW_URL = NS_AUTHORIZATION_UI + "#resourcesView";

    public static final QName AUTZ_UI_TASKS_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "tasksView");
    public static final String AUTZ_UI_TASKS_VIEW_URL = NS_AUTHORIZATION_UI + "#tasksView";

    public static final QName AUTZ_UI_REPORTS_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "reportsView");
    public static final String AUTZ_UI_REPORTS_VIEW_URL = NS_AUTHORIZATION_UI + "#reportsView";

    //ui authorization for CSV export button (will be applied everywhere over mp)
    public static final QName AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminCSVexport");
    public static final String AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_QNAME);

    //ui authorization for create report button under object list table (will be applied everywhere over mp)
    public static final QName AUTZ_UI_ADMIN_CREATE_REPORT_BUTTON_QNAME = new QName(NS_AUTHORIZATION_UI, "adminCreateReportButton");
    public static final String AUTZ_UI_ADMIN_CREATE_REPORT_BUTTON_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_CREATE_REPORT_BUTTON_QNAME);

    //authorization for spring boot actuator endpoints
    public static final QName AUTZ_ACTUATOR_ALL_QNAME = new QName(NS_AUTHORIZATION_ACTUATOR, "all");
    public static final String AUTZ_ACTUATOR_ALL_URL = QNameUtil.qNameToUri(AUTZ_ACTUATOR_ALL_QNAME);

    public static final QName AUTZ_ACTUATOR_THREAD_DUMP_QNAME = new QName(NS_AUTHORIZATION_ACTUATOR, "threadDump");
    public static final String AUTZ_ACTUATOR_THREAD_DUMP_URL = QNameUtil.qNameToUri(AUTZ_ACTUATOR_THREAD_DUMP_QNAME);

    public static final QName AUTZ_ACTUATOR_HEAP_DUMP_QNAME = new QName(NS_AUTHORIZATION_ACTUATOR, "heapDump");
    public static final String AUTZ_ACTUATOR_HEAP_DUMP_URL = QNameUtil.qNameToUri(AUTZ_ACTUATOR_HEAP_DUMP_QNAME);

    public static final QName AUTZ_ACTUATOR_ENV_QNAME = new QName(NS_AUTHORIZATION_ACTUATOR, "env");
    public static final String AUTZ_ACTUATOR_ENV_URL = QNameUtil.qNameToUri(AUTZ_ACTUATOR_ENV_QNAME);

    public static final QName AUTZ_ACTUATOR_INFO_QNAME = new QName(NS_AUTHORIZATION_ACTUATOR, "info");
    public static final String AUTZ_ACTUATOR_INFO_URL = QNameUtil.qNameToUri(AUTZ_ACTUATOR_INFO_QNAME);

    public static final QName AUTZ_ACTUATOR_METRICS_QNAME = new QName(NS_AUTHORIZATION_ACTUATOR, "metrics");
    public static final String AUTZ_ACTUATOR_METRICS_URL = QNameUtil.qNameToUri(AUTZ_ACTUATOR_METRICS_QNAME);

    public static final QName AUTZ_UI_OUTLIERS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "outliersAll");
    public static final String AUTZ_UI_OUTLIERS_ALL_URL = NS_AUTHORIZATION_UI + "#outliersAll";

    public static final QName AUTZ_UI_OUTLIERS_QNAME = new QName(NS_AUTHORIZATION_UI, "outliers");
    public static final String AUTZ_UI_OUTLIERS_URL = NS_AUTHORIZATION_UI + "#outliers";

    public static final QName AUTZ_UI_OUTLIERS_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "outliersView");
    public static final String AUTZ_UI_OUTLIERS_VIEW_URL = NS_AUTHORIZATION_UI + "#outliersView";

    public static final QName AUTZ_UI_ROLE_SUGGESTION_QNAME = new QName(NS_AUTHORIZATION_UI, "roleSuggestion");
    public static final String AUTZ_UI_ROLE_SUGGESTION_URL = NS_AUTHORIZATION_UI + "#roleSuggestion";

    public static final QName AUTZ_UI_ROLE_SUGGESTIONS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "allRoleSuggestions");
    public static final String AUTZ_UI_ROLE_SUGGESTIONS_ALL_URL = NS_AUTHORIZATION_UI + "#allRoleSuggestions";

    public static final QName AUTZ_UI_ROLE_SUGGESTIONS_VIEW_QNAME = new QName(NS_AUTHORIZATION_UI, "roleSuggestionView");
    public static final String AUTZ_UI_ROLE_SUGGESTIONS_VIEW_URL = NS_AUTHORIZATION_UI + "#roleSuggestionView";

    /**
     * Those are the items that midPoint logic controls directly. They have exception from execution-phase
     * authorization enforcement. Their modification in execution phase is always allowed. If it was not
     * allowed then midPoint won't be able to function properly and it may even lead to security issues.
     *
     * Note: this applies only to execution phase. Those items are still controlled by regular authorizations
     * for request phase. Therefore these exceptions do NOT allow user to modify those items. Attempt to do so
     * must pass through request-phase authorization first. This exception only allows midPoint logic to modify
     * those properties without explicit authorizations.
     *
     * Motivation: Strictly speaking, there would be no need for these exceptions. The modification can be
     * allowed by regular authorizations. However, that would mean, that every practical authorization must
     * contain those items. That is error-prone, it is a maintenance burden and it is even an obstacle for
     * evolvability. E.g. if similar properties are added in future midPoint versions (which is likely) then
     * all existing authorizations much be updated. The cost of slightly increased perceived security is not
     * justified by those operational issues.
     */
    public static final PathSet EXECUTION_ITEMS_ALLOWED_BY_DEFAULT = PathSet.of(
            ItemPath.create(InfraItemName.METADATA),
            ItemPath.create(ObjectType.F_METADATA),
            ItemPath.create(ObjectType.F_PARENT_ORG_REF),
            ItemPath.create(ObjectType.F_TENANT_REF),
            ItemPath.create(ObjectType.F_TRIGGER),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
            ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_METADATA),
            ItemPath.create(FocusType.F_ASSIGNMENT, InfraItemName.METADATA),
            ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_POLICY_SITUATION),
            ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_METADATA),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, InfraItemName.METADATA),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_POLICY_SITUATION),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
            // TODO What's this? inducement/assignment/xxx ? There's no such path.
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_METADATA),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, InfraItemName.METADATA),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_POLICY_SITUATION),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
            ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA),
            ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, InfraItemName.METADATA),
            ItemPath.create(FocusType.F_DELEGATED_REF),
            ItemPath.create(FocusType.F_ITERATION),
            ItemPath.create(FocusType.F_ITERATION_TOKEN),
//            ItemPath.create(FocusType.F_LINK_REF), // in fact, linkRef may be omitted here. link/unlink is done after execution authorizations are applied
            ItemPath.create(FocusType.F_PERSONA_REF),
            ItemPath.create(FocusType.F_ROLE_INFLUENCE_REF),
            ItemPath.create(FocusType.F_ROLE_MEMBERSHIP_REF),
            ItemPath.create(FocusType.F_TRIGGERED_POLICY_RULE)
    );

    /**
     * Items that are not considered for authorization in case that the entire container is deleted. MidPoint will
     * ignore those items when deleting containers.
     *
     * Motivation: Those items are automatically created and maintained by midPoint. When a container is created then
     * such items are added. Now the trouble is how to delete such container. The user would need to have authorization
     * to modify those items as well to delete a container value. However, such authorizations would allow him to also
     * modify such values at will. We do not want that.
     * This is important for some use cases, e.g. delete of a role exclusion policy rule. We want user to add/delete
     * exclusion policy rules, but we do not want the user to manipulate the meta data.
     * (also similar evolvability reasoning as for {@link #EXECUTION_ITEMS_ALLOWED_BY_DEFAULT})
     */
    public static final PathSet OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE = PathSet.of(
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
            ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_METADATA),
            ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_POLICY_SITUATION),
            ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            ItemPath.create(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_METADATA),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_POLICY_SITUATION),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_METADATA),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_POLICY_SITUATION),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_TRIGGERED_POLICY_RULE)
    );

}
