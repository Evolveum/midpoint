/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.task.api.TaskHandler;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains registered task handlers.
 *
 * TODO finish review of this class
 */
@Component
public class TaskHandlerRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(TaskHandlerRegistry.class);

    /** Task handlers mapped from their URIs. */
    private final Map<String, TaskHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Handler URI to be used if no URI is specified in the task.
     */
    private String defaultHandlerUri;

    public void registerHandler(@NotNull String uri, @NotNull TaskHandler handler) {
        LOGGER.trace("Registering task handler for URI {}", uri);
        handlers.put(uri, handler);
    }

    void unregisterHandler(@NotNull String uri) {
        LOGGER.trace("Unregistering task handler for {}", uri);
        handlers.remove(uri);
    }

    public TaskHandler getHandler(String uri) {
        String effectiveUri = uri != null ? uri : defaultHandlerUri;
        if (effectiveUri != null) {
            return handlers.get(effectiveUri);
        } else {
            return null;
        }
    }

    void setDefaultHandlerUri(String defaultHandlerUri) {
        this.defaultHandlerUri = defaultHandlerUri;
    }
}
