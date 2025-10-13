/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;

public enum RAuditEventStage implements SchemaEnum<AuditEventStageType> {

    REQUEST(AuditEventStage.REQUEST),

    EXECUTION(AuditEventStage.EXECUTION),

    RESOURCE(AuditEventStage.RESOURCE);

    private final AuditEventStage stage;

    RAuditEventStage(AuditEventStage stage) {
        this.stage = stage;
        RUtil.register(this);
    }

    public AuditEventStage getStage() {
        return stage;
    }

    @Override
    public AuditEventStageType getSchemaValue() {
        return AuditEventStage.toSchemaValue(stage);
    }

    public static RAuditEventStage from(AuditEventStage stage) {
        if (stage == null) {
            return null;
        }

        for (RAuditEventStage st : RAuditEventStage.values()) {
            if (stage.equals(st.getStage())) {
                return st;
            }
        }

        throw new IllegalArgumentException("Unknown audit event stage '" + stage + "'.");
    }

    public static RAuditEventStage fromSchemaValue(AuditEventStageType stage) {
        return stage != null
                ? from(AuditEventStage.fromSchemaValue(stage))
                : null;
    }
}
