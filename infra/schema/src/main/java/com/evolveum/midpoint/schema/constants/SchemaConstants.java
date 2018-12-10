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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_WORK_ITEM;

/**
 * @author Vilo Repan
 * @author Radovan Semancik
 */
public abstract class SchemaConstants {

	public static final String NS_MIDPOINT_PUBLIC = "http://midpoint.evolveum.com/xml/ns/public";
	public static final String NS_MIDPOINT_TEST = "http://midpoint.evolveum.com/xml/ns/test";

	public static final Map<String, String> prefixNsMap = new HashMap<>();

	// NAMESPACES

	public static final String NS_ORG = "http://midpoint.evolveum.com/xml/ns/public/common/org-3";
	public static final String PREFIX_NS_ORG = "org";
	public static final String NS_QUERY = PrismConstants.NS_QUERY;
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
	public static final String NS_CASE = "http://midpoint.evolveum.com/xml/ns/public/common/case-3";

	/**
	 * Namespace for default (bult-in) object collections, such as "all objects", "all roles", ...
	 */
	public static final String NS_OBJECT_COLLECTIONS = NS_MIDPOINT_PUBLIC + "/common/object-collections-3";

	// COMMON NAMESPACE

	public static final ItemName C_FILTER_TYPE_URI = new ItemName(NS_QUERY, "uri");
	public static final ItemName C_ITEM = new ItemName(NS_C, "item");
	public static final ItemName C_OBJECTS = new ItemName(NS_C, "objects");
	public static final ItemName C_OBJECT = new ItemName(NS_C, "object");
	public static final ItemName C_TARGET = new ItemName(NS_C, "target");
	public static final ItemName C_ABSTRACT_ROLE = new ItemName(NS_C, "abstractRole");
	public static final ItemName C_FOCUS = new ItemName(NS_C, "focus");
	public static final QName C_OBJECT_TYPE = new QName(NS_C, "ObjectType");
	public static final ItemName C_OBJECT_REF = new ItemName(NS_C, "objectRef");
	public static final ItemName C_VALUE = new ItemName(NS_C, "value");
	public static final ItemName C_PARAM_VALUE = new ItemName(NS_C, "paramValue");
	public static final ItemName C_REPORT_PARAM_VALUE = new ItemName(NS_C, "reportParamValue");
	public static final ItemName C_OID_ATTRIBUTE = new ItemName(NS_C, "oid");
	public static final QName C_USER_TYPE = new QName(NS_C, "UserType");
	public static final QName C_TASK_TYPE = new QName(NS_C, "TaskType");
	public static final ItemName C_TASK = new ItemName(NS_C, "task");
	public static final ItemName C_RESOURCE = new ItemName(NS_C, "resource");
	public static final ItemName C_RESULT = new ItemName(NS_C, "result");
	public static final ItemName C_USER = new ItemName(NS_C, "user");
	public static final ItemName C_REQUESTER = new ItemName(NS_C, "requester");
	public static final ItemName C_REQUESTEE = new ItemName(NS_C, "requestee");
	public static final ItemName C_ASSIGNEE = new ItemName(NS_C, "assignee");
	public static final ItemName C_OBJECT_TEMPLATE = new ItemName(NS_C, "objectTemplate");
	public static final ItemName C_OBJECT_TEMPLATE_REF = new ItemName(NS_C, "objectTemplateRef");
	public static final QName C_OBJECT_TEMPLATE_TYPE = new QName(NS_C, "ObjectTemplateType");
	public static final QName C_GENERIC_OBJECT_TYPE = new QName(NS_C, "GenericObjectType");
	public static final ItemName C_GENERIC_OBJECT = new ItemName(NS_C, "genericObject");
	public static final ItemName C_ACCOUNT = new ItemName(NS_C, "account");
	public static final QName C_ACCOUNT_SHADOW_TYPE = new QName(NS_C, "AccountShadowType");
	public static final QName C_RESOURCE_TYPE = new QName(NS_C, "ResourceType");
	public static final QName C_CONNECTOR_TYPE = new QName(NS_C, "ConnectorType");
	public static final ItemName C_CONNECTOR = new ItemName(NS_C, "connector");
	public static final QName C_CONNECTOR_HOST_TYPE = new QName(NS_C, "ConnectorHostType");
	public static final ItemName C_CONNECTOR_HOST = new ItemName(NS_C, "connectorHost");
	public static final ItemName C_CONNECTOR_FRAMEWORK = new ItemName(NS_C, "framework");
	public static final ItemName C_CONNECTOR_CONNECTOR_TYPE = new ItemName(NS_C, "connectorType");
	public static final ItemName C_SHADOW = new ItemName(NS_C, "shadow");
	public static final QName C_SHADOW_TYPE = new QName(NS_C, "ShadowType");
        public static final QName C_ORG_TYPE = new QName(NS_C, "OrgType");
	public static final ItemName C_ATTRIBUTES = new ItemName(NS_C, "attributes");
	public static final ItemName C_ASSOCIATION = new ItemName(NS_C, "association");
	public static final QName C_CREDENTIALS_TYPE = new QName(NS_C, "CredentialsType");
	public static final ItemName C_CREDENTIALS = new ItemName(NS_C, "credentials");
	public static final ItemName C_ACTIVATION = new ItemName(NS_C, "activation");
	public static final QName C_SYSTEM_CONFIGURATION_TYPE = new QName(NS_C, "SystemConfigurationType");
	public static final ItemName C_SYSTEM_CONFIGURATION = new ItemName(NS_C, "systemConfiguration");
	public static final ItemName C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS = new ItemName(NS_C,
			"globalAccountSynchronizationSettings");

