/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import com.evolveum.midpoint.audit.api.AuditEventStage;

/**
 * @author lazyman
 */
public enum RAuditEventStage {

    REQUEST(AuditEventStage.REQUEST),

    EXECUTION(AuditEventStage.EXECUTION);

    private AuditEventStage stage;

    private RAuditEventStage(AuditEventStage stage) {
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
