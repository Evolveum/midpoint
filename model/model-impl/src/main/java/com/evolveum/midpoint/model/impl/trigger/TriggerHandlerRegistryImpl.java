/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.trigger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.model.api.trigger.TriggerHandler;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;

import org.springframework.stereotype.Component;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class TriggerHandlerRegistryImpl implements TriggerHandlerRegistry {

    private final Map<String, TriggerHandler> triggerHandlerMap = new ConcurrentHashMap<>();

    @Override public void register(String uri, TriggerHandler handler) {
        triggerHandlerMap.put(uri, handler);
    }

    @Override public TriggerHandler getHandler(String uri) {
        return triggerHandlerMap.get(uri);
    }
}