	public static final ItemName C_REPORT = new ItemName(NS_C, "report");
	public static final ItemName C_REPORT_OUTPUT = new ItemName(NS_C, "reportOutput");
	public static final ItemName C_ITEM_PATH_FIELD = new ItemName(NS_C, "itemPathField");
	public static final QName C_ACTIVATION_STATUS_TYPE = new QName(NS_C, "ActivationStatusType");
	public static final ItemName C_SECURITY_POLICY = new ItemName(NS_C, "securityPolicy");
	public static final ItemName C_MODEL_EXECUTE_OPTIONS = new ItemName(NS_C, "modelExecuteOptions");

	public static final QName T_POLY_STRING_TYPE = new QName(SchemaConstantsGenerated.NS_TYPES,
			"PolyStringType");
	public static final ItemName T_OBJECT_DELTA = new ItemName(SchemaConstantsGenerated.NS_TYPES, "objectDelta");
	public static final QName T_OBJECT_DELTA_TYPE = new QName(SchemaConstantsGenerated.NS_TYPES,
			"ObjectDeltaType");

	/*
	 * Constants for default relations.
	 *
	 * Please DO NOT use these in production code. Use RelationKind values instead.
	 * ===========================================
	 *
	 * You can, however, freely use these in tests.
	 */

	/**
	 * Default membership relation. Used as a relation value in object references.
	 * See RelationKind.MEMBER for more details.
	 */
	public static final QName ORG_DEFAULT = new QName(NS_ORG, "default");

	/**
	 * Default 'manager' relation. Used as a relation value in object references.
	 * See RelationKind.MANAGER for more details.
	 */
	public static final QName ORG_MANAGER = new QName(NS_ORG, "manager");

	/**
	 * Default 'metarole assignment' relation. Used as a relation value in object references.
	 * See RelationKind.META for more details.
	 */
	public static final QName ORG_META = new QName(NS_ORG, "meta");

	/**
	 * Default delegation relation. Used as a relation value in object references.
	 * See RelationKind.DELEGATION for more details.
	 */
	public static final QName ORG_DEPUTY = new QName(NS_ORG, "deputy");

	/**
	 * Default 'approver' relation. Used as a relation value in object references.
	 * See RelationKind.APPROVER for more details.
	 */
	public static final QName ORG_APPROVER = new QName(NS_ORG, "approver");

	/**
	 * Default 'owner' relation. Used as a relation value in object references.
	 * See RelationKind.OWNER for more details.
	 */
	public static final QName ORG_OWNER = new QName(NS_ORG, "owner");

	/**
	 * Default 'consent' relation. Used as a relation value in object references.
	 * See RelationKind.CONSENT for more details.
	 */
	public static final QName ORG_CONSENT = new QName(NS_ORG, "consent");

	public static final ItemPath PATH_PASSWORD = ItemPath.create(C_CREDENTIALS, CredentialsType.F_PASSWORD);
	public static final ItemPath PATH_PASSWORD_VALUE = ItemPath.create(C_CREDENTIALS, CredentialsType.F_PASSWORD,
			PasswordType.F_VALUE);
	public static final ItemPath PATH_PASSWORD_FORCE_CHANGE = ItemPath.create(C_CREDENTIALS, CredentialsType.F_PASSWORD,
			PasswordType.F_FORCE_CHANGE);
	public static final ItemPath PATH_PASSWORD_METADATA = ItemPath.create(C_CREDENTIALS, CredentialsType.F_PASSWORD,
			PasswordType.F_METADATA);
	public static final ItemPath PATH_NONCE = ItemPath.create(C_CREDENTIALS, CredentialsType.F_NONCE);
	public static final ItemPath PATH_NONCE_VALUE = ItemPath.create(C_CREDENTIALS, CredentialsType.F_NONCE,
			NonceType.F_VALUE);

