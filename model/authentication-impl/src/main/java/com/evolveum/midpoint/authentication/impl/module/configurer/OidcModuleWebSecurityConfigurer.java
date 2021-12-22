/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.authentication.impl.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcModuleWebSecurityConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Collections;

/**
 * @author skublik
 */

public class OidcModuleWebSecurityConfigurer<C extends OidcModuleWebSecurityConfiguration> extends ModuleWebSecurityConfigurer<C> {

    private static final Trace LOGGER = TraceManager.getTrace(OidcModuleWebSecurityConfigurer.class);
    public static final String OIDC_LOGIN_PATH = "/oidc/select";

    public OidcModuleWebSecurityConfigurer(C configuration) {
        super(configuration);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        http.antMatcher(AuthUtil.stripEndingSlashes(getPrefix()) + "/**");
        http.csrf().disable();

        getOrApply(http, new MidpointExceptionHandlingConfigurer<>())
                .authenticationEntryPoint(new WicketLoginUrlAuthenticationEntryPoint(OIDC_LOGIN_PATH));

        http.oauth2Login()
                .clientRegistrationRepository(clientRegistrationRepository())
                .loginProcessingUrl(AuthUtil.stripEndingSlashes(getPrefix()) + RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID)
                .loginPage(OIDC_LOGIN_PATH)
                .authorizationEndpoint().baseUri(
                        AuthUtil.stripEndingSlashes(getPrefix()) + RemoteModuleAuthenticationImpl.AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX);
        http.authenticationManager(new ProviderManager(Collections.emptyList(), authenticationManager()));

        LogoutSuccessHandler logoutRequestSuccessHandler = logoutRequestSuccessHandler();
        http.logout().clearAuthentication(true)
                .logoutRequestMatcher(new AntPathRequestMatcher(getPrefix() + "/logout"))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(logoutRequestSuccessHandler);
    }

    private InMemoryClientRegistrationRepository clientRegistrationRepository() {
        return getConfiguration().getClientRegistrationRepository();
    }

    private LogoutSuccessHandler logoutRequestSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(
                        clientRegistrationRepository());
        return oidcLogoutSuccessHandler;
    }
}
