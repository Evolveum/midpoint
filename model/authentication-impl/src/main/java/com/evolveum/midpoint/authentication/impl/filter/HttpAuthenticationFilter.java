/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.handler.BasicMidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.session.MidpointHttpServletRequest;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;

import org.jetbrains.annotations.NotNull;
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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public abstract class HttpAuthenticationFilter<T> extends BasicAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(HttpAuthenticationFilter.class);

    private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private RememberMeServices rememberMeServices = new NullRememberMeServices();
    private final Charset credentialsCharset = StandardCharsets.UTF_8;
    private final AuthenticationSuccessHandler successHandler = new BasicMidPointAuthenticationSuccessHandler();

    public HttpAuthenticationFilter(AuthenticationManager authenticationManager,
                                    AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest newRequest;
        try {
            String header = request.getHeader("Authorization");

            if (header == null) {
                chain.doFilter(request, response);
                return;
            }

            int delim = header.indexOf(" ");

            if (delim == -1) {
                throw new BadCredentialsException("Invalid authentication header, value of header don't contains delimiter ' '."
                        + " Please use form 'Authorization: <type> <credentials>' for successful authentication");
            }

            if (skipFilterForAuthorizationHeader(header)) {
                chain.doFilter(request, response);
                return;
            }

            T tokens = extractAndDecodeHeader(header, request);

            logFoundAuthorizationHeader(tokens, request);

            if (authenticationIsRequired(tokens, request)) {
                AbstractAuthenticationToken authRequest = createAuthenticationToken(tokens, request);
                authRequest.setDetails(
                        getAuthenticationDetailsSource().buildDetails(request));
                Authentication authResult = getAuthenticationManager()
                        .authenticate(authRequest);

                LOGGER.debug("Authentication success: " + authResult);

                AuthSequenceUtil.resolveProxyUserOidHeader(request);

                getRememberMeServices().loginSuccess(request, response, authResult);

                onSuccessfulAuthentication(request, response, authResult);
            }
            newRequest = createWrapperOfRequest(request);

        }
        catch (AuthenticationException failed) {
            LOGGER.debug("Authentication request for failed: " + failed);

            getRememberMeServices().loginFail(request, response);

            onUnsuccessfulAuthentication(request, response, failed);

            return;
        }

        chain.doFilter(newRequest, response);
    }

    protected boolean skipFilterForAuthorizationHeader(String header) {
        return !header.toLowerCase().startsWith(getModuleIdentifier().toLowerCase() + " ");
    }

    protected abstract T extractAndDecodeHeader(String header, HttpServletRequest request);

    protected String createCredentialsFromHeader(String header) {
        int startIndex = getModuleIdentifier().length() + 1;
        byte[] base64Token = header.substring(startIndex).getBytes(this.credentialsCharset);
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Token);
        }
        catch (IllegalArgumentException e) {
            throw new BadCredentialsException(
                    "Failed to decode authentication credentials from header");
        }

        return new String(decoded, this.credentialsCharset);
    }

    protected HttpServletRequest createWrapperOfRequest(HttpServletRequest request) {
        return new MidpointHttpServletRequest(request);
    }

    protected abstract AbstractAuthenticationToken createAuthenticationToken(T tokens, HttpServletRequest request);

    protected abstract boolean authenticationIsRequired(T tokens, HttpServletRequest request);

    protected abstract void logFoundAuthorizationHeader(T tokens, HttpServletRequest request);

    @NotNull
    protected abstract String getModuleIdentifier();

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

    @Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
        try {
            successHandler.onAuthenticationSuccess(request, response, authResult);
        } catch (ServletException e) {
            LOGGER.error("Couldn't execute post successful authentication method", e);
        }
    }

    @Override
    protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        try {
            this.getAuthenticationEntryPoint().commence(request, response, failed);
        } catch (ServletException e) {
            LOGGER.error("Couldn't execute post unsuccessful authentication method", e);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            moduleAuthentication.recordFailure(failed);
        }
    }
}
