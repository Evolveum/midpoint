/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter.duo;

import com.duosecurity.Client;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.filter.RemoteAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.filter.RemoteModuleAuthorizationFilter;
import com.evolveum.midpoint.authentication.impl.module.authentication.DuoModuleAuthentication;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;

import java.io.IOException;

public class DuoAuthorizationRequestRedirectFilter extends RemoteModuleAuthorizationFilter<DuoAuthorizationRequestRedirectFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(RemoteAuthenticationFilter.class);

    private final Client duoClient;

    private final AntPathRequestMatcher authorizationRequestMatcher;

    public DuoAuthorizationRequestRedirectFilter(
            Client duoClient,
            String authorizationRequestBaseUri,
            ModelAuditRecorder auditProvider,
            SecurityContextRepository securityContextRepository) {
        super(auditProvider, securityContextRepository);
        this.duoClient = duoClient;
        this.authorizationRequestMatcher = new AntPathRequestMatcher(authorizationRequestBaseUri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!authorizationRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            duoClient.healthCheck();

            @Nullable ModuleAuthentication duoModule = AuthUtil.getProcessingModuleIfExist();
            if (!(duoModule instanceof DuoModuleAuthentication)) {
                LOGGER.error("Couldn't get processing duo module");
                throw new AuthenticationServiceException("web.security.provider.invalid");
            }

            String duoState = duoClient.generateState();
            ((DuoModuleAuthentication) duoModule).setDuoState(duoState);

            String username = ((DuoModuleAuthentication) duoModule).getUsername();
            if (username == null) {
                LOGGER.error("Couldn't get principal username for duo module");
                throw new AuthenticationServiceException("web.security.provider.invalid");
            }

            String authUrl = duoClient.createAuthUrl(username, duoState);

            getRequestCache().saveRequest(request, response);
            getSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), request, response);
            getAuthorizationRedirectStrategy().sendRedirect(request, response, authUrl);
        } catch (Exception ex) {
            unsuccessfulAuthentication(request, response,
                    new InternalAuthenticationServiceException("web.security.provider.invalid", ex));
        }
    }

    @Override
    protected String getAuthenticationType() {
        return "DUO";
    }
}
