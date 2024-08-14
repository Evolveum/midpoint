/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.RestAuthorizationAction;
import com.evolveum.midpoint.security.enforcer.api.AbstractAuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

/**
 * Represents a model-level action (method) that can be authorized.
 *
 * Documented in https://docs.evolveum.com/midpoint/reference/security/authorization/configuration/#object-authorization-actions
 * with additional (developer-level) information here.
 *
 * @see RestAuthorizationAction
 */
public enum ModelAuthorizationAction implements DisplayableValue<String> {

    READ("read", "Read", "READ_HELP"),
    GET("get", "Get", "GET_HELP"),
    SEARCH("search", "Search", "SEARCH_HELP"),
    ADD("add", "Add", "ADD_HELP"),

    /**
     * Allows the object to be modified. When asked about it (e.g., via {@link SecurityEnforcer#isAuthorized(String,
     * AuthorizationPhaseType, AbstractAuthorizationParameters, SecurityEnforcer.Options, Task, OperationResult)}),
     * please make sure that you provide a non-null delta that describes the requested modification.
     *
     * A special case is asking whether there is _any_ authorization to modify the object.
     * In that case, you may provide an empty delta, provided by it is *non-null*.
     */
    MODIFY("modify", "Modify", "MODIFY_HELP"),
    DELETE("delete", "Delete", "DELETE_HELP"),
    RECOMPUTE("recompute", "Recompute", "RECOMPUTE_HELP"),
    TEST("test", "Test resource", "TEST_RESOURCE_HELP"),

    /**
     * Allows to "use" an object; the exact meaning depends on the type of the object.
     * Currently supported for:
     *
     * - task templates: here it means instantiating the template by
     * {@link ModelInteractionService#submitTaskFromTemplate(String, ActivityCustomization,
     * Task, OperationResult)} and related methods.
     */
    USE("use", "Use", "USE_HELP"),

    /**
     * Import objects from file or a stream. This means importing any type of
     * object (e.g. user, configuration, resource, object templates, ...
     */
    IMPORT_OBJECTS("importObjects", "Import Objects", "IMPORT_OBJECTS_HELP"),

    /**
     * Import resource objects from resource. This means import of accounts, entitlements
     * or other objects from a resource. The import creates shadows.
     */
    IMPORT_FROM_RESOURCE("importFromResource", "Import from Resource", "IMPORT_FROM_RESOURCE_HELP"),

    DISCOVER_CONNECTORS("discoverConnectors", "Discover Connectors", "DISCOVER_CONNECTORS_HELP"),

    ASSIGN("assign", "Assign", "ASSIGN_HELP"),
    UNASSIGN("unassign", "Unassign", "UNASSIGN_HELP"),
    DELEGATE("delegate", "Delegate", "DELEGATE_HELP"),
    ATTORNEY("attorney", "Attorney", "ATTORNEY_HELP"),
    @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated // use bulk-3#all
    EXECUTE_SCRIPT("executeScript", "Execute script", "EXECUTE_SCRIPT_HELP"),
    CHANGE_CREDENTIALS("changeCredentials", "Change credentials", "CHANGE_CREDENTIALS_HELP"),

    NOTIFY_CHANGE("notifyChange", "Notify change", "NOTIFY_CHANGE_HELP"),

    SUSPEND_TASK("suspendTask", "Suspend task", "SUSPEND_TASK_HELP"),
    RESUME_TASK("resumeTask", "Resume task", "RESUME_TASK_HELP"),
    RUN_TASK_IMMEDIATELY("runTaskImmediately", "Run task immediately", "RUN_TASK_IMMEDIATELY_HELP"),
    STOP_SERVICE_THREADS("stopServiceThreads", "Stop service threads", "STOP_SERVICE_THREADS_HELP"),
    START_SERVICE_THREADS("startServiceThreads", "Start service threads", "START_SERVICE_THREADS_HELP"),
    SYNCHRONIZE_TASKS("synchronizeTasks", "Synchronize tasks", "SYNCHRONIZE_TASKS_HELP"),
    SYNCHRONIZE_WORKFLOW_REQUESTS("synchronizeWorkflowRequests", "Synchronize workflow requests", "SYNCHRONIZE_WORKFLOW_REQUESTS_HELP"),
    STOP_TASK_SCHEDULER("stopTaskScheduler", "Stop task scheduler", "STOP_TASK_SCHEDULER_HELP"),
    START_TASK_SCHEDULER("startTaskScheduler", "Start task scheduler", "START_TASK_SCHEDULER_HELP"),

    READ_THREADS("readThreads", "Read threads", "READ_THREADS_HELP"),

    /** Ability to complete a work item (case- or certification- related). */
    COMPLETE_WORK_ITEM("completeWorkItem", "Complete work item", "COMPLETE_WORK_ITEM_HELP"),

    /** Ability to delegate a work item (case- or certification- related). */
    DELEGATE_WORK_ITEM("delegateWorkItem", "Delegate work item", "DELEGATE_WORK_ITEM_HELP"),

