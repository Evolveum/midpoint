/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

/**
 * @author skublik
 */

public class MidpointSecurityContext implements SecurityContext {

    private final SecurityContext securityContext;

    public MidpointSecurityContext (SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Override
    public Authentication getAuthentication() {
        return securityContext.getAuthentication();
    }

    @Override
    public void setAuthentication(Authentication authentication) {
        if (getAuthentication() instanceof MidpointAuthentication
                && !getAuthentication().equals(authentication)) {
            RemoveUnusedSecurityFilterPublisher.get().publishCustomEvent(
                    ((MidpointAuthentication) getAuthentication()).getAuthModules());
        }
        securityContext.setAuthentication(authentication);
    }
}
