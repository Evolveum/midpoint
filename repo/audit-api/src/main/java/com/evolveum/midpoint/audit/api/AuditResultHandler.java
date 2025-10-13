/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.audit.api;

import com.evolveum.midpoint.schema.ContainerableResultHandler;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Handler interface for {@link AuditService#searchObjectsIterative}.
 */
@FunctionalInterface
@Experimental
public interface AuditResultHandler extends ContainerableResultHandler<AuditEventRecordType> {

}
