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

import com.evolveum.midpoint.notifications.api.EventHandler;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;

/**
 *
 */
@Component
public class EventHandlerRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(EventHandlerRegistry.class);

    private Map<Class<? extends EventHandlerType>, EventHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    public <C extends EventHandlerType, E extends Event> void registerEventHandler(Class<C> configType, EventHandler<E, C> handler) {
        LOGGER.trace("Registering event handler {} for config type {}", handler, configType);
        handlers.put(configType, handler);
    }

    public boolean forwardToHandler(Event event, EventHandlerType configuration, Task task, OperationResult result) throws SchemaException {
        EventHandler<?, ?> handler = handlers.get(configuration.getClass());
        if (handler == null) {
            throw new IllegalStateException("Unknown handler for " + configuration);
        } else if (!handler.getEventType().isAssignableFrom(event.getClass())) {
            LOGGER.trace("Not forwarding event {} to handler {} because the handler does not support events of that type",
                    event, handler);
            return true;
        } else {
            //noinspection unchecked,rawtypes
            return ((EventHandler) handler).processEvent(event, configuration, task, result);
        }
    }
}
