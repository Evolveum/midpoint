/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter.duo;

import com.duosecurity.Client;

import com.evolveum.midpoint.authentication.impl.filter.RemoteModuleAuthorizationFilter;
import com.evolveum.midpoint.authentication.impl.filter.configurers.RemoteModuleConfigurer;
import com.evolveum.midpoint.authentication.impl.filter.oidc.OidcLoginAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.module.configurer.DuoModuleWebSecurityConfigurer;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import com.evolveum.midpoint.authentication.impl.filter.oidc.OidcAuthorizationRequestRedirectFilter;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;

public final class DuoFilterConfigurer<B extends HttpSecurityBuilder<B>>
        extends RemoteModuleConfigurer<B, DuoFilterConfigurer<B>, DuoAuthenticationFilter> {

    private final Client duoClient;

    public DuoFilterConfigurer(Client duoClient, ModelAuditRecorder auditProvider) {
        super(auditProvider);
        this.duoClient = duoClient;
    }

    @Override
    public void init(B http) throws Exception {
        DuoAuthenticationFilter authenticationFilter = new DuoAuthenticationFilter(getRemoteModuleLoginProcessingUrl(), getAuditProvider());
        this.setAuthenticationFilter(authenticationFilter);

        sendLoginProcessingUrlToSuper();
        super.loginPage(DuoModuleWebSecurityConfigurer.DUO_LOGIN_PATH);
        super.init(http);
    }

    @Override
    public void configure(B http) throws Exception {

        DuoAuthorizationRequestRedirectFilter authorizationRequestFilter = new DuoAuthorizationRequestRedirectFilter(
                this.duoClient,
                getAuthorizationRequestBaseUri(),
                getAuditProvider(),
                http.getSharedObject(SecurityContextRepository.class));

        setAuthorizationFilter(authorizationRequestFilter);

        try {
            super.configure(http);
        } catch (IllegalArgumentException e) {
            http.addFilterBefore(getAuthenticationFilter(), OidcLoginAuthenticationFilter.class);
        }
    }
}
