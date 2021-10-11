/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.provider.MidPointAbstractAuthenticationProvider;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class MidpointProviderManager implements MidpointAuthenticationManager {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointProviderManager.class);

    private AuthenticationManager parent;
    private List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>();

    public MidpointProviderManager(List<AuthenticationProvider> providers) {
        Assert.notNull(providers, "providers list cannot be null");
        this.parent = parent;
    }

    public List<AuthenticationProvider> getProviders() {
        return providers;
    }

    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        Class<? extends Authentication> toTest = authentication.getClass();
        Authentication result = null;
        boolean debug = LOGGER.isDebugEnabled();

        for (AuthenticationProvider provider : getProviders()) {
            if (provider instanceof MidPointAbstractAuthenticationProvider) {
                if (! ((MidPointAbstractAuthenticationProvider)provider).supports(toTest, authentication)) {
                    continue;
                }

            } else if (!provider.supports(toTest)) {
                continue;
            }

            if (debug) {
                LOGGER.debug("Authentication attempt using "
                        + provider.getClass().getName());
            }

            try {
                result = provider.authenticate(authentication);

                if (result != null) {
                    copyDetails(authentication, result);
                    break;
                }
            }
            catch (AccountStatusException e) {
                throw e;
            }
            catch (InternalAuthenticationServiceException e) {
                throw e;
            }
            catch (AuthenticationException e) {
                throw e;
            }
        }

        if (result != null) {
            return result;
        }
        throw new ProviderNotFoundException("No AuthenticationProvider found for " + toTest.getName());
    }

    private void copyDetails(Authentication source, Authentication dest) {
        if ((dest instanceof AbstractAuthenticationToken) && (dest.getDetails() == null)) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) dest;

            token.setDetails(source.getDetails());
        }
    }
}