	public static final ItemPath PATH_SECURITY_QUESTIONS = ItemPath.create(C_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS);
	public static final ItemPath PATH_SECURITY_QUESTIONS_QUESTION_ANSWER = ItemPath.create(C_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS,
			SecurityQuestionsCredentialsType.F_QUESTION_ANSWER);
	public static final ItemPath PATH_ACTIVATION = ItemPath.create(C_ACTIVATION);
	public static final ItemPath PATH_ACTIVATION_ADMINISTRATIVE_STATUS = ItemPath.create(C_ACTIVATION,
			ActivationType.F_ADMINISTRATIVE_STATUS);
	public static final ItemPath PATH_ACTIVATION_EFFECTIVE_STATUS = ItemPath.create(C_ACTIVATION,
			ActivationType.F_EFFECTIVE_STATUS);
	public static final ItemPath PATH_ACTIVATION_VALID_FROM = ItemPath.create(C_ACTIVATION,
			ActivationType.F_VALID_FROM);
	public static final ItemPath PATH_ACTIVATION_VALID_TO = ItemPath.create(C_ACTIVATION,
			ActivationType.F_VALID_TO);
	public static final ItemPath PATH_ACTIVATION_DISABLE_REASON = ItemPath.create(ShadowType.F_ACTIVATION,
			ActivationType.F_DISABLE_REASON);
	public static final ItemPath PATH_ACTIVATION_LOCKOUT_STATUS = ItemPath.create(C_ACTIVATION,
			ActivationType.F_LOCKOUT_STATUS);
	public static final ItemPath PATH_ACTIVATION_LOCKOUT_EXPIRATION_TIMESTAMP = ItemPath.create(C_ACTIVATION,
			ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP);
	public static final ItemPath PATH_OPERATIONAL_STATE_LAST_AVAILABILITY_STATUS = ItemPath.create(
			ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS);
	public static final ItemPath PATH_ATTRIBUTES = ItemPath.create(C_ATTRIBUTES);
	public static final ItemPath PATH_ASSIGNMENT = ItemPath.create(FocusType.F_ASSIGNMENT);
	public static final ItemPath PATH_INDUCEMENT = ItemPath.create(AbstractRoleType.F_INDUCEMENT);
	public static final ItemPath PATH_ASSIGNMENT_ACTIVATION = ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION);
	public static final ItemPath PATH_ASSIGNMENT_ACTIVATION_EFFECTIVE_STATUS = ItemPath
			.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS);
	public static final ItemPath PATH_ASSIGNMENT_ACTIVATION_VALID_FROM = ItemPath
			.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM);
	public static final ItemPath PATH_ASSIGNMENT_ACTIVATION_VALID_TO = ItemPath
			.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO);
	public static final ItemPath PATH_ASSIGNMENT_TARGET_REF = ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF);
	public static final ItemPath PATH_ASSIGNMENT_DESCRIPTION = ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION);
	public static final ItemPath PATH_ASSOCIATION = ItemPath.create(C_ASSOCIATION);
	public static final ItemPath PATH_TRIGGER = ItemPath.create(ObjectType.F_TRIGGER);
	public static final ItemPath PATH_CREDENTIALS_PASSWORD_FAILED_LOGINS = ItemPath.create(
			UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_FAILED_LOGINS);
	public static final ItemPath PATH_CREDENTIALS_NONCE_FAILED_LOGINS = ItemPath.create(
			UserType.F_CREDENTIALS, CredentialsType.F_NONCE, PasswordType.F_FAILED_LOGINS);
	public static final ItemPath PATH_CREDENTIALS_SECURITY_QUESTIONS_FAILED_LOGINS = ItemPath.create(
			UserType.F_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS, PasswordType.F_FAILED_LOGINS);
	public static final ItemPath PATH_LINK_REF = ItemPath.create(FocusType.F_LINK_REF);
	public static final ItemPath PATH_LIFECYCLE_STATE = ItemPath.create(ObjectType.F_LIFECYCLE_STATE);
	public static final ItemPath PATH_ROLE_MEMBERSHIP_REF = ItemPath.create(FocusType.F_ROLE_MEMBERSHIP_REF);
	public static final ItemPath PATH_AUXILIARY_OBJECT_CLASS = ItemPath.create(ShadowType.F_AUXILIARY_OBJECT_CLASS);
	public static final ItemPath PATH_AUTOASSIGN_ENABLED = ItemPath
			.create(AbstractRoleType.F_AUTOASSIGN, AutoassignSpecificationType.F_ENABLED);
	public static final ItemPath PATH_PARENT_ORG_REF = ItemPath.create(ObjectType.F_PARENT_ORG_REF);
	public static final ItemPath PATH_METADATA_MODIFY_TIMESTAMP = ItemPath.create(ObjectType.F_METADATA, MetadataType.F_MODIFY_TIMESTAMP);

	public static final String NS_PROVISIONING = NS_MIDPOINT_PUBLIC + "/provisioning";
	public static final String NS_PROVISIONING_LIVE_SYNC = NS_PROVISIONING + "/liveSync-3";
	public static final ItemName SYNC_TOKEN = new ItemName(NS_PROVISIONING_LIVE_SYNC, "token");
	public static final String NS_PROVISIONING_TASK = NS_PROVISIONING + "/task";

	// Synchronization constants
	public static final String NS_PROVISIONING_CHANNEL = NS_PROVISIONING + "/channels-3";
	public static final QName CHANGE_CHANNEL_LIVE_SYNC = new QName(NS_PROVISIONING_CHANNEL, "liveSync");
	public static final String CHANGE_CHANNEL_LIVE_SYNC_URI = QNameUtil.qNameToUri(CHANGE_CHANNEL_LIVE_SYNC);
	public static final QName CHANGE_CHANNEL_RECON = new QName(NS_PROVISIONING_CHANNEL, "reconciliation");
	public static final String CHANGE_CHANNEL_RECON_URI = QNameUtil.qNameToUri(CHANGE_CHANNEL_RECON);
	public static final QName CHANGE_CHANNEL_RECOMPUTE = new QName(NS_PROVISIONING_CHANNEL, "recompute");
	public static final String CHANGE_CHANNEL_RECOMPUTE_URI = QNameUtil.qNameToUri(CHANGE_CHANNEL_RECOMPUTE);
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
	public static final QName CHANNEL_REMEDIATION_QNAME = new QName(NS_MODEL_CHANNEL, "remediation");
	public static final String CHANNEL_REMEDIATION_URI = QNameUtil.qNameToUri(CHANNEL_REMEDIATION_QNAME);

	public static final String NS_MODEL_SERVICE = NS_MODEL + "/service-3";

	public static final String NS_MODEL_EXTENSION = NS_MODEL + "/extension-3";
	public static final ItemName MODEL_EXTENSION_FRESHENESS_INTERVAL_PROPERTY_NAME = new ItemName(
			NS_MODEL_EXTENSION, "freshnessInterval"); // unused? TODO consider
														// removing
	public static final ItemName MODEL_EXTENSION_DRY_RUN = new ItemName(NS_MODEL_EXTENSION, "dryRun");
	public static final ItemName SYNC_TOKEN_RETRY_UNHANDLED = new ItemName(NS_MODEL_EXTENSION, "retryLiveSyncErrors");
	public static final ItemName MODEL_EXTENSION_FINISH_OPERATIONS_ONLY = new ItemName(NS_MODEL_EXTENSION, "finishOperationsOnly");
	public static final ItemName MODEL_EXTENSION_KIND = new ItemName(NS_MODEL_EXTENSION, "kind");
	public static final ItemName MODEL_EXTENSION_INTENT = new ItemName(NS_MODEL_EXTENSION, "intent");
	public static final ItemName MODEL_EXTENSION_OBJECTCLASS = new ItemName(NS_MODEL_EXTENSION, "objectclass");
	public static final ItemName MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME = new ItemName(
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
	// should not be used, because it is a transitional constraint
	public static final String MODEL_POLICY_SITUATION_MODIFIED =
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "modified"));
	public static final String MODEL_POLICY_SITUATION_ASSIGNMENT_MODIFIED = MODEL_POLICY_SITUATION_MODIFIED;        // TODO
	public static final String MODEL_POLICY_SITUATION_HAS_ASSIGNMENT =
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "hasAssignment"));        // TODO
	public static final String MODEL_POLICY_SITUATION_HAS_NO_ASSIGNMENT =
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "hasNoAssignment"));        // TODO
	public static final String MODEL_POLICY_SITUATION_OBJECT_STATE =
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "objectState"));           // TODO
	public static final String MODEL_POLICY_SITUATION_ASSIGNMENT_STATE =
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "assignmentState"));      // TODO
	public static final String MODEL_POLICY_SITUATION_TIME_VALIDITY =
			QNameUtil.qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "timeValidity"));

	public static final String NS_MODEL_APPROVAL = NS_MODEL + "/approval";
	public static final String NS_MODEL_APPROVAL_OUTCOME = NS_MODEL_APPROVAL + "/outcome";
	public static final String MODEL_APPROVAL_OUTCOME_APPROVE =
			QNameUtil.qNameToUri(new QName(NS_MODEL_APPROVAL_OUTCOME, "approve"));
	public static final String MODEL_APPROVAL_OUTCOME_REJECT =
			QNameUtil.qNameToUri(new QName(NS_MODEL_APPROVAL_OUTCOME, "reject"));
	public static final String MODEL_APPROVAL_OUTCOME_SKIP =
			QNameUtil.qNameToUri(new QName(NS_MODEL_APPROVAL_OUTCOME, "skip"));

	public static final String NS_MODEL_CERTIFICATION = NS_MODEL + "/certification";
	public static final String NS_MODEL_CERTIFICATION_OUTCOME = NS_MODEL_CERTIFICATION + "/outcome";
	public static final QName MODEL_CERTIFICATION_OUTCOME_ACCEPT_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "accept");
	public static final String MODEL_CERTIFICATION_OUTCOME_ACCEPT = QNameUtil.qNameToUri(MODEL_CERTIFICATION_OUTCOME_ACCEPT_QNAME);
	public static final QName MODEL_CERTIFICATION_OUTCOME_REVOKE_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "revoke");
	public static final String MODEL_CERTIFICATION_OUTCOME_REVOKE = QNameUtil.qNameToUri(MODEL_CERTIFICATION_OUTCOME_REVOKE_QNAME);
	public static final QName MODEL_CERTIFICATION_OUTCOME_REDUCE_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "reduce");
	public static final String MODEL_CERTIFICATION_OUTCOME_REDUCE = QNameUtil.qNameToUri(MODEL_CERTIFICATION_OUTCOME_REDUCE_QNAME);
	public static final QName MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "notDecided");
	public static final String MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED = QNameUtil.qNameToUri(MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED_QNAME);
	public static final QName MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "noResponse");
	public static final String MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE = QNameUtil.qNameToUri(MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE_QNAME);		// only for aggregated decisions

	public static final ItemName MODEL_EXTENSION_OBJECT_TYPE = new ItemName(NS_MODEL_EXTENSION, "objectType");
	public static final ItemName MODEL_EXTENSION_OBJECT_QUERY = new ItemName(NS_MODEL_EXTENSION, "objectQuery");
	public static final ItemName MODEL_EXTENSION_SEARCH_OPTIONS = new ItemName(NS_MODEL_EXTENSION, "searchOptions");
	public static final ItemName MODEL_EXTENSION_USE_REPOSITORY_DIRECTLY = new ItemName(NS_MODEL_EXTENSION, "useRepositoryDirectly");
	public static final ItemName MODEL_EXTENSION_ITERATION_METHOD = new ItemName(NS_MODEL_EXTENSION, "iterationMethod");
	public static final ItemName MODEL_EXTENSION_OBJECT_DELTA = new ItemName(NS_MODEL_EXTENSION, "objectDelta");
	public static final ItemName MODEL_EXTENSION_OBJECT_DELTAS = new ItemName(NS_MODEL_EXTENSION, "objectDeltas");
	public static final ItemName MODEL_EXTENSION_WORKER_THREADS = new ItemName(NS_MODEL_EXTENSION, "workerThreads");
	public static final ItemName MODEL_EXTENSION_OPTION_RAW = new ItemName(NS_MODEL_EXTENSION, "optionRaw");
	public static final ItemName MODEL_EXTENSION_EXECUTE_OPTIONS = new ItemName(NS_MODEL_EXTENSION, "executeOptions");

	public static final ItemName MODEL_EXTENSION_DIAGNOSE = new ItemName(NS_MODEL_EXTENSION, "diagnose");
	public static final ItemName MODEL_EXTENSION_FIX = new ItemName(NS_MODEL_EXTENSION, "fix");
	public static final ItemName MODEL_EXTENSION_DUPLICATE_SHADOWS_RESOLVER = new ItemName(NS_MODEL_EXTENSION,
			"duplicateShadowsResolver");
	public static final ItemName MODEL_EXTENSION_CHECK_DUPLICATES_ON_PRIMARY_IDENTIFIERS_ONLY = new ItemName(
			NS_MODEL_EXTENSION, "checkDuplicatesOnPrimaryIdentifiersOnly");

	public static final ItemName MODEL_EXTENSION_CLEANUP_POLICIES = new ItemName(NS_MODEL_EXTENSION,
			"cleanupPolicies");

	public static final ItemName MODEL_EXTENSION_WORK_ITEM_ID = new ItemName(NS_MODEL_EXTENSION, "workItemId");
	public static final ItemName MODEL_EXTENSION_WORK_ITEM_ACTIONS = new ItemName(NS_MODEL_EXTENSION, "workItemActions");
	public static final ItemName MODEL_EXTENSION_WORK_ITEM_ACTION = new ItemName(NS_MODEL_EXTENSION, "workItemAction");
	public static final ItemName MODEL_EXTENSION_TIME_BEFORE_ACTION = new ItemName(NS_MODEL_EXTENSION, "timeBeforeAction");

	public static final String NOOP_SCHEMA_URI = NS_MIDPOINT_PUBLIC + "/task/noop/handler-3";
	public static final ItemName NOOP_DELAY_QNAME = new ItemName(NOOP_SCHEMA_URI, "delay");
	public static final ItemName NOOP_STEPS_QNAME = new ItemName(NOOP_SCHEMA_URI, "steps");

	public static final String JDBC_PING_SCHEMA_URI = NS_MIDPOINT_PUBLIC + "/task/jdbc-ping/handler-3";
	public static final ItemName JDBC_PING_TESTS_QNAME = new ItemName(JDBC_PING_SCHEMA_URI, "tests");
	public static final ItemName JDBC_PING_INTERVAL_QNAME = new ItemName(JDBC_PING_SCHEMA_URI, "interval");
	public static final ItemName JDBC_PING_TEST_QUERY_QNAME = new ItemName(JDBC_PING_SCHEMA_URI, "testQuery");
	public static final ItemName JDBC_PING_DRIVER_CLASS_NAME_QNAME = new ItemName(JDBC_PING_SCHEMA_URI, "driverClassName");
	public static final ItemName JDBC_PING_JDBC_URL_QNAME = new ItemName(JDBC_PING_SCHEMA_URI, "jdbcUrl");
	public static final ItemName JDBC_PING_JDBC_USERNAME_QNAME = new ItemName(JDBC_PING_SCHEMA_URI, "jdbcUsername");
	public static final ItemName JDBC_PING_JDBC_PASSWORD_QNAME = new ItemName(JDBC_PING_SCHEMA_URI, "jdbcPassword");
	public static final ItemName JDBC_PING_LOG_ON_INFO_LEVEL_QNAME = new ItemName(JDBC_PING_SCHEMA_URI, "logOnInfoLevel");

	public static final String NS_GUI = NS_MIDPOINT_PUBLIC + "/gui";
	public static final String NS_GUI_CHANNEL = NS_GUI + "/channels-3";
	
	// Init channel, used when system is initializing itself
	public static final QName CHANNEL_GUI_INIT_QNAME = new QName(NS_GUI_CHANNEL, "init");
	public static final String CHANNEL_GUI_INIT_URI = QNameUtil.qNameToUri(CHANNEL_GUI_INIT_QNAME);
	
	public static final QName CHANNEL_GUI_SELF_REGISTRATION_QNAME = new QName(NS_GUI_CHANNEL, "selfRegistration");
	public static final String CHANNEL_GUI_SELF_REGISTRATION_URI = QNameUtil.qNameToUri(CHANNEL_GUI_SELF_REGISTRATION_QNAME);
	
	// Channel for self-service part of the user interface. These are the pages when user is changing his own data.
	// E.g. update of his own profile and password change are considered to be self-service.
	public static final QName CHANNEL_GUI_SELF_SERVICE_QNAME = new QName(NS_GUI_CHANNEL, "selfService");
	public static final String CHANNEL_GUI_SELF_SERVICE_URI = QNameUtil.qNameToUri(CHANNEL_GUI_SELF_SERVICE_QNAME);
	
	// Password reset channel. This is *reset*, which means that the user does not know the old password and cannot log in.
	public static final QName CHANNEL_GUI_RESET_PASSWORD_QNAME = new QName(NS_GUI_CHANNEL, "resetPassword");
	public static final String CHANNEL_GUI_RESET_PASSWORD_URI = QNameUtil.qNameToUri(CHANNEL_GUI_RESET_PASSWORD_QNAME);
	
	// Catch-all channel for all user operations in user interface.
	public static final QName CHANNEL_GUI_USER_QNAME = new QName(NS_GUI_CHANNEL, "user");
	public static final String CHANNEL_GUI_USER_URI = QNameUtil.qNameToUri(CHANNEL_GUI_USER_QNAME);

	//GUI constants which are also used in the notificators
	public static final String REGISTRATION_CONFIRAMTION_PREFIX = "/confirm/registration";
	public static final String PASSWORD_RESET_CONFIRMATION_PREFIX = "/confirm/reset";
	public static final String ACCOUNT_ACTIVATION_PREFIX = "/activate/accounts";

	public static final String INTENT_DEFAULT = "default";
	public static final String INTENT_UNKNOWN = "unknown";

	public static final String CONNECTOR_SCHEMA_CONFIGURATION_TYPE_LOCAL_NAME = "ConfigurationType";

	// This constant should not be here. It is used by schema processor to
	// supply correct import. But the dependency should
	// be inverted, eventually (MID-356)
	public static final String ICF_FRAMEWORK_URI = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1";
	public static final String NS_ICF_CONFIGURATION = ICF_FRAMEWORK_URI + "/connector-schema-3";
	public static final ItemName ICF_CONFIGURATION_PROPERTIES = new ItemName(NS_ICF_CONFIGURATION,
			"configurationProperties");
	public static final ItemName ICF_TIMEOUTS = new ItemName(NS_ICF_CONFIGURATION, "timeouts");
	public static final ItemName ICF_RESULTS_HANDLER_CONFIGURATION = new ItemName(NS_ICF_CONFIGURATION,
			"resultsHandlerConfiguration");
	public static final ItemName ICF_CONNECTOR_POOL_CONFIGURATION = new ItemName(NS_ICF_CONFIGURATION,
			"connectorPoolConfiguration");

	// Note! This is also specified in SchemaConstants (MID-356)
	public static final String NS_ICF_SCHEMA = ICF_FRAMEWORK_URI + "/resource-schema-3";
	public static final String NS_ICF_SCHEMA_PREFIX = "icfs";
	public static final ItemName ICFS_NAME = new ItemName(NS_ICF_SCHEMA, "name");
	public static final ItemName ICFS_UID = new ItemName(NS_ICF_SCHEMA, "uid");
	public static final String CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME = "configurationProperties";
	public static final ItemName CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME = new ItemName(NS_ICF_CONFIGURATION,
			CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME);
	public static final String ACCOUNT_OBJECT_CLASS_LOCAL_NAME = "AccountObjectClass";
	public static final String GROUP_OBJECT_CLASS_LOCAL_NAME = "GroupObjectClass";

	public static final String UCF_FRAMEWORK_URI_BUILTIN = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1";

	// OTHER (temporary? [mederly])

	public static final String ICF_CONNECTOR_EXTENSION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-extension-3";
	public static final ItemName ICF_CONNECTOR_USUAL_NAMESPACE_PREFIX = new ItemName(ICF_CONNECTOR_EXTENSION,
			"usualNamespacePrefix");

	public static final String SCRIPTING_EXTENSION_NS = "http://midpoint.evolveum.com/xml/ns/public/model/scripting/extension-3";
	public static final ItemName SE_EXECUTE_SCRIPT = new ItemName(SCRIPTING_EXTENSION_NS, "executeScript");

	public static final String NS_SCRIPTING = "http://midpoint.evolveum.com/xml/ns/public/model/scripting-3";
	public static final ItemName S_PIPELINE = new ItemName(NS_SCRIPTING, "pipeline");
	public static final ItemName S_SEARCH = new ItemName(NS_SCRIPTING, "search");
	public static final ItemName S_SEQUENCE = new ItemName(NS_SCRIPTING, "sequence");
	public static final ItemName S_ACTION = new ItemName(NS_SCRIPTING, "action");

	public static final ItemName S_PIPELINE_DATA = new ItemName(NS_SCRIPTING, "pipelineData");

	public static final ItemName C_EVENT = new ItemName(NS_C, "event");
	public static final ItemName C_EVENT_HANDLER = new ItemName(NS_C, "eventHandler");			// TODO: no such element in common-3 - is it OK?
	public static final ItemName C_TEXT_FORMATTER = new ItemName(NS_C, "textFormatter");
	public static final ItemName C_NOTIFICATION_FUNCTIONS = new ItemName(NS_C, "notificationFunctions");

	public static final ItemName C_TRANSPORT_NAME = new ItemName(NS_C, "transportName");
	public static final ItemName C_FROM = new ItemName(NS_C, "from");
	public static final ItemName C_ENCODED_FROM = new ItemName(NS_C, "encodedFrom");
	public static final ItemName C_TO = new ItemName(NS_C, "to");
	public static final ItemName C_TO_LIST = new ItemName(NS_C, "toList");
	public static final ItemName C_ENCODED_TO = new ItemName(NS_C, "encodedTo");
	public static final ItemName C_ENCODED_TO_LIST = new ItemName(NS_C, "encodedToList");
	public static final ItemName C_MESSAGE_TEXT = new ItemName(NS_C, "messageText");
	public static final ItemName C_ENCODED_MESSAGE_TEXT = new ItemName(NS_C, "encodedMessageText");
	public static final ItemName C_MESSAGE = new ItemName(NS_C, "message");
	public static final ItemName C_WORK_ITEM = new ItemName(NS_C, "workItem");
	public static final ItemName C_WF_PROCESS_INSTANCE = new ItemName(NS_C, "wfProcessInstance");

	public static final ItemName APIT_ITEM_LIST = new ItemName(SchemaConstants.NS_API_TYPES, "itemList");
	public static final ItemName C_ASSIGNMENT = new ItemName(SchemaConstants.NS_C, "assignment");

	public static final ItemName C_NAME = new ItemName(SchemaConstants.NS_C, "name");

	public static final ItemName FAULT_MESSAGE_ELEMENT_NAME = new ItemName(NS_FAULT, "fault");
	public static final ItemName C_MODEL_CONTEXT = new ItemName(NS_C, "modelContext");
	public static final ItemName C_ITEM_TO_APPROVE = new ItemName(NS_C, "itemToApprove");
	public static final ItemName C_SHADOW_DISCRIMINATOR = new ItemName(NS_C, "shadowDiscriminator");

	// Lifecycle

	public static final String LIFECYCLE_DRAFT = "draft";
	public static final String LIFECYCLE_PROPOSED = "proposed";
	public static final String LIFECYCLE_ACTIVE = "active";
	public static final String LIFECYCLE_DEPRECATED = "deprecated";
	public static final String LIFECYCLE_ARCHIVED = "archived";
	public static final String LIFECYCLE_FAILED = "failed";

	// Case: generic reusable case states

	public static final String CASE_STATE_OPEN = "open";
	public static final QName CASE_STATE_OPEN_QNAME = new QName(NS_CASE, CASE_STATE_OPEN);
	public static final String CASE_STATE_CLOSED = "closed";
	public static final QName CASE_STATE_CLOSED_QNAME = new QName(NS_CASE, CASE_STATE_CLOSED);

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
	public static final String OBJECT_COLLECTION_USER_ASSIGNMENTS_URI = QNameUtil.qNameToUri(OBJECT_COLLECTION_USER_ASSIGNMENTS_QNAME);

	// Samples

	public static final QName SAMPLES_SSN = new QName(SchemaConstants.NS_SAMPLES_EXTENSION, "ssn");
	public static final QName SAMPLES_DOMAIN = new QName(SchemaConstants.NS_SAMPLES_EXTENSION, "domain");
	public static final ItemPath PATH_WORKFLOW_CONTEXT_WORK_ITEM = ItemPath.create(F_WORKFLOW_CONTEXT, F_WORK_ITEM);

	// Misc

	public static String BUNDLE_NAME = "schema";
	public static String SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH = "localization/" + BUNDLE_NAME;     // Do not load this bundle explicitly, see MID-4800
	public static final QName APPROVAL_LEVEL_OUTCOME_TYPE_COMPLEX_TYPE = new QName(SchemaConstants.NS_C, ApprovalLevelOutcomeType.class.getSimpleName());

	// registration
	public static final String USER_ID = "user";
	public static final String TOKEN = "token";

	public static final String OBJECT_TYPE_KEY_PREFIX = "ObjectType.";
	public static final String OBJECT_TYPE_LOWERCASE_KEY_PREFIX = "ObjectTypeLowercase.";
	public static final String DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX = "DefaultPolicyConstraint.";
	public static final String DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX = "DefaultPolicyConstraint.Short.";
	public static final String POLICY_CONSTRAINT_KEY_PREFIX = "PolicyConstraint.";
	public static final String POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX = "PolicyConstraint.Short.";
	public static final String POLICY_CONSTRAINTS_BEFORE_KEY = "PolicyConstraints.before";
	public static final String POLICY_CONSTRAINTS_AFTER_KEY = "PolicyConstraints.after";
	public static final String TECHNICAL_OBJECT_SPECIFICATION_KEY = "TechnicalObjectSpecification";
	public static final String OBJECT_SPECIFICATION_KEY = "ObjectSpecification";
	public static final String OBJECT_SPECIFICATION_WITH_PATH_KEY = "ObjectSpecificationWithPath";
	public static final String POLICY_VIOLATION_EXCEPTION_AGGREGATE_KEY = "PolicyViolationException.message.aggregate";

	public static final String RELATION_NAME_KEY_PREFIX = "relation.";

	//	// resetPassword
