/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.AbstractSessionEvent;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionRegistryImpl;

/**
 * @author skublik
 */

public class MidpointSessionRegistry extends SessionRegistryImpl {

    private RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher;

    public MidpointSessionRegistry(RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher){
        this.removeUnusedSecurityFilterPublisher = removeUnusedSecurityFilterPublisher;
    }

    @Override
    public void onApplicationEvent(AbstractSessionEvent event) {
        super.onApplicationEvent(event);
        if (event instanceof SessionDestroyedEvent) {
            SessionDestroyedEvent sessionEvent = (SessionDestroyedEvent) event;
            for (SecurityContext context : sessionEvent.getSecurityContexts()) {
                if (context != null && context.getAuthentication() != null
                        && context.getAuthentication() instanceof MidpointAuthentication) {
                    removeUnusedSecurityFilterPublisher.publishCustomEvent((MidpointAuthentication)context.getAuthentication());
                }
            }
        }
    }
}
