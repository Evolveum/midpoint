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
import com.evolveum.midpoint.authentication.impl.module.authentication.FocusIdentificationModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.impl.module.configurer.FocusIdentificationModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.provider.FocusIdentificationProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentificationAuthenticationModuleType;

/**
 * @author skublik
 */
@Component
public class FocusIdentificationModuleFactory extends AbstractModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        FocusIdentificationModuleWebSecurityConfigurer,
        FocusIdentificationAuthenticationModuleType,
        FocusIdentificationModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof FocusIdentificationAuthenticationModuleType;
    }

    @Override
    protected FocusIdentificationModuleWebSecurityConfigurer createModuleConfigurer(FocusIdentificationAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new FocusIdentificationModuleWebSecurityConfigurer(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new FocusIdentificationProvider());
    }

    @Override
    protected FocusIdentificationModuleAuthenticationImpl createEmptyModuleAuthentication(FocusIdentificationAuthenticationModuleType moduleType,
                                                                                          LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        FocusIdentificationModuleAuthenticationImpl moduleAuthentication = new FocusIdentificationModuleAuthenticationImpl(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        moduleAuthentication.setModuleConfiguration(moduleType.getItem());
        return moduleAuthentication;
    }

}
