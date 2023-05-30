/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter.configurers;


import com.evolveum.midpoint.web.security.MidpointAuthenticationTrustResolverImpl;
import com.evolveum.midpoint.web.security.filter.MidpointAnonymousAuthenticationFilter;
import com.evolveum.midpoint.web.security.filter.MidpointExceptionTranslationFilter;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.RequestMatcherDelegatingAccessDeniedHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;

public class MidpointExceptionHandlingConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractHttpConfigurer<ExceptionHandlingConfigurer<H>, H> {

    private AuthenticationEntryPoint authenticationEntryPoint;

    private AccessDeniedHandler accessDeniedHandler;

    private AuthenticationTrustResolver authenticationTrustResolver = new MidpointAuthenticationTrustResolverImpl();

    private LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> defaultEntryPointMappings = new LinkedHashMap<>();

    private LinkedHashMap<RequestMatcher, AccessDeniedHandler> defaultDeniedHandlerMappings = new LinkedHashMap<>();

    public MidpointExceptionHandlingConfigurer() {
    }

    public MidpointExceptionHandlingConfigurer<H> accessDeniedPage(String accessDeniedUrl) {
        AccessDeniedHandlerImpl accessDeniedHandler = new AccessDeniedHandlerImpl();
        accessDeniedHandler.setErrorPage(accessDeniedUrl);
        return accessDeniedHandler(accessDeniedHandler);
    }

    public MidpointExceptionHandlingConfigurer<H> accessDeniedHandler(
            AccessDeniedHandler accessDeniedHandler) {
        this.accessDeniedHandler = accessDeniedHandler;
        return this;
    }

    public MidpointExceptionHandlingConfigurer<H> authenticationTrustResolver(
            AuthenticationTrustResolver authenticationTrustResolver) {
        this.authenticationTrustResolver = authenticationTrustResolver;
        return this;
    }

    public MidpointExceptionHandlingConfigurer<H> defaultAccessDeniedHandlerFor(
            AccessDeniedHandler deniedHandler, RequestMatcher preferredMatcher) {
        this.defaultDeniedHandlerMappings.put(preferredMatcher, deniedHandler);
        return this;
    }

    public MidpointExceptionHandlingConfigurer<H> authenticationEntryPoint(
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        return this;
    }

    public MidpointExceptionHandlingConfigurer<H> defaultAuthenticationEntryPointFor(
            AuthenticationEntryPoint entryPoint, RequestMatcher preferredMatcher) {
        this.defaultEntryPointMappings.put(preferredMatcher, entryPoint);
        return this;
    }

    @Override
    public void configure(H http) throws Exception {
        AuthenticationEntryPoint entryPoint = getAuthenticationEntryPoint(http);
        ExceptionTranslationFilter exceptionTranslationFilter = new MidpointExceptionTranslationFilter(
                entryPoint, getRequestCache(http)) {
            @Override
            protected Authentication createNewAuthentication(AnonymousAuthenticationToken authentication) {
                return MidpointExceptionHandlingConfigurer.this.createNewAuthentication(authentication);
            }
        };
        AccessDeniedHandler deniedHandler = getAccessDeniedHandler(http);
        exceptionTranslationFilter.setAccessDeniedHandler(deniedHandler);
        exceptionTranslationFilter.setAuthenticationTrustResolver(getAuthenticationTrustResolver());
        exceptionTranslationFilter = postProcess(exceptionTranslationFilter);
        http.addFilterAfter(exceptionTranslationFilter, MidpointAnonymousAuthenticationFilter.class);
    }

    protected Authentication createNewAuthentication(AnonymousAuthenticationToken authentication) {
        return null;
    }

    AccessDeniedHandler getAccessDeniedHandler(H http) {
        AccessDeniedHandler deniedHandler = this.accessDeniedHandler;
        if (deniedHandler == null) {
            deniedHandler = createDefaultDeniedHandler(http);
        }
        return deniedHandler;
    }

    AuthenticationTrustResolver getAuthenticationTrustResolver() {
        return authenticationTrustResolver;
    }

    AuthenticationEntryPoint getAuthenticationEntryPoint(H http) {
        AuthenticationEntryPoint entryPoint = this.authenticationEntryPoint;
        if (entryPoint == null) {
            entryPoint = createDefaultEntryPoint(http);
        }
        return entryPoint;
    }

    private AccessDeniedHandler createDefaultDeniedHandler(H http) {
        if (this.defaultDeniedHandlerMappings.isEmpty()) {
            return new AccessDeniedHandlerImpl();
        }
        if (this.defaultDeniedHandlerMappings.size() == 1) {
            return this.defaultDeniedHandlerMappings.values().iterator().next();
        }
        return new RequestMatcherDelegatingAccessDeniedHandler(
                this.defaultDeniedHandlerMappings,
                new AccessDeniedHandlerImpl());
    }

    private AuthenticationEntryPoint createDefaultEntryPoint(H http) {
        if (this.defaultEntryPointMappings.isEmpty()) {
            return new Http403ForbiddenEntryPoint();
        }
        if (this.defaultEntryPointMappings.size() == 1) {
            return this.defaultEntryPointMappings.values().iterator().next();
        }
        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(
                this.defaultEntryPointMappings);
        entryPoint.setDefaultEntryPoint(this.defaultEntryPointMappings.values().iterator()
                .next());
        return entryPoint;
    }

    private RequestCache getRequestCache(H http) {
        RequestCache result = http.getSharedObject(RequestCache.class);
        if (result != null) {
            return result;
        }
        return new HttpSessionRequestCache();
    }
}
