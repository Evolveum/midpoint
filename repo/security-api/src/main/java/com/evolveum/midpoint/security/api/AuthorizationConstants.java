/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.security.api;

import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class AuthorizationConstants {

	public static final String NS_SECURITY_PREFIX = SchemaConstants.NS_MIDPOINT_PUBLIC_PREFIX + "security/";
	public static final String NS_AUTHORIZATION = NS_SECURITY_PREFIX + "authorization-3";
	public static final String NS_AUTHORIZATION_UI = NS_SECURITY_PREFIX + "authorization-ui-3";
	public static final String NS_AUTHORIZATION_WS = NS_SECURITY_PREFIX + "authorization-ws-3";
	public static final String NS_AUTHORIZATION_REST = NS_SECURITY_PREFIX + "authorization-rest-3";
	public static final String NS_AUTHORIZATION_MODEL = NS_SECURITY_PREFIX + "authorization-model-3";

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

	/**
	 * Authorization for a proxy user. The proxy user may impersonate other users. Special HTTP
	 * header may be used to switch the identity without additional authentication.
	 */
	public static final QName AUTZ_REST_PROXY_QNAME = new QName(NS_AUTHORIZATION_REST, "proxy");
	public static final String AUTZ_REST_PROXY_URL = QNameUtil.qNameToUri(AUTZ_REST_PROXY_QNAME);

	public static final QName AUTZ_WS_ALL_QNAME = new QName(NS_AUTHORIZATION_WS, "all");
	public static final String AUTZ_WS_ALL_URL = QNameUtil.qNameToUri(AUTZ_WS_ALL_QNAME);

//	public static final QName AUTZ_DEVEL_QNAME = new QName(NS_AUTHORIZATION, "devel");
	public static final String AUTZ_NO_ACCESS_URL = NS_AUTHORIZATION + "#noAccess";
//	public static final String AUTZ_DEVEL_URL = QNameUtil.qNameToUri(AUTZ_DEVEL_QNAME);

	public static final QName AUTZ_DENY_ALL_QNAME = new QName(NS_AUTHORIZATION, "denyAll");
	public static final String AUTZ_DENY_ALL_URL = QNameUtil.qNameToUri(AUTZ_DENY_ALL_QNAME);
    public static final String AUTZ_DENY_ALL = NS_AUTHORIZATION + "#denyAll";

    public static final QName AUTZ_GUI_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "all");
    public static final String AUTZ_GUI_ALL_URL = QNameUtil.qNameToUri(AUTZ_GUI_ALL_QNAME);
    public static final String AUTZ_GUI_ALL_LABEL = "Authorization.constants.guiAll.label";
    public static final String AUTZ_GUI_ALL_DESCRIPTION = "Authorization.constants.guiAll.description";

	@Deprecated
    public static final QName AUTZ_GUI_ALL_DEPRECATED_QNAME = new QName(NS_AUTHORIZATION, "guiAll");
	@Deprecated
    public static final String AUTZ_GUI_ALL_DEPRECATED_URL = QNameUtil.qNameToUri(AUTZ_GUI_ALL_DEPRECATED_QNAME);


    // Following constants are ugly ... but they have to be.
    // Expressions such as:
    // public static final String AUTZ_UI_TASKS_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_TASKS_ALL_QNAME);
    // are not constant enough for use in annotations (e.g. in GUI pages)

    //user
	public static final QName AUTZ_UI_USERS_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "usersAll");
	public static final String AUTZ_UI_USERS_ALL_URL = NS_AUTHORIZATION_UI + "#usersAll";

	public static final QName AUTZ_UI_USERS_QNAME = new QName(NS_AUTHORIZATION_UI, "users");
	public static final String AUTZ_UI_USERS_URL = NS_AUTHORIZATION_UI + "#users";

    public static final QName AUTZ_UI_FIND_USERS_QNAME = new QName(NS_AUTHORIZATION_UI, "findUsers");
    public static final String AUTZ_UI_FIND_USERS_URL = NS_AUTHORIZATION_UI + "#findUsers";

	public static final QName AUTZ_UI_USER_QNAME = new QName(NS_AUTHORIZATION_UI, "user");
	public static final String AUTZ_UI_USER_URL = NS_AUTHORIZATION_UI + "#user";

	public static final QName AUTZ_UI_USER_HISTORY_QNAME = new QName(NS_AUTHORIZATION_UI, "userHistory");
	public static final String AUTZ_UI_USER_HISTORY_URL = NS_AUTHORIZATION_UI + "#userHistory";

	public static final QName AUTZ_UI_USER_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "userDetails");
	public static final String AUTZ_UI_USER_DETAILS_URL = NS_AUTHORIZATION_UI + "#userDetails";

	public static final QName AUTZ_UI_MERGE_OBJECTS_QNAME = new QName(NS_AUTHORIZATION_UI, "mergeObjects");
	public static final String AUTZ_UI_MERGE_OBJECTS_URL = NS_AUTHORIZATION_UI + "#mergeObjects";

	public static final QName AUTZ_UI_ORG_STRUCT_QNAME = new QName(NS_AUTHORIZATION_UI, "orgStruct");
	public static final String AUTZ_UI_ORG_STRUCT_URL = NS_AUTHORIZATION_UI + "#orgStruct";

	public static final QName AUTZ_UI_ORG_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "orgAll");
	public static final String AUTZ_UI_ORG_ALL_URL = NS_AUTHORIZATION_UI + "#orgAll";

	public static final QName AUTZ_UI_ORG_TREE_QNAME = new QName(NS_AUTHORIZATION_UI, "orgTree");
	public static final String AUTZ_UI_ORG_TREE_URL = NS_AUTHORIZATION_UI + "#orgTree";

	public static final QName AUTZ_UI_ORG_UNIT_QNAME = new QName(NS_AUTHORIZATION_UI, "orgUnit");
	public static final String AUTZ_UI_ORG_UNIT_URL = NS_AUTHORIZATION_UI + "#orgUnit";

	public static final QName AUTZ_UI_SERVICES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "servicesAll");
	public static final String AUTZ_UI_SERVICES_ALL_URL = NS_AUTHORIZATION_UI + "#servicesAll";

	public static final QName AUTZ_UI_SERVICES_QNAME = new QName(NS_AUTHORIZATION_UI, "services");
	public static final String AUTZ_UI_SERVICES_URL = NS_AUTHORIZATION_UI + "#services";

	public static final QName AUTZ_UI_SERVICE_QNAME = new QName(NS_AUTHORIZATION_UI, "service");
	public static final String AUTZ_UI_SERVICE_URL = NS_AUTHORIZATION_UI + "#service";

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

	//Roles
	public static final QName AUTZ_UI_ROLES_ALL_QNAME = new QName(NS_AUTHORIZATION_UI, "rolesAll");
	public static final String AUTZ_UI_ROLES_ALL_URL = NS_AUTHORIZATION_UI + "#rolesAll";

	public static final QName AUTZ_UI_ROLES_QNAME = new QName(NS_AUTHORIZATION_UI, "roles");
	public static final String AUTZ_UI_ROLES_URL = NS_AUTHORIZATION_UI + "#roles";

	public static final QName AUTZ_UI_ROLE_QNAME = new QName(NS_AUTHORIZATION_UI, "role");
	public static final String AUTZ_UI_ROLE_URL = NS_AUTHORIZATION_UI + "#role";

	public static final QName AUTZ_UI_ROLE_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "roleDetails");
	public static final String AUTZ_UI_ROLE_DETAILS_URL = NS_AUTHORIZATION_UI + "#roleDetails";


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

	public static final QName AUTZ_UI_REPORTS_REPORT_CREATE_QNAME = new QName(NS_AUTHORIZATION_UI, "reportCreate");
	public static final String AUTZ_UI_REPORTS_REPORT_CREATE_URL = NS_AUTHORIZATION_UI + "#reportCreate";

    //Certification
	public static final String AUTZ_UI_CERTIFICATION_DEFINITIONS_URL = NS_AUTHORIZATION_UI + "#certificationDefinitions";
	public static final String AUTZ_UI_CERTIFICATION_DEFINITION_URL = NS_AUTHORIZATION_UI + "#certificationDefinition";
	public static final String AUTZ_UI_CERTIFICATION_NEW_DEFINITION_URL = NS_AUTHORIZATION_UI + "#certificationNewDefinition";
	public static final String AUTZ_UI_CERTIFICATION_CAMPAIGNS_URL = NS_AUTHORIZATION_UI + "#certificationCampaigns";
	public static final String AUTZ_UI_CERTIFICATION_CAMPAIGN_URL = NS_AUTHORIZATION_UI + "#certificationCampaign";
	public static final String AUTZ_UI_CERTIFICATION_DECISIONS_URL = NS_AUTHORIZATION_UI + "#certificationDecisions";

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

	public static final QName AUTZ_UI_SELF_CONSENTS_QNAME = new QName(NS_AUTHORIZATION_UI, "selfConsents");
	public static final String AUTZ_UI_SELF_CONSENTS_URL = NS_AUTHORIZATION_UI + "#selfConsents";

	public static final QName AUTZ_UI_SELF_PROFILE_QNAME = new QName(NS_AUTHORIZATION_UI, "selfProfile");
	public static final String AUTZ_UI_SELF_PROFILE_URL = NS_AUTHORIZATION_UI + "#selfProfile";

	public static final QName AUTZ_UI_SELF_ASSIGNMENT_SHOP_KART_QNAME = new QName(NS_AUTHORIZATION_UI, "selfRequestAssignment");
	public static final String AUTZ_UI_SELF_ASSIGNMENT_SHOP_KART_URL = NS_AUTHORIZATION_UI + "#selfRequestAssignment";

	public static final QName AUTZ_UI_SELF_ASSIGNMENT_DETAILS_QNAME = new QName(NS_AUTHORIZATION_UI, "assignmentDetails");
	public static final String AUTZ_UI_SELF_ASSIGNMENT_DETAILS_URL = NS_AUTHORIZATION_UI + "#assignmentDetails";

	public static final QName AUTZ_UI_SELF_DASHBOARD_QNAME = new QName(NS_AUTHORIZATION_UI, "selfDashboard");
	public static final String AUTZ_UI_SELF_DASHBOARD_URL = NS_AUTHORIZATION_UI + "#selfDashboard";


	//About
