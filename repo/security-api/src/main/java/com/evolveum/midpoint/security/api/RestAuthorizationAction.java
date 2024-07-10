/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.util.DisplayableValue;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a REST action (method) that can be authorized.
 *
 * The methods protected by other means (e.g., by intra-cluster authentication) are currently not listed here.
 *
 * See also `ModelAuthorizationAction`.
 *
 * Notes:
 *
 * . {@link AuthorizationConstants#AUTZ_REST_ALL_URL} is a special action that covers all REST actions.
 * . {@link AuthorizationConstants#AUTZ_REST_PROXY_URL} is *not* a REST action URL.
 */
public enum RestAuthorizationAction implements DisplayableValue<String> {

    GENERATE_VALUE("generateValue", "Generate value", "GENERATE_VALUE_HELP"),
    RPC_GENERATE_VALUE("rpcGenerateValue", "Generate value (RPC)", "RPC_GENERATE_VALUE_HELP"),
    VALIDATE_VALUE("validateValue", "Validate value", "VALIDATE_VALUE_HELP"),
    RPC_VALIDATE_VALUE("rpcValidateValue", "Validate value (RPC)", "RPC_VALIDATE_VALUE_HELP"),
    GET_VALUE_POLICY("getValuePolicy", "Get value policy", "GET_VALUE_POLICY_HELP"),
    GET_OBJECT("getObject", "Get object", "GET_OBJECT_HELP"),
    GET_SELF("getSelf", "Get self", "GET_SELF_HELP"),
    GET_OBJECTS("getObjects", "Get objects", "GET_OBJECTS_HELP"),
    ADD_OBJECT("addObject", "Add object", "ADD_OBJECT_HELP"),
    DELETE_OBJECT("deleteObject", "Delete object", "DELETE_OBJECT_HELP"),
    MODIFY_OBJECT("modifyObject", "Modify object", "MODIFY_OBJECT_HELP"),
    NOTIFY_CHANGE("notifyChange", "Notify change", "NOTIFY_CHANGE_HELP"),
    FIND_SHADOW_OWNER("findShadowOwner", "Find shadow owner", "FIND_SHADOW_OWNER_HELP"),
    IMPORT_SHADOW("importShadow", "Import shadow", "IMPORT_SHADOW_HELP"),
    SEARCH_OBJECTS("searchObjects", "Search objects", "SEARCH_OBJECTS_HELP"),
    IMPORT_FROM_RESOURCE("importFromResource", "Import from resource", "IMPORT_FROM_RESOURCE_HELP"),
    TEST_RESOURCE("testResource", "Test resource", "TEST_RESOURCE_HELP"),
    SUSPEND_TASK("suspendTask", "Suspend task", "SUSPEND_TASK_HELP"),
    RESUME_TASK("resumeTask", "Resume task", "RESUME_TASK_HELP"),
    RUN_TASK("runTask", "Run task", "RUN_TASK_HELP"),
    EXECUTE_SCRIPT("executeScript", "Execute script", "EXECUTE_SCRIPT_HELP"),
    COMPARE_OBJECT("compareObject", "Compare object", "COMPARE_OBJECT_HELP"),
    GET_LOG_SIZE("getLogSize", "Get log size", "GET_LOG_SIZE_HELP"),
    GET_LOG("getLog", "Get log", "GET_LOG_HELP"),
    RESET_CREDENTIAL("resetCredential", "Reset credential", "RESET_CREDENTIAL_HELP"),
    GET_THREADS("getThreads", "Get threads", "GET_THREADS_HELP"),
    GET_TASKS_THREADS("getTasksThreads", "Get tasks threads", "GET_TASKS_THREADS_HELP"),
    GET_TASK_THREADS("getTaskThreads", "Get task threads", "GET_TASK_THREADS_HELP"),
    GET_EXTENSION_SCHEMA("getExtensionSchema", "Get extension schema", "GET_EXTENSION_SCHEMA_HELP"),
    COMPLETE_WORK_ITEM("completeWorkItem", "Complete work item", "COMPLETE_WORK_ITEM_HELP"),
    DELEGATE_WORK_ITEM("delegateWorkItem", "Delegate work item", "DELEGATE_WORK_ITEM_HELP"),
    CLAIM_WORK_ITEM("claimWorkItem", "Claim work item", "CLAIM_WORK_ITEM_HELP"),
    RELEASE_WORK_ITEM("releaseWorkItem", "Release work item", "RELEASE_WORK_ITEM_HELP"),
    CANCEL_CASE("cancelCase", "Cancel case", "CANCEL_CASE_HELP");

    /** The local part of the corresponding action URI (used for authorizations). */
    @NotNull private final String uriLocalPart;
    @NotNull private final String label;
    @NotNull private final String description;

    RestAuthorizationAction(@NotNull String uriLocalPart, @NotNull String label, @NotNull String description) {
        this.uriLocalPart = uriLocalPart;
        this.label = label;
        this.description = description;
    }

    @NotNull public String getUri() {
        return AuthorizationConstants.NS_AUTHORIZATION_REST + "#" + uriLocalPart;
    }

    @Override
    public String getValue() {
        return getUri();
    }

    @Override
    @NotNull public String getLabel() {
        return label;
    }

    @NotNull public String getDescription() {
        return description;
    }
}
