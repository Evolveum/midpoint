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

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AuditFactory implements ApplicationContextAware, RuntimeConfiguration {

    private static final String AUDIT_CONFIGURATION = "midpoint.audit";
    private static final String CONF_AUDIT_SERVICE = "auditService";
    private static final String CONF_AUDIT_SERVICE_FACTORY = "auditServiceFactoryClass";
    private static final Trace LOGGER = TraceManager.getTrace(AuditFactory.class);
    private ApplicationContext applicationContext;
    @Autowired
    MidpointConfiguration midpointConfiguration;
    private List<AuditServiceFactory> serviceFactories = new ArrayList<>();
    private AuditService auditService;

    public void init() {
        Configuration config = getCurrentConfiguration();
        //TODO FIX CONFIGURATION, CLEANUP REALLY NEEDED
        List<SubnodeConfiguration> auditServices = ((XMLConfiguration) ((CompositeConfiguration)
                ((SubsetConfiguration) config).getParent()).getConfiguration(0))
                .configurationsAt(AUDIT_CONFIGURATION + "." + CONF_AUDIT_SERVICE);

        for (SubnodeConfiguration serviceConfig : auditServices) {
            try {
                String factoryClass = getFactoryClassName(serviceConfig);
                Class<AuditServiceFactory> clazz = (Class<AuditServiceFactory>) Class.forName(factoryClass);
                AuditServiceFactory factory = getFactory(clazz);
                factory.init(serviceConfig);

                serviceFactories.add(factory);

            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "AuditServiceFactory implementation class {} failed to " +
                        "initialize.", ex, getFactoryClassName(serviceConfig));
                throw new SystemException("AuditServiceFactory implementation class "
                        + getFactoryClassName(serviceConfig) + " failed to initialize: " + ex.getMessage(), ex);
            }
        }
    }

    private AuditServiceFactory getFactory(Class<AuditServiceFactory> clazz) {
        LOGGER.info("Getting factory '{}'", new Object[]{clazz.getName()});
        return applicationContext.getBean(clazz);
    }

    private String getFactoryClassName(Configuration config) {
        String className = config.getString(CONF_AUDIT_SERVICE_FACTORY);
        if (StringUtils.isEmpty(className)) {
            LOGGER.error("AuditServiceFactory implementation class name ({}) not found in configuration. " +
                    "Provided configuration:\n{}", new Object[]{CONF_AUDIT_SERVICE_FACTORY, config});
            throw new SystemException("AuditServiceFactory implementation class name ("
                    + CONF_AUDIT_SERVICE_FACTORY + ") not found in configuration. Provided configuration:\n"
                    + config);
        }

        return className;
    }

    public void destroy() {

    }

    public AuditService getAuditService() {
        if (auditService == null) {
            AuditServiceProxy proxy = new AuditServiceProxy();
            for (AuditServiceFactory factory : serviceFactories) {
                try {
                    AuditService service = factory.getAuditService();
                    //todo check this autowiring (check logs) how it's done
                    applicationContext.getAutowireCapableBeanFactory().autowireBean(service);

                    proxy.registerService(service);
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't get audit service from factory '{}'", ex, factory);
                    throw new SystemException(ex.getMessage(), ex);
                }
            }

            auditService = proxy;
        }

        return auditService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public String getComponentId() {
        return AUDIT_CONFIGURATION;
    }

    @Override
    public Configuration getCurrentConfiguration() {
        return midpointConfiguration.getConfiguration(AUDIT_CONFIGURATION);
    }
}
