/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author shood
 * @author Viliam Repan (lazyman)
 */
public class UserProfileStorage implements Serializable, DebugDumpable {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGING_SIZE = 20;

    public static final Integer[] DEFAULT_PAGING_SIZES = new Integer[] { 10, DEFAULT_PAGING_SIZE, 50, 100 };
    public static final int DEFAULT_DASHBOARD_PAGING_SIZE = 5;

    /*
     *   Enum containing IDs of all tables. where paging size can be adjusted
     * */
    public enum TableId {
        TABLE_CLUSTER,
        TABLE_SESSION,
        PAGE_USER_SELECTION,
        TABLE_ROLES,
        TABLE_CASES,
        TABLE_USERS,
        TABLE_PARENT_CLUSTER,
        TABLE_ORGS,
        TABLE_SERVICES,
        TABLE_POLICIES,
        TABLE_APPLICATIONS,
        TABLE_ARCHETYPES,
        TABLE_MESSAGE_TEMPLATES,
        TABLE_RESOURCES,
        TABLE_RESOURCE_TEMPLATES,
        TABLE_VALUE_POLICIES,
        TABLE_TASKS,
        TABLE_SUBTASKS,
        TABLE_WORKERS,
        TABLE_OBJECTS_COLLECTION,
        TABLE_OBJECT_TEMPLATES,
        TABLE_SMART_CORRELATION,
        TABLE_SMART_INBOUND_MAPPINGS,
        ROLE_MEMBER_PANEL,
        ORG_MEMBER_PANEL,
        ARCHETYPE_MEMBER_PANEL,
        SERVICE_MEMBER_PANEL,
        POLICY_MEMBER_PANEL,
        TREE_TABLE_PANEL_CHILD,
        TREE_TABLE_PANEL_MEMBER,
        TREE_TABLE_PANEL_MANAGER,
        CONF_PAGE_ACCOUNTS,
        CONF_DEBUG_LIST_PANEL,
        PAGE_CREATED_REPORTS_PANEL,
        PAGE_RESOURCE_PANEL,
        PAGE_RESOURCES_PANEL,
        PAGE_RESOURCE_TASKS_PANEL,
        PAGE_MESSAGE_TEMPLATE_LOCALIZED_CONTENT_PANEL,
        PAGE_RESOURCE_ACCOUNTS_PANEL_REPOSITORY_MODE,
        PAGE_RESOURCE_ACCOUNTS_PANEL_RESOURCE_MODE,
        PAGE_RESOURCE_ENTITLEMENT_PANEL_REPOSITORY_MODE,
        PAGE_RESOURCE_ENTITLEMENT_PANEL_RESOURCE_MODE,
        PAGE_RESOURCE_GENERIC_PANEL_REPOSITORY_MODE,
        PAGE_RESOURCE_GENERIC_PANEL_RESOURCE_MODE,
        PAGE_RESOURCE_OBJECT_CLASS_PANEL,
        PAGE_TASKS_PANEL,
        PAGE_NODES_PANEL,
        PAGE_USERS_PANEL,
        PAGE_WORK_ITEMS,
        PAGE_WORKFLOW_REQUESTS,
        PAGE_RESOURCES_CONNECTOR_HOSTS,
        PAGE_REPORTS,
        PAGE_CERT_CAMPAIGN_OUTCOMES_PANEL,
        PAGE_CERT_CAMPAIGNS_PANEL,
        PAGE_CAMPAIGNS,
        PAGE_CERT_DECISIONS_PANEL,
        PAGE_CERT_DEFINITIONS_PANEL,
        PAGE_CASE_WORK_ITEMS_PANEL,
        PAGE_WORK_ITEM_HISTORY_PANEL,
        PAGE_TASK_HISTORY_PANEL,
        PAGE_TASK_CURRENT_WORK_ITEMS_PANEL,
        PAGE_AUDIT_LOG_VIEWER,
        TASK_EVENTS_TABLE,
        ASSIGNMENTS_TAB_TABLE,
        INDUCEMENTS_TAB_TABLE,
        INDUCED_ENTITLEMENTS_TAB_TABLE,
        POLICY_RULES_TAB_TABLE,
        OBJECT_POLICIES_TAB_TABLE,
        GLOBAL_POLICY_RULES_TAB_TABLE,
        TRIGGERS_TAB_TABLE,
        LOGGING_TAB_LOGGER_TABLE,
        LOGGING_TAB_APPENDER_TABLE,
        NOTIFICATION_TAB_MAIL_SERVER_TABLE,
        COLLECTION_VIEW_TABLE,
        USERS_VIEW_TABLE,
        FOCUS_PROJECTION_TABLE,
        SELF_DASHBOARD_CASES_PANEL,
        PAGE_CASE_WORKITEMS_TAB,
        PAGE_CASE_CHILD_CASES_TAB,
        PAGE_FOCUS_DETAILS_CASES_PANEL,
        PAGE_CASE_EVENTS_TAB,
        PANEL_RESOURCE_OBJECT_TYPES,
        PANEL_OBJECT_COLLECTION_VIEWS,
        PANEL_OBJECT_POLICY_CONTENT,
        PANEL_RELATIONS_CONTENT,
        PANEL_SMS_TRANSPORT_CONTENT,
        PANEL_SUB_SYSTEM_LOGGERS_CONTENT,
        PANEL_USER_DASHBOARD_LINK_CONTENT,
        PANEL_ADDITIONAL_MENU_LINK_CONTENT,
        PANEL_APPENDERS_CONTENT,
        PANEL_CLASS_LOGGERS_CONTENT,
        PANEL_CONFIGURABLE_USER_DASHBOARD_CONTENT,
        PANEL_CUSTOM_TRANSPORT_CONTENT,
        PANEL_EVENT_HANDLER_CONTENT,
        PANEL_FILE_TRANSPORT_CONTENT,
        PANEL_GLOBAL_POLICY_RULE_CONTENT,
        PANEL_GUI_OBJECT_DETAILS_CONTENT,
        PANEL_MAIL_TRANSPORT_CONTENT,
        PANEL_MESSAGE_TRANSPORT_CONTENT,
        PANEL_INBOUND_MAPPING_WIZARD,
        PANEL_OUTBOUND_MAPPING_WIZARD,
        PANEL_MAPPING_OVERRIDE_WIZARD,
        PANEL_SYNCHRONIZATION_REACTION_WIZARD,
        PANEL_CORRELATION_ITEMS_WIZARD,
        PAGE_REQUEST_ACCESS_ROLE_CATALOG,
        PANEL_INDUCT_BY,
        PANEL_ROLE_MEMBERSHIP,
        PANEL_GOVERNANCE_CARDS,
        PANEL_ACCESS_WIZARD_STEP,
        PAGE_SIMULATION_RESULTS,
        PAGE_SIMULATION_RESULT_TAGS,
        PAGE_SIMULATION_RESULT_PROCESSED_OBJECTS,
        PAGE_MARKS_TABLE,
        PANEL_USER_ACCESSES,
        MARK_MARKED_SHADOWS_PANEL,
        PANEL_FILE_SECRET_PROVIDERS,
        PANEL_PROPERTIES_SECRET_PROVIDERS,
        PANEL_ENVIRONMENT_VARIABLES_SECRET_PROVIDERS,
        PANEL_CUSTOM_SECRET_PROVIDERS,
        PANEL_ASSOCIATION_TYPES,
        PAGE_SCHEMAS_TABLE,
        PANEL_DETECTED_PATTERN,
        PANEL_MIGRATED_ROLES,
        PANEL_CANDIDATE_ROLES,
        PANEL_OUTLIER_PROPERTIES,
        TABLE_OUTLIERS,
        PANEL_OUTLIER_PARTITIONS,
        PANEL_RESOURCE_OBJECT_CLASSES,
        PANEL_RESOURCE_OBJECT_TYPES_SUGGESTIONS,
        PANEL_ASSOCIATION_INBOUND,
        PANEL_ASSOCIATION_OUTBOUND,
        PANEL_MARKING_WIZARD,
        PANEL_DEFAULT_OPERATION_POLICIES_WIZARD,
        PANEL_MARKS_OF_OBJECT,
        PANEL_FOCUS_MAPPING_WIZARD,
        PANEL_ATTRIBUTE_VOLATILITY_WIZARD,
        PANEL_TASK_ERRORS,
    }

    private final Map<String, Integer> tables = new HashMap<>();

    public Integer getPagingSize(TableId key) {
        Validate.notNull(key, "Key must not be null.");

        return getPagingSize(key.name());
    }

    public Integer getPagingSize(String key) {
        Validate.notNull(key, "Key must not be null.");

        Integer size = tables.get(key);
        return size == null ? DEFAULT_PAGING_SIZE : size;
    }

    public void setPagingSize(TableId key, Integer size) {
        Validate.notNull(key, "Key must not be null.");

        setPagingSize(key.name(), size);
    }

    public boolean isExistPagingSize(TableId key) {
        Validate.notNull(key, "TableId must not be null.");
        Validate.notNull(key.name(), "TableId.name() must not be null.");

        Integer size = tables.get(key.name());
        return size != null;
    }

    public void setPagingSize(String key, Integer size) {
        Validate.notNull(key, "Key must not be null.");

        tables.put(key, size);
    }

    public Map<String, Integer> getTables() {
        return tables;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("UserProfileStorage\n");
        DebugUtil.debugDumpWithLabel(sb, "tables", tables, indent + 1);
        return sb.toString();
    }

}
