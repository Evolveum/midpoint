/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.model.api.authentication.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.util.MidpointHttpServletRequest;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author skublik
 */

public class HttpBasicAuthenticationFilter extends HttpAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(HttpBasicAuthenticationFilter.class);

    public HttpBasicAuthenticationFilter(AuthenticationManager authenticationManager,
                                         AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.toLowerCase().startsWith(AuthenticationModuleNameConstants.HTTP_BASIC.toLowerCase() + " ")) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest newRequest = request;
        try {
            int startIndex = AuthenticationModuleNameConstants.HTTP_BASIC.length()+1;
            String[] tokens = extractAndDecodeHeader(header, request, startIndex);
            assert tokens.length == 2;

            String username = tokens[0];

            LOGGER.debug("Basic Authentication - Authorization header found for user '" + username + "'");

            if (authenticationIsRequired(username, UsernamePasswordAuthenticationToken.class)) {
                UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                        username, tokens[1]);
                authRequest.setDetails(
                        getAuthenticationDetailsSource().buildDetails(request));
                Authentication authResult = getAuthenticationManager()
                        .authenticate(authRequest);

                LOGGER.debug("Authentication success: " + authResult);

                SecurityUtils.resolveProxyUserOidHeader(request);

                getRememberMeServices().loginSuccess(request, response, authResult);

                onSuccessfulAuthentication(request, response, authResult);
            }
            newRequest = new MidpointHttpServletRequest(request);

        }
        catch (AuthenticationException failed) {

            LOGGER.debug("Authentication request for failed: " + failed);

            getRememberMeServices().loginFail(request, response);

            this.getAuthenticationEntryPoint().commence(request, response, failed);

            return;
        }

        chain.doFilter(newRequest, response);
    }
}
