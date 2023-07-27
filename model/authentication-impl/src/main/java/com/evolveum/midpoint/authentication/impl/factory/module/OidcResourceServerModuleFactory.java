/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.channel.RestAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.OidcResourceServerModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.RemoteModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.OidcResourceServerModuleWebSecurityConfigurer;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcResourceServerAuthenticationModuleType;

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
