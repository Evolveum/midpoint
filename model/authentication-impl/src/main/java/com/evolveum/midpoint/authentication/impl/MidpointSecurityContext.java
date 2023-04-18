/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
