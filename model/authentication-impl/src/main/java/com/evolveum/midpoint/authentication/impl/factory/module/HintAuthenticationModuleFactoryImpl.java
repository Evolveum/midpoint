/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.authentication.HintAuthenticationModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.HintModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.HintAuthenticationProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class HintAuthenticationModuleFactoryImpl extends AbstractModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        HintModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration>,
        HintAuthenticationModuleType,
        HintAuthenticationModuleAuthentication> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof HintAuthenticationModuleType;
    }

    @Override
    protected HintModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration> createModuleConfigurer(HintAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new HintModuleWebSecurityConfigurer<>(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new HintAuthenticationProvider());
    }

    @Override
    protected HintAuthenticationModuleAuthentication createEmptyModuleAuthentication(HintAuthenticationModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        HintAuthenticationModuleAuthentication moduleAuthentication = new HintAuthenticationModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }


}
