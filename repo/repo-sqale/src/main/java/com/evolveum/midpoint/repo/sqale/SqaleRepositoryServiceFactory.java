/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import org.apache.commons.configuration2.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryServiceFactory;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class SqaleRepositoryServiceFactory implements JdbcRepositoryServiceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SqaleRepositoryServiceFactory.class);

    // filled-in by spring when SqaleRepositoryBeanConfig does its job
    @Autowired
    private SqlRepoContext sqlRepoContext;

    private SqaleRepositoryService repositoryService;

    @Override
    public void init(Configuration configuration) throws RepositoryServiceFactoryException {
        LOGGER.info("SqaleRepositoryServiceFactory is going to be initialized");
        // TODO
    }

    @Override
    public RepositoryService createRepositoryService() {
        if (repositoryService == null) {
            repositoryService = new SqaleRepositoryService(sqlRepoContext);
        }
        return repositoryService;
    }

    @Override
    public void destroy() throws RepositoryServiceFactoryException {
        // TODO destroy perf monitor? SqlBaseService.destroy for inspiration
    }

    @Override
    public JdbcRepositoryConfiguration getConfiguration() {
        return null; // TODO
    }
}
