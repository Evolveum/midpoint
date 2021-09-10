/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.module;

import java.util.Collections;

import com.evolveum.midpoint.web.security.saml.MidpointMetadataRelyingPartyRegistrationResolver;

import com.evolveum.midpoint.web.security.saml.MidpointSaml2LoginConfigurer;

import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;

import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.*;
import com.evolveum.midpoint.web.security.filter.configurers.MidpointExceptionHandlingConfigurer;
import com.evolveum.midpoint.web.security.module.configuration.SamlModuleWebSecurityConfiguration;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * @author skublik
 */

public class SamlModuleWebSecurityConfig<C extends SamlModuleWebSecurityConfiguration> extends ModuleWebSecurityConfig<C> {

    private static final Trace LOGGER = TraceManager.getTrace(SamlModuleWebSecurityConfig.class);
    public static final String SAML_LOGIN_PATH = "/saml2/select";

    @Autowired
    private ModelAuditRecorder auditProvider;

    public SamlModuleWebSecurityConfig(C configuration) {
        super(configuration);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        http.antMatcher(SecurityUtils.stripEndingSlashes(getPrefix()) + "/**");
        http.csrf().disable();

        getOrApply(http, new MidpointExceptionHandlingConfigurer())
                .authenticationEntryPoint(new SamlAuthenticationEntryPoint(SAML_LOGIN_PATH));

        MidpointSaml2LoginConfigurer configurer = new MidpointSaml2LoginConfigurer(auditProvider);
        configurer.relyingPartyRegistrationRepository(relyingPartyRegistrations())
                .loginProcessingUrl(getConfiguration().getPrefix() + SamlModuleWebSecurityConfiguration.RESPONSE_PROCESSING_URL_SUFFIX)
                .successHandler(getObjectPostProcessor().postProcess(
                        new MidPointAuthenticationSuccessHandler().setPrefix(getConfiguration().getPrefix())))
                .failureHandler(new MidpointAuthenticationFailureHandler());
        try {
            configurer.authenticationManager(new ProviderManager(Collections.emptyList(), authenticationManager()));
        } catch (Exception e) {
            LOGGER.error("Couldn't initialize authentication manager for saml2 module");
        }
        getOrApply(http, configurer);

        Saml2MetadataFilter filter = new Saml2MetadataFilter(new MidpointMetadataRelyingPartyRegistrationResolver(relyingPartyRegistrations()),
                new OpenSamlMetadataResolver());
        filter.setRequestMatcher(new AntPathRequestMatcher( getConfiguration().getPrefix() + "/metadata"));
        http.addFilterAfter(filter, Saml2WebSsoAuthenticationFilter.class);
    }

    private InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrations() {
        return getConfiguration().getRelyingPartyRegistrationRepository();
    }
}
