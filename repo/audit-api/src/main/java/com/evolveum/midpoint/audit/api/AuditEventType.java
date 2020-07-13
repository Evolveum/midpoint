/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;

public enum AuditEventType {

    GET_OBJECT,

    ADD_OBJECT,

    MODIFY_OBJECT,

    DELETE_OBJECT,

    EXECUTE_CHANGES_RAW,

    SYNCHRONIZATION,
    //  ....

    /**
     * E.g. login
     */
    CREATE_SESSION,

    /**
     * E.g. logout
     */
    TERMINATE_SESSION,

    /**
     * Workflow actions
     */
    WORK_ITEM,

    WORKFLOW_PROCESS_INSTANCE,

    RECONCILIATION,

    SUSPEND_TASK,

    RESUME_TASK,

    RUN_TASK_IMMEDIATELY;

    public static AuditEventType fromSchemaValue(AuditEventTypeType event) {
        if (event == null) {
            return null;
        }
        switch (event) {
            case ADD_OBJECT:
                return AuditEventType.ADD_OBJECT;
            case CREATE_SESSION:
                return AuditEventType.CREATE_SESSION;
            case DELETE_OBJECT:
                return AuditEventType.DELETE_OBJECT;
            case EXECUTE_CHANGES_RAW:
                return AuditEventType.EXECUTE_CHANGES_RAW;
            case GET_OBJECT:
                return AuditEventType.GET_OBJECT;
            case MODIFY_OBJECT:
                return AuditEventType.MODIFY_OBJECT;
            case RECONCILIATION:
                return AuditEventType.RECONCILIATION;
            case SYNCHRONIZATION:
                return AuditEventType.SYNCHRONIZATION;
            case TERMINATE_SESSION:
                return AuditEventType.TERMINATE_SESSION;
            case WORK_ITEM:
                return AuditEventType.WORK_ITEM;
            case WORKFLOW_PROCESS_INSTANCE:
                return AuditEventType.WORKFLOW_PROCESS_INSTANCE;
            case SUSPEND_TASK:
                return AuditEventType.SUSPEND_TASK;
            case RESUME_TASK:
                return AuditEventType.RESUME_TASK;
            case RUN_TASK_IMMEDIATELY:
                return AuditEventType.RUN_TASK_IMMEDIATELY;
            default:
                throw new IllegalArgumentException("Unknown audit event type: " + event);
        }
    }

    public static AuditEventTypeType toSchemaValue(AuditEventType event) {
        if (event == null) {
            return null;
        }
        switch (event) {
            case ADD_OBJECT:
                return AuditEventTypeType.ADD_OBJECT;
            case CREATE_SESSION:
                return AuditEventTypeType.CREATE_SESSION;
            case DELETE_OBJECT:
                return AuditEventTypeType.DELETE_OBJECT;
            case EXECUTE_CHANGES_RAW:
                return AuditEventTypeType.EXECUTE_CHANGES_RAW;
            case GET_OBJECT:
                return AuditEventTypeType.GET_OBJECT;
            case MODIFY_OBJECT:
                return AuditEventTypeType.MODIFY_OBJECT;
            case RECONCILIATION:
                return AuditEventTypeType.RECONCILIATION;
            case SYNCHRONIZATION:
                return AuditEventTypeType.SYNCHRONIZATION;
            case TERMINATE_SESSION:
                return AuditEventTypeType.TERMINATE_SESSION;
            case WORK_ITEM:
                return AuditEventTypeType.WORK_ITEM;
            case WORKFLOW_PROCESS_INSTANCE:
                return AuditEventTypeType.WORKFLOW_PROCESS_INSTANCE;
            case SUSPEND_TASK:
                return AuditEventTypeType.SUSPEND_TASK;
            case RESUME_TASK:
                return AuditEventTypeType.RESUME_TASK;
            case RUN_TASK_IMMEDIATELY:
                return AuditEventTypeType.RUN_TASK_IMMEDIATELY;
            default:
                throw new IllegalArgumentException("Unknown audit event type: " + event);
        }
    }
}
