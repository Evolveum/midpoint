/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.impl.channel.RestAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.OidcClientModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcClientModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.OidcClientModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.OidcClientProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author skublik
 */
@Component
public class OidcClientModuleFactory extends RemoteModuleFactory<
        OidcClientModuleWebSecurityConfiguration,
        OidcClientModuleWebSecurityConfigurer,
        OidcAuthenticationModuleType,
        ModuleAuthenticationImpl> {

    private static final Trace LOGGER = TraceManager.getTrace(OidcClientModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof OidcAuthenticationModuleType && !(authenticationChannel instanceof RestAuthenticationChannel);
    }

    @Override
    protected OidcClientModuleWebSecurityConfigurer createModuleConfigurer(OidcAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new OidcClientModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new OidcClientProvider(null));//TODO configuration.getAdditionalConfiguration()));
    }

    public AuthModule<ModuleAuthenticationImpl> createModuleFilter(OidcAuthenticationModuleType moduleType, String sequenceSuffix, ServletRequest request,
                                         Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy,
            CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType sequenceModule) throws Exception {
        if (!(moduleType instanceof OidcAuthenticationModuleType)) {
            LOGGER.error("This factory support only OidcAuthenticationModuleType, but modelType is " + moduleType);
            return null;
        }

        if (moduleType.getClient().isEmpty()) {
            LOGGER.error("Client configuration of OidcAuthenticationModuleType is null");
            return null;
        }

        isSupportedChannel(authenticationChannel);

        OidcClientModuleWebSecurityConfiguration configuration = OidcClientModuleWebSecurityConfiguration.build(
                moduleType, sequenceSuffix, getPublicUrlPrefix(request), request);
        configuration.setSequenceSuffix(sequenceSuffix);
        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(
                new OidcClientProvider(configuration.getAdditionalConfiguration())));

        OidcClientModuleWebSecurityConfigurer module = getObjectObjectPostProcessor().postProcess(
                new OidcClientModuleWebSecurityConfigurer(configuration));
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        module.setPublicUrlPrefix(getPublicUrlPrefix(request));


        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);
        SecurityFilterChain filter = http.build();

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(configuration, sequenceModule, request);
        moduleAuthentication.setFocusType(moduleType.getFocusType());

        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(OidcAuthenticationModuleType moduleType, OidcClientModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        OidcClientModuleAuthenticationImpl moduleAuthentication = new OidcClientModuleAuthenticationImpl(sequenceModule);
        List<IdentityProvider> providers = new ArrayList<>();
        configuration.getClientRegistrationRepository().forEach(
                client -> {
                    IdentityProvider provider = createIdentityProvider(client, request, configuration);
                    providers.add(provider);
                }
        );
        moduleAuthentication.setClientsRepository(configuration.getClientRegistrationRepository());
        moduleAuthentication.setProviders(providers);
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        return moduleAuthentication;
    }

    private IdentityProvider createIdentityProvider(ClientRegistration client, ServletRequest request, OidcClientModuleWebSecurityConfiguration configuration) {
        String authRequestPrefixUrl = request.getServletContext().getContextPath() + configuration.getPrefixOfModule()
                + OidcClientModuleAuthenticationImpl.AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID;
        return new IdentityProvider()
                .setLinkText(client.getClientName())
                .setRedirectLink(authRequestPrefixUrl.replace("{registrationId}", client.getRegistrationId()));
    }

    public ModuleAuthenticationImpl createEmptyModuleAuthentication(
            OidcClientModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        OidcClientModuleAuthenticationImpl moduleAuthentication = new OidcClientModuleAuthenticationImpl(sequenceModule);
        List<IdentityProvider> providers = new ArrayList<>();
        configuration.getClientRegistrationRepository().forEach(
                client -> {
                    String authRequestPrefixUrl = request.getServletContext().getContextPath() + configuration.getPrefixOfModule()
                            + OidcClientModuleAuthenticationImpl.AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID;
                    IdentityProvider mp = new IdentityProvider()
                                .setLinkText(client.getClientName())
                                .setRedirectLink(authRequestPrefixUrl.replace("{registrationId}", client.getRegistrationId()));
                        providers.add(mp);
                }
        );
        moduleAuthentication.setClientsRepository(configuration.getClientRegistrationRepository());
        moduleAuthentication.setProviders(providers);
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        return moduleAuthentication;
    }
}
