/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

public enum AuditEventType {

    GET_OBJECT(),

    ADD_OBJECT(new DisplayType().icon(new IconType().cssClass("fa fa-plus").color("green")).color("green")),

    MODIFY_OBJECT(new DisplayType().icon(new IconType().cssClass("fa fa-edit").color("blue")).color("blue")),

    DELETE_OBJECT(new DisplayType().icon(new IconType().cssClass("fa fa-minus").color("red")).color("red")),

    EXECUTE_CHANGES_RAW(new DisplayType().icon(new IconType().cssClass("fa fa-edit").color("blue")).color("blue")),

    SYNCHRONIZATION(new DisplayType().icon(new IconType().cssClass("fa fa-refresh"))),
    //  ....

    /**
     * E.g. login
     */
    CREATE_SESSION(),

    /**
     * E.g. logout
     */
    TERMINATE_SESSION(),

    /**
     * Workflow actions
     */
    WORK_ITEM(new DisplayType().icon(new IconType().cssClass("fa fa-inbox"))),

    WORKFLOW_PROCESS_INSTANCE(), // TODO change to CASE

    RECONCILIATION(new DisplayType().icon(new IconType().cssClass("fa fa-exchange"))),

    SUSPEND_TASK(new DisplayType().icon(new IconType().cssClass("fa fa-pause"))),

    RESUME_TASK(new DisplayType().icon(new IconType().cssClass("fa fa-check-square"))),

    RUN_TASK_IMMEDIATELY(new DisplayType().icon(new IconType().cssClass("fa fa-play").color("green")).color("green")),

    DISCOVER_OBJECT,
    INFORMATION_DISCLOSURE;

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
            case DISCOVER_OBJECT:
                return AuditEventType.DISCOVER_OBJECT;
            case INFORMATION_DISCLOSURE:
                return AuditEventType.INFORMATION_DISCLOSURE;
            default:
                throw new IllegalArgumentException("Unknown audit event type: " + event);
        }
    }

    private DisplayType display;

    AuditEventType() {
        this(null);
    }

    AuditEventType(DisplayType display) {
        this.display = display;
    }

    public DisplayType getDisplay() {
        return display;
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
            case DISCOVER_OBJECT:
                return AuditEventTypeType.DISCOVER_OBJECT;
            case INFORMATION_DISCLOSURE:
                return AuditEventTypeType.INFORMATION_DISCLOSURE;
            default:
                throw new IllegalArgumentException("Unknown audit event type: " + event);
        }
    }
}
