/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public abstract class RemoteModuleAuthorizationFilter<T extends RemoteModuleAuthorizationFilter<T>> extends OncePerRequestFilter {

    private final ModelAuditRecorder auditProvider;

    private AuthenticationFailureHandler failureHandler;

    private final SecurityContextRepository securityContextRepository;

    private RequestCache requestCache = new HttpSessionRequestCache();

    private final RedirectStrategy authorizationRedirectStrategy = new DefaultRedirectStrategy();

    protected RemoteModuleAuthorizationFilter(
            ModelAuditRecorder auditProvider,
            SecurityContextRepository securityContextRepository) {
        this.auditProvider = auditProvider;
        this.securityContextRepository = securityContextRepository;
    }

    protected final ModelAuditRecorder getAuditProvider() {
        return auditProvider;
    }

    protected final AuthenticationFailureHandler getAuthenticationFailureHandler() {
        return failureHandler;
    }

    public final T setAuthenticationFailureHandler(AuthenticationFailureHandler failureHandler) {
        Assert.notNull(failureHandler, "failureHandler cannot be null");
        this.failureHandler = failureHandler;
        return (T) this;
    }

    protected final RequestCache getRequestCache() {
        return requestCache;
    }

    public final T setRequestCache(RequestCache requestCache) {
        Assert.notNull(requestCache, "requestCache cannot be null");
        this.requestCache = requestCache;
        return (T) this;
    }

    protected final SecurityContextRepository getSecurityContextRepository() {
        return securityContextRepository;
    }

    protected final void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        String channel;
        MidpointAuthentication actualAuthentication = AuthUtil.getMidpointAuthentication();
        if (actualAuthentication.getAuthenticationChannel() != null) {
            channel = actualAuthentication.getAuthenticationChannel().getChannelId();
        } else {
            channel = SchemaConstants.CHANNEL_USER_URI;
        }

        auditProvider.auditLoginFailure(
                "unknown user",
                null,
                ConnectionEnvironment.create(channel),
                getAuthenticationType() + " authentication module: " + failed.getMessage());

        this.failureHandler.onAuthenticationFailure(request, response, failed);
    }

    protected abstract String getAuthenticationType();

    protected final RedirectStrategy getAuthorizationRedirectStrategy() {
        return authorizationRedirectStrategy;
    }
}
