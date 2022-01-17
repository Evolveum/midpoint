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
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.OidcModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.OidcModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.OidcProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author skublik
 */
@Component
public class OidcModuleFactory extends RemoteModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(OidcModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType) {
        return moduleType instanceof OidcAuthenticationModuleType;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String sequenceSuffix, ServletRequest request,
                                         Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy, CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel) throws Exception {
        if (!(moduleType instanceof OidcAuthenticationModuleType)) {
            LOGGER.error("This factory support only OidcAuthenticationModuleType, but modelType is " + moduleType);
            return null;
        }

        isSupportedChannel(authenticationChannel);

        OidcModuleWebSecurityConfiguration.setProtector(getProtector());
        OidcModuleWebSecurityConfiguration configuration = OidcModuleWebSecurityConfiguration.build(
                (OidcAuthenticationModuleType)moduleType, sequenceSuffix, getPublicUrlPrefix(request), request);
        configuration.setSequenceSuffix(sequenceSuffix);
        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(
                new OidcProvider(configuration.getAdditionalConfiguration())));

        OidcModuleWebSecurityConfigurer<OidcModuleWebSecurityConfiguration> module = getObjectObjectPostProcessor().postProcess(
                new OidcModuleWebSecurityConfigurer<>(configuration));
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication = createEmptyModuleAuthentication(configuration, request);
        moduleAuthentication.setFocusType(moduleType.getFocusType());
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    public ModuleAuthenticationImpl createEmptyModuleAuthentication(OidcModuleWebSecurityConfiguration configuration, ServletRequest request) {
        OidcModuleAuthenticationImpl moduleAuthentication = new OidcModuleAuthenticationImpl();
        List<IdentityProvider> providers = new ArrayList<>();
        configuration.getClientRegistrationRepository().forEach(
                client -> {
                    String authRequestPrefixUrl = request.getServletContext().getContextPath() + configuration.getPrefixOfModule()
                            + OidcModuleAuthenticationImpl.AUTHORIZATION_REQUEST_PROCESSING_URL_SUFFIX_WITH_REG_ID;
                    IdentityProvider mp = new IdentityProvider()
                                .setLinkText(client.getClientName())
                                .setRedirectLink(authRequestPrefixUrl.replace("{registrationId}", client.getRegistrationId()));
                        providers.add(mp);
                }
        );
        moduleAuthentication.setClientsRepository(configuration.getClientRegistrationRepository());
        moduleAuthentication.setProviders(providers);
        moduleAuthentication.setNameOfModule(configuration.getNameOfModule());
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        return moduleAuthentication;
    }
}
