/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.audit.api;

import org.apache.commons.configuration.Configuration;

/**
 * @author lazyman
 */
public interface AuditServiceFactory {

    void init(Configuration config) throws AuditServiceFactoryException;

    void destroy() throws AuditServiceFactoryException;

    void destroyService(AuditService service) throws AuditServiceFactoryException;

    AuditService getAuditService() throws AuditServiceFactoryException;
}
