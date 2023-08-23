/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter.configurers;

import com.evolveum.midpoint.authentication.impl.filter.RemoteModuleAuthorizationFilter;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

public class RemoteModuleConfigurer<B extends HttpSecurityBuilder<B>, T extends RemoteModuleConfigurer<B, T, F>, F extends AbstractAuthenticationProcessingFilter> extends AbstractAuthenticationFilterConfigurer<B, T, F> {

    private String authorizationRequestBaseUri;
    private String loginProcessingUrl;
    private AuthenticationManager authenticationManager;
    private final ModelAuditRecorder auditProvider;
    private AuthenticationFailureHandler failureHandler;

    private RemoteModuleAuthorizationFilter authorizationFilter;

    public RemoteModuleConfigurer(ModelAuditRecorder auditProvider) {
        this.auditProvider = auditProvider;
    }

    public T authenticationManager(AuthenticationManager authenticationManager) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        this.authenticationManager = authenticationManager;
        return (T) this;
    }

    public T authorizationRequestBaseUri(String authorizationRequestBaseUri) {
        Assert.hasText(authorizationRequestBaseUri, "authorizationRequestBaseUri cannot be empty");
        this.authorizationRequestBaseUri = authorizationRequestBaseUri;
        return (T) this;
    }

    protected final T setAuthorizationFilter(RemoteModuleAuthorizationFilter authorizationFilter) {
        Assert.notNull(authorizationFilter, "authorizationFilter cannot be null");
        this.authorizationFilter = authorizationFilter;
        return (T) this;
    }

    @Override
    public T loginProcessingUrl(String loginProcessingUrl) {
        Assert.hasText(loginProcessingUrl, "loginProcessingUrl cannot be empty");
        this.loginProcessingUrl = loginProcessingUrl;
        return (T) this;
    }

    public T sendLoginProcessingUrlToSuper() {
        super.loginProcessingUrl(this.loginProcessingUrl);
        return (T) this;
    }

    protected final String getRemoteModuleLoginProcessingUrl() {
        return loginProcessingUrl;
    }

    @Override
    public void init(B http) throws Exception {
        if (this.authenticationManager != null) {
            getAuthenticationFilter().setAuthenticationManager(this.authenticationManager);
        }

        super.init(http);
    }

    @Override
    public void configure(B http) throws Exception {
        if (authorizationFilter != null) {
            authorizationFilter.setAuthenticationFailureHandler(failureHandler);
            RequestCache requestCache = http.getSharedObject(RequestCache.class);
            if (requestCache != null) {
                authorizationFilter.setRequestCache(requestCache);
            }
            http.addFilterBefore(this.postProcess(authorizationFilter), OAuth2AuthorizationRequestRedirectFilter.class);
        }

        super.configure(http);
    }

    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
        return new AntPathRequestMatcher(loginProcessingUrl);
    }

    public T midpointFailureHandler(AuthenticationFailureHandler authenticationFailureHandler) {
        this.failureHandler = authenticationFailureHandler;
        return super.failureHandler(authenticationFailureHandler);
    }

    protected final AuthenticationFailureHandler getFailureHandler() {
        return failureHandler;
    }

    protected final ModelAuditRecorder getAuditProvider() {
        return auditProvider;
    }

    protected final String getAuthorizationRequestBaseUri() {
        return authorizationRequestBaseUri;
    }
}
