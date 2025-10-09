/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;

/**
 * Used for non-invasive observing audit records being emitted.
 */
public interface DummyAuditEventListener {

    void onAudit(AuditEventRecord record);
}
