/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Audit factory is a managed component that hides multiple actual {@link AuditServiceFactory}
 * components - and {@link AuditService}s they create - behind a single proxy implementation.
 * This is actually "Audit Service Proxy Factory", the service is returned by {@link #createAuditService()}.
 * This component takes care of "midpoint/audit" part of the config XML and "auditService" elements there.
 * <p>
 * Initialization process uses configured managed components (Spring beans) of type {@link AuditServiceFactory}.
 * Audit service factory declared in the config is used, the rest is not.
 * This means the factory class can't do anything disruptive as part of its bean initialization.
 * Actual {@link AuditService} is created as non-managed component but Spring is asked to autowire
 * its dependencies, this means that there is no Spring way how to say the dependencies in advance.
 * This means that at this time all the used dependencies must be initialized already, which can be
 * taken care of by the service's factory, e.g. factory autowiring the field needed by the service,
 * even if the factory itself doesn't need it.
 * <p>
 * While technically this creates {@link AuditService}, hence it is a factory (see its Spring
 * configuration), it is NOT part of {@link AuditServiceFactory} hierarchy.
 */
public class AuditFactory {

    private static final Trace LOGGER = TraceManager.getTrace(AuditFactory.class);

    public static final String CONF_AUDIT_SERVICE = "auditService";
    public static final String CONF_AUDIT_SERVICE_FACTORY = "auditServiceFactoryClass";

    @Autowired private AutowireCapableBeanFactory autowireCapableBeanFactory;
    @Autowired private MidpointConfiguration midpointConfiguration;
    @Autowired private List<AuditServiceFactory> availableServiceFactories;

    private final List<AuditServiceFactory> serviceFactories = new ArrayList<>();

    private AuditService auditService;

    @PostConstruct
    public void init() {
        Configuration config =
                midpointConfiguration.getConfiguration(MidpointConfiguration.AUDIT_CONFIGURATION);
        List<HierarchicalConfiguration<ImmutableNode>> auditServices =
                ((BaseHierarchicalConfiguration) config).configurationsAt(CONF_AUDIT_SERVICE);
        for (Configuration serviceConfig : auditServices) {
            String factoryClass = getFactoryClassName(serviceConfig);
            try {
                //noinspection unchecked
                Class<AuditServiceFactory> clazz =
                        (Class<AuditServiceFactory>) Class.forName(factoryClass);
                AuditServiceFactory factory = getFactory(clazz);
                factory.init(serviceConfig);

                serviceFactories.add(factory);
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER,
                        "AuditServiceFactory implementation class {} failed to initialize.",
                        ex, factoryClass);
                throw new SystemException("AuditServiceFactory implementation class "
                        + factoryClass + " failed to initialize: " + ex.getMessage(), ex);
            }
        }
    }

    private AuditServiceFactory getFactory(Class<AuditServiceFactory> clazz) {
        for (AuditServiceFactory candidate : availableServiceFactories) {
            // class equality or == is not enough to match subclasses (e.g. in test)
            if (clazz.isAssignableFrom(candidate.getClass())) {
                LOGGER.info("Getting factory '{}'", clazz.getName());
                return candidate;
            }
        }
        throw new SystemException("Couldn't find AuditServiceFactory for class " + clazz);
    }

    private String getFactoryClassName(Configuration config) {
        String className = config.getString(CONF_AUDIT_SERVICE_FACTORY);
        if (StringUtils.isEmpty(className)) {
            LOGGER.error("AuditServiceFactory implementation class name ({}) not found in configuration. " +
                    "Provided configuration:\n{}", CONF_AUDIT_SERVICE_FACTORY, config);
            throw new SystemException("AuditServiceFactory implementation class name ("
                    + CONF_AUDIT_SERVICE_FACTORY + ") not found in configuration. Provided configuration:\n"
                    + config);
        }

        return className;
    }

    public synchronized AuditService createAuditService() {
        if (auditService == null) {
            AuditServiceProxy proxy = new AuditServiceProxy();
            for (AuditServiceFactory factory : serviceFactories) {
                try {
                    AuditService service = factory.createAuditService();
                    autowireCapableBeanFactory.autowireBean(service);
                    proxy.registerService(service);
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER,
                            "Couldn't get audit service from factory '{}'", ex, factory);
                    throw new SystemException(ex.getMessage(), ex);
                }
            }

            auditService = proxy;
        }

        return auditService;
    }
}
