/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import com.evolveum.midpoint.audit.api.AuditEventType;

/**
 * @author lazyman
 */
public enum RAuditEventType {

    GET_OBJECT(AuditEventType.GET_OBJECT),

    ADD_OBJECT(AuditEventType.ADD_OBJECT),

    MODIFY_OBJECT(AuditEventType.MODIFY_OBJECT),

    DELETE_OBJECT(AuditEventType.DELETE_OBJECT),

    EXECUTE_CHANGES_RAW(AuditEventType.EXECUTE_CHANGES_RAW),

    SYNCHRONIZATION(AuditEventType.SYNCHRONIZATION),

    CREATE_SESSION(AuditEventType.CREATE_SESSION),

    TERMINATE_SESSION(AuditEventType.TERMINATE_SESSION),

    WORK_ITEM(AuditEventType.WORK_ITEM),

    WORKFLOW_PROCESS_INSTANCE(AuditEventType.WORKFLOW_PROCESS_INSTANCE),

    RECONCILIATION(AuditEventType.RECONCILIATION),

    SUSPEND_TASK(AuditEventType.SUSPEND_TASK),

    RESUME_TASK(AuditEventType.RESUME_TASK),

    RUN_TASK_IMMEDIATELY(AuditEventType.RUN_TASK_IMMEDIATELY);


    private AuditEventType type;

    RAuditEventType(AuditEventType type) {
        this.type = type;
    }

    public AuditEventType getType() {
        return type;
    }


    public static RAuditEventType toRepo(AuditEventType type) {
        if (type == null) {
            return null;
        }

        for (RAuditEventType st : RAuditEventType.values()) {
            if (type.equals(st.getType())) {
                return st;
            }
        }

        throw new IllegalArgumentException("Unknown audit event type '" + type + "'.");
    }
}
