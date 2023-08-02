/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

public abstract class MidpointAbstractAuthenticationProvider extends AbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAbstractAuthenticationProvider.class);

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment, AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof GuiProfiledPrincipal) {
            return authentication;
        }
        try {

            return doAuthenticate(authentication, requireAssignment, channel, focusType);

        } catch (AuthenticationException e) {
            LOGGER.debug("Authentication failed for {}: {}", authentication, e.getMessage());
            throw e;
        }

    }

    protected abstract Authentication doAuthenticate(Authentication authentication,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel,
            Class<? extends FocusType> focusType);
}
