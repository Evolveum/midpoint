/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.Saml2ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.SamlAdditionalConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configuration.SamlModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.SamlModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.Saml2Provider;
import com.evolveum.midpoint.authentication.impl.filter.saml.MidpointSaml2WebSsoAuthenticationRequestFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Saml2AuthenticationModuleType;

/**
 * @author skublik
 */
@Component
public class Saml2ModuleFactory extends RemoteModuleFactory<SamlModuleWebSecurityConfiguration, SamlModuleWebSecurityConfigurer, Saml2AuthenticationModuleType, ModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof Saml2AuthenticationModuleType;
    }

    @Override
    protected SamlModuleWebSecurityConfigurer createModuleConfigurer(Saml2AuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor objectPostProcessor, ServletRequest request) {
        return new SamlModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel, objectPostProcessor, request, new Saml2Provider());

    }

    @Override
    protected void postProcessFilter(SecurityFilterChain filter, SamlModuleWebSecurityConfigurer configurer) {
        for (Filter f : filter.getFilters()){
            if (f instanceof MidpointSaml2WebSsoAuthenticationRequestFilter samlFilter) {
                samlFilter.getAuthenticationRequestResolver().setRequestMatcher(
                        new AntPathRequestMatcher(configurer.getPrefix()
                                + RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID));
                break;
            }
        }
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(Saml2AuthenticationModuleType moduleType,
            SamlModuleWebSecurityConfiguration configuration,
            AuthenticationSequenceModuleType sequenceModule,
            ServletRequest request) {

        Saml2ModuleAuthenticationImpl moduleAuthentication = new Saml2ModuleAuthenticationImpl(sequenceModule);
        List<IdentityProvider> providers = new ArrayList<>();
        for (RelyingPartyRegistration p : configuration.getRelyingPartyRegistrationRepository()) {
            SamlAdditionalConfiguration config = configuration.getAdditionalConfiguration().get(p.getRegistrationId());
            IdentityProvider provider = createIdentityProvider(RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID,
                    p.getRegistrationId(), request, configuration, config.getLinkText());
            providers.add(provider);
        }
        moduleAuthentication.setProviders(providers);
        moduleAuthentication.setAdditionalConfiguration(configuration.getAdditionalConfiguration());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        return moduleAuthentication;
    }

}
