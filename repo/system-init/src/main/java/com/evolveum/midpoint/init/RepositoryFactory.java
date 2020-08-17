/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class RepositoryFactory implements RuntimeConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(RepositoryFactory.class);

    private static final String REPOSITORY_FACTORY_CLASS = "repositoryServiceFactoryClass";

    @Autowired private ApplicationContext applicationContext;
    @Autowired private MidpointConfiguration midpointConfiguration;

    private RepositoryServiceFactory factory;
    private RepositoryService repositoryService;

    @PostConstruct
    public void init() {
        Configuration config = midpointConfiguration.getConfiguration(MidpointConfiguration.REPOSITORY_CONFIGURATION);
        try {
            String className = getFactoryClassName(config);
            LOGGER.info("Repository factory class name from configuration '{}'.", className);

            //noinspection unchecked
            Class<RepositoryServiceFactory> clazz = (Class<RepositoryServiceFactory>) Class.forName(className);
            factory = getFactoryBean(clazz);
            factory.init(config);
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

    private RepositoryServiceFactory getFactoryBean(Class<RepositoryServiceFactory> clazz) {
        LOGGER.info("Getting factory bean '{}'", clazz.getName());
        return applicationContext.getBean(clazz);
    }

    @PreDestroy
    public void destroy() {
        try {
            if (factory != null) {
                factory.destroy();
            }
        } catch (RepositoryServiceFactoryException ex) {
            LoggingUtils.logException(LOGGER, "Failed to destroy RepositoryServiceFactory", ex);
            throw new SystemException("Failed to destroy RepositoryServiceFactory", ex);
        }
    }

    @Override
    public String getComponentId() {
        return MidpointConfiguration.REPOSITORY_CONFIGURATION;
    }

    @Override
    public Configuration getCurrentConfiguration() {
        return midpointConfiguration.getConfiguration(MidpointConfiguration.REPOSITORY_CONFIGURATION);
    }

    public synchronized RepositoryService createRepositoryService() {
        if (repositoryService != null) {
            return repositoryService;
        }

        try {
            LOGGER.debug("Creating repository service using factory {}", factory);
            repositoryService = factory.createRepositoryService();
        } catch (RepositoryServiceFactoryException | RuntimeException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to get repository service from factory " + factory, ex);
            throw new SystemException("Failed to get repository service from factory " + factory, ex);
        } catch (Error ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to get repository service from factory " + factory, ex);
            throw ex;
        }

        return repositoryService;
    }
}
