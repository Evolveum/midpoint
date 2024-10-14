/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

/**
 * Type for container stored in database, used for {@link MContainer#containerType}.
 * Analog of custom PG enum type `ContainerType`, not used in the code, it is automatically filled-in by the database.
 */
public enum MContainerType {
    ACCESS_CERTIFICATION_CASE,
    ACCESS_CERTIFICATION_WORK_ITEM,

    AFFECTED_RESOURCE_OBJECTS,
    AFFECTED_OBJECTS,
    ASSIGNMENT,
    CASE_WORK_ITEM,
    CLUSTER_DETECTED_PATTERN,
    FOCUS_IDENTITY,
    INDUCEMENT, // also represented by AssignmentType,
    LOOKUP_TABLE_ROW,
    OPERATION_EXECUTION,
    OUTLIER_PARTITION,
    SIMULATION_RESULT_PROCESSED_OBJECT,
    TRIGGER,
}
