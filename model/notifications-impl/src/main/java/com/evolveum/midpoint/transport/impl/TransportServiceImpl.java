/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.transport.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportService;
import com.evolveum.midpoint.transport.impl.legacy.LegacyCustomTransport;
import com.evolveum.midpoint.transport.impl.legacy.LegacyFileTransport;
import com.evolveum.midpoint.transport.impl.legacy.LegacyMailTransport;
import com.evolveum.midpoint.transport.impl.legacy.LegacySimpleSmsTransport;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@Component
public class TransportServiceImpl implements TransportService {

    private static final Trace LOGGER = TraceManager.getTrace(TransportServiceImpl.class);

    private final Map<String, Transport<?>> transports = new ConcurrentHashMap<>();

    @Autowired private SystemObjectCache systemObjectCache;

    @Deprecated @Autowired private LegacyMailTransport legacyMailTransport;
    @Deprecated @Autowired private LegacySimpleSmsTransport simpleSmsTransport;
    @Deprecated @Autowired private LegacyFileTransport legacyFileTransport;
    @Deprecated @Autowired private LegacyCustomTransport legacyCustomTransport;

    @PostConstruct
    public void init() throws SchemaException {
        // TODO: Implicit legacy notifiers, but this should go in 4.6.
        registerTransport(LegacyMailTransport.NAME, legacyMailTransport);
        registerTransport(LegacySimpleSmsTransport.NAME, simpleSmsTransport);
        registerTransport(LegacyFileTransport.NAME, legacyFileTransport);
        registerTransport(LegacyCustomTransport.NAME, legacyCustomTransport);

        PrismObject<SystemConfigurationType> sysConfigObject =
                systemObjectCache.getSystemConfiguration(new OperationResult("dummy"));
        // This can be null in some tests, but @EventListener method below should take care of this.
        if (sysConfigObject != null) {
            refreshConfiguration(sysConfigObject.asObjectable());
        }
    }

    // This is also called for initial setup.
    // TODO do we need separate event when systemObjectCache is invalidated? (multi-node setup)
    @EventListener
    public void refreshTransportConfiguration(SystemConfigurationChangeEvent event) {
        refreshConfiguration(event.getSystemConfiguration());
    }

    private void refreshConfiguration(SystemConfigurationType systemConfiguration) {
        // No need to refresh old legacy transport @Components, they read config every time.

        MessageTransportConfigurationType config = systemConfiguration.getMessageTransportConfiguration();
        // TODO
    }

    @Override
    public void send(Message message, String transportName, Event event, Task task, OperationResult parentResult) {
        Transport<?> transport = getTransport(transportName);
        transport.send(message, transportName, event, task, parentResult);
    }

    @Override
    public void registerTransport(String name, Transport<?> transport) {
        LOGGER.trace("Registering notification transport {} under name {}", transport, name);
        Transport<?> oldTransport = transports.get(name);// TODO beware the duality of name vs name before ":" used in getTransport()
        if (oldTransport != null) {
            // TODO logging?
            oldTransport.destroy();
        }
        transports.put(name, transport);
    }

    // TODO should be internal and go away eventually
    // accepts name:subname (e.g. dummy:accounts) - a primitive form of passing parameters (will be enhanced/replaced in the future)
    @Override
    public Transport<?> getTransport(String name) {
        String key = name.split(":")[0];
        Transport<?> transport = transports.get(key);
        if (transport == null) {
            throw new IllegalStateException("Unknown transport named " + key);
        } else {
            return transport;
        }
    }
}
