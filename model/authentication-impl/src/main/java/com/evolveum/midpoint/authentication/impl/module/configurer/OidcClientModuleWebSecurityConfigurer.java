/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcClientModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.oidc.OidcClientLogoutSuccessHandler;
import com.evolveum.midpoint.authentication.impl.oidc.OidcLoginConfigurer;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.Collections;

/**
 * @author skublik
 */

public class OidcClientModuleWebSecurityConfigurer<C extends OidcClientModuleWebSecurityConfiguration> extends RemoteModuleWebSecurityConfigurer<C> {

    private static final Trace LOGGER = TraceManager.getTrace(OidcClientModuleWebSecurityConfigurer.class);
    public static final String OIDC_LOGIN_PATH = "/oidc/select";

    @Autowired
    private ModelAuditRecorder auditProvider;

    private String publicUrlPrefix;

    public OidcClientModuleWebSecurityConfigurer(C configuration) {
        super(configuration);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        OidcLoginConfigurer configurer = new OidcLoginConfigurer(auditProvider);
        configurer.midpointFailureHandler(new MidpointAuthenticationFailureHandler())
                .clientRegistrationRepository(clientRegistrationRepository())
                .loginProcessingUrl(
                        AuthUtil.stripEndingSlashes(getPrefix()) + RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID)
                .authorizationRequestBaseUri(
                        AuthUtil.stripEndingSlashes(getPrefix()) + RemoteModuleAuthenticationImpl.AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX)
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler()));
        try {
            configurer.authenticationManager(new ProviderManager(Collections.emptyList(), authenticationManager()));
        } catch (Exception e) {
            LOGGER.error("Couldn't initialize authentication manager for oidc module");
        }
        getOrApply(http, configurer);
    }

    @Override
    protected String getAuthEntryPointUrl() {
        return OIDC_LOGIN_PATH;
    }

    @Override
    protected LogoutSuccessHandler getLogoutRequestSuccessHandler() {
        OidcClientLogoutSuccessHandler logoutRequestSuccessHandler =
                getObjectPostProcessor().postProcess(new OidcClientLogoutSuccessHandler(clientRegistrationRepository()));
        logoutRequestSuccessHandler.setPostLogoutRedirectUri(getConfiguration().getPrefixOfSequence());
        logoutRequestSuccessHandler.setPublicUrlPrefix(this.publicUrlPrefix);
        return logoutRequestSuccessHandler;
    }

    private InMemoryClientRegistrationRepository clientRegistrationRepository() {
        return getConfiguration().getClientRegistrationRepository();
    }

    @Override
    protected Class<? extends Authentication> getAuthTokenClass() {
        return OAuth2LoginAuthenticationToken.class;
    }

    public void setPublicUrlPrefix(String publicUrlPrefix) {
        this.publicUrlPrefix = publicUrlPrefix;
    }
}
