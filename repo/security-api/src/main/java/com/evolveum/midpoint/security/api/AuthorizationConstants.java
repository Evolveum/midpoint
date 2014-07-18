/*
 * Copyright (c) 2010-2013 Evolveum
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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;

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
	
	public static final QName AUTZ_DEVEL_QNAME = new QName(NS_AUTHORIZATION, "devel");
	public static final String AUTZ_DEVEL_URL = NS_AUTHORIZATION + "#devel";
//	public static final String AUTZ_DEVEL_URL = QNameUtil.qNameToUri(AUTZ_DEVEL_QNAME);
	
	public static final QName AUTZ_DENY_ALL_QNAME = new QName(NS_AUTHORIZATION, "denyAll");
	public static final String AUTZ_DENY_ALL_URL = QNameUtil.qNameToUri(AUTZ_DENY_ALL_QNAME);
    public static final String AUTZ_DENY_ALL = NS_AUTHORIZATION + "#denyAll";

    public static final QName AUTZ_GUI_ALL_QNAME = new QName(NS_AUTHORIZATION, "guiAll");
    public static final String AUTZ_GUI_ALL_URI = QNameUtil.qNameToUri(AUTZ_GUI_ALL_QNAME);
    public static final String AUTZ_GUI_ALL_LABEL = "Authorization.constants.guiAll.label";
    public static final String AUTZ_GUI_ALL_DESCRIPTION = "Authorization.constants.guiAll.description";

    //user
	public static final QName AUTZ_UI_USERS_ALL_QNAME = new QName(NS_AUTHORIZATION, "usersAll");
	public static final String AUTZ_UI_USERS_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_USERS_ALL_QNAME);
	
	public static final QName AUTZ_UI_USERS_QNAME = new QName(NS_AUTHORIZATION, "users");
	public static final String AUTZ_UI_USERS_URL = QNameUtil.qNameToUri(AUTZ_UI_USERS_QNAME);

    public static final QName AUTZ_UI_FIND_USERS_QNAME = new QName(NS_AUTHORIZATION, "findUsers");
    public static final String AUTZ_UI_FIND_USERS_URL = QNameUtil.qNameToUri(AUTZ_UI_FIND_USERS_QNAME);
	
	public static final QName AUTZ_UI_USER_QNAME = new QName(NS_AUTHORIZATION, "user");
	public static final String AUTZ_UI_USER_URL = QNameUtil.qNameToUri(AUTZ_UI_USER_QNAME);
	
	public static final QName AUTZ_UI_USER_DETAILS_QNAME = new QName(NS_AUTHORIZATION, "userDetails");
	public static final String AUTZ_UI_USER_DETAILS_URL = QNameUtil.qNameToUri(AUTZ_UI_USER_DETAILS_QNAME);
	
	public static final QName AUTZ_UI_ORG_STRUCT_QNAME = new QName(NS_AUTHORIZATION, "orgStruct");
	public static final String AUTZ_UI_ORG_STRUCT_URL = QNameUtil.qNameToUri(AUTZ_UI_ORG_STRUCT_QNAME);

	
	//resources
	public static final QName AUTZ_UI_RESOURCES_ALL_QNAME = new QName(NS_AUTHORIZATION, "resourcesAll");
	public static final String AUTZ_UI_RESOURCES_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_RESOURCES_ALL_QNAME);
	
	public static final QName AUTZ_UI_RESOURCES_QNAME = new QName(NS_AUTHORIZATION, "resources");
	public static final String AUTZ_UI_RESOURCES_URL = QNameUtil.qNameToUri(AUTZ_UI_RESOURCES_QNAME);

	public static final QName AUTZ_UI_RESOURCE_QNAME = new QName(NS_AUTHORIZATION, "resource");
	public static final String AUTZ_UI_RESOURCE_URL = QNameUtil.qNameToUri(AUTZ_UI_RESOURCE_QNAME);
	
	public static final QName AUTZ_UI_RESOURCE_DETAILS_QNAME = new QName(NS_AUTHORIZATION, "resourceDetails");
	public static final String AUTZ_UI_RESOURCE_DETAILS_URL = QNameUtil.qNameToUri(AUTZ_UI_RESOURCE_DETAILS_QNAME);
	
	public static final QName AUTZ_UI_RESOURCE_EDIT_QNAME = new QName(NS_AUTHORIZATION, "resourceEdit");
	public static final String AUTZ_UI_RESOURCE_EDIT_URL = QNameUtil.qNameToUri(AUTZ_UI_RESOURCE_EDIT_QNAME);
	
	public static final QName AUTZ_UI_RESOURCES_ACCOUNT_QNAME = new QName(NS_AUTHORIZATION, "resourcesAccount");
	public static final String AUTZ_UI_RESOURCES_ACCOUNT_URL = QNameUtil.qNameToUri(AUTZ_UI_RESOURCES_ACCOUNT_QNAME);
	
	public static final QName AUTZ_UI_RESOURCES_CONTENT_ACCOUNTS_QNAME = new QName(NS_AUTHORIZATION, "resourcesContentAccount");
	public static final String AUTZ_UI_RESOURCES_CONTENT_ACCOUNTS_URL = QNameUtil.qNameToUri(AUTZ_UI_RESOURCES_CONTENT_ACCOUNTS_QNAME);
	
	public static final QName AUTZ_UI_RESOURCES_CONTENT_ENTITLEMENTS_QNAME = new QName(NS_AUTHORIZATION, "resourcesContentEntitlements");
	public static final String AUTZ_UI_RESOURCES_CONTENT_ENTITLEMENTS_URL = QNameUtil.qNameToUri(AUTZ_UI_RESOURCES_CONTENT_ENTITLEMENTS_QNAME);
	
	
	//Configuration
	public static final QName AUTZ_UI_CONFIGURATION_ALL_QNAME = new QName(NS_AUTHORIZATION, "configurationAll");
	public static final String AUTZ_UI_CONFIGURATION_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_CONFIGURATION_ALL_QNAME);
	
	public static final QName AUTZ_UI_CONFIGURATION_QNAME = new QName(NS_AUTHORIZATION, "configuration");
	public static final String AUTZ_UI_CONFIGURATION_URL = QNameUtil.qNameToUri(AUTZ_UI_CONFIGURATION_QNAME);
	
	public static final QName AUTZ_UI_CONFIGURATION_DEBUG_QNAME = new QName(NS_AUTHORIZATION, "debug");
	public static final String AUTZ_UI_CONFIGURATION_DEBUG_URL = QNameUtil.qNameToUri(AUTZ_UI_CONFIGURATION_DEBUG_QNAME);
	
	public static final QName AUTZ_UI_CONFIGURATION_DEBUGS_QNAME = new QName(NS_AUTHORIZATION, "debugs");
	public static final String AUTZ_UI_CONFIGURATION_DEBUGS_URL = QNameUtil.qNameToUri(AUTZ_UI_CONFIGURATION_DEBUGS_QNAME);
	
	public static final QName AUTZ_UI_CONFIGURATION_IMPORT_QNAME = new QName(NS_AUTHORIZATION, "configImport");
	public static final String AUTZ_UI_CONFIGURATION_IMPORT_URL = QNameUtil.qNameToUri(AUTZ_UI_CONFIGURATION_IMPORT_QNAME);
	
	public static final QName AUTZ_UI_CONFIGURATION_LOGGING_QNAME = new QName(NS_AUTHORIZATION, "configLogging");
	public static final String AUTZ_UI_CONFIGURATION_LOGGING_URL = QNameUtil.qNameToUri(AUTZ_UI_CONFIGURATION_LOGGING_QNAME);

    public static final QName AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_QNAME = new QName(NS_AUTHORIZATION, "configSystemConfiguration");
    public static final String AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_URL = QNameUtil.qNameToUri(AUTZ_UI_CONFIGURATION_SYSTEM_CONFIG_QNAME);
	
	//Roles
	public static final QName AUTZ_UI_ROLES_ALL_QNAME = new QName(NS_AUTHORIZATION, "rolesAll");
	public static final String AUTZ_UI_ROLES_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_ROLES_ALL_QNAME);
	
	public static final QName AUTZ_UI_ROLES_QNAME = new QName(NS_AUTHORIZATION, "roles");
	public static final String AUTZ_UI_ROLES_URL = QNameUtil.qNameToUri(AUTZ_UI_ROLES_QNAME);
	
	public static final QName AUTZ_UI_ROLE_QNAME = new QName(NS_AUTHORIZATION, "role");
	public static final String AUTZ_UI_ROLE_URL = QNameUtil.qNameToUri(AUTZ_UI_ROLE_QNAME);
	
	public static final QName AUTZ_UI_ROLE_DETAILS_QNAME = new QName(NS_AUTHORIZATION, "roleDetails");
	public static final String AUTZ_UI_ROLE_DETAILS_URL = QNameUtil.qNameToUri(AUTZ_UI_ROLE_DETAILS_QNAME);
	
	
	//Work Items
	public static final QName AUTZ_UI_WORK_ITEMS_ALL_QNAME = new QName(NS_AUTHORIZATION, "workItemsAll");
	public static final String AUTZ_UI_WORK_ITEMS_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_WORK_ITEMS_ALL_QNAME);
	
	public static final QName AUTZ_UI_WORK_ITEMS_QNAME = new QName(NS_AUTHORIZATION, "workItems");
	public static final String AUTZ_UI_WORK_ITEMS_URL = QNameUtil.qNameToUri(AUTZ_UI_WORK_ITEMS_QNAME);
	
	public static final QName AUTZ_UI_WORK_ITEM_QNAME = new QName(NS_AUTHORIZATION, "workItem");
	public static final String AUTZ_UI_WORK_ITEM_URL = QNameUtil.qNameToUri(AUTZ_UI_WORK_ITEM_QNAME);
	
	public static final QName AUTZ_UI_WORK_ITEMS_ALL_REQUESTS_QNAME = new QName(NS_AUTHORIZATION, "workItemsAllRequests");
	public static final String AUTZ_UI_WORK_ITEMS_ALL_REQUESTS_URL = QNameUtil.qNameToUri(AUTZ_UI_WORK_ITEMS_ALL_REQUESTS_QNAME);
	
	public static final QName AUTZ_UI_WORK_ITEMS_MY_REQUESTS_QNAME = new QName(NS_AUTHORIZATION, "workItemsMyRequests");
	public static final String AUTZ_UI_WORK_ITEMS_MY_REQUESTS_URL = QNameUtil.qNameToUri(AUTZ_UI_WORK_ITEMS_MY_REQUESTS_QNAME);
	
	public static final QName AUTZ_UI_WORK_ITEMS_ABOUT_ME_REQUESTS_QNAME = new QName(NS_AUTHORIZATION, "workItemsAboutMeRequests");
	public static final String AUTZ_UI_WORK_ITEMS_ABOUT_ME_REQUESTS_URL = QNameUtil.qNameToUri(AUTZ_UI_WORK_ITEMS_ABOUT_ME_REQUESTS_QNAME);
	
	public static final QName AUTZ_UI_WORK_ITEMS_PROCESS_INSTANCE_QNAME = new QName(NS_AUTHORIZATION, "workItemsProcessInstance");
	public static final String AUTZ_UI_WORK_ITEMS_PROCESS_INSTANCE_URL = QNameUtil.qNameToUri(AUTZ_UI_WORK_ITEMS_PROCESS_INSTANCE_QNAME);

    public static final QName AUTZ_UI_WORK_ITEMS_APPROVE_OTHERS_ITEMS_QNAME = new QName(NS_AUTHORIZATION, "workItemsApproveOthersItems");
    public static final String AUTZ_UI_WORK_ITEMS_APPROVE_OTHERS_ITEMS_URL = QNameUtil.qNameToUri(AUTZ_UI_WORK_ITEMS_APPROVE_OTHERS_ITEMS_QNAME);

	//Tasks
	public static final QName AUTZ_UI_TASKS_ALL_QNAME = new QName(NS_AUTHORIZATION, "tasksAll");
	public static final String AUTZ_UI_TASKS_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_TASKS_ALL_QNAME);
	
	public static final QName AUTZ_UI_TASKS_QNAME = new QName(NS_AUTHORIZATION, "tasks");
	public static final String AUTZ_UI_TASKS_URL = QNameUtil.qNameToUri(AUTZ_UI_TASKS_QNAME);
	
	public static final QName AUTZ_UI_TASK_QNAME = new QName(NS_AUTHORIZATION, "task");
	public static final String AUTZ_UI_TASK_URL = QNameUtil.qNameToUri(AUTZ_UI_TASK_QNAME);
	
	public static final QName AUTZ_UI_TASK_DETAIL_QNAME = new QName(NS_AUTHORIZATION, "taskDetails");
	public static final String AUTZ_UI_TASK_DETAIL_URL = QNameUtil.qNameToUri(AUTZ_UI_TASK_DETAIL_QNAME);
	
	public static final QName AUTZ_UI_TASK_ADD_QNAME = new QName(NS_AUTHORIZATION, "taskAdd");
	public static final String AUTZ_UI_TASK_ADD_URL = QNameUtil.qNameToUri(AUTZ_UI_TASK_ADD_QNAME);
	
	
	//Reports
	public static final QName AUTZ_UI_REPORTS_QNAME = new QName(NS_AUTHORIZATION, "reports");
	public static final String AUTZ_UI_REPORTS_URL = QNameUtil.qNameToUri(AUTZ_UI_REPORTS_QNAME);
	
	//Home
	public static final QName AUTZ_UI_DASHBOARD_QNAME = new QName(NS_AUTHORIZATION, "dashboard");
	public static final String AUTZ_UI_DASHBOARD_URL = QNameUtil.qNameToUri(AUTZ_UI_DASHBOARD_QNAME);
	
	public static final QName AUTZ_UI_MY_PASSWORDS_QNAME = new QName(NS_AUTHORIZATION, "myPasswords");
	public static final String AUTZ_UI_MY_PASSWORDS_URL = QNameUtil.qNameToUri(AUTZ_UI_MY_PASSWORDS_QNAME);
	
	public static final QName AUTZ_UI_HOME_ALL_QNAME = new QName(NS_AUTHORIZATION, "home");
	public static final String AUTZ_UI_HOME_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_HOME_ALL_QNAME);
	
	//permitAll  - it means, no authorization is required, everyone can access these resources, this is user for about system and about midpoint pages..
	public static final QName AUTZ_UI_PERMIT_ALL_QNAME = new QName(NS_AUTHORIZATION, "permitAll");
	public static final String AUTZ_UI_PERMIT_ALL_URL = QNameUtil.qNameToUri(AUTZ_UI_PERMIT_ALL_QNAME);
    public static final String AUTZ_UI_PERMIT_ALL = NS_AUTHORIZATION + "#permitAll";
	
	

	
	//About
//	public static final QName AUTZ_UI_ABOUT_MIDPOINT_QNAME = new QName(NS_AUTHORIZATION, "aboutMidpoint");
//	public static final String AUTZ_UI_ABOUT_MIDPOINT_URL = QNameUtil.qNameToUri(AUTZ_UI_ABOUT_MIDPOINT_QNAME);
//	
//	public static final QName AUTZ_UI_ABOUT_SYSTEM_QNAME = new QName(NS_AUTHORIZATION, "abountSystem");
//	public static final String AUTZ_UI_ABOUT_SYSTEM_URL = QNameUtil.qNameToUri(AUTZ_UI_ABOUT_SYSTEM_QNAME);
	
}
