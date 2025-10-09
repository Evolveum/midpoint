/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import java.util.List;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */
@Component
public class RemoveUnusedSecurityFilterPublisher {

    private static final Trace LOGGER = TraceManager.getTrace(RemoveUnusedSecurityFilterPublisher.class);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private static RemoveUnusedSecurityFilterPublisher instance;

    public void publishCustomEvent(final List<AuthModule<?>> modules) {
        LOGGER.trace("Publishing RemoveUnusedSecurityFilterEvent event. With authentication modules: " + modules);
        RemoveUnusedSecurityFilterEventImpl customSpringEvent = new RemoveUnusedSecurityFilterEventImpl(this, modules);
        applicationEventPublisher.publishEvent(customSpringEvent);
    }

    @PostConstruct
    public void afterConstruct() {
        instance = this;
    }

    public static RemoveUnusedSecurityFilterPublisher get() {
        return instance;
    }

    private static class RemoveUnusedSecurityFilterEventImpl extends RemoveUnusedSecurityFilterEvent {

        private final List<AuthModule<?>> modules;

        RemoveUnusedSecurityFilterEventImpl(Object source, List<AuthModule<?>> modules) {
            super(source);
            this.modules = modules;
        }

        @Override
        public List<AuthModule<?>> getAuthModules() {
            return modules;
        }
    }
}
