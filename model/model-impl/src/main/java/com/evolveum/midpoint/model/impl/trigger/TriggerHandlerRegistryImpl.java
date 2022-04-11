/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
