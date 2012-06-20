/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
