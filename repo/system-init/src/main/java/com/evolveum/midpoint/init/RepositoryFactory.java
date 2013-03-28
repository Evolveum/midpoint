/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Igor Farinic
 * Portions Copyrighted 2011 Peter Prochazka
 * * Portions Copyrighted 2012 Viliam Repan
 */
package com.evolveum.midpoint.init;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


public class RepositoryFactory implements ApplicationContextAware, RuntimeConfiguration {

    private static final String REPOSITORY_CONFIGURATION = "midpoint.repository";
    private static final String REPOSITORY_FACTORY_CLASS = "repositoryServiceFactoryClass";
    private static final String REPOSITORY_FACTORY_CACHE_CLASS = "com.evolveum.midpoint.repo.cache.RepositoryCacheFactory";
    private static final Trace LOGGER = TraceManager.getTrace(RepositoryFactory.class);
    private ApplicationContext applicationContext;
    @Autowired
    MidpointConfiguration midpointConfiguration;
    //Repository factory
    private RepositoryServiceFactory factory;
    private RepositoryServiceFactory cacheFactory;
    //Repository services
    private RepositoryService repositoryService;
    private RepositoryService cacheRepositoryService;

    public void init() {
        Configuration config = midpointConfiguration.getConfiguration(REPOSITORY_CONFIGURATION);
        try {
            String className = getFactoryClassName(config);
            LOGGER.info("Repository factory class name from configuration '{}'.", new Object[]{className});

            Class<RepositoryServiceFactory> clazz = (Class<RepositoryServiceFactory>) Class.forName(className);
            factory = getFactoryBean(clazz);
            factory.init(config);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "RepositoryServiceFactory implementation class {} failed to " +
                    "initialize.", ex, config.getString(REPOSITORY_FACTORY_CLASS));
            throw new SystemException("RepositoryServiceFactory implementation class " +
                    config.getString(REPOSITORY_FACTORY_CLASS) + " failed to initialize: " + ex.getMessage(), ex);
        }
    }

    private String getFactoryClassName(Configuration config) {
        String className = config.getString(REPOSITORY_FACTORY_CLASS);
        if (StringUtils.isEmpty(className)) {
            LOGGER.error("RepositoryServiceFactory implementation class name ({}) not found in configuration. " +
                    "Provided configuration:\n{}", new Object[]{REPOSITORY_FACTORY_CLASS, config});
            throw new SystemException("RepositoryServiceFactory implementation class name (" + REPOSITORY_FACTORY_CLASS
                    + ") not found in configuration. Provided configuration:\n" + config);
        }

        return className;
    }

    private RepositoryServiceFactory getFactoryBean(Class<RepositoryServiceFactory> clazz) throws
            RepositoryServiceFactoryException {
        LOGGER.info("Getting factory bean '{}'", new Object[]{clazz.getName()});
        return applicationContext.getBean(clazz);
    }

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
        return REPOSITORY_CONFIGURATION;
    }

    @Override
    public Configuration getCurrentConfiguration() {
        return midpointConfiguration.getConfiguration(REPOSITORY_CONFIGURATION);
    }

    public synchronized RepositoryService getRepositoryService() {
        if (repositoryService == null) {
            try {
                repositoryService = factory.getRepositoryService();
            } catch (RepositoryServiceFactoryException ex) {
                LoggingUtils.logException(LOGGER, "Failed to get repository service from factory", ex);
                throw new SystemException("Failed to get repository service from factory", ex);
            }
        }
        return repositoryService;
    }

    public RepositoryServiceFactory getFactory() {
        return factory;
    }

    public synchronized RepositoryService getCacheRepositoryService() {
        if (cacheRepositoryService == null) {
            try {
                Class<RepositoryServiceFactory> clazz = (Class<RepositoryServiceFactory>) Class.forName(REPOSITORY_FACTORY_CACHE_CLASS);
                cacheFactory = getFactoryBean(clazz);
                //TODO decompose this dependency, remove class casting !!!
                RepositoryCache repositoryCache = (RepositoryCache) cacheFactory.getRepositoryService();
                repositoryCache.setRepository(getRepositoryService());

                cacheRepositoryService = repositoryCache;
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "Failed to get cache repository service. ExceptionClass = {}",
                        ex, ex.getClass().getName());
                throw new SystemException("Failed to get cache repository service", ex);
            }
        }
        return cacheRepositoryService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
