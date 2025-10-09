/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.audit.impl;

import org.apache.commons.configuration2.Configuration;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;

public class LoggerAuditServiceFactory implements AuditServiceFactory {

    @Override
    public AuditService createAuditService() {
        return new LoggerAuditServiceImpl();
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(Configuration config) {
    }
}
