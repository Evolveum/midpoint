/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.duosecurity.Client;

import com.duosecurity.model.Token;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.DuoRequestToken;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.nimbusds.jose.jwk.JWK;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.List;
import java.util.function.Function;

/**
 * @author skublik
 */

public class DuoProvider extends RemoteModuleProvider {

    private static final Trace LOGGER = TraceManager.getTrace(DuoProvider.class);

    private final Client duoClient;
    private Function<ClientRegistration, JWK> jwkResolver;

    public DuoProvider(Client duoClient) {
        this.duoClient = duoClient;
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List requireAssignment,
            AuthenticationChannel channel, Class focusType) throws AuthenticationException {
        PreAuthenticatedAuthenticationToken token;

        Token duoToken;

        String username = authentication.getName();
        if (username == null) {
            LOGGER.error("Couldn't get principal username for duo module");
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }

        try {
            duoToken = duoClient.exchangeAuthorizationCodeFor2FAResult(
                    ((DuoRequestToken) authentication).getDuoCode(), username);

            if (!isAuthSuccessful(duoToken)) {
                throw new AuthenticationServiceException("Duo authentication is deny");
            }
        } catch (Exception e) {
            getAuditProvider().auditLoginFailure(null, null, createConnectEnvironment(getChannel()), e.getMessage());
            LOGGER.debug("Unexpected exception in duo module", e);
            throw new AuthenticationServiceException("web.security.provider.unavailable", e);
        }

        try {
            token = getPreAuthenticationToken(username, focusType, requireAssignment, channel);
            ((DuoRequestToken) authentication).setDetails(duoToken);
        } catch (AuthenticationException e) {
            LOGGER.debug("Authentication with duo module failed: {}", e.getMessage());
            throw e;
        }

        MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();

        LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                authentication.getClass().getSimpleName(), principal.getAuthorities());
        return token;
    }

    private boolean isAuthSuccessful(Token token) {
        if (token != null && token.getAuth_result() != null) {
            return "ALLOW".equalsIgnoreCase(token.getAuth_result().getStatus());
        }
        return false;
    }

    @Override
    public boolean supports(Class authentication) {
        return DuoRequestToken.class.equals(authentication);
    }
}
