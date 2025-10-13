/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.audit.api;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;

/**
 * Enumeration that helps distinguish phase when and where audit record was created.
 *
 * {@link AuditEventStageType#REQUEST} and {@link AuditEventStageType#EXECUTION} represent events that happened in midPoint on model layer.
 * {@link AuditEventStageType#RESOURCE} represent events that were processed on provisioning layer.
 */
public enum AuditEventStage {

    /**
     * Audit records that represent changes initiated by user.
     */
    REQUEST,

    /**
     * Audit records that represent changes computed by midPoint on model layer.
     */
    EXECUTION,

    /**
     * Stage that contains events that were recorded on provisioning level.
     * Both before and after something happened in provisioning, e.g. when auditing operations executed through provisioning
     * on managed resource as well as getting changes through live synchronization process (will be implemented a bit later).
     */
    RESOURCE;

    public static AuditEventStage fromSchemaValue(AuditEventStageType stage) {
        if (stage == null) {
            return null;
        }

        switch (stage) {
            case EXECUTION:
                return AuditEventStage.EXECUTION;
            case REQUEST:
                return AuditEventStage.REQUEST;
            case RESOURCE:
                return AuditEventStage.RESOURCE;
            default:
                throw new IllegalArgumentException("Unknown audit event stage: " + stage);
        }
    }

    public static AuditEventStageType toSchemaValue(AuditEventStage stage) {
        if (stage == null) {
            return null;
        }

        switch (stage) {
            case EXECUTION:
                return AuditEventStageType.EXECUTION;
            case REQUEST:
                return AuditEventStageType.REQUEST;
            case RESOURCE:
                return AuditEventStageType.RESOURCE;
            default:
                throw new IllegalArgumentException("Unknown audit event stage: " + stage);
        }
    }
}
