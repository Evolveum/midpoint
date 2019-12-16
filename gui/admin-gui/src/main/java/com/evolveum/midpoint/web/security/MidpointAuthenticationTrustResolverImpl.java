/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

/**
 * @author skublik
 */

public class MidpointAuthenticationTrustResolverImpl extends AuthenticationTrustResolverImpl {

    private Class<? extends Authentication> anonymousClass = AnonymousAuthenticationToken.class;

    Class<? extends Authentication> getAnonymousClass() {
        return anonymousClass;
    }

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
