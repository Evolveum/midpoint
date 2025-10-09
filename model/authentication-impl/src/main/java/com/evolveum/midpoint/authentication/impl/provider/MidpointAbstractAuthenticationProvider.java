/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

public abstract class MidpointAbstractAuthenticationProvider extends AbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAbstractAuthenticationProvider.class);

    @Override
    protected final Authentication internalAuthentication(Authentication authentication, List<ObjectReferenceType> requireAssignment, AuthenticationChannel channel, Class<? extends FocusType> focusType) throws AuthenticationException {
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof GuiProfiledPrincipal) {
            return authentication;
        }
        try {
            String enteredUsername = getEnteredUsername(authentication);
            return doAuthenticate(authentication, enteredUsername, requireAssignment, channel, focusType);

        } catch (AuthenticationException e) {
            LOGGER.debug("Authentication failed for {}: {}", authentication, e.getMessage());
            throw e;
        }
    }

    private String getEnteredUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof MidPointPrincipal mpPrincipal) {
            LOGGER.trace("Found midPoint principal, returning principal username.");
            return mpPrincipal.getUsername();
        }
        if (principal instanceof String enteredUsername) {
            LOGGER.trace("No midPoint principal yet, returning authentication principal object");
            if (StringUtils.isNotBlank(enteredUsername)) {
                return enteredUsername;
            }
        }
        MidPointPrincipal mpPrincipal = AuthUtil.getMidpointPrincipal();
        if (mpPrincipal != null) {
            LOGGER.trace("Found pre-authenticated midPoint principal, returning principal username.");
            return mpPrincipal.getUsername();
        }
        return null;
    }

    protected abstract Authentication doAuthenticate(
            Authentication authentication,
            String enteredUsername,
            List<ObjectReferenceType> requireAssignment,
            AuthenticationChannel channel,
            Class<? extends FocusType> focusType);
}
