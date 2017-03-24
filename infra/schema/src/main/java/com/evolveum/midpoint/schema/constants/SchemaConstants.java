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

package com.evolveum.midpoint.schema.constants;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Vilo Repan
 * @author Radovan Semancik
 */
public abstract class SchemaConstants {

	public static final String NS_MIDPOINT_PUBLIC = "http://midpoint.evolveum.com/xml/ns/public";
	public static final String NS_MIDPOINT_TEST = "http://midpoint.evolveum.com/xml/ns/test";

	public static final Map<String, String> prefixNsMap = new HashMap<String, String>();

	// NAMESPACES

	public static final String NS_ORG = "http://midpoint.evolveum.com/xml/ns/public/common/org-3";
	public static final String PREFIX_NS_ORG = "org";
	public static final String NS_QUERY = QueryConvertor.NS_QUERY;
	public static final String NS_QUERY_PREFIX = "q";
	public static final String NS_TYPES = PrismConstants.NS_TYPES;
	public static final String NS_TYPES_PREFIX = "t";
	public static final String NS_API_TYPES = "http://midpoint.evolveum.com/xml/ns/public/common/api-types-3";
	public static final String NS_MIDPOINT_PUBLIC_PREFIX = "http://midpoint.evolveum.com/xml/ns/public/";
	public static final String NS_C = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
	public static final String NS_C_PREFIX = "c";
	public static final String NS_CAPABILITIES = "http://midpoint.evolveum.com/xml/ns/public/resource/capabilities-3";
	public static final String NS_FILTER = NS_MIDPOINT_PUBLIC + "/common/value-filter-1.xsd";
	public static final String NS_MATCHING_RULE = NS_MIDPOINT_PUBLIC + "/common/matching-rule-3";
	public static final String NS_FAULT = "http://midpoint.evolveum.com/xml/ns/public/common/fault-3";
	public static final String NS_SAMPLES_EXTENSION = "http://midpoint.evolveum.com/xml/ns/samples/extension-3";
	
	/**
	 * Namespace for default (bult-in) object collections, such as "all objects", "all roles", ...
	 */
	public static final String NS_OBJECT_COLLECTIONS = NS_MIDPOINT_PUBLIC + "/common/object-collections-3";

	// COMMON NAMESPACE

