/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

/**
 * Type for container stored in database, used for {@link MContainer#containerType}.
 */
public enum MContainerType {
    ACCESS_CERTIFICATION_CASE,
    ACCESS_CERTIFICATION_WORK_ITEM,
    ASSIGNMENT,
    INDUCEMENT, // also represented by AssignmentType
    LOOKUP_TABLE_ROW,
    OPERATION_EXECUTION,
    TRIGGER
}
