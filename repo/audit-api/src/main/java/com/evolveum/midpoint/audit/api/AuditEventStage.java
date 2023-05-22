/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;

public enum AuditEventStage {

    REQUEST,

    EXECUTION,

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
            default:
                throw new IllegalArgumentException("Unknown audit event stage: " + stage);
        }
    }
}
