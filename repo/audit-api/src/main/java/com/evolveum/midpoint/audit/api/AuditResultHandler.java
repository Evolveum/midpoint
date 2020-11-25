/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

public interface AuditResultHandler {

    @Deprecated
    default boolean handle(AuditEventRecord auditRecord){
        return true;
    }

    boolean handle(AuditEventRecordType auditRecord);

    int getProgress();
}
