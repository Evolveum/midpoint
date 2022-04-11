/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
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

    public void publishCustomEvent(final MidpointAuthentication mpAuthentication) {
        LOGGER.trace("Publishing RemoveUnusedSecurityFilterEvent event. With authentication: " + mpAuthentication);
        RemoveUnusedSecurityFilterEventImpl customSpringEvent = new RemoveUnusedSecurityFilterEventImpl(this, mpAuthentication);
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

        private final MidpointAuthentication mpAuthentication;

        RemoveUnusedSecurityFilterEventImpl(Object source, MidpointAuthentication mpAuthentication) {
            super(source);
            this.mpAuthentication = mpAuthentication;
        }

        @Override
        public MidpointAuthentication getMpAuthentication() {
            return mpAuthentication;
        }
    }
}
