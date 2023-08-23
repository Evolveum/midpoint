/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.filter.duo.DuoFilterConfigurer;
import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.token.DuoRequestToken;
import com.evolveum.midpoint.authentication.impl.module.configuration.DuoModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.provider.DuoProvider;
import com.evolveum.midpoint.authentication.impl.provider.OidcClientProvider;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DuoAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.Collections;

/**
 * @author skublik
 */

public class DuoModuleWebSecurityConfigurer
        extends RemoteModuleWebSecurityConfigurer<DuoModuleWebSecurityConfiguration, DuoAuthenticationModuleType> {

    private static final Trace LOGGER = TraceManager.getTrace(DuoModuleWebSecurityConfigurer.class);
    public static final String DUO_LOGIN_PATH = "/duo/select";

    @Autowired private ModelAuditRecorder auditProvider;

    public DuoModuleWebSecurityConfigurer(DuoAuthenticationModuleType moduleType,
                                          String prefix, AuthenticationChannel authenticationChannel,
                                          ObjectPostProcessor<Object> postProcessor, ServletRequest request) {
        super(moduleType, prefix, authenticationChannel, postProcessor, request, null);

    }

    @Override
    protected DuoModuleWebSecurityConfiguration buildConfiguration(
            DuoAuthenticationModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ServletRequest request) {
        DuoModuleWebSecurityConfiguration configuration = DuoModuleWebSecurityConfiguration.build(
                moduleType, sequenceSuffix, getPublicUrlPrefix(request), request);
        configuration.setSequenceSuffix(sequenceSuffix);

        configuration.addAuthenticationProvider(getObjectPostProcessor().postProcess(
                new DuoProvider(configuration.getDuoClient())));

        return configuration;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        DuoFilterConfigurer configurer = new DuoFilterConfigurer(getConfiguration().getDuoClient(), auditProvider);
        configurer.midpointFailureHandler(new MidpointAuthenticationFailureHandler())
                .loginProcessingUrl(
                        AuthUtil.stripEndingSlashes(getPrefix()) + RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX)
                .authorizationRequestBaseUri(
                        AuthUtil.stripEndingSlashes(getPrefix()) + RemoteModuleAuthenticationImpl.AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX)
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler()));
        try {
            configurer.authenticationManager(new ProviderManager(Collections.emptyList(), authenticationManager()));
        } catch (Exception e) {
            LOGGER.error("Couldn't initialize authentication manager for duo module");
        }
        getOrApply(http, configurer);
    }

    @Override
    protected String getAuthEntryPointUrl() {
        return DUO_LOGIN_PATH;
    }

    @Override
    protected LogoutSuccessHandler getLogoutRequestSuccessHandler() {
        return createLogoutHandler();
    }

    @Override
    protected Class<? extends Authentication> getAuthTokenClass() {
        return DuoRequestToken.class;
    }
}