	public static final QName C_FILTER_TYPE_URI = new QName(NS_QUERY, "uri");
	public static final QName C_ITEM = new QName(NS_C, "item");
	public static final QName C_OBJECTS = new QName(NS_C, "objects");
	public static final QName C_OBJECT = new QName(NS_C, "object");
	public static final QName C_TARGET = new QName(NS_C, "target");
	public static final QName C_ABSTRACT_ROLE = new QName(NS_C, "abstractRole");
	public static final QName C_FOCUS = new QName(NS_C, "focus");
	public static final QName C_OBJECT_TYPE = new QName(NS_C, "ObjectType");
	public static final QName C_OBJECT_REF = new QName(NS_C, "objectRef");
	public static final QName C_VALUE = new QName(NS_C, "value");
	public static final QName C_PARAM_VALUE = new QName(NS_C, "paramValue");
	public static final QName C_REPORT_PARAM_VALUE = new QName(NS_C, "reportParamValue");
	public static final QName C_OID_ATTRIBUTE = new QName(NS_C, "oid");
	public static final QName C_USER_TYPE = new QName(NS_C, "UserType");
	public static final QName C_TASK_TYPE = new QName(NS_C, "TaskType");
	public static final QName C_TASK = new QName(NS_C, "task");
	// public static final QName C_TASK_REQUESTEE = new QName(NS_C,
	// "requestee");
	// public static final QName C_TASK_REQUESTEE_REF = new QName(NS_C,
	// "requesteeRef");
	// public static final QName C_TASK_REQUESTEE_OID = new QName(NS_C,
	// "requesteeOid");
	public static final QName C_RESOURCE = new QName(NS_C, "resource");
	public static final QName C_RESULT = new QName(NS_C, "result");
	public static final QName C_USER = new QName(NS_C, "user");
	public static final QName C_REQUESTER = new QName(NS_C, "requester");
	public static final QName C_REQUESTEE = new QName(NS_C, "requestee");
	public static final QName C_ASSIGNEE = new QName(NS_C, "assignee");
	public static final QName C_OBJECT_TEMPLATE = new QName(NS_C, "objectTemplate");
	public static final QName C_OBJECT_TEMPLATE_REF = new QName(NS_C, "objectTemplateRef");
	public static final QName C_OBJECT_TEMPLATE_TYPE = new QName(NS_C, "ObjectTemplateType");
	public static final QName C_GENERIC_OBJECT_TYPE = new QName(NS_C, "GenericObjectType");
	public static final QName C_GENERIC_OBJECT = new QName(NS_C, "genericObject");
	public static final QName C_ACCOUNT = new QName(NS_C, "account");
	public static final QName C_ACCOUNT_SHADOW_TYPE = new QName(NS_C, "AccountShadowType");
	public static final QName C_RESOURCE_TYPE = new QName(NS_C, "ResourceType");
	public static final QName C_CONNECTOR_TYPE = new QName(NS_C, "ConnectorType");
	public static final QName C_CONNECTOR = new QName(NS_C, "connector");
	public static final QName C_CONNECTOR_HOST_TYPE = new QName(NS_C, "ConnectorHostType");
	public static final QName C_CONNECTOR_HOST = new QName(NS_C, "connectorHost");
	public static final QName C_CONNECTOR_FRAMEWORK = new QName(NS_C, "framework");
	public static final QName C_CONNECTOR_CONNECTOR_TYPE = new QName(NS_C, "connectorType");
	public static final QName C_SHADOW = new QName(NS_C, "shadow");
	public static final QName C_SHADOW_TYPE = new QName(NS_C, "ShadowType");
        public static final QName C_ORG_TYPE = new QName(NS_C, "OrgType");
	public static final QName C_ATTRIBUTES = new QName(NS_C, "attributes");
	public static final QName C_ASSOCIATION = new QName(NS_C, "association");
	public static final QName C_CREDENTIALS_TYPE = new QName(NS_C, "CredentialsType");
	public static final QName C_CREDENTIALS = new QName(NS_C, "credentials");
	public static final QName C_ACTIVATION = new QName(NS_C, "activation");
	public static final QName C_SYSTEM_CONFIGURATION_TYPE = new QName(NS_C, "SystemConfigurationType");
	public static final QName C_SYSTEM_CONFIGURATION = new QName(NS_C, "systemConfiguration");
	public static final QName C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS = new QName(NS_C,
			"globalAccountSynchronizationSettings");

	public static final QName C_REPORT = new QName(NS_C, "report");
	public static final QName C_REPORT_OUTPUT = new QName(NS_C, "reportOutput");
	public static final QName C_ITEM_PATH_FIELD = new QName(NS_C, "itemPathField");
	public static final QName C_ACTIVATION_STATUS_TYPE = new QName(NS_C, "ActivationStatusType");
	public static final QName C_SECURITY_POLICY = new QName(NS_C, "securityPolicy");
	public static final QName C_MODEL_EXECUTE_OPTIONS = new QName(NS_C, "modelExecuteOptions");

	public static final QName T_POLY_STRING_TYPE = new QName(SchemaConstantsGenerated.NS_TYPES,
			"PolyStringType");
	public static final QName T_OBJECT_DELTA = new QName(SchemaConstantsGenerated.NS_TYPES, "objectDelta");
	public static final QName T_OBJECT_DELTA_TYPE = new QName(SchemaConstantsGenerated.NS_TYPES,
			"ObjectDeltaType");
	// public static final QName T_PROTECTED_STRING_TYPE = new QName(NS_C,
	// "ProtectedStringType");
	// public static final QName T_PROTECTED_STRING = new QName(NS_C,
	// "protectedString");
	// public static final QName T_PROTECTED_BYTE_ARRAY_TYPE = new QName(NS_C,
	// "ProtectedByteArrayType");

