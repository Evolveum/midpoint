/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.channel.RestAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.DuoModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.OidcClientModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.DuoModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcClientModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.DuoModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.module.configurer.OidcClientModuleWebSecurityConfigurer;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DuoAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcAuthenticationModuleType;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */
@Component
public class DuoModuleFactory extends RemoteModuleFactory<
        DuoModuleWebSecurityConfiguration,
        DuoModuleWebSecurityConfigurer,
        DuoAuthenticationModuleType,
        ModuleAuthenticationImpl> {

    private static final Trace LOGGER = TraceManager.getTrace(DuoModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof DuoAuthenticationModuleType;
    }

    @Override
    protected DuoModuleWebSecurityConfigurer createModuleConfigurer(
            DuoAuthenticationModuleType moduleType,
            String sequenceSuffix,
            AuthenticationChannel authenticationChannel,
            ObjectPostProcessor<Object> objectPostProcessor,
            ServletRequest request) {
        return new DuoModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request);
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(
            DuoAuthenticationModuleType moduleType,
            DuoModuleWebSecurityConfiguration configuration,
            AuthenticationSequenceModuleType sequenceModule,
            ServletRequest request) {
        DuoModuleAuthentication moduleAuthentication = new DuoModuleAuthentication(sequenceModule);

        IdentityProvider provider = createIdentityProvider(
                RemoteModuleAuthenticationImpl.AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX,
                "",
                request,
                configuration,
                configuration.getModuleIdentifier());
        moduleAuthentication.setProviders(List.of(provider));

        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        return moduleAuthentication;
    }
}
