/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.transport.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import jakarta.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportService;
import com.evolveum.midpoint.notifications.api.transports.TransportSupport;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeEvent;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.transport.impl.legacy.LegacyCustomTransport;
import com.evolveum.midpoint.transport.impl.legacy.LegacyFileTransport;
import com.evolveum.midpoint.transport.impl.legacy.LegacyMailTransport;
import com.evolveum.midpoint.transport.impl.legacy.LegacySimpleSmsTransport;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@Component
public class TransportServiceImpl implements TransportService {

    private static final Trace LOGGER = TraceManager.getTrace(TransportServiceImpl.class);

    private final Map<String, Transport<?>> transports = new ConcurrentHashMap<>();

    /**
     * This holds the names of the transports configured from system config object.
     * When new config is read we don't want to remove other transports registered explicitly (e.g. tests).
     */
    private final List<String> transportsFromSysConfig = new ArrayList<>();

    // injected fields for TransportSupport
    @Autowired private ApplicationContext applicationContext;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private Protector protector;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    private TransportSupport transportSupport; // initialized in post-construct

    // injected legacy transports, can go away after 4.5
    @Deprecated @Autowired private LegacyMailTransport legacyMailTransport;
    @Deprecated @Autowired private LegacySimpleSmsTransport simpleSmsTransport;
    @Deprecated @Autowired private LegacyFileTransport legacyFileTransport;
    @Deprecated @Autowired private LegacyCustomTransport legacyCustomTransport;

    @PostConstruct
    public void init() {
        transportSupport = new TransportSupport() {
            @Override
            public PrismContext prismContext() {
                return prismContext;
            }

            @Override
            public ExpressionFactory expressionFactory() {
                return expressionFactory;
            }

            @Override
            public RepositoryService repositoryService() {
                return repositoryService;
            }

            @Override
            public Protector protector() {
                return protector;
            }

            @Override
            public ApplicationContext applicationContext() {
                return applicationContext;
            }
        };

        registerLegacyTransports();
    }

    /**
     * This is also called for initial setup, no `@PostConstruct` is needed.
     * TODO do we need separate event when systemObjectCache is invalidated? (multi-node setup) NEEDS CHECK!
     */
    @EventListener
    public void refreshTransportConfiguration(SystemConfigurationChangeEvent event) {
        clearPreviousConfiguration();
        createTransports(event.getSystemConfiguration());
    }

    /** TODO: Implicit legacy notifiers, this should go in 4.6. */
    @Deprecated
    private void registerLegacyTransports() {
        registerTransport(legacyMailTransport);
        registerTransport(simpleSmsTransport);
        registerTransport(legacyFileTransport);
        registerTransport(legacyCustomTransport);
    }

    @Override
    public void send(Message message, String transportName, Event event, Task task, OperationResult parentResult) {
        Transport<?> transport = getTransport(transportName);
        transport.send(message, transportName, event, task, parentResult);
    }

    @Override
    public void registerTransport(@NotNull Transport<?> transport) {
        String name = transport.getName();
        LOGGER.trace("Registering message transport {} with name {}", transport, name);
        transports.put(name, transport);
    }

    // TODO should be internal and go away eventually
    // accepts name:subname (e.g. dummy:accounts) - a primitive form of passing parameters (will be enhanced/replaced in the future)
    @Override
    public Transport<?> getTransport(String name) {
        Transport<?> transport = transports.get(name);
        if (transport == null) {
            // TODO fallback for legacy
            String key = name.split(":")[0];
            transport = transports.get(key);
        }
        if (transport == null) {
            throw new IllegalStateException("Unknown transport named '" + name + "'");
        } else {
            return transport;
        }
    }

    private void createTransports(SystemConfigurationType systemConfiguration) {
        MessageTransportConfigurationType config = systemConfiguration.getMessageTransportConfiguration();
        if (config == null) {
            return;
        }

        config.getMail().forEach(mailConfig -> createTransport(mailConfig, MailMessageTransport::new));
        config.getSms().forEach(smsConfig -> createTransport(smsConfig, SmsMessageTransport::new));
        config.getFile().forEach(fileConfig -> createTransport(fileConfig, FileMessageTransport::new));
        config.getCustomTransport().forEach(this::createCustomTransport);
    }

    private void clearPreviousConfiguration() {
        for (String transport : transportsFromSysConfig) {
            transports.remove(transport);
        }
        transportsFromSysConfig.clear();
    }

    private void createCustomTransport(CustomTransportConfigurationType customConfig) {
        String name = customConfig.getName();
        if (name == null) {
            LOGGER.warn("CustomTransportConfigurationType without name - IGNORING: {}", customConfig);
            return;
        }
        try {
            String className = customConfig.getType();
            //noinspection unchecked
            Transport<CustomTransportConfigurationType> transport = className != null
                    ? (Transport<CustomTransportConfigurationType>) Class.forName(className).getConstructor().newInstance()
                    : new CustomMessageTransport();
            transport.configure(customConfig, transportSupport);
            registerTransport(transport);
            transportsFromSysConfig.add(transport.getName());
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOGGER.warn("CustomTransportConfigurationType creation problem, IGNORING: {}", customConfig, e);
        }
    }

    private <C extends GeneralTransportConfigurationType> void createTransport(
            C transportConfig, Supplier<Transport<C>> transportSupplier) {
        String name = transportConfig.getName();
        if (name == null) {
            LOGGER.warn("{} without name - IGNORING: {}", transportConfig.getClass().getSimpleName(), transportConfig);
            return;
        }
        Transport<C> transport = transportSupplier.get();
        transport.configure(transportConfig, transportSupport);
        registerTransport(transport);
        transportsFromSysConfig.add(transport.getName());
    }
}
