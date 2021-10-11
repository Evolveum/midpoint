/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.result;

/**
 * @author Radovan Semancik
 *
 */
public class OperationConstants {

    public static final String PREFIX = "com.evolveum.midpoint.common.operation";

    public static final String LIVE_SYNC = PREFIX + ".liveSync";
    public static final String LIVE_SYNC_STATISTICS = PREFIX + ".liveSync.statistics";
    public static final String ASYNC_UPDATE = PREFIX + ".asyncUpdate";
    public static final String ASYNC_UPDATE_STATISTICS = PREFIX + ".asyncUpdate.statistics";
    public static final String RECONCILIATION = PREFIX + ".reconciliation";
    public static final String RECONCILE_ACCOUNT = PREFIX + ".reconciliation.account";
    public static final String RECOMPUTE = PREFIX + ".recompute";
    public static final String RECOMPUTE_USER = PREFIX + ".recompute.user";
    public static final String RECOMPUTE_STATISTICS = PREFIX + ".recompute.statistics";
    public static final String CLEANUP = PREFIX + ".cleanup";

    public static final String EXECUTE = PREFIX + ".execute";
    public static final String EXECUTE_SCRIPT = PREFIX + ".executeScript";

    public static final String IMPORT_ACCOUNTS_FROM_RESOURCE = PREFIX + ".import.accountsFromResource";
    public static final String IMPORT_ACCOUNTS_FROM_RESOURCE_STATISTICS = PREFIX + ".import.accountsFromResource.statistics";
    public static final String IMPORT_OBJECTS_FROM_FILE = PREFIX + ".import.objectsFromFile";
    public static final String IMPORT_OBJECTS_FROM_CAMEL = PREFIX + ".import.objectsFromCamel";
    public static final String IMPORT_OBJECTS_FROM_STREAM = PREFIX + ".import.objectsFromStream";
    public static final String IMPORT_OBJECT = PREFIX + ".import.object";

    public static final String FOCUS_VALIDITY_SCAN = PREFIX + ".focusValidityScan";
    public static final String TRIGGER_SCAN = PREFIX + ".triggerScan";

    public static final String CREATE_REPORT_FILE = PREFIX + ".createReportFile";

    public static final String CHECK_SHADOW_INTEGRITY = PREFIX + ".checkShadowIntegrity";
    public static final String CHECK_OBJECT_INTEGRITY = PREFIX + ".checkObjectIntegrity";
    public static final String REINDEX = PREFIX + ".reindex";
    public static final String AUDIT_REINDEX = PREFIX + ".auditReindex";
    public static final String SHADOW_REFRESH = PREFIX + ".shadowRefresh";

    public static final String PROVISIONING_PROPAGATION = "com.evolveum.midpoint.provisioning.propagation";

    public static final String OPERATION_SEARCH_RESULT = "com.evolveum.midpoint.schema.result.searchResult";

    public static final String DELETE_NOT_UPDATED_SHADOWS = PREFIX + ".delNotUpdatedShadows";
}
