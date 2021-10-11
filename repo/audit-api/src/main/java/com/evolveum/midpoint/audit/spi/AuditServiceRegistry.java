/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
