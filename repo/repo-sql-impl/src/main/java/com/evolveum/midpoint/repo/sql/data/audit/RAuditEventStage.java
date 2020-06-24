/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import com.evolveum.midpoint.audit.api.AuditEventStage;

/**
 * @author lazyman
 */
public enum RAuditEventStage {

    REQUEST(AuditEventStage.REQUEST),

    EXECUTION(AuditEventStage.EXECUTION);

    private final AuditEventStage stage;

    RAuditEventStage(AuditEventStage stage) {
        this.stage = stage;
    }

    public AuditEventStage getStage() {
        return stage;
    }

    public static RAuditEventStage toRepo(AuditEventStage stage) {
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
}