	/**
	 * Default relation, usually meaning "has" or "is member of". Used as a relation value in object references.
	 * Specifies that the subject is a member of organization, or that the subject
	 * has been assigned a role in a way that he gets authorizations and other content
	 * provided by that role.
	 */
	public static final QName ORG_DEFAULT = new QName(NS_ORG, "default");

	/**
	 * Relation "is manager of". Used as a relation value in object references.
	 * Specifies that the subject is a manager of organizational unit.
	 */
	public static final QName ORG_MANAGER = new QName(NS_ORG, "manager");

	/**
	 * Relation used for metarole assignments. Sometimes it is important to
	 * distinguish metarole and member assignments. This relation is used
	 * for that purpose.
	 */
	public static final QName ORG_META = new QName(NS_ORG, "meta");

	/**
	 * Relation "is deputy of". Used as a relation value in object references.
	 * Specifies that the subject is a deputy of another user.
	 */
	public static final QName ORG_DEPUTY = new QName(NS_ORG, "deputy");
	
	/**
	 * Relation "is approver of". Used as a relation value in object references.
	 * Specifies that the subject is a (general) approver of specified (abstract) role.
	 * The approver will be asked for decision if the role is assigned, if there is
	 * a rule conflict during assignment (e.g. SoD conflict) or if there is any similar
	 * situation.
	 * 
	 * This is a generic approver used for all the situation. The system may be customized
	 * with more specific approver roles, e.g. technicalApprover, securityApprover, etc.
	 * 
	 * This approver is responsible for the use of the role, which mostly means
	 * that he decides about role assignment. It is NOT meant to approve role changes.
	 * Role owner is meant for that purpose.
	 */
	public static final QName ORG_APPROVER = new QName(NS_ORG, "approver");
	
	/**
	 * Relation "is owner of". Used as a relation value in object references.
	 * Specifies that the subject is a (business) owner of specified (abstract) role.
	 * The owner will be asked for decision if the role is modified, when the associated
	 * policy changes and so on.
	 * 
	 * This owner is responsible for maintaining role definition and policies. It is
	 * NPT necessarily concerned with role use (e.g. assignment). The approver relation
	 * is meant for that purpose. 
	 */
	public static final QName ORG_OWNER = new QName(NS_ORG, "owner");

