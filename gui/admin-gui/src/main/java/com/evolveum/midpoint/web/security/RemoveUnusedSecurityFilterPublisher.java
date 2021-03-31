/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author skublik
 */

@Component
public class RemoveUnusedSecurityFilterPublisher {

    private static final Trace LOGGER = TraceManager.getTrace(RemoveUnusedSecurityFilterPublisher.class);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishCustomEvent(final MidpointAuthentication mpAuthentication) {
        LOGGER.trace("Publishing RemoveUnusedSecurityFilterEvent event. With authentication: " + mpAuthentication);
        RemoveUnusedSecurityFilterEvent customSpringEvent = new RemoveUnusedSecurityFilterEvent(this, mpAuthentication);
        applicationEventPublisher.publishEvent(customSpringEvent);
    }
}
