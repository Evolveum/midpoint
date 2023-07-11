/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.task.ActivityPath;

/**
 * Model constants referenced from the outside.
 * (TODO reconsider with regards to SchemaConstants)
 */
public class ModelPublicConstants {

    //not sure if this is correct place
    public static final String CLUSTER_REPORT_FILE_PATH = "/reportFiles";
    public static final String CLUSTER_REPORT_FILE_FILENAME_PARAMETER = "filename";

    public static final String RECONCILIATION_OPERATION_COMPLETION_ID = "operationCompletion";
    public static final String RECONCILIATION_RESOURCE_OBJECTS_PREVIEW_ID = "resourceObjectsPreview";
    public static final String RECONCILIATION_RESOURCE_OBJECTS_ID = "resourceObjects";
    public static final String RECONCILIATION_REMAINING_SHADOWS_PREVIEW_ID = "remainingShadowsPreview";
    public static final String RECONCILIATION_REMAINING_SHADOWS_ID = "remainingShadows";

    public static final ActivityPath RECONCILIATION_OPERATION_COMPLETION_PATH = ActivityPath.fromId(RECONCILIATION_OPERATION_COMPLETION_ID);
    public static final ActivityPath RECONCILIATION_RESOURCE_OBJECTS_PATH = ActivityPath.fromId(RECONCILIATION_RESOURCE_OBJECTS_ID);
    public static final ActivityPath RECONCILIATION_REMAINING_SHADOWS_PATH = ActivityPath.fromId(RECONCILIATION_REMAINING_SHADOWS_ID);

    public static final String FOCUS_VALIDITY_SCAN_FULL_ID = "full";
    public static final String FOCUS_VALIDITY_SCAN_OBJECTS_ID = "objects";
    public static final String FOCUS_VALIDITY_SCAN_ASSIGNMENTS_ID = "assignments";

    public static final ActivityPath FOCUS_VALIDITY_SCAN_FULL_PATH = ActivityPath.fromId(FOCUS_VALIDITY_SCAN_FULL_ID);
    public static final ActivityPath FOCUS_VALIDITY_SCAN_OBJECTS_PATH = ActivityPath.fromId(FOCUS_VALIDITY_SCAN_OBJECTS_ID);
    public static final ActivityPath FOCUS_VALIDITY_SCAN_ASSIGNMENTS_PATH = ActivityPath.fromId(FOCUS_VALIDITY_SCAN_ASSIGNMENTS_ID);

    // Trigger handlers
    public static final String NS_MODEL_TRIGGER_PREFIX = SchemaConstants.NS_MODEL + "/trigger";
    public static final String UNLOCK_TRIGGER_HANDLER_URI = NS_MODEL_TRIGGER_PREFIX + "/unlock/handler-3";
}
