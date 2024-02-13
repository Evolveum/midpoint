/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.api;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a REST method callable by general public, primarily for authorizing its use.
 *
 * Methods protected by other means (e.g., by intra-cluster authentication) need not be listed here.
 */
public enum RestMethod {

    GENERATE_VALUE("generateValue"),
    RPC_GENERATE_VALUE("rpcGenerateValue"),
    VALIDATE_VALUE("validateValue"),
    RPC_VALIDATE_VALUE("rpcValidateValue"),
    GET_VALUE_POLICY("getValuePolicy"),
    GET_OBJECT("getObject"),
    GET_SELF("getSelf"),
    POST_OBJECT("postObject"),
    PUT_OBJECT("putObject"),
    GET_OBJECTS("getObjects"),
    DELETE_OBJECT("deleteObject"),
    MODIFY_OBJECT("modifyObject"),
    NOTIFY_CHANGE("notifyChange"),
    FIND_SHADOW_OWNER("findShadowOwner"),
    IMPORT_SHADOW("importShadow"),
    SEARCH_OBJECTS("searchObjects"),
    IMPORT_FROM_RESOURCE("importFromResource"),
    TEST_RESOURCE("testResource"),
    SUSPEND_TASK("suspendTask"),
    RESUME_TASK("resumeTask"),
    RUN_TASK("runTask"),
    EXECUTE_SCRIPT("executeScript"),
    COMPARE_OBJECT("compareObject"),
    GET_LOG_SIZE("getLogSize"),
    GET_LOG("getLog"),
    RESET_CREDENTIAL("resetCredential"),
    GET_THREADS("getThreads"),
    GET_TASKS_THREADS("getTasksThreads"),
    GET_TASK_THREADS("getTaskThreads"),
    GET_EXTENSION_SCHEMA("getExtensionSchema");

    @NotNull private final String localUriPart;

    RestMethod(@NotNull String localUriPart) {
        this.localUriPart = localUriPart;
    }

    @NotNull public String getActionUri() {
        return AuthorizationConstants.NS_AUTHORIZATION_REST + "#" + localUriPart;
    }

    @NotNull public String getLabel() {
        return "RestEndpoint.authRest." + localUriPart + ".label";
    }

    @NotNull public String getDescription() {
        return "RestEndpoint.authRest." + localUriPart + ".description";
    }
}
