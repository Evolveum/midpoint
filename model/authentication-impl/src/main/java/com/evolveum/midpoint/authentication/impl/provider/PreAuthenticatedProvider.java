/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.provider;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.evaluator.context.PreAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;


public class PreAuthenticatedProvider extends MidpointAbstractAuthenticationProvider {

    private static final Trace LOGGER = TraceManager.getTrace(PreAuthenticatedProvider.class);

    @Autowired private AuthenticationEvaluator<PreAuthenticationContext, PreAuthenticatedAuthenticationToken> preAuthenticatedEvaluator;

    @Override
    protected Authentication createNewAuthenticationToken(Authentication actualAuthentication, Collection<? extends GrantedAuthority> newAuthorities) {
        if (actualAuthentication instanceof PreAuthenticatedAuthenticationToken) {
            return new PreAuthenticatedAuthenticationToken(actualAuthentication.getPrincipal(), actualAuthentication.getCredentials(), newAuthorities);
        } else {
            return actualAuthentication;
        }
    }

    @Override
    protected Authentication doAuthenticate(Authentication authentication, String enteredUsername, List<ObjectReferenceType> requireAssignment, AuthenticationChannel channel, Class<? extends FocusType> focusType) {
        LOGGER.trace("Authenticating username '{}'", enteredUsername);

        ConnectionEnvironment connEnv = createEnvironment(channel);

        Authentication token = preAuthenticatedEvaluator.authenticate(connEnv, new
                PreAuthenticationContext(enteredUsername, focusType, requireAssignment, channel));

        MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();

        LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                authentication.getClass().getSimpleName(), principal.getAuthorities());
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.equals(authentication);
    }
}
