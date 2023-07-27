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
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.FocusIdentificationModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.FocusIdentificationProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class FocusIdentificationModuleFactoryImpl extends AbstractModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        FocusIdentificationModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration>,
        FocusIdentificationAuthenticationModuleType,
        FocusIdentificationModuleAuthentication> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof FocusIdentificationAuthenticationModuleType;
    }

    @Override
    protected FocusIdentificationModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration> createModuleConfigurer(FocusIdentificationAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new FocusIdentificationModuleWebSecurityConfigurer<>(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new FocusIdentificationProvider());
    }

    @Override
    protected FocusIdentificationModuleAuthentication createEmptyModuleAuthentication(FocusIdentificationAuthenticationModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        FocusIdentificationModuleAuthentication moduleAuthentication = new FocusIdentificationModuleAuthentication(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setModuleConfiguration(moduleType.getItem());
        return moduleAuthentication;
    }

}
