/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test;

import org.apache.commons.configuration2.Configuration;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;

/**
 * Factory for DummyAuditService. Only for test use.
 *
 * @author semancik
 */
public class DummyAuditServiceFactory implements AuditServiceFactory {

    @Override
    public AuditService getAuditService() {
        return DummyAuditService.getInstance();
    }

    @Override
    public void init(Configuration config) {
        // Nothing to do
    }

    @Override
    public void destroy() {
        // Nothing to do
    }
}
