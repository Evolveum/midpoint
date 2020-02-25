/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * Model constants referenced from the outside.
 * (TODO reconsider with regards to SchemaConstants)
 *
 * @author mederly
 */
public class ModelPublicConstants {

    private static final String NS_SYNCHRONIZATION_PREFIX = SchemaConstants.NS_MODEL +"/synchronization";
    public static final String NS_SYNCHRONIZATION_TASK_PREFIX = NS_SYNCHRONIZATION_PREFIX + "/task";

    public static final String DELETE_TASK_HANDLER_URI = NS_SYNCHRONIZATION_TASK_PREFIX + "/delete/handler-3";    // TODO why "synchronization"?
    public static final String REINDEX_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/reindex/handler-3";
    public static final String AUDIT_REINDEX_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/auditReindex/handler-3";
    public static final String CLEANUP_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/cleanup/handler-3";
    public static final String SHADOW_INTEGRITY_CHECK_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/shadow-integrity-check/handler-3";
    public static final String OBJECT_INTEGRITY_CHECK_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/object-integrity-check/handler-3";
    public static final String DEPRECATED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI = NS_SYNCHRONIZATION_TASK_PREFIX + "/focus-validation-scanner/handler-3";
    public static final String FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/focus-validity-scanner/handler-3";
    public static final String PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/partitioned-focus-validity-scanner/handler-3";
    public static final String PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_1 = PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI + "#1";
    public static final String PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_2 = PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI + "#2";
    public static final String TRIGGER_SCANNER_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/trigger/scanner/handler-3";
    public static final String SHADOW_REFRESH_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/shadowRefresh/handler-3";
    public static final String RECONCILIATION_TASK_HANDLER_URI = NS_SYNCHRONIZATION_TASK_PREFIX + "/reconciliation/handler-3";
    public static final String PARTITIONED_RECONCILIATION_TASK_HANDLER_URI = NS_SYNCHRONIZATION_TASK_PREFIX + "/partitioned-reconciliation/handler-3";
    public static final String PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_1 = PARTITIONED_RECONCILIATION_TASK_HANDLER_URI + "#1";
    public static final String PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_2 = PARTITIONED_RECONCILIATION_TASK_HANDLER_URI + "#2";
    public static final String PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_3 = PARTITIONED_RECONCILIATION_TASK_HANDLER_URI + "#3";
    public static final String SCRIPT_EXECUTION_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/scripting/handler-3";
    public static final String ITERATIVE_SCRIPT_EXECUTION_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/iterative-scripting/handler-3";
    public static final String EXECUTE_DELTAS_TASK_HANDLER_URI = SchemaConstants.NS_MODEL + "/execute-deltas/handler-3";
    public static final String DELETE_NOT_UPDATE_SHADOW_TASK_HANDLER_URI = NS_SYNCHRONIZATION_TASK_PREFIX + "/delete-not-updated-shadow/handler-3";
}
