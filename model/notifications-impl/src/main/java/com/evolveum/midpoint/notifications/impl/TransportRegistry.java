/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 *
 */
@Component
public class TransportRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(TransportRegistry.class);

    private Map<String, Transport> transports = new ConcurrentHashMap<>();

    public void registerTransport(String name, Transport transport) {
        LOGGER.trace("Registering notification transport {} under name {}", transport, name);
        transports.put(name, transport);
    }

    // accepts name:subname (e.g. dummy:accounts) - a primitive form of passing parameters (will be enhanced/replaced in the future)
    public Transport getTransport(String name) {
        String key = name.split(":")[0];
        Transport transport = transports.get(key);
        if (transport == null) {
            throw new IllegalStateException("Unknown transport named " + key);
        } else {
            return transport;
        }
    }
}
