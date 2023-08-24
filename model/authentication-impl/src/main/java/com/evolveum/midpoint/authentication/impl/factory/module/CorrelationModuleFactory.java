/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationModuleOptionsType;

import jakarta.servlet.ServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.CorrelationModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.CorrelationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.CorrelationModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.CorrelationProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationAuthenticationModuleType;

/**
 * @author skublik
 */
@Component
public class CorrelationModuleFactory extends AbstractModuleFactory
        <LoginFormModuleWebSecurityConfiguration,
                CorrelationModuleWebSecurityConfigurer,
                CorrelationAuthenticationModuleType,
                CorrelationModuleAuthentication> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof CorrelationAuthenticationModuleType;
    }

    @Override
    protected CorrelationModuleWebSecurityConfigurer createModuleConfigurer(CorrelationAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new CorrelationModuleWebSecurityConfigurer(
                moduleType,
                sequenceSuffix,
                authenticationChannel,
                objectPostProcessor,
                request,
                new CorrelationProvider());
    }

    @Override
    protected CorrelationModuleAuthentication createEmptyModuleAuthentication(CorrelationAuthenticationModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        CorrelationModuleAuthenticationImpl moduleAuthentication = new CorrelationModuleAuthenticationImpl(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setCorrelators(moduleType.getCorrelator());
        moduleAuthentication.setCorrelationMaxUsersNumber(getCorrelationMaxUserNumber(moduleType.getOptions()));
        return moduleAuthentication;
    }

    private Integer getCorrelationMaxUserNumber(CorrelationModuleOptionsType options) {
        if (options == null) {
            return null;
        }
        return options.getCandidateLimit();
    }


    @Override
    protected void isSupportedChannel(AuthenticationChannel authenticationChannel) {
        if (authenticationChannel == null) {
            return;
        }
        if (!SchemaConstants.CHANNEL_IDENTITY_RECOVERY_URI.equals(authenticationChannel.getChannelId())) {
            throw new IllegalArgumentException("Unsupported factory " + this.getClass().getSimpleName()
                    + " for channel " + authenticationChannel.getChannelId());
        }
    }
}
