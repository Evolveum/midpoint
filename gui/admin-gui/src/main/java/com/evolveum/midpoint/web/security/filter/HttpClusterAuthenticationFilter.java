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
import com.evolveum.midpoint.web.security.BasicMidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.web.security.module.authentication.ClusterAuthenticationToken;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author skublik
 */

public class HttpClusterAuthenticationFilter extends HttpAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(HttpClusterAuthenticationFilter.class);

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private RememberMeServices rememberMeServices = new NullRememberMeServices();
    private String credentialsCharset = "UTF-8";
    private AuthenticationSuccessHandler successHandler = new BasicMidPointAuthenticationSuccessHandler();

    public HttpClusterAuthenticationFilter(AuthenticationManager authenticationManager,
                                           AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.toLowerCase().startsWith(AuthenticationModuleNameConstants.CLUSTER.toLowerCase() + " ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String credentials = extractAndDecodeHeader(header, request);
            String remoteAddress = request.getRemoteAddr();

            LOGGER.debug("Cluster Authentication - Authorization header found for remote address '" + remoteAddress + "'");

            if (authenticationIsRequired(remoteAddress, ClusterAuthenticationToken.class)) {
                ClusterAuthenticationToken authRequest = new ClusterAuthenticationToken(
                        remoteAddress, credentials);
                authRequest.setDetails(
                        this.authenticationDetailsSource.buildDetails(request));
                Authentication authResult = getAuthenticationManager()
                        .authenticate(authRequest);

                LOGGER.debug("Authentication success: " + authResult);

                SecurityUtils.resolveProxyUserOidHeader(request);

                this.rememberMeServices.loginSuccess(request, response, authResult);

                onSuccessfulAuthentication(request, response, authResult);
            }

        }
        catch (AuthenticationException failed) {

            LOGGER.debug("Authentication request for failed: " + failed);

            this.rememberMeServices.loginFail(request, response);

            this.getAuthenticationEntryPoint().commence(request, response, failed);

            return;
        }

        chain.doFilter(request, response);
    }

    private String extractAndDecodeHeader(String header, HttpServletRequest request)
            throws IOException {

        int startIndex = AuthenticationModuleNameConstants.CLUSTER.length() + 1;
        byte[] base64Token = header.substring(startIndex).getBytes(StandardCharsets.UTF_8);
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Token);
        }
        catch (IllegalArgumentException e) {
            throw new BadCredentialsException(
                    "Failed to decode security question authentication token");
        }

        String token = new String(decoded, getCredentialsCharset(request));

        return token;
    }
}
