/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.authentication.impl.saml.MidpointSaml2WebSsoAuthenticationRequestFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequest;

import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.provider.Saml2Provider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.SamlModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.module.authentication.Saml2ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.SamlAdditionalConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configuration.SamlModuleWebSecurityConfiguration;

import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class Saml2ModuleFactory extends RemoteModuleFactory<SamlModuleWebSecurityConfiguration, SamlModuleWebSecurityConfigurer, Saml2AuthenticationModuleType, ModuleAuthenticationImpl> {

    private static final Trace LOGGER = TraceManager.getTrace(Saml2ModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof Saml2AuthenticationModuleType;
    }

    @Override
    protected SamlModuleWebSecurityConfigurer createModuleConfigurer(Saml2AuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor objectPostProcessor, ServletRequest request) {
        return new SamlModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel, objectPostProcessor, request, new Saml2Provider());

    }

    public AuthModule<ModuleAuthenticationImpl> createModuleFilter(Saml2AuthenticationModuleType moduleType, String sequenceSuffix, ServletRequest request,
                                         Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy,
            CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception {
        if (!(moduleType instanceof Saml2AuthenticationModuleType)) {
            LOGGER.error("This factory support only Saml2AuthenticationModuleType, but modelType is " + moduleType);
            return null;
        }

        isSupportedChannel(authenticationChannel);

        SamlModuleWebSecurityConfiguration configuration = SamlModuleWebSecurityConfiguration.build(moduleType, sequenceSuffix, getPublicUrlPrefix(request), request);
        configuration.setSequenceSuffix(sequenceSuffix);
        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(new Saml2Provider()));

        SamlModuleWebSecurityConfigurer module = getObjectObjectPostProcessor().postProcess(
                new SamlModuleWebSecurityConfigurer(configuration));
        HttpSecurity http = null;//getNewHttpSecurity(module);
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(configuration, sequenceModule, request);
        moduleAuthentication.setFocusType(moduleType.getFocusType());
        SecurityFilterChain filter = http.build();

        //TODO filterimg
        for (Filter f : filter.getFilters()){
            if (f instanceof MidpointSaml2WebSsoAuthenticationRequestFilter) {
                ((MidpointSaml2WebSsoAuthenticationRequestFilter) f).getAuthenticationRequestResolver().setRequestMatcher(
                        new AntPathRequestMatcher(module.getPrefix()
                                + RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID));
                break;
            }
        }
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
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
            IdentityProvider provider = createIdentityProvider(p, request, configuration);
            providers.add(provider);
        }
        moduleAuthentication.setProviders(providers);
        moduleAuthentication.setAdditionalConfiguration(configuration.getAdditionalConfiguration());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        return moduleAuthentication;
    }

    private IdentityProvider createIdentityProvider(RelyingPartyRegistration relyingParty, ServletRequest request, SamlModuleWebSecurityConfiguration configuration) {
        String authRequestPrefixUrl = request.getServletContext().getContextPath() + configuration.getPrefixOfModule()
                + RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID;
        SamlAdditionalConfiguration config = configuration.getAdditionalConfiguration().get(relyingParty.getRegistrationId());
        return new IdentityProvider()
                .setLinkText(config.getLinkText())
                .setRedirectLink(authRequestPrefixUrl.replace("{registrationId}", relyingParty.getRegistrationId()));
    }

    public ModuleAuthenticationImpl createEmptyModuleAuthentication(
            SamlModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        Saml2ModuleAuthenticationImpl moduleAuthentication = new Saml2ModuleAuthenticationImpl(sequenceModule);
        List<IdentityProvider> providers = new ArrayList<>();
        configuration.getRelyingPartyRegistrationRepository().forEach(
                p -> {
                    String authRequestPrefixUrl = request.getServletContext().getContextPath() + configuration.getPrefixOfModule()
                            + RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID;
                    SamlAdditionalConfiguration config = configuration.getAdditionalConfiguration().get(p.getRegistrationId());
                    IdentityProvider mp = new IdentityProvider()
                                .setLinkText(config.getLinkText())
                                .setRedirectLink(authRequestPrefixUrl.replace("{registrationId}", p.getRegistrationId()));
                        providers.add(mp);
                }
        );
        moduleAuthentication.setProviders(providers);
        moduleAuthentication.setAdditionalConfiguration(configuration.getAdditionalConfiguration());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        return moduleAuthentication;
    }
}
