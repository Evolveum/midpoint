/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test;

import org.apache.commons.configuration.Configuration;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.audit.api.AuditServiceFactoryException;

/**
 * Factory for DummyAuditService. Only for test use.
 *
 * @author semancik
 *
 */
public class DummyAuditServiceFactory implements AuditServiceFactory {

	@Override
	public AuditService getAuditService() throws AuditServiceFactoryException {
		return DummyAuditService.getInstance();
	}

	@Override
	public void init(Configuration config) throws AuditServiceFactoryException {
		// Nothing to do
	}

	@Override
	public void destroy() throws AuditServiceFactoryException {
		// Nothing to do
	}

	@Override
	public void destroyService(AuditService service) throws AuditServiceFactoryException {
		// Nothing to do
	}

}
