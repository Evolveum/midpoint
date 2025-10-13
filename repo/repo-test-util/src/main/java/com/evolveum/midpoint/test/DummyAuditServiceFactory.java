/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
    public AuditService createAuditService() {
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
