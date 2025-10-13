/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.audit.spi;

import com.evolveum.midpoint.audit.api.AuditService;

/**
 * @author lazyman
 */
public interface AuditServiceRegistry {

    void registerService(AuditService service);

    void unregisterService(AuditService service);
}
