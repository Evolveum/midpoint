/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.channel.RestAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.OidcResourceServerModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.OidcResourceServerModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.OidcResourceServerModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.OidcResourceServerProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletRequest;
import java.util.Map;

/**
 * @author skublik
 */
@Component
public class OidcResourceServerModuleFactory extends RemoteModuleFactory {

    private static final Trace LOGGER = TraceManager.getTrace(OidcResourceServerModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof OidcAuthenticationModuleType && authenticationChannel instanceof RestAuthenticationChannel;
    }

    @Override
    public AuthModule createModuleFilter(AbstractAuthenticationModuleType moduleType, String sequenceSuffix, ServletRequest request,
                                         Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy,
            CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType necessity) throws Exception {
        if (!(moduleType instanceof OidcAuthenticationModuleType)) {
            LOGGER.error("This factory support only OidcAuthenticationModuleType, but modelType is " + moduleType);
            return null;
        }

        if (((OidcAuthenticationModuleType) moduleType).getResourceServer() == null) {
            LOGGER.error("Resource configuration of OidcAuthenticationModuleType is null");
            return null;
        }

        isSupportedChannel(authenticationChannel);

        OidcResourceServerModuleWebSecurityConfiguration configuration = OidcResourceServerModuleWebSecurityConfiguration.build(
                (OidcAuthenticationModuleType)moduleType, sequenceSuffix);
        configuration.setSequenceSuffix(sequenceSuffix);
        OidcResourceServerAuthenticationModuleType resourceServer = ((OidcAuthenticationModuleType) moduleType).getResourceServer();
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        if (resourceServer.getNameOfUsernameClaim() != null) {
            jwtAuthenticationConverter.setPrincipalClaimName(resourceServer.getNameOfUsernameClaim());
        }
        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(
                new OidcResourceServerProvider(configuration.getDecoder(), jwtAuthenticationConverter)));

        OidcResourceServerModuleWebSecurityConfigurer<OidcResourceServerModuleWebSecurityConfiguration> module
                = getObjectObjectPostProcessor().postProcess(new OidcResourceServerModuleWebSecurityConfigurer<>(configuration));
        module.setObjectPostProcessor(getObjectObjectPostProcessor());
        HttpSecurity http = module.getNewHttpSecurity();
        setSharedObjects(http, sharedObjects);

        ModuleAuthenticationImpl moduleAuthentication =
                createEmptyModuleAuthentication(configuration, resourceServer, necessity);
        moduleAuthentication.setFocusType(moduleType.getFocusType());
        SecurityFilterChain filter = http.build();
        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
    }

    private ModuleAuthenticationImpl createEmptyModuleAuthentication(OidcResourceServerModuleWebSecurityConfiguration configuration,
            OidcResourceServerAuthenticationModuleType resourceServer, AuthenticationSequenceModuleType sequenceModule) {
        OidcResourceServerModuleAuthentication moduleAuthentication = new OidcResourceServerModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setRealm(resourceServer.getRealm());
        return moduleAuthentication;
    }

    protected void isSupportedChannel(AuthenticationChannel authenticationChannel) {
        if (authenticationChannel == null) {
            return;
        }
        if (!SchemaConstants.CHANNEL_REST_URI.equals(authenticationChannel.getChannelId())) {
            throw new IllegalArgumentException("Unsupported factory " + this.getClass().getSimpleName()
                    + " for channel " + authenticationChannel.getChannelId());
        }
    }
}