	public static final ItemPath PATH_PASSWORD = new ItemPath(C_CREDENTIALS, CredentialsType.F_PASSWORD);
	public static final ItemPath PATH_PASSWORD_VALUE = new ItemPath(C_CREDENTIALS, CredentialsType.F_PASSWORD,
			PasswordType.F_VALUE);
	public static final ItemPath PATH_NONCE = new ItemPath(C_CREDENTIALS, CredentialsType.F_NONCE);
	public static final ItemPath PATH_NONCE_VALUE = new ItemPath(C_CREDENTIALS, CredentialsType.F_NONCE,
			NonceType.F_VALUE);
	public static final ItemPath PATH_ACTIVATION = new ItemPath(C_ACTIVATION);
	public static final ItemPath PATH_ACTIVATION_ADMINISTRATIVE_STATUS = new ItemPath(C_ACTIVATION,
			ActivationType.F_ADMINISTRATIVE_STATUS);
	public static final ItemPath PATH_ACTIVATION_EFFECTIVE_STATUS = new ItemPath(C_ACTIVATION,
			ActivationType.F_EFFECTIVE_STATUS);
	public static final ItemPath PATH_ACTIVATION_VALID_FROM = new ItemPath(C_ACTIVATION,
			ActivationType.F_VALID_FROM);
	public static final ItemPath PATH_ACTIVATION_VALID_TO = new ItemPath(C_ACTIVATION,
			ActivationType.F_VALID_TO);
	public static final ItemPath PATH_ACTIVATION_DISABLE_REASON = new ItemPath(ShadowType.F_ACTIVATION,
			ActivationType.F_DISABLE_REASON);
	public static final ItemPath PATH_ACTIVATION_LOCKOUT_STATUS = new ItemPath(C_ACTIVATION,
			ActivationType.F_LOCKOUT_STATUS);
	public static final ItemPath PATH_OPERATIONAL_STATE_LAST_AVAILABILITY_STATUS = new ItemPath(
			ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS);
	public static final ItemPath PATH_ATTRIBUTES = new ItemPath(C_ATTRIBUTES);
	public static final ItemPath PATH_ASSIGNMENT = new ItemPath(FocusType.F_ASSIGNMENT);
	public static final ItemPath PATH_ASSOCIATION = new ItemPath(C_ASSOCIATION);
	public static final ItemPath PATH_TRIGGER = new ItemPath(ObjectType.F_TRIGGER);
	public static final ItemPath PATH_CREDENTIALS_PASSWORD_FAILED_LOGINS = new ItemPath(
			UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_FAILED_LOGINS);
	public static final ItemPath PATH_CREDENTIALS_NONCE_FAILED_LOGINS = new ItemPath(
			UserType.F_CREDENTIALS, CredentialsType.F_NONCE, PasswordType.F_FAILED_LOGINS);
	public static final ItemPath PATH_CREDENTIALS_SECURITY_QUESTIONS_FAILED_LOGINS = new ItemPath(
			UserType.F_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS, PasswordType.F_FAILED_LOGINS);
	public static final ItemPath PATH_LINK_REF = new ItemPath(FocusType.F_LINK_REF);
	public static final ItemPath PATH_LIFECYCLE_STATE = new ItemPath(ObjectType.F_LIFECYCLE_STATE);

	public static final String NS_PROVISIONING = NS_MIDPOINT_PUBLIC + "/provisioning";
	public static final String NS_PROVISIONING_LIVE_SYNC = NS_PROVISIONING + "/liveSync-3";
	public static final QName SYNC_TOKEN = new QName(NS_PROVISIONING_LIVE_SYNC, "token");
        
	// Synchronization constants
	public static final String NS_PROVISIONING_CHANNEL = NS_PROVISIONING + "/channels-3";
	public static final QName CHANGE_CHANNEL_LIVE_SYNC = new QName(NS_PROVISIONING_CHANNEL, "liveSync");
	public static final String CHANGE_CHANNEL_LIVE_SYNC_URI = QNameUtil.qNameToUri(CHANGE_CHANNEL_LIVE_SYNC);
	public static final QName CHANGE_CHANNEL_RECON = new QName(NS_PROVISIONING_CHANNEL, "reconciliation");
	public static final String CHANGE_CHANNEL_RECON_URI = QNameUtil.qNameToUri(CHANGE_CHANNEL_RECON);
	public static final QName CHANGE_CHANNEL_RECOMPUTE = new QName(NS_PROVISIONING_CHANNEL, "recompute");
	public static final QName CHANGE_CHANNEL_DISCOVERY = new QName(NS_PROVISIONING_CHANNEL, "discovery");
	public static final String CHANGE_CHANNEL_DISCOVERY_URI = QNameUtil.qNameToUri(CHANGE_CHANNEL_DISCOVERY);
	public static final QName CHANGE_CHANNEL_IMPORT = new QName(NS_PROVISIONING_CHANNEL, "import");

	public static final String NS_MODEL = NS_MIDPOINT_PUBLIC + "/model";
	public static final String NS_MODEL_WS = NS_MODEL + "/model-3";

	public static final String NS_REPORT = NS_MIDPOINT_PUBLIC + "/report";
	public static final String NS_REPORT_WS = NS_REPORT + "/report-3";
	public static final String NS_CERTIFICATION = NS_MIDPOINT_PUBLIC + "/certification";
	public static final String NS_WORKFLOW = NS_MIDPOINT_PUBLIC + "/workflow";