    CREATE_CERTIFICATION_CAMPAIGN("createCertificationCampaign", "Create a certification campaign", "CREATE_CERTIFICATION_CAMPAIGN_HELP"),
    OPEN_CERTIFICATION_CAMPAIGN_REVIEW_STAGE("openCertificationCampaignReviewStage", "Open access certification campaign review stage", "OPEN_CERTIFICATION_CAMPAIGN_REVIEW_STAGE_HELP"),
    CLOSE_CERTIFICATION_CAMPAIGN_REVIEW_STAGE("closeCertificationCampaignReviewStage", "Close access certification campaign review stage", "CLOSE_CERTIFICATION_CAMPAIGN_REVIEW_STAGE_HELP"),
    START_CERTIFICATION_REMEDIATION("startCertificationRemediation", "Start certification campaign results remediation", "START_CERTIFICATION_REMEDIATION_HELP"),
    CLOSE_CERTIFICATION_CAMPAIGN("closeCertificationCampaign", "Close certification campaign", "CLOSE_CERTIFICATION_CAMPAIGN_HELP"),
    REITERATE_CERTIFICATION_CAMPAIGN("reiterateCertificationCampaign", "Reiterate certification campaign", "REITERATE_CERTIFICATION_CAMPAIGN_HELP"),

    /** Replaced by {@link #READ}, see AuthorizationMigrator. No longer used in the production code. */
    @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
    READ_OWN_CERTIFICATION_DECISIONS("readOwnCertificationDecisions", "Read own access certification decisions", "READ_OWN_CERTIFICATION_DECISIONS_HELP"),

    /** Replaced by {@link #COMPLETE_WORK_ITEM}. No longer used in the production code. */
    @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
    RECORD_CERTIFICATION_DECISION("recordCertificationDecision", "Record access certification decision", "RECORD_CERTIFICATION_DECISION_HELP"),

    /** Replaced by {@link #COMPLETE_WORK_ITEM}. No longer used in the production code. */
    @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
    COMPLETE_ALL_WORK_ITEMS("completeAllWorkItems", "Complete all work items", "COMPLETE_ALL_WORK_ITEMS_HELP"),

    /** Replaced by {@link #DELEGATE_WORK_ITEM}. No longer used in the production code. */
    @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated // use DELEGATE_WORK_ITEM instead
    DELEGATE_ALL_WORK_ITEMS("delegateAllWorkItems", "Delegate all work items", "DELEGATE_ALL_WORK_ITEMS_HELP"),

    /** Replaced by {@link #DELEGATE_WORK_ITEM}. No longer used in the production code. */
    @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
    DELEGATE_OWN_WORK_ITEMS("delegateOwnWorkItems", "Delegate own work items", "DELEGATE_OWN_WORK_ITEMS_HELP"),

    CANCEL_CASE("cancelCase", "Cancel case", "CANCEL_CASE_HELP"),

    AUDIT_READ("auditRead", "Audit Read", "AUDIT_READ_HELP"),
    // Authorization to create a user-level (custom) audit record. Does not apply to internal records that are created automatically by the model without
    // any special authorization
    AUDIT_RECORD("auditRecord", "Audit Record", "AUDIT_RECORD_HELP"),
    // Ability to manage the audit log, e.g. to clean it up (expunge old records).
    AUDIT_MANAGE("auditManage", "Audit Manage", "AUDIT_MANAGE_HELP"),

    RAW_OPERATION("rawOperation", "Raw operation", "RAW_OPERATION_HELP"),
    PARTIAL_EXECUTION("partialExecution", "Partial execution", "PARTIAL_EXECUTION_HELP"),
    GET_EXTENSION_SCHEMA("getExtensionSchema", "Get extension schema", "GET_EXTENSION_SCHEMA_HELP"),

    RUN_REPORT("runReport", "Run report", "RUN_REPORT_HELP"),
    IMPORT_REPORT("importReport", "Import report", "IMPORT_REPORT_HELP"),

    @Experimental
    CLEANUP_AUDIT_RECORDS("cleanupAuditRecords", "Clean up audit records", "CLEANUP_AUDIT_RECORDS_HELP"),

    @Experimental
    RECORD_TRACE("recordTrace", "Record trace", "RECORD_TRACE_HELP"),

    @Experimental
    READ_TRACE("readTrace", "Read trace", "READ_TRACE_HELP");

    public static final String[] AUTZ_ACTIONS_URLS_SEARCH = new String[] { READ.getUrl(),  SEARCH.getUrl() };
    public static final String[] AUTZ_ACTIONS_URLS_GET = new String[] { READ.getUrl(),  GET.getUrl() };
    public static final String[] AUTZ_ACTIONS_URLS_ADD = new String[] { ADD.getUrl() };
    public static final String[] AUTZ_ACTIONS_URLS_MODIFY = new String[] { MODIFY.getUrl() };
    public static final String[] AUTZ_ACTIONS_URLS_ASSIGN = new String[] { ASSIGN.getUrl() };
    public static final String[] AUTZ_ACTIONS_URLS_ATTORNEY = new String[] { ATTORNEY.getUrl() };

    private final String url;
    private final String label;
    private final String description;

    ModelAuthorizationAction(String urlLocalPart, String label, String desc) {
        this.url = QNameUtil.qNameToUri(new QName(ModelService.AUTZ_NAMESPACE, urlLocalPart));
        this.label = label;
        this.description = desc;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String getValue() {
        return url;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

}
