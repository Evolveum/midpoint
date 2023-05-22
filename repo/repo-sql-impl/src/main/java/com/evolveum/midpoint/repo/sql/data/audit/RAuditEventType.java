/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;

public enum RAuditEventType implements SchemaEnum<AuditEventTypeType> {

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

    RUN_TASK_IMMEDIATELY(AuditEventType.RUN_TASK_IMMEDIATELY),

    DISCOVER_OBJECT(AuditEventType.DISCOVER_OBJECT);

    private final AuditEventType type;

    RAuditEventType(AuditEventType type) {
        this.type = type;
    }

    public AuditEventType getType() {
        return type;
    }

    @Override
    public AuditEventTypeType getSchemaValue() {
        return AuditEventType.toSchemaValue(type);
    }

    public static RAuditEventType from(AuditEventType type) {
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

    public static @Nullable RAuditEventType fromSchemaValue(@Nullable AuditEventTypeType type) {
        return type != null
                ? from(AuditEventType.fromSchemaValue(type))
                : null;
    }
}
