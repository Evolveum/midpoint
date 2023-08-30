/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.module.configurer;

import java.util.Collections;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.handler.MidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.authentication.impl.handler.MidpointAuthenticationFailureHandler;
import com.evolveum.midpoint.authentication.impl.filter.saml.MidpointMetadataRelyingPartyRegistrationResolver;
import com.evolveum.midpoint.authentication.impl.filter.saml.MidpointSaml2LoginConfigurer;
import com.evolveum.midpoint.authentication.impl.filter.saml.MidpointSaml2LogoutRequestResolver;
import com.evolveum.midpoint.authentication.impl.filter.saml.MidpointSaml2LogoutRequestSuccessHandler;
import com.evolveum.midpoint.authentication.impl.module.configuration.SamlModuleWebSecurityConfiguration;

import com.evolveum.midpoint.xml.ns._public.common.common_3.Saml2AuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2RelyingPartyInitiatedLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

public class SamlModuleWebSecurityConfigurer extends RemoteModuleWebSecurityConfigurer<SamlModuleWebSecurityConfiguration, Saml2AuthenticationModuleType> {

    private static final Trace LOGGER = TraceManager.getTrace(SamlModuleWebSecurityConfigurer.class);
    public static final String SAML_LOGIN_PATH = "/saml2/select";

    @Autowired private ModelAuditRecorder auditProvider;

    public SamlModuleWebSecurityConfigurer(Saml2AuthenticationModuleType moduleType,
            String sequenceSuffix, AuthenticationChannel channel,
            ObjectPostProcessor<Object> postProcessor,
            ServletRequest request,
            AuthenticationProvider provider) {
        super(moduleType, sequenceSuffix, channel, postProcessor, request, provider);
    }

    @Override
    protected SamlModuleWebSecurityConfiguration buildConfiguration(Saml2AuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ServletRequest request) {
        SamlModuleWebSecurityConfiguration configuration = SamlModuleWebSecurityConfiguration.build(moduleType, sequenceSuffix, getPublicUrlPrefix(request), request);
        configuration.setSequenceSuffix(sequenceSuffix);
        return configuration;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        MidpointSaml2LoginConfigurer configurer = new MidpointSaml2LoginConfigurer<>(auditProvider);
        configurer.relyingPartyRegistrationRepository(relyingPartyRegistrations())
                .loginProcessingUrl(getConfiguration().getPrefixOfModule() + SamlModuleWebSecurityConfiguration.SSO_LOCATION_URL_SUFFIX)
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler()))
                .failureHandler(new MidpointAuthenticationFailureHandler());
        try {
            configurer.authenticationManager(new ProviderManager(Collections.emptyList(), authenticationManager()));
        } catch (Exception e) {
            LOGGER.error("Couldn't initialize authentication manager for saml2 module");
        }
        getOrApply(http, configurer);

        Saml2MetadataFilter filter = new Saml2MetadataFilter(new MidpointMetadataRelyingPartyRegistrationResolver(relyingPartyRegistrations()),
                new OpenSamlMetadataResolver());
        filter.setRequestMatcher(new AntPathRequestMatcher( getConfiguration().getPrefixOfModule() + "/metadata/*"));
        http.addFilterAfter(filter, Saml2WebSsoAuthenticationFilter.class);
    }

    @Override
    protected String getAuthEntryPointUrl() {
        return SAML_LOGIN_PATH;
    }

    @Override
    protected LogoutSuccessHandler getLogoutRequestSuccessHandler() {
        RelyingPartyRegistrationResolver registrationResolver = new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrations());
        Saml2LogoutRequestResolver logoutRequestResolver = new MidpointSaml2LogoutRequestResolver(
                new OpenSaml4LogoutRequestResolver(registrationResolver));
        Saml2RelyingPartyInitiatedLogoutSuccessHandler handler = new Saml2RelyingPartyInitiatedLogoutSuccessHandler(logoutRequestResolver);
        return getObjectPostProcessor().postProcess(new MidpointSaml2LogoutRequestSuccessHandler(
                handler));
    }

    @Override
    protected Class<? extends Authentication> getAuthTokenClass() {
        return Saml2AuthenticationToken.class;
    }

    private InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrations() {
        return getConfiguration().getRelyingPartyRegistrationRepository();
    }
}
