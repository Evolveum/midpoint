/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.init;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
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
    @Autowired
    private PrismContext prismContext;
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
            	LOGGER.debug("Creating repository service using factory {}", factory);
                repositoryService = factory.getRepositoryService();
            } catch (RepositoryServiceFactoryException | RuntimeException ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Failed to get repository service from factory " + factory, ex);
                throw new SystemException("Failed to get repository service from factory " + factory, ex);
            } catch (Error ex) {
            	LoggingUtils.logUnexpectedException(LOGGER, "Failed to get repository service from factory " + factory, ex);
                throw ex;
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
                repositoryCache.setRepository(getRepositoryService(), prismContext);

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
