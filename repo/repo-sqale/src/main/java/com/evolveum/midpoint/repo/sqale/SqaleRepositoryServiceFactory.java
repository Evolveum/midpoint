/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import org.apache.commons.configuration2.Configuration;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class SqaleRepositoryServiceFactory implements RepositoryServiceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SqaleRepositoryServiceFactory.class);

    @Override
    public void init(Configuration configuration) throws RepositoryServiceFactoryException {
        LOGGER.info("SqaleRepositoryServiceFactory is going to be initialized");
    }

    @Override
    public RepositoryService createRepositoryService() throws RepositoryServiceFactoryException {
        LOGGER.info("SqaleRepositoryServiceFactory: creating repository service");
        return null;
    }

    @Override
    public void destroy() throws RepositoryServiceFactoryException {
        LOGGER.info("SqaleRepositoryServiceFactory: destroy");
    }
}
