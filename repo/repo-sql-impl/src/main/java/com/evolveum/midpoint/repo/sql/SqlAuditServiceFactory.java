/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.audit.api.AuditServiceFactoryException;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

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
