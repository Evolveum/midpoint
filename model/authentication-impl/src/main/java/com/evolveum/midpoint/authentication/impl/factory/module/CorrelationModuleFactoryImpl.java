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
public class CorrelationModuleFactoryImpl extends AbstractModuleFactory
        <LoginFormModuleWebSecurityConfiguration,
                CorrelationModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration>,
                CorrelationAuthenticationModuleType,
                CorrelationModuleAuthentication> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof CorrelationAuthenticationModuleType;
    }

    @Override
    protected CorrelationModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration> createModuleConfigurer(CorrelationAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new CorrelationModuleWebSecurityConfigurer<>(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new CorrelationProvider());
    }

    @Override
    protected CorrelationModuleAuthentication createEmptyModuleAuthentication(CorrelationAuthenticationModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        CorrelationModuleAuthenticationImpl moduleAuthentication = new CorrelationModuleAuthenticationImpl(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setCorrelatorIdentifier(moduleType.getCorrelationRuleIdentifier());
        return moduleAuthentication;
    }

}
