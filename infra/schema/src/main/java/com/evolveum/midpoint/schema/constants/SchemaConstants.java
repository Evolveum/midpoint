/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.constants;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import static com.evolveum.midpoint.util.QNameUtil.qNameToUri;

/**
 * @author Vilo Repan
 * @author Radovan Semancik
 */
@SuppressWarnings("WeakerAccess")
public abstract class SchemaConstants {

    public static final String NS_MIDPOINT_PUBLIC = "http://midpoint.evolveum.com/xml/ns/public";
    public static final String NS_MIDPOINT_PUBLIC_COMMON = NS_MIDPOINT_PUBLIC + "/common";
    public static final String NS_MIDPOINT_TEST = "http://midpoint.evolveum.com/xml/ns/test";

    // NAMESPACES

    public static final String NS_ORG = "http://midpoint.evolveum.com/xml/ns/public/common/org-3";
    public static final String NS_NORMALIZED_DATA = "http://midpoint.evolveum.com/xml/ns/public/common/normalized-data-3";
    public static final String PREFIX_NS_ORG = "org";
    public static final String NS_QUERY = PrismConstants.NS_QUERY;
    public static final String NS_TYPES = PrismConstants.NS_TYPES;
    public static final String NS_MIDPOINT_PUBLIC_PREFIX = "http://midpoint.evolveum.com/xml/ns/public/";
    public static final String NS_C = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
    public static final String NS_CAPABILITIES = "http://midpoint.evolveum.com/xml/ns/public/resource/capabilities-3";
    public static final String NS_CASE = "http://midpoint.evolveum.com/xml/ns/public/common/case-3";
    public static final String NS_PRISM_TYPES = "http://prism.evolveum.com/xml/ns/public/types-3";

    /**
     * Namespace for default (built-in) object collections, such as "all objects", "all roles", ...
     */
    public static final String NS_OBJECT_COLLECTIONS = NS_MIDPOINT_PUBLIC_COMMON + "/object-collections-3";

    // COMMON NAMESPACE

    public static final ItemName C_OBJECTS = new ItemName(NS_C, "objects");
    public static final ItemName C_OBJECT = new ItemName(NS_C, "object");
    public static final ItemName C_TARGET = new ItemName(NS_C, "target");
    public static final ItemName C_ABSTRACT_ROLE = new ItemName(NS_C, "abstractRole");
    public static final ItemName C_FOCUS = new ItemName(NS_C, "focus");
    public static final QName C_OBJECT_TYPE = new QName(NS_C, "ObjectType");
    public static final ItemName C_VALUE = new ItemName(NS_C, "value");
    public static final ItemName C_PARAM_VALUE = new ItemName(NS_C, "paramValue");
    public static final ItemName C_OID_ATTRIBUTE = new ItemName(NS_C, "oid");
    public static final QName C_USER_TYPE = new QName(NS_C, "UserType");
    public static final QName C_TASK_TYPE = new QName(NS_C, "TaskType");
    public static final ItemName C_TASK = new ItemName(NS_C, "task");
    public static final ItemName C_RESOURCE = new ItemName(NS_C, "resource");
    public static final ItemName C_RESULT = new ItemName(NS_C, "result");
    public static final ItemName C_USER = new ItemName(NS_C, "user");
    public static final ItemName C_OBJECT_TEMPLATE = new ItemName(NS_C, "objectTemplate");
    public static final QName C_OBJECT_TEMPLATE_TYPE = new QName(NS_C, "ObjectTemplateType");
    public static final QName C_GENERIC_OBJECT_TYPE = new QName(NS_C, "GenericObjectType");
    public static final ItemName C_GENERIC_OBJECT = new ItemName(NS_C, "genericObject");
    public static final ItemName C_ACCOUNT = new ItemName(NS_C, "account");
    public static final QName C_RESOURCE_TYPE = new QName(NS_C, "ResourceType");
    public static final QName C_CONNECTOR_TYPE = new QName(NS_C, "ConnectorType");
    public static final ItemName C_CONNECTOR = new ItemName(NS_C, "connector");
    public static final QName C_CONNECTOR_DEVELOPMENT_TYPE = new QName(NS_C, "ConnectorDevelopmentType");
    public static final ItemName C_CONNECTOR_DEVELOPMENT = new ItemName(NS_C, "connectorDevelopment");

    public static final QName C_CONNECTOR_HOST_TYPE = new QName(NS_C, "ConnectorHostType");
    public static final ItemName C_CONNECTOR_HOST = new ItemName(NS_C, "connectorHost");
    public static final ItemName C_ROLE_ANALYSIS_CLUSTER_TYPE = new ItemName(NS_C, "RoleAnalysisClusterType");
    public static final ItemName C_ROLE_ANALYSIS_CLUSTER = new ItemName(NS_C, "roleAnalysisCluster");
    public static final ItemName C_ROLE_ANALYSIS_SESSION_TYPE = new ItemName(NS_C, "RoleAnalysisSessionType");
    public static final ItemName C_ROLE_ANALYSIS_SESSION = new ItemName(NS_C, "roleAnalysisSession");
    public static final ItemName C_ROLE_ANALYSIS_OUTLIER = new ItemName(NS_C, "roleAnalysisOutlier");
    public static final ItemName C_ROLE_ANALYSIS_OUTLIER_TYPE = new ItemName(NS_C, "RoleAnalysisOutlierType");
    public static final ItemName C_CONNECTOR_FRAMEWORK = new ItemName(NS_C, "framework");
    public static final ItemName C_CONNECTOR_CONNECTOR_TYPE = new ItemName(NS_C, "connectorType");
    public static final ItemName C_SHADOW = new ItemName(NS_C, "shadow");
    public static final QName C_SHADOW_TYPE = new QName(NS_C, "ShadowType");
    public static final ItemName C_ATTRIBUTES = new ItemName(NS_C, "attributes");
    public static final ItemName C_CREDENTIALS = new ItemName(NS_C, "credentials");
    public static final ItemName C_ACTIVATION = new ItemName(NS_C, "activation");
    public static final QName C_SYSTEM_CONFIGURATION_TYPE = new QName(NS_C, "SystemConfigurationType");
    public static final ItemName C_SYSTEM_CONFIGURATION = new ItemName(NS_C, "systemConfiguration");
    public static final ItemName C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS = new ItemName(NS_C,
            "globalAccountSynchronizationSettings");

