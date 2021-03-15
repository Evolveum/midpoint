/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

/**
 * @author skublik
 */

public class MidpointSecurityContext implements SecurityContext {

    private SecurityContext securityContext;
    private RemoveUnusedSecurityFilterPublisher publisher;

    public MidpointSecurityContext (SecurityContext securityContext, RemoveUnusedSecurityFilterPublisher publisher) {
        this.securityContext = securityContext;
        this.publisher = publisher;
    }

    @Override
    public Authentication getAuthentication() {
        return securityContext.getAuthentication();
    }

    @Override
    public void setAuthentication(Authentication authentication) {
        if (getAuthentication() instanceof MidpointAuthentication
                && !getAuthentication().equals(authentication)) {
            publisher.publishCustomEvent((MidpointAuthentication) getAuthentication());
        }
        securityContext.setAuthentication(authentication);
    }
}
