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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.audit.api.AuditServiceFactoryException;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;

/**
 * @author lazyman
 */
public class SqlAuditServiceFactory implements AuditServiceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceFactory.class);
    private SqlRepositoryFactory repositoryFactory;

    public SqlRepositoryFactory getRepositoryFactory() {
        return repositoryFactory;
    }

    public void setRepositoryFactory(SqlRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    @Override
    public synchronized void destroy() throws AuditServiceFactoryException {
        LOGGER.info("Destroying Sql audit service factory.");
        try {
            repositoryFactory.destroy();
        } catch (RepositoryServiceFactoryException ex) {
            throw new AuditServiceFactoryException(ex.getMessage(), ex);
        }
        LOGGER.info("Sql audit service factory destroy complete.");
    }

    @Override
    public synchronized void init(Configuration config) throws AuditServiceFactoryException {
        LOGGER.info("Initializing Sql audit service factory.");
        try {
            repositoryFactory.init(config);
        } catch (RepositoryServiceFactoryException ex) {
            throw new AuditServiceFactoryException(ex.getMessage(), ex);
        }
        LOGGER.info("Sql audit service factory initialization complete.");
    }

    @Override
    public void destroyService(AuditService service) throws AuditServiceFactoryException {
        //we don't need destroying service objects, they will be GC correctly
    }

    @Override
    public AuditService getAuditService() throws AuditServiceFactoryException {
        return new SqlAuditServiceImpl(repositoryFactory);
    }
}