//	public static final String RESET_PASSWORD_ID = "user";
//	public static final String RESET_PASSWORD_TOKEN = "token";

	// a bit of hack
	public static final String COMPLETED_TASK_CLEANUP_TRIGGER_HANDLER_URI = SchemaConstants.NS_MODEL + "/completedTaskCleanup/handler-3";

	// other (temp)
	public static final ItemPath PATH_CREDENTIALS_PASSWORD_VALUE = ItemPath
			.create(SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);

	public static final ItemPath PATH_CREDENTIALS_PASSWORD = ItemPath
			.create(SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD);

	public static final ItemPath PATH_MODEL_EXTENSION_OBJECT_QUERY = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
	public static final ItemPath PATH_MODEL_EXTENSION_OBJECT_TYPE = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
	public static final ItemPath PATH_MODEL_EXTENSION_OBJECT_DELTA = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
	public static final ItemPath PATH_MODEL_EXTENSION_EXECUTE_OPTIONS = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
	public static final ItemPath PATH_MODEL_EXTENSION_DRY_RUN = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_DRY_RUN);
	public static final ItemPath PATH_CREDENTIALS_PASSWORD_METADATA = ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA);
	public static final ItemPath PATH_CREDENTIALS_PASSWORD_HISTORY_ENTRY = ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_HISTORY_ENTRY);
	public static final ItemPath PATH_CREDENTIALS_PASSWORD_VALUE_POLICY_REF = ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_VALUE_POLICY_REF);
	public static final ItemPath PATH_CREDENTIALS_PASSWORD_HISTORY_LENGTH = ItemPath.create(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_HISTORY_LENGTH);
	public static final ItemPath PATH_ASSIGNMENT_CONSTRUCTION_KIND = ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_KIND);
	public static final ItemPath PATH_ASSIGNMENT_CONSTRUCTION_INTENT = ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT);
	public static final ItemPath PATH_ACTIVATION_ENABLE_TIMESTAMP = ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP);
	public static final ItemPath PATH_ACTIVATION_DISABLE_TIMESTAMP = ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP);
	public static final ItemPath PATH_ACTIVATION_ARCHIVE_TIMESTAMP = ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP);

	public static final ItemPath PATH_PARENT = ItemPath.create(PrismConstants.T_PARENT);
	public static final ItemPath PATH_OBJECT_REFERENCE = ItemPath.create(PrismConstants.T_OBJECT_REFERENCE);
}
