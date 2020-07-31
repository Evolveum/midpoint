/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RuntimeConfiguration;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public class AuditFactory implements RuntimeConfiguration {

    private static final String CONF_AUDIT_SERVICE = "auditService";
    private static final String CONF_AUDIT_SERVICE_FACTORY = "auditServiceFactoryClass";
    private static final Trace LOGGER = TraceManager.getTrace(AuditFactory.class);

    @Autowired private ApplicationContext applicationContext;
    @Autowired private MidpointConfiguration midpointConfiguration;

    private final List<AuditServiceFactory> serviceFactories = new ArrayList<>();

    private AuditService auditService;

    public void init() {
        Configuration config = getCurrentConfiguration();
        List<HierarchicalConfiguration<ImmutableNode>> auditServices =
                ((BaseHierarchicalConfiguration) config).configurationsAt(CONF_AUDIT_SERVICE);
        for (Configuration serviceConfig : auditServices) {
            try {
                String factoryClass = getFactoryClassName(serviceConfig);
                //noinspection unchecked
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
        LOGGER.info("Getting factory '{}'", new Object[] { clazz.getName() });
        return applicationContext.getBean(clazz);
    }

    private String getFactoryClassName(Configuration config) {
        String className = config.getString(CONF_AUDIT_SERVICE_FACTORY);
        if (StringUtils.isEmpty(className)) {
            LOGGER.error("AuditServiceFactory implementation class name ({}) not found in configuration. " +
                    "Provided configuration:\n{}", new Object[] { CONF_AUDIT_SERVICE_FACTORY, config });
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
    public String getComponentId() {
        return MidpointConfiguration.AUDIT_CONFIGURATION;
    }

    @Override
    public Configuration getCurrentConfiguration() {
        return midpointConfiguration.getConfiguration(MidpointConfiguration.AUDIT_CONFIGURATION);
    }
}
