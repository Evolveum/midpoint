/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.authentication.impl.provider.AbstractAuthenticationProvider;

import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class MidpointProviderManager implements AuthenticationManager {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointProviderManager.class);

    private final List<AuthenticationProvider> providers = new ArrayList<>();

    public MidpointProviderManager() {
    }

    public List<AuthenticationProvider> getProviders() {
        return providers;
    }

    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        Authentication result = null;
        boolean debug = LOGGER.isDebugEnabled();

        for (AuthenticationProvider provider : getProviders()) {
            if (provider instanceof AbstractAuthenticationProvider) {
                if (! ((AbstractAuthenticationProvider)provider).supports(authentication)) {
                    continue;
                }

            } else {
                continue;
            }

            if (debug) {
                LOGGER.debug("Authentication attempt using "
                        + provider.getClass().getName());
            }

            result = provider.authenticate(authentication);

            if (result != null) {
                copyDetails(authentication, result);
                break;
            }
        }

        if (result != null) {
            return result;
        }
        throw new ProviderNotFoundException("No AuthenticationProvider found for " + authentication.getClass().getName());
    }

    private void copyDetails(Authentication source, Authentication dest) {
        if ((dest instanceof AbstractAuthenticationToken) && (dest.getDetails() == null)) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) dest;

            token.setDetails(source.getDetails());
        }
    }
}