	public static final String NS_MODEL_CHANNEL = NS_MODEL + "/channels-3";
	public static final QName CHANNEL_WEB_SERVICE_QNAME = new QName(NS_MODEL_CHANNEL, "webService");
	public static final String CHANNEL_WEB_SERVICE_URI = QNameUtil.qNameToUri(CHANNEL_WEB_SERVICE_QNAME);
	public static final QName CHANNEL_OBJECT_IMPORT_QNAME = new QName(NS_MODEL_CHANNEL, "objectImport");
	public static final String CHANNEL_OBJECT_IMPORT_URI = QNameUtil.qNameToUri(CHANNEL_OBJECT_IMPORT_QNAME);
	public static final QName CHANNEL_REST_QNAME = new QName(NS_MODEL_CHANNEL, "rest");
	public static final String CHANNEL_REST_URI = QNameUtil.qNameToUri(CHANNEL_REST_QNAME);

	public static final String NS_MODEL_SERVICE = NS_MODEL + "/service-3";

	public static final String NS_MODEL_EXTENSION = NS_MODEL + "/extension-3";
	public static final QName MODEL_EXTENSION_FRESHENESS_INTERVAL_PROPERTY_NAME = new QName(
			NS_MODEL_EXTENSION, "freshnessInterval"); // unused? TODO consider
														// removing
	public static final QName MODEL_EXTENSION_DRY_RUN = new QName(NS_MODEL_EXTENSION, "dryRun");
        public static final QName SYNC_TOKEN_RETRY_UNHANDLED = new QName(NS_MODEL_EXTENSION, "retryLiveSyncErrors");
	public static final QName MODEL_EXTENSION_FINISH_OPERATIONS_ONLY = new QName(NS_MODEL_EXTENSION, "finishOperationsOnly");
	public static final QName MODEL_EXTENSION_KIND = new QName(NS_MODEL_EXTENSION, "kind");
	public static final QName MODEL_EXTENSION_INTENT = new QName(NS_MODEL_EXTENSION, "intent");
	public static final QName OBJECTCLASS_PROPERTY_NAME = new QName(NS_MODEL_EXTENSION, "objectclass");
	public static final QName MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME = new QName(
			NS_MODEL_EXTENSION, "lastScanTimestamp");

	public static final String NS_MODEL_DISABLE_REASON = NS_MODEL + "/disableReason";
	public static final String MODEL_DISABLE_REASON_EXPLICIT = 
			QNameUtil.qNameToUri(new QName(NS_MODEL_DISABLE_REASON, "explicit"));
	public static final String MODEL_DISABLE_REASON_DEPROVISION = 
			QNameUtil.qNameToUri(new QName(NS_MODEL_DISABLE_REASON, "deprovision"));
	public static final String MODEL_DISABLE_REASON_MAPPED = 
			QNameUtil.qNameToUri(new QName(NS_MODEL_DISABLE_REASON, "mapped"));

