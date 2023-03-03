/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.HttpModuleAuthentication;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * @author skublik
 */

public class OidcResourceServerProvider extends RemoteModuleProvider {

    private static final Trace LOGGER = TraceManager.getTrace(OidcResourceServerProvider.class);

    private final JwtAuthenticationProvider oidcProvider;

    public OidcResourceServerProvider(JwtDecoder decoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
        oidcProvider = new JwtAuthenticationProvider(decoder);
        oidcProvider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
    }

    @Override
    protected Authentication internalAuthentication(Authentication authentication, List requireAssignment,
            AuthenticationChannel channel, Class focusType) throws AuthenticationException {
        Authentication token;
        if (authentication instanceof BearerTokenAuthenticationToken) {
            BearerTokenAuthenticationToken oidcAuthenticationToken = (BearerTokenAuthenticationToken) authentication;
            JwtAuthenticationToken jwtAuthentication;
            try {
                jwtAuthentication = (JwtAuthenticationToken) oidcProvider.authenticate(oidcAuthenticationToken);
            } catch (AuthenticationException e) {
                getAuditProvider().auditLoginFailure(null, null, createConnectEnvironment(getChannel()), e.getMessage());
                throw e;
            }

            HttpModuleAuthentication oidcModule = (HttpModuleAuthentication) AuthUtil.getProcessingModule();
            try {
                    String username = jwtAuthentication.getName();
                    if (StringUtils.isEmpty(username)) {
                        LOGGER.error("Username from jwt token don't contains value");
                        throw new AuthenticationServiceException("web.security.provider.invalid");
                    }
                token = getPreAuthenticationToken(authentication, username, focusType, requireAssignment, channel);
            } catch (AuthenticationException e) {
                oidcModule.setAuthentication(oidcAuthenticationToken);
                LOGGER.info("Authentication with oidc module failed: {}", e.getMessage());
                throw e;
            }
        } else {
            LOGGER.error("Unsupported authentication {}", authentication);
            throw new AuthenticationServiceException("web.security.provider.unavailable");
        }
        MidPointPrincipal principal = (MidPointPrincipal) token.getPrincipal();
        LOGGER.debug("User '{}' authenticated ({}), authorities: {}", authentication.getPrincipal(),
                authentication.getClass().getSimpleName(), principal.getAuthorities());
        return token;
    }

    @Override
    public boolean supports(Class authentication) {
        return oidcProvider.supports(authentication);
    }
}
