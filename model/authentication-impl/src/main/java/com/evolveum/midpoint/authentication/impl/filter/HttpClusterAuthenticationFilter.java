/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

import com.evolveum.midpoint.authentication.impl.module.authentication.token.ClusterAuthenticationToken;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public class HttpClusterAuthenticationFilter extends HttpAuthenticationFilter<String> {

    private static final Trace LOGGER = TraceManager.getTrace(HttpClusterAuthenticationFilter.class);

    public HttpClusterAuthenticationFilter(AuthenticationManager authenticationManager,
                                           AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    protected String extractAndDecodeHeader(String header, HttpServletRequest request) {
        return createCredentialsFromHeader(header);
    }

    @Override
    protected UsernamePasswordAuthenticationToken createAuthenticationToken(String credentials, HttpServletRequest request) {
        return new ClusterAuthenticationToken(request.getRemoteAddr(), credentials);
    }

    @Override
    protected boolean authenticationIsRequired(String tokens, HttpServletRequest request) {
        return authenticationIsRequired(request.getRemoteAddr(), ClusterAuthenticationToken.class);
    }

    @Override
    protected void logFoundAuthorizationHeader(String tokens, HttpServletRequest request) {
        LOGGER.debug("Cluster Authentication - Authorization header found for remote address '" + request.getRemoteAddr() + "'");
    }

    @Override
    protected @NotNull String getModuleIdentifier() {
        return AuthenticationModuleNameConstants.CLUSTER;
    }

    @Override
    protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        response.setHeader("WWW-Authenticate", AuthenticationModuleNameConstants.CLUSTER + " realm=\"midpoint\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(" test error ");
        response.getWriter().flush();
        response.getWriter().close();
    }

    @Override
    protected HttpServletRequest createWrapperOfRequest(HttpServletRequest request) {
        return request;
    }
}
