/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.audit.impl;

import org.apache.commons.configuration2.Configuration;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;

public class LoggerAuditServiceFactory implements AuditServiceFactory {

    @Override
    public AuditService getAuditService() {
        return new LoggerAuditServiceImpl();
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(Configuration config) {
    }
}
