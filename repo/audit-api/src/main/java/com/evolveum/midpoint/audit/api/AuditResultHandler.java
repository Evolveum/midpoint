/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Handler interface for {@link AuditService#searchObjectsIterative}.
 */
@FunctionalInterface
@Experimental
public interface AuditResultHandler {

    /**
     * Handle a single audit event record.
     *
     * @param eventRecord audit event record to process
     * @return true if the operation should proceed, false if it should stop
     */
    boolean handle(AuditEventRecordType eventRecord, OperationResult parentResult);
}