    public static final ItemName C_REPORT = new ItemName(NS_C, "report");
    public static final ItemName C_REPORT_DATA = new ItemName(NS_C, "reportData");
    public static final QName C_ACTIVATION_STATUS_TYPE = new QName(NS_C, "ActivationStatusType");
    public static final ItemName C_SECURITY_POLICY = new ItemName(NS_C, "securityPolicy");
    public static final ItemName C_MODEL_EXECUTE_OPTIONS = new ItemName(NS_C, "modelExecuteOptions");
    public static final ItemName C_CONFLICT_RESOLUTION_ACTION_TYPE = new ItemName(NS_C, "ConflictResolutionActionType");
    public final static ItemName F_EXECUTION_OPTIONS = new ItemName(NS_C, "executionOptions");

    public static final QName T_POLY_STRING_TYPE = new QName(SchemaConstantsGenerated.NS_TYPES,
            "PolyStringType");
    public static final ItemName T_OBJECT_DELTA = new ItemName(SchemaConstantsGenerated.NS_TYPES, "objectDelta");

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

    /**
     * Default 'related' relation. Used as a relation value in object references.
     * See RelationKind.RELATED for more details.
     */
    public static final QName ORG_RELATED = new QName(NS_ORG, "related");

    public static final ItemPath PATH_PASSWORD = ItemPath.create(C_CREDENTIALS, CredentialsType.F_PASSWORD);
    public static final ItemPath PATH_PASSWORD_VALUE = ItemPath.create(C_CREDENTIALS, CredentialsType.F_PASSWORD,
            PasswordType.F_VALUE);
    public static final ItemPath PATH_PASSWORD_FORCE_CHANGE = ItemPath.create(C_CREDENTIALS, CredentialsType.F_PASSWORD,
            PasswordType.F_FORCE_CHANGE);
    public static final ItemPath PATH_PASSWORD_METADATA =
            ItemPath.create(C_CREDENTIALS, CredentialsType.F_PASSWORD, InfraItemName.METADATA);
    public static final ItemPath PATH_NONCE = ItemPath.create(C_CREDENTIALS, CredentialsType.F_NONCE);
    public static final ItemPath PATH_NONCE_VALUE = ItemPath.create(C_CREDENTIALS, CredentialsType.F_NONCE,
            NonceType.F_VALUE);

    public static final ItemPath PATH_SECURITY_QUESTIONS = ItemPath.create(C_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS);
    public static final ItemPath PATH_SECURITY_QUESTIONS_QUESTION_ANSWER =
            ItemPath.create(C_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS, SecurityQuestionsCredentialsType.F_QUESTION_ANSWER);
    public static final ItemPath PATH_ACTIVATION = C_ACTIVATION;
    public static final ItemPath PATH_ACTIVATION_ADMINISTRATIVE_STATUS =
            ItemPath.create(C_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
    public static final ItemPath PATH_ACTIVATION_EFFECTIVE_STATUS =
            ItemPath.create(C_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS);
    public static final ItemPath PATH_ACTIVATION_VALID_FROM = ItemPath.create(C_ACTIVATION, ActivationType.F_VALID_FROM);
    public static final ItemPath PATH_ACTIVATION_VALID_TO = ItemPath.create(C_ACTIVATION, ActivationType.F_VALID_TO);
    public static final ItemPath PATH_ACTIVATION_EXISTENCE = ItemPath.create(C_ACTIVATION, "existence");
    public static final ItemPath PATH_ACTIVATION_DISABLE_REASON =
            ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_DISABLE_REASON);
    public static final ItemPath PATH_ACTIVATION_LOCKOUT_STATUS = ItemPath.create(C_ACTIVATION, ActivationType.F_LOCKOUT_STATUS);
    public static final ItemPath PATH_BEHAVIOUR_LAST_LOGIN_TIMESTAMP =
            ItemPath.create(ShadowType.F_BEHAVIOR, ShadowBehaviorType.F_LAST_LOGIN_TIMESTAMP);
    public static final ItemPath PATH_OPERATIONAL_STATE_LAST_AVAILABILITY_STATUS =
            ItemPath.create(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS);
    public static final ItemPath PATH_INDUCEMENT_POLICY_RULE = ItemPath.create(RoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE);

