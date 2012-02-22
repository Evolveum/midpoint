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
import org.springframework.stereotype.Component;

@Component
public class RepositoryFactory implements ApplicationContextAware, RuntimeConfiguration {

    private static final String REPOSITORY_CONFIGURATION = "midpoint.repository";
    private static final String REPOSITORY_FACTORY_CLASS = "repositoryServiceFactoryClass";
    private static final Trace LOGGER = TraceManager.getTrace(RepositoryFactory.class);
    private ApplicationContext applicationContext;
    @Autowired
    MidpointConfiguration midpointConfiguration;
    RepositoryService repositoryService;
    RepositoryService cacheRepositoryService;

    public void init() {
        Configuration config = midpointConfiguration.getConfiguration(REPOSITORY_CONFIGURATION);
        try {
            RepositoryServiceFactory factory = getFactory(config);
            factory.init(config);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "RepositoryServiceFactory implementation class {} failed to " +
                    "initialize.", ex, config.getString(REPOSITORY_FACTORY_CLASS));
            throw new SystemException("RepositoryServiceFactory implementation class " +
                    config.getString(REPOSITORY_FACTORY_CLASS) + " failed to initialize.", ex);
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

    private RepositoryServiceFactory getFactory(Configuration config) throws RepositoryServiceFactoryException {
        try {
            String className = getFactoryClassName(config);
            Class<RepositoryServiceFactory> clazz = (Class<RepositoryServiceFactory>) Class.forName(className);
            return applicationContext.getBean(clazz);
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException(ex.getMessage(), ex);
        }
    }

    public void destroy() {
        try {
            Configuration config = midpointConfiguration.getConfiguration(REPOSITORY_CONFIGURATION);
            RepositoryServiceFactory factory = getFactory(config);
            factory.destroy();
        } catch (RepositoryServiceFactoryException ex) {
            LoggingUtils.logException(LOGGER, "Failed to destroy RepositoryServiceFactory", ex);
            throw new SystemException("Failed to destroy RepositoryServiceFactory", ex);
        }
    }

    @Override
    public String getComponentId() {
        return "midpoint.repository";
    }

    @Override
    public Configuration getCurrentConfiguration() {
        return midpointConfiguration.getConfiguration(REPOSITORY_CONFIGURATION);
    }

    public synchronized RepositoryService getRepositoryService() {
        if (repositoryService == null) {
            try {
                Configuration config = midpointConfiguration.getConfiguration(REPOSITORY_CONFIGURATION);
                RepositoryServiceFactory factory = getFactory(config);
                repositoryService = factory.getRepositoryService();
            } catch (RepositoryServiceFactoryException e) {
                LoggingUtils.logException(LOGGER, "Failed to get repository service from factory", e);
                throw new SystemException("Failed to get repository service from factory", e);
            }
        }
        return repositoryService;
    }

    public synchronized RepositoryService getCacheRepositoryService() {
        if (cacheRepositoryService == null) {
            try {
                ClassLoader classLoader = RepositoryFactory.class.getClassLoader();
                //FIXME: create hammer factory factory solution also for RepositoryCache, if required
                Class<RepositoryService> repositoryCacheServiceClass = (Class<RepositoryService>)
                        classLoader.loadClass("com.evolveum.midpoint.repo.cache.RepositoryCache");
                //TODO: test
                cacheRepositoryService = repositoryCacheServiceClass.getConstructor(RepositoryService.class)
                        .newInstance(getRepositoryService());
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Failed to get cache repository service. ExceptionClass = {}",
                        e, e.getClass().getName());
                throw new SystemException("Failed to get cache repository service", e);
            }
        }
        return cacheRepositoryService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
