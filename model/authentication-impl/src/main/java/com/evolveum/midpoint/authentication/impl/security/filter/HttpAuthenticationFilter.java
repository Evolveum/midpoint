/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.authentication.MidpointAuthentication;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.Assert;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.impl.security.handler.BasicMidPointAuthenticationSuccessHandler;

/**
 * @author skublik
 */

public abstract class HttpAuthenticationFilter extends BasicAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(HttpAuthenticationFilter.class);

    private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private RememberMeServices rememberMeServices = new NullRememberMeServices();
    private String credentialsCharset = "UTF-8";
    private final AuthenticationSuccessHandler successHandler = new BasicMidPointAuthenticationSuccessHandler();

    public HttpAuthenticationFilter(AuthenticationManager authenticationManager,
                                    AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected abstract void doFilterInternal(HttpServletRequest request,
                                             HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException;

    protected String[] extractAndDecodeHeader(String header, HttpServletRequest request, int startIndex)
            throws IOException {

        byte[] base64Token = header.substring(startIndex).getBytes(StandardCharsets.UTF_8);
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Token);
        }
        catch (IllegalArgumentException e) {
            throw new BadCredentialsException(
                    "Failed to decode authentication token");
        }

        String token = new String(decoded, getCredentialsCharset(request));

        int delim = token.indexOf(":");

        if (delim == -1) {
            throw new BadCredentialsException("Invalid authentication token");
        }
        return new String[] { token.substring(0, delim), token.substring(delim + 1) };
    }

    protected boolean authenticationIsRequired(String username, Class<? extends Authentication> basicClass) {
        Authentication existingAuth = SecurityContextHolder.getContext()
                .getAuthentication();

        if (existingAuth == null || !existingAuth.isAuthenticated()) {
            return true;
        }

        if ((existingAuth.getClass().isAssignableFrom(basicClass)|| existingAuth instanceof MidpointAuthentication)
                && !existingAuth.getName().equals(username)) {
            return true;
        }

        return existingAuth instanceof AnonymousAuthenticationToken;
    }

    public void setRememberMeServices(RememberMeServices rememberMeServices) {
        Assert.notNull(rememberMeServices, "rememberMeServices cannot be null");
        this.rememberMeServices = rememberMeServices;
    }

    protected RememberMeServices getRememberMeServices() {
        return rememberMeServices;
    }

    protected AuthenticationDetailsSource<HttpServletRequest, ?> getAuthenticationDetailsSource() {
        return authenticationDetailsSource;
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