	public static final String NS_MODEL_POLICY = NS_MODEL + "/policy";
	public static final String NS_MODEL_POLICY_SITUATION = NS_MODEL_POLICY + "/situation";
	public static final String MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION = 
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "exclusionViolation"));
	public static final String MODEL_POLICY_SITUATION_UNDERASSIGNED = 
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "underassigned"));
	public static final String MODEL_POLICY_SITUATION_OVERASSIGNED = 
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "overassigned"));
	public static final String MODEL_POLICY_SITUATION_MODIFIED = 
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "modified"));
	public static final String MODEL_POLICY_SITUATION_ASSIGNED = 
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "assigned"));

	
	public static final QName MODEL_EXTENSION_OBJECT_TYPE = new QName(NS_MODEL_EXTENSION, "objectType");
	public static final QName MODEL_EXTENSION_OBJECT_QUERY = new QName(NS_MODEL_EXTENSION, "objectQuery");
	public static final QName MODEL_EXTENSION_OBJECT_DELTA = new QName(NS_MODEL_EXTENSION, "objectDelta");
	public static final QName MODEL_EXTENSION_WORKER_THREADS = new QName(NS_MODEL_EXTENSION, "workerThreads");
	public static final QName MODEL_EXTENSION_OPTION_RAW = new QName(NS_MODEL_EXTENSION, "optionRaw");

	public static final QName MODEL_EXTENSION_DIAGNOSE = new QName(NS_MODEL_EXTENSION, "diagnose");
	public static final QName MODEL_EXTENSION_FIX = new QName(NS_MODEL_EXTENSION, "fix");
	public static final QName MODEL_EXTENSION_DUPLICATE_SHADOWS_RESOLVER = new QName(NS_MODEL_EXTENSION,
			"duplicateShadowsResolver");
	public static final QName MODEL_EXTENSION_CHECK_DUPLICATES_ON_PRIMARY_IDENTIFIERS_ONLY = new QName(
			NS_MODEL_EXTENSION, "checkDuplicatesOnPrimaryIdentifiersOnly");

	public static final QName MODEL_EXTENSION_CLEANUP_POLICIES = new QName(NS_MODEL_EXTENSION,
			"cleanupPolicies");

	public static final QName MODEL_EXTENSION_WORK_ITEM_ID = new QName(NS_MODEL_EXTENSION, "workItemId");
	public static final QName MODEL_EXTENSION_WORK_ITEM_ACTIONS = new QName(NS_MODEL_EXTENSION, "workItemActions");
	public static final QName MODEL_EXTENSION_WORK_ITEM_ACTION = new QName(NS_MODEL_EXTENSION, "workItemAction");
	public static final QName MODEL_EXTENSION_TIME_BEFORE_ACTION = new QName(NS_MODEL_EXTENSION, "timeBeforeAction");

	public static final String NOOP_SCHEMA_URI = NS_MIDPOINT_PUBLIC + "/task/noop/handler-3";
	public static final QName NOOP_DELAY_QNAME = new QName(NOOP_SCHEMA_URI, "delay");
	public static final QName NOOP_STEPS_QNAME = new QName(NOOP_SCHEMA_URI, "steps");

	public static final String JDBC_PING_SCHEMA_URI = NS_MIDPOINT_PUBLIC + "/task/jdbc-ping/handler-3";
	public static final QName JDBC_PING_TESTS_QNAME = new QName(JDBC_PING_SCHEMA_URI, "tests");
	public static final QName JDBC_PING_INTERVAL_QNAME = new QName(JDBC_PING_SCHEMA_URI, "interval");
	public static final QName JDBC_PING_TEST_QUERY_QNAME = new QName(JDBC_PING_SCHEMA_URI, "testQuery");
	public static final QName JDBC_PING_DRIVER_CLASS_NAME_QNAME = new QName(JDBC_PING_SCHEMA_URI, "driverClassName");
	public static final QName JDBC_PING_JDBC_URL_QNAME = new QName(JDBC_PING_SCHEMA_URI, "jdbcUrl");
	public static final QName JDBC_PING_JDBC_USERNAME_QNAME = new QName(JDBC_PING_SCHEMA_URI, "jdbcUsername");
	public static final QName JDBC_PING_JDBC_PASSWORD_QNAME = new QName(JDBC_PING_SCHEMA_URI, "jdbcPassword");
	public static final QName JDBC_PING_LOG_ON_INFO_LEVEL_QNAME = new QName(JDBC_PING_SCHEMA_URI, "logOnInfoLevel");

	public static final String NS_GUI = NS_MIDPOINT_PUBLIC + "/gui";
	public static final String NS_GUI_CHANNEL = NS_GUI + "/channels-3";
	public static final QName CHANNEL_GUI_INIT_QNAME = new QName(NS_GUI_CHANNEL, "init");
	public static final String CHANNEL_GUI_INIT_URI = QNameUtil.qNameToUri(CHANNEL_GUI_INIT_QNAME);
	public static final QName CHANNEL_GUI_SELF_REGISTRATION_QNAME = new QName(NS_GUI_CHANNEL, "seflRegistration");
	public static final String CHANNEL_GUI_SELF_REGISTRATION_URI = QNameUtil.qNameToUri(CHANNEL_GUI_SELF_REGISTRATION_QNAME);
	public static final QName CHANNEL_GUI_RESET_PASSWORD_QNAME = new QName(NS_GUI_CHANNEL, "resetPassword");
	public static final String CHANNEL_GUI_RESET_PASSWORD_URI = QNameUtil.qNameToUri(CHANNEL_GUI_RESET_PASSWORD_QNAME);
	public static final QName CHANNEL_GUI_USER_QNAME = new QName(NS_GUI_CHANNEL, "user");
	public static final String CHANNEL_GUI_USER_URI = QNameUtil.qNameToUri(CHANNEL_GUI_USER_QNAME);

	public static final String INTENT_DEFAULT = "default";

	// This constant should not be here. It is used by schema processor to
	// supply correct import. But the dependency should
	// be inverted, eventually (MID-356)
	public static final String NS_ICF_CONFIGURATION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-schema-3";
	public static final QName ICF_CONFIGURATION_PROPERTIES = new QName(NS_ICF_CONFIGURATION,
			"configurationProperties");
	public static final QName ICF_TIMEOUTS = new QName(NS_ICF_CONFIGURATION, "timeouts");
	public static final QName ICF_RESULTS_HANDLER_CONFIGURATION = new QName(NS_ICF_CONFIGURATION,
			"resultsHandlerConfiguration");
	public static final QName ICF_CONNECTOR_POOL_CONFIGURATION = new QName(NS_ICF_CONFIGURATION,
			"connectorPoolConfiguration");

	// These are used in script expressions, they should remain here
	public static final String NS_ICF_SCHEMA = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";
	public static final String NS_ICF_SCHEMA_PREFIX = "icfs";
	public static final QName ICFS_NAME = new QName(NS_ICF_SCHEMA, "name");
	public static final QName ICFS_UID = new QName(NS_ICF_SCHEMA, "uid");

	// OTHER (temporary? [mederly])

	public static final String ICF_CONNECTOR_EXTENSION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-extension-3";
	public static final QName ICF_CONNECTOR_USUAL_NAMESPACE_PREFIX = new QName(ICF_CONNECTOR_EXTENSION,
			"usualNamespacePrefix");

	public static final String SCRIPTING_EXTENSION_NS = "http://midpoint.evolveum.com/xml/ns/public/model/scripting/extension-3";
	public static final QName SE_EXECUTE_SCRIPT = new QName(SCRIPTING_EXTENSION_NS, "executeScript");

	public static final String NS_SCRIPTING = "http://midpoint.evolveum.com/xml/ns/public/model/scripting-3";
	public static final QName S_PIPELINE = new QName(NS_SCRIPTING, "pipeline");
	public static final QName S_SEARCH = new QName(NS_SCRIPTING, "search");
	public static final QName S_SEQUENCE = new QName(NS_SCRIPTING, "sequence");
	public static final QName S_ACTION = new QName(NS_SCRIPTING, "action");

	public static final QName C_EVENT = new QName(NS_C, "event");
	public static final QName C_EVENT_HANDLER = new QName(NS_C, "eventHandler");			// TODO: no such element in common-3 - is it OK?
	public static final QName C_TEXT_FORMATTER = new QName(NS_C, "textFormatter");

	public static final QName C_TRANSPORT_NAME = new QName(NS_C, "transportName");
	public static final QName C_FROM = new QName(NS_C, "from");
	public static final QName C_TO = new QName(NS_C, "to");
	public static final QName C_ENCODED_MESSAGE_TEXT = new QName(NS_C, "encodedMessageText");
	public static final QName C_MESSAGE = new QName(NS_C, "message");
	public static final QName C_WORK_ITEM = new QName(NS_C, "workItem");
	public static final QName C_WF_PROCESS_INSTANCE = new QName(NS_C, "wfProcessInstance");

	public static final QName APIT_ITEM_LIST = new QName(SchemaConstants.NS_API_TYPES, "itemList");
	public static final QName C_ASSIGNMENT = new QName(SchemaConstants.NS_C, "assignment");

	public static final QName C_NAME = new QName(SchemaConstants.NS_C, "name");

	public static final QName FAULT_MESSAGE_ELEMENT_NAME = new QName(NS_FAULT, "fault");
	public static final QName C_MODEL_CONTEXT = new QName(NS_C, "modelContext");
	public static final QName C_ITEM_TO_APPROVE = new QName(NS_C, "itemToApprove");
	public static final QName C_SHADOW_DISCRIMINATOR = new QName(NS_C, "shadowDiscriminator");
	
	// Lifecycle
	
	public static final String LIFECYCLE_DRAFT = "draft";
	public static final String LIFECYCLE_PROPOSED = "proposed";
	public static final String LIFECYCLE_ACTIVE = "active";
	public static final String LIFECYCLE_DEPRECATED = "deprecated";
	public static final String LIFECYCLE_ARCHIVED = "archived";
	public static final String LIFECYCLE_FAILED = "failed";

	
	// Object collections
	
	/**
	 * All objects in role catalog. It means all the objects in all the categories that are placed under the
	 * primary role catalog defined in the system. If used in a context where the role catalog can be displayed
	 * as a tree then this collection will be displayed as a tree.
	 */
	public static final QName OBJECT_COLLECTION_ROLE_CATALOG_QNAME = new QName(NS_OBJECT_COLLECTIONS, "roleCatalog");
	public static final String OBJECT_COLLECTION_ROLE_CATALOG_URI = QNameUtil.qNameToUri(OBJECT_COLLECTION_ROLE_CATALOG_QNAME);
	
	/**
	 * Collection that contains all roles.
	 */
	public static final QName OBJECT_COLLECTION_ALL_ROLES_QNAME = new QName(NS_OBJECT_COLLECTIONS, "allRoles");
	public static final String OBJECT_COLLECTION_ALL_ROLES_URI = QNameUtil.qNameToUri(OBJECT_COLLECTION_ALL_ROLES_QNAME);
	
	/**
	 * Collection that contains all orgs.
	 */
	public static final QName OBJECT_COLLECTION_ALL_ORGS_QNAME = new QName(NS_OBJECT_COLLECTIONS, "allOrgs");
	public static final String OBJECT_COLLECTION_ALL_ORGS_URI = QNameUtil.qNameToUri(OBJECT_COLLECTION_ALL_ORGS_QNAME);
	
	/**
	 * Collection that contains all services.
	 */
	public static final QName OBJECT_COLLECTION_ALL_SERVICES_QNAME = new QName(NS_OBJECT_COLLECTIONS, "allServices");
	public static final String OBJECT_COLLECTION_ALL_SERVICES_URI = QNameUtil.qNameToUri(OBJECT_COLLECTION_ALL_SERVICES_QNAME);
	
	/**
	 * Collection that contains user's assignments.
	 */
	public static final QName OBJECT_COLLECTION_USER_ASSIGNMENTS_QNAME = new QName(NS_OBJECT_COLLECTIONS, "userAssignments");
	public static final String OBJECT_COLLECTION_USER_ASSIGNMENTS_URI = QNameUtil.qNameToUri(OBJECT_COLLECTION_ALL_SERVICES_QNAME);

	// Samples

	public static final QName SAMPLES_SSN = new QName(SchemaConstants.NS_SAMPLES_EXTENSION, "ssn");
	public static final QName SAMPLES_DOMAIN = new QName(SchemaConstants.NS_SAMPLES_EXTENSION, "domain");

	// Misc
	
	public static String SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH = "localization/schema";
	
	// registration
	public static final String REGISTRATION_ID = "registrationId";
	public static final String REGISTRATION_TOKEN = "token";
	
	// resetPassword
	public static final String RESET_PASSWORD_ID = "user";
	public static final String RESET_PASSWORD_TOKEN = "token";
}
