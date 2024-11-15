/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter.configurers;

import java.util.LinkedHashMap;

import com.evolveum.midpoint.authentication.impl.filter.MidpointAnonymousAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.MidpointExceptionTranslationFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

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

import com.evolveum.midpoint.authentication.impl.MidpointAuthenticationTrustResolverImpl;

public class MidpointExceptionHandlingConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractHttpConfigurer<ExceptionHandlingConfigurer<H>, H> {

    private AuthenticationEntryPoint authenticationEntryPoint;

    private AccessDeniedHandler accessDeniedHandler;

    private AuthenticationTrustResolver authenticationTrustResolver = new MidpointAuthenticationTrustResolverImpl();

    private final LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> defaultEntryPointMappings = new LinkedHashMap<>();

    private final LinkedHashMap<RequestMatcher, AccessDeniedHandler> defaultDeniedHandlerMappings = new LinkedHashMap<>();

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
        AuthenticationEntryPoint entryPoint = getAuthenticationEntryPoint();
        ExceptionTranslationFilter exceptionTranslationFilter = new MidpointExceptionTranslationFilter(
                entryPoint, getRequestCache(http)) {
            @Override
            protected Authentication createNewAuthentication(AnonymousAuthenticationToken authentication,
                    AuthenticationSequenceChannelType channel) {
                return MidpointExceptionHandlingConfigurer.this.createNewAuthentication(authentication, channel);
            }
        };
        AccessDeniedHandler deniedHandler = getAccessDeniedHandler();
        exceptionTranslationFilter.setAccessDeniedHandler(deniedHandler);
        exceptionTranslationFilter.setAuthenticationTrustResolver(getAuthenticationTrustResolver());
        exceptionTranslationFilter = postProcess(exceptionTranslationFilter);
        http.addFilterAfter(exceptionTranslationFilter, MidpointAnonymousAuthenticationFilter.class);
    }

    protected Authentication createNewAuthentication(AnonymousAuthenticationToken authentication,
            AuthenticationSequenceChannelType channel) {
        return null;
    }

    AccessDeniedHandler getAccessDeniedHandler() {
        AccessDeniedHandler deniedHandler = this.accessDeniedHandler;
        if (deniedHandler == null) {
            deniedHandler = createDefaultDeniedHandler();
        }
        return deniedHandler;
    }

    AuthenticationTrustResolver getAuthenticationTrustResolver() {
        return authenticationTrustResolver;
    }

    AuthenticationEntryPoint getAuthenticationEntryPoint() {
        AuthenticationEntryPoint entryPoint = this.authenticationEntryPoint;
        if (entryPoint == null) {
            entryPoint = createDefaultEntryPoint();
        }
        return entryPoint;
    }

    private AccessDeniedHandler createDefaultDeniedHandler() {
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

    private AuthenticationEntryPoint createDefaultEntryPoint() {
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
        if (result == null) {
            result = new HttpSessionRequestCache();
        }

        if (result instanceof HttpSessionRequestCache httpSessionRequestCache) {
            httpSessionRequestCache.setMatchingRequestParameterName(null);
        }

        return result;
    }
}
