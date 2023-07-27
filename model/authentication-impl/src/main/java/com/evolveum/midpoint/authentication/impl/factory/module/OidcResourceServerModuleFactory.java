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
import com.evolveum.midpoint.authentication.impl.module.configuration.JwtOidcResourceServerConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configuration.OpaqueTokenOidcResourceServerConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configuration.RemoteModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.OidcResourceServerModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.OidcResourceServerProvider;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.security.config.annotation.ObjectPostProcessor;
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
public class OidcResourceServerModuleFactory<C extends RemoteModuleWebSecurityConfiguration> extends RemoteModuleFactory<
        C,
        OidcResourceServerModuleWebSecurityConfigurer<C>,
        OidcAuthenticationModuleType,
        ModuleAuthenticationImpl> {

    private static final Trace LOGGER = TraceManager.getTrace(OidcResourceServerModuleFactory.class);

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof OidcAuthenticationModuleType && authenticationChannel instanceof RestAuthenticationChannel;
    }

    @Override
    protected OidcResourceServerModuleWebSecurityConfigurer<C> createModuleConfigurer(OidcAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        OidcResourceServerAuthenticationModuleType resourceServer = moduleType.getResourceServer();
        if (resourceServer == null) {
            LOGGER.error("Resource configuration of OidcAuthenticationModuleType is null");
            return null;
        }

        return new OidcResourceServerModuleWebSecurityConfigurer<>(moduleType, sequenceSuffix, authenticationChannel, objectPostProcessor, request, null);
    }

    @Override
    protected ModuleAuthenticationImpl createEmptyModuleAuthentication(OidcAuthenticationModuleType moduleType, C configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        OidcResourceServerModuleAuthentication moduleAuthentication = new OidcResourceServerModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setRealm(getRealm(moduleType.getResourceServer()));
        return moduleAuthentication;
    }

//    public AuthModule<ModuleAuthenticationImpl> createModuleFilter(OidcAuthenticationModuleType moduleType, String sequenceSuffix, ServletRequest request,
//                                         Map<Class<?>, Object> sharedObjects, AuthenticationModulesType authenticationsPolicy,
//            CredentialsPolicyType credentialPolicy, AuthenticationChannel authenticationChannel, AuthenticationSequenceModuleType necessity) throws Exception {
//        if (!(moduleType instanceof OidcAuthenticationModuleType)) {
//            LOGGER.error("This factory support only OidcAuthenticationModuleType, but modelType is " + moduleType);
//            return null;
//        }
//
//        OidcResourceServerAuthenticationModuleType resourceServer = moduleType.getResourceServer();
//        if (resourceServer == null) {
//            LOGGER.error("Resource configuration of OidcAuthenticationModuleType is null");
//            return null;
//        }
//
//        isSupportedChannel(authenticationChannel);
//
//        RemoteModuleWebSecurityConfiguration configuration;
//        if (resourceServer.getJwt() != null) {
//            configuration = createJwtResourceServerConfiguration(moduleType, resourceServer, sequenceSuffix);
//        } else if (resourceServer.getOpaqueToken() != null) {
//            configuration = createOpaqueTokenResourceServerConfiguration(moduleType, resourceServer, sequenceSuffix);
//        } else {
//            configuration = createJwtResourceServerConfiguration(moduleType, resourceServer, sequenceSuffix);
//        }
//
//
//        OidcResourceServerModuleWebSecurityConfigurer<RemoteModuleWebSecurityConfiguration> module
//                = getObjectObjectPostProcessor().postProcess(new OidcResourceServerModuleWebSecurityConfigurer<>(configuration));
//        module.setObjectPostProcessor(getObjectObjectPostProcessor());
//        HttpSecurity http = module.getNewHttpSecurity();
//        setSharedObjects(http, sharedObjects);
//
//        ModuleAuthenticationImpl moduleAuthentication =
//                createEmptyModuleAuthentication(configuration, resourceServer, necessity);
//        moduleAuthentication.setFocusType(moduleType.getFocusType());
//        SecurityFilterChain filter = http.build();
//        return AuthModuleImpl.build(filter, configuration, moduleAuthentication);
//    }

//    private RemoteModuleWebSecurityConfiguration createOpaqueTokenResourceServerConfiguration(
//            AbstractAuthenticationModuleType moduleType,
//            OidcResourceServerAuthenticationModuleType resourceServer,
//            String sequenceSuffix) {
//        OpaqueTokenOidcResourceServerConfiguration configuration =
//                OpaqueTokenOidcResourceServerConfiguration.build(
//                        (OidcAuthenticationModuleType)moduleType,
//                        sequenceSuffix);
//        configuration.setSequenceSuffix(sequenceSuffix);
//
//        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(
//                new OidcResourceServerProvider(configuration.getIntrospector())));
//        return configuration;
//    }

//    private RemoteModuleWebSecurityConfiguration createJwtResourceServerConfiguration(
//            AbstractAuthenticationModuleType moduleType,
//            OidcResourceServerAuthenticationModuleType resourceServer,
//            String sequenceSuffix) {
//
//        JwtOidcResourceServerConfiguration configuration =
//                JwtOidcResourceServerConfiguration.build(
//                        (OidcAuthenticationModuleType)moduleType,
//                        sequenceSuffix);
//        configuration.setSequenceSuffix(sequenceSuffix);
//
//        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
//        if (resourceServer.getJwt() != null && resourceServer.getJwt().getNameOfUsernameClaim() != null) {
//            jwtAuthenticationConverter.setPrincipalClaimName(resourceServer.getJwt().getNameOfUsernameClaim());
//        } else if (resourceServer.getNameOfUsernameClaim() != null) {
//            jwtAuthenticationConverter.setPrincipalClaimName(resourceServer.getNameOfUsernameClaim());
//        }
//        configuration.addAuthenticationProvider(getObjectObjectPostProcessor().postProcess(
//                new OidcResourceServerProvider(configuration.getDecoder(), jwtAuthenticationConverter)));
//        return configuration;
//    }

//    private ModuleAuthenticationImpl createEmptyModuleAuthentication(RemoteModuleWebSecurityConfiguration configuration,
//            OidcResourceServerAuthenticationModuleType resourceServer, AuthenticationSequenceModuleType sequenceModule) {
//        OidcResourceServerModuleAuthentication moduleAuthentication = new OidcResourceServerModuleAuthentication(sequenceModule);
//        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
//        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
//        moduleAuthentication.setRealm(getRealm(resourceServer));
//        return moduleAuthentication;
//    }

    private String getRealm(OidcResourceServerAuthenticationModuleType resourceServer) {
        if (resourceServer.getJwt() != null) {
            return resourceServer.getJwt().getRealm();
        }

        if (resourceServer.getOpaqueToken() != null) {
            return resourceServer.getOpaqueToken().getRealm();
        }

        return resourceServer.getRealm();
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
