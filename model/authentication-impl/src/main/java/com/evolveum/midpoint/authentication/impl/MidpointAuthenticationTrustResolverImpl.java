/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

/**
 * @author skublik
 */

public class MidpointAuthenticationTrustResolverImpl extends AuthenticationTrustResolverImpl {

    private Class<? extends Authentication> anonymousClass = AnonymousAuthenticationToken.class;

    public boolean isAnonymous(Authentication authentication) {
        if ((anonymousClass == null) || (authentication == null)) {
            return false;
        }

        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication == null || moduleAuthentication.getAuthentication() == null) {
                return false;
            }
            return  anonymousClass.isAssignableFrom(moduleAuthentication.getAuthentication().getClass());
        }

        return anonymousClass.isAssignableFrom(authentication.getClass());
    }

    public void setAnonymousClass(Class<? extends Authentication> anonymousClass) {
        this.anonymousClass = anonymousClass;
    }
}
