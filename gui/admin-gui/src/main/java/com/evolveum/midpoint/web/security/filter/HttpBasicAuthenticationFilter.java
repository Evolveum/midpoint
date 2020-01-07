/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.NameOfModuleType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.BasicMidPointAuthenticationSuccessHandler;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

/**
 * @author skublik
 */

public class HttpBasicAuthenticationFilter extends BasicAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(HttpBasicAuthenticationFilter.class);

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private RememberMeServices rememberMeServices = new NullRememberMeServices();
    private String credentialsCharset = "UTF-8";
    private AuthenticationSuccessHandler successHandler = new BasicMidPointAuthenticationSuccessHandler();

    public HttpBasicAuthenticationFilter(AuthenticationManager authenticationManager,
                                         AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.toLowerCase().startsWith(NameOfModuleType.HTTP_BASIC.getName().toLowerCase() + " ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String[] tokens = extractAndDecodeHeader(header, request);
            assert tokens.length == 2;

            String username = tokens[0];

            LOGGER.debug("Basic Authentication - Authorization header found for user '" + username + "'");

            if (authenticationIsRequired(username)) {
                UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                        username, tokens[1]);
                authRequest.setDetails(
                        this.authenticationDetailsSource.buildDetails(request));
                Authentication authResult = getAuthenticationManager()
                        .authenticate(authRequest);

                LOGGER.debug("Authentication success: " + authResult);

                SecurityContextHolder.getContext().setAuthentication(authResult);

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

    private String[] extractAndDecodeHeader(String header, HttpServletRequest request)
            throws IOException {

        int startIndex = NameOfModuleType.HTTP_BASIC.getName().length()+1;
        byte[] base64Token = header.substring(startIndex).getBytes("UTF-8");
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Token);
        }
        catch (IllegalArgumentException e) {
            throw new BadCredentialsException(
                    "Failed to decode basic authentication token");
        }

        String token = new String(decoded, getCredentialsCharset(request));

        int delim = token.indexOf(":");

        if (delim == -1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return new String[] { token.substring(0, delim), token.substring(delim + 1) };
    }

    private boolean authenticationIsRequired(String username) {
        Authentication existingAuth = SecurityContextHolder.getContext()
                .getAuthentication();

        if (existingAuth == null || !existingAuth.isAuthenticated()) {
            return true;
        }

        if ((existingAuth instanceof UsernamePasswordAuthenticationToken
        || existingAuth instanceof MidpointAuthentication)
                && !existingAuth.getName().equals(username)) {
            return true;
        }

        if (existingAuth instanceof AnonymousAuthenticationToken) {
            return true;
        }

        return false;
    }

    public void setRememberMeServices(RememberMeServices rememberMeServices) {
        Assert.notNull(rememberMeServices, "rememberMeServices cannot be null");
        this.rememberMeServices = rememberMeServices;
    }

    public void setCredentialsCharset(String credentialsCharset) {
        Assert.hasText(credentialsCharset, "credentialsCharset cannot be null or empty");
        this.credentialsCharset = credentialsCharset;
    }

    protected String getCredentialsCharset(HttpServletRequest httpRequest) {
        return this.credentialsCharset;
    }

    @Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
        try {
            successHandler.onAuthenticationSuccess(request, response, authResult);
        } catch (ServletException e) {
            LOGGER.error("Couldn't execute post successful authentication method", e);
        }
    }

}
