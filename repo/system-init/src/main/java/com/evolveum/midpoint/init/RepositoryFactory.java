/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import javax.annotation.PreDestroy;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Generic factory that creates concrete {@link RepositoryServiceFactory} based on configuration.
 * This is used as a Spring factory bean, something like "RepositoryServiceFactoryFactory".
 * Created {@link RepositoryServiceFactory} is autowired and then its method
 * {@link RepositoryServiceFactory#init(Configuration)} is called.
 * This class does not care about {@link com.evolveum.midpoint.repo.api.RepositoryService} (since
 * version 4.3).
 */
public class RepositoryFactory {

    private static final Trace LOGGER = TraceManager.getTrace(RepositoryFactory.class);

    private static final String REPOSITORY_FACTORY_CLASS = "repositoryServiceFactoryClass";

    @Autowired private ApplicationContext applicationContext;
    @Autowired private MidpointConfiguration midpointConfiguration;

    private RepositoryServiceFactory repositoryServiceFactory;

    public RepositoryServiceFactory createRepositoryServiceFactory() {
        Configuration config = midpointConfiguration.getConfiguration(MidpointConfiguration.REPOSITORY_CONFIGURATION);
        try {
            String className = getFactoryClassName(config);
            LOGGER.info("Repository factory class name from configuration '{}'.", className);

            //noinspection unchecked
            Class<RepositoryServiceFactory> clazz = (Class<RepositoryServiceFactory>) Class.forName(className);
            repositoryServiceFactory = applicationContext.getAutowireCapableBeanFactory().createBean(clazz);
            repositoryServiceFactory.init(config);
            return repositoryServiceFactory;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER,
                    "RepositoryServiceFactory implementation class {} failed to initialize.",
                    ex, config.getString(REPOSITORY_FACTORY_CLASS));
            throw new SystemException("RepositoryServiceFactory implementation class " +
                    config.getString(REPOSITORY_FACTORY_CLASS) + " failed to initialize: " + ex.getMessage(), ex);
        }
    }

    private String getFactoryClassName(Configuration config) {
        String className = config.getString(REPOSITORY_FACTORY_CLASS);
        if (StringUtils.isEmpty(className)) {
            LOGGER.error("RepositoryServiceFactory implementation class name ({}) not found in configuration. " +
                    "Provided configuration:\n{}", REPOSITORY_FACTORY_CLASS, config);
            throw new SystemException("RepositoryServiceFactory implementation class name (" + REPOSITORY_FACTORY_CLASS
                    + ") not found in configuration. Provided configuration:\n" + config);
        }

        return className;
    }

    /**
     * Calls {@link RepositoryServiceFactory#destroy()} to assure proper repository shutdown.
     */
    @PreDestroy
    public void destroy() {
        try {
            repositoryServiceFactory.destroy();
        } catch (RepositoryServiceFactoryException ex) {
            LoggingUtils.logException(LOGGER, "Failed to destroy RepositoryServiceFactory", ex);
            throw new SystemException("Failed to destroy RepositoryServiceFactory", ex);
        }
    }
}
