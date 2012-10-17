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

    TERMINATE_SESSION(AuditEventType.TERMINATE_SESSION);

    private AuditEventType type;

    private RAuditEventType(AuditEventType type) {
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
