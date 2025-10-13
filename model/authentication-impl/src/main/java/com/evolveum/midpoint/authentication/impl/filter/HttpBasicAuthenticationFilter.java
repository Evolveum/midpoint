/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public class HttpBasicAuthenticationFilter extends HttpAuthenticationFilter<AbstractMap.SimpleImmutableEntry<String, String>> {

    private static final Trace LOGGER = TraceManager.getTrace(HttpBasicAuthenticationFilter.class);

    public HttpBasicAuthenticationFilter(AuthenticationManager authenticationManager,
                                         AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected AbstractMap.SimpleImmutableEntry<String, String> extractAndDecodeHeader(String header, HttpServletRequest request) {
        String token = createCredentialsFromHeader(header);

        int delim = token.indexOf(":");

        if (delim == -1) {
            throw new BadCredentialsException("Invalid authentication token");
        }
        return new AbstractMap.SimpleImmutableEntry<>(token.substring(0, delim), token.substring(delim + 1) );
    }

    @Override
    protected UsernamePasswordAuthenticationToken createAuthenticationToken(
            AbstractMap.SimpleImmutableEntry<String, String> tokens, HttpServletRequest request) {
        return new UsernamePasswordAuthenticationToken( tokens.getKey(), tokens.getValue());
    }

    @Override
    protected boolean authenticationIsRequired(AbstractMap.SimpleImmutableEntry<String, String> tokens, HttpServletRequest request) {
        return authenticationIsRequired(tokens.getKey(), UsernamePasswordAuthenticationToken.class);
    }

    @Override
    protected void logFoundAuthorizationHeader(AbstractMap.SimpleImmutableEntry<String, String> tokens, HttpServletRequest request) {
        LOGGER.debug("Basic Authentication - Authorization header found for user '" + tokens.getKey() + "'");
    }

    @Override
    protected @NotNull String getModuleIdentifier() {
        return AuthenticationModuleNameConstants.HTTP_BASIC;
    }
}
