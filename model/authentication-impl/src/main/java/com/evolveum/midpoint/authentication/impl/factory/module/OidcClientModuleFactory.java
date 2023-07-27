/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.impl.channel.RestAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.OidcClientModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcClientModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.OidcClientModuleWebSecurityConfigurer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcAuthenticationModuleType;

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
        if (moduleType.getClient().isEmpty()) {
            LOGGER.error("Client configuration of OidcAuthenticationModuleType is null");
            return null; //TODO shouldn't we throw exception?
        }
        return new OidcClientModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request);
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(OidcAuthenticationModuleType moduleType, OidcClientModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        OidcClientModuleAuthenticationImpl moduleAuthentication = new OidcClientModuleAuthenticationImpl(sequenceModule);
        List<IdentityProvider> providers = new ArrayList<>();
        for (ClientRegistration client : configuration.getClientRegistrationRepository()) {
            IdentityProvider provider = createIdentityProvider(RemoteModuleAuthenticationImpl.AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID,
                    client.getRegistrationId(),
                    request, configuration, client.getClientName());
            providers.add(provider);
        }
        moduleAuthentication.setClientsRepository(configuration.getClientRegistrationRepository());
        moduleAuthentication.setProviders(providers);
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        return moduleAuthentication;
    }

}