    public static final ItemPath PATH_ASSIGNMENT_ACTIVATION_EFFECTIVE_STATUS =
            ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS);
    public static final ItemPath PATH_ASSIGNMENT_ACTIVATION_VALID_FROM =
            ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM);
    public static final ItemPath PATH_ASSIGNMENT_ACTIVATION_VALID_TO =
            ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO);
    public static final ItemPath PATH_ASSIGNMENT_TARGET_REF = ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF);
    public static final ItemPath PATH_ASSIGNMENT_DESCRIPTION = ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION);
    public static final ItemPath PATH_TRIGGER = ItemPath.create(ObjectType.F_TRIGGER);
    public static final ItemPath PATH_AUTHENTICATION_BEHAVIOR_FAILED_LOGINS = ItemPath.create(FocusType.F_BEHAVIOR,
            BehaviorType.F_AUTHENTICATION, AuthenticationBehavioralDataType.F_FAILED_LOGINS);
    public static final ItemPath PATH_CREDENTIALS_PASSWORD_FAILED_LOGINS = ItemPath.create(
            UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_FAILED_LOGINS);
    public static final ItemPath PATH_CREDENTIALS_NONCE_FAILED_LOGINS = ItemPath.create(
            UserType.F_CREDENTIALS, CredentialsType.F_NONCE, PasswordType.F_FAILED_LOGINS);
    public static final ItemPath PATH_CREDENTIALS_SECURITY_QUESTIONS_FAILED_LOGINS = ItemPath.create(
            UserType.F_CREDENTIALS, CredentialsType.F_SECURITY_QUESTIONS, PasswordType.F_FAILED_LOGINS);
    public static final ItemPath PATH_LINK_REF = ItemPath.create(FocusType.F_LINK_REF);
    public static final ItemPath PATH_ROLE_MEMBERSHIP_REF = ItemPath.create(FocusType.F_ROLE_MEMBERSHIP_REF);
    public static final ItemPath PATH_AUTOASSIGN_ENABLED = ItemPath
            .create(AbstractRoleType.F_AUTOASSIGN, AutoassignSpecificationType.F_ENABLED);
    public static final ItemPath PATH_PARENT_ORG_REF = ItemPath.create(ObjectType.F_PARENT_ORG_REF);

    public static final ItemPath PATH_METADATA_LAST_PROVISIONING_TIMESTAMP_NAMES_ONLY =
            ItemPath.create(InfraItemName.METADATA, ValueMetadataType.F_PROVISIONING, ProvisioningMetadataType.F_LAST_PROVISIONING_TIMESTAMP);

    public static final String NS_PROVISIONING = NS_MIDPOINT_PUBLIC + "/provisioning";
    public static final String NS_PROVISIONING_LIVE_SYNC = NS_PROVISIONING + "/liveSync-3";
    public static final ItemName SYNC_TOKEN = new ItemName(NS_PROVISIONING_LIVE_SYNC, "token");
    public static final String NS_PROVISIONING_TASK = NS_PROVISIONING + "/task";

    public static final String NS_CHANNEL = NS_MIDPOINT_PUBLIC_COMMON + "/channels-3";
    public static final String NS_CHANNEL_PLUS_HASH = NS_CHANNEL + "#";

    public static final String CHANNEL_LIVE_SYNC_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#liveSync";
    public static final String CHANNEL_RECON_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#reconciliation";
    public static final String CHANNEL_RECOMPUTE_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#recompute";
    public static final String CHANNEL_DISCOVERY_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#discovery";
    public static final String CHANNEL_WEB_SERVICE_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#webService";
    public static final String CHANNEL_OBJECT_IMPORT_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#objectImport";
    public static final String CHANNEL_REST_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#rest";
    public static final String CHANNEL_INIT_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#init";
    public static final String CHANNEL_USER_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#user";
    public static final String CHANNEL_SELF_REGISTRATION_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#selfRegistration";
    public static final String CHANNEL_RESET_PASSWORD_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/gui/channels-3#resetPassword";
    public static final String CHANNEL_IMPORT_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#import";
    public static final String CHANNEL_ASYNC_UPDATE_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#asyncUpdate";
    public static final String CHANNEL_REMEDIATION_LEGACY_URI = "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#remediation";

    // Synchronization constants
    public static final QName CHANNEL_LIVE_SYNC = new QName(NS_CHANNEL, "liveSync");
    public static final String CHANNEL_LIVE_SYNC_URI = qNameToUri(CHANNEL_LIVE_SYNC);
    public static final QName CHANNEL_ASYNC_UPDATE = new QName(NS_CHANNEL, "asyncUpdate");
    public static final String CHANNEL_ASYNC_UPDATE_URI = qNameToUri(CHANNEL_ASYNC_UPDATE);
    public static final QName CHANNEL_RECON = new QName(NS_CHANNEL, "reconciliation");
    public static final String CHANNEL_RECON_URI = qNameToUri(CHANNEL_RECON);
    public static final QName CHANNEL_RECOMPUTE = new QName(NS_CHANNEL, "recompute");
    public static final String CHANNEL_RECOMPUTE_URI = qNameToUri(CHANNEL_RECOMPUTE);
    public static final QName CHANNEL_DISCOVERY = new QName(NS_CHANNEL, "discovery");
    public static final String CHANNEL_DISCOVERY_URI = qNameToUri(CHANNEL_DISCOVERY);
    public static final QName CHANNEL_IMPORT = new QName(NS_CHANNEL, "import");
    public static final String CHANNEL_IMPORT_URI = qNameToUri(CHANNEL_IMPORT);
    public static final QName CHANNEL_SHADOW_RECLASSIFICATION= new QName(NS_CHANNEL, "shadowReclassification");
    public static final String CHANNEL_SHADOW_RECLASSIFICATION_URI = qNameToUri(CHANNEL_SHADOW_RECLASSIFICATION);
    public static final QName CHANGE_CHANNEL_DEL_NOT_UPDATED_SHADOWS = new QName(NS_CHANNEL, "delNotUpdatedShadows");
    public static final String CHANGE_CHANNEL_DEL_NOT_UPDATED_SHADOWS_LEGACY_URI = qNameToUri(CHANGE_CHANNEL_DEL_NOT_UPDATED_SHADOWS);
    public static final QName CHANNEL_CLEANUP = new QName(NS_CHANNEL, "cleanup");
    public static final String CHANNEL_CLEANUP_URI = qNameToUri(CHANNEL_CLEANUP);

    public static final String NS_MODEL = NS_MIDPOINT_PUBLIC + "/model";

    public static final String NS_REPORT = NS_MIDPOINT_PUBLIC + "/report";
    public static final String NS_REPORT_EXTENSION = NS_REPORT + "/extension-3";
    /**
     * Must not be the same namespace as {@link #NS_REPORT_EXTENSION}, because the parameters are _not_ defined in the schema.
     * The parser would then fail on them, as the schema is not present, but these definitions would not be.
     * So they must be in a separate schema, which has no XSD. Then the parser is OK with that.
     */
    public static final String NS_REPORT_PARAM_EXTENSION = NS_REPORT_EXTENSION + "/reportParam";
    public static final String NS_CERTIFICATION = NS_MIDPOINT_PUBLIC + "/certification";
    public static final String NS_WORKFLOW = NS_MIDPOINT_PUBLIC + "/workflow"; // TODO change to "case" or "cases"

    public static final QName CHANNEL_WEB_SERVICE_QNAME = new QName(NS_CHANNEL, "webService");
    public static final String CHANNEL_WEB_SERVICE_URI = qNameToUri(CHANNEL_WEB_SERVICE_QNAME);
    public static final QName CHANNEL_OBJECT_IMPORT_QNAME = new QName(NS_CHANNEL, "objectImport");
    public static final String CHANNEL_OBJECT_IMPORT_URI = qNameToUri(CHANNEL_OBJECT_IMPORT_QNAME);
    public static final String CHANNEL_REST_LOCAL = "rest";
    public static final QName CHANNEL_REST_QNAME = new QName(NS_CHANNEL, CHANNEL_REST_LOCAL);
    public static final String CHANNEL_REST_URI = qNameToUri(CHANNEL_REST_QNAME);
    public static final QName CHANNEL_ACTUATOR_QNAME = new QName(NS_CHANNEL, "actuator");
    public static final String CHANNEL_ACTUATOR_URI = qNameToUri(CHANNEL_ACTUATOR_QNAME);
    public static final QName CHANNEL_REMEDIATION_QNAME = new QName(NS_CHANNEL, "remediation");
    public static final String CHANNEL_REMEDIATION_URI = qNameToUri(CHANNEL_REMEDIATION_QNAME);

    public static final String NS_MODEL_DISABLE_REASON = NS_MODEL + "/disableReason";
    public static final String MODEL_DISABLE_REASON_EXPLICIT =
            qNameToUri(new QName(NS_MODEL_DISABLE_REASON, "explicit"));
    public static final String MODEL_DISABLE_REASON_DEPROVISION =
            qNameToUri(new QName(NS_MODEL_DISABLE_REASON, "deprovision"));
    public static final String MODEL_DISABLE_REASON_MAPPED =
            qNameToUri(new QName(NS_MODEL_DISABLE_REASON, "mapped"));

    public enum ModelDisableReason {

        EXPLICIT(MODEL_DISABLE_REASON_EXPLICIT),
        DEPROVISION(MODEL_DISABLE_REASON_DEPROVISION),
        MAPPED(MODEL_DISABLE_REASON_MAPPED);

        public final String uri;

        ModelDisableReason(String uri) {
            this.uri = uri;
        }

        public static ModelDisableReason fromUri(String uri) {
            return fromUri(uri, false);
        }

        public static ModelDisableReason fromUriRelaxed(String uri) {
            return fromUri(uri, true);
        }

        public static ModelDisableReason fromUri(String uri, boolean relaxed) {
            if (uri == null) {
                return null;
            }

            for (ModelDisableReason value : values()) {
                if (value.uri.equals(uri)) {
                    return value;
                }
            }

            if (relaxed){
                return null;
            }

            throw new IllegalArgumentException("Unknown disable reason URI: " + uri);
        }
    }

    public static final String NS_MODEL_POLICY = NS_MODEL + "/policy";
    public static final String NS_MODEL_POLICY_SITUATION = NS_MODEL_POLICY + "/situation";
    public static final String MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "exclusionViolation"));
    public static final String MODEL_POLICY_SITUATION_UNDERASSIGNED =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "underassigned"));
    public static final String MODEL_POLICY_SITUATION_OVERASSIGNED =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "overassigned"));
    // should not be used, because it is a transitional constraint
    public static final String MODEL_POLICY_SITUATION_MODIFIED =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "modified"));
    public static final String MODEL_POLICY_SITUATION_ASSIGNMENT_MODIFIED = MODEL_POLICY_SITUATION_MODIFIED;
    public static final String MODEL_POLICY_SITUATION_HAS_ASSIGNMENT =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "hasAssignment"));
    public static final String MODEL_POLICY_SITUATION_HAS_NO_ASSIGNMENT =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "hasNoAssignment"));
    public static final String MODEL_POLICY_SITUATION_OBJECT_STATE =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "objectState"));
    public static final String MODEL_POLICY_SITUATION_ASSIGNMENT_STATE =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "assignmentState"));
    public static final String MODEL_POLICY_SITUATION_TIME_VALIDITY =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "timeValidity"));

    // TODO decide on the final form of the following (e.g. namespace = model? provisioning? something else?)
    @Experimental public static final String MODEL_POLICY_SITUATION_PROTECTED_SHADOW =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "protectedShadow"));
    @Experimental public static final String MODEL_POLICY_SITUATION_INVALID_DATA =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "invalidData"));

    /**
     * An orphaned task is such that has a parent but that parent does not exist.
     */
    public static final String MODEL_POLICY_SITUATION_ORPHANED =
            qNameToUri(new QName(NS_MODEL_POLICY_SITUATION, "orphaned"));

    public static final String NS_MODEL_CASES = NS_MODEL + "/cases";
    public static final String NS_MODEL_CASES_OUTCOME = NS_MODEL_CASES + "/outcome";
    public static final String NS_MODEL_CASES_OUTCOME_DEFAULT =
            qNameToUri(new QName(NS_MODEL_CASES_OUTCOME, "default"));

    public static final String NS_MODEL_APPROVAL = NS_MODEL + "/approval";
    public static final String NS_MODEL_APPROVAL_OUTCOME = NS_MODEL_APPROVAL + "/outcome";
    public static final String MODEL_APPROVAL_OUTCOME_APPROVE =
            qNameToUri(new QName(NS_MODEL_APPROVAL_OUTCOME, "approve"));
    public static final String MODEL_APPROVAL_OUTCOME_REJECT =
            qNameToUri(new QName(NS_MODEL_APPROVAL_OUTCOME, "reject"));
    public static final String MODEL_APPROVAL_OUTCOME_SKIP =
            qNameToUri(new QName(NS_MODEL_APPROVAL_OUTCOME, "skip"));

    public static final String NS_MODEL_CERTIFICATION = NS_MODEL + "/certification";
    public static final String NS_MODEL_CERTIFICATION_OUTCOME = NS_MODEL_CERTIFICATION + "/outcome";
    public static final QName MODEL_CERTIFICATION_OUTCOME_ACCEPT_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "accept");
    public static final String MODEL_CERTIFICATION_OUTCOME_ACCEPT = qNameToUri(MODEL_CERTIFICATION_OUTCOME_ACCEPT_QNAME);
    public static final QName MODEL_CERTIFICATION_OUTCOME_REVOKE_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "revoke");
    public static final String MODEL_CERTIFICATION_OUTCOME_REVOKE = qNameToUri(MODEL_CERTIFICATION_OUTCOME_REVOKE_QNAME);
    public static final QName MODEL_CERTIFICATION_OUTCOME_REDUCE_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "reduce");
    public static final String MODEL_CERTIFICATION_OUTCOME_REDUCE = qNameToUri(MODEL_CERTIFICATION_OUTCOME_REDUCE_QNAME);
    public static final QName MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "notDecided");
    public static final String MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED = qNameToUri(MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED_QNAME);
    public static final QName MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE_QNAME = new QName(NS_MODEL_CERTIFICATION_OUTCOME, "noResponse");
    public static final String MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE = qNameToUri(MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE_QNAME);        // only for aggregated decisions

    // Model constants in task extension

    public static final String NS_MODEL_EXTENSION = NS_MODEL + "/extension-3";

    public static final ItemName LEGACY_NOT_UPDATED_DURATION_PROPERTY_NAME =
            new ItemName(NS_MODEL_EXTENSION, "notUpdatedShadowsDuration");

    @VisibleForTesting // should be ignored by production code since 4.8
    public static final ItemName MODEL_EXTENSION_DRY_RUN = new ItemName(NS_MODEL_EXTENSION, "dryRun");
    @VisibleForTesting // should be ignored by production code since 4.8
    public static final ItemPath PATH_MODEL_EXTENSION_DRY_RUN = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_DRY_RUN);

    // Temporary
    public static final ItemName MODEL_EXTENSION_UPDATE_ID_MATCH = new ItemName(NS_MODEL_EXTENSION, "updateIdMatch");

    public static final ItemName MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS = new ItemName(NS_MODEL_EXTENSION, "retryLiveSyncErrors");
    public static final ItemName MODEL_EXTENSION_UPDATE_LIVE_SYNC_TOKEN_IN_DRY_RUN = new ItemName(NS_MODEL_EXTENSION, "updateLiveSyncTokenInDryRun");
    public static final ItemName MODEL_EXTENSION_LIVE_SYNC_BATCH_SIZE = new ItemName(NS_MODEL_EXTENSION, "liveSyncBatchSize");
    public static final ItemName MODEL_EXTENSION_KIND = new ItemName(NS_MODEL_EXTENSION, "kind");
    public static final ItemName MODEL_EXTENSION_INTENT = new ItemName(NS_MODEL_EXTENSION, "intent");
    public static final ItemName MODEL_EXTENSION_OBJECTCLASS = new ItemName(NS_MODEL_EXTENSION, "objectclass");
    public static final ItemName MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME = new ItemName(
            NS_MODEL_EXTENSION, "lastScanTimestamp");
    public static final ItemName MODEL_EXTENSION_PROFILING_INTERVAL = new ItemName(NS_MODEL_EXTENSION, "profilingInterval");
    public static final ItemName MODEL_EXTENSION_TRACING_INTERVAL = new ItemName(NS_MODEL_EXTENSION, "tracingInterval");
    public static final ItemName MODEL_EXTENSION_TRACING_PROFILE = new ItemName(NS_MODEL_EXTENSION, "tracingProfile");
    public static final ItemName MODEL_EXTENSION_TRACING_ROOT = new ItemName(NS_MODEL_EXTENSION, "tracingRoot");
    public static final ItemName MODEL_EXTENSION_TRACING = new ItemName(NS_MODEL_EXTENSION, "tracing");
    public static final ItemName MODEL_EXTENSION_REPORTING_OPTIONS = new ItemName(NS_MODEL_EXTENSION, "reporting");

    public static final ItemName MODEL_EXTENSION_OBJECT_TYPE = new ItemName(NS_MODEL_EXTENSION, "objectType");
    public static final ItemPath PATH_MODEL_EXTENSION_OBJECT_TYPE = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);

    public static final ItemName MODEL_EXTENSION_OBJECT_QUERY = new ItemName(NS_MODEL_EXTENSION, "objectQuery");
    public static final ItemPath PATH_MODEL_EXTENSION_OBJECT_QUERY = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);

    public static final ItemName MODEL_EXTENSION_SEARCH_OPTIONS = new ItemName(NS_MODEL_EXTENSION, "searchOptions");
    public static final ItemName MODEL_EXTENSION_USE_REPOSITORY_DIRECTLY = new ItemName(NS_MODEL_EXTENSION, "useRepositoryDirectly");
    public static final ItemName MODEL_EXTENSION_ITERATION_METHOD = new ItemName(NS_MODEL_EXTENSION, "iterationMethod");
    public static final ItemName MODEL_EXTENSION_FAILED_OBJECTS_SELECTOR = new ItemName(NS_MODEL_EXTENSION, "failedObjectsSelector");

    public static final ItemName MODEL_EXTENSION_OBJECT_DELTA = new ItemName(NS_MODEL_EXTENSION, "objectDelta");
    public static final ItemPath PATH_MODEL_EXTENSION_OBJECT_DELTA = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);

    public static final ItemName MODEL_EXTENSION_OBJECT_DELTAS = new ItemName(NS_MODEL_EXTENSION, "objectDeltas");
    public static final ItemName MODEL_EXTENSION_WORKER_THREADS = new ItemName(NS_MODEL_EXTENSION, "workerThreads");
    public static final ItemName MODEL_EXTENSION_OPTION_RAW = new ItemName(NS_MODEL_EXTENSION, "optionRaw");
    public static final ItemName MODEL_EXTENSION_EXECUTE_OPTIONS = new ItemName(NS_MODEL_EXTENSION, "executeOptions");
    public static final ItemPath PATH_MODEL_EXTENSION_EXECUTE_OPTIONS = ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
    public static final ItemName MODEL_EXTENSION_MODEL_EXECUTE_OPTIONS = new ItemName(NS_MODEL_EXTENSION, "modelExecuteOptions");

    public static final ItemName MODEL_EXTENSION_DIAGNOSE = new ItemName(NS_MODEL_EXTENSION, "diagnose");
    public static final ItemName MODEL_EXTENSION_FIX = new ItemName(NS_MODEL_EXTENSION, "fix");
    public static final ItemName MODEL_EXTENSION_DUPLICATE_SHADOWS_RESOLVER = new ItemName(NS_MODEL_EXTENSION,
            "duplicateShadowsResolver");
    public static final ItemName MODEL_EXTENSION_CHECK_DUPLICATES_ON_PRIMARY_IDENTIFIERS_ONLY = new ItemName(
            NS_MODEL_EXTENSION, "checkDuplicatesOnPrimaryIdentifiersOnly");

    public static final ItemName MODEL_EXTENSION_CLEANUP_POLICIES = new ItemName(NS_MODEL_EXTENSION,
            "cleanupPolicies");

    // TEMPORARY (until moved to new object type)
    public static final ItemName MODEL_EXTENSION_RESOURCE_OID = ItemName.from(NS_MODEL_EXTENSION, "resourceOid");
    public static final ItemName MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME = ItemName.from(NS_MODEL_EXTENSION, "objectClassLocalName");
    public static final ItemName MODEL_EXTENSION_STATISTICS = ItemName.from(NS_MODEL_EXTENSION, "statistics");

    public static final ItemName MODEL_EXTENSION_WORK_ITEM_ID = new ItemName(NS_MODEL_EXTENSION, "workItemId");
    public static final ItemName MODEL_EXTENSION_WORK_ITEM_ACTIONS = new ItemName(NS_MODEL_EXTENSION, "workItemActions");
    public static final ItemName MODEL_EXTENSION_WORK_ITEM_ACTION = new ItemName(NS_MODEL_EXTENSION, "workItemAction");
    public static final ItemName MODEL_EXTENSION_TIME_BEFORE_ACTION = new ItemName(NS_MODEL_EXTENSION, "timeBeforeAction");
    public static final ItemName MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT = new ItemName(NS_MODEL_EXTENSION, "plannedOperationAttempt");

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

    // Init channel, used when system is initializing itself
    public static final QName CHANNEL_INIT_QNAME = new QName(NS_CHANNEL, "init");
    public static final String CHANNEL_INIT_URI = qNameToUri(CHANNEL_INIT_QNAME);

    public static final QName CHANNEL_SELF_REGISTRATION_QNAME = new QName(NS_CHANNEL, "selfRegistration");
    public static final String CHANNEL_SELF_REGISTRATION_URI = qNameToUri(CHANNEL_SELF_REGISTRATION_QNAME);

    public static final QName CHANNEL_INVITATION_QNAME = new QName(NS_CHANNEL, "invitation");
    public static final String CHANNEL_INVITATION_URI = qNameToUri(CHANNEL_INVITATION_QNAME);

    // Channel for self-service part of the user interface. These are the pages when user is changing his own data.
    // E.g. update of his own profile and password change are considered to be self-service.
    public static final QName CHANNEL_SELF_SERVICE_QNAME = new QName(NS_CHANNEL, "selfService");
    public static final String CHANNEL_SELF_SERVICE_URI = qNameToUri(CHANNEL_SELF_SERVICE_QNAME);

    // Password reset channel. This is *reset*, which means that the user does not know the old password and cannot log in.
    public static final QName CHANNEL_RESET_PASSWORD_QNAME = new QName(NS_CHANNEL, "resetPassword");
    public static final String CHANNEL_RESET_PASSWORD_URI = qNameToUri(CHANNEL_RESET_PASSWORD_QNAME);


    public static final QName CHANNEL_IDENTITY_RECOVERY_QNAME = new QName(NS_CHANNEL, "identityRecovery");
    public static final String CHANNEL_IDENTITY_RECOVERY_URI = qNameToUri(CHANNEL_IDENTITY_RECOVERY_QNAME);


    // Catch-all channel for all user operations in user interface.
    public static final String CHANNEL_USER_LOCAL = "user";
    public static final QName CHANNEL_USER_QNAME = new QName(NS_CHANNEL, "user");
    public static final String CHANNEL_USER_URI = qNameToUri(CHANNEL_USER_QNAME);

    //GUI constants which are also used in the notifiers
    public static final String WORK_ITEM_URL_PREFIX = "/admin/workItem?pathParameter=";
    public static final String CASE_URL_PREFIX = "/admin/caseNew/";
    public static final String REGISTRATION_PREFIX = "/registration";
    public static final String PASSWORD_RESET_CONFIRMATION_PREFIX = "/confirm/reset";
    public static final String ACCOUNT_ACTIVATION_PREFIX = "/activate/accounts";
    public static final String AUTH_MODULE_PREFIX = "/auth";

    public static final String INTENT_DEFAULT = "default";
    public static final String INTENT_UNKNOWN = "unknown";

    // This constant should not be here. It is used by schema processor to supply correct import. But the dependency should
    // be inverted, eventually (MID-356)
    public static final String ICF_FRAMEWORK_URI = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1";
    public static final String NS_ICF_CONFIGURATION = ICF_FRAMEWORK_URI + "/connector-schema-3";
    public static final String ICF_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME = "ConfigurationPropertiesType";
    public static final String ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME = "configurationProperties";
    public static final ItemName ICF_CONFIGURATION_PROPERTIES_NAME =
            ItemName.from(NS_ICF_CONFIGURATION, ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME);

    /** The namespace for subtypes in ConnId schemas. For example, see "POLYSTRING_SUBTYPE" in the LDAP connector. */
    public static final String NS_ICF_SUBTYPES = ICF_FRAMEWORK_URI + "/subtypes";
    public static final QName ICF_SUBTYPES_POLYSTRING_QNAME = new QName(NS_ICF_SUBTYPES, "PolyString");
    public static final String ICF_SUBTYPES_POLYSTRING_URI = qNameToUri(ICF_SUBTYPES_POLYSTRING_QNAME);

    // People could expect to find the constant here.
    public static final String NS_RI = MidPointConstants.NS_RI;

    public static final String NS_ICF_SCHEMA = ICF_FRAMEWORK_URI + "/resource-schema-3";
    public static final String NS_ICFS = NS_ICF_SCHEMA; // alternative name
    public static final String NS_ICF_SCHEMA_PREFIX = "icfs";
    public static final ItemName ICFS_NAME = new ItemName(NS_ICF_SCHEMA, "name");
    public static final ItemPath ICFS_NAME_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, ICFS_NAME);
    public static final ItemName ICFS_UID = new ItemName(NS_ICF_SCHEMA, "uid");
    public static final ItemPath ICFS_UID_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, ICFS_UID);
    public static final ItemName ICFS_PASSWORD = new ItemName(NS_ICF_SCHEMA, "password");

    public static final String ACCOUNT_OBJECT_CLASS_LOCAL_NAME = "AccountObjectClass";
    public static final String GROUP_OBJECT_CLASS_LOCAL_NAME = "GroupObjectClass";
    public static final ItemName RI_ACCOUNT_OBJECT_CLASS = new ItemName(MidPointConstants.NS_RI, ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
    public static final ItemName RI_GROUP_OBJECT_CLASS = new ItemName(MidPointConstants.NS_RI, GROUP_OBJECT_CLASS_LOCAL_NAME);

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

    public static final ItemName C_NAME = new ItemName(SchemaConstants.NS_C, "name");

    public static final ItemName C_MODEL_CONTEXT = new ItemName(NS_C, "modelContext");

    // Lifecycle

    public static final String LIFECYCLE_DRAFT = "draft";
    public static final String LIFECYCLE_PROPOSED = "proposed";
    public static final String LIFECYCLE_ACTIVE = "active";
    public static final String LIFECYCLE_SUSPENDED = "suspended";
    public static final String LIFECYCLE_DEPRECATED = "deprecated";
    public static final String LIFECYCLE_ARCHIVED = "archived";
    public static final String LIFECYCLE_FAILED = "failed";

    // Case: generic reusable case states
    // Not all cases use all these states; most common are OPEN and CLOSED.

    // Case was created but it is not yet open. E.g. there should be no work items.
    public static final String CASE_STATE_CREATED = "created";
    public static final QName CASE_STATE_CREATED_QNAME = new QName(NS_CASE, CASE_STATE_CREATED);

    // Case is open - work items are created, completed, delegated, etc. Associated work is carried out.
    public static final String CASE_STATE_OPEN = "open";
    public static final QName CASE_STATE_OPEN_QNAME = new QName(NS_CASE, CASE_STATE_OPEN);
    public static final String CASE_STATE_OPEN_URI = qNameToUri(CASE_STATE_OPEN_QNAME);

    // All human interaction regarding the case is over. But there might be some actions pending, e.g.
    // submitting change execution task, waiting for subtasks to be closed, and so on.
    public static final String CASE_STATE_CLOSING = "closing";
    public static final QName CASE_STATE_CLOSING_QNAME = new QName(NS_CASE, CASE_STATE_CLOSING);

    // The case now proceeds by means of automated execution of defined actions (e.g. approved changes);
    // or waiting for the execution to start.
    public static final String CASE_STATE_EXECUTING = "executing";

    // The case is closed. No further actions nor changes are expected.
    public static final String CASE_STATE_CLOSED = "closed";
    public static final QName CASE_STATE_CLOSED_QNAME = new QName(NS_CASE, CASE_STATE_CLOSED);

    // Object collections

    /**
     * All objects in role catalog. It means all the objects in all the categories that are placed under the
     * primary role catalog defined in the system. If used in a context where the role catalog can be displayed
     * as a tree then this collection will be displayed as a tree.
     */
    public static final QName OBJECT_COLLECTION_ROLE_CATALOG_QNAME = new QName(NS_OBJECT_COLLECTIONS, "roleCatalog");
    public static final String OBJECT_COLLECTION_ROLE_CATALOG_URI = qNameToUri(OBJECT_COLLECTION_ROLE_CATALOG_QNAME);

    /**
     * Collection that contains all roles.
     */
    public static final QName OBJECT_COLLECTION_ALL_ROLES_QNAME = new QName(NS_OBJECT_COLLECTIONS, "allRoles");
    public static final String OBJECT_COLLECTION_ALL_ROLES_URI = qNameToUri(OBJECT_COLLECTION_ALL_ROLES_QNAME);

    /**
     * Collection that contains all orgs.
     */
    public static final QName OBJECT_COLLECTION_ALL_ORGS_QNAME = new QName(NS_OBJECT_COLLECTIONS, "allOrgs");
    public static final String OBJECT_COLLECTION_ALL_ORGS_URI = qNameToUri(OBJECT_COLLECTION_ALL_ORGS_QNAME);

    /**
     * Collection that contains all services.
     */
    public static final QName OBJECT_COLLECTION_ALL_SERVICES_QNAME = new QName(NS_OBJECT_COLLECTIONS, "allServices");
    public static final String OBJECT_COLLECTION_ALL_SERVICES_URI = qNameToUri(OBJECT_COLLECTION_ALL_SERVICES_QNAME);

    /**
     * Collection that contains user's assignments.
     */
    public static final QName OBJECT_COLLECTION_USER_ASSIGNMENTS_QNAME = new QName(NS_OBJECT_COLLECTIONS, "userAssignments");
    public static final String OBJECT_COLLECTION_USER_ASSIGNMENTS_URI = qNameToUri(OBJECT_COLLECTION_USER_ASSIGNMENTS_QNAME);

    // Misc

    public static final String BUNDLE_NAME = "schema";
    public static final String SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH = "localization/" + BUNDLE_NAME;     // Do not load this bundle explicitly, see MID-4800

    // registration
    public static final String USER_ID = "user";
    public static final String TOKEN = "token";

    public static final String OBJECT_TYPE_KEY_PREFIX = "ObjectType.";
    public static final String OBJECT_TYPE_LOWERCASE_KEY_PREFIX = "ObjectTypeLowercase.";
    public static final String DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX = "DefaultPolicyConstraint.";
    public static final String DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX = "DefaultPolicyConstraint.Short.";
    public static final String DEFAULT_POLICY_CONSTRAINT_SHORT_REL_MESSAGE_KEY_PREFIX = "DefaultPolicyConstraint.ShortWithRelation.";
    public static final String POLICY_CONSTRAINT_KEY_PREFIX = "PolicyConstraint.";
    public static final String POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX = "PolicyConstraint.Short.";
    public static final String POLICY_CONSTRAINTS_BEFORE_KEY = "PolicyConstraints.before";
    public static final String POLICY_CONSTRAINTS_AFTER_KEY = "PolicyConstraints.after";
    public static final String TECHNICAL_OBJECT_SPECIFICATION_KEY = "TechnicalObjectSpecification";
    public static final String OBJECT_SPECIFICATION_KEY = "ObjectSpecification";
    public static final String OBJECT_SPECIFICATION_WITH_PATH_KEY = "ObjectSpecificationWithPath";
    public static final String USER_DESCRIPTIVE_NAME = "UserDescriptiveName";

    // a bit of hack
    public static final String COMPLETED_TASK_CLEANUP_TRIGGER_HANDLER_URI = SchemaConstants.NS_MODEL + "/completedTaskCleanup/handler-3";

    // other (temp)
    public static final ItemPath PATH_CREDENTIALS_PASSWORD_VALUE = ItemPath
            .create(SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);

    public static final ItemPath PATH_CREDENTIALS_PASSWORD = ItemPath
            .create(SchemaConstantsGenerated.C_CREDENTIALS, CredentialsType.F_PASSWORD);

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

    // diagnostic information types

    public static final QName TASK_THREAD_DUMP = new QName(NS_C, "taskThreadDump");
    public static final String TASK_THREAD_DUMP_URI = qNameToUri(TASK_THREAD_DUMP);

    // diagnostic information causes

    public static final QName USER_REQUEST = new QName(NS_C, "userRequest");
    public static final String USER_REQUEST_URI = qNameToUri(USER_REQUEST);
    public static final QName INTERNAL = new QName(NS_C, "internal");
    public static final String INTERNAL_URI = qNameToUri(INTERNAL);

    public static final String TRACE_DICTIONARY_PREFIX = "#dictionary#";

    public static final String ID_AUDIT_RECORDS_CLEANUP = "auditRecords";
    public static final String ID_CLOSED_TASKS_CLEANUP = "closedTasks";
    public static final String ID_CLOSED_CASES_CLEANUP = "closedCases";
    public static final String ID_DEAD_NODES_CLEANUP = "deadNodes";
    public static final String ID_OUTPUT_REPORTS_CLEANUP = "outputReports";
    public static final String ID_CLOSED_CERTIFICATION_CAMPAIGNS_CLEANUP = "closedCertificationCampaigns";

    public static final ActivityPath PATH_AUDIT_RECORDS_CLEANUP = ActivityPath.fromId(ID_AUDIT_RECORDS_CLEANUP);
    public static final ActivityPath PATH_CLOSED_TASKS_CLEANUP = ActivityPath.fromId(ID_CLOSED_TASKS_CLEANUP);
    public static final ActivityPath PATH_CLOSED_CASES_CLEANUP = ActivityPath.fromId(ID_CLOSED_CASES_CLEANUP);
    public static final ActivityPath PATH_DEAD_NODES_CLEANUP = ActivityPath.fromId(ID_DEAD_NODES_CLEANUP);
    public static final ActivityPath PATH_OUTPUT_REPORTS_CLEANUP = ActivityPath.fromId(ID_OUTPUT_REPORTS_CLEANUP);
    public static final ActivityPath PATH_CLOSED_CERTIFICATION_CAMPAIGNS_CLEANUP =
            ActivityPath.fromId(ID_CLOSED_CERTIFICATION_CAMPAIGNS_CLEANUP);

    public static final String CORRELATION_NONE = "none";
    public static final String CORRELATION_EXISTING_PREFIX = "existing-";

    public static final @NotNull ItemPath PATH_FOCUS_IDENTITY =
            ItemPath.create(FocusType.F_IDENTITIES, FocusIdentitiesType.F_IDENTITY);
    public static final @NotNull ItemPath PATH_FOCUS_NORMALIZED_DATA =
            ItemPath.create(FocusType.F_IDENTITIES, FocusIdentitiesType.F_NORMALIZED_DATA);
    public static final @NotNull ItemPath PATH_FOCUS_DEFAULT_AUTHORITATIVE_SOURCE =
            ItemPath.create(FocusType.F_IDENTITIES, FocusIdentitiesType.F_DEFAULT_AUTHORITATIVE_SOURCE);

    public static final String SIMULATION_RESULT_DEFAULT_TRANSACTION_ID = "default";

    public static final @NotNull ItemPath CORRELATION_SITUATION_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_SITUATION);
    public static final @NotNull ItemPath CORRELATION_RESULTING_OWNER_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_RESULTING_OWNER);
    public static final @NotNull ItemPath CORRELATION_OWNER_OPTIONS_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_OWNER_OPTIONS);
    public static final @NotNull ItemPath CORRELATION_START_TIMESTAMP_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_START_TIMESTAMP);
    public static final @NotNull ItemPath CORRELATION_END_TIMESTAMP_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_END_TIMESTAMP);
    public static final @NotNull ItemPath CORRELATION_CASE_OPEN_TIMESTAMP_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_CASE_OPEN_TIMESTAMP);
    public static final @NotNull ItemPath CORRELATION_CASE_CLOSE_TIMESTAMP_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_CASE_CLOSE_TIMESTAMP);
    public static final @NotNull ItemPath CORRELATION_PERFORMER_REF_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_PERFORMER_REF);
    public static final @NotNull ItemPath CORRELATION_PERFORMER_COMMENT_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_PERFORMER_COMMENT);
    public static final @NotNull ItemPath CORRELATION_CORRELATOR_STATE_PATH =
            ItemPath.create(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATOR_STATE);

    /** ID of "allow all" expression profile. */
    public static final String FULL_EXPRESSION_PROFILE_ID = "##full";

    /** ID of "allow none" expression profile. */
    public static final String NONE_EXPRESSION_PROFILE_ID = "##none";

    /** ID of "legacy unprivileged mode" expression profile for scripting (bulk actions). */
    public static final String LEGACY_UNPRIVILEGED_BULK_ACTIONS_PROFILE_ID = "##legacyUnprivilegedBulkActions";

    /**
     * The ID for built-in Groovy permission and script expression profiles.
     *
     * The ID is not really used for anything serious, except that each profile should have one.
     */
    public static final String BUILTIN_GROOVY_EXPRESSION_PROFILE_ID = "##groovyBuiltIn";
    public static final String CONNECTOR_CONFIGURATION_PREFIX = "cfg";

    public static final String MARK_PROTECTED_OID = SystemObjectsType.MARK_PROTECTED.value();
}
