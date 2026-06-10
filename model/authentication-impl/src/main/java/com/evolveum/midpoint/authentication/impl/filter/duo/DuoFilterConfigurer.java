/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter.duo;

import com.duosecurity.Client;

import com.evolveum.midpoint.authentication.impl.filter.configurers.RemoteModuleConfigurer;
import com.evolveum.midpoint.authentication.impl.filter.oidc.OidcLoginAuthenticationFilter;
import com.evolveum.midpoint.authentication.impl.module.configurer.DuoModuleWebSecurityConfigurer;

import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.web.context.SecurityContextRepository;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;

public final class DuoFilterConfigurer<B extends HttpSecurityBuilder<B>>
        extends RemoteModuleConfigurer<B, DuoFilterConfigurer<B>, DuoAuthenticationFilter> {

    private final Client duoClient;

    public DuoFilterConfigurer(Client duoClient, ModelAuditRecorder auditProvider) {
        super(auditProvider);
        this.duoClient = duoClient;
    }

    @Override
    public void init(B http) {
        DuoAuthenticationFilter authenticationFilter = new DuoAuthenticationFilter(getRemoteModuleLoginProcessingUrl(), getAuditProvider());
        this.setAuthenticationFilter(authenticationFilter);

        sendLoginProcessingUrlToSuper();
        super.loginPage(DuoModuleWebSecurityConfigurer.DUO_LOGIN_PATH);
        super.init(http);
    }

    @Override
    public void configure(B http) {

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
