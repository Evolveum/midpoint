/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter.oidc;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.filter.HttpAuthenticationFilter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author skublik
 */

public class OidcBearerTokenAuthenticationFilter extends HttpAuthenticationFilter<String> {

    private static final Trace LOGGER = TraceManager.getTrace(OidcBearerTokenAuthenticationFilter.class);

    BearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();

    public OidcBearerTokenAuthenticationFilter(AuthenticationManager authenticationManager,
                                              AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected String extractAndDecodeHeader(String header, HttpServletRequest request) {
        return tokenResolver.resolve(request);
    }

    @Override
    protected BearerTokenAuthenticationToken createAuthenticationToken(
            String token, HttpServletRequest request) {
        return new BearerTokenAuthenticationToken(token);
    }

    @Override
    protected boolean authenticationIsRequired(String token, HttpServletRequest request) {
        return token!= null;
    }

    @Override
    protected void logFoundAuthorizationHeader(String token, HttpServletRequest request) {
        LOGGER.debug("Oidc Authentication - Authorization header found");
    }

    @Override
    protected @NotNull String getModuleIdentifier() {
        return AuthenticationModuleNameConstants.OIDC;
    }

    protected boolean skipFilterForAuthorizationHeader(String header) {
        return !header.toLowerCase().startsWith("Bearer ".toLowerCase());
    }
}
