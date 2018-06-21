/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
