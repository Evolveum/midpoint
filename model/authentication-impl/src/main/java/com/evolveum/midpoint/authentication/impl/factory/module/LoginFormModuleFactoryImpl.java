/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.impl.provider.PasswordProvider;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.module.configurer.LoginFormModuleWebSecurityConfigurer;
import com.evolveum.midpoint.authentication.impl.module.authentication.LoginFormModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configuration.LoginFormModuleWebSecurityConfiguration;

import jakarta.servlet.ServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class LoginFormModuleFactoryImpl extends AbstractCredentialModuleFactory<
        LoginFormModuleWebSecurityConfiguration,
        LoginFormModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration, LoginFormAuthenticationModuleType>,
        LoginFormAuthenticationModuleType,
        LoginFormModuleAuthenticationImpl> {

    @Override
    public boolean match(AbstractAuthenticationModuleType moduleType, AuthenticationChannel authenticationChannel) {
        return moduleType instanceof LoginFormAuthenticationModuleType;
    }

    @Override
    protected LoginFormModuleWebSecurityConfigurer<LoginFormModuleWebSecurityConfiguration, LoginFormAuthenticationModuleType> createModuleConfigurer(LoginFormAuthenticationModuleType moduleType, String sequenceSuffix, AuthenticationChannel authenticationChannel, ObjectPostProcessor<Object> objectPostProcessor, ServletRequest request) {
        return new LoginFormModuleWebSecurityConfigurer<>(moduleType, sequenceSuffix, authenticationChannel,
                objectPostProcessor, request,
                new PasswordProvider());
    }

    @Override
    protected Class<? extends CredentialPolicyType> supportedClass() {
        return PasswordCredentialsPolicyType.class;
    }

    @Override
    protected LoginFormModuleAuthenticationImpl createEmptyModuleAuthentication(LoginFormAuthenticationModuleType moduleType,
            LoginFormModuleWebSecurityConfiguration configuration, AuthenticationSequenceModuleType sequenceModule, ServletRequest request) {
        LoginFormModuleAuthenticationImpl moduleAuthentication = new LoginFormModuleAuthenticationImpl(sequenceModule);
        moduleAuthentication.setPrefix(configuration.getPrefixOfModule());
        moduleAuthentication.setCredentialName(moduleType.getCredentialName());
        moduleAuthentication.setCredentialType(supportedClass());
        moduleAuthentication.setNameOfModule(configuration.getModuleIdentifier());
        return moduleAuthentication;
    }

}
