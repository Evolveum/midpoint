/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter.oidc;

import com.evolveum.midpoint.authentication.impl.filter.configurers.RemoteModuleConfigurer;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcAdditionalConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.OidcClientModuleWebSecurityConfigurer;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.*;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

public final class OidcLoginConfigurer<B extends HttpSecurityBuilder<B>>
        extends RemoteModuleConfigurer<B, OidcLoginConfigurer<B>, OidcLoginAuthenticationFilter> {

    private ClientRegistrationRepository clientRegistrations;
    private Map<String, OidcAdditionalConfiguration> additionalConfiguration = new HashMap<>();

    public OidcLoginConfigurer(ModelAuditRecorder auditProvider) {
        super(auditProvider);
        loginProcessingUrl(OAuth2LoginAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI);
    }

    public OidcLoginConfigurer<B> clientRegistrationRepository(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrations = clientRegistrationRepository;
        return this;
    }

    public OidcLoginConfigurer<B> additionalClientConfiguration(Map<String, OidcAdditionalConfiguration> additionalConfiguration) {
        this.additionalConfiguration = additionalConfiguration;
        return this;
    }

    @Override
    public void init(B http) throws Exception {
        OidcLoginAuthenticationFilter authenticationFilter = new OidcLoginAuthenticationFilter(
                clientRegistrations, getRemoteModuleLoginProcessingUrl(), getAuditProvider());
        this.setAuthenticationFilter(authenticationFilter);

        sendLoginProcessingUrlToSuper();
        super.loginPage(OidcClientModuleWebSecurityConfigurer.OIDC_LOGIN_PATH);
        super.init(http);
    }

    @Override
    public void configure(B http) throws Exception {
        OidcAuthorizationRequestRedirectFilter authorizationRequestFilter = new OidcAuthorizationRequestRedirectFilter(
                clientRegistrations,
                additionalConfiguration,
                getAuthorizationRequestBaseUri(),
                getAuditProvider(),
                http.getSharedObject(SecurityContextRepository.class));
        setAuthorizationFilter(authorizationRequestFilter);

        super.configure(http);
    }
}
