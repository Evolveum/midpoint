/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.trigger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class TriggerHandlerRegistry {

    private final Map<String, TriggerHandler> triggerHandlerMap = new ConcurrentHashMap<>();

    public void register(String uri, TriggerHandler handler) {
        triggerHandlerMap.put(uri, handler);
    }

    public TriggerHandler getHandler(String uri) {
        return triggerHandlerMap.get(uri);
    }
}
