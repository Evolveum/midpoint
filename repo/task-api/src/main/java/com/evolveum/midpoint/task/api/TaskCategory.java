/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 * This concept was replaced by task archetypes in 4.1.
 */
@Deprecated // Remove in 4.2
public class TaskCategory {

    public static final String DEMO = "Demo";
    public static final String IMPORTING_ACCOUNTS = "ImportingAccounts";
    public static final String IMPORT_FROM_FILE = "ImportFromFile";
    public static final String LIVE_SYNCHRONIZATION = "LiveSynchronization";
    public static final String ASYNCHRONOUS_UPDATE = "AsynchronousUpdate";
    public static final String BULK_ACTIONS = "BulkActions";
    public static final String MOCK = "Mock";
    public static final String RECOMPUTATION = "Recomputation";
    public static final String EXECUTE_CHANGES = "ExecuteChanges";
    public static final String RECONCILIATION = "Reconciliation";
    public static final String WORKFLOW = "Workflow";
    public static final String SYSTEM = "System";
    public static final String REPORT = "Report";
    public static final String CUSTOM = "Custom";
    public static final String ACCESS_CERTIFICATION = "AccessCertification";
    public static final String CLEANUP = "Cleanup";
    public static final String UTIL = "Utility";
}