//	public static final QName AUTZ_UI_ABOUT_MIDPOINT_QNAME = new QName(NS_AUTHORIZATION, "aboutMidpoint");
//	public static final String AUTZ_UI_ABOUT_MIDPOINT_URL = QNameUtil.qNameToUri(AUTZ_UI_ABOUT_MIDPOINT_QNAME);
//
//	public static final QName AUTZ_UI_ABOUT_SYSTEM_QNAME = new QName(NS_AUTHORIZATION, "abountSystem");
//	public static final String AUTZ_UI_ABOUT_SYSTEM_URL = QNameUtil.qNameToUri(AUTZ_UI_ABOUT_SYSTEM_QNAME);

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
	public static final String AUTZ_UI_ADMIN_UNASSIGN_MEMBER_TAB_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_UNASSIGN_MEMBER_ACTION_QNAME);

	public static final QName AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminRecomputeMember");
	public static final String AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_RECOMPUTE_MEMBER_ACTION_QNAME);

	//ui authorizations for menu items on the Governance members tab of Role type object
	public static final QName AUTZ_UI_ADMIN_ASSIGN_GOVERNANCE_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminAssignGovernance");
	public static final String AUTZ_UI_ADMIN_ASSIGN_GOVERNANCE_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_ASSIGN_GOVERNANCE_ACTION_QNAME);

	public static final QName AUTZ_UI_ADMIN_UNASSIGN_GOVERNANCE_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminUnassignGovernance");
	public static final String AUTZ_UI_ADMIN_UNASSIGN_GOVERNANCE_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_UNASSIGN_GOVERNANCE_ACTION_QNAME);

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

	//ui authorization for CSV export button (will be applied everywhere over mp)
	public static final QName AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_QNAME = new QName(NS_AUTHORIZATION_UI, "adminCSVexport");
	public static final String AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI = QNameUtil.qNameToUri(AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_QNAME);
	
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
	public static final Collection<ItemPath> EXECUTION_ITEMS_ALLOWED_BY_DEFAULT = Arrays.asList(
			new ItemPath(ObjectType.F_METADATA),
			new ItemPath(ObjectType.F_PARENT_ORG_REF),
			new ItemPath(ObjectType.F_TENANT_REF),
			new ItemPath(ObjectType.F_TRIGGER),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
			new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_METADATA),
			new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_POLICY_SITUATION),
			new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_METADATA),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_POLICY_SITUATION),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_METADATA),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_POLICY_SITUATION),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
			new ItemPath(FocusType.F_DELEGATED_REF),
			new ItemPath(FocusType.F_ITERATION),
			new ItemPath(FocusType.F_ITERATION_TOKEN),
//			new ItemPath(FocusType.F_LINK_REF), // in fact, linkRef may be omitted here. link/unlink is done after execution authorizations are applied
			new ItemPath(FocusType.F_PERSONA_REF),
			new ItemPath(FocusType.F_ROLE_INFLUENCE_REF),
			new ItemPath(FocusType.F_ROLE_MEMBERSHIP_REF),
			new ItemPath(FocusType.F_TRIGGERED_POLICY_RULE)
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
	 * (also similar evolvability reasoning as for EXECUTION_ITEMS_ALLOWED_BY_DEFAULT)
	 */
	public static final Collection<ItemPath> OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE = Arrays.asList(
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
			new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
			new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_METADATA),
			new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_POLICY_SITUATION),
			new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
			new ItemPath(FocusType.F_ASSIGNMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_METADATA),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_POLICY_SITUATION),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_TRIGGERED_POLICY_RULE),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_REASON),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_METADATA),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_POLICY_SITUATION),
			new ItemPath(AbstractRoleType.F_INDUCEMENT, FocusType.F_ASSIGNMENT, AssignmentType.F_TRIGGERED_POLICY_RULE)
		);

}
